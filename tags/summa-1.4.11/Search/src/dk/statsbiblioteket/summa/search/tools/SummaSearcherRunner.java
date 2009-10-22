package dk.statsbiblioteket.summa.search.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.SummaSearcherFactory;

/**
 * Helper class to launch a search engine from the command line.
 */
public class SummaSearcherRunner {

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

            // Block indefinitely (non-busy)
            while(true) {
                synchronized (conf) {
                    conf.wait();
                }
            }
        } catch (Throwable t) {
            log.fatal("Caught toplevel exception: " + t.getMessage(), t);
            t.printStackTrace();
        }
    }

}
