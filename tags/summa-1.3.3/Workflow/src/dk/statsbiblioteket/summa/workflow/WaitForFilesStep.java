package dk.statsbiblioteket.summa.workflow;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.watch.FolderWatcher;
import dk.statsbiblioteket.util.watch.RecursiveFolderWatcher;
import dk.statsbiblioteket.util.watch.FolderListener;
import dk.statsbiblioteket.util.watch.FolderEvent;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link WorkflowStep} that blocks until it detects changes in a directory.
 * It is configurable whether or not changes in sub directories of the monitored
 * directory will end this step.
 */
public class WaitForFilesStep implements WorkflowStep {

    /**
     * Property defining whether the directory to monitor for changes.
     * Default value is {@link #DEFAULT_DIR}
     */
    public static final String CONF_DIR =
                                   "summa.workflow.step.waitforfiles.dir";

    /**
     * Default value for {@link #CONF_DIR}
     */
    public static final String DEFAULT_DIR = "incoming";

    /**
     * Property defining whether or not to watch child directories of the
     * monitored directory. Default value is {@link #DEFAULT_DEEP_WATCH}
     */
    public static final String CONF_DEEP_WATCH =
                                   "summa.workflow.step.waitforfiles.deepwatch";

    /**
     * Default value for {@link #CONF_DEEP_WATCH}
     */
    public static final boolean DEFAULT_DEEP_WATCH = false;

    /**
     * Property defining interval between scans of the monitored directory
     * in seconds. Default value is {@link #DEFAULT_POLL_INTERVAL}
     */
    public static final String CONF_POLL_INTERVAL =
                                "summa.workflow.step.waitforfiles.pollinterval";

    /**
     * Default value for {@link #CONF_POLL_INTERVAL}
     */
    public static final int DEFAULT_POLL_INTERVAL = 5;

    /**
     * Property defining the time that file sizes must remain stable before
     * a file change event is generated. This is useful for handling files being
     * uploaded over slow network connections or other.
     * The value is in milli seconds and the default is
     * {@link #DEFAULT_GRACE_TIME}
     */
    public static final String CONF_GRACE_TIME =
                                "summa.workflow.step.waitforfiles.gracetime";

    /**
     * Default value for {@link #CONF_GRACE_TIME}
     */
    public static final int DEFAULT_GRACE_TIME = 1000;

    /**
     * Property defining if any file change events in between separate calls
     * to {@link #run} should be dropped. If this property is {@code true}
     * changes will be dropped, if {@code false} they will trigger immediately
     * upon invocation of {@link #run}. The default value is
     * {@link #DEFAULT_FORGETFUL}
     */
    public static final String CONF_FORGETFUL =
                                "summa.workflow.step.waitforfiles.forgetful";

    /**
     * Default value for {@link #CONF_FORGETFUL}
     */
    public static final boolean DEFAULT_FORGETFUL = false;

    private static class Listener implements FolderListener {

        private boolean hasEvent;

        public Listener() {
            hasEvent = false;
        }

        public void folderChanged(FolderEvent folderEvent) {
            if (log.isDebugEnabled()) {
                log.debug("Got folder event at " + new Date());
            }

            hasEvent = true;
        }

        public boolean hasEvent() {
            return hasEvent;
        }

        public void clearEvents () {
            hasEvent = false;
        }
    }

    private static final Log log = LogFactory.getLog(WaitForFilesStep.class);

    private File watchDir;
    private boolean deepWatch;
    private boolean hasEvent;
    private boolean forgetful;
    private int pollInterval;
    private Listener listener;

    private FolderWatcher watcher;

    public WaitForFilesStep (File watchDir, boolean deepWatch,
                             int pollInterval, int graceTime,
                             boolean forgetful) throws IOException {
        this.watchDir = watchDir;
        this.deepWatch = deepWatch;
        this.pollInterval = pollInterval;
        this.forgetful = forgetful;

        hasEvent = false;

        if (deepWatch) {
            watcher = new RecursiveFolderWatcher(watchDir,-1, pollInterval,
                                                 graceTime);
        } else {
            watcher = new FolderWatcher(watchDir, pollInterval, graceTime);
        }

        listener = new Listener();
        watcher.addFolderListener(listener);
    }

     public WaitForFilesStep (Configuration conf) throws IOException {
        this(new File(conf.getString(CONF_DIR, DEFAULT_DIR)),
             conf.getBoolean(CONF_DEEP_WATCH, DEFAULT_DEEP_WATCH),
             conf.getInt(CONF_POLL_INTERVAL, DEFAULT_POLL_INTERVAL),
             conf.getInt(CONF_GRACE_TIME, DEFAULT_GRACE_TIME),
             conf.getBoolean(CONF_FORGETFUL, DEFAULT_FORGETFUL));
    }

    public void run() {
        if (forgetful && listener.hasEvent()) {
            log.debug("Forgetting file events");
            listener.clearEvents();
        } else {
            log.debug("Detected file events in between run() invocations");
            listener.clearEvents();
            return;
        }

        while (!listener.hasEvent()) {
            try {
                /* Use this.wait() instead of Thread.sleep() so that
                 * the workflow main thread can interrupt us with a
                 * this.notify()*/
                synchronized (this) {
                    this.wait(pollInterval);
                }
            } catch (InterruptedException e) {
                log.info("Interrupted while waiting for file changes");
            }
        }

        if (listener.hasEvent()) {
            log.info("File change detected");
        } else {
            log.warn("Returning without file changes");
        }

        listener.clearEvents();
    }
}
