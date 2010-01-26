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

