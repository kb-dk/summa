/* $Id: ClientConnection.java,v 1.15 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.15 $
 * $Date: 2007/10/11 12:56:25 $
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
package dk.statsbiblioteket.summa.score.api;

import dk.statsbiblioteket.summa.score.client.Client;
import dk.statsbiblioteket.summa.score.bundle.BundleRepository;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.List;
import java.io.IOException;

/**
 * A connection to a running {@link Client} used to deploy 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface ClientConnection {

    /** <p>Property defining the id under which the client should report itself
     * via {@link #getId}. This is also known as the client's <i>instance
     * id.</i></p>
     *
     * <p>The client will install itself under
     * <{@code summa.score.client.basepath}>/<{@code summa.score.client.id}></p>
     *
     * <p>The client's RMI service will also run under this name.</p> 
     * */
    public static final String CLIENT_ID = "summa.score.client.id";

    /** <p>Property defining the relative path under which the client
     * should install itself. The path is relative to the system property
     * {@code user.home} of the client's jvm</p>
     *
     * <p>The client will install itself under
     * {@code <summa.score.client.basepath>/<summa.score.client.id>}.</p>
     *
     * <p>The default value is {@code summa-score}</p>
     */
    public static final String CLIENT_BASEPATH_PROPERTY =
                                                  "summa.score.client.basepath";

    /** <p>Property defining the port on which the client's rmi service
     * should run</p>
     *
     * <p>Default is 27002</p>
     */
    public static final String SERVICE_PORT_PROPERTY =
                                            "summa.score.client.service.port";

    /** <p>Property defining the port on which the client should contact or create
     * an rmi registry, see {@link #REGISTRY_HOST_PROPERTY}</p>
     *
     * <p>Default is 27000</p>
     */
    public static final String REGISTRY_PORT_PROPERTY =
                                             "summa.score.client.registry.port";

    /** <p>Property defining the host on which the client can find the rmi registry.
     * If this is set to {@code localhost} the client will create the registry
     * if it is not already running</p>
     *
     * <p>Default is localhost</p>
     */
    public static final String REGISTRY_HOST_PROPERTY =
                                            "summa.score.client.registry.host";

    /** <p>Property containing the class of {@link dk.statsbiblioteket.summa.score.bundle.BundleRepository}
     * a {@link Client} should use for fetching bundles.</p>
     *
     * <p>Default is to use a
     * {@link dk.statsbiblioteket.summa.score.bundle.URLRepository}</p>
     */
    public static final String REPOSITORY_CLASS_PROPERTY =
                                          "summa.score.client.repository.class";

    /**
     * Property defining the path uner which persisten files for Clients as
     * well as Services should be stored.
     */
    public static final String CLIENT_PERSISTENT_DIR_PROPERTY =
                                            "summa.score.client.persistent.dir";

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
     * {@link BundleRepository} and deploys it according to the bundle's
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
     * <p>If the service is deployed, but not running the returned status code
     * will be {@link Status.CODE#not_instantiated}</p>.
     * @param id the <i>instance id</i> for the service.
     * @return   the status for the service.
     * @throws IOException in case of communication errors.
     */
    public Status getServiceStatus(String id) throws IOException;

    /**
     * Iterate through the deployed services and collect a list of the ids
     * for said services.
     * @return a list of all the deployed services.
     * @throws IOException in case of communication errors.
     */
    public List<String> getServices() throws IOException;

    /**
     * Return the id of the client as set through the configuration property
     * {@link #CLIENT_ID}
     * @return the id
     * @throws IOException in case of communication errors.
     */
    public String getId() throws IOException;

}
