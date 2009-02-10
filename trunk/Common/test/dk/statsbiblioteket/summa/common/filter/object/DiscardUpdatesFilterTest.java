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
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;
import java.util.*;

/**
 * DiscardUpdatesFilter Tester.
 *
 * @author <Authors name>
 * @since <pre>02/10/2009</pre>
 * @version 1.0
 */
public class DiscardUpdatesFilterTest extends TestCase implements ObjectFilter {

    public DiscardUpdatesFilterTest(String name) {
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
        DiscardUpdatesFilter discarder =
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
