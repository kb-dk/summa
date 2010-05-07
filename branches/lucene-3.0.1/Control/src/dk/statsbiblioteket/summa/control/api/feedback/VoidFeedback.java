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
package dk.statsbiblioteket.summa.control.api.feedback;

import dk.statsbiblioteket.summa.control.api.feedback.Feedback;
import dk.statsbiblioteket.summa.control.api.feedback.Message;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.List;

/**
 * A {@link dk.statsbiblioteket.summa.control.api.feedback.Feedback} implementation discarding all messages,
 * and answering a configurable string on all response requests.
 */
public class VoidFeedback implements Feedback, Configurable {

    /**
     * Configuration property defining the string to return to any plain message
     * requiring a response. The default value is the empty string.
     */
    public static final String CONF_PLAIN_RESPONSE =
                                        "summa.control.feedback.response.plain";

    /**
     * Configuration property defining the string to return to any message
     * requiring a a secret response. The common case here is passwords.
     * The default value is the empty string.
     */
    public static final String CONF_SECRET_RESPONSE =
                                       "summa.control.feedback.response.secret";

    private String plainResponse;
    private String secretResponse;

    public VoidFeedback (Configuration conf) {
        plainResponse = conf.getString(CONF_PLAIN_RESPONSE, "");
        plainResponse = conf.getString(CONF_SECRET_RESPONSE, "");
    }

    public VoidFeedback () {
        this (Configuration.newMemoryBased());
    }

    public void putMessages(List<Message> messages) {
        for (Message msg : messages) {
            putMessage (msg);
        }
    }

    public void putMessage(Message message) {
        switch (message.getMessageType()) {
            case Message.MESSAGE_PLAIN :
            case Message.MESSAGE_ALERT :
                break;
            case Message.MESSAGE_REQUEST :
                message.setResponse(plainResponse);
                break;
            case Message.MESSAGE_SECRET_REQUEST :
                message.setResponse(secretResponse);
                break;
            default:
                throw new RuntimeException ("Got unknown message type '"
                                            + message.getMessageType() + "': "
                                            + message.getMessage());
        }
    }
}




