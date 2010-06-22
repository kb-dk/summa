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

import dk.statsbiblioteket.summa.control.api.feedback.Message;
import dk.statsbiblioteket.summa.control.api.feedback.Feedback;
import dk.statsbiblioteket.summa.control.api.feedback.ConsoleFeedback;
import dk.statsbiblioteket.summa.control.api.feedback.VoidFeedback;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;

import java.util.List;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class RemoteConsoleFeedback extends UnicastRemoteObject
                                   implements RemoteFeedback {    

    private static Log log = LogFactory.getLog (RemoteConsoleFeedback.class);

    private Feedback feedback;
    private int registryPort;
    private String serviceName;
    private boolean closed;

    public RemoteConsoleFeedback(Configuration conf) throws IOException {
        super (getServicePort(conf));
        try {
            feedback = new ConsoleFeedback(conf);
        } catch (Exception e) {
            log.warn ("Unable to create ConsoleFeedback. Falling back"
                         + "to VoidFeedback", e);
            feedback = new VoidFeedback();
        }
        closed = false;
        registryPort = conf.getInt (CONF_REGISTRY_PORT, 27000);
        serviceName = conf.getString(CONF_SERVICE_NAME,
                                     "remoteConsole");

        RemoteHelper.exportRemoteInterface(this, registryPort, serviceName);
    }

    public RemoteConsoleFeedback () throws IOException {
        this (Configuration.getSystemConfiguration(true));
    }

    private static int getServicePort (Configuration conf) {
        return conf.getInt(CONF_SERVICE_PORT, 27091);
    }

    public void putMessages(List<Message> messages) throws RemoteException {
        try {
            feedback.putMessages(messages);
        } catch (IOException e) {
            throw new RemoteException("Broken output stream", e);
        }
    }

    public void putMessage(Message message) throws RemoteException {
        try {
            feedback.putMessage(message);
        } catch (IOException e) {
            throw new RemoteException("Broken output stream", e);
        }
    }

    public void close () throws IOException {
        if (closed) return;        

        closed = true;
        RemoteHelper.unExportRemoteInterface(serviceName, registryPort);
    }
}




