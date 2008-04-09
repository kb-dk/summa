/* $Id: RemoteStorageTest.java,v 1.4 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:18 $
 * $Author: te $
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
package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.summa.common.configuration.storage.RemoteStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class RemoteStorageTest extends ConfigurationStorageTestCase {

    /** For debugging purposes */
    public RemoteStorage direct_storage;

    public RemoteStorageTest () throws Exception {
        super (new FileStorage("configuration.xml"));
        testName = this.getClass().getSimpleName();
    }

    public void setUp () throws Exception {
        Configuration conf = new Configuration(storage);

        System.out.println (testName + ": Creating remote storage");
        direct_storage = (RemoteStorage) conf.create(RemoteStorage.class);

        System.out.println (testName + ": Connecting to registry on " +
                                                  conf.getString(RemoteStorage.PROP_REGISTRY_HOST) +
                                                  ":" +
                                                  conf.getInt(RemoteStorage.PROP_REGISTRY_PORT));

        Registry reg = LocateRegistry.getRegistry(conf.getString(RemoteStorage.PROP_REGISTRY_HOST),
                                                  conf.getInt(RemoteStorage.PROP_REGISTRY_PORT));

        System.out.println("Connecting to remote storage at '"
                           + conf.getString(RemoteStorage.PROP_NAME) + "'");
        storage = (ConfigurationStorage) reg.lookup(conf.getString(RemoteStorage.PROP_NAME));

        System.out.println (testName + ": Remote storage prepared");
    }
}
