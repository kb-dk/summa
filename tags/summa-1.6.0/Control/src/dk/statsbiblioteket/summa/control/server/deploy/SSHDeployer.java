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

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import dk.statsbiblioteket.summa.control.api.ClientDeployer;
import dk.statsbiblioteket.summa.control.api.ClientDeploymentException;
import dk.statsbiblioteket.summa.control.server.ControlUtils;
import dk.statsbiblioteket.summa.control.api.feedback.Feedback;
import dk.statsbiblioteket.summa.control.api.feedback.Message;
import dk.statsbiblioteket.summa.control.api.feedback.ConsoleFeedback;
import dk.statsbiblioteket.summa.control.api.feedback.VoidFeedback;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.BadConfigurationException;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.control.bundle.BundleStub;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.console.ProcessRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>{@link ClientDeployer} that uses ssh to copy and start Clients.</p>
 * 
 * $Id$
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te, mke",
        comment="mke made substantial changes over te's original impl")
public class SSHDeployer implements ClientDeployer {
    private static final Log log = LogFactory.getLog(SSHDeployer.class);

    private static final int START_TIMEOUT = 7000;

    private String login;
    private String feedbackLogin; // Just for feedback, includes port
    private String destination;
    private String source;
    private String clientId;
    private String confLocation;
    private int port = 22;

    protected Configuration configuration;

    public SSHDeployer(Configuration conf) {
        login = conf.getString(CONF_DEPLOYER_TARGET);
        removePortFromLogin();
        destination = conf.getString(CONF_BASEPATH, "summa-control");
        source = conf.getString(CONF_DEPLOYER_BUNDLE_FILE);
        clientId = conf.getString(CONF_INSTANCE_ID);
        confLocation = conf.getString(CONF_CLIENT_CONF, "configuration.xml");

        destination += File.separator + clientId;
    }

