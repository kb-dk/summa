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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import dk.statsbiblioteket.summa.score.server.ClientDeployer;
import dk.statsbiblioteket.summa.score.server.ClientDeploymentException;
import dk.statsbiblioteket.summa.score.server.ScoreUtils;
import dk.statsbiblioteket.summa.score.api.Feedback;
import dk.statsbiblioteket.summa.score.api.Message;
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.api.BadConfigurationException;
import dk.statsbiblioteket.summa.score.bundle.Bundle;
import dk.statsbiblioteket.summa.score.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.score.bundle.BundleStub;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.NativeRunner;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;
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
        source = conf.getString(DEPLOYER_BUNDLE_FILE_PROPERTY);
        clientId = conf.getString (INSTANCE_ID_PROPERTY);
        confLocation = conf.getString (CLIENT_CONF_PROPERTY,
                                       "configuration.xml");

        destination += File.separator + clientId;
    }

    public void deploy(Feedback feedback) throws Exception {
        log.info("Deploying client");

        if (source == null) {
            throw new BadConfigurationException(DEPLOYER_BUNDLE_FILE_PROPERTY
                                                + " not set");
        }

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


        ensurePermissions(feedback);


        log.info("Finished deploy of " + source + " to " + destination);
        /*
       scp foo.zip bar@zoo:/path/to/somewhere
       ssh bar@zoo unzip /path/to/somewhere/foo.zip
        */
    }

    private String[] privateFiles = new String[]{"jmx.access", "jmx.password"};
    /**
     * The permissions for JMX.access and JMX.password needs to be readable only
     * for the owner. The Unzip provided by Java does not handle file
     * permissions and a readily available substitute has not been found.
     * This method performs a recursive descend and sets the correct permissions
     * for all jmx.access and jmx.password files it encounters.
     * @param root where to start the search for JMX-files.
     */
    private void fixJMXPermissions(File root) {
        log.trace("fixJMXPermissions(" + root + ") entered");
        try {
            File[] files = root.listFiles();
            for (File file: files) {
                if (file.isDirectory()) {
                    fixJMXPermissions(file);
                } else {
                    for (String privateFile: privateFiles) {
                        if (privateFile.equals(file.getName())) {
                            log.debug("Setting permissions for '"
                                      + file.getAbsoluteFile()
                                      + " to read-only for owner and no "
                                      + "permissions for everyone else");
                            file.setReadable(false, false);
                            file.setReadable(true, true);
                            file.setWritable(false, false);
                            file.setExecutable(false, false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("fixJMXPermissions: Could not handle '" + root
                     + "'. Skipping");
        }
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

    /**
     * Set file permissions as described in the ClientDeployer interface
     */
    private void ensurePermissions (Feedback feedback) throws IOException {
        log.debug("Setting file permissions for '" + destination + "'");
        String[] command = new String[]{"ssh", login,
                                        "cd", destination,
                                        ";",
                                        "chmod", "u=r",
                                        BundleStub.POLICY_FILE,
                                        BundleStub.JMX_ACCESS_FILE,
                                        BundleStub.JMX_PASSWORD_FILE};
        NativeRunner runner =
                new NativeRunner(command);
        String error = null;
        try {
            int returnValue = runner.execute(50000, 50000);
            if (returnValue != 0) {
                error = "Failed to set file permissions on '" + destination
                        + "'. Got " + returnValue + " and message:\n\t"
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Failed to run:\n"
                    + Strings.join(Arrays.asList(command), " ") + "\n"
                    + "Got: " + e.getMessage() + "\n\n\t"
                    + runner.getProcessErrorAsString();
            log.error(error, e);
        }
        if (error != null) {
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new ClientDeploymentException(error);
        }
    }

    public void start(Feedback feedback) throws Exception {
        log.info("Starting service");

        /* Read the bundle spec */
        File bdlFile = new File (source);
        log.trace("Creating InputStream for bdlFile '" + bdlFile
                  + "', client.xml");
        InputStream clientSpec;
        try {
            clientSpec = new ByteArrayInputStream
                    (ScoreUtils.getZipEntry(bdlFile, "client.xml"));
        } catch(IOException e) {
            throw new IOException("Could not create InputStream for bdlFile '"
                                  + bdlFile + "', client.xml", e);
        }
        log.trace("Opening clientSpec with BundleSpecBuilder");
        BundleSpecBuilder builder = BundleSpecBuilder.open (clientSpec);
        log.trace("Getting BundleStub from BundleSpecBuilder");
        BundleStub stub = builder.getStub();

        log.trace("Adding properties to command line");
        /* Add properties to the command line as we are obliged to */
        stub.addSystemProperty(Configuration.CONFIGURATION_PROPERTY,
                               confLocation);
        stub.addSystemProperty(ClientConnection.CLIENT_ID,
                               clientId);

        log.debug("Building command line for " + clientId + " with login "
                  + login + " and configuration server " + confLocation);

        /* Build the command line with and ssh prefix */
        List<String> commandLine = new ArrayList<String>();
        commandLine.addAll (Arrays.asList("ssh", login,
                                          "cd", destination,
                                          ";"));
        commandLine.addAll(stub.buildCommandLine());

        log.debug ("Command line for '" + clientId + "':\n"
                   + Strings.join(commandLine, " "));

        /* Run the command line */
        NativeRunner runner =
                new NativeRunner(commandLine.toArray(
                                               new String[commandLine.size()]));

        String error = null;
        try {
            int returnValue = runner.execute(50000, 50000);
            if (returnValue != 0) {
                error = "Could not run client '" + clientId + "' with login "
                        + login + " and configuration server "
                        + confLocation + ". Got return value "
                        + returnValue + " and message "
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Could not start client '" + clientId + "' with login "
                    + login + " and configuration server " + confLocation
                    + ": " + runner.getProcessErrorAsString();
            log.error("Exception in start: " + e.getMessage(), e);
        }
        if (error != null) {
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new Exception(error);

        }
        log.info("Finished start of '" + clientId + "' with login "
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
