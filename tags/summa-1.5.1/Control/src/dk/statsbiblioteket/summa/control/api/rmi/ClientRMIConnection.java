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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Package private specification of a {@link ClientConnection} using RMI
 * as transport. This is used to abstract out RMI from the public API.
 */
public interface ClientRMIConnection extends Remote, ClientConnection {

    public void stop() throws RemoteException;


    public Status getStatus() throws RemoteException;


    public String deployService(String bundleId,
                                String instanceId,
                                String configLocation)
                                                         throws RemoteException;

    public void removeService(String instanceId) throws RemoteException;

    public void startService(String id, String configLocation)
                                                         throws RemoteException;

    public void stopService(String id) throws RemoteException;

    public Status getServiceStatus(String id) throws RemoteException;

    public Service getServiceConnection (String id) throws RemoteException;

    public List<String> getServices() throws RemoteException;

    public String getId() throws RemoteException;

    public BundleRepository getRepository () throws RemoteException;

    public String getBundleSpec (String instanceId)
                                                         throws RemoteException;

    public void reportError (String id) throws RemoteException;

}




