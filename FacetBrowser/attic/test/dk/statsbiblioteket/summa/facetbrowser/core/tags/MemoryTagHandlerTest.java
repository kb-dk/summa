/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.TestCommon;
import dk.statsbiblioteket.summa.facetbrowser.IndexBuilder;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.index.IndexReader;

/**
 * MemoryTagHandler Tester.
 *
 * @author <Authors name>
 * @since <pre>10/06/2006</pre>
 * @version 1.0
 * @deprecated as MemoryTagHandler is deprecated.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", "deprecation"})
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class MemoryTagHandlerTest extends TestCase {
    private String[] facets = new String[]{"fooFacet", "barFacet", "zooFacet"};
    private String[][] tags = new String[][]{{"fooTag3", "fooTag2", "fooTag1"},
                                     {},
                                     {"zooTagSingle"}};
    private TagHandler tagHandler;
    private File persistenceLocation =
            new File(new File(System.getProperty("java.io.tmpdir")),
                                                 "tagTest").getAbsoluteFile();

    public MemoryTagHandlerTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        tagHandler =
                   new MemoryTagHandler(new StructureDescription(facets), tags);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestCommon.deleteDir(persistenceLocation);
    }

    public void dumpSpeed() throws Exception {
        IndexBuilder.checkIndex();
        String[] fn = new String[]{IndexBuilder.AUTHOR, IndexBuilder.GENRE,
                                   IndexBuilder.TITLE, IndexBuilder.FREETEXT,
                                   IndexBuilder.VARIABLE};
        List<String> facetNames = new ArrayList<String>(fn.length);
        for (String f: fn) {
            facetNames.add(f);
        }
        StructureDescription structure = new StructureDescription(facetNames);

        IndexReader reader = IndexBuilder.getReader();
        TagHandler tagHandler = null;
        System.out.println("Warming up...");
        for (int i = 0; i < 5 ; i++) {
            tagHandler = new MemoryTagHandler(reader, structure);
        }
        System.out.println("Testing performance");
        Profiler profiler = new Profiler();
        for (int i = 0; i < 5 ; i++) {
            tagHandler = new MemoryTagHandler(reader, structure);
            profiler.beat();
        }
        System.out.println("Average " + 1000 / profiler.getBps(true) + " ms on "
                           + IndexBuilder.REPLICATIONCOUNT + " documents");
        assert tagHandler != null;
        System.out.println(tagHandler.getStats());
    }


    public void testGetTagID() throws Exception {
        assertEquals("The ID for fooFacet should be 0",
                     0, tagHandler.getFacetID(facets[0]));
        assertEquals("Unknown facet should give -1",
                     -1, tagHandler.getFacetID("Non-existing"));
    }

    public void testGetTagName() throws Exception {
        assertEquals("The name for the first tag in fooFacet should be " +
                     "fooTag1 as they should be sorted",
                     tags[0][2], tagHandler.getTagName(0, 0));
        try {
            tagHandler.getTagName(27, 0);
            fail("Requesting the tag name from a non-existing facet should " +
                 "throw an exception");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        } catch (Exception e) {
            fail("Requesting the tag name from a non-existing facet should " +
                 "throw an IndexOutOfBoundsException. Instead we got " +
                 e.getMessage());
        }

        try {
            tagHandler.getTagName(0, 45);
            fail("Requesting the tag name from a non-existing tag should " +
                 "throw an exception");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        } catch (Exception e) {
            fail("Requesting the tag name from a non-existing tag should " +
                 "throw an IndexOutOfBoundsException. Instead we got " +
                 e.getClass() + " with " +
                 e.getMessage());
        }
    }

    public void testGetFacetID() throws Exception {
        int counter = 0;
        for (String facet: facets) {
            assertEquals("The ID for the facet " + facet +
                         " should be " + counter,
                         counter++, tagHandler.getFacetID(facet));
        }
        assertEquals("The id for an unknown facet should be -1",
                     -1, tagHandler.getFacetID("Nonexisting"));
    }

    public void testGetFacetSize() throws Exception {
        int counter = 0;
        for (String facet: facets) {
            assertEquals("The size of " + facet + " should be " +
                         tags[counter].length,
                         tags[counter].length,
                         tagHandler.getFacetSize(counter));
            counter++;
        }
        // TODO: Test out of bounds
    }

    public void testStore() {
        try {
            tagHandler.store(persistenceLocation);
        } catch (IOException e) {
            fail("Failed to store content: " + e.getMessage());
        }
        MemoryTagHandler tagHandler2 = null;
        try {
            tagHandler2 =
               new MemoryTagHandler(persistenceLocation,
                                    new StructureDescription(facets));
        } catch (IOException e) {
            fail("Failed to load content: " + e);
        }
        assertEquals("There should be the same number of Facets",
                     tagHandler.getFacetNames().size(),
                     tagHandler2.getFacetNames().size());
        int facetID = 0;
        for (String facet: tagHandler.getFacetNames()) {
            assertEquals("Facet IDs should be equal",
                         tagHandler.getFacetID(facet),
                         tagHandler2.getFacetID(facet));
            int tagCount = tagHandler.getFacetSize(facetID);
            for (int tagID = 0 ; tagID < tagCount ; tagID++) {
                String sourceName = tagHandler.getTagName(facetID, tagID);
                String destName = tagHandler2.getTagName(facetID, tagID);
                assertEquals("The tag names should be equal",
                             sourceName, destName);
                assertNotNull("The taghandler2 should be defined", tagHandler2);
                assertNotNull("Source name should be defined", sourceName);
                assertTrue("facetID should be 0 or positive", facetID >= 0);

                // Don't test getTagID, as it is not supported after open
/*
                assertEquals("The tag IDs for facet #" + facetID + " and sourceName " + sourceName
                             + " from the loaded should be -1, "
                             + "as part of memory optimization",
                             -1,
                             tagHandler2.getTagID(facetID, sourceName));
                             */
            }
            facetID++;
        }

        int counter = 0;
        for (String facet: facets) {
            assertEquals("The size of " + facet + " should be " +
                         tags[counter].length,
                         tags[counter].length,
                         tagHandler.getFacetSize(counter));
            counter++;
        }
    }

    public static Test suite() {
        return new TestSuite(MemoryTagHandlerTest.class);
    }
}
