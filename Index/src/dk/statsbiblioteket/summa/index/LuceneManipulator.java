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
import java.util.LinkedHashSet;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * Handles iterative updates of a Lucene index. A Record that is an update of an
 * already ingested Record is treaded as a deletion followed by an addition.
 * </p><p>
 * The manipulator maintains a cache of RecordIDs in the given index, mapped to
 * LuceneIDs. This consumes memory linear to the number of Documents.
 */
// TODO: Verify that the order of documents is strict under all operations
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

    /**
     * The subfolder in the index root containing the lucene index.
     * This will be appended to {@link #indexRoot}.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String LUCENE_FOLDER = "lucene";

    /* The location of the index */
    private File indexRoot;
    /* The Lucene Directory with the index */
    private FSDirectory indexDirectory;
    /* The writer is used for additions */
    private IndexWriter writer;
    /* The reader is used for deletions */
    private IndexReader reader;
    /**
     * An ordered collections of the deletions that should be performed upon
     * commit. The deletions takes place before additions.
     */
    private LinkedHashSet<Payload> deletions = new LinkedHashSet<Payload>(100);
    /**
     * An ordered collection of the additions that should be performed upon
     * commit. The additions takes place after deletions.
     */
    private LinkedHashSet<Payload> additions = new LinkedHashSet<Payload>(100);

    /**
     * Keeps track of RecordIDs. The idMapper is used and updated by
     * {@link #update}, {@link #flushDeletions} and {@link #flushAdditions}. 
     */
    private IDMapper idMapper;

    @SuppressWarnings({"UnusedDeclaration", "UnusedDeclaration"})
    private int bufferSizePayloads = DEFAULT_BUFFER_SIZE_PAYLOADS;

    /*

    No autocommit,
    huge buffer (option) - what to do on overflow?
    no automerge

     */

    public LuceneManipulator(Configuration conf) {
        bufferSizePayloads = conf.getInt(CONF_BUFFER_SIZE_PAYLOADS,
                                         DEFAULT_BUFFER_SIZE_PAYLOADS);
        log.debug("LuceneManipulator created. bufferSizePayloads is "
                  + bufferSizePayloads);
    }

    public synchronized void open(File indexRoot) throws IOException {
        log.info("Opening Lucene index at '" + indexRoot + "/"
                 + LUCENE_FOLDER + "'");
        close(false);
        this.indexRoot = indexRoot;
        File concrete = new File(indexRoot, LUCENE_FOLDER);
        if (!concrete.exists()) {
            log.debug("Creating folder '" + concrete + "' for Lucene index");
            if (!concrete.mkdirs()) {
                throw new IOException("Could not create the folder '"
                                      + concrete + "'");
            }
        }
        indexDirectory = FSDirectory.getDirectory(new File(indexRoot,
                                                           LUCENE_FOLDER));
        if (IndexReader.indexExists(indexDirectory)) {
            log.debug("Extracting existing RecordIDs from index at '"
                      + indexDirectory.getFile() + "'");
            idMapper = new IDMapper(indexDirectory);
        }
        checkWriter();
    }

    /**
     * Opens a writer at indexDirectory if no writer is currently open.
     * @throws IOException if the writer could not be opened.
     */
    private void checkWriter() throws IOException {
        if (writer != null) {
            log.debug("checkWriter: Writer already opened");
            return;
        }
        try {
            writer = new IndexWriter(indexDirectory, false,
                                     new StandardAnalyzer(), false);
            writer.setMaxFieldLength(Integer.MAX_VALUE-1);
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

    public synchronized boolean update(Payload payload) throws IOException {
        // TODO: Implement this
        // If no deletions: add to index and update idMapper
        return deletions.size() + additions.size() >= bufferSizePayloads;
    }

    public synchronized void commit() throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("commit() called");
        long startTime = System.currentTimeMillis();
        close(true);
        flushDeletions();
        flushAdditions();
        log.trace("Commit finished in "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    private void flushAdditions() throws IOException {
        if (additions.size() > 0) {
            log.debug("Flushing ' " + additions + " additions");
        }
        // TODO: Implement this
    }

    private void flushDeletions() throws IOException {
        if (deletions.size() == 0) {
            log.debug("No deletions to flush");
            return;
        }
        log.trace("Calling close(true)");
        close(true);
        log.debug("Opening reader ");
        openReader();
        log.debug("Flushing '" + deletions.size() + " deletions");

        // TODO: Remove and remove from idMapper
    }

    public synchronized void consolidate() throws IOException {
        log.trace("consolidate() called. Calling commit()");
        commit();
        // TODO: Implement this
    }

    public synchronized void close() throws IOException {
        close(true);
    }

    private void close(boolean flush) throws IOException {
        log.trace("close(flush) called for '" + indexDirectory.getFile() + "'");
        if (flush) {
            commit();
        }
        if (writer != null) {
            log.debug("Closing writer for '" + indexDirectory.getFile() + "'");
            try {
                writer.close();
            } catch (CorruptIndexException e) {
                throw new IOException("Corrupt index in writer for '"
                                      + indexDirectory.getFile() + "'", e);
            } catch (IOException e) {
                throw new IOException("Exception closing writer for '"
                                      + indexDirectory.getFile() + "'", e);
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

}
