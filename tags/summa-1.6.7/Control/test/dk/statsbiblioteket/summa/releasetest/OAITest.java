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
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

/**
 * This test it partly manual, as the result (primarily the index) needs to be
 * inspected manually. It is placed in the System's temporary folder under
 * the sub folder "summatest".
 */
// TODO: Change the root to tmp-dir
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class OAITest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(OAITest.class);

    @Override
    public void setUp () throws Exception {
        super.setUp();
        ReleaseTestCommon.setup();
        SearchTest.INDEX_ROOT.mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        ReleaseTestCommon.tearDown();
    }

    public void testIngest() throws Exception {
        getStorageService();
        Profiler ingestProfiler = new Profiler();
        FilterService ingest = performOAIIngest();
        String ingestTime = ingestProfiler.getSpendTime();
        log.info("Finished ingest in " + ingestTime);
        ingest.stop();
    }

    public static StorageService getStorageService() throws IOException {
        Configuration storageConf = Configuration.load(Resolver.getURL(
                "test-storage-1/config/configuration.xml").getFile());
        storageConf.set(Service.CONF_SERVICE_ID, "StorageService");
        log.info("Starting storage");
        StorageService storage = new StorageService(storageConf);
        storage.start();
        storage.getStorage().clearBase("dummy");
        return storage;
    }

    public void testFull() throws Exception {
        StorageService storage = getStorageService();

        Profiler ingestProfiler = new Profiler();
        FilterService ingest = performOAIIngest();
        String ingestTime = ingestProfiler.getSpendTime();

        Profiler indexProfiler = new Profiler();
        performOAIIndex();
        log.info("Finished indexing in " + indexProfiler.getSpendTime()
                 + ", ingesting in " + ingestTime);

        SearchService search = getSearchService();

        ingest.stop();
        search.stop();
        storage.stop();
    }

    public void testDump() throws Exception {
        StorageService storage = getStorageService();

        FilterService ingest = performOAIIngest();
        performOAIIndex();

        SummaSearcher search = getSearch();

        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "oai");
        log.info("Result of test-search for 'oai':\n"
                 + search.search(request).toXML());

        ingest.stop();
        search.close();
        storage.stop();
    }

    public static SearchService getSearchService() throws IOException {
        log.info("Starting search service");
//        String indexDescriptorLocation = new File(
//                ReleaseTestCommon.DATA_ROOT,
//                "oai/oai_IndexDescriptor.xml").toString();
        Configuration searchConf = Configuration.load(Resolver.getURL(
                "test-facet-search-1/config/configuration.xml").
                getFile());
        searchConf.set(Service.CONF_SERVICE_ID, "SearchService");
//        searchConf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
//                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
//                    indexDescriptorLocation);
        SearchService search = new SearchService(searchConf);
        search.start();
        return search;
    }

    public static SummaSearcher getSearch() throws IOException {
        log.info("Starting search");
//        String indexDescriptorLocation = new File(
//                ReleaseTestCommon.DATA_ROOT,
//                "oai/oai_IndexDescriptor.xml").toString();
        Configuration searchConf = Configuration.load(Resolver.getURL(
                "test-facet-search-1/config/configuration.xml").
                getFile());
        return new SummaSearcherImpl(searchConf);
    }

    public static void performOAIIndex() throws Exception {
        log.info("Starting index");
        Configuration indexConf = Configuration.load(Resolver.getURL(
                "test-facet-index-1/config/configuration.xml").
                getFile());
        indexConf.set(Service.CONF_SERVICE_ID, "IndexService");
        String oaiTransformerXSLT = new File(
                ReleaseTestCommon.DATA_ROOT,
                "oai/oai_index.xsl").toString();
        log.debug("oaiTransformerXSLT: " + oaiTransformerXSLT);
        indexConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(1).
//                getSubConfiguration("XMLTransformer").
                set(XMLTransformer.CONF_XSLT, oaiTransformerXSLT);
        String indexDescriptorLocation = new File(
                ReleaseTestCommon.DATA_ROOT,
                "oai/oai_IndexDescriptor.xml").toString();
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
        FilterService index = new FilterService(indexConf);
        index.start();
        while (index.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for index ½ a second");
            Thread.sleep(500);
        }
        index.stop();
    }

    public static FilterService performOAIIngest() throws Exception {
        log.info("Starting ingest");
        Configuration ingestConf = Configuration.load(Resolver.getURL(
                "test-ingest-oai/config/configuration.xml").getFile());
        ingestConf.set(Service.CONF_SERVICE_ID, "IngestService");
        ingestConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
//                getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER,
                    new File(ReleaseTestCommon.DATA_ROOT,
                             "oai/minidump"));
        FilterService ingest = new FilterService(ingestConf);
        ingest.start();
        while (ingest.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest ½ a second");
            Thread.sleep(500);
        }
        return ingest;
    }
}

