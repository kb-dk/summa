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
    private Payload payload = null;

    public PayloadException() {
        super();
    }

    public PayloadException(String message) {
        super(message);
    }

    public PayloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public PayloadException(Throwable cause) {
        super(cause);
    }

    public PayloadException(Payload payload) {
        super();
        this.payload = payload;
    }

    public PayloadException(String message, Payload payload) {
        super(message);
        this.payload = payload;
    }

    public PayloadException(String message, Throwable cause, Payload payload) {
        super(message, cause);
        this.payload = payload;
    }

    public PayloadException(Throwable cause, Payload payload) {
        super(cause);
        this.payload = payload;
    }

    public Payload getPayload() {
        return payload;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (payload == null ? "" : payload.toString());
    }
}

