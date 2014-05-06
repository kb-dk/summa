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
package dk.statsbiblioteket.summa.control.client;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import dk.statsbiblioteket.summa.control.api.BadConfigurationException;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A helper class for the {@link Client} to manage a collection of
 * {@link Service}s.
 */
public class ServiceManager extends ConnectionManager<Service>
                            implements Configurable, Iterable<String> {
    /** Local log instance. */
    private static Log log = LogFactory.getLog(ServiceManager.class);

    /**
     * Configuration property defining the class of the
     * {@link ConnectionFactory} to use for creating service connections.
     */
    public static final String CONF_CONNECTION_FACTORY =
                                          GenericConnectionFactory.CONF_FACTORY;
    /** The client ID. */
    private String clientId;
    /** The base path. */
    private String basePath;
    /** The service path. */
    private String servicePath;
    /** The registry port. */
    private int registryPort;

    /**
     * Constructs a service manager from the configuration.
     * @param conf The configuration.
     */
    public ServiceManager(Configuration conf) {
        super (getConnectionFactory(conf));

        registryPort = conf.getInt(Client.CONF_REGISTRY_PORT,
                                   Client.DEFAULT_REGISTRY_PORT);
        clientId = System.getProperty(Client.CONF_CLIENT_ID);

        if (clientId == null) {
            throw new BadConfigurationException("System property '"
                                                + Client.CONF_CLIENT_ID
                                                + "' not set");
        }
        basePath = System.getProperty("user.dir");
        servicePath = basePath + File.separator + "services";
    }

    @Override
    public ConnectionContext<Service> get(String serviceId) {
        String address = getServiceAddress(serviceId);
        log.trace("Getting address for '" + serviceId + "': " + address + "");
        return super.get(address);
    }

    /**
     * Return a connection factory.
     * @param conf The configuration.
     * @return A connection factory.
     */
    @SuppressWarnings("unchecked")
    private static ConnectionFactory<? extends Service>
                                      getConnectionFactory(Configuration conf) {
        Class<? extends ConnectionFactory> connFactClass =
                conf.getClass(CONF_CONNECTION_FACTORY, ConnectionFactory.class,
                              SummaRMIConnectionFactory.class);

        ConnectionFactory connFact = Configuration.create(connFactClass, conf);
        connFact.setGraceTime(1);
        connFact.setNumRetries(2);

        return (ConnectionFactory<Service>) connFact;
    }

    /**
     * Register an instance ID. This is a no-op currently, so this call doesn't
     * do anything.
     * @param instanceId The instance ID.
     */
    public void register(String instanceId) {
        if (instanceId == null) {
            throw new NullPointerException("Trying to register service with id"
                                           + " 'null'");
        }
        log.debug("Currently regsiter() is a no-op");
    }

    /**
     * Returns an iterator over all services.
     * @return An iterator of all services.
     */
    @Override
    public Iterator<String> iterator() {
        return getServices().iterator();
    }

    /**
     * Return true if the serviceID is known.
     * @param instanceId The serviceID.
     * @return True if the service is known.
     */
    public boolean knows(String instanceId) {
        return getServiceFile(instanceId).exists();
    }

    /**
     * Return the service directory.
     * @param serviceId The service ID.
     * @return The service directory.
     */
    public File getServiceDir(String serviceId) {
        return new File(servicePath, serviceId);
    }

    /**
     * Return the service address.
     * @param serviceId The service ID.
     * @return the service address.
     */
    public String getServiceAddress(String serviceId) {
        if (serviceId == null) {
            throw new NullPointerException("Trying to retrieve service address "
                                           + "for service id 'null'");
        }
        return "//localhost:" + registryPort + "/" + serviceId;
    }

    /**
     * Return the service file.
     * @param serviceId A service ID.
     * @return The service file.
     */
    public File getServiceFile(String serviceId) {
        return new File(getServiceDir(serviceId), "service.xml");
    }

    /**
     * Return the bundle ID as string.
     * @param serviceId The service ID.
     * @return The bundle ID.
     */
    public String getBundleId(String serviceId) {
        return getBundleSpec(serviceId).getBundleId();
    }

    /**
     * Return the bundle specification.
     * @param serviceId The service ID, for which bundle specification should
     * be returned.
     * @return The bundle specification.
     */
    public BundleSpecBuilder getBundleSpec(String serviceId) {
        File bundleFile = getServiceFile(serviceId);
        try {
            return BundleSpecBuilder.open(bundleFile);
        } catch (IOException e) {
            log.warn ("Failed to read bundle file for " + serviceId, e);
            return null;
        }
    }

    /**
     * Return the services lists.
     * @return All services.
     */
    public List<String> getServices() {
        String[] serviceFiles = new File(servicePath).list();
        return new ArrayList<>(Arrays.asList(serviceFiles));
    }
}

