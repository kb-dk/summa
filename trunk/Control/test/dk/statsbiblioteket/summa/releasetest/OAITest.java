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
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a unit-test mimicking
 * http://wiki.statsbiblioteket.dk/summa/Community/Tutorials/MinimalDeployment1.0
 * as such, it is neccessary to follow steps 1.3-1.4 
 *
 * Tests OAI ingesting from a corpus of 1300 records.
 * </p><p>
 *
 * The records must be downloaded from
 * http://wiki.statsbiblioteket.dk/summa/Community/Tutorials
 * and unpacked in /tmp creating /tmp/summa_test_data_small_OAI/
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class OAITest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(FacetTest.class);

    public void setUp () throws Exception {
        super.setUp();
        SearchTest.INDEX_ROOT.mkdirs();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testFull() throws Exception {
        Configuration storageConf = Configuration.load(Resolver.getURL(
                        "test-storage-1/config/configuration.xml").getFile());
        storageConf.set(Service.CONF_SERVICE_ID, "StorageService");
        StorageService storage = new StorageService(storageConf);
        storage.start();
        storage.getStorage().clearBase("dummy");

        log.info("Starting ingest");
        Configuration ingestConf = Configuration.load(Resolver.getURL(
                        "test-ingest-oai/config/configuration.xml").getFile());
        ingestConf.set(Service.CONF_SERVICE_ID, "IngestService");
        FilterService ingest = new FilterService(ingestConf);
        ingest.start();
        while (ingest.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest ½ a second");
            Thread.sleep(500);
        }

        log.info("Starting index");
        Configuration indexConf = Configuration.load(Resolver.getURL(
                        "test-facet-index-1/config/configuration.xml").
                getFile());
        indexConf.set(Service.CONF_SERVICE_ID, "IndexService");
        FilterService index = new FilterService(indexConf);
        index.start();
        while (index.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for index ½ a second");
            Thread.sleep(500);
        }

        log.info("Starting search");
        Configuration searchConf = Configuration.load(Resolver.getURL(
                        "test-facet-search-1/config/configuration.xml").
                getFile());
        searchConf.set(Service.CONF_SERVICE_ID, "SearchService");
        SearchService search = new SearchService(searchConf);
        search.start();

        ingest.stop();
        search.stop();
        storage.stop();
    }

}



