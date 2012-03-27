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
package dk.statsbiblioteket.summa.control.api.feedback.rmi;

import dk.statsbiblioteket.summa.control.api.feedback.Feedback;
import dk.statsbiblioteket.summa.control.api.feedback.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 *
 */
public interface RemoteFeedback extends Remote, Feedback {

    /**
     * Configuration property naming the port on which the RMI service should
     * communicate. Default is 27091.
     */
    public static final String CONF_SERVICE_PORT = "summa.control.remoteconsole.service.port";

    /**
     * Configuration property specifying the name of the service exposing the
     * {@link Feedback} interface. Default is "remoteConsole".
     */
    public static final String CONF_SERVICE_NAME = "summa.control.remoteconsole.service.name";

    /**
     * Configuration property specifying the port on which the registry should
     * run. Default is 27000.
     */
    public static final String CONF_REGISTRY_PORT = "summa.control.remoteconsole.registry.port";

    /**
     * Configuration property specifying the host on which the registry should
     * run. Default is "localhost".
     */
    public static final String CONF_REGISTRY_HOST = "summa.control.remoteconsole.registry.host";

    public void putMessages(List<Message> messages) throws RemoteException;

    public void putMessage(Message message) throws RemoteException;

}




