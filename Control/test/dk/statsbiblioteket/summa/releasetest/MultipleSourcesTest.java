/* $Id:$
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
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Performs ingest, index and search on multiple sources using the MUXFilter.
 * </p><p>
 * As with OAITest, this test is a pseudo-unit-test, which requires test-data
 * to be in place under /tmp before run. Follow steps 1.3 and 1.4 from
* http://wiki.statsbiblioteket.dk/summa/Community/Tutorials/MinimalDeployment1.0
 *  to get the test-data and the XSLTs in place.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MultipleSourcesTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(MultipleSourcesTest.class);

    public void setUp () throws Exception {
        super.setUp();
        SearchTest.INDEX_ROOT.mkdirs();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testFull() throws Exception {
        Profiler profiler = new Profiler();
        Configuration storageConf = Configuration.load(Resolver.getURL(
                        "test-storage-1/config/configuration.xml").getFile());
        storageConf.set(Service.CONF_SERVICE_ID, "StorageService");
        StorageService storage = new StorageService(storageConf);
        storage.start();
        storage.getStorage().clearBase("dummy");

        Profiler ingestProfiler = new Profiler();
        log.info("Starting ingest of OAI");
        Configuration ingestConf = Configuration.load(Resolver.getURL(
                        "test-ingest-oai/config/configuration.xml").getFile());
        ingestConf.set(Service.CONF_SERVICE_ID, "IngestService");
        FilterService ingest = new FilterService(ingestConf);
        ingest.start();
        while (ingest.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest ½ a second");
            Thread.sleep(500);
        }
        String ingestTime = ingestProfiler.getSpendTime();

        Profiler ingestProfilerFagref = new Profiler();
        log.info("Starting ingest of OAI");
        Configuration ingestFagrefConf = Configuration.load(Resolver.getURL(
                        "test-ingest-fagref/config/configuration.xml").
                getFile());
        ingestFagrefConf.set(Service.CONF_SERVICE_ID, "IngestServiceFagref");
        FilterService ingestFagref = new FilterService(ingestFagrefConf);
        ingestFagref.start();
        while (ingestFagref.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest ½ a second");
            Thread.sleep(500);
        }
        String ingestFagrefTime = ingestProfilerFagref.getSpendTime();

        Profiler indexProfiler = new Profiler();
        log.info("Starting index");
        Configuration indexConf = Configuration.load(Resolver.getURL(
                        "data/multiple/index_configuration.xml").
                getFile());
        indexConf.set(Service.CONF_SERVICE_ID, "IndexService");
        FilterService index = new FilterService(indexConf);
        index.start();
        while (index.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for index ½ a second");
            Thread.sleep(500);
        }
        log.info("Finished indexing in " + indexProfiler.getSpendTime()
                 + ", ingesting OAI in " + ingestTime + ", fagref in "
                 + ingestFagrefTime);

        log.info("Starting search");
        Configuration searchConf = Configuration.load(Resolver.getURL(
                        "test-facet-search-1/config/configuration.xml").
                getFile());
        searchConf.set(Service.CONF_SERVICE_ID, "SearchService");
        SearchService search = new SearchService(searchConf);
        search.start();

        ingest.stop();
        ingestFagref.stop();
        search.stop();
        storage.stop();
    }
}
