/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
    public static final String CONF_PORT = "summa.configuration.service.port";

    /**
     * Property defining what name to bind the exposed RMI service under.
     * Default is {@code configurationStorage}.
     */
    public static final String CONF_NAME = "summa.configuration.service.name";

    /**
     * Property defining on what port the registry runs. Default is
     * 27000.
     */
    public static final String CONF_REGISTRY_PORT = "summa.configuration.registry.port";

    /**
     * Property defining the name of the host on which the registry runs.
     * Default is {@code localhost}. 
     */
    public static final String CONF_REGISTRY_HOST = "summa.configuration.registry.host";

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




