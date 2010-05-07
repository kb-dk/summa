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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.MachineStats;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.List;

public class MachineStatsFilterTest extends TestCase {
    public MachineStatsFilterTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(MachineStatsFilterTest.class);
    }

    public void testPayloadCountLogging() throws Exception {
        MachineStatsFilter stats =
                new MachineStatsFilter(Configuration.newMemoryBased(
                        MachineStats.CONF_GC_BEFORE_LOG, true,
                        MachineStats.CONF_GC_SLEEP_MS, 0,
                        MachineStats.CONF_LOG_INTERVAL_PINGS, 5,
                        MachineStats.CONF_LOG_INTERVAL_MS, 10000
                ));
        stats.setSource(getFeeder(100, 5));
        while (stats.hasNext()) {
            stats.next().close();
        }
        stats.close(true);
    }

    private PayloadFeederHelper getFeeder(int payloadCount, int delay) {
        List<Payload> payloads = new ArrayList<Payload>(payloadCount);
        for (int i = 0 ; i < payloadCount ; i++) {
            payloads.add(new Payload(new Record(
                    Integer.toString(i), "dummy", new byte[10])));
        }
        return new PayloadFeederHelper(payloads, delay);
    }
}

