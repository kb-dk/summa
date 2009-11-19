package dk.statsbiblioteket.summa.common.util;

import org.apache.log4j.Layout;
import org.apache.log4j.RollingFileAppender;

import java.io.IOException;

/**
 * Custom rolling file appender implementation for Log4J that rolls
 * the log files each time the JVM starts
 */
public class CleanSlateRollingFileAppender extends RollingFileAppender {

    public CleanSlateRollingFileAppender () {
        super();
    }


    public CleanSlateRollingFileAppender(Layout layout, String filename)
                                                            throws IOException {
        super(layout, filename);
    }

    public CleanSlateRollingFileAppender(
            Layout layout, String filename, boolean append) throws IOException {
        super(layout, filename, append);
    }

    @Override
    public void activateOptions() {
        super.activateOptions();
        rollOver();
    }
}
