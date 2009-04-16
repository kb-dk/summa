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

/**
 * FilterSequence Tester.
 *
 * @author <Authors name>
 * @since <pre>11/10/2008</pre>
 * @version 1.0
 */
public class FilterSequenceTest extends TestCase implements ObjectFilter {
    public FilterSequenceTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

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
