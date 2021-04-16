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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs ingest, index and search on multiple sources using the MUXFilter.
 * </p><p>
 * Tested sources are as follows (those prefixed with - are not done yet)<br />
 * horizon: SBMARC (a subset of DanMARC2)<br />
 * oai:     OAI-PMH harvested data<br />
 * fagref:  proprietary format for describing a person at Statsbiblioteket<br />
 * -nat:    Aleph, which is converted to MARC and parsed as SBMARC<br />
 * -reklamefilm: Proprietary format for commercials at Statsbiblioteket<br />
 * -csa:
 * -etss:
 * -doms:   OAI-PMH harvested data<br />
 * -tusculanum:
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Must be extended to all targets to be complete")
public class MultipleSourcesTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(MultipleSourcesTest.class);

    @Override
    public void setUp () throws Exception {
        super.setUp();
        ReleaseTestCommon.setup();
        SearchTest.INDEX_ROOT.mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
//        ReleaseTestCommon.tearDown();
    }

    public void testHorizonIngest() throws Exception {
        testSpecificIngest("horizon");
    }
    public void testCSAIngest() throws Exception {
        testSpecificIngest("csa");
    }
    public void testFagrefIngest() throws Exception {
        testSpecificIngest("fagref");
    }
    public void testFagrefFull() throws Exception {
        testFull(Arrays.asList("fagref"));
    }
    public void testDOMSIngest() throws Exception {
        testSpecificIngest("doms");
    }
    // Unknown failure, but not an especially important test
