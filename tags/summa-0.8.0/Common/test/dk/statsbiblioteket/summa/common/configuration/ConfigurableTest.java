/* $Id: ConfigurableTest.java,v 1.3 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.3 $
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

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.RemoteStorage;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class ConfigurableTest extends TestCase {
    
    public void setUp () throws Exception {

    }

    public void tearDown () throws Exception {

    }

    /**
     * Try and instantiate each {@link ConfigurationStorage} as a {@link Configurable}
     * through {@link Configuration#create}.
     *
     * Assert that Configuration based on these storages are equal (this is a deep check)
     */
    public void testStorageInstantiations () throws Exception {
        Configuration base = new Configuration(new FileStorage("configuration.xml"));

        Configuration fileConf = new Configuration ((ConfigurationStorage)Configuration.create (FileStorage.class, base));
        assertTrue (base.equals(fileConf));

        Configuration memConf = new Configuration ((ConfigurationStorage)Configuration.create(MemoryStorage.class, base));
        assertTrue (base.equals(memConf));

        Configuration remoteConf = new Configuration ((ConfigurationStorage)Configuration.create(RemoteStorage.class, base));
        assertTrue (base.equals(remoteConf));
    }
}
