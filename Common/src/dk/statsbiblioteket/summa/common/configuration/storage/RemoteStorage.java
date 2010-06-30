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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.AccessException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.io.Serializable;
import java.io.IOException;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A decorator class for {@link ConfigurationStorage}s to expose their methods
 * over JMX and rmi.
 *
 * To use this class you typically bootstrap it with a {@link FileStorage} backed
 * {@link Configuration}. The remote storage will read its own configuration from
 * the provided {@code Configuration} as well as expose it remotely.
 *
 * <p>Example:
 * <p><blockquote><pre>
 *     ConfigurationStorage backend = new FileStorage ("configuration.xml");
 *     Configuration conf = new Configuration (backend);
 *
 *     // Expose backend over rmi+jmx
 *     ConfigurationStorage remoteStorage = new RemoteStorage (conf);
 * </pre></blockquote>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Some method-documentations misses parts")
public class RemoteStorage extends UnicastRemoteObject implements
                                                       RemoteStorageMBean {
    public static final long serialVersionUID = 34581040194821L;
    private final Log log = LogFactory.getLog(RemoteStorage.class);

    private String serviceName, registryHost;
    private int servicePort, registryPort;
    private Configuration conf;
    private ConfigurationStorage storage;

    /**
     * Export the {@link ConfigurationStorage} of a {@link Configuration} over
     * RMI.
     *
     * FIXME: Should we export a JMX iface as well?
     *
     * @param conf the configuration to read the rmi configuration from and
     *                         expose remotely as well.
     * @throws RemoteException if there is an error exporting the interface
     */
    public RemoteStorage (Configuration conf) throws RemoteException {
        super (conf.getInt(CONF_PORT));
        this.conf = conf;
        storage = conf.getStorage();
        serviceName = conf.getString(CONF_NAME, "configurationStorage");
        servicePort = conf.getInt (CONF_PORT, 27007);
        registryPort = conf.getInt (CONF_REGISTRY_PORT, 27000);
        registryHost = conf.getString (CONF_REGISTRY_HOST, "localhost");

        exportRMIInterface();
    }

    /**
     * Change the underlying storage provider.
     * @param storage new {@link ConfigurationStorage} to use
     */
    public void setStorage (ConfigurationStorage storage) {
        this.storage = storage;
    }

    /**
     * Convenience method to create a connection to a remote configuration
     * storage
     * @param rmiPath Path to remote service, fx {@code //host:port/servicename}
     * @return A proxy for the remote configuration storage 
     * @throws NotBoundException if name is not currently bound
     * @throws RemoteException if registry could not be contacted
     * @throws MalformedURLException if the name is not an appropriately
     */
    public static ConfigurationStorage getRemote (String rmiPath)
              throws MalformedURLException, NotBoundException, RemoteException {
        return (ConfigurationStorage) Naming.lookup (rmiPath);
    }

    public String getServiceUrl () {
        return "//" + registryHost + ":" + registryPort + "/" + serviceName;
    }

    /**
     * Exports this storage interface over RMI.
     *
     * @throws java.rmi.RemoteException if export fails.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private void exportRMIInterface() throws RemoteException {
        Registry reg = null;

        try {
            if ("localhost".equals(registryHost)) {
                reg = LocateRegistry.createRegistry(registryPort);
                log.info("Created registry on port " + servicePort);
            }

        } catch (RemoteException e) {
            try {
                reg = LocateRegistry.getRegistry(registryHost, registryPort);
                log.info ("Found registry " + registryHost + ":" + registryPort);
            } catch (RemoteException ee) {
                log.error("Failed to locate or create registry: ", e);
                throw ee;
            }
        }

        if (reg == null) {
            throw new RemoteException ("Failed to locate or create registry on "
                                       + registryHost + ":" + registryPort);
        }

        try {
            reg.rebind(serviceName, this);
        } catch (AccessException ee) {
            String error = "Failed to connect to registry with '"
                           + serviceName + "'";
            log.error(error, ee);
            throw new RemoteException(error, ee);
        } catch (RemoteException ee) {
            String error = "Failed to bind in registry with '"
                           + serviceName + "'";
            log.error(error, ee);
            throw new RemoteException(error, ee);
        }

        log.info(getClass().getSimpleName() + " bound in registry as '"
                 + serviceName + "' on port "
                 + servicePort);                 
    }

    public void put(String key, Serializable value) throws RemoteException {
        try {
            storage.put (key, value);
        } catch (IOException e) {
            throw new RemoteException (e.toString());
        }
    }

    public Serializable get(String key) throws RemoteException {
        try {
            return storage.get (key);
        } catch (IOException e) {
            throw new RemoteException (e.toString());
        }
    }

    public Iterator<Map.Entry<String, Serializable>> iterator() throws RemoteException {
        try {
            return storage.iterator();
        } catch (IOException e) {
            throw new RemoteException (e.toString());
        }
    }

    public void purge (String key) throws RemoteException {
        try {
            storage.purge(key);
        } catch (IOException e) {
            throw new RemoteException (e.toString());
        }
    }

    public int size () throws RemoteException {
        try {
            return storage.size();
        } catch (IOException e) {
            throw new RemoteException (e.toString());
        }
    }

    public boolean supportsSubStorage() throws RemoteException {
        try {
            return storage.supportsSubStorage();
        } catch (IOException e) {
            throw new RemoteException(e.toString());
        }
    }

    // TODO: Can we return a RemoteStorage here?
    public ConfigurationStorage createSubStorage(String key) throws
                                                          RemoteException {
        try {
            return storage.createSubStorage(key);
        } catch (IOException e) {
            throw new RemoteException (e.toString(), e);
        }
    }

    // TODO: Can we return a RemoteStorage here?
    public ConfigurationStorage getSubStorage(String key) throws
                                                          RemoteException {
        try {
            return storage.getSubStorage(key);
        } catch (IOException e) {
            throw new RemoteException (e.toString(), e);
        }
    }

    public List<ConfigurationStorage> createSubStorages(String key, int count)
                                                        throws RemoteException {
        try {
            return storage.createSubStorages(key, count);
        } catch (IOException e) {
            throw new RemoteException (e.toString(), e);
        }
    }

    public List<ConfigurationStorage> getSubStorages(String key) throws
                                                               RemoteException {
        try {
            return storage.getSubStorages(key);
        } catch (IOException e) {
            throw new RemoteException (e.toString(), e);
        }
    }

    public String[] getConfigDump() {
        return conf.dump();
    }

    /**
     * Run a RemoteStorage. This should not be used for production servers,
     * only for testing.
     *
     * The storage exported will be the underlying storage of the specified
     * system configuration. See {@link Configuration#getSystemConfiguration()}
     * for details on how to set this.
     *
     * @param args You must pass two integer arguments on the command line,
     *             namely {@code registryPort} and {@code servicePort} in that
     *             order.
     * @throws Exception if there is an error setting up the remote interface
     */
    public static void main (String[] args) {
        System.out.println("Starting configuration server");

        Configuration conf = Configuration.getSystemConfiguration();

        try {
            RemoteStorage remote = new RemoteStorage(conf);
            System.out.println ("Configuration server running on "
                                + remote.getServiceUrl() + ", with service"
                                + " on port " + conf.getString(CONF_PORT));
        } catch (Throwable t) {
            System.err.println ("Configuration server  encountered an error."
                               + " Bailing out.");
            t.printStackTrace();
            System.exit(1);
        }

    }
}




