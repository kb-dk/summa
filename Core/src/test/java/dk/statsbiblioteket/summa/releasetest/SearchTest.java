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
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexException;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.Security;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.index.lucene.LuceneManipulator;
import dk.statsbiblioteket.summa.index.lucene.StreamingDocumentCreator;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.SummaSearcherFactory;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.filter.RecordWriter;
import dk.statsbiblioteket.summa.support.lucene.search.LuceneSearchNode;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The purpose of this class is to test the workflow
 * "files => ingest-chain => storage => index-chain => index = search".
 * It relies on the modules Common, Ingest, Index, Storage and Search.
 * </p><p>
 * IMPORTANT: Due to problems with releasing JDBC, the tests cannot be run
 * in succession, but must be started one at a time in their own JVM.
 */
// TODO: Fix shutdown problem, so that all unit-tests can run sequentially
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(SearchTest.class);

    public final static String fagref_hj =
        Resolver.getFile("integration/search/input/part1").getAbsolutePath();
    public final static String fagref_jh_gm =
        Resolver.getFile("integration/search/input/part2").getAbsolutePath();
    public final static String fagref_1plus2 =
        Resolver.getFile("integration/search/input/part1plus2").getAbsolutePath();
    public final static String fagref_clone =
        Resolver.getFile("integration/search/input/partClone").getAbsolutePath();

    @Override
    public void setUp () throws Exception {
        super.setUp();
        Security.checkSecurityManager();
        cleanup();
        if (!INDEX_ROOT.mkdirs()) {
            throw new IOException("Unable to create folder " + INDEX_ROOT);
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        cleanup();
    }

    private void cleanup() throws Exception {
        ReleaseHelper.cleanup();
        if (INDEX_ROOT.exists()) {
            Files.delete(INDEX_ROOT);
        }
        ReleaseHelper.cleanup();
    }

    public static File root = new File(Resolver.getURL(
        "integration/search/SearchTest_IngestConfiguration.xml").getFile()).getParentFile();
    public static String BASE = "fagref";

    public void testResourceCopying() {
        assertTrue("Source '" + fagref_hj + "' should exist", Resolver.getFile(fagref_hj).exists());
    }

    /* ingest the data in the given folder to Storage, assuming that Storage
     * is running. */
/*    public static void ingest(String storage, File folder) throws Exception {
        Configuration conf = Configuration.load(
                "search/SearchTest_IngestConfiguration.xml");
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
//                getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER, folder.getAbsolutePath());

        FilterService ingester = new FilterService(conf);
        ingester.start();
        IndexTest.waitForService(ingester);
        ingester.stop();
    }*/

    public static void update(Record record) throws Exception {
        Payload payload = new Payload(record);
        PayloadFeederHelper feeder =
            new PayloadFeederHelper(Arrays.asList(payload));
                                                        
        Configuration conf = Configuration.load("integration/search/SearchTest_IngestConfiguration.xml");
        Configuration writerConf =
            conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(2);
//                getSubConfiguration("Writer").
        RecordWriter writer = new RecordWriter(writerConf);

        writer.setSource(feeder);
        writer.pump();
        writer.close(true);
    }

    public static void update(String storageID, Record record) throws Exception {
        Payload payload = new Payload(record);
        PayloadFeederHelper feeder =
            new PayloadFeederHelper(Arrays.asList(payload));

        Configuration conf = Configuration.load("integration/search/SearchTest_IngestConfiguration.xml");
        Configuration writerConf =
            conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(2); // RecordWriter

//                getSubConfiguration("Writer").
        writerConf.set(ConnectionConsumer.CONF_RPC_TARGET, "//localhost:28000/" + storageID);
        RecordWriter writer = new RecordWriter(writerConf);

        writer.setSource(feeder);
        writer.pump();
        writer.close(true);
    }

    public void testIngest() throws Exception {
        final String STORAGE = "search_ingest_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        ingestFagref(STORAGE, fagref_hj);
        verifyStorage(STORAGE, "hj", "fagref:hj@example.com");
        ingestFagref(STORAGE, fagref_jh_gm);
        verifyStorage(STORAGE, "jh", "fagref:jh@example.com");
        storage.close();
    }

    @SuppressWarnings("ConstantConditions")
    public void testSortValue() throws Exception {
        final int AMOUNT = 1000;
        final String INDEX = INDEX_ROOT.toString();

        createFagrefIndex(INDEX, AMOUNT);
        SummaSearcher searcher = new SummaSearcherImpl(getSearcherConfiguration());
        verifySearch(searcher, "*:*", AMOUNT);

        DocumentResponse docs = getRecords(searcher, new Request(
            DocumentKeys.SEARCH_QUERY, "*:*",
            DocumentKeys.SEARCH_MAX_RECORDS, Math.min(AMOUNT, 10000),
            DocumentKeys.SEARCH_SORTKEY, "sort_title"
            ));
        List<String> sortValues = extractSortValue(docs);
        List<String> resorted = new ArrayList<String>(sortValues);
        Collections.sort(resorted, Collator.getInstance(new Locale("da")));
        ExtraAsserts.assertEquals(
            "The explicit sorted values should match order of the directly returned\n" + Strings.join(sortValues, "\n"),
            resorted, sortValues);

        File[] luceneFiles = new File(INDEX).listFiles()[0].listFiles()[0].listFiles();
        assertTrue("The number of files in the Lucene index folder should exceed 20 but was " + luceneFiles.length,
                   luceneFiles.length > 20);
    }

    public void testPaging() throws Exception {
        final int AMOUNT = 100;
        final int PAGE_SIZE = 20;
        final String SORT_FIELD = "sort_verify";
        final String SORT_VERIFY = "sort_verify";
        final String INDEX = INDEX_ROOT.toString();

        createFagrefIndex(INDEX, AMOUNT);
        SummaSearcher searcher = new SummaSearcherImpl(getSearcherConfiguration());
        verifySearch(searcher, "*:*", AMOUNT);

        {
            DocumentResponse docs = getRecords(searcher, new Request(
                    DocumentKeys.SEARCH_QUERY, "*:*",
                    DocumentKeys.SEARCH_SORTKEY, SORT_FIELD,
                    DocumentKeys.SEARCH_MAX_RECORDS, PAGE_SIZE,
                    DocumentKeys.SEARCH_RESULT_FIELDS, SORT_VERIFY
            ));
            assertPaging("Result for no start index value", docs, 0, PAGE_SIZE);
        }
        {
            DocumentResponse docs = getRecords(searcher, new Request(
                    DocumentKeys.SEARCH_QUERY, "*:*",
                    DocumentKeys.SEARCH_SORTKEY, SORT_FIELD,
                    DocumentKeys.SEARCH_START_INDEX, 0,
                    DocumentKeys.SEARCH_MAX_RECORDS, PAGE_SIZE,
                    DocumentKeys.SEARCH_RESULT_FIELDS, SORT_VERIFY
            ));
            assertPaging("Result for start index 0", docs, 0, PAGE_SIZE);
        }
        {
            DocumentResponse docs = getRecords(searcher, new Request(
                    DocumentKeys.SEARCH_QUERY, "*:*",
                    DocumentKeys.SEARCH_SORTKEY, SORT_FIELD,
                    DocumentKeys.SEARCH_START_INDEX, PAGE_SIZE,
                    DocumentKeys.SEARCH_MAX_RECORDS, PAGE_SIZE,
                    DocumentKeys.SEARCH_RESULT_FIELDS, SORT_VERIFY
            ));
            assertPaging("Result for start index " + PAGE_SIZE, docs, PAGE_SIZE, PAGE_SIZE);
        }
        {
            DocumentResponse docs = getRecords(searcher, new Request(
                    DocumentKeys.SEARCH_QUERY, "*:*",
                    DocumentKeys.SEARCH_SORTKEY, SORT_FIELD,
                    DocumentKeys.SEARCH_START_INDEX, PAGE_SIZE*2,
                    DocumentKeys.SEARCH_MAX_RECORDS, PAGE_SIZE,
                    DocumentKeys.SEARCH_RESULT_FIELDS, SORT_VERIFY
            ));
            assertPaging("Result for start index " + PAGE_SIZE, docs, PAGE_SIZE*2, PAGE_SIZE);
        }
        searcher.close();
    }

    private static void assertPaging(String message, DocumentResponse docs, int start, int pageSize) {
        List<String> expected = new ArrayList<String>(pageSize);
        for (int page = start ; page < start + pageSize ; page++) {
            expected.add("Jensen Hans " + String.format("%07d", page));
        }
        assertEquals(message, Strings.join(expected), Strings.join(getContent(docs, "sort_verify")));
    }

    private static List<String> getContent(DocumentResponse docs, String fieldName) {
        List<String> terms = new ArrayList<String>();
        for (DocumentResponse.Record records: docs.getRecords()) {
            for (DocumentResponse.Field field: records.getFields()) {
                if (fieldName.equals(field.getName())) {
                    terms.add(field.getContent());
                }
            }
        }
        return terms;
    }

    public void testConcurrent() throws Exception {
        final int AMOUNT = 1000;
        final int MAX_CONCURRENT = 10;
        final int THREADS = 10;
        final int HAMMER_RUNS = 20;
        final Random random = new Random(87);
        final String INDEX = INDEX_ROOT.toString();

        createFagrefIndex(INDEX, AMOUNT);
        Configuration conf = getSearcherConfiguration();
        conf.set(LuceneSearchNode.CONF_NUMBER_OF_CONCURRENT_SEARCHES, MAX_CONCURRENT);
        final SummaSearcherImpl searcher = new SummaSearcherImpl(conf);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future> tasks = new ArrayList<Future>(THREADS);
        log.info("Starting " + THREADS + " search threads");
        for (int t = 0 ; t < THREADS ; t++) {
            FutureTask task = new FutureTask(new Callable() {
                @Override
                public Object call() throws Exception {
                    for (int h = 0 ; h < HAMMER_RUNS ; h++) {
                        ResponseCollection responses = searcher.search(new Request(
                                DocumentKeys.SEARCH_QUERY, "*:*",
                                DocumentKeys.SEARCH_COLLECT_DOCIDS, true
                        ));
                        assertTrue("There should be at least 1 response", !responses.isEmpty());
                        if (h < HAMMER_RUNS-1) {
                            Thread.sleep(random.nextInt(10));
                        }
                    }
                    return null;
                }
            });
            tasks.add(task);
            executor.submit(task);
        }
        log.info("Waiting for search threads to finish");
        int counter = 0;
        for (Future task: tasks) {
            int concurrent = searcher.getConcurrentSearches();
            log.debug("Waiting for task " + counter++ +". Concurrent searchers: " + concurrent);
            task.get();
        }
        log.info("Finished with maximum concurrent searches " + searcher.getMaxConcurrent()
                 + " with a theoretical max of " + MAX_CONCURRENT);
        assertTrue("The maximum concurrent searchers should be > 1, but was " + searcher.getMaxConcurrent(),
                   searcher.getMaxConcurrent() > 1);
    }

    public void testSearcherReload() throws Exception {
        assertEquals("There should be 1 file indexed",
                     1, index(0, new File(fagref_hj, "hans.jensen.xml").toString()));
        SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
        verifySearch(searcher, "*:*", 1);
        assertEquals("There should be 1 extra file indexed",
                     1, index(1, new File(fagref_1plus2, "gurli.margrethe.xml").toString()));
        log.info("Reloading index...");
        searcher.reloadIndex();
        log.info("Index reloaded, performing test search...");
        verifySearch(searcher, "*:*", 2);
        log.info("Reload test done");
    }

    private int index(int startID, String... files) throws IOException {
        ObjectFilter feeder = new PayloadFeederHelper(startID, files);
        ObjectFilter indexer = createIndexChain(
            feeder, INDEX_ROOT.toString(), "integration/search/fagref_xslt/fagref_index_boost.xsl", 1, false);
        int counter = 0;
        while (indexer.hasNext()) {
            indexer.next();
            counter++;
        }
        indexer.close(true);
        return counter;
    }

    @SuppressWarnings("ConstantConditions")
    public void testBoosting() throws Exception {
        final int AMOUNT = 1;

        createFagrefIndex(INDEX_ROOT.toString(), AMOUNT, "integration/search/fagref_xslt/fagref_index_boost.xsl");
        SummaSearcher searcher = new SummaSearcherImpl(getSearcherConfiguration());
        verifySearch(searcher, "*:*", AMOUNT);
    }

    // Temporary test to check sorting on the production index
    public void disabled_testSortProductionIndex() throws Exception {
        final File INDEX = new File("/home/te/tmp/remote/sb/");
        Profiler profiler = new Profiler();
//        final File INDEX = new File("/home/te/tmp/sumfresh/sites/sb/index/sb/");

        @SuppressWarnings("ConstantConditions") // INDEX/sb/20121108-1234/IndexDescriptor.xml
        File descriptorLocation = new File(INDEX.listFiles()[0], "IndexDescriptor.xml");
        assertTrue("The descriptor should exist", descriptorLocation.exists());

        Configuration searcherConf = Configuration.load("integration/search/SearchTest_SearchConfiguration.xml");
        assertNotNull("The configuration should not be empty", searcherConf);
        searcherConf.set(SummaSearcherImpl.CONF_SEARCHER_AVAILABILITY_TIMEOUT, 99999999); // It takes time to open index
        searcherConf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).set(
            IndexDescriptor.CONF_ABSOLUTE_LOCATION, descriptorLocation);
        searcherConf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT, INDEX.toString());

        log.info("Opening index");
        SummaSearcherImpl searcher = new SummaSearcherImpl(searcherConf);
        log.info("Performing availability test search");
        assertTrue("There should be responses from the test search",
                   getHits(searcher, new Request(
                       DocumentKeys.SEARCH_QUERY, "foo",
                       DocumentKeys.SEARCH_COLLECT_DOCIDS, false // Disable faceting
                   )) > 0);

        log.debug("Performing real test search");
        System.out.println(searcher.search(new Request(
            DocumentKeys.SEARCH_QUERY, "knausgaard",
            DocumentKeys.SEARCH_FILTER, "lma_long:01",
            DocumentKeys.SEARCH_SORTKEY, "sort_title",
            DocumentKeys.SEARCH_COLLECT_DOCIDS, false // Disable faceting
        )).toXML());
        log.info("Finished test in " + profiler.getSpendTime());
    }

    private List<String> extractSortValue(DocumentResponse docs) {
        List<String> sortValues = new ArrayList<String>(docs.size());
        for (DocumentResponse.Record record: docs.getRecords()) {
            assertNotNull("There should be a sortValue for record " + record.getId(), record.getSortValue());
            sortValues.add(record.getSortValue());
        }
        return sortValues;
    }

    private DocumentResponse getRecords(SummaSearcher searcher, Request request) throws IOException {
        ResponseCollection responses = searcher.search(request);
        return (DocumentResponse) responses.iterator().next();
    }

    private void createFagrefIndex(String indexLocation, int amount) throws IOException {
        createFagrefIndex(indexLocation, amount, "integration/search/fagref_xslt/fagref_index.xsl");
    }
    private void createFagrefIndex(String indexLocation, int amount, String xslt) throws IOException {
        ObjectFilter producer = getSortDocuments(amount);

        ObjectFilter manipulator = createIndexChain(producer, indexLocation, xslt, amount, true);

        log.info("Generating index with " + amount + " sample documents");
        final Profiler profiler = new Profiler(amount, 10000);
        for (int i = 0 ; i < amount ; i++) {
            assertTrue("There should be a next Payload for request #" + (i + 1), manipulator.hasNext());
            manipulator.next();
            profiler.beat();
            if (i % 10000 == 0 || i == amount-1) {
                log.info("Indexed " + i + " documents at rate " + (int)profiler.getBps(true)
                         + " Payloads/second. ETS: " + profiler.getETAAsString(true));
            }
        }
        assertFalse("The chain should be depleted", manipulator.hasNext());
        manipulator.close(true);
    }

    // Custom producer
    private ObjectFilter createIndexChain(
            ObjectFilter producer, String indexLocation, String xslt, int amount, boolean isNew) throws IOException {
        ObjectFilter transformer = new XMLTransformer(Configuration.newMemoryBased(
            XMLTransformer.CONF_XSLT, Resolver.getFile(xslt)
        ));
        transformer.setSource(producer);

        ObjectFilter legacy = new XMLTransformer(Configuration.newMemoryBased(
            XMLTransformer.CONF_XSLT, Resolver.getFile("LegacyToSummaDocumentXML.xslt")
        ));
        legacy.setSource(transformer);

        Configuration docConf = Configuration.newMemoryBased();
        Configuration descConf = docConf.createSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR);
        descConf.set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, "integration/search/SearchTest_IndexDescriptor.xml");
        ObjectFilter documentCreator = new StreamingDocumentCreator(docConf);
        documentCreator.setSource(legacy);

        Configuration indexConf = Configuration.newMemoryBased(
            IndexControllerImpl.CONF_CREATE_NEW_INDEX, isNew,
            IndexControllerImpl.CONF_INDEX_ROOT_LOCATION, indexLocation,
            IndexControllerImpl.CONF_COMMIT_MAX_DOCUMENTS, amount < 10 ? 2 : amount / 5
        );
        Configuration luceneConf = indexConf.createSubConfigurations(IndexControllerImpl.CONF_MANIPULATORS, 1).get(0);
        luceneConf.set(IndexControllerImpl.CONF_MANIPULATOR_CLASS, LuceneManipulator.class);
        Configuration luceneDescConf = luceneConf.createSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR);
        luceneDescConf.set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, "integration/search/SearchTest_IndexDescriptor.xml");
        ObjectFilter manipulator = new IndexControllerImpl(indexConf);
        manipulator.setSource(documentCreator);
        return manipulator;
    }

    private ObjectFilter getSortDocuments(final int amount) throws UnsupportedEncodingException {
        final Random random = new Random();

        return new ObjectFilter() {
            private int delivered = 0;
            private StringBuilder sb = new StringBuilder(1000);
            @Override
            public boolean hasNext() {
                return delivered < amount;
            }

            @Override
            public void setSource(Filter filter) { }

            @Override
            public boolean pump() throws IOException {
                if (hasNext()) {
                    next();
                }
                return hasNext();
            }

            @Override
            public void close(boolean success) { }

            @Override
            public Payload next() {
                String id = "fagref:hj@example.com" + delivered;
                String sortValue = String.format("Jensen Hans %07d", delivered);
                sb.setLength(0);

                sb.append("<fagref>\n");
                sb.append("    <navn>Hans Jensen</navn>\n");
                sb.append("    <navn_sort>").append(sortValue).append("</navn_sort>\n");
                sb.append("    <sort_verify>").append(sortValue).append("</sort_verify>\n");
                sb.append("    <stilling>Fagekspert i Datalogi</stilling>\n");
                sb.append("    <titel>IT-").append(getRandomWord(random)).append("</titel>\n");
                sb.append("    <email>hj@example.com").append(id).append("</email>\n");
                sb.append("    <emneLink>http://www.example.com/emneguide/science/fysik/</emneLink>\n");
                sb.append("    <beskrivelse>\n");
                sb.append("        Ansvarlig for at vejlede indenfor alle datalogi-relaterede områder,\n");
                sb.append("        såsom programmering, hardware og pizzaspisning.\n");
                sb.append("        Er specielt vidende om sprogene Javakaffe og Pythonslanger.\n");
                sb.append("    </beskrivelse>\n");
                sb.append("    <emner>\n");
                sb.append("        <emne>Coca Cola</emne>\n");
                sb.append("        <emne>Pizza</emne>\n");
                sb.append("    </emner>\n");
                sb.append("</fagref>\n");
                delivered++;
                try {
                    return new Payload(new Record(id, "fagref", sb.toString().getBytes("utf-8")));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("UTF-8 should be supported", e);
                }
            }

            @Override
            public void remove() { }
        };
    }

    private String getRandomWord(Random random) {
        int length = random.nextInt(10)+1;
        String result = "";
        for (int i = 0 ; i < length ; i++) {
            result += (char)(random.nextInt(25)+97);
        }
        switch (random.nextInt(3)) {
            case 0: result += "æ"; break;
            case 1: result += "ø"; break;
            case 2: result += "å"; break;
            default: throw new IllegalStateException("Only æ, ø or å should be selected");
        }
        return result;
    }


    private static void verifyStorage(
        String storage, String feedback, String recordID) throws IOException {
        assertNotNull("A Record for " + feedback + " should be present",
                      ReleaseHelper.getRecord(storage, recordID));
    }

    public static void ingestFagref(String storage, String source) {
        ReleaseHelper.ingest(storage, source, "fagref", "fagref:", "fagref", "email");
    }

    public final static File INDEX_ROOT = new File(System.getProperty("java.io.tmpdir"), "testindex");

    private SummaSearcher createSearchService() throws Exception {
        return SummaSearcherFactory.createSearcher(getSearcherConfiguration());
    }

    private SummaSearcher createSearcher() throws Exception {
        return new SummaSearcherImpl(getSearcherConfiguration());
    }

    private Configuration getSearcherConfiguration() throws Exception {
        URL descriptorLocation = Resolver.getURL("integration/search/SearchTest_IndexDescriptor.xml");
        assertNotNull("The descriptor location should not be null", descriptorLocation);

        Configuration searcherConf = Configuration.load("integration/search/SearchTest_SearchConfiguration.xml");
        assertNotNull("The configuration should not be empty", searcherConf);
        searcherConf.set(LuceneSearchNode.CONF_FSDIRECTORY, LuceneSearchNode.FS_MMAP);
        searcherConf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    descriptorLocation.getFile());
        searcherConf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT,
                         INDEX_ROOT.toString());
        return searcherConf;
    }

    public void testSearcher() throws Exception {
        SummaSearcher searcher = createSearcher();
        try {
            searcher.search(simpleRequest("hans"));
            fail("A RemoteException should be thrown after timeout as no index data are present yet");
        } catch (RemoteException e) {
            // Expected
        }
    }

    public static Request simpleRequest(String query) {
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, query);
        return request;
    }

    public void NonfunctioningYettestSearchService() throws Exception {
        SummaSearcher searcher = createSearchService();
        try {
            searcher.search(simpleRequest("hans"));
            fail("An IndexException should be thrown as no index data are present yet");
        } catch (IndexException e) {
            // Expected
        }
/*        JMXServiceURL url = new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi://:2783/dk.statsbiblioteket.summa."
                + "control.service.SearchService:type=Service");     // 2783
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
*/
        Thread.sleep(Integer.MAX_VALUE);
    }