    /**
     * If a port is present in the login in the form of [user@]hostname[:port],
     * it will be removed from the login and assigned to {@link #port}.
     */
    private void removePortFromLogin() {
        String[] tokens = login.split(":");
        if (tokens.length == 1) {
            feedbackLogin = login + ":" + port;
            return;
        }
        try {
            port = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Malformed port definition: "
                                             + tokens[1]);
        }
        login = tokens[0];
        log.debug("removePortFromLogin got login '" + login
                  + "' and port " + port);
        feedbackLogin = login + ":" + port;
    }

    public void deploy(Feedback feedback) throws Exception {
        log.info("Deploying client");

        if (source == null) {
            throw new BadConfigurationException(CONF_DEPLOYER_BUNDLE_FILE
                                                + " not set");
        }

        /* Make sure target dir exists */
        makeDestination(login, port, destination);

        /* Copy package to destination */
        log.debug("Deploying from " + source + " to " + destination);
        ProcessRunner runner = new ProcessRunner(
                Arrays.asList("scp", "-P", Integer.toString(port), source,
                              login + ":"  + destination));
        try {
            runner.run();
        } catch(Exception e) {
            String error = "Could not deploy from source '" + source
                           + "' to destination '" + destination + "'";
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new ClientDeploymentException(error, e);
        }

        /* Check that the copy succeeded */
        if (runner.getReturnCode() != 0) {
            String error = "Could not deploy from source '" + source
                           + "' to destination '" + destination + "'";
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new ClientDeploymentException(error);
        }

        log.debug("Deployed from " + source + " to " + destination);

        /* Calculate archive name */
        String archive = source;
        if (source.lastIndexOf(File.separator) > 0) {
            archive = source.substring(source.lastIndexOf(File.separator) + 1);
        }
        String archivePath = destination + File.separator + archive;

        /* Unpack */
        log.debug("Unpacking '" + archivePath
                  + "' with login '" + feedbackLogin + "'");
        runner = new ProcessRunner(Arrays.asList(
                "ssh", "-p" + port, login, "cd", destination, ";",
                "unzip", "-o", archive));
        String error = null;
        try {
            runner.run();
            if (runner.getReturnCode() != 0) {
                error = "Could not unpack '" + archivePath + "' with login '"
                        + feedbackLogin + "'. Got return value "
                        + runner.getReturnCode() + " and message:\n\t"
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Could not unpack archive '" + archivePath
                    + "' with login '" + feedbackLogin + "': " + e.getMessage()
                    + "\n\n\t" + runner.getProcessErrorAsString();
            log.error(error, e);
        }
        if (error != null) {
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new ClientDeploymentException(error);
        }
        log.debug("Unpacked " + archive + " at " + destination
                  + " with login " + feedbackLogin);

        /* Clean up */
        log.debug("Deleting " + archivePath + " at " + destination
                  + " with login " + feedbackLogin);
        runner = new ProcessRunner(Arrays.asList(
                "ssh", "-p" + port, login, "cd", destination, ";",
                "rm", "-f", archive));
        error = null;
        try {
            runner.run();
            if (runner.getReturnCode() != 0) {
                error = "Could not delete '" + archivePath + "' with login '"
                        + feedbackLogin + "'. Got return value "
                        + runner.getReturnCode() + " and message:\n\t"
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Could not delete '" + archivePath + "' with login '"
                    + feedbackLogin + "': " + e.getMessage() + "\n\n\t"
                    + runner.getProcessErrorAsString();
            log.error(error, e);
        }
        if (error != null) {
            log.error(error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new ClientDeploymentException(error);
        }
        log.debug("Deleted '" + archivePath + "' with login " + feedbackLogin);


        ensurePermissions(feedback);


        log.info("Finished deploy of " + source + " to " + destination);
    }

    /**
     * Check to see whether the destination folde rexists. If it doesn't, try
     * to create it.
     * @param login       the login for the destination machine.
     * @param port        the port for the destination machine (normally 22).
     * @param destination the folder that should be created.
     * @throws Exception if the folder could not be created.
     */
    private void makeDestination(String login, int port, String destination)
            throws Exception {
        log.trace("Verifying the existence of " + destination
                  + " with login " + login);
        ProcessRunner runner =
                new ProcessRunner(Arrays.asList(
                        "ssh", "-p" + port, login, "cd", destination));
        runner.run();
        if (runner.getReturnCode() == 0 &&
            "".equals(runner.getProcessErrorAsString())) {
            log.trace("The destination " + destination + " already exists");
            return;
        }
        log.debug("The destination " + destination + " with login " + login
                  + " does not exist. Attempting creation");
        runner = new ProcessRunner(Arrays.asList(
                "ssh", "-p" + port, login, "mkdir", "-p", destination));
        runner.run();
        if (runner.getReturnCode() == 0) {
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
    private void ensurePermissions(Feedback feedback) throws IOException {
        log.debug("Setting file permissions for '" + destination + "'");

        /* The 'cd destination part' needs to be added a single arg */
        List<String> command = Arrays.asList(
                "ssh", "-p" + port, login, "cd", destination, ";",
                "chmod", "a=,u=r", BundleStub.POLICY_FILE,
                BundleStub.JMX_ACCESS_FILE, BundleStub.JMX_PASSWORD_FILE);
        ProcessRunner runner = new ProcessRunner(command);
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
                    (ControlUtils.getZipEntry(bdlFile, "client.xml"));
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

        log.debug("Building command line for " + clientId + " with login "
                  + feedbackLogin + " and configuration server "
                  + confLocation);

        /* Build the command line with and ssh prefix */
        List<String> commandLine = new ArrayList<String>();
        commandLine.addAll(Arrays.asList("ssh", "-p" + port, login));

        /* We need to escape all elements of the remote command line
         * since it must be passed as a single arg to ssh */
        String remoteArg = "cd " + destination + ";";
        List<String> remoteCmdLine = stub.buildCommandLine();
        remoteCmdLine.set(0, probeJVMPath()); // Detect the JVM path remotely and override it
        for (String arg : remoteCmdLine) {
            remoteArg = remoteArg + " " + arg.replace(" ", "\\ ");
        }
        commandLine.add(remoteArg);

        log.debug("Command line for '" + clientId + "':\n"
                   + Logs.expand(commandLine, 100));

        /* Exec the command line */
        ProcessRunner runner = new ProcessRunner(commandLine);

        String error = null;
        try {
            Thread processThread = new Thread(runner);
            processThread.setDaemon(true); // Allow JVM to exit
            processThread.start();

            /* Wait until the deployment is done or times out */
            processThread.join(START_TIMEOUT);

            if (runner.isTimedOut()) {
                String errorMsg = runner.getProcessErrorAsString();
                error = "Start request for client '" + clientId
                        + "' with login " + feedbackLogin
                        + " and configuration server "
                        + confLocation + ". Timed out"
                        + (errorMsg != null ? ":\n" + errorMsg : "");
            } else if (processThread.isAlive()) {
                /* The process is still running. This is probably a good sign,
                 * but we have no way to be sure */
                log.debug("Process thread for '" + clientId + "' still "
                          + "running. Let's hope it is doing good");
            } else if (runner.getReturnCode() != 0) {
                error = "Could not run client '" + clientId + "' with login "
                        + feedbackLogin + " and configuration server "
                        + confLocation + ". Got return value "
                        + runner.getReturnCode() + " and message "
                        + runner.getProcessErrorAsString();
            }
        } catch(Exception e) {
            error = "Could not start client '" + clientId + "' with login "
                    + feedbackLogin + " and configuration server "
                    + confLocation + ": " + runner.getProcessErrorAsString();
            log.error("Exception in start: " + e.getMessage(), e);
        }
        if (error != null) {
            log.error("Error when starting client: " + error);
            feedback.putMessage(new Message(Message.MESSAGE_ALERT, error));
            throw new Exception(error);

        }
        log.info("Finished start of '" + clientId + "' with login "
                           + feedbackLogin + " and configuration server "
                           + confLocation + ": "
                           + runner.getProcessErrorAsString());
        /**
         ssh bar@zoo java -Dsumma.control.configuration=//example.com/myConfServer -jar /path/to/somewhere/runClient.jar
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

    private String probeJVMPath() {
        final String[] rootProspects = new String[] {"/usr/java/",
                                             "/usr/lib/jvm/"};

        final String[] jrePropsects = new String[] {"java-6-sun",
                                           "java-1.6-sun",
                                           "java-1.6.0-sun",
                                           "java-6-openjdk",
                                           "java-1.6-openjdk",
                                           "java-1.6.0-openjdk",
                                           "jdk1.6",
                                           "jre1.6"};

        log.debug("Probing for JVM path on " + feedbackLogin);

        for (String jre : jrePropsects) {
            for (String root : rootProspects) {
                String probePath = root + jre + "/bin/java";
                log.trace("Probing for JRE " +probePath);
                ProcessRunner probe = new ProcessRunner(
                        "ssh", "-p" + port, login, probePath + " -version");
                probe.run();
                if (probe.getReturnCode() == 0) {
                    log.debug("Found JVM on " + login + ": " + probePath);
                    return probePath;
                } else {
                    log.trace("No JRE on "+probePath+":\n"
                              + probe.getProcessOutputAsString() + "\n"
                              + probe.getProcessErrorAsString());
                }
            }
        }

        String probePath = "/usr/bin/java";
        log.trace("Probing for JRE " + probePath);
        ProcessRunner probe = new ProcessRunner(
                "ssh", "-p" + port, login, probePath + " -version");
        probe.run();
        if (probe.getReturnCode() == 0) {
            log.debug("Found JVM on " + login + ": " + probePath);
            return probePath;
        }

        throw new ClientDeploymentException("Unable to detect JRE on "
                                            + feedbackLogin);
    }

    public String getLogin() {
        return login;
    }

    public int getPort() {
        return port;
    }

    public static void main (String[] args) throws Exception {
        ClientDeployer d = new SSHDeployer(
                Configuration.newMemoryBased("summa.control.deployer.target", "localhost:222",
                                             "summa.control.deployer.bundle.file", "/home/mke/summa-control/repository/test-client-1.bundle",
                                             "summa.control.client.id", "t3"));
        d.start(new VoidFeedback());

    }
}

