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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.support.api.DidYouMeanKeys;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SBSolrDidYouMeanTranslationTest extends SolrSearchTestBase {
    private static Log log = LogFactory.getLog(SBSolrDidYouMeanTranslationTest.class);

    private static final String PRE = SolrSearchNode.CONF_SOLR_PARAM_PREFIX;

    public SBSolrDidYouMeanTranslationTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SBSolrDidYouMeanTranslationTest.class);
    }

    public void testDirectSolrDYM() throws SolrServerException, IOException, InterruptedException {
        ingest(Arrays.asList("Thomas Egense", "Toke Eskildsen", "Tore Eskilsen", "Tomas Hansen"));
        QueryResponse response = assertDirectDIM("gense", "[egense, hansen]");
//        System.out.println(response.toString().replace("}", "}\n"));
    }

    // TODO: Add test for explicit DYM query
    // TODO: Add check for multiple terms
    public void testSolrResponseTranslation() throws SolrServerException, IOException, InterruptedException {
        ingest(Arrays.asList("Thomas Egense", "Toke Eskildsen", "Tore Eskilsen", "Tomas Hansen"));

        Request request = new Request(
            PRE + "q", "gense",
            PRE + "qt", "/didyoumean",
            PRE + "spellcheck", Boolean.toString(true),
            PRE + "spellcheck.dictionary", "summa_spell",
            PRE + "spellcheck.count", Integer.toString(5)
        );

        String expected = "<didyoumean score=\"1.0\">egense</didyoumean>";
        assertSummaDYM(request, expected);
        //System.out.println(responses.toXML().replace(">", ">\n"));
    }

    public void testSummaRequestParameters() throws SolrServerException, IOException, InterruptedException {
        ingest(Arrays.asList("Thomas Egense", "Toke Eskildsen", "Tore Eskilsen", "Tomas Hansen"));

        Request request = new Request(
            DidYouMeanKeys.SEARCH_QUERY, "gense"
        );
        String expected = "<didyoumean score=\"1.0\">egense</didyoumean>";
        assertSummaDYM(request, expected);
        //System.out.println(responses.toXML().replace(">", ">\n"));
    }

    private void assertSummaDYM(Request request, String expected) throws RemoteException {
        SearchNode searcher = new SBSolrSearchNode(Configuration.newMemoryBased(
//                                  SBSolrSearchNode.CONF_SOLR_RESTCALL, "/solr/didyoumean"
                              ));
        ResponseCollection responses = new ResponseCollection();
        searcher.search(request, responses);
        searcher.close();

        assertTrue("The response should contain "+ expected + "\n" + responses.toXML().replace(">", ">\n"),
                   responses.toXML().contains(expected));
    }

    private QueryResponse assertDirectDIM(String query, String expected) throws SolrServerException {
        SolrQuery sQuery = new SolrQuery(query);
        sQuery.set("qt", "/didyoumean");
        sQuery.set("spellcheck", Boolean.toString(true));
        sQuery.set("spellcheck.dictionary", "summa_spell");
        sQuery.set("spellcheck.count", Integer.toString(5));
        QueryResponse response = solrServer.query(sQuery);
        assertTrue("The suggestions should be as expected\n" + response, response.toString().contains(expected));
        return response;
    }


}