/*    private static class ClientListener implements NotificationListener {
            public void handleNotification(Notification notification,
                                           Object handback) {
                log.debug("\nReceived notification:");
                log.debug("\tClassName: " + notification.getClass().getName());
                log.debug("\tSource: " + notification.getSource());
                log.debug("\tType: " + notification.getType());
                log.debug("\tMessage: " + notification.getMessage());
                if (notification instanceof AttributeChangeNotification) {
                    AttributeChangeNotification acn =
                        (AttributeChangeNotification) notification;
                    log.debug("\tAttributeName: " + acn.getAttributeName());
                    log.debug("\tAttributeType: " + acn.getAttributeType());
                    log.debug("\tNewValue: " + acn.getNewValue());
                    log.debug("\tOldValue: " + acn.getOldValue());
                }
            }
        }

  */
    public static void updateIndex(String storageID) throws Exception {
        Configuration indexConf = IndexTest.loadFagrefProperties(
            storageID, "integration/search/SearchTest_IndexConfiguration.xml");
        IndexTest.updateIndex(indexConf);
    }

    public static void verifySearch(SummaSearcher searcher, String query,
                              int results) throws Exception {
        log.debug("Verifying existence of " + results + " documents with query '" + query + "'");
        assertEquals("The result for query '" + query + "' should match the expected count",
                     results, getHits(searcher, query));
    }

    private static Pattern hitPattern =
            Pattern.compile(".*hitCount\\=\\\"([0-9]+)\\\".*", Pattern.DOTALL);
    public static int getHits(SummaSearcher searcher, String query) throws Exception {
        return getHits(searcher, simpleRequest(query));
    }

    public static int getHits(SummaSearcher searcher, Request request) throws Exception {
        String result = searcher.search(request).toXML();
        Matcher matcher = hitPattern.matcher(result);
        if (!matcher.matches()) {
            throw new NullPointerException("Could not locate hitCount in " + result);
        }
        return Integer.parseInt(matcher.group(1));
    }

    public static int getHits(Response response) throws Exception {
        String result = response.toXML();
        Matcher matcher = hitPattern.matcher(result);
        if (!matcher.matches()) {
            throw new NullPointerException("Could not locate hitcount in " + result);
        }
        return Integer.parseInt(matcher.group(1));
    }

    public void testHitPattern() throws Exception {
        String TEST = " searchTime=\"42\" hitCount=\"3\">\n";
        Matcher matcher = hitPattern.matcher(TEST);
        assertTrue("hitPattern should match", matcher.matches());
        assertEquals("hitPattern should match 3", "3", matcher.group(1));
    }


