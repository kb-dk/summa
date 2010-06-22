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
package dk.statsbiblioteket.summa.index.lucene;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * Handles iterative updates of a Lucene index. A Record that is an update of an
 * already ingested Record is treated as a deletion followed by an addition.
 * </p><p>
 * The manipulator maintains a cache of RecordIDs in the given index, mapped to
 * LuceneIDs. This consumes memory linear to the number of Documents. The cache
 * is filled upon opening of an index.
 * </p><p>
 * The RecordID is extracted from incoming Payloads as the id from the embedded
 * Records. If no Record exists, the id is extracted from the embedded
 * Document as field RecordID.
 */
// TODO: Add maximum number of segments property for consolidate
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneManipulator implements IndexManipulator {
    private static Log log = LogFactory.getLog(LuceneManipulator.class);

    /**
     * The amount of Payloads to buffer before a flush is requested.
     * -1 means no flushes are requested.
     * </p><p>
     * This property is optional. Default is -1 (disabled).
     */
    public static final String CONF_BUFFER_SIZE_PAYLOADS =
            "summa.index.lucene.buffersizepayloads";
    public static final int DEFAULT_BUFFER_SIZE_PAYLOADS = -1;

    /**
     * The amount of MB in the Lucene buffer before a flush is requested.
     * </p><p>
     * This property is optional. Default is
     * {@link IndexWriter#DEFAULT_RAM_BUFFER_SIZE_MB} (16.0 MB).
     */
    public static final String CONF_BUFFER_SIZE_MB =
            "summa.index.lucene.buffersizemb";
    public static final double DEFAULT_BUFFER_SIZE_MB =
            IndexWriter.DEFAULT_RAM_BUFFER_SIZE_MB;

    /**
     * The number of Threads to use while adding or deleting Documents to the
     * Lucene index. Note that numbers higher than 1 will result in non-reliable
     * ordering of documents in the index, meaning that chained manipulators,
     * such as the FacetManipulator, will have to compensate. For the
     * FacetManipulator this means a full rebuild upon commit and consolidate.
     * </p><p>
     * Warning: This is experimental. If in doubt, leave it at 1.
     * </p><p>
     * Optional. Default is 1.
     */
    public static final String CONF_WRITER_THREADS =
            "summa.index.lucene.writerthreads";
    public static final int DEFAULT_WRITER_THREADS = 1;

    /**
     * The maximum number of segments after a consolidate. Setting this to 1
     * increases consolidation time considerably on large (multi-GB) indexes.
     * Setting this to a high number (20+?) decreases search-performance on
     * conventional harddisks. Setting this very high (200+) taxes the system
     * for file handles.
     * </p><p>
     * This property is optional. Default is 5.
     */
    public static final String CONF_MAX_SEGMENTS_ON_CONSOLIDATE =
            "summa.index.lucene.consolidate.maxsegments";
    public static final int DEFAULT_MAX_SEGMENTS_ON_CONSOLIDATE = 5;

    /**
     * If true, deletes are expunged from the index upon commit. If subsequent
     * index manipulators relies on the docIDs to be consistent after deletes,
     * this needs to be true. If there is no such need - e.g. if the subsequent
     * manipulators build their index from scratch after commit - this should be
     * set to false, at it makes commits *much* faster.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_EXPUNGE_DELETES_ON_COMMIT =
            "summa.index.lucene.expungedeletesoncommit";
    public static final boolean DEFAULT_EXPUNGE_DELETES_ON_COMMIT = true;

    /** The index descriptor, used for providing Analyzers et al. */
    private LuceneIndexDescriptor descriptor;
    /** The general index folder, which contains the concrete index-parts. */
    private File indexRoot;
    /** The directory with the Lucene index. This will be a sub-folder to
     * indexRoot. */
    private FSDirectory indexDirectory;
    /** The connection to the Lucene index */
    private IndexWriter writer;

    /*
     * Keeps track of RecordIDs. The idMapper is used and updated by
     * {@link #updateAddition}.
     */
//    private IDMapper idMapper;

    private int bufferSizePayloads = DEFAULT_BUFFER_SIZE_PAYLOADS;
    private double buffersizeMB = DEFAULT_BUFFER_SIZE_MB;
    private int maxMergeOnConsolidate =
                                    DEFAULT_MAX_SEGMENTS_ON_CONSOLIDATE;
    private int writerThreads = DEFAULT_WRITER_THREADS;
    private boolean expungeDeleted = DEFAULT_EXPUNGE_DELETES_ON_COMMIT;

    private boolean orderChanged = false;
    // WriterCallables automatically reappears in available after use
    private BlockingQueue<WriterCallable> available;
    private ExecutorService executor;

    public LuceneManipulator(Configuration conf) {
        bufferSizePayloads = conf.getInt(
                CONF_BUFFER_SIZE_PAYLOADS, bufferSizePayloads);
        String bsMB = conf.getString(
                CONF_BUFFER_SIZE_MB, Double.toString(buffersizeMB));
        try {
            buffersizeMB = Double.parseDouble(bsMB);
        } catch (NumberFormatException e) {
            log.warn(String.format(
                    "Unable to parse '%s' from %s as a double",
                    bsMB, CONF_BUFFER_SIZE_MB));
        }
        maxMergeOnConsolidate = conf.getInt(
                CONF_MAX_SEGMENTS_ON_CONSOLIDATE, maxMergeOnConsolidate);
        writerThreads = conf.getInt(CONF_WRITER_THREADS, writerThreads);
        if (writerThreads < 1) {
            throw new ConfigurationException(
                    "The number of writer threads must be > 0. It was "
                    + writerThreads);
        }
        expungeDeleted = conf.getBoolean(
                CONF_EXPUNGE_DELETES_ON_COMMIT, expungeDeleted);
        descriptor = LuceneIndexUtils.getDescriptor(conf);
        available = new ArrayBlockingQueue<WriterCallable>(writerThreads);
        for (int i = 0 ; i < writerThreads ; i++) {
            available.add(new WriterCallable(available));
        }
        executor = null;
        log.info(String.format(
                "LuceneManipulator created. bufferSizePayloads is %d"
                + ", bufferSizeMB is %f"
                + ", maxMergeOnConsolidate is %d, writerThreads is %d, "
                + "expungeDeletedOnCommit=%b",
                bufferSizePayloads, buffersizeMB, maxMergeOnConsolidate,
                writerThreads, expungeDeleted));
    }

    public synchronized void open(File indexRoot) throws IOException {
        log.info("Opening Lucene index at '" + indexRoot + "/"
                 + LuceneIndexUtils.LUCENE_FOLDER + "'");
        close(false);
        this.indexRoot = indexRoot;
        File concrete = new File(indexRoot, LuceneIndexUtils.LUCENE_FOLDER);
        if (!concrete.exists()) {
            log.debug("Creating folder '" + concrete + "' for Lucene index");
            if (!concrete.mkdirs()) {
                throw new IOException("Could not create the folder '"
                                      + concrete + "'");
            }
        }
        indexDirectory = new NIOFSDirectory(
                new File(indexRoot, LuceneIndexUtils.LUCENE_FOLDER));
/*        if (IndexReader.indexExists(indexDirectory)) {
            log.debug("Extracting existing RecordIDs from index at '"
                      + indexDirectory.getFile() + "'");
            idMapper = new IDMapper(indexDirectory);
        } else {
            log.debug("No existing index, creating empty idMapper");
            idMapper = new IDMapper();
        }*/
        checkWriter();
    }

    /**
     * Opens a writer at indexDirectory if no writer is currently open.
     * @throws IOException if the writer could not be opened.
     */
    private void checkWriter() throws IOException {
        if (writer != null) {
            log.trace("checkWriter: Writer already opened");
            return;
        }
        try {
            log.debug(String.format(
                    "Checking for index existence at '%s'", indexDirectory));
            if (IndexReader.indexExists(indexDirectory)) {
                log.debug(String.format(
                        "checkWriter: Opening writer for existing index at '%s",
                          indexDirectory.getFile()));
                writer = new IndexWriter(
                        indexDirectory,
                        new StandardAnalyzer(Version.LUCENE_30), false,
                        IndexWriter.MaxFieldLength.UNLIMITED);
                writer.setMergeFactor(80); // TODO: Verify this
                // We want to avoid implicit merging of segments as is messes
                // up document ordering
            } else {
                log.debug("No existing index at '" + indexDirectory.getFile()
                          + "', creating new index");
                writer = new IndexWriter(
                        indexDirectory,
                        new StandardAnalyzer(Version.LUCENE_30), true,
                        IndexWriter.MaxFieldLength.UNLIMITED);
            }

            if (bufferSizePayloads != -1) {
                writer.setMaxBufferedDocs(bufferSizePayloads);
            }
            writer.setRAMBufferSizeMB(buffersizeMB);
            // Old style merging to preserve order of documents
            writer.setMergeScheduler(new SerialMergeScheduler());
            writer.setMergePolicy(new LogByteSizeMergePolicy(writer));
        } catch (CorruptIndexException e) {
            throw new IOException(String.format(
                    "Corrupt index found at '%s'", indexDirectory.getFile()),e);
        } catch (LockObtainFailedException e) {
            throw new IOException(String.format(
                    "Index at '%s' is locked", indexDirectory.getFile()), e);
        } catch (IOException e) {
            throw new IOException(String.format(
                    "Exception opening index '%s'",
                    indexDirectory.getFile()), e);
        }
    }

    /*
     * Opens a reader at indexDirectory if no reader is currently open.
     * @throws IOException if the reader could not be opened.
     * @return a reader connected to the current index.
     *
    private IndexReader openReader() throws IOException {
        if (writer != null) {
            log.warn("checkReader: A Writer is open at '"
                     + indexDirectory.getFile() + "'");
        }
        if (IndexReader.isLocked(indexDirectory)) {
            throw new IOException("The folder '" + indexDirectory.getFile()
                                  + "' is locked. Cannot open a Reader in "
                                  + "order to delete documents");
        }
        try {
            return IndexReader.open(indexDirectory);
        } catch (CorruptIndexException e) {
            throw new IOException("Corrupt index for reader found at '"
                                  + indexDirectory.getFile() + "'",e );
        } catch (IOException e) {
            throw new IOException("Exception opening reader at '"
                                  + indexDirectory.getFile() + "'", e);
        }
    } */

    public synchronized void clear() throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("clear() called");
        log.trace("clear: Calling close() on any existing index at '"
                  + indexDirectory.getFile() + "'");
        close(false);
        log.trace("clear: Removing old folder with Lucene index at '"
                  + indexDirectory.getFile() + "'");
        Files.delete(indexDirectory.getFile());
        log.trace("clear: Opening new Lucene index at '"
                  + indexDirectory.getFile() + "'");
        open(indexRoot);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    @QAInfo(level = QAInfo.Level.PEDANTIC,
            state = QAInfo.State.IN_DEVELOPMENT,
            author = "te")
    public synchronized boolean update(Payload payload) throws IOException {
        if (payload.getData(Payload.LUCENE_DOCUMENT) == null) {
            throw new IllegalArgumentException(
                    "No Document defined in Payload '" + payload + "'");
        }
        String id = payload.getId();
        if (id == null) {
            throw new IllegalArgumentException(String.format(
                    "Could not extract id from %s", payload));
        }
        IndexUtils.assignBasicProperties(payload);
        if (payload.getRecord() == null) {
            log.debug("update: The Payload " + id + " did not have a record, so"
                      + " it will always be processed as a plain addition");
        }
        if (payload.getRecord() != null &&
            (payload.getRecord().isDeleted() ||
             payload.getRecord().isModified())) {
            orderChangedSinceLastCommit();
        }

        dispatchJob(payload);
        return false;
    }

    private void dispatchJob(Payload payload) throws IOException {
        log.trace("Requesting writerCallable");
        WriterCallable writerCallable = null;
        while (writerCallable == null) {
            try {
                writerCallable = available.take();
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for a writerCallable for "
                         + payload + ". Retrying");
            }
        }
        writerCallable.init(writer, payload, descriptor.getIndexAnalyzer());
        log.trace("Submitting writerCallable");

        checkExecutor();
        Future<Long> writerFuture = executor.submit(writerCallable);
        if (writerThreads == 1) {
            waitForJob(writerFuture, payload);
        }
        log.trace("writerCallable submitted");
    }

    private void waitForJob(Future<Long> writerFuture, Payload payload) {
        log.trace("Waiting for job");
        try {
            writerFuture.get();
        } catch (InterruptedException e) {
            log.warn(String.format(
                    "Interrupted while waiting for writer job for %s. "
                    + "Signalling that index documents might be out of order",
                    e));
            orderChangedSinceLastCommit();
        } catch (ExecutionException e) {
            Logging.logProcess(
                    "LuceneManipulator.waitForJob",
                    "Index failed due to exception",
                    Logging.LogLevel.WARN, payload, e);
            log.warn(String.format(
                    "The write job for %s failed with exception", payload), e);
            // Do nothing else as the WriterCallable handles Exceptions itself
        }
        log.trace("Finished waiting for job");
    }


    /**
     * Additions are either plain additions or updates. Updates are handled by
     * deleting any existing Document with the given id and then adding the
     * new document.
     * </p><p>
     * In order to make a proper signalling to any following index manipulators,
     * the position of the deleted document might be determined before deletion.
     * This increases processing time a bit.
     * @param id      the id of the document to add or update.
     * @param payload the payload containing the document to add.
     * @throws IOException if the document could not be added.
     */
    private void updateAddition(String id, Payload payload) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding '" + id + "'");
        checkWriter();
        Document document = (Document)payload.getData(Payload.LUCENE_DOCUMENT);
        // TODO: Add support for Tokenizer and Filters
        try {
            writer.addDocument(document, descriptor.getIndexAnalyzer());
        } catch (IOException e) {
            String message = String.format(
                    "Encountered IOException '%s' during addition of document "
                    + "'%s' to index. Offending payload was %s. The index "
                    + "location was '%s'. JVM shutdown in %d seconds",
                    e.getMessage(), id, indexDirectory.getFile(), payload, 5);
            log.fatal(message, e);
            System.err.println(message);
            e.printStackTrace(System.err);
            new DeferredSystemExit(1, 5);
            throw new IOException(message, e);
        }
        //noinspection DuplicateStringLiteralInspection
        Logging.logProcess("LuceneManipulator", "Added Lucene document",
                           Logging.LogLevel.TRACE, payload);
        if (log.isTraceEnabled()) {
            log.trace("Dumping analyzed fields for " + payload);
            for (Object field: document.getFields()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("Field " + ((Field)field).name() + " has content "
                          + ((Field)field).stringValue());
            }
        }
        // TODO: Verify that docCount is trustable with regard to deletes
        payload.getData().put(
                LuceneIndexUtils.META_ADD_DOCID, writer.maxDoc()-1);
        log.trace("Updating idMapper with id '" + id + "' and pos "
                  + (writer.maxDoc()-1));
