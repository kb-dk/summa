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
