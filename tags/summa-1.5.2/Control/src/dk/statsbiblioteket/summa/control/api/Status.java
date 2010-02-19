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

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;

/**
 * Generic object to represent the status of a ClientManager service or client.
 * @see Service
 * @see dk.statsbiblioteket.summa.control.client.Client
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Status implements Serializable {
    public static enum CODE {
        /**
         * The object does not exist, but can be instantiated. This state
         * is typically returned by some service proxying the object.
         * For example a ClientManager Client managing a service returns this
         * state if the service is deployed, but not running.  
         */
        not_instantiated,
        /**
         * This object has been constructed and is ready for start.
         */
        constructed,
        /**
         * The object is initialising its state.
         */
        startingUp,
        /**
         * The object is ready for action.
         */
        idle,
        /**
         * The object state is running - further requests to the object might
         * be delayed.
         */
        running,
        /**
         * The object has experienced a failure and is recovering.
         */
        recovering,
        /**
         * The object has been intentionally terminated.
         */
        stopping,
        /**
         * The object has been intentionally terminated.
         */
        stopped,
        /**
         * The structure of the object has been compromised, so that it
         * can no longer function. Termination is required.
         */
        crashed
    }

    private CODE code;
    private String message;

    /**
     * Create a new Status object.
     * @param code Status code for status object
     * @param message Message with explanatory text
     */
    public Status(CODE code, String message) {
        this.code = code;
        this.message = message;
    }

    public String toString() {
        return "<"+code+":"+message+">";
    }

    public CODE getCode () {
        return code;
    }
}




