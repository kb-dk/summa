/* $Id: Storage.java,v 1.4 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;

/**
 * Storage is a unified interface for {@link ReadableStorage} and
 * {@link WritableStorage}.
 *
 * Client wishing to connect to remote Storage services should look
 * at {@link StorageConnectionFactory}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface Storage extends ReadableStorage, WritableStorage {

    /**
     * Configuration property specifying the class to use as storage
     * implementation. The default is
     * {@link dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy}
     */
    public static final String CONF_CLASS = "summa.storage.class";

    /**
     * Configuration property specifying which port the Storage service
     * should communicate on. Default is 27027.
     */
    public static final String CONF_SERVICE_PORT = "summa.storage.service.port";        

    /**
     * Configuration property specifying where the storage should store
     * persistent data. Default is {@code ~/summa-control/persistent}.
     */
    public static final String CONF_DATA_DIR = "summa.storage.data.dir";
}



