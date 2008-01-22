/* $Id: SSHDeployer.java,v 1.8 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.8 $
 * $Date: 2007/10/04 13:28:20 $
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
package dk.statsbiblioteket.summa.score.server.deploy;

import java.io.File;

import dk.statsbiblioteket.summa.score.server.ClientDeployer;
import dk.statsbiblioteket.summa.score.server.ClientDeploymentException;
import dk.statsbiblioteket.summa.score.api.Feedback;
import dk.statsbiblioteket.summa.score.api.Message;
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.NativeRunner;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>{@link ClientDeployer} that uses ssh to copy and start Clients.</p>
 * 
 * $Id: SSHDeployer.java,v 1.8 2007/10/04 13:28:20 te Exp $
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te, mke",
        comment="mke made substantial changes over te's original impl")
public class SSHDeployer implements ClientDeployer {
    private static final Log log = LogFactory.getLog(SSHDeployer.class);

    private String login;
    private String destination;
    private String source;
    private String clientId;
    private String confLocation;

    protected Configuration configuration;

    public SSHDeployer(Configuration conf) {
        login = conf.getString(DEPLOYER_TARGET_PROPERTY);
        destination = conf.getString(BASEPATH_PROPERTY, "summa-score");
        source = conf.getString(DEPLOYER_BUNDLE_PROPERTY);
        clientId = conf.getString (INSTANCE_ID_PROPERTY);
        confLocation = conf.getString (CLIENT_CONF_PROPERTY,
                                       "configuration.xml");

        destination += File.separator + clientId;
    }

    public void deploy(Feedback feedback) throws Exception {
        log.info("Deploying client");

        /* Make sure target dir exists */
        makeDestination(login, destination);

        /* Copy package to destination */
        log.debug("Deploying from " + source + " to " + destination);
        try {
            NativeRunner runner =
                    new NativeRunner(new String[]{"scp", source,
                                                  login + ":" + destination});
            runner.execute(50000, 50000);
        } catch(Exception e) {
            String error = "Could not deploy from source '" + source
                           + "' to destination '" + destination + "'";
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new ClientDeploymentException(error, e);
        }
        log.debug("Deployed from " + source + " to " + destination);

        /* Calculate archive name */
        String archive = source;
        if (source.lastIndexOf(File.separator) > 0) {
            archive = source.substring(source.lastIndexOf(File.separator) + 1);
        }
        String archivePath = destination + File.separator + archive;

        /* Unpack */
        log.debug("Unpacking '" + archivePath + "' with login '" + login + "'");
        NativeRunner runner =
                new NativeRunner(new String[]{"ssh", login,
                                              "cd", destination,
                                              ";", "unzip", "-u",
                                              archive});
        String error = null;
        try {
            int returnValue = runner.execute(50000, 50000);
            if (returnValue != 0) {
                error = "Could not unpack '" + archivePath + "' with login '"
                        + login + "'. Got return value "
                        + returnValue + " and message:\n\t"
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Could not unpack archive '" + archivePath + "' with login '"
                    + login + "': " + e.getMessage() + "\n\n\t"
                    + runner.getProcessErrorAsString();
            log.error(error, e);
        }
        if (error != null) {
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new ClientDeploymentException(error);
        }
        log.debug("Unpacked " + archive + " at " + destination
                  + " with login " + login);

        /* Clean up */
        log.debug("Deleting " + archivePath + " at " + destination
                  + " with login " + login);
        runner =
                new NativeRunner(new String[]{"ssh", login,
                                              "cd", destination,
                                              ";", "rm", "-f",
                                              archive});
        error = null;
        try {
            int returnValue = runner.execute(50000, 50000);
            if (returnValue != 0) {
                error = "Could not delete '" + archivePath + "' with login '"
                        + login + "'. Got return value "
                        + returnValue + " and message:\n\t"
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Could not delete '" + archivePath + "' with login '"
                    + login + "': " + e.getMessage() + "\n\n\t"
                    + runner.getProcessErrorAsString();
            log.error(error, e);
        }
        if (error != null) {
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new ClientDeploymentException(error);
        }
        log.debug("Deleted '" + archivePath + "' with login " + login);

        log.info("Finished deploy of " + source + " to " + destination);
        /*
       scp foo.zip bar@zoo:/path/to/somewhere
       ssh bar@zoo unzip /path/to/somewhere/foo.zip
        */
    }

    /**
     * Check to see whether the destination folde rexists. If it doesn't, try
     * to create it.
     * @param login       the login for the destination machine.
     * @param destination the folder that should be created.
     * @throws Exception if the folder could not be created.
     */
    private void makeDestination(String login, String destination)
            throws Exception {
        log.trace("Verifying the existence of " + destination
                  + " with login " + login);
        NativeRunner runner =
                new NativeRunner(new String[]{"ssh", login,
                                              "cd", destination});
        if (runner.execute(50000, 50000) == 0 &&
            "".equals(runner.getProcessErrorAsString())) {
            log.trace("The destination " + destination + " already exists");
            return;
        }
        log.debug("The destination " + destination + " with login " + login
                  + " does not exist. Attempting creation");
        runner = new NativeRunner(new String[]{"ssh", login,
                                               "mkdir", "-p", destination});
        if (runner.execute(50000, 50000) == 0) {
            log.debug("The destination " + destination + " was created");
        } else {
            String error = "Could not create directry '" + destination
                           + "' with login '" + login
                           + "':\n\t" + runner.getProcessErrorAsString();
            log.warn(error);
            throw new ClientDeploymentException(error);
        }
    }

    public void start(Feedback feedback) throws Exception {
        log.info("Starting service");

        String jar = "Main.jar"; //getProperty(PROPERTY_START_JAR);

        log.debug("Running " + jar + " with login " + login
                  + " and configuration server " + confLocation);
        NativeRunner runner =
                new NativeRunner(new String[]{
                        "ssh", login,
                        "cd", destination,
                        ";", "java",
                        "-cp lib/*.jar:config:.",
                        "-D" + Configuration.CONFIGURATION_PROPERTY + "=" + confLocation,
                        "-D" + INSTANCE_ID_PROPERTY + "=" + clientId,
                        "-jar", jar});
        String error = null;
        try {
            int returnValue = runner.execute(50000, 50000);
            if (returnValue != 0) {
                error = "Could not run " + jar + " with login "
                        + login + " and configuration server "
                        + confLocation + ". Got return value "
                        + returnValue + " and message "
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Could not run" + jar + " with login "
                    + login + " and configuration server " + confLocation
                    + ": " + runner.getProcessErrorAsString();
            log.error("Exception in start: " + e.getMessage(), e);
        }
        if (error != null) {
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new Exception(error);

        }
        log.info("Finished start of " + jar + " with login "
                           + login + " and configuration server " + confLocation
                           + ": " + runner.getProcessErrorAsString());
        /**
         ssh bar@zoo java -Dsumma.score.configuration=//example.com/myConfServer -jar /path/to/somewhere/runClient.jar
         */

    }

    public String getTargetHost() {
        // Extract hostname from a string like user@host:/dir
        int split = login.indexOf("@");
        split += 1;
        String hostname = login.substring(split);
        split = hostname.indexOf(":");
        if (split == -1) {
            return hostname;
        }
        return hostname.substring(0, split);
    }

    private String getProperty(String key) {
        String value = configuration.getString(key);
        if (key == null) {
            String error = "Could not get the property " + key;
            log.fatal(error);
            throw new IllegalArgumentException(error);
        }
        return value;
    }
}
