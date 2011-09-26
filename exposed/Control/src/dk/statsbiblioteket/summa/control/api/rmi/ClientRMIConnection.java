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
package dk.statsbiblioteket.summa.control.api.rmi;

import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Package private specification of a {@link ClientConnection} using RMI
 * as transport. This is used to abstract out RMI from the public API.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public interface ClientRMIConnection extends Remote, ClientConnection {

    /**
     * Stop the client specified in the base configuration for the
     * ClientDeployer.
     *
     * This call should stop the JVM of the client. Ie, call {@link System#exit}
     *
     * @throws RemoteException in case of communication errors.
     */
    @Override
    public void stop() throws RemoteException;

    /**
     * Returning status for this client.
     * @return This clients status.
     */
    @Override
    public Status getStatus() throws RemoteException;

    /**
     * Deploy a service.
     * @param bundleId The bundle ID for the deployed service.
     * @param instanceId The instance ID.
     * @param configLocation The configuration location for the service which
     * should be deployed.
     * @return Instance ID.
     * @throws RemoteException If error occur when communication over RMI.
     */
    @Override
    public String deployService(String bundleId, String instanceId,
                                String configLocation) throws RemoteException;

    /**
     * Stop a service and move its package file to artifacts.
     * @param instanceId The service to stop and remove.
     * @throws RemoteException Upon communication errors with the service.
     */
    @Override
    public void removeService(String instanceId) throws RemoteException;

    /**
     * Start a service.
     * @param id The instance ID.
     * @param configLocation The configuration location for the service.
     * @throws RemoteException If error occur while doing RMI work.
     */
    @Override
    public void startService(String id, String configLocation)
                                                         throws RemoteException;

    /**
     * Stops a given service.
     * @param id The ID of the service to stop.
     * @throws RemoteException If error occur over RMI, while connecting to
     * service.
     */
    @Override
    public void stopService(String id) throws RemoteException;

    /**
     * Return the service status.
     * @param id The services ID.
     * @return The status of the service with the given ID.
     * @throws RemoteException If error occur handling RMI connection.
     */
    @Override
    public Status getServiceStatus(String id) throws RemoteException;

    /**
     * Get connection to the service specified by the ID.
     * @param id The instance id of the service to connect to.
     * @return A connection to the service.
     * @throws RemoteException If error occur when connection to service.
     */
    @Override
    public Service getServiceConnection(String id) throws RemoteException;

    /**
     * Getting a list of all services attached to this client.
     * 
     * @return list of services.
     * @throws RemoteException If this call fails.
     */
    @Override
    public List<String> getServices() throws RemoteException;

    /**
     * Return this clients ID.
     * @return The ID of this client.
     * @throws RemoteException If this call fails.
     */
    public String getId() throws RemoteException;

    /**
     * Return the repository used by this client to access the bundles.
     * @return The bundle repository for this class.
     * @throws RemoteException If the repository is an RMI repository and error
     * occurs when connection to it.
     */
    @Override
    public BundleRepository getRepository() throws RemoteException;

    /**
     * Return the specification of the instance, identified by the given ID.
     * @param instanceId The instance ID.
     * @return The specification for the instance.
     * @throws RemoteException If error occur while connection to instance over
     * RMI.
     */
    @Override
    public String getBundleSpec(String instanceId) throws RemoteException;

    /**
     * Get error report on the specified instance ID.
     * @param id The instance ID.
     * @throws RemoteException If error occur while connection to the instance
     * over RMI.
     */
    @Override
    public void reportError(String id) throws RemoteException;

}




