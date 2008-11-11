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
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.File;
import java.net.URL;

/**
 * Performs ingest, index and search on multiple sources using the MUXFilter.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
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

    public void testTargetExistence() throws Exception {
        URL fagrefLocation =
                Resolver.getURL("targets/fagreferent/fagref_index.xsl");
        log.debug("Fagref XSLT location was " + fagrefLocation);
        assertNotNull("A location should be available for fagres XSTL",
                      fagrefLocation);
        URL sbmarcTestDataLocation =
                Resolver.getURL("data/horizon/one_book.xml");
        log.debug("SBMARC data location was " + sbmarcTestDataLocation);
        assertNotNull("A location should be available for SBMARC",
                      sbmarcTestDataLocation);
    }

    public void testHorizonIngest() throws Exception {
        StorageService storage = OAITest.getStorageService();
        FilterService horizon = performHorizonIngest();
        assertEquals("There should be the expected number of horizon records",
                     1, countRecords(storage, "horizon"));
        log.debug("All OK. Closing down");
        horizon.stop();
        storage.stop();
    }

    public void testFull() throws Exception {
        StorageService storage = OAITest.getStorageService();

        Profiler ingestProfiler = new Profiler();
//        FilterService ingestOAI = OAITest.performOAIIngest();
        String ingestOAITime = ingestProfiler.getSpendTime();
//        assertTrue("There should be more than 0 records from base oai",
 //                  countRecords(storage, "oai") > 0);

        Profiler ingestProfilerFagref = new Profiler();
        FilterService ingestFagref = performFagrefIngest();
        String ingestFagrefTime = ingestProfilerFagref.getSpendTime();
        assertTrue("There should be more than 0 records from base fagref",
                   countRecords(storage, "fagref") > 0);

        Profiler ingestProfilerHorizon = new Profiler();
        FilterService ingestHorizon = performHorizonIngest();
        String ingestHorizonTime = ingestProfilerHorizon.getSpendTime();
        assertTrue("There should be more than 0 records from base horizon",
                   countRecords(storage, "horizon") > 0);

        Profiler indexProfiler = new Profiler();
        performMUXIndex();
        String indexTime = indexProfiler.getSpendTime();

        SearchService search = OAITest.getSearchService();

   //     ingestOAI.stop();
        ingestFagref.stop();
        ingestHorizon.stop();
        search.stop();
        storage.stop();

        log.info("Finished indexing in " + indexTime
                 + ", ingesting OAI in " + ingestOAITime
                 + ", horizon in " + ingestHorizonTime
                 + ", fagref in " + ingestFagrefTime);
    }

    private int countRecords(StorageService storage, String base) throws
                                                                  IOException {
        StorageIterator iterator = new StorageIterator(storage.getStorage(),
                            storage.getStorage().getRecordsFromBase(base));
        int counter = 0;
        while (iterator.hasNext()) {
            counter++;
            iterator.next();
        }
        return counter;
    }

    public static FilterService performFagrefIngest() throws IOException,
                                                          InterruptedException {
        log.info("Starting ingest of OAI");
        Configuration ingestFagrefConf = Configuration.load(Resolver.getURL(
                        "test-ingest-fagref/config/configuration.xml").
                getFile());
        ingestFagrefConf.set(Service.CONF_SERVICE_ID, "IngestServiceFagref");
        ingestFagrefConf.getSubConfiguration("SingleChain").
                getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER,
                    new File(ReleaseTestCommon.DATA_ROOT, "fagref"));
        FilterService ingestFagref = new FilterService(ingestFagrefConf);
        ingestFagref.start();
        while (ingestFagref.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest ½ a second");
            Thread.sleep(500);
        }
        return ingestFagref;
    }

    public static FilterService performHorizonIngest() throws IOException,
                                                          InterruptedException {
        log.info("Starting ingest of Horizon");
        Configuration ingestFagrefConf = Configuration.load(Resolver.getURL(
                        "data/multiple/horizon_ingest_configuration.xml").
                getFile());
        ingestFagrefConf.set(Service.CONF_SERVICE_ID, "IngestServiceHorizon");
        ingestFagrefConf.getSubConfiguration("SingleChain").
                getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER,
                    new File(ReleaseTestCommon.DATA_ROOT, "horizon"));
        FilterService ingestHorizon = new FilterService(ingestFagrefConf);
        ingestHorizon.start();
        while (ingestHorizon.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for horizon ingest ½ a second");
            Thread.sleep(500);
        }
        return ingestHorizon;
    }

    private void performMUXIndex() throws IOException, InterruptedException {
        log.info("Starting index");
        Configuration indexConf = Configuration.load(Resolver.getURL(
                        "data/multiple/index_configuration.xml").
                getFile());
        indexConf.set(Service.CONF_SERVICE_ID, "IndexService");

        String oaiTransformerXSLT = Resolver.getURL(
                "targets/oai/oai_index.xsl").getFile();
        log.debug("oaiTransformerXSLT: " + oaiTransformerXSLT);

        String fagrefTransformerXSLT = Resolver.getURL(
                "targets/fagreferent/fagref_index.xsl").getFile();
        log.debug("fagrefTransformerXSLT: " + fagrefTransformerXSLT);

        String horizonTransformerXSLT = Resolver.getURL(
                "targets/horizon/horizon_index.xsl").getFile();
        log.debug("horizonTransformerXSLT: " + horizonTransformerXSLT);

        String indexDescriptorLocation = new File(
                ReleaseTestCommon.DATA_ROOT,
                "oai/oai_IndexDescriptor.xml").toString();
        log.debug("indexDescriptorLocation: " + indexDescriptorLocation);

        indexConf.getSubConfiguration("SingleChain").
                getSubConfiguration("Muxer").
                getSubConfiguration("FagrefTransformer").
                set(XMLTransformer.CONF_XSLT, fagrefTransformerXSLT);
        indexConf.getSubConfiguration("SingleChain").
                getSubConfiguration("Muxer").
                getSubConfiguration("OAITransformer").
                set(XMLTransformer.CONF_XSLT, oaiTransformerXSLT);
        indexConf.getSubConfiguration("SingleChain").
                getSubConfiguration("Muxer").
                getSubConfiguration("HorizonTransformer").
                set(XMLTransformer.CONF_XSLT, horizonTransformerXSLT);

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

        FilterService index = new FilterService(indexConf);
        index.start();
        while (index.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for index ½ a second");
            Thread.sleep(500);
        }
        index.stop();
    }
}
