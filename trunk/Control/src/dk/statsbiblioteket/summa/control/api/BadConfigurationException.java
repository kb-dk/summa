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
package dk.statsbiblioteket.summa.control.api;

import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Exception thrown when a {@link Client} or {@link Service} receives a
 * configuration that is insufficient or malformed in a such way that they
 * cannot continue operation.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class BadConfigurationException extends RuntimeException {

    /**
     * @param message message about the exact configuration paramter causing
     *        the problem
     */
    public BadConfigurationException(String message) {
        super(message);
    }

    /**
     * Create a {@code BadConfigurationException} with a message and a cause.
     * @param msg the message to include
     * @param cause the {@code Throwable} that triggered the exception
     */
    public BadConfigurationException (String msg, Throwable cause) {
        super (msg, cause);
    }
}




