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
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.util.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DiscardUpdatesFilterTest extends TestCase implements ObjectFilter {

    public DiscardUpdatesFilterTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ids.clear();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(DiscardUpdatesFilterTest.class);
    }

    public void testDiscarding() {
        assertTrue("The first list should get all Payloads through",
                   gotThrough(Arrays.asList("a", "b", "c")));
        assertFalse("The second list should not get all Payloads through",
                    gotThrough(Arrays.asList("a", "b", "c", "a")));
    }

    private boolean gotThrough(List<String> testIDs) {
        this.ids.clear();
        this.ids.addAll(testIDs);
        List<String> processed = new ArrayList<String>(testIDs.size());
        ObjectFilter discarder =
                new DiscardUpdatesFilter(Configuration.newMemoryBased());
        discarder.setSource(this);
        while (discarder.hasNext()) {
            processed.add(discarder.next().getId());
        }
        return Arrays.equals(testIDs.toArray(), processed.toArray());
    }

    /* Feeder for the Filter */
    private List<String> ids = new ArrayList<String>(10);

    public boolean hasNext() {
        return ids.size() > 0;
    }
    public Payload next() {
        return new Payload(new Record(ids.remove(0), "foo", new byte[0]));
    }
    public void remove() {
        // Nada
    }
    public void setSource(Filter filter) {
        // Nada
    }
    public boolean pump() throws IOException {
        if (hasNext()) {
            next().close();
        }
        return hasNext();
    }
    public void close(boolean success) {
        // Nada
    }
}

