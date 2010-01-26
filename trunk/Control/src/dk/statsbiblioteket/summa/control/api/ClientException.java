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
 * Generic base exception for {@link Client} objects
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Some methods needs Javadoc")
public class ClientException extends RuntimeException {

    private String id;

    public ClientException (String clientId, String message) {
        super ("For client id '" + clientId + "': " + message);
        id = clientId;
    }

    public ClientException (Client client, String message) {
        super ("Client[" + getClientId(client) + "] " + message);
        id = client.getId();
    }

    public ClientException (Client client, String message, Throwable cause) {
        super ("Client[" + getClientId(client) + "] " + message, cause);
        id = client.getId();
    }

    public ClientException (Client client, Throwable cause) {
        super ("Client[" + getClientId(client) + "]", cause);
        id = client.getId();
    }

    public String getClientId () {
        return id;
    }

    private static String getClientId (Client client) {
        return client.getId();
    }
}




