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
                break;
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




