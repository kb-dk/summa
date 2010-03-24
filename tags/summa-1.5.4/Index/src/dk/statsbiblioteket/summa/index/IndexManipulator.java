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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;

/**
 * A manipulator is responsible for updating part of an index. This could be a
 * Lucene index, the persistent structure for Facets or SearchDescriptor.
 * If the specific part of the index is not initialized upon start, the
 * manipulator is responsible for the initialization.
 * </p><p>
 * Manipulators should not do anything active to any index, before the open-
 * method has been called.
 * </p><p>
 * Note that it is expected that readers of the index can connect to the index
 * at any time, so manipulators /must/ ensure that the persistent files are
 * kept in a readable state at all time.
 * </p><p>
 * Note: The field RecordID is mandatory for the searchable index (typically
 * Lucene). The index should be stored for debug reasons and must be indexed.
 * The RecordID is the unique identifier for the Record and thus one and only
 * one Document in a given index has any given RecordID.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface IndexManipulator extends Configurable {

    /**
     * The fully qualified class-name for a manipulator. Used to create an
     * IndexManipulator through reflection or via a {@link ManipulatorFactory}.
     * <p/>
     * This property must be present in all subconfigurations listed by
     * {@link IndexControllerImpl#CONF_MANIPULATORS}.
     * </p><p>
     * This property is mandatory. No default.
     */
    public static final String CONF_MANIPULATOR_CLASS =
            "summa.index.manipulatorclass";

    /**
     * If an index exists at the indexRoot, the manipulator should open it.
     * If not, the manipulator should create it. In either case, the manipulator
     * must be ready for updates after open has been called.
     * </p><p>
     * If a manipulator is already open, when open is called, all cached content
     * should be discarded. If is the responsibility of the caller to ensure
     * that close has been called, if a gracefull shutdown is required.
     * @param indexRoot the root location for the index. It is allowed to create
     *                  sub-folders to this root for index structures, but
     *                  manipulators should not put files highter in the folder
     *                  structure than indexRoot.
     * @throws IOException if an index could not be opened.
     */
    public void open(File indexRoot) throws IOException;

    /**
     * Clear the index, preparing for a fresh start. This discards all cached
     * data.
     * @throws IOException in case of I/O problems.
     */
    public void clear() throws IOException;

    /**
     * Update the given payload in the index. The nature of the update can be
     * determined by the Record in the payload and spans new, changed and
     * deleted.
     * </p><p>
     * Manipulators are required to preserve the order of added payloads in the
     * index-updating precess, but in the event of changed or deleted payloads,
     * ordering can be ignored. This is due to Lucene being slow at deletes.
     * @param payload the parload containing a Document to add, change or
     *                delete in the index.
     * @throws IOException in case of I/O problems.
     * @return true   if a commit is requested. This might be to reasons such as
     *                internal buffers being full.
     */
    public boolean update(Payload payload) throws IOException;

    /**
     * Commit any cached structures to persistent storage and ensure that new
     * searchers will open the commited index parts. It is strongly recommended
     * to optimize this call for time, so that commits are cheap. This ensures
     * short turn-around times for index updated.
     * @throws IOException in case of I/O problems.
     */
    public void commit() throws IOException;

    /**
     * Perform clean-up and optimization of index parts. For Lucene indexes,
     * this will most probably correspond to a call to optimize(). This call
     * might take some time to perform and will normally be called as part
     * of a batch clean-up at times with light workload.
     * @throws IOException in case of I/O problems.
     */
    public void consolidate() throws IOException;

    /**
     * Ensure that all content is flushed to persistent storage and close down
     * any open connections. After a close has been called, the only allowable
     * action on a manipulator is open.
     * @throws IOException in case of I/O problems.
     */
    public void close() throws IOException;

    /**
     * Notifies the manipulator that the order of indexed Records might have
     * changed since last commit. This might be caused by e.x. a delete of
     * an existing Lucene document, followed by a merge. 
     * @throws IOException in case of I/O problems.
     */
    public void orderChangedSinceLastCommit() throws IOException;

    /**
     * This method is used for signalling other manipulators. It will normally
     * be called once for each Record. Consolidates always imply a commit.
     * @return true if this manipulator might have changed the order of indexed 
     * Records since last commit.
     * @throws IOException in case of I/O problems.
     */
    public boolean isOrderChangedSinceLastCommit() throws IOException;
}

