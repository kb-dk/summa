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
package dk.statsbiblioteket.summa.storage.rmi;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.Files;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class RMIStorageProxyTest extends TestCase {
    public RMIStorageProxyTest(String name) {
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
        return new TestSuite(RMIStorageProxyTest.class);
    }

    private File testRoot = new File(System.getProperty(
            "java.io.tmpdir", "storagetest"));
    private Storage getRMIStorage() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                DatabaseStorage.CONF_LOCATION,
                testRoot.toString());
        return new RMIStorageProxy(conf);
    }

    public void testGetRecord() throws Exception {
        Storage storage = getRMIStorage();
        storage.flush(new Record("Dummy1", "SomeBase", new byte[0]));
        assertFalse("The added record should exist",
                    storage.getRecord("Dummy1", null).isDeleted());
    }

    public void testDirectClear() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                DatabaseStorage.CONF_LOCATION,
                testRoot.toString());
        Storage storage = StorageFactory.createStorage(conf);
        testClearBase(storage);
    }

    public void testClearBase() throws Exception {
        Storage storage = getRMIStorage();
        testClearBase(storage);
    }

    public static void testClearBase(Storage storage) throws Exception {
        storage.flush(new Record("Dummy1", "SomeBase", new byte[0]));
        assertFalse("The added record should exist",
                   storage.getRecord("Dummy1", null).isDeleted());
        storage.clearBase("SomeBase");
        assertTrue("The added record should not exist anymore",
                   storage.getRecord("Dummy1", null).isDeleted());
    }
}
