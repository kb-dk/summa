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
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.BadConfigurationException;
import dk.statsbiblioteket.summa.control.api.NoSuchClientException;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.api.ClientDeployer;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.control.bundle.Bundle;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.Strings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class helps the {@link ControlCore} manage its collection of clients.
 *
 * @see dk.statsbiblioteket.summa.control.client.Client
 * @see dk.statsbiblioteket.summa.control.api.ClientConnection
 * @see ControlConnection
 * @see ControlCore
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class ClientManager extends ConnectionManager<ClientConnection>
                                         implements Runnable, Iterable<String> {

    /**
     * Configuration property defining the class of the
     * {@link ConnectionFactory} to use for creating client connections.
     */
    public static final String CONF_CONNECTION_FACTORY =
                                          GenericConnectionFactory.CONF_FACTORY;

    /**
     * File extension for client metadata files
     */
    public static final String CLIENT_CONTROL_FILE = "control.xml";

    private Log log = LogFactory.getLog (ClientManager.class);
    private File baseDir;

    public ClientManager(Configuration conf) throws IOException {
        super (getConnectionFactory(conf));

        baseDir = ControlUtils.getControlBaseDir(conf);
    }

    @SuppressWarnings("unchecked")
    private static ConnectionFactory<? extends ClientConnection>
                                     getConnectionFactory (Configuration conf) {
        Class<? extends ConnectionFactory> connFactClass =
                conf.getClass(CONF_CONNECTION_FACTORY, ConnectionFactory.class,
                              SummaRMIConnectionFactory.class);

        ConnectionFactory connFact = Configuration.create(connFactClass, conf);
        connFact.setGraceTime(1);
        connFact.setNumRetries(2);
	
        return (ConnectionFactory<ClientConnection>) connFact;
    }

    /**
     * Return true <i>iff</i> the client metadata file for the given
     * instance id exists.
     * @param instanceId the instance id of the client to check for
     * @return true if the client is already registered
     */
    public boolean knowsClient (String instanceId) {
        return getClientControlFile(instanceId).exists();
    }

    /**
     * Store connection parameters for the given Client instance.
     * @param instanceId unique instance id of the client
     * @param clientHost hostname of the machine where the client runs
     * @param deployConfig configuration passed to the {@link ClientDeployer}
     *                     responsible for deploying the given client
     * @param spec a bundle spec for the client being deployed
     * @throws BadConfigurationException if a client with instance id
     *                                   {@code instanceId} is already
     *                                   registered.
     */
    public void register (String instanceId,
                          String clientHost,
                          Configuration deployConfig,
                          BundleSpecBuilder spec) {
        log.debug ("Registering Client instance '" + instanceId + "' on host '"
                   + clientHost + "'");

        File clientMeta = getClientControlFile(instanceId);

        if (clientMeta.exists()) {
            throw new BadConfigurationException ("Client with instance id '"
                                                 + instanceId + "' is already"
                                                 + " registered");
        }

        // Make sure the client directory exists
        clientMeta.getParentFile().mkdir();

        try {
            Configuration conf = new Configuration (
                                                new FileStorage(clientMeta));
            conf.importConfiguration(deployConfig);
            conf.set (ClientConnection.CONF_REGISTRY_HOST, clientHost);

            int regPort = getClientRegistryPort (instanceId, deployConfig);
            conf.set (ClientConnection.CONF_REGISTRY_PORT,
                      Integer.toString(regPort));

            /* Store whether this bundle should be automagically started
             * by the Control server */
            conf.set(Bundle.CONF_AUTO_START,
                     Boolean.toString(spec.isAutoStart()));

        } catch (IOException e) {
            log.error ("Failed to write client registration for '"
                       + instanceId + "' in '" + clientMeta + "': "
                       + e.getMessage(), e);
        }

    }

    /**
     * Return the value of {@link ClientDeployer#CONF_DEPLOYER_TARGET}
     * as set when the client was deployed.
     * @param instanceId the instance id of the client to look uo deployment
     *                   target for
     * @return the target property or null if it is not found
     */
    public String getDeployTarget (String instanceId) {
        try {
            File clientMeta = getClientControlFile(instanceId);
            Configuration conf = new Configuration (new FileStorage(clientMeta));
            return conf.getString(ClientDeployer.CONF_DEPLOYER_TARGET);
        } catch (IOException e) {
            log.error ("Error while looking up deployment target for '"
                       + instanceId + "'");
            return null;
        }
    }

    public Configuration getDeployConfiguration (String instanceId) {
        if (!knowsClient(instanceId)) {
            throw new NoSuchClientException("No such client " + instanceId);
        }

        try {
            /* We use a memory backed conf storage to avoid people being
            * able to edit the legacy configs */
            File clientMeta = getClientControlFile(instanceId);
            InputStream in = new FileInputStream(clientMeta);
            return new Configuration (new MemoryStorage(in));
        } catch (IOException e) {
            log.error ("Error while looking up deployment target for '"
                       + instanceId + "': " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Return the bundle id that a given instance is based on.
     * @param instanceId the instance id of the client to look up the bundkle id
     *                   for
     * @return the bundle id or null if the instance id is not known
     */
    public String getBundleId (String instanceId) {
        if (!knowsClient (instanceId)) {
            throw new NoSuchClientException ("No such client '"
                                             + instanceId + "'");
        }

        try {
            File clientMeta = getClientControlFile(instanceId);
            Configuration conf = new Configuration (new FileStorage(clientMeta));

            return conf.getString(ClientDeployer.CONF_DEPLOYER_BUNDLE);
        } catch (IOException e) {
            log.error ("Error while looking up bundle id for '"
                       + instanceId + "'");
            return null;
        }
    }

    private String getClientHost (String instanceId) {
        Configuration conf = getDeployConfiguration(instanceId);
        return getClientHost(conf);
    }

    private String getClientHost (Configuration deployConfig) {
        return deployConfig.getString(ClientConnection.CONF_REGISTRY_HOST);
    }

    /**
     * Find out what registry port a client uses
     * @param instanceId
     * @param deployConfig
     * @return
     */
    private int getClientRegistryPort(String instanceId,
                                      Configuration deployConfig) {
        return deployConfig.getInt (ClientConnection.CONF_REGISTRY_PORT,
				    ClientConnection.DEFAULT_REGISTRY_PORT);
    }

    public ConnectionContext<ClientConnection>get (String clientId) {
        Configuration deployConf = getDeployConfiguration(clientId);
        String address = getClientAddress (clientId, deployConf);

        log.trace ("Returning client address '" + address + "' for client "
                   + "'" + clientId + "'");

        return super.get (address);        
    }

    public String getClientAddress (String instanceId) {
        if (!knowsClient(instanceId)) {
            throw new NoSuchClientException("No such client: " + instanceId);
        }

        Configuration clientDeployConf = getDeployConfiguration (instanceId);

        String address = "//" + getClientHost (clientDeployConf);
        address += ":" + getClientRegistryPort(instanceId, clientDeployConf);
        address += "/" + instanceId;

        return address;
    }

    public String getClientAddress (String instanceId,
                                    Configuration clientDeployConf) {
        if (!knowsClient(instanceId)) {
            throw new NoSuchClientException("No such client: " + instanceId);
        }

        String address = "//" + getClientHost (clientDeployConf);
        address += ":" + getClientRegistryPort(instanceId, clientDeployConf);
        address += "/" + instanceId;

        return address;
    }

    public void run() {
        throw new UnsupportedOperationException();
    }

    private File getClientControlFile(String instanceId) {
        File controlFile = new File (baseDir,
                                  instanceId + File.separator
                                  + CLIENT_CONTROL_FILE);
        log.trace ("Controlfile for '" + instanceId + " is " + controlFile);
        return controlFile;
    }

    /**
     * Return a list of all clients known by the control server.
     * This amounts to a list of all directories in the control's base dir
     * containing a {@code control.xml} file.
     * @return
     */
    public List<String> getClients() {
        log.trace ("Getting client list");
        String[] baseContents = baseDir.list();

        if (baseContents == null) {
            log.warn ("Error reading " + baseDir + ". It has probably been" +
                    "deleted");
            return new ArrayList<String>(0);
        }

        List<String> clients = new ArrayList<String>(baseContents.length);

        for (String client : baseContents) {
 	    if (new File(baseDir, 
                         client + File.separator 
                         + CLIENT_CONTROL_FILE).exists()) {
                clients.add (client);
            }
        }

        log.trace ("Found clients: " + Strings.join(clients, ", "));

        return clients;
    }

    public Iterator<String> iterator () {
        return getClients ().iterator();
    }
}




