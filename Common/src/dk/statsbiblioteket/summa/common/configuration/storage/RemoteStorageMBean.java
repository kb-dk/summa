/* $Id: RemoteStorageMBean.java,v 1.4 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:21 $
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
package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.Serializable;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

/**
 * Interface for exposing a remote {@link ConfigurationStorage} both as a JMX
 * bean and rmi service.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface RemoteStorageMBean extends Remote, ConfigurationStorage {

    /**
     * Property defining which port to export the RMI service on.
     * Default is 27007.
     */
    public static final String PROP_PORT = "summa.configuration.service.port";

    /**
     * Property defining what name to bind the exposed RMI service under.
     * Default is {@code configurationStorage}.
     */
    public static final String PROP_NAME = "summa.configuration.service.name";

    /**
     * Property defining on what port the registry runs. Default is
     * 27000.
     */
    public static final String PROP_REGISTRY_PORT = "summa.configuration.registry.port";

    /**
     * Property defining the name of the host on which the registry runs.
     * Default is {@code localhost}. 
     */
    public static final String PROP_REGISTRY_HOST = "summa.configuration.registry.host";

    public void put(String key, Serializable value) throws RemoteException;

    public Serializable get(String key) throws RemoteException;

    public Iterator<Map.Entry<String,Serializable>> iterator() throws
                                                               RemoteException;

    public void purge(String key) throws RemoteException;

    public String[] getConfigDump() throws RemoteException;

    public boolean supportsSubStorage() throws RemoteException;

    public ConfigurationStorage getSubStorage(String key) throws
                                                          RemoteException;

    public ConfigurationStorage createSubStorage(String key) throws
                                                             RemoteException;

    public List<ConfigurationStorage> createSubStorages(String key, int count)
                                                        throws RemoteException;

    public List<ConfigurationStorage> getSubStorages(String key) throws
                                                                RemoteException;
}
