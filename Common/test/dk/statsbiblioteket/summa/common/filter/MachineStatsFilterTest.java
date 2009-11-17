/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
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
                        MachineStatsFilter.CONF_GC_SLEEP_MS, 0,
                        MachineStatsFilter.CONF_LOG_INTERVAL_PAYLOADS, 5
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
