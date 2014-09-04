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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Thrown in case of problems with a specific Payload.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PayloadException extends Exception {
    /** The serial version UID. */
    private static final long serialVersionUID = 2348343L;
    /** The payload which was the reason for this exception. */
    private Payload payload = null;

    /**
     * Constructs an empty payload exception.
     */
    public PayloadException() {
        super();
    }

    /**
     * Constructs a payload exception with a message.
     * @param message The message.
     */
    public PayloadException(String message) {
        super(message);
    }

    /**
     * Constructs a payload exception with a message and the cause.
     * @param message The message for this exception.
     * @param cause The cause for this exception.
     */
    public PayloadException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a payload exception with a cause.
     * @param cause The cause of this exceptopn.
     */
    public PayloadException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a payload exception with a payload.
     * @param payload The paylod responsible.
     */
    public PayloadException(Payload payload) {
        super();
        this.payload = payload;
    }

    /**
     * Constructs a payload
     * @param message The message for this exception.
     * @param payload The paylod responsible.
     */
    public PayloadException(String message, Payload payload) {
        super(message);
        this.payload = payload;
    }

    /**
     * Constructs a payload exception with message, the cause and the
     * responsible payload.
     * @param message The message for this exception.
     * @param cause The cause of this exception.
     * @param payload The paylod responsible.
     */
    public PayloadException(String message, Throwable cause, Payload payload) {
        super(message, cause);
        this.payload = payload;
    }

    /**
     * Constructs a payload exception with a cause and a payload.
     * @param cause The cause of this exception.
     * @param payload The paylod responsible.
     */
    public PayloadException(Throwable cause, Payload payload) {
        super(cause);
        this.payload = payload;
    }

    /**
     * Return the payload responsible for this exception.
     * @return The payload responsible for this exception.
     */
    public Payload getPayload() {
        return payload;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (payload == null ? "" : ": " + payload.toString());
    }
}

