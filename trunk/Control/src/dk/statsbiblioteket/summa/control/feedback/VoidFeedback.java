package dk.statsbiblioteket.summa.control.feedback;

import dk.statsbiblioteket.summa.control.api.Feedback;
import dk.statsbiblioteket.summa.control.api.Message;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.List;
import java.io.IOException;

/**
 * A {@link Feedback} implementation discarding all messages,
 * and answering a configurable string on all response requests.
 */
public class VoidFeedback implements Feedback, Configurable {

    /**
     * Configuration property defining the string to return to any plain message
     * requiring a response. The default value is the empty string.
     */
    public static final String PROP_PLAIN_RESPONSE =
                                        "summa.control.feedback.response.plain";

    /**
     * Configuration property defining the string to return to any message
     * requiring a a secret response. The common case here is passwords.
     * The default value is the empty string.
     */
    public static final String PROP_SECRET_RESPONSE =
                                       "summa.control.feedback.response.secret";

    private String plainResponse;
    private String secretResponse;

    public VoidFeedback (Configuration conf) {
        plainResponse = conf.getString(PROP_PLAIN_RESPONSE, "");
        plainResponse = conf.getString(PROP_SECRET_RESPONSE, "");
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
