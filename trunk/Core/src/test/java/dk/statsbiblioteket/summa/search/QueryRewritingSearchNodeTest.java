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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.summa.support.harmonise.hub.QueryReducer;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryRewritingSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(QueryRewritingSearchNodeTest.class);

    public QueryRewritingSearchNodeTest(String name) {
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
        return new TestSuite(QueryRewritingSearchNodeTest.class);
    }

    public void testIdentity() throws RemoteException {
        assertTransformations(Configuration.newMemoryBased(
                QueryRewritingSearchNode.CONF_PHRASE_QUERIES, false,
                QueryRewritingSearchNode.CONF_SANITIZE_QUERIES, false
        ), new String[][]{
            {"foo:\"bar zoo\"", "foo:\"bar zoo\""}
        });
    }

    public void testSpace() throws RemoteException {
        assertTransformations(Configuration.newMemoryBased(), new String[][]{
            {"foo", "\"foo\""},
            {"foo bar", "\"foo\" \"bar\""},
            {"foo NOT bar", "+\"foo\" -\"bar\""},
            {"foo OR bar", "\"foo\" OR \"bar\""},
            {"foo -bar", "+\"foo\" -\"bar\""}
        });
    }

    public void testColonWeight() throws RemoteException {
        assertTransformations(Configuration.newMemoryBased(
                QueryRewriter.CONF_TERSE, false,
                QueryRewritingSearchNode.CONF_PHRASE_QUERIES, false,
                QueryRewriter.CONF_QUOTE_TERMS, false
        ), new String[][]{
            {"foo:\"bar:zoo\"", "foo:bar\\:zoo"},
            {"foo:\"bar:zoo\"^2.5", "foo:bar\\:zoo^2.5"}
        });
    }

    public void testExplicit() throws RemoteException {
        assertTransformations(Configuration.newMemoryBased(
            QueryRewriter.CONF_TERSE, false
        ), new String[][]{
            {"foo", "\"foo\""},
            {"foo bar", "(+\"foo\" +\"bar\")"},
            {"foo NOT bar", "(+\"foo\" -\"bar\")"},
            {"foo OR bar", "(\"foo\" OR \"bar\")"},
            {"foo -bar", "(+\"foo\" -\"bar\")"}
        });
    }

    public void testReduceToNull() throws IOException {
        Configuration conf = getDefaultReducerConf(false);

        assertTransformations(conf, new String[][]{{
                "Language:abcde32542f",
                null
        }, {
                "recordBase:s*",
                null
        }, {
                "(recordBase:s*)",
                null
        }, {
                "(recordBase:s* OR Language:abcde32542f) AND recordBase:c*",
                null
        }});
    }

    public void testReduceToEmpty() throws IOException {
        Configuration conf = getDefaultReducerConf(true);

        assertTransformations(conf, new String[][]{{
                "Language:abcde32542f",
                ""
        }, {
                "recordBase:s*",
                ""
        }, {
                "(recordBase:s*)",
                ""
        }, {
                "(recordBase:s* OR Language:abcde32542f) AND recordBase:c*",
                ""
        }});
    }

    // No matter what, the empty Query should result in null
    public void testNonexistingToNull() throws IOException {
        assertTransformations(getDefaultReducerConf(true), new String[][]{{"", null}});
        assertTransformations(getDefaultReducerConf(false), new String[][]{{"", null}});
    }

    public void testComplexReduce() throws IOException {
        Configuration conf = getDefaultReducerConf(false);

        assertTransformations(conf, new String[][]{{
                "+(hello my:world phrase:\"mongo pongo\") +(something OR Language:abcde32542f)",
                "(hello my:world phrase:\"mongo pongo\") something"
        }});
    }

    private Configuration getDefaultReducerConf(boolean keepEmpty) throws IOException {
        Configuration conf = Configuration.newMemoryBased(
                QueryRewritingSearchNode.CONF_REDUCE, true,
                QueryRewriter.CONF_QUOTE_TERMS, false,
                QueryRewritingSearchNode.CONF_KEEP_EMPTY_QUERIES, keepEmpty,
                QueryRewritingSearchNode.CONF_KEEP_EMPTY_FILTERS, keepEmpty
        );
        Configuration reducerConf = conf.createSubConfigurations(QueryReducer.CONF_TARGETS, 1).get(0);
        reducerConf.set(QueryReducer.ReducerTarget.CONF_MATCH_NONES, new ArrayList<String>(Arrays.asList(
                "Language:abcde32542f",
                "recordBase:")));
        return conf;
    }


    public void assertTransformations(Configuration conf, String[][] tests) throws RemoteException {
        CollectingSearchNode inner = new CollectingSearchNode();
        QueryRewritingSearchNode rewriter = new QueryRewritingSearchNode(conf, inner);
        for (String[] test: tests) {
            final String RAW = test[0];
            final String EXPECTED = test[1];

            rewriter.search(new Request(DocumentKeys.SEARCH_QUERY, RAW), new ResponseCollection());
            String actual = inner.lastRequest.getString(DocumentKeys.SEARCH_QUERY, null);
            if (actual == null && inner.lastRequest.containsKey(DocumentKeys.SEARCH_QUERY)) {
                actual = "";
            }
            if (EXPECTED == null) {
                assertNull("The query should not be present in the request but was '" + actual
                           + "' for input '" + RAW + "'", actual);
            }
            assertEquals("The query '" + RAW + "' should be transformed correctly",
                         EXPECTED, actual);
        }
    }

    public static class CollectingSearchNode implements SearchNode {
        private static Log log = LogFactory.getLog(CollectingSearchNode.class);
        public Request lastRequest = null;
        @Override
        public void search(Request request, ResponseCollection responses) throws RemoteException {
            lastRequest = request;
            log.debug("search(" + request.toString(true) + ", " + responses.toXML() + ") called");
        }

        @Override
        public void warmup(String request) {
            log.debug("warmup(" + request + ") called");
        }

        @Override
        public void open(String location) throws RemoteException {
            log.debug("open(" + location + ") called");
        }

        @Override
        public void close() throws RemoteException {
            log.debug("close called");
        }

        @Override
        public int getFreeSlots() {
            log.debug("Returning free slots Integer.MAX_VALUE");
            return Integer.MAX_VALUE;
        }
    }
}

