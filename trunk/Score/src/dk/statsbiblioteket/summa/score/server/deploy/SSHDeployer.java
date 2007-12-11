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
import dk.statsbiblioteket.summa.score.api.Feedback;
import dk.statsbiblioteket.summa.score.api.Message;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.NativeRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@lin ClientDeployer} that uses ssh to copy and start Clients.
 * $Id: SSHDeployer.java,v 1.8 2007/10/04 13:28:20 te Exp $
 */
public class SSHDeployer implements ClientDeployer {
    private static final Log log = LogFactory.getLog(SSHDeployer.class);

    /**
     * The login and the destination machine. In the current version of the
     * deployer, interactive login is not supported explicitly.<br />
     * Example: summa@example.org.
     */
    public static final String PROPERTY_LOGIN =
            "summa.score.SSHDeployer.login";
    /**
     * The path to the package to deploy. This must be a ZIP-file, as
     * specified in the package description.<br />
     * Example: /home/te/projects/summa/score/deploy/client.zip.
     */
    public static final String PROPERTY_SOURCE =
            "summa.score.SSHDeployer.source";
    /**
     * The destination on the remote machine. If the destination does not
     * exist, an attempt will be made to create it.<br />
     * Example: /home/summa/client.
     */
    public static final String PROPERTY_DESTINATION =
            "summa.score.SSHDeployer.destination";
    /**
     * The RMI-address for the configuration-server for the client.<br />
     * Example: //example.org/score-server:12345.
     */
    public static final String PROPERTY_START_CONFSERVER =
            "summa.score.SSHDeployer.start_confserver";

    /**
     * The instanceID for the client. This must be unique for the Summa
     * installation.
     */
    public static final String PROPERTY_CLIENT_INSTANCEID =
            "summa.score.SSHDeployer.client_instance_id";

    protected Configuration configuration;

    public SSHDeployer(Configuration configuration) {
        this.configuration = configuration;
    }

    public void deploy(Feedback feedback) throws Exception {
        log.info("Deploying client");
        String login = getProperty(PROPERTY_LOGIN);
        String source = getProperty(PROPERTY_SOURCE);
        String destination = getProperty(PROPERTY_DESTINATION);
        makeDestination(login, destination);

        log.debug("Deploying from " + source + " to " + destination);
        try {
            NativeRunner runner =
                    new NativeRunner(new String[]{"scp", source,
                                                  login + ":" + destination});
            runner.execute(50000, 50000);
        } catch(Exception e) {
            String error = "Could not deploy from " + source
                           + " to " + destination;
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new Exception(error, e);
        }
        log.debug("Deployed from " + source + " to " + destination);

        /* Unpack */
        String archive = source;
        if (source.lastIndexOf(File.separator) > 0) {
            archive = source.substring(source.lastIndexOf(File.separator) + 1);
        }

        log.debug("Unpacking " + archive + " at " + destination
                  + " with login " + login);
        NativeRunner runner =
                new NativeRunner(new String[]{"ssh", login,
                                              "cd", destination,
                                              ";", "unzip", "-u",
                                              archive});
        String error = null;
        try {
            int returnValue = runner.execute(50000, 50000);
            if (returnValue != 0) {
                error = "Could not unpack " + archive + " with login "
                        + login + ". Got return value "
                        + returnValue + " and message "
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Could not unpack " + archive + " with login "
                    + login + ": "
                    + runner.getProcessErrorAsString();
            log.error("Exception in deploy: " + e.getMessage(), e);
        }
        if (error != null) {
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new Exception(error);
        }
        log.debug("Unpacked " + archive + " at " + destination
                  + " with login " + login);

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
            String error = "Could not create " + destination
                           + " with login " + login
                           + ": " + runner.getProcessErrorAsString();
            log.warn(error);
            throw new Exception(error);
        }
    }

    public void start(Feedback feedback) throws Exception {
        log.info("Starting service");
        String login = getProperty(PROPERTY_LOGIN);
        String confServer = getProperty(PROPERTY_START_CONFSERVER);
        String destination = getProperty(PROPERTY_DESTINATION);
        String clientID = getProperty(PROPERTY_CLIENT_INSTANCEID);

        String jar = "Main.jar"; //getProperty(PROPERTY_START_JAR);

        log.debug("Running " + jar + " with login " + login
                  + " and configuration server " + confServer);
        NativeRunner runner =
                new NativeRunner(new String[]{
                        "ssh", login,
                        "cd", destination,
                        ";", "java",
                        "-cp lib/*.jar;config;.",
                        "-D" + Configuration.CONFIGURATION_PROPERTY + "=" + confServer,
                        "-D" + INSTANCE_ID_PROPERTY + "=" + clientID,
                        "-jar", jar});
        String error = null;
        try {
            int returnValue = runner.execute(50000, 50000);
            if (returnValue != 0) {
                error = "Could not run " + jar + " with login "
                        + login + " and configuration server "
                        + confServer + ". Got return value "
                        + returnValue + " and message "
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Could not run" + jar + " with login "
                    + login + " and configuration server " + confServer
                    + ": " + runner.getProcessErrorAsString();
            log.error("Exception in start: " + e.getMessage(), e);
        }
        if (error != null) {
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new Exception(error);

        }
        log.info("Finished start of " + jar + " with login "
                           + login + " and configuration server " + confServer
                           + ": " + runner.getProcessErrorAsString());
        /**
         ssh bar@zoo java -Dsumma.score.configuration=//example.com/myConfServer -jar /path/to/somewhere/runClient.jar
         */

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
