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

import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.feedback.Feedback;
import dk.statsbiblioteket.summa.control.api.feedback.Message;

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
    private String lastError;

    public FeedbackShellContext (Feedback feedback) {
        this.feedback = feedback;
        lineBuffer = new Stack<String>();
        lastError = null;
        log = LogFactory.getLog(FeedbackShellContext.class);

        log.trace ("Created with feedback " + feedback);
    }

    public void error(String msg) {
        lineBuffer.clear();
        lastError = msg;
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

    public String getLastError() {
        return lastError;
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




