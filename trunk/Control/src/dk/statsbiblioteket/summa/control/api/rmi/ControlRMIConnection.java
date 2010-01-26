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

import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI specialization of the public {@link ControlConnection} interface.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public interface ControlRMIConnection extends ControlConnection, Remote {

    public ClientConnection getClient(String instanceId) throws RemoteException;

    public void deployClient(Configuration conf) throws RemoteException;

    public void startClient(Configuration conf) throws RemoteException;

    public List<String> getClients() throws RemoteException;

    public List<String> getBundles() throws RemoteException;

    public Configuration getDeployConfiguration (String instanceId)
                                                        throws RemoteException;

    public Status getStatus () throws RemoteException;

}




