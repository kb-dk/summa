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
package dk.statsbiblioteket.summa.common.lucene;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.summa.index.lucene.LuceneManipulator;
import dk.statsbiblioteket.summa.index.lucene.StreamingDocumentCreator;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.support.lucene.search.LuceneSearchNode;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

/**
 * Tests whether indexing and searching on dynamic (prefix-based) fields works.
 */
public class DynamicFieldTest extends TestCase {
    public static final File DESCRIPTOR = Resolver.getFile("common/lucene/DynamicDescriptor.xml");
    public static final File INDEX = new File(System.getProperty("java.io.tmpdir"), "dynamictmp");

    public static final File DOC_SIMPLE = Resolver.getFile("common/lucene/SimpleDocument.xml");
    public static final File DOC_JOKER = Resolver.getFile("common/lucene/JokerDocument.xml");

    public DynamicFieldTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!INDEX.exists()) {
            if (!INDEX.mkdirs()) {
                throw new IllegalStateException("Unable to create folder " + INDEX);
            }
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (INDEX.exists()) {
            Files.delete(INDEX);
        }
    }

    public static Test suite() {
        return new TestSuite(DynamicFieldTest.class);
    }

    public void testPlainFlow() throws IOException {
        ObjectFilter feeder = new PayloadFeederHelper(0, DOC_SIMPLE.toString());
        ObjectFilter indexer = getIndexChain(feeder);
        assertTrue("There should be a single record", indexer.hasNext());
        Payload indexed = indexer.next();
        assertNotNull("The records should pass through the chain", indexed);
        indexed.close();
        indexer.close(true);

        assertEquals("The number of hits should be as expected", 1, getHitCount(new Request(
                DocumentSearcher.SEARCH_QUERY, "jens"
        )));
    }

    public void testJokerFlow() throws IOException {
        ObjectFilter feeder = new PayloadFeederHelper(0, DOC_JOKER.toString());
        ObjectFilter indexer = getIndexChain(feeder);
        assertTrue("There should be a single record", indexer.hasNext());
        Payload indexed = indexer.next();
        assertNotNull("The records should pass through the chain", indexed);
        indexed.close();
        indexer.close(true);

        assertEquals("The number of hits should be as expected for plain query", 1, getHitCount(new Request(
                DocumentSearcher.SEARCH_QUERY, "jens"
        )));
        assertEquals("The number of hits should be as expected for field based query", 1, getHitCount(new Request(
                DocumentSearcher.SEARCH_QUERY, "author:jens"
        )));
        assertEquals("The number of hits should be as expected for field based joker query", 1, getHitCount(new Request(
                DocumentSearcher.SEARCH_QUERY, "joker123:harley"
        )));
    }

    private long getHitCount(Request request) throws RemoteException {
        SearchNode searcher = getSearcher();
        try {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(request, responses);
            for (Response response: responses) {
                if (response instanceof DocumentResponse) {
                    DocumentResponse dr = (DocumentResponse)response;
                    return dr.getHitCount();
                }
            }
            throw new IllegalStateException("The responses for " + request + " should contain a DocumentResponse");
        } finally {
            searcher.close();
        }
    }

    private ObjectFilter getIndexChain(ObjectFilter feeder) throws IOException {
        Configuration sdcConf = Configuration.newMemoryBased();
        {
            Configuration idConf = sdcConf.createSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR);
            idConf.set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, DESCRIPTOR.toString());
        }
        ObjectFilter xmlToDoc = new StreamingDocumentCreator(sdcConf);
        xmlToDoc.setSource(feeder);

        Configuration icConf = Configuration.newMemoryBased(
                IndexControllerImpl.CONF_INDEX_ROOT_LOCATION, INDEX.toString(),
                IndexControllerImpl.CONF_CREATE_NEW_INDEX, true
        );
        {
            Configuration idConf = icConf.createSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR);
            idConf.set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, DESCRIPTOR.toString());
            Configuration manConf = icConf.createSubConfigurations(IndexControllerImpl.CONF_MANIPULATORS, 1).get(0);
            manConf.set(IndexManipulator.CONF_MANIPULATOR_CLASS, LuceneManipulator.class);
        }
        ObjectFilter indexer = new IndexControllerImpl(icConf);
        indexer.setSource(xmlToDoc);
        return indexer;
    }

    @SuppressWarnings("ConstantConditions")
    private SearchNode getSearcher() throws RemoteException {
        SearchNode searcher = new LuceneSearchNode(getSearcherConf());
        searcher.open(INDEX.listFiles()[0].toString());
        return searcher;
    }

    private Configuration getSearcherConf() {
        return Configuration.newMemoryBased(
                LuceneSearchNode.CONF_RESULT_FIELDS, "recordID, recordBase, shortformat, author, title"
        );
    }
}
