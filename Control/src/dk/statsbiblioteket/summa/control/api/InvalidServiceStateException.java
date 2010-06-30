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
 * <p>Thrown when somebody tries to perform an action on a service
 * which does not make sense in the service's current state.</p>
 *
 * <p>For example thrown if somebody tries to stop a non-running service.</p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class InvalidServiceStateException extends ClientException {
    private static final long serialVersionUID = 777933331894684L;
    /**
     *
     * @param client the client encountering the exceptional state
     * @param serviceId the id of the service which is involved
     * @param action the action which was requested to be performed on the service
     * @param cause Short string describing the cause of the problem
     */
    public InvalidServiceStateException (Client client, String serviceId,
                                         String action, String cause) {
        super (client, "Request '" + action + "' on service '" + serviceId
                + "' is illegal. Cause: " + cause);
    }

    /**
     *
     * @param clientId the instance id of the client encountering the
     *                 exceptional state
     * @param serviceId the id of the service which is involved
     * @param action the action which was requested to be performed on the service
     * @param cause Short string describing the cause of the problem
     */
    public InvalidServiceStateException (String clientId, String serviceId,
                                         String action, String cause) {
        super (clientId, "Request '" + action + "' on service '" + serviceId
                + "' is illegal. Cause: " + cause);
    }
}




