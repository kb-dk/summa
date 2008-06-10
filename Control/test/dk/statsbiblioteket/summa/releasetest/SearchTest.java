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

import java.io.File;
import java.rmi.RMISecurityManager;
import java.security.Permission;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.summa.storage.io.Control;
import dk.statsbiblioteket.summa.storage.io.RecordIterator;
import dk.statsbiblioteket.summa.storage.StorageFactory;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import junit.framework.TestCase;

/**
 * The purpose of this class is to test the workflow
 * "files => ingest-chain => storage => index-chain => index = search".
 * It relies on the modules Common, Ingest, Index, Storage and Search.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(SearchTest.class);

    // Set up searcher, check for null
    // Set up storage
    // Run index w/ update on storage
    // Perform small ingest
    // Run index w/ update on storage, verify result in searcher
    // Perform ingest 2
    // Run index w/ update on storage, verify result in searcher
    // Delete record in storage
    // Run index w/ update on storage, verify result in searcher

    public void setUp () throws Exception {
        super.setUp();
        checkSecurityManager();
        IngestTest.deleteOldStorages();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        IngestTest.deleteOldStorages();
    }

    private File root = new File(Resolver.getURL(
            "data/search/SearchTest_IngestConfiguration.xml").
            getFile()).getParentFile();
    private String BASE = "fagref";

    private static void checkSecurityManager() {
        if (System.getSecurityManager() == null) {
            log.debug("Setting allow-all security manager");
            System.setSecurityManager(new RMISecurityManager() {
                public void checkPermission(Permission perm) {
                }
                public void checkPermission(Permission perm, Object context) {
                }
            });
        }
    }

    public StorageService startStorage() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        StorageService storage = new StorageService(storageConf);
        storage.start();
        return storage;
    }
        
    /* ingest the data in the given folder to Storage, assuming that Storage
     * is running. */
    private void ingest(File folder) throws Exception {
        final int TIMEOUT = 10000;

        Configuration conf = Configuration.load(
                "data/search/SearchTest_IngestConfiguration.xml");
        conf.getSubConfiguration("SingleChain").getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER, folder.getAbsolutePath());

        FilterService ingester = new FilterService(conf);
        ingester.start();

        long endTime = System.currentTimeMillis() + TIMEOUT;
        while (!ingester.getStatus().getCode().equals(Status.CODE.stopped) &&
               System.currentTimeMillis() < endTime) {
            log.trace("Sleeping a bit");
            Thread.sleep(100);
        }
        assertTrue("The ingester should have stopped by now",
                   ingester.getStatus().getCode().equals(Status.CODE.stopped));
        ingester.stop();
    }

    /* Connects to Storage, iterates all records and verifies that the given
       id exists.
     */
    private void verifyExistence(StorageService storage, String id) throws
                                                                    Exception {
        RecordIterator recordIterator =
                storage.getRecordsModifiedAfter(0, BASE);
        assertTrue("The iterator should have at least one element",
                   recordIterator.hasNext());
        while (recordIterator.hasNext()) {
            if (recordIterator.next().getId().equals(id)) {
                return;
            }
        }
        fail("A Record with id '" + id + "' should be present in the storage");
    }

    public void testIngest() throws Exception {
        StorageService storage = startStorage();
        ingest(new File(Resolver.getURL("data/search/input/part1").getFile()));
        verifyExistence(storage, "fagref:hj@example.com");
        storage.stop();
    }
}
