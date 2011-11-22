/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.dao.hibernate;

import static org.opennms.core.utils.InetAddressUtils.str;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.dao.AtInterfaceDao;
import org.opennms.netmgt.dao.IpInterfaceDao;
import org.opennms.netmgt.model.OnmsAtInterface;
import org.opennms.netmgt.model.OnmsCriteria;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.springframework.beans.factory.annotation.Autowired;

public class AtInterfaceDaoHibernate extends AbstractDaoHibernate<OnmsAtInterface, Integer>  implements AtInterfaceDao {
    
    @Autowired
    private IpInterfaceDao m_ipInterfaceDao;

    public AtInterfaceDaoHibernate() {
        super(OnmsAtInterface.class);
    }

	@Override
	public void markDeletedIfNodeDeleted() {
	    final OnmsCriteria criteria = new OnmsCriteria(OnmsAtInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("node.type", "D"));
        
        for (final OnmsAtInterface iface : findMatching(criteria)) {
        	iface.setStatus('D');
        	saveOrUpdate(iface);
        }
	}

    @Override
    public void deactivateForNodeIdIfOlderThan(final int nodeid, final Timestamp scanTime) {
        OnmsCriteria criteria = new OnmsCriteria(OnmsAtInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("node.id", nodeid));
        criteria.add(Restrictions.lt("lastPollTime", scanTime));
        criteria.add(Restrictions.eq("status", "A"));
        
        for (final OnmsAtInterface iface : findMatching(criteria)) {
            iface.setStatus('N');
            saveOrUpdate(iface);
        }
    }

    @Override
    public Collection<OnmsAtInterface> findByMacAddress(final String macAddress) {
        // SELECT atinterface.nodeid, atinterface.ipaddr, ipinterface.ifindex from atinterface left JOIN ipinterface ON atinterface.nodeid = ipinterface.nodeid AND atinterface.ipaddr = ipinterface.ipaddr WHERE atphysaddr = ? AND atinterface.status <> 'D'

        final OnmsCriteria criteria = new OnmsCriteria(OnmsAtInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("macAddress", macAddress));
        criteria.add(Restrictions.ne("status", "D"));

        return findMatching(criteria);
    }

    @Override
    public void setStatusForNode(Integer nodeid, Character action) {
        // UPDATE atinterface set status = ?  WHERE sourcenodeid = ? OR nodeid = ?

        final OnmsCriteria criteria = new OnmsCriteria(OnmsAtInterface.class);
        criteria.add(Restrictions.or(Restrictions.eq("node.id", nodeid), Restrictions.eq("sourceNodeId", nodeid)));
        
        for (final OnmsAtInterface iface : findMatching(criteria)) {
            iface.setStatus(action);
            saveOrUpdate(iface);
        }
    }

    @Override
    public void setStatusForNodeAndIp(final Integer nodeid, final String ipAddr, final Character action) {
        // ps = dbConn.prepareStatement("UPDATE atinterface set status = ?  WHERE nodeid = ? AND ipaddr = ?");
        
        final OnmsCriteria criteria = new OnmsCriteria(OnmsAtInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("node.id", nodeid));
        criteria.add(Restrictions.eq("ipAddress", ipAddr));
        
        for (final OnmsAtInterface iface : findMatching(criteria)) {
            iface.setStatus(action);
            saveOrUpdate(iface);
        }
    }

    @Override
    public void setStatusForNodeAndIfIndex(final Integer nodeid, final Integer ifIndex, final Character action) {
        // UPDATE atinterface set status = ?  WHERE sourcenodeid = ? AND ifindex = ?

        final OnmsCriteria criteria = new OnmsCriteria(OnmsAtInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("node.id", nodeid));
        criteria.add(Restrictions.eq("ifIndex", ifIndex));
        
        for (final OnmsAtInterface iface : findMatching(criteria)) {
            iface.setStatus(action);
            saveOrUpdate(iface);
        }
    }

    @Override
    public OnmsAtInterface findByNodeAndAddress(final Integer nodeId, final InetAddress ipAddress, final String macAddress) {
        final OnmsCriteria criteria = new OnmsCriteria(OnmsAtInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("node.id", nodeId));
        criteria.add(Restrictions.eq("ipAddress", ipAddress));
        criteria.add(Restrictions.eq("macAddress", macAddress));
        
        final List<OnmsAtInterface> ifaces = findMatching(criteria);
        if (ifaces.size() == 0) {
            return null;
        } else {
            return ifaces.get(0);
        }
    }

    @Override
    public void saveAtInterface(final Connection dbConn, final OnmsAtInterface at) {
        OnmsAtInterface atInterface = findByNodeAndAddress(at.getNode().getId(), at.getIpAddress(), at.getMacAddress());
        if (atInterface == null) {
            atInterface = at;
        } else {
            atInterface.setIfIndex(at.getIfIndex());
            atInterface.setIpAddress(at.getIpAddress());
            atInterface.setLastPollTime(at.getLastPollTime());
            atInterface.setMacAddress(at.getMacAddress());
            atInterface.setNode(at.getNode());
            atInterface.setSourceNodeId(at.getSourceNodeId());
            atInterface.setStatus(at.getStatus());
        }
        
        // "nodeId", "ipAddr", "atPhysAddr
        saveOrUpdate(at);
    }

    // SELECT node.nodeid,ipinterface.ifindex FROM node LEFT JOIN ipinterface ON node.nodeid = ipinterface.nodeid WHERE nodetype = 'A' AND ipaddr = ?
    @Override
    public OnmsAtInterface getAtInterfaceForAddress(final Connection dbConn, final InetAddress address) {
        final String addressString = str(address);

        if (address.isLoopbackAddress() || addressString.equals("0.0.0.0")) return null;

        // See if we have an existing version of this OnmsAtInterface first
        final OnmsCriteria criteria = new OnmsCriteria(OnmsAtInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("node.type", "A"));
        criteria.add(Restrictions.eq("ipAddress", addressString));
        List<OnmsAtInterface> interfaces = findMatching(criteria);

        if (interfaces.isEmpty()) {
            // Create a new OnmsAtInterface if the IP address is in the database
            // WE NEED TO UPSERT!!!
            OnmsIpInterface iface;
            final List<OnmsIpInterface> ifaces = m_ipInterfaceDao.findByIpAddress(addressString);
            if (ifaces.isEmpty()) {
                return null;
            } else {
                if (ifaces.size() > 1) {
                    LogUtils.debugf(this, "getAtInterfaceForAddress: More than one IpInterface matched address %s!", addressString);
                }
                iface = ifaces.get(0);
                return new OnmsAtInterface(iface.getNode(), iface.getIpAddress());
            }
        } else {
            if (interfaces.size() > 1) {
                LogUtils.debugf(this, "getAtInterfaceForAddress: More than one AtInterface matched address %s!", addressString);
            }
            return interfaces.get(0);
        }
    }

}
