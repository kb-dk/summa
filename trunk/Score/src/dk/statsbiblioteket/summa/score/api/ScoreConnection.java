/* $Id: ScoreConnection.java,v 1.3 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.3 $
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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.server.ClientManager;
import dk.statsbiblioteket.summa.score.bundle.BundleRepository;
import dk.statsbiblioteket.summa.score.client.Client;
import dk.statsbiblioteket.summa.score.server.ClientDeployer;
import dk.statsbiblioteket.summa.score.api.NoSuchServiceException;
import dk.statsbiblioteket.summa.score.server.ClientDeploymentException;

import java.io.IOException;
import java.util.List;


@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public interface ScoreConnection {    

    public ConfigurationStorage getConfigurationStorage () throws IOException;

    public BundleRepository getRepository () throws IOException;

    /**
     * <p>Get the {@link Configuration} the client with <i>instanceId</i>
     * uses.</p>
     *
     * <p>This method works whether the client is running or not.</p>
     *
     * @param instanceId the id of the client to look up the configuration for
     * @return the configuration used by the client
     */
    /*public Configuration getClientConfiguration (String instanceId);*/

    /**
     * <p>Create a connection to a {@link Client}. If the client is deployed
     * but not running, this method will return {@code null}.</p>
     *
     * <p>If the client is not deployed a {@link NoSuchServiceException} will
     * be thrown.</p>
     *
     * @param instanceId unique id of the client to connect to
     * @return A connection to the client or {@code null} if the client is
     *         known, but not running
     * @throws NoSuchServiceException if the no client with {@code instanceId}
     *                                is known
     */
    public ClientConnection getClient (String instanceId) throws IOException;

    /**
     * <p>Copy a client bundle to a remote machine. The configuration should
     * contain the properties described in {@link ClientDeployer}.</p>
     *
     * <p>What the score server does is to instantiate a {@link ClientDeployer}
     * as described by the configuration's
     * {@link ClientDeployer#DEPLOYER_CLASS_PROPERTY} property.</p>
     *
     * <p>If the {@link ClientDeployer#CLIENT_CONF_PROPERTY} is not set in the
     * configuration the Score server will set it to point at the Score server's
     * configuration server before passing the configuration to the deployer
     * class.</p>
     *
     * <p>The {@link ClientDeployer#DEPLOYER_BUNDLE_PROPERTY} should contain the
     * <i>bundle id</i> of the bundle to deploy. Before passing the configuration
     * to the ClientDeployer's constructor the Score server will replace it
     * with the full path to the bundle file to deploy.</p>
     *
     * <p>The client deployer is passed the same configuration to
     * its constructor as the one passed in this method.</p>
     * @param conf configuration used to instantiate deployer
     * @throws IOException if there is a problem communicating with the deployer
     *                     or if there is a problem deploying the bundle specified
     *                     by the configuration
     * @throws BadConfigurationException if any one of the required configuration
     *                                   parameters are missing
     * @throws ClientDeploymentException if there is an error in the deployment
     *                                   sub system
     */
    public void deployClient (Configuration conf) throws IOException;

    /**
     * <p>Start a client by name with a configuration at a given place.</p>
     *
     * <p>The passed {@link Configuration} {@code conf} will be used
     * to instantiate and configure a {@link ClientDeployer} as described in
     * {@link #deployClient}.</p>
     *
     * @param conf configuration object passed to the client deployer
     * @throws IOException if there is an error communicating with the client
     * @throws NoSuchServiceException if no client with the given client id is
     *                                deployed
     * @throws BadConfigurationException if any one of the required configuration
     *                                   parameters are missing
     */
    public void startClient (Configuration conf) throws IOException;

    /**
     * <p>Stop a running client and all services managed by it.</p>
     *
     * <p>As specified by {@link ClientConnection#stop} this call will
     * stop the JVM of the running client.</p>
     *
     * <p>If the client is not running a warning will be logged, but this method
     * will do nothing.</p>
     *
     * @param instanceId id of the client to stop
     * @throws IOException if there is an error communicating with the client
     * @throws NoSuchServiceException if no client with the given client id is
     *                                deployed
     */
    public void stopClient (String instanceId) throws IOException;

    public List<String> getClients () throws IOException;

}
