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
package dk.statsbiblioteket.summa.search.tools;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.summa.common.util.MachineStats;
import dk.statsbiblioteket.summa.search.SummaSearcherFactory;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Helper class to launch a search engine from the command line.
 */
public class SummaSearcherRunner {

    private static MachineStats stats;
    
    /**
     * Create a new SummaSearcher instance as defined by the configuration
     * obtained via {@link Configuration#getSystemConfiguration(boolean true)}.
     * <p/>
     * Any paramters to this method are ignored.
     *
     * @param args ignored
     */
    public static void main (String[] args) {
        Log log = LogFactory.getLog(SummaSearcherRunner.class);

        Thread.setDefaultUncaughtExceptionHandler(
                                              new LoggingExceptionHandler(log));

        Configuration conf = Configuration.getSystemConfiguration(true);

        log.info("Creating search engine");
        try {
            SummaSearcher searcher = SummaSearcherFactory.createSearcher(conf);


            log.info("Search engine is running");

            try {
                stats = new MachineStats(conf, "Searcher");
            } catch (Exception e) {
                log.warn("Failed to create machine stats. Not critical, but "
                         + "memory stats will not be logged", e);
            }

            // Block indefinitely (non-busy)
            while(true) {
                synchronized (conf) {
                    conf.wait();
                }
            }
        } catch (Throwable t) {
            String message = String.format(
                    "Caught toplevel throwable in SummaSearcherRunner.main " 
                    + "with arguments %s", Strings.join(args, ", "));
            log.fatal(message, t);
            System.err.println(message);
            t.printStackTrace(System.err);
            new DeferredSystemExit(1, 5);
        }
    }

}

