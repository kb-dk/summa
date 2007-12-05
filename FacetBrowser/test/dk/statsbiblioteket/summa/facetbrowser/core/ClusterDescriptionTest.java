/* $Id: ClusterDescriptionTest.java,v 1.8 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.8 $
 * $Date: 2007/10/04 13:28:17 $
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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;

/**
 * StructureDescription Tester.
 *
 * @author <Authors name>
 * @since <pre>11/02/2006</pre>
 * @version 1.0
 */
@SuppressWarnings({"deprecation"})
public class ClusterDescriptionTest extends TestCase {
    private StructureDescription cd;

    public ClusterDescriptionTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
//        ClusterCommon.getProperties().setProperty(StructureDescription.MAX_FACETS,
//                                                  Integer.toString(5));
        ClusterCommon.getProperties().setProperty(StructureDescription.DEFAULT_MAX_CLUSTER_TAGS,
                                                  Integer.toString(6));
        //noinspection DuplicateStringLiteralInspection
        ClusterCommon.getProperties().setProperty("default_maxClusterObjects",
                                                  Integer.toString(7));
        ClusterCommon.getProperties().setProperty(ClusterCommon.FACET_NAMES,
                                                  "a, b (8), c");
        cd = new StructureDescription();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetMaxFacets() throws Exception {
//        assertEquals("Max for facets should be 5", 5, cd.getMaxFacets());
    }

    public void testGetMaxTags() throws Exception {
        assertEquals("Max for a should be 6", 6, cd.getMaxTags("a"));
        assertEquals("Max for b should be 8", 8, cd.getMaxTags("b"));
        assertEquals("Max for c should be 6", 6, cd.getMaxTags("c"));
    }

    public void testGetMaxTags1() throws Exception {
        assertEquals("Max for 0 should be 6", 6, cd.getMaxTags(0));
        assertEquals("Max for 1 should be 8", 8, cd.getMaxTags(1));
        assertEquals("Max for 2 should be 6", 6, cd.getMaxTags(2));
    }

    public void testGetMaxTags2() throws Exception {
        assertEquals("Default max for tags should be 6", 6, cd.getMaxTags());
    }

/*    public void testGetMaxObjects() throws Exception {
        assertEquals("Default max for objects should be 7",
                     7, cd.getMaxObjects());
    }*/

    public void testGetFacetNames() throws Exception {
        String[] expected = new String[]{"a", "b", "c"};
        int counter = 0;
        for (String name: cd.getFacetNames()) {
            assertEquals("Facet #" + counter + " should be \"" +
                         expected[counter] + "\"",
                         expected[counter], name);
            counter++;
        }
    }

    public void testGetFacetName() throws Exception {
        assertEquals("Facet 0 should be \"a\"", "a", cd.getFacetName(0));
        assertEquals("Facet 1 should be \"b\"", "b", cd.getFacetName(1));
        assertEquals("Facet 2 should be \"c\"", "c", cd.getFacetName(2));
    }

    public void testGetFacetSortOrder() throws Exception {
        assertEquals("Facet \"a\" should have sort order 0",
                     0, cd.getFacetSortOrder("a"));
        assertEquals("Facet \"a\" should have sort order 1",
                     1, cd.getFacetSortOrder("b"));
        assertEquals("Facet \"a\" should have sort order 2",
                     2, cd.getFacetSortOrder("c"));
    }

    public static Test suite() {
        return new TestSuite(ClusterDescriptionTest.class);
    }
}
