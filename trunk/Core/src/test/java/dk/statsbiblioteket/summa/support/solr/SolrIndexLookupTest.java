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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.support.embeddedsolr.EmbeddedJettyWithSolrServer;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.solr.exposed.ExposedIndexLookupParams.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrIndexLookupTest extends TestCase {
    private static Log log = LogFactory.getLog(SolrIndexLookupTest.class);

    public static final String SOLR_HOME = "support/solr_home1"; //data-dir (index) will be created here.
    private static final String PRE = SolrSearchNode.CONF_SOLR_PARAM_PREFIX;

    private EmbeddedJettyWithSolrServer server = null;
    private SolrServer solrServer;

    public SolrIndexLookupTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("basedir", ".");
        // TODO: Clear existing data
        server = new EmbeddedJettyWithSolrServer(SOLR_HOME);
        server.run();
        solrServer = new HttpSolrServer(server.getServerUrl());
        solrServer.deleteByQuery("*:*");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        server.stopSolr();
    }

    public static Test suite() {
        return new TestSuite(SolrIndexLookupTest.class);
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

        SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased(
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
            IndexKeys.SEARCH_INDEX_FIELD, "lti"
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

  public void testIndexLookupLocale() throws Exception {
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

    private void ingest(List<String> terms) throws IOException {
        ObjectFilter data = getDataProvider(terms);
        ObjectFilter indexer = getIndexer();
        indexer.setSource(data);
        //noinspection StatementWithEmptyBody
        while (indexer.pump());
        indexer.close(true);
    }

    private void verifyLookup(Request request, String  expectedTerms) throws Exception {
        verifyLookup(request, expectedTerms, true);
    }
    private void verifyLookup(Request request, String  expectedTerms, boolean explicitRest) throws Exception {
        SearchNode searcher = explicitRest ?
                              new SolrSearchNode(Configuration.newMemoryBased(
                                  SolrSearchNode.CONF_SOLR_RESTCALL, "/solr/lookup"
                              )) :
                              getSearcher();
        ResponseCollection responses = new ResponseCollection();
        searcher.search(request, responses);
        searcher.close();

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


    private ObjectFilter getDataProvider(List<String> terms) throws UnsupportedEncodingException {
        List<Payload> samples = new ArrayList<Payload>(terms.size());
        for (int i = 0 ; i < terms.size() ; i++) {
            samples.add(new Payload(new Record(
                "doc" + i, "dummy",
                ("<doc><field name=\"recordId\">doc" + i + "</field>\n"
                 + "   <field name=\"recordBase\">myBase</field>\n"
                 + "   <field name=\"lti\">" + terms.get(i) + "</field></doc>").getBytes("utf-8"))));
        }
        return new PayloadFeederHelper(samples);
    }

    private IndexController getIndexer() throws IOException {
        Configuration controllerConf = Configuration.newMemoryBased(
            IndexController.CONF_FILTER_NAME, "testcontroller");
        Configuration manipulatorConf = controllerConf.createSubConfigurations(
            IndexControllerImpl.CONF_MANIPULATORS, 1).get(0);
        manipulatorConf.set(IndexControllerImpl.CONF_MANIPULATOR_CLASS, SolrManipulator.class.getCanonicalName());
        manipulatorConf.set(SolrManipulator.CONF_ID_FIELD, "recordId"); // 'id' is the default ID field for Solr
        return new IndexControllerImpl(controllerConf);
    }

    private SearchNode getSearcher() throws RemoteException {
        return new SolrSearchNode(Configuration.newMemoryBased());
    }
}
