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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.FacetIndexDescriptor;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.util.Strings;

import java.util.List;
import java.util.Map;
import java.io.StringWriter;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class FacetRequestTest extends TestCase {
    public FacetRequestTest(String name) {
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

    public void testTrivialParse() throws Exception {
        Structure structure = new Structure(Resolver.getURL(
                "data/TestIndexDescriptor.xml"));
        assertEquals("The facets should be as expected",
                     "a, foo", Strings.join(structure.getFacetNames(), ", "));
        FacetRequest request = new FacetRequest(
                new DocIDCollector(), "a", structure);
        assertEquals("Only a single facet should be requested",
                     1, request.getFacetNames().size());
    }

    public void testAdvancedParse() throws Exception {
        Structure structure = new Structure(Resolver.getURL(
                "data/TestIndexDescriptor.xml"));
        // input, expected
        String[][] TESTS = new String[][]{
                {"a", "a (15 POPULARITY)"},
                {"a(2),foo(9 POPULARITY)",
                        "a (2 POPULARITY), foo (9 POPULARITY)"},
                {"a(3), foo(9 POPULARITY)",
                        "a (3 POPULARITY), foo (9 POPULARITY)"},
                {"a(4 ALPHA),  foo(9 POPULARITY)",
                        "a (4 ALPHA), foo (9 POPULARITY)"},
                {"  a( 5  ALPHA ),  foo(9 POPULARITY) ",
                        "a (5 ALPHA), foo (9 POPULARITY)"},
                {"a(ALPHA),  foo(7 POPULARITY)",
                        "a (15 ALPHA), foo (7 POPULARITY)"},
                {"a  (6),foo(9 POPULARITY)",
                        "a (6 POPULARITY), foo (9 POPULARITY)"}
        };
        for (String[] test: TESTS) {
            FacetRequest request = new FacetRequest(
                    new DocIDCollector(), test[0], structure);
            assertEquals("The request '" + test[0] + " should be parsable",
                         test[1], toString(request));
        }
    }

    private String toString(FacetRequest fr) {
        StringWriter sw = new StringWriter(1000);
        boolean first = true;
        for (Map.Entry<String, FacetStructure> entry:
                fr.getFacets().entrySet()) {
            FacetStructure facet = entry.getValue();
            if (first) {
                first = false;
            } else {
                sw.append(", ");
            }
            sw.append(facet.getName()).append(" (");
            sw.append(Integer.toString(facet.getWantedTags())).append(" ");
            sw.append(facet.getSortType()).append(")");
        }
        return sw.toString();
    }

    public static Test suite() {
        return new TestSuite(FacetRequestTest.class);
    }
}
