package dk.statsbiblioteket.summa.storage.api.watch;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.IOException;

/**
 * Change notification mechanism for a Summa storage service. Change
 * notification can be controlled on a per-base level.
 * <p/>
 * The implementation relies on {@link ReadableStorage#getModificationTime}
 * and does active polling to keep track of changes.
 * The implementation only relies on the server side system time to avoid
 * problems with the client and server system times not matching up. In fact
 * the timestamps are tracked on a per-base level to make sure that the watcher
 * will also work with a
 * {@link dk.statsbiblioteket.summa.storage.AggregatingStorage} where each
 * base might map to a different server.
 * <p/>
 * <h3>Waiting for StorageWatcher Signals</h3>
 * If you need to perform a blocking wait for notifications from the storage
 * watcher you don't even have to implement the {@link StorageChangeListener}
 * interface or to subscribe to events. Simply call the standard {@link #wait}
 * method from the Java {@link Object} class. Note however that you should be
 * cautious of <i>"spurious interrupts"</i> - please see the Java documentation
 * for {@link Object#wait} on how to handle this.
 * <p/>
 * <b>FIXME:</b> Batching of events if there are many? We should probably handle
 *               that with care since if allow user generated content in the
 *               storage things might change all the time
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class StorageWatcher implements Configurable, Runnable {

    /**
     * Configuration property defining the number of milliseconds between
     * polling {@link ReadableStorage#getModificationTime(String)}. Defaults
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
    private Map<String,Long> pollTimes;
    private Map<String,Long> notifyTimes;
    private long startTime;

    private Log log;

    public StorageWatcher (ReadableStorage reader, int pollInterval) {
        log = LogFactory.getLog(StorageWatcher.class);

        this.pollInterval = pollInterval;
        this.reader = reader;

        startTime = System.currentTimeMillis();
        pollTimes = new HashMap<String,Long>(10);
        notifyTimes = new HashMap<String,Long>(10);
        bases = new HashSet<String>(10);
        listeners = new HashMap<StorageChangeListener,ListenerContext>();

        thread = new Thread(this, this.getClass().getSimpleName());
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
    public synchronized void notifyListeners (String base, long eventTime) {
        if (log.isTraceEnabled()) {
            log.trace ("Notifying listeners on base: "
                       + (base != null ? base : BASE_WILDCARD));
        }

        notifyTimes.put(base, System.currentTimeMillis());

        // Notify threads blocking on wait()
        notifyAll();

        for (ListenerContext ctx : listeners.values()) {

            if (ctx.getBases() == null && base == null) {
                /* Listeners subscribed to all changes should only be notified
                 * here to prevent multiple notifications on the same change */
                ctx.getListener().storageChanged(this, null, eventTime,
                                                 ctx.getUserData());
            } else if (ctx.getBases() != null &&
                       ctx.getBases().contains(base)) {
                ctx.getListener().storageChanged(this, base, eventTime,
                                                 ctx.getUserData());
            }
        }
    }

    /**
     * Get the system time for the last notification emitted on {@code base}
     *
     * @param base the base to check for notifications
     * @return system time as reported by {@link System#currentTimeMillis()}
     *         at the time of the last call to {@link #notifyListeners} or
     *         {@code -1} if no notifications has been emitted on {@code base}
     */
    public long getLastNotify (String base) {
        Long lastNotify = notifyTimes.get(base);

        if (lastNotify == null) {
            return -1;
        }

        return lastNotify;
    }

    public void run() {
        while (mayRun) {
            if (log.isTraceEnabled()) {
                log.trace ("Polling storage with interval: "
                           + getPollInterval() + " ms");
            }

            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                log.debug("Interrupted");
            }

            if (!mayRun) {
                log.debug("Skipping check as mayRun is false");
                return;
            }

            for (String base : bases) {
                if (log.isTraceEnabled()) {
                    log.trace("Polling base: " + base);
                }

                Long lastCheck = pollTimes.get(base);

                if (lastCheck == null) {
                    log.warn("No timestamp for base '" + base + "'. Skipping");
                    updatePollTimes ();
                    continue;
                }

                try {
                    String b = BASE_WILDCARD.equals(base) ? null : base;
                    Long mtime = reader.getModificationTime(b);

                    /* If we have changes notify the listeners and store the new
                     * timestamp */
                    if (mtime > lastCheck) {
                        notifyListeners(b, mtime);
                        pollTimes.put(base, mtime);
                    }
                } catch (IOException e) {
                    log.warn("Error connecting to storage "
                             + reader + ", base '"
                             + base + "': " + e.getMessage(), e);
                }
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

        updatePollTimes();
    }

    private void updatePollTimes () {
        for (String base : bases) {
            if (!pollTimes.containsKey(base)) {
                try {
                    log.debug("Getting initial timestamp for base '" + base
                              + "'");
                    pollTimes.put(base, reader.getModificationTime(base));
                    log.trace("Got initial timestamp for base '" + base + "'");
                } catch (IOException e) {
                    log.warn("Failed to update timestamp for base '" + base
                             + "': " + e.getMessage (), e);
                }
            }

        }
    }

    public void setPollInterval (int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getPollInterval () {
        return pollInterval;
    }
}
