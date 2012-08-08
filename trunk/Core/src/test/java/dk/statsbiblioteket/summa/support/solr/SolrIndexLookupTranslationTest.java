/**  Licensed under the Apache License, Version 2.0 (the "License");
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
import dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.exposed.ExposedIndexLookupParams;

import java.io.IOException;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.solr.exposed.ExposedIndexLookupParams.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrIndexLookupTranslationTest extends SolrSearchTestBase {
    private static Log log = LogFactory.getLog(SolrIndexLookupTranslationTest.class);

    private static final String PRE = SolrSearchNode.CONF_SOLR_PARAM_PREFIX;

    public SolrIndexLookupTranslationTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SolrIndexLookupTranslationTest.class);
    }

    public void testDirectSolrIndexLookup() throws SolrServerException, IOException, InterruptedException {
        // Yes, elephant is stated twice. We want to check the count
        ingest(Arrays.asList("aardvark", "bison", "cougar", "deer", "elephant", "elephant", "fox", "giraffe", "horse"));
        assertDirect("recordBase:myBase", "ele",
                     "elookup={fields={lti={terms={deer=1,elephant=2,fox=1},origo=1}}}");
        assertDirect("*:*", "ele",
                     "elookup={fields={lti={terms={deer=1,elephant=2,fox=1},origo=1}}}");
        assertDirect("recordBase:myBase", "efe", // TODO: Shouldn't origo be -1?
                     "elookup={fields={lti={terms={deer=1,elephant=2,fox=1},origo=1}}}");
    }

    public void testDirectSolrIndexLookupCase() throws SolrServerException, IOException, InterruptedException {
        // Yes, elephant is stated twice. We want to check the count
        ingest(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H"));
        assertDirect("recordBase:myBase", "F",
                     "elookup={fields={lti={terms={e=1,f=1,g=1},origo=1}}}");
    }

    private void assertDirect(String queryStr, String term, String expected) throws SolrServerException {
        SolrQuery query = new SolrQuery(queryStr);
        query.set(ELOOKUP, Boolean.toString(true));
        query.set(ELOOKUP_DELTA, Integer.toString(-1));
        query.set(ELOOKUP_FIELD, "lti");
        query.set(ELOOKUP_LENGTH, Integer.toString(3));
        query.set(ELOOKUP_TERM, term);
        query.set(ELOOKUP_SORT, "index");
        query.setQueryType("/lookup");
        QueryResponse response = solrServer.query(query);
        String responseStr = response.getResponse().toString();
        assertTrue("The index lookup with query '" + queryStr + "' and term '" + term
                   + "' should be correct.\nExpected: " + expected + "\nGot: " + responseStr,
                   responseStr.contains(expected));
    }

    public void testIndexLookupWithSolrKeys() throws Exception {
        // Yes, elephant is stated twice. We want to check the count
        ingest(Arrays.asList("aardvark", "bison", "cougar", "deer", "elephant", "elephant", "fox", "giraffe", "horse"));

        SearchNode searcher = new SBSolrSearchNode(Configuration.newMemoryBased(
            SolrSearchNode.CONF_SOLR_RESTCALL, "/solr/lookup")); // TODO: Switch to flexible path
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(
            DocumentKeys.SEARCH_QUERY, "recordBase:myBase"
        ), responses);
        assertTrue("Sanity-check search should find doc2", responses.toXML().contains("doc2"));
        searcher.close();

        verifyLookup(new Request(
            DocumentKeys.SEARCH_QUERY, "recordBase:myBase",
            PRE + ELOOKUP, true,
            PRE + ELOOKUP_DELTA, -1,
            PRE + ELOOKUP_FIELD, "lti",
            PRE + ELOOKUP_LENGTH, 3,
            PRE + ELOOKUP_TERM, "ele",
            PRE + ELOOKUP_SORT, "index"
        ), "deer(1) elephant(2) fox(1)");
    }

    public void testIndexLookupEnd() throws Exception {
        ingest(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H"));
        verifyLookup(new Request(
            DocumentKeys.SEARCH_QUERY, "recordBase:myBase",
            //PRE + "qt", "/lookup", // Now added automatically
            IndexKeys.SEARCH_INDEX_DELTA, -1,
            IndexKeys.SEARCH_INDEX_FIELD, "lti",
            IndexKeys.SEARCH_INDEX_LENGTH, 4,
            IndexKeys.SEARCH_INDEX_TERM, "g"
        ), "f(1) g(1) h(1)", false);
    }

    public void testNoQuery() throws Exception {
        ingest(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H"));
        verifyLookup(new Request(
            IndexKeys.SEARCH_INDEX_DELTA, -1,
            IndexKeys.SEARCH_INDEX_FIELD, "lti",
            IndexKeys.SEARCH_INDEX_LENGTH, 3,
            IndexKeys.SEARCH_INDEX_TERM, "F"
        ), "e(1) f(1) g(1)", false);
    }

    public void testSubsetQuery() throws Exception {
        ingest(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H"));
        verifyLookup(new Request(
            DocumentKeys.SEARCH_QUERY, "lti:E",
            IndexKeys.SEARCH_INDEX_FIELD, "lti",
            IndexKeys.SEARCH_INDEX_MINCOUNT, 1
        ), "e(1)", false);
    }

    public void testCaseSensitive() throws Exception {
        ingest(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H"));
        verifyLookup(new Request(
            DocumentKeys.SEARCH_QUERY, "recordBase:myBase",
            //PRE + "qt", "/lookup", // Now added automatically
            IndexKeys.SEARCH_INDEX_DELTA, -1,
            IndexKeys.SEARCH_INDEX_FIELD, "lti",
            IndexKeys.SEARCH_INDEX_LENGTH, 3,
            IndexKeys.SEARCH_INDEX_TERM, "F"
        ), "e(1) f(1) g(1)", false);
    }

  public void testIndexLookupNoLocale() throws Exception {
      ingest(Arrays.asList("ægir", "åse", "ødis"));
      verifyLookup(new Request(
          DocumentKeys.SEARCH_QUERY, "recordBase:myBase",
          //PRE + "qt", "/lookup", // Now added automatically
          IndexKeys.SEARCH_INDEX_DELTA, -1,
          IndexKeys.SEARCH_INDEX_FIELD, "lti",
          IndexKeys.SEARCH_INDEX_LENGTH, 30,
          IndexKeys.SEARCH_INDEX_TERM, ""
      ), "åse(1) ægir(1) ødis(1)", false);
  }

    public void testIndexLookupLocaleSumma() throws Exception {
        ingest(Arrays.asList("ægir", "åse", "ødis"));
        verifyLookup(new Request(
            DocumentKeys.SEARCH_QUERY, "recordBase:myBase",
            //PRE + "qt", "/lookup", // Now added automatically
            IndexKeys.SEARCH_INDEX_SORT, IndexKeys.INDEX_SORTBYLOCALE,
            IndexKeys.SEARCH_INDEX_LOCALE, "da",
            IndexKeys.SEARCH_INDEX_DELTA, -1,
            IndexKeys.SEARCH_INDEX_FIELD, "lti",
            IndexKeys.SEARCH_INDEX_LENGTH, 30,
            IndexKeys.SEARCH_INDEX_TERM, ""
        ), "ægir(1) ødis(1) åse(1)", false);    // TODO: Oder matter so upgrade the testing!
    }

    public void testIndexLookupLocaleSolr() throws Exception {
        ingest(Arrays.asList("ægir", "åse", "ødis"));
        verifyLookup(new Request(
            DocumentKeys.SEARCH_QUERY, "recordBase:myBase",
            //PRE + "qt", "/lookup", // Now added automatically
            SolrSearchNode.CONF_SOLR_PARAM_PREFIX + ExposedIndexLookupParams.ELOOKUP_SORT,
            ExposedIndexLookupParams.ELOOKUP_SORT_BYLOCALE,
            SolrSearchNode.CONF_SOLR_PARAM_PREFIX + ExposedIndexLookupParams.ELOOKUP_SORT_LOCALE_VALUE, "da",
            IndexKeys.SEARCH_INDEX_DELTA, -1,
            IndexKeys.SEARCH_INDEX_FIELD, "lti",
            IndexKeys.SEARCH_INDEX_LENGTH, 30,
            IndexKeys.SEARCH_INDEX_TERM, ""
        ), "ægir(1) ødis(1) åse(1)", false);    // TODO: Oder matter so upgrade the testing!
    }

    // TODO: Test without query
    // TODO: Test origo
    public void testIndexLookupWithSummaKeys() throws Exception {
        // Yes, elephant is stated twice. We want to check the count
        ingest(Arrays.asList("aardvark", "bison", "cougar", "deer", "elephant", "elephant", "fox", "giraffe", "horse"));

        verifyLookup(new Request(
            DocumentKeys.SEARCH_QUERY, "recordBase:myBase",
            //PRE + "qt", "/lookup", // Now added automatically
            IndexKeys.SEARCH_INDEX_DELTA, -1,
            IndexKeys.SEARCH_INDEX_FIELD, "lti",
            IndexKeys.SEARCH_INDEX_LENGTH, 3,
            IndexKeys.SEARCH_INDEX_TERM, "ele" // Note: No sort
        ), "deer(1) elephant(2) fox(1)", false);
    }

    private void verifyLookup(Request request, String  expectedTerms) throws Exception {
        verifyLookup(request, expectedTerms, false);
    }
    private void verifyLookup(Request request, String  expectedTerms, boolean explicitRest) throws Exception {
        SearchNode searcher = explicitRest ?
                              new SBSolrSearchNode(Configuration.newMemoryBased(
                                  SBSolrSearchNode.CONF_SOLR_RESTCALL, "/solr/lookup"
                              )) :
                              getSearcher();
        ResponseCollection responses;
        try {
            responses = new ResponseCollection();
            searcher.search(request, responses);
        } finally {
            searcher.close();
        }
        // We need to wait a little bit for the Solr thread to shut down.
        Thread.sleep(500);

        String actual = getTerms(responses.toXML());
        assertEquals("The terms should be as expected in\n" + responses.toXML(),
                     expectedTerms, actual);
    }

    private String getTerms(String response) { // Beware: Not XML parsing at all
      final Pattern RESPONSE = Pattern.compile(
        "<indexresponse(.+?)</indexresponse>", Pattern.DOTALL);
      Matcher reMatch = RESPONSE.matcher(response);
      if (!reMatch.find()) {
        return "";
      }
      response = reMatch.group(1);
      //  <int name="å">1</int><int name="æble">1</int>...

      final Pattern TERMS = Pattern.compile(
        "<term count=\"([^\"]+)\">([^<]+)</term>");
      StringWriter result = new StringWriter();
      Matcher matcher = TERMS.matcher(response);
      boolean subsequent = false;
      while (matcher.find()) {
        if (subsequent) {
          result.append(" ");
        }
        subsequent = true;
        result.append(matcher.group(2));
        result.append("(").append(matcher.group(1)).append(")");
      }
      return result.toString();
    }

    private SearchNode getSearcher() throws RemoteException {
        return new SBSolrSearchNode(Configuration.newMemoryBased());
    }
}
