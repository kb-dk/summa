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
package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A runtime exception thrown if there is an error loading a bundle
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class BundleLoadingException extends RuntimeException {
    /** Serial version UID. */
    private static final long serialVersionUID = 86991866841831L;
    /**
     * Default constructor.
     */
    public BundleLoadingException() {
        super();
    }

    /**
     * Exception with a message.
     * @param msg The message.
     */
    public BundleLoadingException(String msg) {
        super(msg);
    }

    /**
     * Exception with message and cause.
     * @param msg The message.
     * @param cause The cause of this exception.
     */
    public BundleLoadingException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Exception with a cause.
     * @param cause The cause of this exception.
     */
    public BundleLoadingException(Throwable cause) {
        super(cause);
    }
}

