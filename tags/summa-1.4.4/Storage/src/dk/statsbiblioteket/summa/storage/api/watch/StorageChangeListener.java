package dk.statsbiblioteket.summa.storage.api.watch;

import dk.statsbiblioteket.summa.storage.api.watch.StorageWatcher;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Notification interface for objects that want to monitor a
 * {@link ReadableStorage} for changes via a {@link StorageWatcher}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public interface StorageChangeListener {

    /**
     * Receive notification that there has been a change in the monitored
     * storage.
     *
     * @param watch The watcher sending the notification
     * @param base the base in which changes where detected. Possibly
     *             {@code null} if the changed base is unknown
     * @param timeStamp the system time the watcher detected the change
     * @param userData any user data that was passed to
     *                  {@link StorageWatcher#addListener}
     */
    void storageChanged (StorageWatcher watch, String base,
                         long timeStamp, Object userData);

}