//        idMapper.put(id, writer.maxDoc()-1);
    }

    private void updateDeletion(String id, Payload payload) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Deleting '" + id + "'");
        checkWriter();

//        if (idMapper.containsKey(id)) {
//            payload.getData().put(LuceneIndexUtils.META_DELETE_DOCID,
//                                  idMapper.get(id));
            try {
                writer.deleteDocuments(new Term(IndexUtils.RECORD_FIELD, id));
                Logging.logProcess(
                        "LuceneManipulator", "Deleted Lucene document",
                        Logging.LogLevel.TRACE, payload);
                orderChanged = true;
        } catch (IOException e) {
                String message = String.format(
                     "Encountered IOException '%s' during deletion of document "
                     + "'%s' to index. Offending payload was %s. The index "
                     + "location was '%s'. JVM shutdown in %d seconds",
                     e.getMessage(), id, indexDirectory.getFile(), payload, 5);
                log.fatal(message, e);
                System.err.println(message);
                e.printStackTrace(System.err);
                new DeferredSystemExit(1, 5);
                throw new IOException(message, e);
        }
//        } else {
//            Logging.logProcess(
//                    "LuceneManipulator", "Delete requested, but the Record was "
//                                         + "not present in the index",
//                    Logging.LogLevel.DEBUG, payload);
//        }
    }

    public synchronized void commit() throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("commit() called for '" + indexRoot + "'");
        long startTime = System.currentTimeMillis();
        if (writer == null) {
            log.trace("commit: No writer, commit finished in "
                  + (System.currentTimeMillis() - startTime) + " ms");
            return;
        }

        log.debug("commit: Flushing index at '"
                  + indexRoot + "' with docCount " + writer.maxDoc());
        flushPending();
        orderChanged = false;
        if (expungeDeleted) {
            log.trace("commit: Expunging deleted documents from '"
                      + indexRoot + "'");
            writer.expungeDeletes();
        } else {
            log.trace("commit: Skipping the expunge deleted documents step");
        }
        log.trace("commit: Lucene committing index at '" + indexRoot + "'");
        writer.commit();
        log.debug(String.format(
                "Commit finished for '%s' in %s ms with docCount %d and "
                + "expungeDeleted=%b",
                indexRoot , (System.currentTimeMillis() - startTime),
                writer.maxDoc(), expungeDeleted));
    }

    /* Note: Sets executor = null */
    private void flushPending() {
        if (executor == null) {
            log.debug("Nothing to flush");
            return;
        }

        log.debug("Waiting for " + available.size()
                  + " pending jobs to finish");
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.warn("flushPending(): Interrupted while waiting for pending "
                     + "jobs to finish. Some Payloads might not be indexed");
        }
        executor = null;
    }

    /* Create a new executor unless we already have one */
    private void checkExecutor() {
        if (executor == null) {
            log.debug("Creating new thread pool");
            executor = Executors.newFixedThreadPool(writerThreads);
        }
    }

    public synchronized void consolidate() throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("consolidate() called");
        log.trace("consolidate(): Calling commit()");
        long startTime = System.currentTimeMillis();
        commit();
        checkWriter();

        /**
        log.trace("consolidate(): Removing deletions");
        writer.expungeDeletes(true);*/
        log.debug(String.format(
                "Optimizing index at %s to a maximum of %d segments. "
                + "This might take a while",
                indexDirectory.getFile(), maxMergeOnConsolidate));
        writer.optimize(maxMergeOnConsolidate, true);
        log.trace("Closing writer");
        closeWriter(); // Is this still necessary?
        log.debug("Consolidate finished in "
                  + (System.currentTimeMillis()- startTime) + " ms");
    }

    public synchronized void close() throws IOException {
        close(true);
    }

    private void close(boolean flush) throws IOException {
        if (indexDirectory == null) { // Never opened
            return;
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("close(" + flush + ") called for '"
                  + indexDirectory.getFile() + "'");
        if (flush) {
            commit();
        } else {
            flushPending();
        }
        if (writer != null) {
            log.debug("Closing writer for '" + indexDirectory.getFile() + "'");
            try {
                closeWriter();
            } finally {
                try {
                    if (IndexWriter.isLocked(indexDirectory)) {
                        log.error("Lucene lock at '" + indexDirectory.getFile()
                                  + "' after close. Attempting removal");
                        IndexWriter.unlock(indexDirectory);
                    }
                } catch (IOException e) {
                    log.error("Could not remove lock at '"
                              + indexDirectory.getFile() + "'");
                }
            }
            //noinspection AssignmentToNull
            writer = null;
        }

        indexDirectory.close();
        //noinspection AssignmentToNull
        indexDirectory = null;

//        idMapper.clear();
        //noinspection AssignmentToNull
//        idMapper = null;
    }

    private void closeWriter() throws IOException {
        if (indexDirectory == null) {
            log.trace("closeWriter: Index never opened");
            return;
        }
        if (writer == null) {
            log.trace("closeWriter: No writer present");
            return;
        }
        log.debug("closeWriter: Closing '" + indexDirectory.getFile() + "'");
        try {
            writer.close();
            //noinspection AssignmentToNull
            writer = null;
        } catch (CorruptIndexException e) {
            throw new IOException("Corrupt index in writer for '"
                                  + indexDirectory.getFile() + "'", e);
        } catch (IOException e) {
            throw new IOException("Exception closing writer for '"
                                  + indexDirectory.getFile() + "'", e);
        }
    }

    @Override
    public void orderChangedSinceLastCommit() {
        orderChanged = true;
    }

    /**
     * The order The FacetManipulator never sets orderChanged to true by itself. It only
     * reacts on external messages.
     * @return true if the order has been marked as changed since last commit
     *              or consolidate.
     */
    @Override
    public boolean isOrderChangedSinceLastCommit() {
        return orderChanged;
    }
}

