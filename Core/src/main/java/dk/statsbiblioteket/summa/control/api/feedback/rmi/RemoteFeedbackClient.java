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
import dk.statsbiblioteket.summa.control.server.shell.ControlShell;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import dk.statsbiblioteket.util.Logs;

import java.util.List;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link Feedback} implementation talking to a {@link RemoteFeedback}.
 * This is for example used to implement remote consoles, where an instance of
 * this class is running on the Control server, and a
 * {@link RemoteConsoleFeedback} is running in a
 * {@link ControlShell}.
 */
public class RemoteFeedbackClient implements Feedback {

    private ConnectionManager<Feedback> cm;
    private String remoteAddress;
    private Log log = LogFactory.getLog (RemoteFeedbackClient.class);

    public RemoteFeedbackClient(Configuration conf) {
        cm = new ConnectionManager<Feedback> (new RMIConnectionFactory<RemoteFeedback>());


        remoteAddress = "//" 
                       + conf.getString(RemoteFeedback.CONF_REGISTRY_HOST,
                                        "localhost")
                       + ":"
                       + conf.getInt(RemoteFeedback.CONF_REGISTRY_PORT,
                                     27000)
                       + "/"
                       + conf.getString(RemoteFeedback.CONF_SERVICE_NAME,
                                        "remoteConsole");

        log.debug ("Using remote console on '" + remoteAddress + "'");

    }

    public RemoteFeedbackClient () {
        this (Configuration.getSystemConfiguration(true));
    }

    public void putMessages(List<Message> messages) {
        ConnectionContext<Feedback> conn = null;
        try {
            conn = cm.get (remoteAddress);
            if (conn == null) {
                log.error ("Error putting messages " + Logs.expand(messages, 10)
                          + ". Failed to connect to " + remoteAddress);
                return;
            }
            Feedback feedback = conn.getConnection();
            try {
                feedback.putMessages(messages);
            } catch (IOException e) {
                log.error ("Error putting messages " + Logs.expand(messages, 10)
                          + ". Error was ", e);
            }
        } finally {
            if (conn != null) {
                cm.release (conn);
            }
        }
    }

    public void putMessage(Message message) throws IOException {
        ConnectionContext<Feedback> conn = null;
        try {
            conn = cm.get (remoteAddress);
            if (conn == null) {
                log.error ("Error putting message '" + message + "'. Failed to "
                           + "connect to " + remoteAddress);
                return;
            }
            Feedback feedback = conn.getConnection();
            try {
                feedback.putMessage(message);
            } catch (IOException e) {
                log.error("Error putting message '" + message + "' . Error was ",
                          e);
            }
        } finally {
            if (conn != null) {
                cm.release (conn);
            }
        }
    }
}




