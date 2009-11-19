package dk.statsbiblioteket.summa.common.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Simple class that logs any uncaught exceptions via the commons-logging
 * configured logging mechanism.</p>
 * <p>Exceptions "caught" this way will still kill the running thread.</p>
 * <p>Example:<br/>
 * <code>
 *  Thread.setDefaultUncaughtExceptionHandler(new LoggingExceptionHandler());
 * </code></p>
 * </p><p>
 * The behaviour on uncaught Exceptions is to shut down the JVM.
 */
public class LoggingExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Log log;

    /**
     * Create a new {@code LoggingExceptionHandler} with a private logger.
     */
    public LoggingExceptionHandler () {
        log = LogFactory.getLog(LoggingExceptionHandler.class);
    }

    /**
     * Create a new {@code LoggingExceptionHandler} with a given logger.
     * @param log the log to use for uncaught exceptions
     */
    public LoggingExceptionHandler (Log log) {
        this.log = log;
    }

    public void uncaughtException(Thread thread, Throwable e) {
        String message = String.format(
                "Uncaught exception in thread '%s'. Processing is unstable "
                + "and as a result, the JVM will be terminated in 5 seconds", 
                thread);
        log.fatal(message, e);
        System.err.println(message);
        e.printStackTrace(System.err);
        new DeferredSystemExit(1, 5);
    }
}



