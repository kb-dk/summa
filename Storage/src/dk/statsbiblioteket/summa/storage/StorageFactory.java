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
package dk.statsbiblioteket.summa.storage;

import java.rmi.RemoteException;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.database.derby.DerbyStorage;
import dk.statsbiblioteket.summa.storage.api.Storage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * te forgot to document this class.
 */
public class StorageFactory {
    private static Log log = LogFactory.getLog(StorageFactory.class);

    /**
     * The fully classified class name for the wanted Storage implementation.
     */
    public static final String PROP_STORAGE = "summa.storage.class";

    private static final Class<? extends StorageBase> DEFAULT_STORAGE =
            DerbyStorage.class;

    /**
     * <p>Construct a storage instance based on the given properties.
     * The properties are also passed to the constructor for the storage.</p>
     *
     * <p>Most interestingly is probably the property {@link #PROP_STORAGE}
     * used to specify the class of the storage implementation to use.</p>
     *
     * @param conf setup for the wanted storage along with the
     *        property {@link #PROP_STORAGE} which should hold the class-name
     *        for the wanted {@link Storage}. If no storage is specified,
     *        the {@code StorageFactory} defaults to {@link DerbyStorage}.
     * @return an object implementing the {@link Storage} interface.
     * @throws RemoteException if the controller could not be created.
     */
    public static Storage createStorage(Configuration conf) throws IOException {
        log.trace("createStorage called");

        Class<? extends Storage> storageClass;
        try {
            storageClass = conf.getClass(PROP_STORAGE,
                                         Storage.class,
                                         DEFAULT_STORAGE);
        } catch (Exception e) {
            throw new RemoteException("Could not get metadata storage control"
                                      + " class from property "
                                      + PROP_STORAGE, e);
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Instantiating storage class " + storageClass);

        try {
            // FIXME: This forces a RMI call when packing as a service. Not good 
            return Configuration.create(storageClass, conf);        
        } catch (Exception e) {
            throw new IOException("Failed to instantiate storage class "
                                  + storageClass, e);
        }
    }

}
