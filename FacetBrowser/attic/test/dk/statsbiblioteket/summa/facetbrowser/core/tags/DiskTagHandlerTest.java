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

import java.util.List;
import java.util.LinkedList;
import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * DiskTagHandler Tester.
 *
 * @author <Authors name>
 * @since <pre>10/10/2006</pre>
 * @version 1.0
 * @deprecated as DiskTagHandler is deprecated.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", "deprecation"})
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class DiskTagHandlerTest extends TestCase {
    public DiskTagHandlerTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        DiskTagHandlerFacetTest.createFiles();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        //DiskTagHandlerFacetTest.deleteFiles();
    }

    public void testGetTagID() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetTagName() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetFacetID() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetFacetSize() throws Exception {
        List<String> facetNames = new LinkedList<String>();
        facetNames.add(DiskTagHandlerFacetTest.name);
        DiskTagHandler handler =
                new DiskTagHandler(DiskTagHandlerFacetTest.folder,
                                   new StructureDescription(facetNames));
        assertEquals("The maximum number of tags should be 5", 5, 
                     handler.getMaxTagCount());
    }

    public void testGetMaxTagCount() throws Exception {
        //TODO: Test goes here...
    }

    public void fullMap() throws Exception {
        List<String> facetNames = new LinkedList<String>();
        facetNames.add("ldbk");
        facetNames.add("linst");
        facetNames.add("llang");
        facetNames.add("llcc");
        DiskTagHandler handler =
                new DiskTagHandler(new File(new File(System.getProperty("java.io.tmpdir")),
                                            "completeClustermap").getAbsoluteFile(),
                                   new StructureDescription(facetNames));
        System.out.println("Max tags: " + handler.getMaxTagCount());
    }

    public void testGetFacetNames() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(DiskTagHandlerTest.class);
    }
}


