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

