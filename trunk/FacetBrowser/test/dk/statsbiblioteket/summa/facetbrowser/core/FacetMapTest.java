/* $Id: FacetMapTest.java,v 1.4 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/11 12:56:25 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.facetbrowser.core;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.io.StringWriter;
import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapBitStuffed;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerImpl;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.Facet;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.MemoryStringPool;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * FacetMap Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetMapTest extends TestCase {
    public FacetMapTest(String name) {
        super(name);
    }

    private FacetMap map;
    private CoreMap core;
    private TagHandler handler;

    public void setUp() throws Exception {
        super.setUp();

        core = new CoreMapBitStuffed(100, 3);
        MemoryStorage memStore = new MemoryStorage();
        memStore.put(StructureDescription.FACETS, "A, B, C");
        Configuration config = new Configuration(memStore);
        StructureDescription structure = new StructureDescription(config);
        Facet[] pools = new Facet[structure.getFacetNames().size()];
        int position = 0;
        //noinspection UnusedDeclaration
        for (String facetName: structure.getFacetNames()) {
            //noinspection DuplicateStringLiteralInspection
            pools[position++] = new Facet(
                    new File(System.getProperty("java.io.tmpdir")), 
                    "facettest" + position, null, null, true, true);
        }
        handler = new TagHandlerImpl(structure, pools);
        map = new FacetMap(config, core, handler);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        //noinspection AssignmentToNull
        map = null;
    }

    public Map<String, List<String>> arraysToMap(String[] facets,
                                                 String[][] tags) {
        Map<String, List<String>> result =
                new HashMap<String, List<String>>(facets.length);
        int facetID = 0;
        for (String facet: facets) {
            result.put(facet, Arrays.asList(tags[facetID++]));
        }
        return result;
    }

    public void testGetDocCount() throws Exception {
        assertEquals("Initial count should be 0", 0, map.getDocCount());
        map.addToDocument(0, arraysToMap(new String[]{"A"},
                                         new String[][]{{"AFoo", "ABar"}}));
        assertEquals("Adding one should give 1", 1, map.getDocCount());
        map.addToDocument(0, arraysToMap(new String[]{"B"},
                                         new String[][]{{"BFoo", "BBar"}}));
        assertEquals("Updating should still give 1", 1, map.getDocCount());
        map.addToDocument(1, arraysToMap(new String[]{"B"},
                                         new String[][]{{"BZoo", "BBar"}}));
        assertEquals("Adding another should give 2", 2, map.getDocCount());
        map.removeDocument(0);
        assertEquals("Removing a document should decrease the count", 
                     1, map.getDocCount());
    }

    protected String dump(String[] values) {
        StringWriter sw = new StringWriter(values.length * 4);
        sw.append("[");
        for (int i = 0 ; i < values.length ; i++) {
            sw.append(values[i]);
            if (i < values.length - 1) {
                sw.append(", ");
            }
        }
        sw.append("]");
        return sw.toString();
    }

    public void assertHasTags(String message, int docID, String facet,
                              String[] expectedTags) throws Exception {
        int facetID = handler.getFacetID(facet);
        if (facetID == -1) {
            fail(message + ". Facet \"" + facet + "\" was not present");
        }
        int[] tagIDs = core.get(docID, facetID);
        String[] tagNames = new String[tagIDs.length];
        for (int i = 0 ; i < tagIDs.length ; i++) {
            tagNames[i] = handler.getTagName(facetID, tagIDs[i]);
        }
        if (Arrays.equals(expectedTags, tagNames)) {
            return;
        }
        fail(message + ". Expected: " + dump(expectedTags)
             + " got " + dump(tagNames));
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void testCorrectness() throws Exception {
        map.addToDocument(0, arraysToMap(new String[]{"A"},
                                         new String[][]{{"AFoo",
                                                         "ABar",
                                                         "AZoo",
                                                         "ABoo"}}));
        assertHasTags("Facet A should be added", 0, "A", 
                      new String[]{"AFoo", "ABar", "AZoo", "ABoo"});
        map.addToDocument(0, arraysToMap(new String[]{"A"},
                                         new String[][]{{"AKazam"}}));
        assertHasTags("Facet A should have AKazam added", 0, "A",
                      new String[]{"AFoo", "ABar", "AZoo", "ABoo", "AKazam"});
        map.addToDocument(0, arraysToMap(new String[]{"B"},
                                         new String[][]{{"BKazam"}}));
        assertHasTags("Facet B should have BKazam only", 0, "B",
                      new String[]{"BKazam"});
        map.addToDocument(1, arraysToMap(new String[]{"A"},
                                         new String[][]{{"AFoo",
                                                         "ABar",
                                                         "AZoo",
                                                         "ABii"}}));
        assertHasTags("Facet 1/A should be added", 1, "A",
                      new String[]{"AFoo", "ABar", "AZoo", "ABii"});
        assertHasTags("Facet 0/A should be unchanged", 0, "A", 
                      new String[]{"AFoo", "ABar", "AZoo", "ABoo", "AKazam"});
        map.removeDocument(0);
        assertHasTags("Facet 1/A should now be 0/A", 0, "A",
                      new String[]{"AFoo", "ABar", "AZoo", "ABii"});
        try {
            core.get(1, 0);
            fail("Document 1 should no longer exist");
        } catch(Exception e) {
            // Expected
        }
    }

    public static Test suite() {
        return new TestSuite(FacetMapTest.class);
    }
}
