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
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;

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
            {"foo:\"bar zoo\"", "foo:bar\\ zoo"}
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

    public void assertTransformations(Configuration conf, String[][] tests) throws RemoteException {
        CollectingSearchNode inner = new CollectingSearchNode();
        QueryRewritingSearchNode rewriter = new QueryRewritingSearchNode(conf, inner);
        for (String[] test: tests) {
            rewriter.search(new Request(DocumentKeys.SEARCH_QUERY, test[0]), new ResponseCollection());
            assertEquals("The query '" + test[0] + "' should be transformed correctly",
                         test[1], inner.lastRequest.getString(DocumentKeys.SEARCH_QUERY));
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