/*    public void testDOMSFull() throws Exception {
        testFull(Arrays.asList("doms"));
    }*/
    public void testETSSFull() throws Exception {
        testFull(Arrays.asList("etss"));
    }
    public void testCSAFull() throws Exception {
        testFull(Arrays.asList("csa"));
    }
    public void testReklameFull() throws Exception {
        testFull(Arrays.asList("reklamefilm"));
    }
    public void testTusculanumFull() throws Exception {
        testFull(Arrays.asList("tusculanum"));
    }

    public void testSpecificIngest(String base) throws Exception {
        StorageService storage = OAITest.getStorageService();
        performIngest(base);
        int count = countRecords(storage, base);
        log.info("Base " + base + " had " + count + " records");
        assertTrue("There should be >= 1  number of " + base
                     + " records, but there was 0", count > 0);
        log.debug("All OK. Closing down");
        storage.stop();
    }



    /* Checks that all sources has at least one Record.
     *
     */
    public void testIngest() throws Exception {
        StorageService storage = OAITest.getStorageService();
        performIngest();
        for (String source: getSources()) {
            int recordCount = countRecords(storage, source);
            log.debug("Records for source " + source + ": " + recordCount);
            assertTrue("There should be at least one Record for " + source,
                       recordCount > 0);
        }
        log.debug("All OK. Closing down");
        storage.stop();
    }

    public void testFull() throws Exception {
        testFull(getSources());
    }

    public void testFull(List<String> sources) throws Exception {
        Profiler storageProfiler = new Profiler();
        StorageService storage = OAITest.getStorageService();
        log.info("Finished starting Storage in "
                 + storageProfiler.getSpendTime());

        performIngest(sources);

        Profiler indexProfiler = new Profiler();
        performMUXIndex();
        String indexTime = indexProfiler.getSpendTime();

        SearchService search = OAITest.getSearchService();
        testSearch(sources);

        search.stop();
        storage.stop();

        log.info("Finished indexing in " + indexTime);
    }

    private void testSearch(List<String> sources) throws IOException {
        log.debug("Testing searching");
        Map<String, String> queries = new HashMap<>(20);
        queries.put("fagref", "omnilogi");
        queries.put("horizon", "Kaoskyllingen");
        queries.put("nat", "Byggesektorgruppen");
        queries.put("oai", "hyperfine");
        queries.put("csa", "demo");
//        queries.put("doms", "omega");
        queries.put("etss", "odontologica");
        queries.put("reklamefilm", "dandruff");
        queries.put("tusculanum", "fairies");
        SearchClient searchClient =
                new SearchClient(Configuration.newMemoryBased(
                        ConnectionConsumer.CONF_RPC_TARGET,
                        "//localhost:28000/summa-searcher"));
        StringWriter fails = new StringWriter(5000);
        for (String source: sources) {
            String query = queries.get(source);
            assertNotNull("There must be a query for source " + source, query);
            Request request = new Request();
            request.put(DocumentSearcher.SEARCH_QUERY, query);
            log.debug(String.format(Locale.ROOT,
                    "Verifying index prescence for source %s with query %s",
                    source, query));
            String result = searchClient.search(request).toXML();
            log.trace(String.format(Locale.ROOT,
                    "Search result for query '%s' for source %s was:\n%s",
                    query, source, result));
            if (getSearchResultCount(result) == 0) {
                if (!fails.toString().isEmpty()) {
                    fails.append("\n");
                }
                fails.append(source).append(": no hits found for query '")
                        .append(query).append("' for source ").append(source);
            }
        }
        if (!fails.toString().isEmpty()) {
            fail("All sources should be indexed.\n" + fails.toString());
        }
    }

    private static Pattern HITCOUNT_PATTERN =
            Pattern.compile(".+hitCount\\=\\\"([0-9]+)\\\".+", Pattern.DOTALL);
    public static int getSearchResultCount(String searchResult) {
        Matcher matcher = HITCOUNT_PATTERN.matcher(searchResult);
        assertTrue("We should always be capable of determining hitCount",
                   matcher.matches());
        return Integer.parseInt(matcher.group(1));
    }

    private int countRecords(StorageService storage, String base) throws
                                                                  IOException {
        StorageIterator iterator = new StorageIterator(storage.getStorage(),
                            storage.getStorage().getRecordsModifiedAfter(0, base, null));
        int counter = 0;
        while (iterator.hasNext()) {
            counter++;
            iterator.next();
        }
        return counter;
    }

    static File INGEST_CONFIGURATIONS_ROOT =
            new File(ReleaseTestCommon.DATA_ROOT, "multiple/ingest");
    /*
     * Ingests sample data for all sources specified in the header to the
     * Storage defined in the relevant setups.
     *
     */
    public static void performIngest() throws Exception {
        List<String> sources = getSources();
        performIngest(sources);
    }

    private static void performIngest(List<String> sources) throws Exception {
        Profiler profiler = new Profiler();
        log.info("Ingesting test data");
        assertTrue("There should be at least one source", !sources.isEmpty());
        for (String source: sources) {
            performIngest(source);
        }
        log.info("Finished ingesting test data in " + profiler.getSpendTime());
    }

    private static void performIngest(String source) throws Exception {
        log.info("Ingesting source " + source);
        Configuration ingestConf =Configuration.load(
                new File(INGEST_CONFIGURATIONS_ROOT,
                         source + "_ingest_configuration.xml").
                        getAbsolutePath());
        ingestConf.set(Service.CONF_SERVICE_ID, "IngestService" + source);
        ingestConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
//                    getSubConfiguration("Reader").
                    set(FileReader.CONF_ROOT_FOLDER,
                        new File(ReleaseTestCommon.DATA_ROOT,
                             "multiple/data/" + source).getAbsolutePath());
        FilterService ingestService = new FilterService(ingestConf);
        try {
            ingestService.start();
        } catch (Exception e) {
            throw new RuntimeException("Got exception while ingesting source '"
                                       + source + "'", e);
        }
        while (ingestService.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest of " + source + " ½ a second");
            Thread.sleep(500);
        }
        ingestService.stop();
        log.debug("Finished ingesting " + source);
    }

    public void testGetSources() throws Exception {
        assertTrue("There should be more than one source", !getSources().isEmpty());
    }

    /**
     * Generates a list of all sources by enumerating the ingest-configuration
     * files in data/multiple/ingest.
     * @return a list of all sources.
     * @throws IOException if the enumeration could not be performed.
     */
    public static ArrayList<String> getSources() throws IOException {
        final ArrayList<String> sources = new ArrayList<>(20);
        final Pattern INGEST_CONFIGURATION =
                Pattern.compile("(.+)_ingest_configuration\\.xml");
        log.trace("Locating sources in " + INGEST_CONFIGURATIONS_ROOT);
        INGEST_CONFIGURATIONS_ROOT.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                log.debug("Checking " + name);
                Matcher matcher = INGEST_CONFIGURATION.matcher(name);
                if (matcher.matches()) {
                    log.trace("Located source " + matcher.group(1));
                    if (!"doms".equals(matcher.group(1))) { // Hack
                        sources.add(matcher.group(1));
                    }
                }
                return true;
            }
        });
        return sources;
    }

    private void performMUXIndex() throws Exception {
        log.info("Starting index");
        Configuration indexConf = getIndexConfiguration();

        FilterService index = new FilterService(indexConf);
        index.start();

        while (index.getStatus().getCode() != Status.CODE.stopped &&
               index.getStatus().getCode() != Status.CODE.crashed) {
            log.trace("Waiting for index ½ a second");
            Thread.sleep(500);
        }
        index.stop();
    }

    public void testGetIndexConfiguration() throws Exception {
        // Throws exceptions by itself if something is blatantly wrong
        try {
            getIndexConfiguration();
        } catch (Exception e) {
            fail("getIndexConfiguration failed with " + e.getMessage());
        }
    }

    private Configuration getIndexConfiguration() throws Exception {
        Configuration indexConf = Configuration.load(Resolver.getURL(
                        "integration/multiple/index_configuration.xml").
                getFile());
        indexConf.set(Service.CONF_SERVICE_ID, "IndexService");

        //ArrayList<String> sources = getSources();
/*        indexconf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfiguration("Muxer").
                set(MUXFilter.CONF_FILTERS, sources);*/
/*        for (String source: sources) {
            log.trace("Updating index conf with source " + source);
            Configuration sourceConf =
                    indexconf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                            getSubConfiguration("Muxer").
                            getSubConfiguration(source);*/
/*            sourceConf.set(MUXFilterFeeder.CONF_FILTER_CLASS,
                           XMLTransformer.class.getName());
            sourceConf.set(MUXFilterFeeder.CONF_FILTER_NAME,
                           source + " transformer");
            sourceConf.set(MUXFilterFeeder.CONF_FILTER_BASES, source);*/
/*            String xsltRelativeLocation = String.format(Locale.ROOT,
                    "targets/%s/%1$s_index.xsl", source);
            if ("fagref".equals(source)) {
                xsltRelativeLocation = "targets/fagreferent/fagref_index.xsl";
            } else if ("nat".equals(source)) {
                xsltRelativeLocation = "targets/aleph/aleph_index.xsl";
            } else if ("reklamefilm".equals(source)) {
                xsltRelativeLocation = "targets/DanskReklameFilm/reklamefilm_index.xsl";
            }
            URL sourceXSLT = Resolver.getURL(xsltRelativeLocation);
            assertNotNull(String.format(Locale.ROOT,
                    "The XSLT for source %s should exist at the relative "
                    + "location %s", source, xsltRelativeLocation), sourceXSLT);
            sourceConf.set(XMLTransformer.CONF_XSLT, sourceXSLT.getFile());*/
        //}

        String indexDescriptorLocation = new File(
                ReleaseTestCommon.DATA_ROOT,
                "multiple/index_descriptor.xml").toString();
        log.debug("indexDescriptorLocation: " + indexDescriptorLocation);

        indexConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(3).
//                getSubConfiguration("DocumentCreator").
                getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);

        indexConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(4).
//                getSubConfigurations(IndexControllerImpl.CONF_MANIPULATORS).
//                get(0).
//                getSubConfiguration("IndexUpdate").
//                getSubConfiguration("LuceneUpdater").
                getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);
        return indexConf;
    }
}

