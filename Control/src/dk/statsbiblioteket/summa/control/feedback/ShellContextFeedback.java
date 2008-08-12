package dk.statsbiblioteket.summa.control.feedback;

import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.Feedback;
import dk.statsbiblioteket.summa.control.api.Message;

import java.util.List;
import java.io.IOException;

/**
 * A {@link Feedback} bridging the messages into a {@link ShellContext}.
 */
public class ShellContextFeedback implements Feedback {

    private ShellContext ctx;

    public ShellContextFeedback (ShellContext ctx) {
        this.ctx = ctx;
    }

    public void putMessages(List<Message> messages) throws IOException {
        for (Message msg : messages) {
            putMessage(msg);
        }
    }

    public void putMessage(Message message) throws IOException {
        switch (message.getMessageType()) {
            case Message.MESSAGE_PLAIN :
                ctx.info(message.getMessage());
                break;
            case Message.MESSAGE_ALERT :
                ctx.warn (message.getMessage());
            case Message.MESSAGE_REQUEST :
                ctx.prompt(message.getMessage());
                message.setResponse(ctx.readLine());
                break;
            case Message.MESSAGE_SECRET_REQUEST :
                ctx.warn ("This secret response will be displayed on screen");
                ctx.prompt(message.getMessage());
                message.setResponse(ctx.readLine());
                break;
            default:
                ctx.error ("Got unknown message type '"
                           + message.getMessageType() + "': "
                           + message.getMessage());
        }
    }

}
