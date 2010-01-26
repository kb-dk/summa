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

import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link WorkflowStep} that clears a given base in a given storage
 * by calling {@link WritableStorage#clearBase}.
 * <p/>
 * The connection to the storage will be controlled via
 * {@link StorageWriterClient} instance and any configuration parameters
 * to control the connection to the storage are documented in that class.
 * Most notably the property {@link ConnectionConsumer#CONF_RPC_TARGET}
 * <i>must</i> be defined.
 */
public class ClearBaseStep implements WorkflowStep {

    /**
     * Configuration property defining the base to clear in the configured
     * storage. This property <i>must</i> be defined.
     */
    public static final String CONF_BASE = "summa.workflow.step.clearbase.base";

    private StorageWriterClient writer;
    private String base;
    private Log log;

    public ClearBaseStep (Configuration conf) {
        log = LogFactory.getLog(this.getClass().getName());

        writer = new StorageWriterClient(conf);
        log.debug("Created storage writer for storage: "
                  + writer.getVendorId());

        try {
            base = conf.getString(CONF_BASE);
        } catch (NullPointerException e) {
            throw new ConfigurationException("The mandatory property "
                                             + CONF_BASE + " is not defined "
                                             + "in the configuration");
        }
        log.debug("Configured to clear base: '" + base + "'");
    }

    public void run() {
        try {
            log.info("Clearing base '" + base + "'");
            writer.clearBase(base);
        } catch (IOException e) {
            // FIXME: Throw a better unchecked exception. This is not too bad
            // though, because a WorkflowManager will catch it anyway...
            throw new RuntimeException("Failed to clear base '" + base
                                       + "' on " + writer.getVendorId()
                                       + ": " + e.getMessage(), e); 
        }
    }
}

