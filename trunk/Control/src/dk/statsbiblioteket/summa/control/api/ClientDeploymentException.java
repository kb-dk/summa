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

/**
 * Thrown when there is an error deploying or starting a client
 */
public class ClientDeploymentException extends RuntimeException {
    private static final long serialVersionUID = 2555484788L;
    public ClientDeploymentException (String msg) {
        super(msg);
    }

    public ClientDeploymentException (Throwable cause) {
        super(cause);
    }

    public ClientDeploymentException (String msg, Throwable cause) {
        super(msg, cause);
    }

}




