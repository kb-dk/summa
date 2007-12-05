/* $Id: FileStorageTest.java,v 1.6 2007/10/11 12:56:24 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/11 12:56:24 $
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
package dk.statsbiblioteket.summa.score.configuration;

import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class FileStorageTest extends ConfigurationStorageTestCase {

    public FileStorageTest () throws Exception {
        super (new FileStorage("configuration.xml"));
    }

    public void testEmptyConstructor () throws Exception {
        FileStorage s = new FileStorage();
        System.out.println ("Created empty file storage at: " + s.getFilename());

        testPersitence();

        new File(s.getFilename()).delete();
    }

    public void testPersitence () throws Exception {
        System.out.println (testName + ": Testing persistence");

        String persitenceTestValue = "PersistenceTestValue^//></&>#0";
        String persitenceTestKey = "PersistenceTestKey";
        String resultValue;

        storage.put (persitenceTestKey, persitenceTestValue);

        ConfigurationStorage newStorage = new FileStorage(configFilename);
        resultValue = (String) newStorage.get (persitenceTestKey);

        assertTrue ("Setting a value on a FileStorage should save the underlying configuration file", persitenceTestValue.equals (resultValue));

        storage.purge (persitenceTestKey);

    }

}
