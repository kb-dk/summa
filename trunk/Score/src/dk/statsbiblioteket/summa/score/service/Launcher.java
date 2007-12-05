package dk.statsbiblioteket.summa.score.service;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.summa.score.api.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Simple class used to help launching a {@link Service}.</p>
 *
 * <p>The service class to instantiate will be read from the
 * system configuration's {@code score.launcher.service.class} property</p>
 *
 * @see Configuration#getSystemConfiguration()
 */
public class Launcher {

    public static final String SERVICE_CLASS = "score.launcher.service.class";

    public static void main (String[] args) {
        Log log = LogFactory.getLog (Launcher.class);
        Thread.setDefaultUncaughtExceptionHandler(
                                              new LoggingExceptionHandler(log));

        log.debug ("Getting system configuration ");

        Configuration conf = Configuration.getSystemConfiguration();

        log.trace ("Got system configuration");

        Class<Service> serviceClass = conf.getClass (SERVICE_CLASS,
                                                     Service.class);
        log.debug ("Using service class " + SERVICE_CLASS
                   + " = " + serviceClass);

        Service service = conf.create (serviceClass);

    }

}
