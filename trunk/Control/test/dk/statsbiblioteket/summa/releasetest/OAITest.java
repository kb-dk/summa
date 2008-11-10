/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * This is a unit-test mimicking
 *http://wiki.statsbiblioteket.dk/summa/Community/Tutorials/MinimalDeployment1.0
 * as such, it is neccessary to follow steps 1.3-1.4 to get the test-data and
 * the XSLTs in place.
 * </p><p>
 * This test it partly manual, as the result (primarily the index) needs to be
 * inspected manually. It is places under the current directory, along with the
 * storage folder.
 */
// TODO: Change the root to tmp-dir
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class OAITest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(OAITest.class);

    public void setUp () throws Exception {
        super.setUp();
        ReleaseTestCommon.setup();
        SearchTest.INDEX_ROOT.mkdirs();
    }

    public void tearDown() throws Exception {
        super.tearDown();
//        ReleaseTestCommon.tearDown();
    }

    public void testIngest() throws Exception {
        Profiler profiler = new Profiler();
        Configuration storageConf = Configuration.load(Resolver.getURL(
                        "test-storage-1/config/configuration.xml").getFile());
        storageConf.set(Service.CONF_SERVICE_ID, "StorageService");
        log.info("Starting storage");
        StorageService storage = new StorageService(storageConf);
        storage.start();
        storage.getStorage().clearBase("dummy");

        Profiler ingestProfiler = new Profiler();
        log.info("Starting ingest");
        Configuration ingestConf = Configuration.load(Resolver.getURL(
                        "test-ingest-oai/config/configuration.xml").getFile());
        ingestConf.set(Service.CONF_SERVICE_ID, "IngestService");
/**/        ingestConf.getSubConfiguration("SingleChain").
                getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER,
                    new File(ReleaseTestCommon.DATA_ROOT,
                             "oai/minidump"));
        FilterService ingest = new FilterService(ingestConf);
        ingest.start();
        while (ingest.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest ½ a second");
            Thread.sleep(500);
        }
        String ingestTime = ingestProfiler.getSpendTime();
        log.info("Finished ingest in " + ingestTime);
    }

    public void testFull() throws Exception {
        Profiler profiler = new Profiler();
        Configuration storageConf = Configuration.load(Resolver.getURL(
                        "test-storage-1/config/configuration.xml").getFile());
        storageConf.set(Service.CONF_SERVICE_ID, "StorageService");
        log.info("Starting storage");
        StorageService storage = new StorageService(storageConf);
        storage.start();
        storage.getStorage().clearBase("dummy");

        Profiler ingestProfiler = new Profiler();
        log.info("Starting ingest");
        Configuration ingestConf = Configuration.load(Resolver.getURL(
                        "test-ingest-oai/config/configuration.xml").getFile());
        ingestConf.set(Service.CONF_SERVICE_ID, "IngestService");
/**/        ingestConf.getSubConfiguration("SingleChain").
                getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER,
                    new File(ReleaseTestCommon.DATA_ROOT,
                             "oai/minidump"));
        FilterService ingest = new FilterService(ingestConf);
        ingest.start();
        while (ingest.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest ½ a second");
            Thread.sleep(500);
        }
        String ingestTime = ingestProfiler.getSpendTime();

        Profiler indexProfiler = new Profiler();
        log.info("Starting index");
        Configuration indexConf = Configuration.load(Resolver.getURL(
                        "test-facet-index-1/config/configuration.xml").
                getFile());
        indexConf.set(Service.CONF_SERVICE_ID, "IndexService");
        String oaiTransformerXSLT = new File(
                ReleaseTestCommon.DATA_ROOT,
                "oai/oai_index.xsl").toString();
        log.debug("oaiTransformerXSLT: " + oaiTransformerXSLT);
/**/        indexConf.getSubConfiguration("SingleChain").
                getSubConfiguration("XMLTransformer").
                set(XMLTransformer.CONF_XSLT, oaiTransformerXSLT);
        String indexDescriptorLocation = new File(
                ReleaseTestCommon.DATA_ROOT,
                "oai/oai_IndexDescriptor.xml").toString();
        log.debug("indexDescriptorLocation: " + indexDescriptorLocation);
/**/        indexConf.getSubConfiguration("SingleChain").
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
        log.info("Finished indexing in " + indexProfiler.getSpendTime()
                 + ", ingesting in " + ingestTime);

        log.info("Starting search");
        Configuration searchConf = Configuration.load(Resolver.getURL(
                        "test-facet-search-1/config/configuration.xml").
                getFile());
        searchConf.set(Service.CONF_SERVICE_ID, "SearchService");
/**/        searchConf.getSubConfigurations(SearchNodeFactory.CONF_NODES).
                get(0).
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);
        SearchService search = new SearchService(searchConf);
        search.start();

        ingest.stop();
        search.stop();
        storage.stop();
    }

}
