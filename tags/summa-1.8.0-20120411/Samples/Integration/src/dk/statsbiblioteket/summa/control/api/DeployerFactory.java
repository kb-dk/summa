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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.server.deploy.SSHDeployer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class used to instantiate {@link dk.statsbiblioteket.summa.control.api.feedback.Feedback} objects of the right type.
 */
public class DeployerFactory {

    private static final Log log = LogFactory.getLog(DeployerFactory.class);

    /**
     * Instantiate a new {@link ClientDeployer} based on the the
     * {@link ClientDeployer#CONF_DEPLOYER_CLASS} of {@code conf}.
     * If this property is not defined the factory will default to using a
     * {@link SSHDeployer}.
     *
     * @param conf the configuration used to look up the
     *             {@link ClientDeployer#CONF_DEPLOYER_CLASS} property
     * @return a newly instantiated {@link ClientDeployer}
     */
    public static ClientDeployer createClientDeployer (Configuration conf) {
        log.debug("Creating deployer from class: "
                  + conf.getString(ClientDeployer.CONF_DEPLOYER_CLASS));
        Class<? extends ClientDeployer> deployerClass =
                           conf.getClass(ClientDeployer.CONF_DEPLOYER_CLASS,
                                         ClientDeployer.class,
                                         SSHDeployer.class);
        ClientDeployer deployer = Configuration.create(deployerClass, conf);

        return deployer;
    }

}




