/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.MachineStats;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;

/**
 * Very simple search node that periodically outputs stats about #searches performed and free memory to log.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StatsSearchNode extends SearchNodeImpl {
    private static Log log = LogFactory.getLog(StatsSearchNode.class);

    private final MachineStats stats;

    public StatsSearchNode(Configuration conf) {
        super(conf);
        stats = new MachineStats(conf);
        log.info("Created StatsSearchNode");
    }

    @Override
    protected void managedWarmup(String request) { }

    @Override
    protected void managedOpen(String location) throws RemoteException { }

    @Override
    protected void managedClose() throws RemoteException { }

    @Override
    protected void managedSearch(Request request, ResponseCollection responses) throws RemoteException {
        stats.ping();
    }

    @Override
    public String getID() {
        return "StatsSearchNode";
    }

    @Override
    public int getFreeSlots() {
        return 9999;
    }
}
