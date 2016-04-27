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

import java.io.IOException;

/**
 * An interface for objects that have a state that can be monitored.
 * This is mostly targeted at remote services.
 *
 * @see StatusMonitor
 */
public interface Monitorable {

    /**
     * Get the status of the object.
     * 
     * @return the status of the object. In case the object can not be contacted,
     *         fx. when it is not running, {@code null} should be returned
     * @throws IOException if there are errors commnunicating with the object
     */
    public Status getStatus () throws IOException;
}




