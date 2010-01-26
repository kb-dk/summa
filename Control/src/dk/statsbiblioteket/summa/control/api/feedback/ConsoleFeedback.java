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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Console;
import java.io.PrintWriter;
import java.util.List;

/**
 * Simple implementation of Feedback, that writes and reads from the
 * console.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Some methods neeeds Javadoc")
public class ConsoleFeedback implements Feedback {

    private Console in;
    private PrintWriter out;

    public ConsoleFeedback () {
        in = System.console();
        
        if (in == null) {
            throw new RuntimeException("Unable to get system console");
        }

        out = System.console().writer();
    }

    public ConsoleFeedback (Configuration conf) {
        this();
    }

    /**
     * Output all the messages sequentially to the console and collect
     * requested responses.
     * @param messages the messages to display and get responses for.
     */
    public void putMessages(List<Message> messages) {
        for (Message message: messages) {
            putMessage(message);
        }
    }

    public void putMessage(Message message) {
        switch (message.getMessageType()) {
            case Message.MESSAGE_PLAIN:
                out.println(message);
                out.println("");
                break;
            case Message.MESSAGE_ALERT:
                out.print("[ALERT] ");
                out.println(message);
                out.println("");
                break;
            case Message.MESSAGE_REQUEST:
                out.println(message);
                message.setResponse(in.readLine());
                break;
            case Message.MESSAGE_SECRET_REQUEST:
                out.println(message);
                message.setRawResponse(in.readPassword());
                break;
            default:
                out.println("[ERROR] Unknown message type: "
                        + message.getMessageType());
        }
    }
}




