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
package dk.statsbiblioteket.summa.control.api;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.feedback.Feedback;
import dk.statsbiblioteket.summa.control.bundle.BundleStub;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Abstract representation of a way of deploying and controlling
 * {@link dk.statsbiblioteket.summa.control.client.Client}s.
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
     * @see ClientConnection#CONF_CLIENT_ID 
     */
    public static final String CONF_INSTANCE_ID =
            ClientConnection.CONF_CLIENT_ID;

    /**
     * <p>Configuration property defining a path relative to the system
     * property {@code user.home} (of the client side JVM) where to install
     * the client bundle.</p>
     *
     * <p>Default is {@code summa-control}.</p>
     *
     * @see ClientConnection#CONF_CLIENT_BASEPATH
     */
    public static final String CONF_BASEPATH =
            ClientConnection.CONF_CLIENT_BASEPATH;

    /**
     * <p>Configuration property defining the class name of the
     * {@link ClientDeployer} implementation to use when deploying the
     * client.</p>
     *
     * <p>This property <i>must</i> be provided by the configuration.</p>
     */
    public static final String CONF_DEPLOYER_CLASS =
            "summa.control.deployer.class";

    /**
     * <p>Configuration property containing a freeform string describing the
     * target to deploy to.</p>
     *
     * <p>For example the
     * {@link dk.statsbiblioteket.summa.control.server.deploy.SSHDeployer} this
     * should be {@code [user@]hostname[:port]}.</p>
     *
     * <p>This property <i>must</i> be provided by the configuration.</p>
     */
    public static final String CONF_DEPLOYER_TARGET =
            "summa.control.deployer.target";

    /**
     * <p>Configuration property defining the system property where the client is
     * to look up a {@link Configuration} with.</p>
     *
     * <p>If this property is not set the
     * {@link dk.statsbiblioteket.summa.control.server.ControlCore} will set it
     * to {@code configuration.xml} before passing the configuration to
     * {@link ClientDeployer}'s constructor.</p>
     *
     * {@link Configuration#getSystemConfiguration()}. 
     */
    public static final String CONF_CLIENT_CONF =
            Configuration.CONF_CONFIGURATION_PROPERTY;

    /**
     * <p>Configuration property defining the bundle to deploy. The
     * {@link Configuration} passed to {@link ControlConnection#deployClient}
     * or {@link ControlConnection#startClient} should contain the
     * <i>bundle id</i> to use.</p>
     *
     * <p>This property <i>must</i> be provided by the configuration.</p>
     */
    public static final String CONF_DEPLOYER_BUNDLE =
                                             "summa.control.deployer.bundle.id";

    /**
     * Configuration property set by the Control server. It contains the absolute
     * path to the .bundle file to deploy.
     */
    public static final String CONF_DEPLOYER_BUNDLE_FILE =
                                           "summa.control.deployer.bundle.file";

    /**
     * <p>Configuration property naming the class used to provide user
     * feedback.</p>
     *
     * <p>This property <i>must</i> be provided by the configuration.</p>
     */
    public static final String CONF_DEPLOYER_FEEDBACK =
                                        "summa.control.deployer.feedback.class";

    /**
     * <p>Deploy the client, as specified in the {@link Configuration} supplied
     * in the ClientDeployer's constructor.</p>
     *
     * <p>It is the responsibility of the deployer to ensure that
     * JMX files and policy have proper file permissions. Ie only the owner
     * can read them. These files are:
     * <ul>
     *   <li>${@link BundleStub#POLICY_FILE}</li>
     *   <li>${@link BundleStub#JMX_ACCESS_FILE}</li>
     *   <li>${@link BundleStub#JMX_PASSWORD_FILE}</li>
     * </ul>
     * </p>
     * @param feedback callback for communication with the user.
     * @throws Exception if something goes wrong during deploy.
     */
    public void deploy (Feedback feedback) throws Exception;

    /**
     * <p>Start the client specified in the base configuration for the
     * ClientDeployer.</p>
     *
     * <p>It is the reponsibility of the deployer to set the required system
     * properties for the client. Besides those written in the client's spec
     * file the following two properties must also be set
     * <ul>
     *   <li>summa.configuration</li>
     *   <li>summa.control.client.id</li>
     * </ul></p>
     *
     * @param feedback callback for communication with the user.
     * @throws Exception if something goes wrong during start.
     */
    public void start (Feedback feedback) throws Exception;

    /**
     * Get the hostname of the target to deploy to. The Control server needs this
     * to be able to construct the address to contact the Client at.
     * @return hostname of the target machine
     */
    public String getTargetHost ();
}




