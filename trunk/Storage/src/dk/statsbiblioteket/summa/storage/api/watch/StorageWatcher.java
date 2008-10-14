package dk.statsbiblioteket.summa.storage.api.watch;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.IOException;

/**
 * FIXME: Batching of events if there are many? We should probably handle that
 *        with care since if allow user generated content in the storage things
 *        might change all the time
 */
public class StorageWatcher implements Configurable, Runnable {

    /**
     * Configuration property defining the number of milliseconds between
     * polling {@link ReadableStorage#isModifiedAfter(long,String)}. Defaults
     * to {@link #DEFAULT_POLL_INTERVAL}
     */
    public static final String CONF_POLL_INTERVAL =
                                           "summa.storage.watcher.pollinterval";

    /**
     * Default value for {@link #CONF_POLL_INTERVAL}
     */
    public static final int DEFAULT_POLL_INTERVAL = 10000;

    private static final String BASE_WILDCARD = "*";

    private static class ListenerContext {
        private List<String> bases;
        private Object userData;
        private StorageChangeListener l;

        public ListenerContext(StorageChangeListener l,
                               List<String> bases,
                               Object userData) {
            this.l = l;
            this.bases = bases;
            this.userData = userData;
        }

        public StorageChangeListener getListener () {
            return l;
        }

        public List<String> getBases () {
            return bases;
        }

        public Object getUserData () {
            return userData;
        }
    }

    private ReadableStorage reader;
    private Thread thread;
    private boolean mayRun;
    private Map<StorageChangeListener,ListenerContext> listeners;
    private Set<String> bases;
    private int pollInterval;
    private long lastCheck;

    private Log log;

    public StorageWatcher (ReadableStorage reader, int pollInterval) {
        log = LogFactory.getLog(StorageWatcher.class);

        this.pollInterval = pollInterval;
        this.reader = reader;

        lastCheck = System.currentTimeMillis();
        bases = new HashSet<String>(10);
        listeners = new HashMap<StorageChangeListener,ListenerContext>();

        thread = new Thread(this);
        thread.setDaemon(true); // Allow JVM to exit when watcher is running

        mayRun = false;
    }

    public StorageWatcher (ReadableStorage reader) {
        this (reader, DEFAULT_POLL_INTERVAL);
    }

    public StorageWatcher (Configuration conf) {
        this(new StorageReaderClient(conf));

        pollInterval = conf.getInt(CONF_POLL_INTERVAL, DEFAULT_POLL_INTERVAL);
    }

    public void start () {
        log.info ("Starting");
        mayRun = true;
        thread.start();
    }

    public void stop () {
        log.debug ("Stopping");
        mayRun = false;
        thread.interrupt();

        try {
            thread.join();
        } catch (InterruptedException e) {
            log.warn("Interrupted while joining watcher thread");
        }

        log.info("Stopped");
    }

    /**
     * Notify all listeners on {@code base} that there has been a change in the
     * storage at time {@code eventTime}.
     * @param base the base in which there has been changes
     * @param eventTime the time these changes where detected by the watcher
     */
    public void notifyListeners (String base, long eventTime) {
        if (log.isTraceEnabled()) {
            log.trace ("Notifying listeners on base: "
                       + (base != null ? base : BASE_WILDCARD));
        }

        for (ListenerContext ctx : listeners.values()) {

            if (base == null && ctx.getBases() == null) {
                ctx.getListener().storageChanged(this, null, eventTime,
                                                 ctx.getUserData());


            } else if (ctx.getBases() != null &&
                       ctx.getBases().contains(base)) {
                ctx.getListener().storageChanged(this, base, eventTime,
                                                 ctx.getUserData());
            }
        }
    }

    public void run() {
        while (mayRun) {
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                log.debug("Interrupted");
            }

            if (log.isTraceEnabled()) {
                log.trace ("Polling storage with interval: "
                           + getPollInterval() + " ms");
            }

            long now = System.currentTimeMillis();

            for (String base : bases) {
                if (log.isTraceEnabled()) {
                    log.trace ("Polling base: " + base);
                }

                if (BASE_WILDCARD.equals(base)) {
                    base = null;
                }

                try {
                    if (reader.isModifiedAfter(lastCheck, base)) {
                        notifyListeners(base, now);
                    }
                } catch (IOException e) {
                    log.warn("Error connecting to storage "
                             + reader + ", base '"
                             + base + "': " + e.getMessage(), e);
                }

                lastCheck = now;
            }
        }
    }

    /**
     * Add a listener requesting notification for changes on any of the bases
     * in {@code bases}. If {@code bases==null} changes to any base will trigger
     * the listener
     *
     * @param l the listener to notify on changes
     * @param monitoredBases the bases to monitor. May be {@code null}
     * @param userData any user data to pass back to the listener on changes.
     *                 May be {@code null}
     */
    public void addListener (StorageChangeListener l,
                             List<String> monitoredBases, Object userData) {

        listeners.put (l, new ListenerContext(l, monitoredBases, userData));

        if (monitoredBases != null) {
            log.debug("Adding listener " + l + ", on bases: "
                  + Strings.join(monitoredBases, ", "));
            bases.addAll(monitoredBases);
        } else {
            log.debug("Adding listener " + l + ", on all bases");
            bases.add (BASE_WILDCARD);
        }
    }

    public void setPollInterval (int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getPollInterval () {
        return pollInterval;
    }
}
