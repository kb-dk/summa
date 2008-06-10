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
package dk.statsbiblioteket.summa.index;

import java.io.IOException;
import java.io.File;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;

/**
 * Handles iterative updates of a Lucene index. A Record that is an update of an
 * already ingested Record is treaded as a deletion followed by an addition.
 * </p><p>
 * The manipulator maintains a cache of RecordIDs in the given index, mapped to
 * LuceneIDs. This consumes memory linear to the number of Documents.
 * </p><p>
 * The RecordID is extracted from incoming Payloads as the id from the embedded
 * Records. If no Record exists, the id is extracted from the embedded
 * Document as field RecordID.
 */
// TODO: Verify that the order of documents is strict under all operations
// TODO: Use indexable instead of deleted
// TODO: Add maximum number of segments property for consolidate
// TODO: Add memory based flushing policy
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
            "summa.index.lucene.BUFFER_SIZE_PAYLOADS";
    public static final int DEFAULT_BUFFER_SIZE_PAYLOADS = -1;

    /** The index descriptor, used for providing Analyzers et al. */
    private LuceneIndexDescriptor descriptor;
    /** The general index folder, which contains the concrete index-parts. */
    private File indexRoot;
    /** The directory with the Lucene index. This will be a sub-folder to
     * indexRoot. */
    private FSDirectory indexDirectory;
    /** The writer is used for additions */
    private IndexWriter writer;
    /** The reader is used for deletions */
    private IndexReader reader;
    /**
     * An ordered collections of the deletions that should be performed upon
     * commit. The deletions takes place before additions. The map goes from
     * RecordIDs to Payloads.
     */
    private LinkedHashMap<String, Payload> deletions =
            new LinkedHashMap<String, Payload>(100);
    /**
     * An ordered collection of the additions that should be performed upon
     * commit. The additions takes place after deletions. The map goes from
     * RecordIDs to Payloads.
     */
    private LinkedHashMap<String, Payload> additions =
            new LinkedHashMap<String, Payload>(100);

    /**
     * Keeps track of RecordIDs. The idMapper is used and updated by
     * {@link #update}, {@link #flushDeletions} and {@link #flushAdditions}. 
     */
    private IDMapper idMapper;

    @SuppressWarnings({"UnusedDeclaration", "UnusedDeclaration"})
    private int bufferSizePayloads = DEFAULT_BUFFER_SIZE_PAYLOADS;

    public LuceneManipulator(Configuration conf) {
        bufferSizePayloads = conf.getInt(CONF_BUFFER_SIZE_PAYLOADS,
                                         DEFAULT_BUFFER_SIZE_PAYLOADS);
        descriptor = LuceneIndexUtils.getDescriptor(conf);
        log.debug("LuceneManipulator created. bufferSizePayloads is "
                  + bufferSizePayloads);
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
        indexDirectory = FSDirectory.getDirectory(new File(indexRoot,
                                                           LuceneIndexUtils.LUCENE_FOLDER));
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

    /**
     * Opens a reader at indexDirectory if no reader is currently open.
     * @throws IOException if the reader could not be opened.
     */
    private void openReader() throws IOException {
        if (reader != null) {
            log.debug("checkReader: Reader already opened at '"
                     + indexDirectory.getFile() + "'");
            return;
        }
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
            reader = IndexReader.open(indexDirectory);
        } catch (CorruptIndexException e) {
            throw new IOException("Corrupt index for reader found at '"
                                  + indexDirectory.getFile() + "'",e );
        } catch (IOException e) {
            throw new IOException("Exception opening reader at '"
                                  + indexDirectory.getFile() + "'", e);
        }
    }

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

    /*
      * new deletion & existing deletion & no existing addition =>
      *   remove(existing deletion), add(new deletion)
      * new deletion & existing deletion & existing addition =>
      *   remove(existing deletion), remove(existing addition), add(new deletion)
      * new deletion & no existing deletion & no existing addition =>
      *   add(new deletion)
      * new deletion & no existing deletion & existing addition =>
      *   remove(existing addition), add(new deletion)
      *
      * new addition & existing deletion & no existing addition =>
      *   add(new addition)
      * new addition & existing deletion & existing addition =>
      *   remove(existing addition), add(new addition)
      * new addition & no existing deletion & no existing addition =>
      *   add(new addition)
      * new addition & no existing deletion & existing addition =>
      *   remove(existing addition), add(new deletion)
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    @QAInfo(level = QAInfo.Level.PEDANTIC,
            state = QAInfo.State.IN_DEVELOPMENT,
            author = "te")
    // TODO: Consider if adds can be done immediately as dels does not shift ids
    public synchronized boolean update(Payload payload) throws IOException {
        if (payload.getData(Payload.LUCENE_DOCUMENT) == null) {
            throw new IllegalArgumentException("No Document defined in"
                                               + " Payload '" + payload + "'");
        }
        String id = payload.getId();
        if (id == null) {
            throw new IllegalArgumentException("Could not extract id from "
                                               + "Payload '" + payload + "'");
        }
        ensureStoredID(id, payload);
        boolean deleted =
                payload.getRecord() != null && payload.getRecord().isDeleted();
        if (deleted) {
            updateDeletion(id, payload);
        } else {
            updateAddition(id, payload);
        }
        return deletions.size() + additions.size() >= bufferSizePayloads;
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private void updateAddition(String id, Payload payload) throws IOException {
        StringWriter debug = new StringWriter(300);
        debug.write("new addition(" + id + ")");
        if (idMapper.containsKey(id)) {
            log.debug(debug.toString() + " already present in index, so delete "
                      + "is called before addition");
            updateDeletion(id, payload);
            debug.write(" was present in index (a delete has been performed)");
        }

        if (deletions.size() == 0) {
            if (additions.size() > 0) {
                log.error("update(" + payload + ") has 0 pending deletions "
                          + "but " + additions.size() + " pending additions"
                          + " (this should be 0)");
            }
            debug.write(" and no pending deletions");
            debug.write(" => ingesting and updating idMap");
            log.debug(debug.toString());
            checkWriter();
            Document document =
                    (Document)payload.getData(Payload.LUCENE_DOCUMENT);
            // TODO: Add support for Tokenizer and Filters
            writer.addDocument(document, descriptor.getIndexAnalyzer());
            idMapper.put(id, writer.docCount());
        } else {
            if (deletions.containsKey(id)) {
                debug.write(" & existing deletion");
                if (additions.containsKey(id)) {
                    debug.write(" & existing addition =>");
                    debug.write(" remove(existing addition),");
                } else {
                    debug.write(" & no existing addition =>");
                }
            } else {
                debug.write(" & no existing deletion");
                if (additions.containsKey(id)) {
                    debug.write(" & existing addition =>");
                    debug.write(" remove(existing addition),");
                } else {
                    debug.write(" & no existing addition =>");
                    // Ingesting here messes up the order, so we queue instead
                }
            }
            debug.write(" add(new addition)");
            log.debug(debug.toString());
            additions.put(id, payload); // Replaces any existing addition
        }
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    // TODO: Optimize the case where delete has no effect
    private void updateDeletion(String id, Payload payload) {
        StringWriter debug = new StringWriter(300);
        debug.write("new deletion(" + id + ")");
        if (deletions.containsKey(id)) {
            debug.write(" & existing deletion");
            if (additions.containsKey(id)) {
                debug.write(" & existing addition =>");
                debug.write(" remove(existing deletion),");
                debug.write(" remove(existing addition),");
                additions.remove(id);
            } else {
                debug.write(" & no existing addition =>");
                debug.write(" remove(existing deletion),");
            }
        } else {
            debug.write(" & no existing deletion");
            if (additions.containsKey(id)) {
                debug.write(" & existing addition =>");
                debug.write(" remove(existing addition),");
                additions.remove(id);
            } else {
                debug.write(" & no existing addition =>");
            }
        }
        debug.write(" add(new deletion)");
        log.debug(debug.toString());
        deletions.put(id, payload); // Replaces any existing deletion
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
        log.trace("commit() called");
        long startTime = System.currentTimeMillis();
        closeWriter();
        flushDeletions();
        flushAdditions();
        log.debug("commit: Flushing index");
        closeWriter();
        log.trace("Commit finished in "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    /* Note: The flush does not call commit */
    private void flushAdditions() throws IOException {
        if (additions.size() == 0) {
            log.debug("No additions to flush");
            return;
        }
        log.debug("Flushing  " + additions.size() + " additions");
        checkWriter();
        for (Map.Entry<String, Payload> entry: additions.entrySet()) {
            Payload addition = entry.getValue();
            //noinspection DuplicateStringLiteralInspection
            log.debug("Adding '" + addition.getId() + "' to index");
            idMapper.put(addition.getId(), writer.docCount());
            Document document =
                    (Document)addition.getData(Payload.LUCENE_DOCUMENT);
            writer.addDocument(document);
        }
        additions.clear();
        log.trace("Finished addition flush");
    }

    private void flushDeletions() throws IOException {
        if (deletions.size() == 0) {
            log.debug("No deletions to flush");
            return;
        }
        log.trace("Calling close(true)");
        closeWriter();
        log.debug("Opening reader ");
        openReader();
        //noinspection DuplicateStringLiteralInspection
        log.debug("Flushing " + deletions.size() + " deletions");
        for (Map.Entry<String, Payload> entry: deletions.entrySet()) {
            Payload deletion = entry.getValue();
            int delCount;
            if ((delCount =
                    reader.deleteDocuments(new Term(IndexUtils.RECORD_FIELD,
                                                    deletion.getId()))) != 1) {
                if (delCount == 0) {
                    log.warn("flushDeletions: Deleted 0 documents for id '"
                             + deletion.getId() + "'");
                } else {
                    log.warn("flushDeletions: Deleted " + delCount
                             + " documents for id '" + deletion.getId()
                             + "'. Expected 1");
                }
            }
            idMapper.remove(deletion.getId());
        }
        reader.close();
        deletions.clear();
        log.trace("flushDeletions() finished");
    }

    public synchronized void consolidate() throws IOException {
        log.trace("consolidate() called. Calling commit()");
        long startTime = System.currentTimeMillis();
        commit();
        checkWriter();
        writer.optimize();
        writer.flush();
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
        log.trace("close(flush) called for '" + indexDirectory.getFile() + "'");
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
        if (!flush && (additions.size() > 0 || deletions.size() > 0)) {
            log.warn("Closing without flush: "
                     + (additions.size() + deletions.size())
                     + " cached updates are lost");
        }
        additions.clear();
        deletions.clear();

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

