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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class DiscardSizeFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(
            DiscardSizeFilterTest.class);

    public DiscardSizeFilterTest(String name) {
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
        return new TestSuite(DiscardSizeFilterTest.class);
    }

    private List<Payload> getSampleData() {
        List<Payload> payloads = new ArrayList<>(10);

        Record largeRecord = new Record("Large", "dummy", new byte[10*1024]);
        Record middleRecord = new Record("Middle", "dummy", new byte[1024]);
        Record smallRecord =  new Record("Small",  "dummy", new byte[1]);

        payloads.add(new Payload(largeRecord));
        payloads.add(new Payload(middleRecord));
        payloads.add(new Payload(smallRecord));

        return payloads;
    }

    private List<Payload> suck(ObjectFilter filter) {
        PayloadFeederHelper feeder = new PayloadFeederHelper(getSampleData());
        filter.setSource(feeder);
        List<Payload> result = new ArrayList<>(10);
        while (filter.hasNext()) {
            result.add(filter.next());
        }
        return result;
    }

    private List<String> suckIDs(ObjectFilter filter) {
        List<String> ids = new ArrayList<>();
        List<Payload> payloads = suck(filter);
        for (Payload payload: payloads) {
            ids.add(payload.getId());
        }
        return ids;
    }

    public void testDiscardSizeDefault() {
        DiscardSizeFilter discarder = new DiscardSizeFilter(Configuration.newMemoryBased(
        ));
        assertEquals("All IDs should pass for default setup",
                     "Large, Middle, Small",
                     Strings.join(suckIDs(discarder)));
    }
    public void testDiscardSizeMax() {
        DiscardSizeFilter discarder = new DiscardSizeFilter(Configuration.newMemoryBased(
                DiscardSizeFilter.CONF_MAX, 5*1024
        ));
        assertEquals("Large record should be discarded",
                     "Middle, Small",
                     Strings.join(suckIDs(discarder)));
    }
    public void testDiscardSizeMin() {
        DiscardSizeFilter discarder = new DiscardSizeFilter(Configuration.newMemoryBased(
                DiscardSizeFilter.CONF_MIN, 100
        ));
        assertEquals("Small record should be discarded",
                     "Large, Middle",
                     Strings.join(suckIDs(discarder)));
    }
}
