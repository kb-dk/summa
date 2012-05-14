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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RelativesFilterTest extends TestCase implements ObjectFilter {
    public RelativesFilterTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        reset();
    }

    private void reset() {
        records.clear();

        records.add(new Record("Alone1", "bar", new byte[0]));
        records.add(new Record("Alone2", "bar", new byte[0]));

        Record hasParent = new Record("HasParent", "bar", new byte[0]);
        hasParent.setParents(Arrays.asList(
                new Record("Parent", "bar", new byte[0])));
        records.add(hasParent);
        assertEquals("hasParent should have a parent",
                     1, hasParent.getParents().size());

        Record hasChildren = new Record("HasChildren", "bar", new byte[0]);
        hasChildren.setChildren(Arrays.asList(
                new Record("Child", "bar", new byte[0])));
        assertEquals("hasChildren should have a child",
                     1, hasChildren.getChildren().size());
        records.add(hasChildren);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(RelativesFilterTest.class);
    }

    public void testPlain() throws Exception {
        assertTrue("With no rules, everything should go through",
                   gotThrough(Configuration.newMemoryBased(),
                              Arrays.asList("Alone1", "Alone2",
                                            "HasParent", "HasChildren")));
        reset();
        assertTrue("Discard parents should discard properly",
                   gotThrough(Configuration.newMemoryBased(
                           DiscardRelativesFilter.CONF_DISCARD_HASPARENT, true),
                              Arrays.asList("Alone1", "Alone2",
                                            "HasChildren")));
        reset();
        assertTrue("Discard children should discard properly",
                   gotThrough(Configuration.newMemoryBased(
                           DiscardRelativesFilter.CONF_DISCARD_HASCHILDREN, true),
                              Arrays.asList("Alone1", "Alone2", "HasParent")));
        reset();
        assertTrue("Discard parent & children should discard properly",
                   gotThrough(Configuration.newMemoryBased(
                           DiscardRelativesFilter.CONF_DISCARD_HASPARENT, true,
                           DiscardRelativesFilter.CONF_DISCARD_HASCHILDREN, true),
                              Arrays.asList("Alone1", "Alone2")));
    }

    private boolean gotThrough(Configuration conf, List<String> testIDs) {
        List<String> processed = new ArrayList<String>(testIDs.size());
        ObjectFilter discarder = new DiscardRelativesFilter(conf);
        discarder.setSource(this);
        while (discarder.hasNext()) {
            processed.add(discarder.next().getId());
        }
        return Arrays.equals(testIDs.toArray(), processed.toArray());
    }

    /* Feeder for the Filter */
    private List<Record> records = new ArrayList<Record>(10);

    public boolean hasNext() {
        return records.size() > 0;
    }
    public Payload next() {
        return new Payload(records.remove(0));
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

