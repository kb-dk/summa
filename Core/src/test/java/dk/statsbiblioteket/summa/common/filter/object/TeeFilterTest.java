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
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TeeFilterTest extends TestCase {

    public void testQuad() {
        PayloadFeederHelper feeder = getFeeder();
        ObjectFilter tee = new TeeFilter(Configuration.newMemoryBased(
                TeeFilter.CONF_BASES, "baseA, baseB",
                TeeFilter.CONF_ID_PREFIXES, "prefixA_, prefixB_"
        ));
        List<Payload> generated = extractRecords(feeder, tee, 8);
        for (String prefix: Arrays.asList("prefixA_", "prefixB_")) {
            for (String id: Arrays.asList("original1", "original2")) {
                String concat = prefix + id;
                for (String base: Arrays.asList("baseA", "baseB")) {
                    assertContains(concat, base, generated);
                }
            }
        }
    }

    public void testID() {
        PayloadFeederHelper feeder = getFeeder();
        ObjectFilter tee = new TeeFilter(Configuration.newMemoryBased(
                TeeFilter.CONF_ID_PREFIXES, "prefixA_, prefixB_"
        ));
        List<Payload> generated = extractRecords(feeder, tee, 4);
        for (String prefix: Arrays.asList("prefixA_", "prefixB_")) {
            for (String id: Arrays.asList("original1", "original2")) {
                String concat = prefix + id;
                assertContains(concat, "originalBase", generated);
            }
        }
    }

    public void testBase() {
        PayloadFeederHelper feeder = getFeeder();
        ObjectFilter tee = new TeeFilter(Configuration.newMemoryBased(
                TeeFilter.CONF_BASES, "baseA, baseB"
        ));
        List<Payload> generated = extractRecords(feeder, tee, 4);
        for (String id: Arrays.asList("original1", "original2")) {
            for (String base: Arrays.asList("baseA", "baseB")) {
                assertContains(id, base, generated);
            }
        }
    }

    private List<Payload> extractRecords(PayloadFeederHelper feeder, ObjectFilter tee, int expected) {
        tee.setSource(feeder);
        List<Payload> generated = new ArrayList<>();
        while (tee.hasNext()) {
            generated.add(tee.next());
        }
        assertEquals("There should be the right number of produced Records", expected, generated.size());
        assertContent(generated);
        return generated;
    }

    private PayloadFeederHelper getFeeder() {
        PayloadFeederHelper feeder;
        feeder = new PayloadFeederHelper(Arrays.asList(
                new Payload(new Record("original1", "originalBase", "content".getBytes())),
                new Payload(new Record("original2", "originalBase", "content".getBytes()))
        ));
        return feeder;
    }

    private void assertContains(String id, String base, List<Payload> payloads) {
        for (Payload payload: payloads) {
            if (payload.getId().equals(id) && payload.getRecord().getBase().equals(base)) {
                return;
            }
        }
        fail("Unable to locate a Payload with id=" + id + ", base=" + base + " in\n" + Strings.join(payloads, "\n"));
    }

    private void assertContent(List<Payload> payloads) {
        for (Payload payload: payloads) {
            assertEquals("The content of " + payload + " should be as expected",
                         "content", payload.getRecord().getContentAsUTF8());
        }
    }
}
