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
package dk.statsbiblioteket.summa.control.api;

import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.List;
import java.io.IOException;

/**
 * A connection to a running {@link Client} used to deploy 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface ClientConnection extends Monitorable {

    /** <p>Property defining the id under which the client should report itself
     * via {@link #getId}. This is also known as the client's <i>instance
     * id.</i></p>
     *
     * <p>The client will install itself under
     * <{@code summa.control.client.basepath}>/<{@code summa.control.client.id}></p>
     *
     * <p>The client's RMI service will also run under this name.</p> 
     * */
    public static final String CONF_CLIENT_ID = "summa.control.client.id";

    /** <p>Property defining the relative path under which the client
     * should install itself. The path is relative to the system property
     * {@code user.home} of the client's jvm</p>
     *
     * <p>The client will install itself under
     * {@code <summa.control.client.basepath>/<summa.control.client.id>}.</p>
     *
     * <p>The default value is {@code summa-control}</p>
     */
    public static final String CONF_CLIENT_BASEPATH =
                                                  "summa.control.client.basepath";

    /** <p>Property defining the port on which the client's rmi service
     * should run</p>
     *
     * <p>The default value is defined in {@link #DEFAULT_CLIENT_PORT}</p>
     */
    public static final String CONF_CLIENT_PORT =
                                            "summa.control.client.service.port";

    /**
     * The default value for the {@link #CONF_CLIENT_PORT} 
     */
    public static final int DEFAULT_CLIENT_PORT = 27002;

    /** <p>Property defining the port on which the client should contact or create
     * an rmi registry, see {@link #CONF_REGISTRY_HOST}</p>
     *
     * <p>Default is 27000</p>
     */
    public static final String CONF_REGISTRY_PORT =
                                             "summa.control.client.registry.port";

    /**
     * Default value for {@link #CONF_REGISTRY_PORT}
     */
    public static final int DEFAULT_REGISTRY_PORT = 27000;

    /** <p>Property defining the host on which the client can find the rmi registry.
     * If this is set to {@code localhost} the client will create the registry
     * if it is not already running</p>
     *
     * <p>Default is localhost</p>
     */
    public static final String CONF_REGISTRY_HOST =
                                            "summa.control.client.registry.host";

    /** <p>Property containing the class of {@link dk.statsbiblioteket.summa.control.api.bundle.BundleRepository}
     * a {@link Client} should use for fetching bundles.</p>
     *
     * <p>Default is to use a
     * {@link dk.statsbiblioteket.summa.control.bundle.RemoteURLRepositoryClient}</p>
     */
    public static final String CONF_REPOSITORY_CLASS =
                                          "summa.control.client.repository.class";

    /**
     * Property defining the path uner which persisten files for Clients as
     * well as Services should be stored.
     */
    public static final String CONF_CLIENT_PERSISTENT_DIR =
            Resolver.SYSPROP_PERSISTENT_DIR;

    /**
     * Stop the client specified in the base configuration for the
     * ClientDeployer.
     *
     * This call should stop the JVM of the client. Ie, call {@link System#exit}
     * 
     * @throws IOException in case of communication errors.
     */
    public void stop() throws IOException;

    /**
     * Get the status for the client.
     * @return null if the client could not be contacted, else a client-specific
     * string.
     * @throws IOException in case of communication errors.
     */
    public Status getStatus() throws IOException;

    /**
     * Fetches the service bundle with a given bundle id from the configured
     * {@link dk.statsbiblioteket.summa.control.api.bundle.BundleRepository} and deploys it according to the bundle's
     * configuration and instance id.
     * @param bundleId          The <i>bundle id</i> for the service
     * @param instanceId        The <i>instance id</i> to deploy the service
     *                          under
     * @param configLocation    deploy-specific properties. This should not be
     *                         confused with the properties for
     *                         {@link #startService}, although it is probably
     *                         easiest just to merge the two configurations to
     *                         one.
     * @return the instance id of the deployed service or null on errors
     * @throws IOException in case of communication errors.
     */
    public String deployService(String bundleId,
                                String instanceId,
                                String configLocation)
                              throws IOException;

    /**
     * Remove a given service from the client. If the service is already
     * running it will be stopped with a call to {@link #stopService(String)}
     * @param instanceId the instance if of the service to stop
     * @throws IOException on communication errors
     * @throws NoSuchServiceException if the service {@code instanceId}is not
     *                                installed in the client
     */
    public void removeService(String instanceId) throws IOException;

    /**
     * Start the given service with the given configuration. If the service
     * is not deployed an error is thrown.
     * @param id               the <i>instance id</i> for the service.
     * @param configLocation    service-specific properties.
     * @throws IOException in case of communication errors.
     */
    public void startService(String id, String configLocation)
                             throws IOException;

    /**
     * <p>Stop the given service. If the service is not deployed, an error is
     * thrown. If the service is already stopped, nothing happens.</p>
     *
     * <p>A service is allowed to either exit its JVM or enter the state
     * {@link Status.CODE#stopped} in which all of its connections and
     * pipes should be flushed and closed.</p>
     * @param id               the <i>instance id</i> for the service.
     * @throws IOException in case of communication errors.
     */
    public void stopService(String id) throws IOException;

    /**
     * <p>Get the status for a specific service. If the service is not deployed,
     * an error is thrown.</p>
     * <p>If the service is deployed, but not running an
     * {@link InvalidServiceStateException} is thrown</p>.
     * @param id the <i>instance id</i> for the service.
     * @return   the status for the service.
     * @throws IOException in case of communication errors
     * @throws InvalidServiceStateException if the service is deployed but not
     *                                      running
     * @throws NoSuchServiceException if the service is not known by the client
     */
    public Status getServiceStatus(String id) throws IOException;

    /**
     * Return a connection to a service given by its instance id.
     *
     * @param id the instance id of the service to look up
     * @return a proxy for the service object
     * @throws IOException in case of communication errors
     * @throws InvalidServiceStateException if the service is not running or
     *                                      has a broken connection
     * @throws NoSuchServiceException if the client does not know of a service
     *                                with the id {@code id}.
     */
    public Service getServiceConnection (String id) throws IOException;

    /**
     * Iterate through the deployed services and collect a list of the ids
     * for said services.
     * @return a list of all the deployed services.
     * @throws IOException in case of communication errors.
     */
    public List<String> getServices() throws IOException;

    /**
     * Return the id of the client as set through the configuration property
     * {@link #CONF_CLIENT_ID}
     * @return the id
     * @throws IOException in case of communication errors.
     */
    public String getId() throws IOException;

    /**
     * Get the {@link dk.statsbiblioteket.summa.control.api.bundle.BundleRepository} the client uses.
     */
    public BundleRepository getRepository () throws IOException;

    /**
     * Get the contents of the bundle spec of a deployed service.
     * If {@code instanceId} is {@code null} the bundle spec of the
     * running client will be returned
     * @param instanceId the instance id of the bundle to inspect, or
     *        {@code null} to get the bundle spec for the running client
     * @return the contents of the bundle spec file
     * @throws IOException on communication errors
     * @throws NoSuchServiceException if the client does not know of a service
     *                                with the id {@code instanceId}
     */
    public String getBundleSpec (String instanceId) throws IOException;

    /**
     * Report to the client that the given service is acting up and
     * that it should check its connection to the service.
     * @param id the service to report
     * @throws IOException on communication errors with the client
     * @throws NoSuchServiceException if a service by the given id doesn't
     *                                exist
     */
    public void reportError (String id) throws IOException;
}



