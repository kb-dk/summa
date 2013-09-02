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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ManyToManyMapper;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.lucene.queryparser.classic.ParseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryAdjusterTest extends TestCase {

    private QueryAdjuster adjuster;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ManyToManyMapper fieldMap = new ManyToManyMapper(Arrays.asList("a - b", "c;d - e", "f - g;h", "i;j - k;l"));
        Map<String, ManyToManyMapper> termMaps = new HashMap<String, ManyToManyMapper>();
        ManyToManyMapper bghMap = new ManyToManyMapper(Arrays.asList(
                "ta - tb", "tc;td - te", "tf - tg;th", "ti;tj - tk;tl",
                "hard ware - HW"));
        termMaps.put("b",  bghMap);
        termMaps.put("g",  bghMap);
        termMaps.put("h",  bghMap);
        adjuster = new QueryAdjuster(Configuration.newMemoryBased(
                QueryRewriter.CONF_QUOTE_TERMS, false
        ), fieldMap, termMaps);
    }

    public void testTermQueryTermMapping() throws Exception {
        assertAdjustments(adjuster, new String[][] { // Expected, query
                {"b:tb", "b:ta"}, // ta - tb
                {"b:te", "b:tc"}, // c;d - e
                {"b:te", "b:td"}, // c;d - e
                {"b:tg OR b:th", "b:tf"}, // f - g;h
                {"b:tl OR b:tk", "b:ti"}, // i;j - k;l
                {"b:tl OR b:tk", "b:tj"} // i;j - k;l
        });
    }

    public void testTermQueryFieldMapping() throws Exception {
        assertAdjustments(adjuster, new String[][] { // Expected, query
                {"b:foo", "a:foo"}, // a - b
                {"e:foo", "c:foo"}, // c;d - e
                {"e:foo", "d:foo"}, // c;d - e
                {"g:foo OR h:foo", "f:foo"}, // f - g;h
                {"l:foo OR k:foo", "i:foo"}, // i;j - k;l
                {"l:foo OR k:foo", "j:foo"} // i;j - k;l
        });
    }

    public void testDualMapping() throws Exception {
        assertAdjustments(adjuster, new String[][] { // Expected, query
                {"g:tl OR g:tk OR h:tl OR h:tk", "f:ti"}, // f - g;h + ti;tj - tk;tl
        });
    }
    public void testNoFieldChange() throws Exception {
        assertAdjustments(adjuster, new String[][] { // Expected, query
                {"foo", "foo"},
                {"foo:bar", "foo:bar"},
                {"e:foo", "e:foo"} // c;d - e
        });
    }

    public void testFieldPhrase() throws Exception {
        assertAdjustments(adjuster, new String[][] { // Expected, query
                {"b:\"hello world\"", "a:\"hello world\""},
                {"l:\"hello world\" OR k:\"hello world\"", "j:\"hello world\""}
        });
    }

    public void testTermPhrase() throws Exception {
        assertAdjustments(adjuster, new String[][] { // Expected, query
                // We accept not collapsing the transformed phrase to term
                {"b:\"HW\"", "b:\"hard ware\""}
        });
    }

    public void testNestedField() throws Exception {
        assertAdjustments(adjuster, new String[][] { // Expected, query
                {"foo (g:foo OR h:foo)", "foo f:foo"}
        });
    }

    private void assertAdjustments(QueryAdjuster adjuster, String[][] tests) throws ParseException {
        for (String[] test: tests) {
            String expected = test[0];
            String query = test[1];
            assertEquals("The query '" + query + " ' should be transformed correctly",
                         expected, adjuster.rewrite(query));
        }
    }
}
