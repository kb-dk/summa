package dk.statsbiblioteket.summa.search.tools;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.summa.common.util.MachineStats;
import dk.statsbiblioteket.summa.search.SummaSearcherFactory;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
            log.fatal("Caught toplevel exception: " + t.getMessage(), t);
            t.printStackTrace();
            System.exit(1);
        }
    }

}
