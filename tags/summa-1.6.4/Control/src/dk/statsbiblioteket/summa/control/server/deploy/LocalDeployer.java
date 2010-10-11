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
package dk.statsbiblioteket.summa.control.server.deploy;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.BadConfigurationException;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.ClientDeployer;
import dk.statsbiblioteket.summa.control.api.ClientDeploymentException;
import dk.statsbiblioteket.summa.control.api.feedback.Feedback;
import dk.statsbiblioteket.summa.control.api.feedback.Message;
import dk.statsbiblioteket.summa.control.api.feedback.VoidFeedback;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.control.bundle.BundleStub;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.console.ProcessRunner;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * FIXME: Missing class docs for LocalDeployer
 *
 * @author Mikkel Kamstrup <mailto:mke@statsbiblioteket.dk>
 * @author Henrik Kirk <mailto:hbk@statsbiblioteket.dk>
 * @since Sep 2, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke, hbk",
        comment = "Needs JavaDoc")
public class LocalDeployer implements ClientDeployer {
    private static final Log log = LogFactory.getLog(SSHDeployer.class);

    private static final int START_TIMEOUT = 7000;

    private String destination;
    private String source;
    private String clientId;
    private String confLocation;

    protected Configuration configuration;

    public LocalDeployer(Configuration conf) {
        destination = conf.getString(CONF_BASEPATH, "summa-control");
        source = conf.getString(CONF_DEPLOYER_BUNDLE_FILE);
        clientId = conf.getString(CONF_INSTANCE_ID);
        confLocation = conf.getString(CONF_CLIENT_CONF, "configuration.xml");

        destination += File.separator + clientId;
    }

    public void deploy(Feedback feedback) throws Exception {
        File sourceFile = new File(source);
        //File destFile = new File(destination);

        /* Calculate destination archive path */
        String archive = sourceFile.getName();
        String archivePath = destination + File.separator + archive;
        File archiveFile = new File(archivePath);

        log.info("Deploying client");

        if (source == null) {
            throw new BadConfigurationException(CONF_DEPLOYER_BUNDLE_FILE
                                                + " not set");
        }

        /* Make sure target dir exists */
        makeDestination();

        /* Copy package to destination */
        log.trace("Deploying from " + source + " to " + destination);
        Files.copy(sourceFile, archiveFile, false);
        log.debug("Deployed from " + source + " to " + destination);        


        /* Unpack */
        log.trace("Unpacking '" + archivePath);
        Zips.unzip(archivePath, destination, false);
        log.debug("Unpacked " + archivePath + " to " + destination);

        /* Clean up */
        log.trace("Deleting " + archivePath);
        Files.delete(archivePath);
        log.debug("Deleted '" + archivePath);

        ensurePermissions(feedback);

        log.info("Finished deploy of " + source + " to " + destination);
    }

    /**
     * Check to see whether the destination folder exists. If it doesn't, try
     * to create it.
     * @throws IOException if the folder could not be created.
     */
    private void makeDestination()
            throws Exception {
        File dest = new File(destination);
        if (dest.isFile()) {
            throw new IOException("Target destination is a regular file: "
                                  + destination);
        }

        if(!dest.exists() && !dest.mkdirs()) {
            throw new IOException("Target destination '" + destination
                    + "' could not be created");
        }
    }

    /**
     * Set file permissions as described in the ClientDeployer interface
     * @param feedback the Feedback object.
     * @throws IOException If an error occurs.
     */
    private void ensurePermissions(Feedback feedback) throws IOException {
        log.debug("Setting file permissions for '" + destination + "'");

        /* The 'cd destination part' needs to be added a single arg */
        List<String> command = Arrays.asList(
                "chmod", "a=,u=r", BundleStub.POLICY_FILE,
                BundleStub.JMX_ACCESS_FILE, BundleStub.JMX_PASSWORD_FILE);
        ProcessRunner runner = new ProcessRunner(command);
        runner.setStartingDir(new File(destination));
        log.trace("Command to ensure permissions:\n"
                  + Strings.join(command, " "));
        String error = null;
        try {
            runner.run();
            if (runner.getReturnCode() != 0) {
                error = "Failed to set file permissions on '" + destination
                        + "'. Got " + runner.getReturnCode()
                        + " and message:\n\t"
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
        log.trace("File permissions fixed for client '" + clientId + "'");
    }

    public void start(Feedback feedback) throws Exception {
        log.info("Starting service");

        /* Read the bundle spec */
        File bdlFile = new File(source);
        log.trace("Creating InputStream for bdlFile '" + bdlFile
                  + "', client.xml");
        InputStream clientSpec;
        try {
            clientSpec = new ByteArrayInputStream
                    (Zips.getZipEntry(bdlFile, "client.xml"));
        } catch(IOException e) {
            throw new IOException("Could not create InputStream for bdlFile '"
                                  + bdlFile + "', client.xml", e);
        }
        log.trace("Opening clientSpec with BundleSpecBuilder");
        BundleSpecBuilder builder = BundleSpecBuilder.open(clientSpec);
        log.trace("Getting BundleStub from BundleSpecBuilder");
        BundleStub stub = builder.getStub();

        /* Add properties to the command line as we are obliged to */
        log.trace("Adding properties to command line");
        stub.addSystemProperty(ClientConnection.CONF_CLIENT_ID,
                               clientId);

        log.debug("Building command line for " + clientId
                  + " and configuration server " + confLocation);

        /* Exec the command line */
        List<String> cmdLine = stub.buildCommandLine();
        ProcessRunner runner = new ProcessRunner(cmdLine);
        runner.setStartingDir(new File(destination));
        String error = null;
        log.debug("Starting instance '" + clientId + "' with:\n"
                  + Strings.join(cmdLine, " "));
        try {
            Thread processThread = new Thread(runner, "LocalDeployer Thread");
            processThread.setDaemon(true); // Allow JVM to exit
            processThread.start();

            /* Wait until the deployment is done or times out */
            processThread.join(START_TIMEOUT);

            if (runner.isTimedOut()) {
                String errorMsg = runner.getProcessErrorAsString();
                error = "Start request for client '" + clientId
                        + "' with configuration server "
                        + confLocation + ". Timed out"
                        + (errorMsg != null ? ":\n" + errorMsg : "");
            } else if (processThread.isAlive()) {
                /* The process is still running. This is probably a good sign,
                 * but we have no way to be sure */
                log.debug("Process thread for '" + clientId + "' still "
                          + "running. Let's hope it is doing good");
            } else if (runner.getReturnCode() != 0) {
                error = "Could not run client '" + clientId
                        + "' with  configuration server "
                        + confLocation + ". Got return value "
                        + runner.getReturnCode() + " and message "
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Could not start client '" + clientId
                    + "' with  configuration server " + confLocation
                    + ": " + runner.getProcessErrorAsString();
            log.error("Exception in start: " + e.getMessage(), e);
        }
        if (error != null) {
            log.error("Error when starting client: " + error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new Exception(error);

        }
        log.info("Finished start of '" + clientId
                 + "' with  configuration server " + confLocation + ": "
                 + runner.getProcessErrorAsString());

    }

    public String getTargetHost() {
        return "localhost";
    }

    public static void main (String[] args) throws Exception {
        ClientDeployer d = new LocalDeployer(
                Configuration.newMemoryBased("summa.control.deployer.target",
                                              "localhost:222",
                                           "summa.control.deployer.bundle.file",
                      "/home/mke/summa-control/repository/test-client-1.bundle",
                                              "summa.control.client.id", "t3"));
        d.start(new VoidFeedback());

    }
}

