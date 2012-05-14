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
import dk.statsbiblioteket.summa.common.filter.DummyFilter;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.util.List;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class FilterSequenceTest extends TestCase implements ObjectFilter {
    public FilterSequenceTest(String name) {
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
        return new TestSuite(FilterSequenceTest.class);
    }

    public void testSimpleSequence() throws Exception {
        Configuration seqConf = Configuration.newMemoryBased();
        List<Configuration> seqElements =
                seqConf.createSubConfigurations(FilterSequence.CONF_FILTERS, 2);

        Configuration aConf = seqElements.get(0); // a
        aConf.set(FilterSequence.CONF_FILTER_CLASS, DummyFilter.class);
        aConf.set("key", "aFoo");
        aConf.set("value", "aBar");

        Configuration bConf = seqElements.get(1); // b
        bConf.set(FilterSequence.CONF_FILTER_CLASS, DummyFilter.class);
        bConf.set("key", "bFoo");
        bConf.set("value", "bBar");

        FilterSequence sequence = new FilterSequence(seqConf);
        sequence.setSource(this);

        Payload first = sequence.next();
        assertEquals("The first Payload should have the right id",
                     lastID, first.getId());
        assertTrue("The Payload should be marked from Dummy a",
                   first.getData("aFoo") != null);
    }

    public void testEnable() throws Exception {
        Configuration seqConf = Configuration.newMemoryBased();
        List<Configuration> seqElements =
                seqConf.createSubConfigurations(FilterSequence.CONF_FILTERS, 2);

        Configuration aConf = seqElements.get(0);
        aConf.set(FilterSequence.CONF_FILTER_CLASS, DummyFilter.class);
        aConf.set(FilterSequence.CONF_FILTER_ENABLED, false);
        aConf.set("key", "aFoo");
        aConf.set("value", "aBar");

        Configuration bConf = seqElements.get(1);
        bConf.set(FilterSequence.CONF_FILTER_CLASS, DummyFilter.class);
        bConf.set("key", "bFoo");
        bConf.set("value", "bBar");

        FilterSequence sequence = new FilterSequence(seqConf);
        assertEquals("The created filter sequence should only have a single "
                     + "enabled filter", 1, sequence.getFilters().size());
    }

    /* Object filter interface */

    public boolean hasNext() {
        return true;
    }

    public void setSource(Filter filter) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean pump() throws IOException {
        return hasNext() && next() != null;
    }

    public void close(boolean success) {
        // Do nothing
    }

    private int counter = 0;
    private String lastID;
    public Payload next() {
        lastID = "Dummy" + counter++;
        return new Payload(new Record(lastID, "foo", new byte[0]));
    }

    public void remove() {
        throw new UnsupportedOperationException("Not implemented");
    }
}

