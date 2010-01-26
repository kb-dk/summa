/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
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

