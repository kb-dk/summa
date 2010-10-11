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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.index.lucene.LuceneManipulator;
import dk.statsbiblioteket.summa.index.lucene.StreamingDocumentCreator;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.support.lucene.search.LuceneSearchNode;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.rmi.RemoteException;

/**
 * Tests that the same analyzers are used in the indexing and searching phases.
 * This of course depends on the IndexDescriptor, that allows for different
 * analyzers to be used in the different steps, but for now we just want to test
 * the case where they should be the same.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AnalyzerTest extends TestCase {
    private static Log log = LogFactory.getLog(AnalyzerTest.class);

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private File ROOT = new File(System.getProperty("java.io.tmpdir"),
                                 "lucene-index");

    @Override
    protected void setUp() throws Exception {
        if (ROOT.exists()) {
            Files.delete(ROOT);
        }
        ROOT.mkdirs();
    }

    public void testIndexAnalyzer() throws Exception {
        createIndex();
    }

    private void createIndex() throws Exception {
        log.debug("Creating index");
        Configuration creatorConf = Configuration.load(
                "data/analyzer/DocumentCreatorConfiguration.xml");
        StreamingDocumentCreator creator =
                new StreamingDocumentCreator(creatorConf);
        creator.setSource(new PayloadFeederHelper(getPayloads()));
        List<Payload> processed = extractPayloads(creator);

        // We've got Lucene Documents, ready for indexing

        Configuration luceneConf = Configuration.load(
                "data/analyzer/LuceneConfiguration.xml");
        LuceneManipulator lucene = new LuceneManipulator(luceneConf);
        lucene.open(ROOT);

        for (Payload payload: processed) {
            log.debug("Adding " + payload + " to index");
            lucene.update(payload);
            payload.close();
        }
        log.debug("Consolidating");
        lucene.consolidate();
        lucene.close();
        log.debug("Index created");
    }

    public void testSearch() throws Exception {
        createIndex();
        LuceneSearchNode search = getLuceneSearchNode();
        assertHits(search, "split:\"token1 token2 token3\"");
        assertHits(search, "t*");
        assertHits(search, "token1");
        assertHits(search, "split:token1");
        assertHits(search, "split:token1 split:token2 split:token3");
        assertNoHits(search, "author:Direct");
        assertNoHits(search, "author:Directo");
        assertNoHits(search, "author:\"Direct field\"");
        assertHits(search, "author:\"Direct  field\"");
        search.close();
    }

    private void assertHits(LuceneSearchNode search, String query) throws
                                                               RemoteException {
        String result = search(search, query);
        assertTrue("The search for '" + query + "' should give 1+ hits",
                   result.contains("recordID\""));
    }

    private void assertNoHits(LuceneSearchNode search, String query) throws
                                                               RemoteException {
        String result = search(search, query);
        assertFalse("The search for '" + query + "' should give 0 hits. "
                    + "Result was\n" + result,
                    result.contains("recordID\""));
    }

    private String search(LuceneSearchNode node, String query) throws
                                                               RemoteException {
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        request.put(DocumentKeys.SEARCH_QUERY, query);
        node.search(request, responses);
        assertEquals("There should be a response for '" + query + "'",
                     1, responses.size());
        return responses.toXML();
    }

    private LuceneSearchNode getLuceneSearchNode() throws IOException {
        Configuration searchConf = Configuration.load(
                "data/analyzer/SearchConfiguration.xml");
        LuceneSearchNode sn = new LuceneSearchNode(searchConf);
        sn.open(ROOT.toString());
        assertTrue("There should be at least one document indexed",
                   sn.getDocCount() > 0);
        return sn;
    }

    private List<Payload> getPayloads() throws IOException {
        String[] DOCS = new String[]{"document1.xml"};
        List<Payload> payloads = new ArrayList<Payload>(DOCS.length);
        for (String doc: DOCS) {
            String content = Resolver.getUTF8Content("data/analyzer/" + doc);
            Record record = new Record(
                    doc, "dummyBase", content.getBytes("utf-8"));
            Payload payload = new Payload(record);
            payloads.add(payload);
        }
        return payloads;
    }

    private List<Payload> extractPayloads(ObjectFilter source) {
        List<Payload> processed = new ArrayList<Payload>();
        while(source.hasNext()) {
            processed.add(source.next());
        }
        return processed;
    }

}

