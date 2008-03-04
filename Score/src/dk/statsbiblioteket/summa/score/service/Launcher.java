package dk.statsbiblioteket.summa.score.service;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Simple class used to help launching a {@link Service}.</p>
 *
 * <p>The service class to instantiate will be read from the
 * system configuration's {@code score.launcher.service.class} property</p>
 *
 * <p>The typical usage is to specify this class as the main class
 * in the bundle specs.</p>
 * 
 * @see Configuration#getSystemConfiguration()
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class Launcher {

    public static final String SERVICE_CLASS = "score.launcher.service.class";

    public static void main(String[] args) {
        Log log = LogFactory.getLog(Launcher.class);
        Thread.setDefaultUncaughtExceptionHandler(
                                              new LoggingExceptionHandler(log));

        log.debug ("Getting system configuration ");

        try {
            Configuration conf = Configuration.getSystemConfiguration();

            log.trace ("Got system configuration");

            Class<Service> serviceClass = conf.getClass (SERVICE_CLASS,
                                                         Service.class);
            log.debug ("Using service class " + SERVICE_CLASS
                       + " = " + serviceClass);

            Service service = conf.create (serviceClass);
            log.debug("Created service. The launch has completed.");
        } catch (Throwable t) {
            log.fatal("Caught toplevel exception, bailing out.", t);
            System.err.println ("Service launcher caught toplevel exception. "
                                + "Bailing out: " + t.getMessage());
            System.exit (1);
        }

    }

}
