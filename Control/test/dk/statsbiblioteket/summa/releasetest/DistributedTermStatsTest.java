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
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexCommon;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermStatExtractor;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermStat;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermEntry;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.pool.SortedPool;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.SummaSearcherAggregator;
import dk.statsbiblioteket.summa.search.rmi.RMISearcherProxy;
import dk.statsbiblioteket.summa.support.lucene.search.LuceneSearchNode;
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
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.security.Policy;

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

    public void testDistributedTermStats() throws Exception {
        List<File> indexLocations = createThreeIndexes();
        File mergedLocation = extractStats(
                extendFiles(indexLocations, "lucene"));
        log.info("Merged stats at '" + mergedLocation + "'/currentdate");

        List<SummaSearcher> searchers = createSearchers(
                mergedLocation.getParentFile(),
                Arrays.asList(new File(INDEX_ROOT, "index_1"),
                              new File(INDEX_ROOT, "index_2"),
                              new File(INDEX_ROOT, "index_3")));
        SummaSearcher aggregator = createAggregator(searchers);
        assertResult("The aggregator should return results in expected order",
                     aggregator, "bar1 bar2",
                     Arrays.asList("foo3", "foo2", "foo1"));
        String oldResult = search(aggregator, "bar1 bar2");
        log.info("old search result:\n" + oldResult);
        aggregator.close();
        close(searchers);

        TermStat termStat = new TermStat(Configuration.newMemoryBased());
        termStat.open(mergedLocation, false);
        assertEquals("The count for bar2 should be correct",
                     4, termStat.getTermCount("multi_token:bar2"));
        updateCount(termStat, "multi_token:bar2", 10); // bar2 is now common
        assertEquals("The count for bar2 should be updated",
                     10, termStat.getTermCount("multi_token:bar2"));
        termStat.store();

        searchers = createSearchers(
                mergedLocation.getParentFile(),
                Arrays.asList(new File(INDEX_ROOT, "index_1"),
                              new File(INDEX_ROOT, "index_2"),
                              new File(INDEX_ROOT, "index_3")));
        aggregator = createAggregator(searchers);
        String newResult = search(aggregator, "bar1 bar2");
        assertFalse("The old result and the new result should differ",
                    oldResult.replaceAll(
                            "searchTime=\\\"[0-9]+\\\"", "").
                            equals(newResult.replaceAll(
                            "searchTime=\\\"[0-9]+\\\"", "")));
        assertResult("The new termstats should change the order",
                     aggregator, "bar1 bar2",
                     Arrays.asList("foo3", "foo1", "foo2"));
        aggregator.close();
        close(searchers);
    }

    private void updateCount(TermStat termStat, String term, int count)
                                                              throws Exception {
        Method method = TermStat.class.getDeclaredMethod("getTermCounts");
        method.setAccessible(true);
        Object result = method.invoke(termStat);
        //noinspection unchecked
        SortedPool<TermEntry> pool = (SortedPool<TermEntry>)result;
        TermEntry te = new TermEntry(term, count);
        pool.remove(pool.indexOf(te));
        pool.add(te);
    }

    private List<File> extendFiles(List<File> indexLocations, String subdir) {
        for (int i = 0 ; i < indexLocations.size() ; i++) {
            indexLocations.set(i, new File(indexLocations.get(i), subdir));
        }
        return indexLocations;
    }

    private File extractStats(List<File> indexLocations) throws IOException {
        Configuration conf = Configuration.newMemoryBased();
        TermStatExtractor extractor = new TermStatExtractor(conf);
        List<File> statLocations = new ArrayList<File>(indexLocations.size());
        for (File indexLocation: indexLocations) {
            File dumpLocation = new File(indexLocation.getParent(),
                                         TermStat.TERMSTAT_PERSISTENT_NAME);
            extractor.dumpStats(indexLocation, dumpLocation);
            statLocations.add(dumpLocation);
            log.debug(String.format("Extracted stats from '%s' to '%s'",
                                    indexLocation, dumpLocation));
        }
        File mergedRoot = new File(
                indexLocations.get(0).getParentFile().
                        getParentFile().getParentFile(),
                TermStat.TERMSTAT_PERSISTENT_NAME);
        File concreteMerge = new File(mergedRoot, IndexCommon.getTimestamp());
        extractor.mergeStats(statLocations, concreteMerge);

        // Version
        Files.saveString(Long.toString(System.currentTimeMillis()),
                         new File(concreteMerge, IndexCommon.VERSION_FILE));

        log.debug(String.format("Merged %d stat sources into '%s'",
                                statLocations.size(), concreteMerge));
        return concreteMerge;
    }

    public void testBasicDistribution() throws Exception {
        createThreeIndexes();
        List<SummaSearcher> searchers = createSearchers(
                Arrays.asList(new File(INDEX_ROOT, "index_1"),
                              new File(INDEX_ROOT, "index_2"),
                              new File(INDEX_ROOT, "index_3")));
        SummaSearcher aggregator = createAggregator(searchers);
        log.info("Starting search-test");
        for (int i = 0 ; i < 3 ; i++) {
            int searchID = 0;
            for (SummaSearcher searcher: searchers) {
                log.info(String.format(
                        "Search with searcher %d for 'bar%d' gave the "
                        + "result:\n%s",
                        searchID++, (i+1), search(searcher, "bar" + (i+1))));
            }
            log.info(String.format(
                    "Aggregated searching for 'bar%d' gave the result:\n%s",
                    (i+1), search(aggregator, "bar" + (i+1))));
        }

        assertResult("The aggregator should return results in expected order",
                     aggregator, "bar1", Arrays.asList("foo3", "foo1", "foo2"));
        assertResult("The aggregator should return results in expected order "
                     + "for multi search",
                     aggregator, "bar1 bar2 bar3",
                     Arrays.asList("foo3", "foo2", "foo1"));
        aggregator.close();
        close(searchers);
    }

    private List<File> createThreeIndexes() throws IOException {
        List<Pair<String, List<Pair<String, String>>>> corpus =
        new ArrayList<Pair<String, List<Pair<String, String>>>>(10);
        ArrayList<File> locations = new ArrayList<File>(corpus.size());

        corpus.add(new Pair<String, List<Pair<String, String>>>(
                "foo1", Arrays.asList(
                new Pair<String, String>("multi_token", "bar1 bar1 bar1"),
                new Pair<String, String>("multi_token", "bar2"),
                new Pair<String, String>("multi_token", "bar3"))));
        locations.add(createIndex(DESCRIPTOR, new File(INDEX_ROOT, "index_1"), 
                                  corpus));

        corpus.clear();
        corpus.add(new Pair<String, List<Pair<String, String>>>(
                "foo2", Arrays.asList(
                new Pair<String, String>("multi_token", "bar1"),
                new Pair<String, String>("multi_token", "bar2 bar2"),
                new Pair<String, String>("multi_token", "bar3"))));
        locations.add(createIndex(DESCRIPTOR, new File(INDEX_ROOT, "index_2"),
                                  corpus));

        corpus.clear();
        corpus.add(new Pair<String, List<Pair<String, String>>>(
                "foo3", Arrays.asList(
                new Pair<String, String>("multi_token", "bar1"),
                new Pair<String, String>("multi_token", "bar2"),
                new Pair<String, String>("multi_token", "bar3 bar3 bar3"))));
        corpus.add(new Pair<String, List<Pair<String, String>>>(
                "skewer_bar2", Arrays.asList( // Makes bar2 more common
                new Pair<String, String>("multi_token", "bar2"))));
        for (int i = 0 ; i < 100 ; i++) { // Let's create some documents
            corpus.add(new Pair<String, List<Pair<String, String>>>(
                    "filler" + i, Arrays.asList(
                    new Pair<String, String>("multi_token", "pingo"))));
        }
        locations.add(createIndex(DESCRIPTOR, new File(INDEX_ROOT, "index_3"),
                                  corpus));
        return locations;
    }

    /* Internal testing of the helpers for this release test below */

    public void testCreateAggregator() throws Exception {
        testCreateIndex();
        List<SummaSearcher> searchers =
                createSearchers(Arrays.asList(new File(INDEX_ROOT, "index_1")));
        SummaSearcher aggregator = createAggregator(searchers);
        log.info("Aggregated search for 'bar' resulted in:\n"
                 + search(aggregator, "bar"));
        assertTrue("The result should contain a field with 'bar'",
                   search(aggregator, "bar").contains(
                           "<field name=\"single_token\">bar</field>"));
        close(searchers);
    }

    private void close(List<SummaSearcher> searchers) throws IOException {
        for (SummaSearcher searcher: searchers) {
            searcher.close();
        }
    }

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
                createSearchers(Arrays.asList(new File(INDEX_ROOT, "index_1")));
        assertEquals("A single searcher should be created",
                     1, searchers.size());
        SummaSearcher searcher = searchers.get(0);
        String xmlResponse = search(searcher, "bar");
        log.info("The result of a search for 'bar' was:\n" + xmlResponse);
        assertTrue("The result should contain a field with 'bar'",
                   xmlResponse.contains(
                           "<field name=\"single_token\">bar</field>"));
        close(searchers);
    }

    /* Helpers for this release test */

    /**
     * Performs a search for query and checks that the result contains the
     * given recordIDs in the given order.
     * @param message   the message to display if the test fails.
     * @param searcher  the SummaSearcher to use.
     * @param query     the query for the searcher.
     * @param recordIDs the IDs to look for.
     * @throws java.io.IOException if the search could not be performed.
     */
    public static  void assertResult(String message, SummaSearcher searcher,
                                     String query, List<String> recordIDs)
                                                            throws IOException {
        String result = search(searcher, query);
        log.debug(String.format("The result for query '%s' was:\n%s",
                                query, result));
        // <field name="recordID">foo2</field>
        int lastPos = -1;
        String lastID = null;
        for (String recordID: recordIDs) {
            int pos = result.indexOf(String.format(
                    "<field name=\"recordID\">%s</field>", recordID));
            if (pos == -1) {
                fail(String.format(
                        "%s. The id '%s' could not be located in the result for"
                        + " query '%s'. The result was:\n%s",
                        message, recordID, query, result));
            }
            if (pos < lastPos) {
                fail(String.format(
                        "%s. The id '%s' was found at position %d, which is "
                        + "less than position %d from the previous id '%s' in "
                        + "query '%s'. The result was:\n%s",
                        message, recordID, pos, lastPos, lastID, query,
                        result));
            }
            lastPos = pos;
            lastID = recordID;
        }
    }

    private static String search(SummaSearcher searcher, String query)
                                                            throws IOException {
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, query);
        return searcher.search(request).toXML();
    }

    /**
     * Create an aggregator for the given searchers.
     * @param searchers the searchers to aggregate. This is quite a hack as
     *                  the searchers are assumed to be RMI-exposed on
     *                  {@code //localhost:28000/" + "searcher_#"} where # goes
     *                  from 0 to searchers.size()-1.
     * @return an aggregator based on the given searchers.
     * @throws java.io.IOException if the aggregator could not be created.
     * @see {@link #createSearchers}.
     */
    private static SummaSearcher createAggregator(List<SummaSearcher> searchers)
                                                            throws IOException {
        Configuration conf = Configuration.newMemoryBased();
        List<Configuration> connections = conf.createSubConfigurations(
                SummaSearcherAggregator.CONF_SEARCHERS, searchers.size());
        for (int shardNumber = 0 ; shardNumber < searchers.size() ;
             shardNumber++){
            String address = "//localhost:28000/" + "searcher_" + shardNumber;
            log.debug("Connecting aggregator to '" + address + "'");
            connections.get(shardNumber).set(
                    SearchClient.CONF_RPC_TARGET, address);
        }
        return new SummaSearcherAggregator(conf);
    }

    public static List<SummaSearcher> createSearchers(List<File> locations)
            throws IOException {
        return createSearchers(null, locations);
    }

    /**
     * Create searchers for the given indexes. The searchers will have the names
     * "searcher_#", where # goes from 0 to locations.size()-1.
     * @param termStatLocation the location of the term stats. This can be null
     *        in which case distributes term stats will not be used.
     * @param locations paths to index folders.
     * @return a list of searcher opened for the locations.
     * @throws IOException if a searcher could not be created.
     */
    public static List<SummaSearcher> createSearchers(File termStatLocation,
            List<File> locations) throws IOException {
        List<SummaSearcher> searchers = new ArrayList<SummaSearcher>(
                locations.size());
        Configuration conf = Configuration.load(
                "data/distribution/search_configuration.xml");
        if (termStatLocation != null) {
            conf.getSubConfiguration(
                    LuceneSearchNode.CONF_TERMSTAT_CONFIGURATION).set(
                    IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT, 
                    termStatLocation.getAbsolutePath());
        }
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
