/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.filter.object.MUXFilter;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

    public void setUp () throws Exception {
        super.setUp();
        ReleaseTestCommon.setup();
        SearchTest.INDEX_ROOT.mkdirs();
    }

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
    public void testDOMSFull() throws Exception {
        testFull(Arrays.asList("doms"));
    }
    public void testETSSFull() throws Exception {
        testFull(Arrays.asList("etss"));
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
        Map<String, String> queries = new HashMap<String, String>(20);
        queries.put("fagref", "omnilogi");
        queries.put("horizon", "Kaoskyllingen");
        queries.put("nat", "Byggesektorgruppen");
        queries.put("oai", "hyperfine");
        queries.put("csa", "demo");
        queries.put("doms", "omega");
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
            log.debug(String.format(
                    "Verifying index prescence for source %s with query %s",
                    source, query));
            String result = searchClient.search(request).toXML();
            log.trace(String.format(
                    "Search result for query '%s' for source %s was:\n%s",
                    query, source, result));
            if (getSearchResultCount(result) == 0) {
                if (fails.toString().length() != 0) {
                    fails.append("\n");
                }
                fails.append(source).append(": no hits found for query '")
                        .append(query).append("' for source ").append(source);
            }
        }
        if (fails.toString().length() != 0) {
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
    public static void performIngest() throws IOException,
                                              InterruptedException {
        List<String> sources = getSources();
        performIngest(sources);
    }

    private static void performIngest(List<String> sources) throws IOException,
                                                          InterruptedException {
        Profiler profiler = new Profiler();
        log.info("Ingesting test data");
        assertTrue("There should be at least one source", sources.size() > 0);
        for (String source: sources) {
            performIngest(source);
        }
        log.info("Finished ingesting test data in " + profiler.getSpendTime());
    }

    private static void performIngest(String source) throws IOException,
                                                          InterruptedException {
        log.info("Ingesting source " + source);
        Configuration ingestConf =Configuration.load(
                new File(INGEST_CONFIGURATIONS_ROOT,
                         source + "_ingest_configuration.xml").
                        getAbsolutePath());
        ingestConf.set(Service.CONF_SERVICE_ID, "IngestService" + source);
        ingestConf.getSubConfiguration("SingleChain").
                    getSubConfiguration("Reader").
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
        assertTrue("There should be more than one source", 
                   getSources().size() > 0);
    }

    /**
     * Generates a list of all sources by enumerating the ingest-configuration
     * files in data/multiple/ingest.
     * @return a list of all sources.
     * @throws IOException if the enumeration could not be performed.
     */
    public static ArrayList<String> getSources() throws IOException {
        final ArrayList<String> sources = new ArrayList<String>(20);
        final Pattern INGEST_CONFIGURATION =
                Pattern.compile("(.+)_ingest_configuration\\.xml");
        log.trace("Locating sources in " + INGEST_CONFIGURATIONS_ROOT);
        INGEST_CONFIGURATIONS_ROOT.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                log.debug("Checking " + name);
                Matcher matcher = INGEST_CONFIGURATION.matcher(name);
                if (matcher.matches()) {
                    log.trace("Located source " + matcher.group(1));
                    sources.add(matcher.group(1));
                }
                return true;
            }
        });
        return sources;
    }

    private void performMUXIndex() throws IOException, InterruptedException {
        log.info("Starting index");
        Configuration indexConf = getIndexConfiguration();

        FilterService index = new FilterService(indexConf);
        index.start();
        while (index.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for index ½ a second");
            Thread.sleep(500);
        }
        index.stop();
    }

    public void testGetIndexConfiguration() throws Exception {
        // Throws exceptions by itself if something is blatantly wrong
        getIndexConfiguration();
    }

    private Configuration getIndexConfiguration() throws IOException {
        Configuration indexConf = Configuration.load(Resolver.getURL(
                        "data/multiple/index_configuration.xml").
                getFile());
        indexConf.set(Service.CONF_SERVICE_ID, "IndexService");

        ArrayList<String> sources = getSources();
        indexConf.getSubConfiguration("SingleChain").
                getSubConfiguration("Muxer").
                set(MUXFilter.CONF_FILTERS, sources);
/*        for (String source: sources) {
            log.trace("Updating index conf with source " + source);
            Configuration sourceConf =
                    indexConf.getSubConfiguration("SingleChain").
                            getSubConfiguration("Muxer").
                            getSubConfiguration(source);*/
/*            sourceConf.set(MUXFilterFeeder.CONF_FILTER_CLASS,
                           XMLTransformer.class.getName());
            sourceConf.set(MUXFilterFeeder.CONF_FILTER_NAME,
                           source + " transformer");
            sourceConf.set(MUXFilterFeeder.CONF_FILTER_BASES, source);*/
/*            String xsltRelativeLocation = String.format(
                    "targets/%s/%1$s_index.xsl", source);
            if ("fagref".equals(source)) {
                xsltRelativeLocation = "targets/fagreferent/fagref_index.xsl";
            } else if ("nat".equals(source)) {
                xsltRelativeLocation = "targets/aleph/aleph_index.xsl";
            } else if ("reklamefilm".equals(source)) {
                xsltRelativeLocation = "targets/DanskReklameFilm/reklamefilm_index.xsl";
            }
            URL sourceXSLT = Resolver.getURL(xsltRelativeLocation);
            assertNotNull(String.format(
                    "The XSLT for source %s should exist at the relative "
                    + "location %s", source, xsltRelativeLocation), sourceXSLT);
            sourceConf.set(XMLTransformer.CONF_XSLT, sourceXSLT.getFile());*/
        //}

        String indexDescriptorLocation = new File(
                ReleaseTestCommon.DATA_ROOT,
                "multiple/index_descriptor.xml").toString();
        log.debug("indexDescriptorLocation: " + indexDescriptorLocation);

        indexConf.getSubConfiguration("SingleChain").
                getSubConfiguration("DocumentCreator").
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);

        indexConf.getSubConfiguration("SingleChain").
                getSubConfiguration("IndexUpdate").
                getSubConfiguration("LuceneUpdater").
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);
        return indexConf;
    }
}
