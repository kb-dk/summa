/* $Id: ClientDeployer.java,v 1.7 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.7 $
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
package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.api.Feedback;
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.api.ScoreConnection;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Abstract representation of a way of deploying and controlling ClientManager Clients.
 * Example {@code ClientDeployer}s could include <i>ssh</i>,
 * <i>local deployment</i>, <i>rmi</i>, etc.
 * </p><p>
 * The ClientDeployer is responsible maintaining or reestablishing contact
 * with clients, in order to perform start or stop.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface ClientDeployer extends Configurable {
    
    /**
     * <p>Configuration property containing the instance id for the client to
     * deploy.</p>
     *
     * <p>This property <i>must</i> be provided by the configuration.</p>
     *
     * @see ClientConnection#CLIENT_ID 
     */
    public static final String INSTANCE_ID_PROPERTY =
                                                     ClientConnection.CLIENT_ID;

    /**
     * <p>Configuration property defining a path relative to the system
     * property {@code user.home} (of the client side JVM) where to install
     * the client bundle.</p>
     *
     * <p>Default is {@code summa-score}.</p>
     *
     * @see ClientConnection#CLIENT_BASEPATH
     */
    public static final String BASEPATH_PROPERTY =
                                               ClientConnection.CLIENT_BASEPATH;

    /**
     * <p>Configuration property defining the class name of the
     * {@link ClientDeployer} implementation to use when deploying the
     * client.</p>
     *
     * <p>This property <i>must</i> be provided by the configuration.</p>
     */
    public static final String DEPLOYER_CLASS_PROPERTY =
                                                  "summa.score.deployer.class";

    /**
     * <p>Configuration property containing a freeform string describing the
     * target to deploy to.</p>
     *
     * <p>For example the
     * {@link dk.statsbiblioteket.summa.score.server.deploy.SSHDeployer} this
     * should be {@code user@hostname}.</p>
     *
     * <p>This property <i>must</i> be provided by the configuration.</p>
     */
    public static final String DEPLOYER_TARGET_PROPERTY =
                                                  "summa.score.deployer.target";

    /**
     * <p>Configuration property defining the system property where the client is
     * to look up a {@link Configuration} with.</p>
     *
     * <p>If this property is not set the {@link ScoreCore} will set it to
     * point at the Score's configuration server before passing the configuration
     * to {@link ClientDeployer}'s constructor.</p>
     *
     * {@link Configuration#getSystemConfiguration()}. 
     */
    public static final String CLIENT_CONF_PROPERTY =
                                                  Configuration.CONFIGURATION_PROPERTY;

    /**
     * <p>Configuration property defining the bundle to deploy. The
     * {@link Configuration} passed to {@link ScoreConnection#deployClient}
     * or {@link ScoreConnection#startClient} should contain the
     * <i>bundle id</i> to use.</p>
     *
     * <p>The Score server will replace the bundle id with the full path
     * to the local bundle file before passing the {@link Configuration} to the
     * {@link ClientDeployer}s constructor.</p>
     *
     * <p>This property <i>must</i> be provided by the configuration.</p>
     */
    public static final String DEPLOYER_BUNDLE_PROPERTY =
                                                  "summa.score.deployer.bundle";

    /**
     * <p>Configuration property naming the class used to provide user
     * feedback.</p>
     *
     * <p>This property <i>must</i> be provided by the configuration.</p>
     */
    public static final String DEPLOYER_FEEDBACK_PROPERTY =
                                          "summa.score.deployer.feedback.class";

    /**
     * Deploy the client, as specified in the {@link Configuration} supplied
     * in the ClientDeployer's constructor.
     * @param feedback callback for communication with the user.
     * @throws Exception if something goes wrong during deploy.
     */
    public void deploy (Feedback feedback) throws Exception;

    /**
     * Start the client specified in the base configuration for the
     * ClientDeployer.
     * @param feedback callback for communication with the user.
     * @throws Exception if something goes wrong during start.
     */
    public void start (Feedback feedback) throws Exception;

    /**
     * Get the hostname of the target to deploy to. The Score server needs this
     * to be able to construct the address to contact the Client at.
     * @return hostname of the target machine
     */
    public String getTargetHost ();
}
