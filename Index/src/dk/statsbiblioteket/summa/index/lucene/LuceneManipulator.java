/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.index.lucene;

import java.io.IOException;
import java.io.File;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;

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
// TODO: Add memory based flushing policy
// TODO: Verify that adds + deletes inside the same commit works as expected 
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneManipulator implements IndexManipulator {
    private static Log log = LogFactory.getLog(LuceneManipulator.class);

    /**
     * The amount of Payloads to buffer before a flush is requested.
     * -1 means no flushes are requested.
     * </p><p>
     * This property is optional. Default is -1.
     */
    public static final String CONF_BUFFER_SIZE_PAYLOADS =
            "summa.index.lucene.buffersizepayloads";
    public static final int DEFAULT_BUFFER_SIZE_PAYLOADS = -1;

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

    /** The index descriptor, used for providing Analyzers et al. */
    private LuceneIndexDescriptor descriptor;
    /** The general index folder, which contains the concrete index-parts. */
    private File indexRoot;
    /** The directory with the Lucene index. This will be a sub-folder to
     * indexRoot. */
    private FSDirectory indexDirectory;
    /** The connection to the Lucene index */
    private IndexWriter writer;

    /**
     * Keeps track of RecordIDs. The idMapper is used and updated by
     * {@link #updateAddition}.
     */
    private IDMapper idMapper;

    private int bufferSizePayloads = DEFAULT_BUFFER_SIZE_PAYLOADS;
    private int maxMergeOnConsolidate = DEFAULT_MAX_SEGMENTS_ON_CONSOLIDATE;

    public LuceneManipulator(Configuration conf) {
        bufferSizePayloads = conf.getInt(CONF_BUFFER_SIZE_PAYLOADS,
                                         bufferSizePayloads);
        maxMergeOnConsolidate = conf.getInt(CONF_MAX_SEGMENTS_ON_CONSOLIDATE,
                                            maxMergeOnConsolidate);
        descriptor = LuceneIndexUtils.getDescriptor(conf);
        log.debug("LuceneManipulator created. bufferSizePayloads is "
                  + bufferSizePayloads + ", maxMergeOnConsolidate is "
                  + maxMergeOnConsolidate);
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
        indexDirectory = FSDirectory.getDirectory(
                new File(indexRoot, LuceneIndexUtils.LUCENE_FOLDER));
        if (IndexReader.indexExists(indexDirectory)) {
            log.debug("Extracting existing RecordIDs from index at '"
                      + indexDirectory.getFile() + "'");
            idMapper = new IDMapper(indexDirectory);
        } else {
            log.debug("No existing index, creating empty idMapper");
            idMapper = new IDMapper();
        }
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
            if (IndexReader.indexExists(indexDirectory)) {
                log.debug("checkWriter: Opening writer for existing index at '"
                          + indexDirectory.getFile() + "'");
                writer = new IndexWriter(indexDirectory, false,
                                         new StandardAnalyzer(), false);
                writer.setMergeFactor(100); // TODO: Verify this
                // We want to avoid implicit merging of segments as is messes
                // up document ordering
            } else {
                log.debug("No existing index at '" + indexDirectory.getFile()
                          + "', creating new index");
                writer = new IndexWriter(indexDirectory, false,
                                         new StandardAnalyzer(), true);
            }
            writer.setMaxFieldLength(Integer.MAX_VALUE-1);

            // This changes from memory-based check and disables auto flush
            writer.setMaxBufferedDocs(Integer.MAX_VALUE); // Dangerous...
            // Old style merging to preserve order of documents
            writer.setMergeScheduler(new SerialMergeScheduler());
            writer.setMergePolicy(new LogDocMergePolicy());
            // TODO: Set conservative merges et al
            // TODO: Infer analyzer
        } catch (CorruptIndexException e) {
            throw new IOException("Corrupt index found at '"
                                  + indexDirectory.getFile() + "'",e );
        } catch (LockObtainFailedException e) {
            throw new IOException("Index at '" + indexDirectory.getFile()
                                  + "' is locked");
        } catch (IOException e) {
            throw new IOException("Exception opening writer at '"
                                  + indexDirectory.getFile() + "'", e);
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
            throw new IllegalArgumentException("No Document defined in"
                                               + " Payload '" + payload + "'");
        }
        String id = payload.getId();
        if (id == null) {
            throw new IllegalArgumentException(String.format(
                    "Could not extract id from %s", payload));
        }
        ensureStoredID(id, payload);
        if (payload.getRecord() == null) {
            log.debug("update: The Payload " + id + " did not have a record, so"
                      + " it will always be processed as a plain addition");
        }
        boolean deleted =
                payload.getRecord() != null && payload.getRecord().isDeleted();
        boolean updated =
                payload.getRecord() != null && payload.getRecord().isModified();
        if (deleted) { // Plain delete
            updateDeletion(id, payload);
        } else { // Addition or update
            if (updated) { // Update, so we delete first
                updateDeletion(id, payload);
            }
            updateAddition(id, payload);
        }
        return false; // TODO: Return true if payload counter gets too high
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
        log.debug("Adding '" + id + "'");
        checkWriter();
        Document document = (Document)payload.getData(Payload.LUCENE_DOCUMENT);
        // TODO: Add support for Tokenizer and Filters
        writer.addDocument(document, descriptor.getIndexAnalyzer());
        // TODO: Verify that docCount is trustable with regard to deletes
        payload.getData().put(LuceneIndexUtils.META_ADD_DOCID,
                              writer.docCount()-1);
        log.trace("Updating idMapper with id '" + id + "' and pos "
                  + (writer.docCount()-1));
        idMapper.put(id, writer.docCount()-1);
    }

    private void updateDeletion(String id, Payload payload) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Deleting '" + id + "'");
        checkWriter();
        if (idMapper.containsKey(id)) {
            payload.getData().put(LuceneIndexUtils.META_DELETE_DOCID,
                                  idMapper.get(id));
            writer.flush();
            writer.deleteDocuments(new Term(IndexUtils.RECORD_FIELD, id));
            // TODO: Consider if we can delay flush
            // The problem is add(a), delete(a), add(a). Without flushing we
            // don't know if a will be present in the index or not.
            // Another problem is add(a), delete(a), add(a), delete(a) as the
            // deltions are stored in a HashMap
            writer.flush();
        } else {
            log.info("Delete requested for " + payload + ", but it was not "
                     + "present in the index");
        }
    }

    /**
     * Ensures that the Document in payload has a RecordID-field which contains
     * id as term.
     * @param id      the RecordID for the Document.
     * @param payload the container containing the Document.
     */
    private void ensureStoredID(String id, Payload payload) {
        Document document =
                (Document)payload.getData(Payload.LUCENE_DOCUMENT);
        IndexUtils.assignID(id, document);
    }

    public synchronized void commit() throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("commit() called for '" + indexRoot + "'");
        long startTime = System.currentTimeMillis();
        closeWriter();
        if (writer == null) {
            log.trace("commit: No writer, commit finished in "
                  + (System.currentTimeMillis() - startTime) + " ms");
            return;
        }
        log.debug("commit: Flushing index at '" + indexRoot + "' with docCount "
                  + writer.docCount());
        closeWriter();
        log.trace("Commit finished for '" + indexRoot + "' in "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    public synchronized void consolidate() throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("consolidate() called");
        log.trace("consolidate(): Calling commit()");
        long startTime = System.currentTimeMillis();
        commit();
        checkWriter();

        // TODO: When we change to Lucene 2.4, activate expungeDeletes
        /**
        log.trace("consolidate(): Removing deletions");
        writer.expungeDeletes(true);*/
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
        }
        if (writer != null) {
            log.debug("Closing writer for '" + indexDirectory.getFile() + "'");
            try {
                closeWriter();
            } finally {
                try {
                    if (IndexReader.isLocked(indexDirectory)) {
                        log.error("Lucene lock at '" + indexDirectory.getFile()
                                  + "' after close. Attempting removal");
                        IndexReader.unlock(indexDirectory);
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

        idMapper.clear();
        //noinspection AssignmentToNull
        idMapper = null;
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

}
