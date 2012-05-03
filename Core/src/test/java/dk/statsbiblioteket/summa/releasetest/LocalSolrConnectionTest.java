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
package dk.statsbiblioteket.summa.releasetest;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.solr.SolrManipulator;
import dk.statsbiblioteket.summa.support.solr.SolrSearchNode;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;

/**
 * Test connection to a local Solr at port 8983 (default).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LocalSolrConnectionTest extends TestCase {
    private static Log log = LogFactory.getLog(LocalSolrConnectionTest.class);

    @Override
    public void setUp () throws Exception {
        ReleaseTestCommon.setup();
        // TODO: set up a local Solr instance
    }

    @Override
    public void tearDown() throws Exception {
        ReleaseTestCommon.tearDown();
        // TODO: Tear down the local Solr instance
    }

    // Only tests if ingest passes without error
    public void testBasicIngest() throws Exception {
        ObjectFilter data = getDataProvider(false);
        ObjectFilter indexer = getIndexer();
        indexer.setSource(data);
        assertTrue("There should be a next for the indexer", indexer.hasNext());
        indexer.next();
        assertFalse("After a single call to next() there should be no more Payloads", indexer.hasNext());
        indexer.close(true);
        log.debug("Finished basic ingest");
    }

    public void testBasicSearch() throws Exception {
        testBasicIngest();
        SearchNode searcher = getSearcher();
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(
            DocumentKeys.SEARCH_QUERY, "description:first",
            SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "id score name description"
        ), responses);
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                     1, ((DocumentResponse)responses.iterator().next()).getHitCount());

        String PHRASE = "Solr sample document";
        assertTrue("The result should contain the phrase '" + PHRASE + "'", responses.toXML().contains(PHRASE));
        searcher.close();
    }
    public void testFacetedSearch() throws Exception {
        testBasicIngest();
        SearchNode searcher = getSearcher();
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(
            DocumentKeys.SEARCH_QUERY, "description:first",
            SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "id score name description",
            DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
            FacetKeys.SEARCH_FACET_FACETS, "description"
        ), responses);
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                     1, ((DocumentResponse) responses.iterator().next()).getHitCount());
        assertTrue("The result should contain tag 'solr' with count 1",
                   responses.toXML().contains("<tag name=\"simple\" addedobjects=\"1\""));
//        System.out.println(responses.toXML());
    }
    public void testDelete() throws Exception {
        testBasicIngest();
        SearchNode searcher = getSearcher();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request(
            DocumentKeys.SEARCH_QUERY, "description:first",
            SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "id score name description"
        );
        searcher.search(request, responses);
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                     1, ((DocumentResponse)responses.iterator().next()).getHitCount());

        ObjectFilter data = getDataProvider(true);
        ObjectFilter indexer = getIndexer();
        indexer.setSource(data);
        indexer.next();
        indexer.close(true);

        responses.clear();
        searcher.search(request, responses);
        assertTrue("There should be a response after document delete", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits after delete. Response was\n" + responses.toXML(),
                     0, ((DocumentResponse)responses.iterator().next()).getHitCount());
        searcher.close();
    }

    private ObjectFilter getDataProvider(boolean deleted) throws IOException {
        String doc1 = Resolver.getUTF8Content("solr/SolrSampleDocument1.xml");
        Payload payload = new Payload(new Record("doc1", "dummy", doc1.getBytes("utf-8")));
        payload.getRecord().setDeleted(deleted);
        return new PayloadFeederHelper(Arrays.asList(payload));
    }

    private IndexController getIndexer() throws IOException {
        Configuration controllerConf = Configuration.newMemoryBased(
            IndexController.CONF_FILTER_NAME, "testcontroller");
        Configuration manipulatorConf = controllerConf.createSubConfigurations(
            IndexControllerImpl.CONF_MANIPULATORS, 1).get(0);
        manipulatorConf.set(IndexControllerImpl.CONF_MANIPULATOR_CLASS, SolrManipulator.class.getCanonicalName());
        manipulatorConf.set(SolrManipulator.CONF_ID_FIELD, "id"); // 'id' is the default ID field for Solr
        return new IndexControllerImpl(controllerConf);
    }

    private SearchNode getSearcher() throws RemoteException {
        return new SolrSearchNode(Configuration.newMemoryBased());
    }
}
