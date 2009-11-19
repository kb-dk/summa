package dk.statsbiblioteket.summa.control.service;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.summa.common.util.MachineStats;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Simple class used to help launching a {@link Service}.</p>
 *
 * <p>The service class to instantiate will be read from the
 * system configuration's {@code control.launcher.service.class} property</p>
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
    public static final String CONF_SERVICE_CLASS =
            "control.launcher.service.class";

    private static MachineStats stats;

    public static void main(String[] args) {
        Log log = LogFactory.getLog(Launcher.class);
        Thread.setDefaultUncaughtExceptionHandler(new LoggingExceptionHandler(
                log));

        log.debug ("Getting system configuration ");

        log.debug("SecurityManager: " + System.getSecurityManager());

        try {
            Configuration conf = Configuration.getSystemConfiguration(true);

            log.trace ("Got system configuration");

            Class<? extends Service> serviceClass = null;
            try {
                serviceClass = Configuration.getClass(
                        CONF_SERVICE_CLASS, Service.class, conf);
            } catch (NullPointerException e) {
                log.fatal (String.format(
                        "Property '%s' not defined in configuration. Config "
                        + "was:\n\n%s", CONF_SERVICE_CLASS, conf.dumpString()));
                System.exit(2);
            }

            try {
                stats = new MachineStats(conf, "Launcher");
            } catch (Exception e) {
                log.warn("Failed to create machine stats. Not critical, but "
                         + "memory stats will not be logged", e);
            }

            log.debug ("Using service class " + CONF_SERVICE_CLASS
                       + " = " + serviceClass);

            Configuration.create(serviceClass, conf);
            log.debug("Created service. The launch has completed.");
        } catch (Throwable t) {
            String message = "Service launcher caught toplevel exception. "
                               + "Bailing out: " + t.getMessage();
            log.fatal(message, t);
            System.err.println(message);
            System.exit (1);
        }

    }

}



