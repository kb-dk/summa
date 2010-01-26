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

import dk.statsbiblioteket.summa.control.api.feedback.Message;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.List;
import java.io.IOException;

/**
 * Provides feedback with optional requests for text input.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface Feedback extends Configurable {
    /**
     * Display the given messages to the user and collect responses. This
     * could be sequentially on a prompt or all at a time at a webpage.
     * This method can be called more than once.
     * @param messages the messages to display and collect responses for.
     * @throws IOException if the is a communication error
     */
    public void putMessages(List<Message> messages) throws IOException;

    /**
     * Convenience method for {@link #putMessages}.
     * @param message the message to display and collect a response for.
     * @throws IOException if the is a communication error 
     */
    public void putMessage(Message message) throws IOException;
}




