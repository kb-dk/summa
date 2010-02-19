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
package dk.statsbiblioteket.summa.common.lucene.index;

import java.io.Serializable;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This is a generic wrapper exception for use over RMI. It is used so that the
 * client should not care about which exception are thrown (and therefore is
 * needed in client class path ).<br />
 * Exceptions part of java core libraries are wrapped in this exception,
 * otherwise an an appropriate message is given.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IndexServiceException extends Exception {

    /**
     * Constructs an IndexServiceException with an error message.
     *
     * @param message the message of the exception.
     */
    public IndexServiceException(final String message) {
        super(message);
    }

    /**
     * Constructs an IndexServiceException with both an error message, and with
     * the cause of the exception.
     *
     * @param message the message of the exception.
     * @param cause   the causing exception to the exception.
     */
    public IndexServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an IndexServiceException, wrapping the cause exception.
     *
     * @param cause the causing exception to the exception.
     */
    public IndexServiceException(final Throwable cause) {
        super(cause);
    }

}




