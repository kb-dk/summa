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

import java.io.File;
import java.net.URL;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.score.service.StorageService;
import dk.statsbiblioteket.summa.score.service.FilterService;
import dk.statsbiblioteket.summa.score.api.Status;
import dk.statsbiblioteket.summa.storage.io.RecordIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(IndexTest.class);

    public static final String TESTBASE = "fagref";
    public static final int NUM_RECORDS = 3;

    public void testIngestAndIndex() throws Exception {
        StorageService storage = fillStorage();
    }

    /**
     * Create a Storage and fill it with test-data, ready for indexing.
     * @return the StorageService containing the filled Storage.
     * @throws Exception if the fill failed.
     */
    public StorageService fillStorage() throws Exception {
        URL dataLocation =
                Thread.currentThread().getContextClassLoader().getResource(
                        "data/fagref/fagref_testdata.txt");
        assertNotNull("The data location should not be null", dataLocation);
        File ingestRoot = new File(dataLocation.getFile()).getParentFile();

        int TIMEOUT = 10000;

        // Storage
        Configuration storageConf = IngestTest.getStorageConfiguration();
        StorageService storage = new StorageService(storageConf);
        storage.start();

        // FIXME: Use classloader to locate the test root
        // TODO: Copy and replace %INGEST_ROOT%
        File filterConfFile = new File("Score/test/data/5records/"
                                       + "filter_setup.xml").getAbsoluteFile();
        assertTrue("The filter conf. should exist", filterConfFile.exists());
        Configuration filterConf = Configuration.load(filterConfFile.getPath());
        assertNotNull("Configuration should contain "
                      + FilterControl.CONF_CHAINS,
                      filterConf.getString(FilterControl.CONF_CHAINS));

        FilterService ingester = new FilterService(filterConf);
        ingester.start();

        long endTime = System.currentTimeMillis() + TIMEOUT;
        while (!ingester.getStatus().getCode().equals(Status.CODE.stopped) &&
               System.currentTimeMillis() < endTime) {
            log.debug("Sleeping a bit");
            Thread.sleep(100);
        }
        assertTrue("The ingester should have stopped by now",
                   ingester.getStatus().getCode().equals(Status.CODE.stopped));

        RecordIterator recordIterator =
                storage.getRecordsModifiedAfter(0, TESTBASE);
        assertTrue("The iterator should have at least one element",
                   recordIterator.hasNext());
        for (int i = 0 ; i < NUM_RECORDS ; i++) {
            assertTrue("Storage should have next for record #" + (i+1),
                       recordIterator.hasNext());
            Record record = recordIterator.next();
            assertNotNull("The next should give a record", record);
        }
        assertFalse("After " + NUM_RECORDS + " Records, iterator should finish",
                    recordIterator.hasNext());
        return storage;
    }
}
