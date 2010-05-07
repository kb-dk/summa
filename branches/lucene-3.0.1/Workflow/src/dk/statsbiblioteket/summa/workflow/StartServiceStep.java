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
package dk.statsbiblioteket.summa.workflow;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.control.api.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;

/**
 * A {@link WorkflowStep} that will stop a {@link Service} by calling
 * {@link Service#stop}.
 *
 * @see StopServiceStep
 */
public class StartServiceStep extends ConnectionConsumer<Service>
                             implements WorkflowStep {

    private Log log;

    public StartServiceStep (Configuration conf) {
        super(conf);
        log = LogFactory.getLog(this.getClass().getName());

        log.debug("Configured to start service: '" + getVendorId() + "'");
    }

    public void run() {
        Service service = getConnection();
        try {
            log.debug("Starting " + getVendorId());
            service.start();
        } catch (RemoteException e) {
            throw new RuntimeException("Error starting service "
                                       + getVendorId() + ": " + e.getMessage(),
                                       e);
        } finally {
            releaseConnection();
        }        
    }
}