/*
    We need to create an index here!
    
    public void testFullSearcher() throws Exception {
        testFullSearcher(createSearcher());
        log.debug("Calling close on Storage");
    }*/

    // Set up searcher, check for null
     // Set up storage
     // Exec index w/ update on storage
     // Perform small ingest
     // Exec index w/ update on storage, verify result in searcher
     // Perform ingest 2
     // Exec index w/ update on storage, verify result in searcher
     // Delete record in storage
     // Exec index w/ update on storage, verify result in searcher

    // TODO: The test fails sometimes, probably a race-condition. Fix it!
    public static void testFullSearcher(SummaSearcher searcher) throws Exception {
        final String STORAGE = "search_full_storage";

        log.debug("testFullSearcher started. Performing search");
        try {
            searcher.search(simpleRequest("hans"));
            fail("A RemoteException should be thrown after timeout as no index data are present yet");
        } catch (RemoteException e) {
            // Expected
        }
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        log.debug("Storage started");
        updateIndex(STORAGE);
        log.debug("updateIndex called");
//        Thread.sleep(3000); // Wait for searcher to discover new content
        try {
            searcher.search(simpleRequest("dummy"));
            fail("A search with empty index should fail");
        } catch (Exception e) {
            // Expected
        }
        ingestFagref(STORAGE, Resolver.getURL(fagref_hj).getFile());
        verifyStorage(STORAGE, "hj", "fagref:hj@example.com");
        updateIndex(STORAGE);
        log.debug("Finished updating of index. It should now contain 1 doc");
        Thread.sleep(5000); // Wait for searcher to discover new content
        try {
            verifySearch(searcher, "Hans", 1);
        } catch (IndexException e) {
            fail("Failed search 1 for Hans: " + e.getMessage());
        }
        try {
            verifySearch(searcher, "Gurli", 0);
        } catch (IndexException e) {
            fail("Failed search 1 for Gurli: " + e.getMessage());
        }
        log.debug("Adding new material");
        ingestFagref(STORAGE, fagref_jh_gm);
        verifyStorage(STORAGE, "hj", "fagref:hj@example.com");
        updateIndex(STORAGE);
        log.debug("Finished updating of index. It should now contain 3 docs");
        Thread.sleep(5000); // Wait for searcher to discover new content
        try {
            verifySearch(searcher, "Gurli", 1);
        } catch (IndexException e) {
            fail("Failed search 2 for Gurli: " + e.getMessage());
        }
        try {
            verifySearch(searcher, "Hans", 1);
        } catch (IndexException e) {
            fail("Failed search 2 for Hans: " + e.getMessage());
        }
        storage.close();
    }
}
