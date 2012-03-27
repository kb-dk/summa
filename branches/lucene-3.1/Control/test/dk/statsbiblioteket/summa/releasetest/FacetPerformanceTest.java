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
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.ingest.source.RecordGenerator;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Creates an index with fagref pseudodata, taxing the facet builder.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetPerformanceTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(FacetPerformanceTest.class);


    public static final File TMP =
        new File("tmp", "facetperformance");
    public static final String INDEX = new File(TMP, "index").getAbsolutePath();
    @Override
    public void setUp () throws Exception {
        super.setUp();
        cleanup();
        TMP.mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void cleanup() throws Exception {
        if (TMP.exists()) {
            Files.delete(TMP);
        }
    }

    public static Configuration getSearcherConfiguration() throws Exception {
        Configuration searcherConf = Configuration.load(
              "data/facetperformance/FacetPerformance_SearchConfiguration.xml");
        assertNotNull("The Facet configuration should not be empty",
                      searcherConf);
        searcherConf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT, INDEX);
        return searcherConf;
    }

    public static Configuration getIndexConfiguration(int records)
                                                              throws Exception {
        URL descriptorLocation = Resolver.getURL(
                "data/facetperformance/FacetPerformance_IndexDescriptor.xml");
        assertNotNull("The descriptor location should not be null",
                      descriptorLocation);
        URL xsltLocation = Resolver.getURL(
            "data/facetperformance/fagref/fagref_index.xsl");
        assertNotNull("The XSLT location should not be null",
                      descriptorLocation);
        Configuration indexConf = Configuration.load(
               "data/facetperformance/FacetPerformance_IndexConfiguration.xml");
        assertNotNull("The index configuration should not be empty",
                      indexConf);

        List<Configuration> filters =
            indexConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0)
            .getSubConfigurations(FilterSequence.CONF_FILTERS);

        filters.get(0). // Generator
            set(RecordGenerator.CONF_RECORDS, records);
        filters.get(1). // Transformer
            set(XMLTransformer.CONF_XSLT, xsltLocation.toURI().toString());
        filters.get(2). // Document creator
            getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
            set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                descriptorLocation.toURI().toString());
        filters.get(3). // Index update
            set(IndexControllerImpl.CONF_INDEX_ROOT_LOCATION, INDEX);
        filters.get(3).
            getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
            set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                descriptorLocation.toURI().toString());
        return indexConf;
    }

    public void testSmallIndex() throws Exception {
        FilterService indexService =
            new FilterService(getIndexConfiguration(10));
        indexService.start();
        waitForService(indexService, Integer.MAX_VALUE);
        indexService.stop();
    }

    public String measureIndexTime(int records) throws Exception {
        FilterService indexService =
            new FilterService(getIndexConfiguration(records));
        Profiler profiler = new Profiler();
        indexService.start();
        waitForService(indexService, Integer.MAX_VALUE);
        indexService.stop();
        return profiler.getSpendTime();
    }

    public void testIndexBuildTime() throws Exception {
        // Can be change for performance test, not to commit to SVN
        int[] RECORDS = new int[]{1000, 10000}; //, 100000, 1000000};
        for (int records: RECORDS) {
            cleanup();
            log.info(
                records + " records: " + measureIndexTime(records));
        }
    }

    public void testInterativeBuild() throws Exception {
        log.info("Update of a single record: " + measureIndexTime(1));
    }

    private void waitForService(FilterService service, int timeout)
                                  throws RemoteException, InterruptedException {
        long endTime = System.currentTimeMillis() + timeout;
        log.debug("Waiting a maximum of " + timeout + " ms for service");
        while (!service.getStatus().getCode().equals(Status.CODE.stopped) &&
               System.currentTimeMillis() < endTime) {
            log.trace("Sleeping a bit");
            Thread.sleep(100);
        }
        assertTrue("The service '" + service + "' should have stopped by now",
                   service.getStatus().getCode().equals(Status.CODE.stopped));
        log.debug("Finished waiting for service");
    }


    public void testCreateSearcher() throws Exception {
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        log.debug("Searcher created: " + searcher);
        searcher.close();
    }

    public void testSmallSearch() throws Exception {
        testSmallIndex();
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        log.debug("Searcher created: " + searcher);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "*");
        ResponseCollection responses = searcher.search(request);
        log.info(responses.toXML());
        searcher.close();
    }
}