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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

/**
 * Reusable callable for adding, updating or deleting  the document from a
 * Payload to Lucene. The Payload is expected to contain a Document.
 * Returns the time in ns it took to perform the modification.
 * </p><p>
 * {@link #init} must be called prior to use.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class WriterCallable implements Callable<Long> {
    private static Log log = LogFactory.getLog(WriterCallable.class);

    private BlockingQueue<WriterCallable> callbackQueue;

    private IndexWriter writer = null;
    private Payload payload = null;
    private Analyzer analyzer= null;

    /**
     * @param callbackQueue when call is finished, the callable will be added
     *                      to the callbackQueue.
     */
    public WriterCallable(BlockingQueue<WriterCallable> callbackQueue) {
        this.callbackQueue = callbackQueue;
    }

    public void init(IndexWriter writer, Payload doc, Analyzer analyzer) {
        if (log.isTraceEnabled()) {
            log.trace("Creating WriterCallable for " + payload);
        }
        this.writer = writer;
        this.payload = doc;
        this.analyzer = analyzer;
    }

    /**
     * Add, update or delete Document to the Lucene index, using the provided
     * IndexWriter.
     * @return the time in ns that it took to handle the Document.
     * @throws Exception if the Document could not be handled.
     */
    @Override
    public Long call() throws Exception {
        try {
            Long exeTime = protectedCall();
            if (log.isTraceEnabled()) {
                log.trace("Finished writing of " + payload + " in "
                          + (exeTime / 1000000.0) + "ms");
            }
        } catch (Exception e) {
            log.warn("Got exception while writing " + payload, e);
            throw e; // Rethrow to keep the concrete Exception
            // TODO: Consider signalling orderChangedSinceLastCommit
        } catch (Error e) {
            String message = String.format(
                    "Encountered Error '%s' during protectedCall of %s "
                    + "to index. The index "
                    + "location was '%s'. JVM shutdown in %d seconds",
                    e.getMessage(), payload, writer.getDirectory(), 5);
            log.fatal(message, e);
            System.err.println(message);
            e.printStackTrace(System.err);
            new DeferredSystemExit(1, 5);
            throw new IOException(message, e);
        } finally {
            log.trace("Returning this to callback queue");
            writer = null;
            payload = null;
            analyzer = null;
            callbackQueue.put(this);
        }
        return 0L; // We're in an exception-state here
    }

    private long protectedCall() throws Exception {
        long startTime = System.nanoTime();
        if (payload.getRecord() == null) {
            log.debug("update: " + payload + " did not have a record, so"
                      + " it will always be processed as a plain addition");
        }
        boolean deleted =
                payload.getRecord() != null && payload.getRecord().isDeleted();
        boolean updated =
                payload.getRecord() != null && payload.getRecord().isModified();
        if (deleted) { // Plain delete
            updateDeletion();
        } else { // Addition or update
            if (updated) { // Update, so we delete first
                updateDeletion();
            }
            updateAddition();
            if (log.isTraceEnabled()) {
                log.trace("Dumping analyzed fields for " + payload);
                Document document =
                        (Document)payload.getData(Payload.LUCENE_DOCUMENT);
                for (Object field: document.getFields()) {
                    log.trace(
                            "Field " + ((Field)field).name() + " has content '"
                            + ((Field)field).stringValue() + "'");
                }
            }
        }

        return System.nanoTime() - startTime;
    }

    private void updateAddition() throws IOException {
        Document document = (Document)payload.getData(Payload.LUCENE_DOCUMENT);
        try {
            writer.addDocument(document, analyzer);
        } catch (IOException e) {
            die(e, "addition");
        }
        payload.getData().put(
                LuceneIndexUtils.META_ADD_DOCID, writer.maxDoc()-1);

        Logging.logProcess("LuceneManipulator", "Added Lucene document",
                           Logging.LogLevel.TRACE, payload);
    }

    private void updateDeletion() throws IOException {
        try {
            writer.deleteDocuments(new Term(
                    IndexUtils.RECORD_FIELD, payload.getId()));
            Logging.logProcess(
                    "LuceneManipulator", "Deleted Lucene document",
                    Logging.LogLevel.TRACE, payload);
        } catch (IOException e) {
            die(e, "deletion");
        }
        // It would be nice to determine the docID of the Document here
        Logging.logProcess("LuceneManipulator", "Added Lucene document",
                           Logging.LogLevel.TRACE, payload);
    }

    private void die(IOException e, String action) throws IOException {
        String message = String.format(
                "Encountered IOException '%s' during %s of document "
                + "to index. Offending payload was %s. The index "
                + "location was '%s'. JVM shutdown in %d seconds",
                e.getMessage(), action, payload, writer.getDirectory(), 5);
        log.fatal(message, e);
        System.err.println(message);
        e.printStackTrace(System.err);
        new DeferredSystemExit(1, 5);
        throw new IOException(message, e);
    }
}

