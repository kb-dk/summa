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
package dk.statsbiblioteket.summa.storage.api;

import java.rmi.RemoteException;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.storage.database.derby.DerbyStorage;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.StorageBase;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to create instances of {@link Storage} implementations
 * given a configuration.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class StorageFactory {
    private static Log log = LogFactory.getLog(StorageFactory.class);

    /**
     * The default storage class to instantiate if {@link Storage#CONF_CLASS}
     * is not specified in the configuration passed to {@link #createStorage} 
     */
    public static final Class<? extends Storage> DEFAULT_STORAGE =
            RMIStorageProxy.class;

    /**
     * <p>Construct a storage instance based on the given properties.
     * The properties are also passed to the constructor for the storage.</p>
     *
     * <p>Most interestingly is probably the property {@link Storage#CONF_CLASS}
     * used to specify the class of the storage implementation to use.</p>
     *
     * @param conf setup for the wanted storage along with the
     *        property {@link Storage#CONF_CLASS} which should hold the class-name
     *        for the wanted {@link Storage}. If no storage is specified,
     *        the {@code StorageFactory} defaults to {@link #DEFAULT_STORAGE}.
     * @return an object implementing the {@link Storage} interface.
     * @throws IOException if the storage could not be created.
     */
    public static Storage createStorage(Configuration conf)
            throws IOException {
        return createStorage(conf, Storage.CONF_CLASS);
    }

    /**
     * <p>Construct a storage instance based on the given properties.
     * The properties are also passed to the constructor for the storage.</p>
     *
     * <p>The property {@code classProp} within {@code conf} determines
     * the class used for instantiating the storage.</p>
     *
     * @param conf setup for the wanted storage determined by the
     *             {@code classProp} property wihtin {@code conf}
     * @param classProp the property defining the storage class to instantiate
     * @return an object implementing the {@link Storage} interface.
     * @throws IOException if the storage could not be created.
     */
    public static Storage createStorage(Configuration conf, String classProp)
            throws IOException {
        log.trace("createStorage(conf,prop) called");

        Class<? extends Storage> storageClass;
        try {
            storageClass = Configuration.getClass(classProp,
                                                  Storage.class,
                                                  DEFAULT_STORAGE,
                                                  conf);
        } catch (Exception e) {
            throw new Configurable.ConfigurationException(
                                      "Could not get storage"
                                      + " class from property "
                                      + Storage.CONF_CLASS + ": "
                                      + e.getMessage(), e);
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Instantiating storage class " + storageClass);

        try {
            // FIXME: This forces a RMI call when packing as a service. Not good
            return Configuration.create(storageClass, conf);
        } catch (Exception e) {
            throw new IOException("Failed to instantiate storage class "
                                  + classProp + ": " + e.getMessage(), e);
        }
    }

}



