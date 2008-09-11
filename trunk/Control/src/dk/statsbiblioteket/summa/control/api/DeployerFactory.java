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



