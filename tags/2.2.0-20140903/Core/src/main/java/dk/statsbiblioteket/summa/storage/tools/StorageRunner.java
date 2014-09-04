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
package dk.statsbiblioteket.summa.storage.tools;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.summa.common.util.MachineStats;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A small utility to launch a Storage instance
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class StorageRunner {

    public static final Boolean shutdown = false;

    /**
     * Create a new storage instance as defined by the configuration
     * obtained via {@link Configuration#getSystemConfiguration(boolean true)}.
     * <p/>
     * Any parameters to this method are ignored.
     *
     * @param args ignored
     */
    @SuppressWarnings({"CallToPrintStackTrace", "UnusedDeclaration", "UseOfSystemOutOrSystemErr"})
    public static void main (String[] args) {
        Log log = LogFactory.getLog(StorageRunner.class);

        Thread.setDefaultUncaughtExceptionHandler(new LoggingExceptionHandler(log));

        Configuration conf = Configuration.getSystemConfiguration(true);        

        log.debug("Creating storage instance");
        try {
            Storage storage = StorageFactory.createStorage(conf);
            log.info("Storage is running in the background");
            try {
                MachineStats stats = new MachineStats(conf, "Storage");
            } catch (Exception e) {
                log.warn("Failed to create machine stats. Not critical, but memory stats will not be logged", e);
            }

            // Block indefinitely (non-busy)
            while (!shutdown) {
                synchronized (shutdown) {
                    shutdown.wait();
                }
            }
        } catch (Throwable t) {
            Logging.fatal(log, "StorageRunner-main", "Caught toplevel exception", t);
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
