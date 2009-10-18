//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
// 
// Created: October 5th, 2009
//
// Copyright (C) 2009 The OpenNMS Group, Inc.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
package org.opennms.web.command;

import java.util.Date;

public class DatabaseReportCommand {
    

        private String m_reportId;

        private String m_reportCategory;

        private Date m_endDate;

        private String m_mailTo;

        private String m_logo;

        private Boolean m_persist;
        
        private Boolean m_sendMail;
        
        private String m_mailFormat;

        public String getLogo() {
            return m_logo;
        }

        public void setLogo(String logo) {
            m_logo = logo;
        }

        public String getMailTo() {
            return m_mailTo;
        }

        public void setMailTo(String email) {
            m_mailTo = email;
        }

        public String getReportCategory() {
            return m_reportCategory;
        }

        public void setReportCategory(String categoryName) {
            m_reportCategory = categoryName;
        }

        public String getMailFormat() {
            return m_mailFormat;
        }

        public void setMailFormat(String format) {
            m_mailFormat = format;
        }

        public Date getEndDate() {
            return m_endDate;
        }

        public void setEndDate(Date endDate) {
            m_endDate = endDate;
        }

        public void setPersist(Boolean persist) {
            m_persist = persist;
        }

        public Boolean getPersist() {
            return m_persist;
        }
        
        public void setSendMail(Boolean sendEmail) {
            m_sendMail = sendEmail;
        }

        public Boolean getSendMail() {
            return m_sendMail;
        }

        public void setReportId(String reportId) {
            m_reportId = reportId;
        }

        public String getReportId() {
            return m_reportId;
        }

}
