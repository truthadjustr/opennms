/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.core.ipc.sink.aws.sqs;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.ipc.sink.api.SyncDispatcher;
import org.opennms.core.ipc.sink.aws.sqs.heartbeat.Heartbeat;
import org.opennms.core.ipc.sink.aws.sqs.heartbeat.HeartbeatConsumer;
import org.opennms.core.ipc.sink.aws.sqs.heartbeat.HeartbeatGenerator;
import org.opennms.core.ipc.sink.aws.sqs.heartbeat.HeartbeatModule;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * Used to help profile the sink producer and consumer against AWS SQS.
 * 
 * <b>Warning:</b> This test requires AWS Access and appropriate credentials stored on ~/.aws/credentials
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
@Ignore
@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-mockDao.xml",
        "classpath:/META-INF/opennms/applicationContext-proxy-snmp.xml",
        "classpath:/META-INF/opennms/applicationContext-ipc-sink-server-aws.xml"
})
@JUnitConfigurationEnvironment
public class HeartbeatSinkPerfIT {

    /** The consumer manager. */
    @Autowired
    private AwsMessageConsumerManager consumerManager;

    /** The message dispatcher factory. */
    private AwsRemoteMessageDispatcherFactory messageDispatcherFactory = new AwsRemoteMessageDispatcherFactory();

    /** The generators. */
    private List<HeartbeatGenerator> generators = new ArrayList<>();

    /** The metrics. */
    private final MetricRegistry metrics = new MetricRegistry();

    /** The received meter. */
    private final Meter receivedMeter = metrics.meter("receivedMeter");

    /** The sent meter. */
    private final Meter sentMeter = metrics.meter("sent");

    /** The send timer. */
    private final Timer sendTimer = metrics.timer("send");

    /** The Constant NUM_CONSUMER_THREADS. */
    // Tuneables
    private static final int NUM_CONSUMER_THREADS = 2;

    /** The Constant NUM_GENERATORS. */
    private static final int NUM_GENERATORS = 2;

    /** The Constant RATE_PER_GENERATOR. */
    private static final double RATE_PER_GENERATOR = 1000.0;

    /**
     * Sets the up.
     *
     * @throws Exception the exception
     */
    @Before
    public void setUp() throws Exception {
        Hashtable<String, Object> awsConfig = new Hashtable<String, Object>();
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class, RETURNS_DEEP_STUBS);
        when(configAdmin.getConfiguration(AwsSinkConstants.AWS_CONFIG_PID).getProperties())
        .thenReturn(awsConfig);
        messageDispatcherFactory.setConfigAdmin(configAdmin);
        messageDispatcherFactory.init();
        consumerManager.afterPropertiesSet();
    }

    /**
     * Configure generators.
     *
     * @throws Exception the exception
     */
    public void configureGenerators() throws Exception {
        System.err.println("Starting Heartbeat generators.");

        // Start the consumer
        final HeartbeatModule parallelHeartbeatModule = new HeartbeatModule() {
            @Override
            public int getNumConsumerThreads() {
                return NUM_CONSUMER_THREADS;
            }
        };
        final HeartbeatConsumer consumer = new HeartbeatConsumer(parallelHeartbeatModule, receivedMeter);
        consumerManager.registerConsumer(consumer);

        // Start the dispatcher
        final SyncDispatcher<Heartbeat> dispatcher = messageDispatcherFactory.createSyncDispatcher(HeartbeatModule.INSTANCE);

        // Fire up the generators
        generators = new ArrayList<>(NUM_GENERATORS);
        for (int k = 0; k < NUM_GENERATORS; k++) {
            final HeartbeatGenerator generator = new HeartbeatGenerator(dispatcher, RATE_PER_GENERATOR, sentMeter, sendTimer);
            generators.add(generator);
            generator.start();
        }
    }

    /**
     * Tear down.
     *
     * @throws Exception the exception
     */
    @After
    public void tearDown() throws Exception {
        if (generators != null) {
            for (HeartbeatGenerator generator : generators) {
                generator.stop();
            }
            generators.clear();
        }
        consumerManager.unregisterAllConsumers();
    }

    /**
     * Quick run.
     *
     * @throws Exception the exception
     */
    @Test(timeout=30000)
    public void quickRun() throws Exception {
        configureGenerators();
        await().until(() -> Long.valueOf(receivedMeter.getCount()), greaterThan(100L)); 
    }

}
