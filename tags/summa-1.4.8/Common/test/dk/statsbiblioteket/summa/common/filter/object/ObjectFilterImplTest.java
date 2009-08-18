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
package dk.statsbiblioteket.summa.common.filter.object;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.Record;

import java.util.List;
import java.util.Arrays;

public class ObjectFilterImplTest extends TestCase {
    public ObjectFilterImplTest(String name) {
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
        return new TestSuite(ObjectFilterImplTest.class);
    }

    public void testMultipleCalls() throws Exception {
        List<Payload> payloads = Arrays.asList(
                new Payload(new Record("A", "F", new byte[0])),
                new Payload(new Record("B", "F", new byte[0])),
                new Payload(new Record("C", "F", new byte[0])));
        PayloadFeederHelper feeder = new PayloadFeederHelper(payloads);
        SimpleProcesser processor =
                new SimpleProcesser(Configuration.newMemoryBased());
        processor.setSource(feeder);
        int count = 1;
        while (processor.pump()) {
            count++;
        }
        assertEquals("The number of processed Payloads should match the input",
                     payloads.size(), count);

    }

    private class SimpleProcesser extends ObjectFilterImpl {
        public SimpleProcesser(Configuration conf) {
            super(conf);
        }

        @Override
        protected boolean processPayload(Payload payload) {
            // Nada
            return true;
        }
    }
}
