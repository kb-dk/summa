/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Monitor registry settings and response time statistics over JMX.
 * @deprecated in favor of {@link SummaSearcher}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, mv")
public interface SearchEngineImplMBean {

    /** Returns the response time of the last executed query.
     *
     * @return Last known response tim in milliseconds. 0 if no queries
     * performed yet.
     */
    int getLastResponseTime();

    /** Get the registry server used when talken to storage RMI service.
     *
     * @return The registry server, or "Unknown".
     */
    String getRegistryServer();

    /** Get the registry port used when talking to storage RMI service.
     *
     * @return The registry port, or -1 for unknown
     */
    int getRegistryPort();

    /** Get the RMI service name used when talking to storage RMI service.
     *
     * @return The rmi service name, or "Unknown".
     */
    String getRegistryServiceName();
}


