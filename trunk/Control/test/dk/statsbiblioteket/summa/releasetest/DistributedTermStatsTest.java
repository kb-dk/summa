/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.releasetest;

import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.dummy.SummaSearcherDummy;
import dk.statsbiblioteket.summa.search.rmi.RMISearcherProxy;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.MalformedURLException;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DistributedTermStatsTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(DistributedTermStatsTest.class);

    @Override
    public void setUp () throws Exception {
        super.setUp();
        if (INDEX_ROOT.exists()) {
            Files.delete(INDEX_ROOT);
        }
        INDEX_ROOT.mkdirs();
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final File INDEX_ROOT = new File(
            System.getProperty("java.io.tmpdir"), "testdist");
    public static final File DESCRIPTOR = Resolver.getFile(
            "data/distribution/index_descriptor.xml");

    public void testCreateIndex() throws Exception {
        List<Pair<String, List<Pair<String, String>>>> corpus =
        new ArrayList<Pair<String, List<Pair<String, String>>>>(1);

        corpus.add(new Pair<String, List<Pair<String, String>>>(
                "foo1", Arrays.asList(
                new Pair<String, String>("single_token", "bar"))));

        File indexLocation = createIndex(
                DESCRIPTOR, new File(INDEX_ROOT, "index_1"), corpus);
        log.info("Created index at " + indexLocation);
    }

    public void testSearcher() throws Exception {
        testCreateIndex();
        List<SummaSearcher> searchers =
                getSearchers(Arrays.asList(new File(INDEX_ROOT, "index_1")));
        assertEquals("A single searcher should be created",
                     1, searchers.size());
        SummaSearcher searcher = searchers.get(0);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "bar");
        ResponseCollection responses = searcher.search(request);
        String xmlResponse = responses.toXML();
        log.info("The result of a search for 'bar' was:\n" + xmlResponse);
    }

    /**
     * Create an aggregator for the given searchers.
     * @param searchers the searchers to aggregate.
     * @return an aggregator based on the given searchers.
     */
    public static SummaSearcher createAggregator(List<SummaSearcher> searchers){
        return null; // TODO: Implement this
    }

    /**
     * @param locations paths to index folders.
     * @return a list of searcher opened for the locations.
     * @throws IOException if a searcher could not be created.
     */
    public static List<SummaSearcher> getSearchers(
            List<File> locations) throws IOException {
        List<SummaSearcher> searchers = new ArrayList<SummaSearcher>(
                locations.size());
        Configuration conf = Configuration.load(
                "data/distribution/search_configuration.xml");
        int id = 0;
        for (File location: locations) {
            conf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT, location);
            conf.set(RMISearcherProxy.CONF_BACKEND,
                     SummaSearcherImpl.class.getCanonicalName());
            conf.set(RMISearcherProxy.CONF_SERVICE_NAME, "searcher_" + id);
            id++;
            searchers.add(new RMISearcherProxy(conf));
        }
        return searchers;
    }

    /**
     * Create a Summa index.
     * @param indexDescriptor the location of the IndexDescriptor.
     * @param destination     where to store the index.
     * @param corpus          List(id, (field, value)*)*.
     * @return the ultimate destination of the index.
     * @throws java.io.IOException if the index could not be created.
     */
    public static File createIndex(
            File indexDescriptor, File destination, 
            List<Pair<String, List<Pair<String, String>>>> corpus)
                                                            throws IOException {
        
        Configuration indexConf = Configuration.load(
                "data/distribution/index_configuration.xml");
        indexConf.set(IndexControllerImpl.CONF_INDEX_ROOT_LOCATION,
                      destination.getAbsolutePath());
        indexConf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptor.getAbsolutePath());

        IndexControllerImpl indexer = new IndexControllerImpl(indexConf);
        PayloadFeederHelper feeder = new PayloadFeederHelper(
                createPayloads(indexDescriptor, corpus));

        indexer.setSource(feeder);
        //noinspection StatementWithEmptyBody
        while (indexer.pump());
        indexer.close(true);
        return indexer.getIndexLocation();
    }

    // corpus: List(id, (field, value)*)*.
    private static List<Payload> createPayloads(
            File indexDescriptor,
            List<Pair<String, List<Pair<String, String>>>> corpus)
                                                            throws IOException {
        LuceneIndexDescriptor descriptor = new LuceneIndexDescriptor(
                indexDescriptor.toURI().toURL());
        List<Payload> payloads = new ArrayList<Payload>(corpus.size());
        for (Pair<String, List<Pair<String, String>>> recordDef: corpus) {
            Document document = new Document();
            for (Pair<String, String> content: recordDef.getValue()) {
                LuceneIndexField indexField = descriptor.getFieldForIndexing(
                        content.getKey());
                Field field = new Field(
                        content.getKey(), content.getValue(),
                        indexField.getStore(), indexField.getIndex(),
                        indexField.getTermVector());
                document.add(field);
            }
            Record record = new Record(recordDef.getKey(), "foo", new byte[0]);
            Payload payload = new Payload(record);
            payload.getData().put(Payload.LUCENE_DOCUMENT, document);
            IndexUtils.assignBasicProperties(payload);
            payloads.add(payload);
        }
        return payloads;
    }
}
