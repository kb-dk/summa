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
package dk.statsbiblioteket.summa.control.server;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.control.api.BadConfigurationException;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

/**
 * Various util methods for general use in the {@link ControlCore} and related
 * classes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "mke")
public class ControlUtils {
    private static final Log log = LogFactory.getLog (ControlUtils.class);

    /** Class of the SSH deployer. */
    public static final String SSHDEPLOYER =
                  "dk.statsbiblioteket.summa.control.server.deploy.SSHDeployer";
    /** Class of the Local deployer. */
    public static final String LOCALDEPLOYER =
                "dk.statsbiblioteket.summa.control.server.deploy.LocalDeployer";

    /**
     * <p>Work out what base dir to use given a configuration. This is
     * extracted from the {@link ControlCore#CONF_CONTROL_BASE_DIR} property.
     * </p>
     *
     * <p>If the directory does not exist it will be created.</p>
     *
     * @param conf Configuration to extract the properties from.
     * @return Base directory for installation.
     */
    public static File getControlBaseDir(Configuration conf) {
        String basePath = conf.getString(ControlCore.CONF_CONTROL_BASE_DIR,
                                          System.getProperty("user.home")
                                        + File.separator
                                        + "summa-control");
        File baseDir = new File(basePath);
        if (!baseDir.exists()) {
            if (baseDir.mkdirs()) {
                log.debug("Created control base directory '" + baseDir + "'");
            }
        }

        log.trace("Calculated control base dir '" + baseDir + "'");
        return baseDir;
    }    

    /**
     * <p>Work out what default base directory to deploy clients to. This is
     * extracted from the {@link ClientConnection#CONF_CLIENT_BASEPATH}
     * property.</p>
     *
     * Note that system properties enclosed in <code>${user.home}</code> will
     * <i>not</i> be escaped. This way one can pass the property into a remote
     * configuration and have it magically work.
     *
     * @param conf Configuration to extract the properties from.
     * @return Base directory for installation, without system properties
     *         escaped.
     */
    public static String getClientBasePath(Configuration conf) {
        // We want the string without any ${}s escaped, so we fetch it directly
        // from the underlying configuration storage
        ConfigurationStorage storage = conf.getStorage();
        String basePath = null;

        try {
            basePath = (String)
                             storage.get(ControlCore.CONF_CLIENT_BASE_DIR);
        } catch (IOException e) {
            log.warn("Error reading default client base path from config: "
                     + e.getMessage(), e);
        }

        if (basePath == null) {
            basePath = "${user.home}" + File.separator + "summa-control";
        }

        log.trace("Calculated default client base dir '" + basePath + "'");
        return basePath;
    }

    /**
     * <p>Get the directory used to store incoming bundles uploaded by the
     * control administrator(s).</p>
     *
     * <p>If the directory does not exist it will be created.</p>
     *
     * @param conf Configuration to extract the properties from.
     * @return File pointing at the directory for incoming bundles.
     */
    public static File getIncomingDir(Configuration conf) {
        String incPath = conf.getString(RepositoryManager.CONF_INCOMING_DIR,
                                        new File(getControlBaseDir(conf),
                                                   "incoming").toString());

        File incDir = new File(incPath);
        if (!incDir.exists()) {
            if (incDir.mkdirs()) {
                log.debug("Created dir for incoming bundles '" + incDir + "'");
            }
        }
        return incDir;
    }

    /**
     * Get the address the repository is exposed on. This is the address
     * clients will use to download bundles from.
     * 
     * @param conf The configuration.
     * @return The repository address exposed according to the configuration.
     */
    public static String getRepositoryAddress(Configuration conf) {
        return conf.getString(BundleRepository.CONF_REPO_ADDRESS,
                              "http://"
                              + RemoteHelper.getHostname() + ":8080"
                              + "/summa-control/repo");
    }

    /**
     * Translate abbreviated deployer class name to fully qualified class name.
     *
     * @param shortDesc Deployer abbrev.
     * @return The fully qualified class name.
     * @throws BadConfigurationException If the deployer class is not known.
     */
    public static String getDeployerClassName(String shortDesc) {
        if ("ssh".equals (shortDesc)) {
            return SSHDEPLOYER;
        } else if ("local".equals (shortDesc)) {
            return LOCALDEPLOYER;
        } else {
            throw new BadConfigurationException("Unknown deployment transport "
                                               + "'" + shortDesc + "'");
        }
    }
}




