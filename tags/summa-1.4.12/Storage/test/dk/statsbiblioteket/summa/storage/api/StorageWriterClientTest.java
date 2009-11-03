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
package dk.statsbiblioteket.summa.storage.api;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxyTest;
import dk.statsbiblioteket.util.Files;

import java.io.File;

/**
 * StorageWriterClient Tester.
 *
 * @author <Authors name>
 * @since <pre>08/13/2009</pre>
 * @version 1.0
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class StorageWriterClientTest extends TestCase {
    public StorageWriterClientTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(StorageWriterClientTest.class);
    }

    private File testRoot = new File(System.getProperty(
            "java.io.tmpdir", "storagetest"));
    private Storage getRMIStorage() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                DatabaseStorage.CONF_LOCATION,
                testRoot.toString());
        return new RMIStorageProxy(conf);
    }

    public void testLocalWrite() throws Exception {
        Storage storage = getRMIStorage();
        storage.flush(new Record("Dummy1", "SomeBase", new byte[0]));
        assertNotNull("The added record should exist",
                      storage.getRecord("Dummy1", null));
    }

    public void testRemoteWriteGetClear() throws Exception {
        Storage localStorage = getRMIStorage();
        Configuration conf = Configuration.newMemoryBased(
                ConnectionConsumer.CONF_RPC_TARGET,
                "//localhost:28000/summa-storage");
        StorageWriterClient remoteStorage = new StorageWriterClient(conf);
        StorageWriterClient remoteStorage2 = new StorageWriterClient(conf);

        remoteStorage.flush(new Record("Dummy1", "SomeBase", new byte[0]));
        remoteStorage2.flush(new Record("Dummy2", "SomeBase", new byte[0]));
        assertFalse("The added record should exist",
                    localStorage.getRecord("Dummy1", null).isDeleted());
        remoteStorage.clearBase("SomeBase");
        assertTrue("The added record should not exist anymore",
                   localStorage.getRecord("Dummy1", null).isDeleted());
        remoteStorage.close();
        remoteStorage2.close();
        localStorage.close();
    }

    public void testRemoteClear() throws Exception {
        Storage storage = getRMIStorage();
        RMIStorageProxyTest.testClearBase(storage);
    }
}
