/* $Id: DiskTagHandlerFacetTest.java,v 1.4 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:24 $
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
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * DiskTagHandlerFacet Tester.
 *
 * @author <Authors name>
 * @since <pre>10/09/2006</pre>
 * @version 1.0
 * @deprecated as {@link DiskTagHandlerFacet} is deprecated.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", "deprecation"})
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class
        DiskTagHandlerFacetTest extends TestCase {
    public static File folder = new File(System.getProperty("java.io.tmpdir"));
    public static String name = "dummy";
    private static String content = "Header\n" +
                             "0\tTag 0\n" +
                             "1\tTag 1\n" +
                             "2\tTag 2\n" +
                             "3\tTag 3\n" +
                             "4\tTag 4\n";

    public DiskTagHandlerFacetTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        createFiles();
    }

    public static void createFiles() throws Exception {
        Files.saveString(content, DiskTagHandlerFacet.getFile(folder, name));
    }

    public static void deleteFiles() throws Exception {
        DiskTagHandlerFacet.getFile(folder, name).delete();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        deleteFiles();
    }

    public void testGetTagName() throws Exception {
        DiskTagHandlerFacet facet = new DiskTagHandlerFacet(folder, name);
        assertEquals("First tag should be \"Tag 0\"", "Tag 0",
                     facet.getTagName(0));
        assertNull("Unknown id should give null", facet.getTagName(546));
        for (int i = 0 ; i < 4 ; i++) {
            assertEquals("Tag #" + i + " should be \"Tag " + i + "\"",
                         "Tag " + i, facet.getTagName(i));
        }
        facet.close();
    }

    public void testGetMaxTagCount() throws Exception {
        DiskTagHandlerFacet facet = new DiskTagHandlerFacet(folder, name);
        assertEquals("The number of tags should be 5", 5, facet.size());
    }

    public void testReload() throws Exception {
        DiskTagHandlerFacet facet = new DiskTagHandlerFacet(folder, name);
        assertEquals("First tag should be \"Tag 0\"", "Tag 0",
                     facet.getTagName(0));
        facet = new DiskTagHandlerFacet(folder, name);
        assertTrue("The index for the tag names should be loaded",
                   facet.indexLoadPerformed);
        assertEquals("The loaded index should contain 5 elements",
                     5, facet.size());
        for (int i = 0 ; i < 4 ; i++) {
            assertEquals("Tag #" + i + " should be \"Tag " + i + "\"",
                         "Tag " + i, facet.getTagName(i));
        }
        facet.close();
        new File(folder, facet.getIndexName(name)).delete(); // Bad style
    }

    public static Test suite() {
        return new TestSuite(DiskTagHandlerFacetTest.class);
    }
}
