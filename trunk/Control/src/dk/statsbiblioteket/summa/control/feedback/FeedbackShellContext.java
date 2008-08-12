package dk.statsbiblioteket.summa.control.feedback;

import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.Feedback;
import dk.statsbiblioteket.summa.control.api.Message;

import java.io.IOException;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Aug 11, 2008 Time: 1:45:06 PM To
 * change this template use File | Settings | File Templates.
 */
public class FeedbackShellContext implements ShellContext {

    private Feedback feedback;
    private Log log;
    private Stack<String> lineBuffer;

    public FeedbackShellContext (Feedback feedback) {
        this.feedback = feedback;
        lineBuffer = new Stack<String>();
        log = LogFactory.getLog(FeedbackShellContext.class);

        log.trace ("Created with feedback " + feedback);
    }

    public void error(String msg) {
        try {
            feedback.putMessage(new Message(Message.MESSAGE_ALERT,
                                            "[ERROR] " + msg));
        } catch (IOException e) {
            log.warn("Failed to write error message '" + msg
                     + "' to feedback", e);
        }

    }

    public void info(String msg) {
        try {
            feedback.putMessage(new Message(Message.MESSAGE_PLAIN, msg));
        } catch (IOException e) {
            log.warn("Failed to write info message '" + msg
                     + "' to feedback", e);
        }
    }

    public void warn(String msg) {
        try {
            feedback.putMessage(new Message(Message.MESSAGE_ALERT,
                                            "[WARN] " + msg));
        } catch (IOException e) {
            log.warn("Failed to write warning '" + msg
                     + "' to feedback", e);
        }
    }

    public void debug(String msg) {
        // We use the logging level of the log to determine whether or not
        // to emit debug messages.
        if (!log.isDebugEnabled()) {
            log.trace ("Skipping debug routing of: " + msg);
            return;
        }

        try {
            feedback.putMessage(new Message(Message.MESSAGE_PLAIN,
                                            "[DEBUG] " + msg));
        } catch (IOException e) {
            log.warn("Failed to write debug message '" + msg
                     + "' to feedback", e);
        }
    }

    public String readLine() {
        if (!lineBuffer.isEmpty()) {
            String line =  lineBuffer.pop();
            log.trace ("Returning string from line buffer: " + line);
            return line;
        }

        log.trace ("Requesting line from feedback");
        try {
            Message msg = new Message(Message.MESSAGE_REQUEST, "");
            feedback.putMessage(msg);
            return msg.getResponse();
        } catch (IOException e) {
            log.warn("Failed to post readline request to feedback", e);
            return "";
        }
    }

    public void pushLine(String line) {
        if (line == null) {
            throw new NullPointerException("Can not push 'null' line");
        }

        lineBuffer.push(line);
    }

    public void prompt(String msg) {
        try {
            feedback.putMessage(new Message(Message.MESSAGE_PLAIN,
                                            msg + "> "));
        } catch (IOException e) {
            log.warn("Failed to write prompt '" + msg
                     + "' to feedback", e);
        }
    }
}
