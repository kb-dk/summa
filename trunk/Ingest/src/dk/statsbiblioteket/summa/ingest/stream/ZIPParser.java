/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParser;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Handles ZIP-streams by uncompressing and splitting the content into separate
 * streams. the ZIPHandler is normally used by a {@link StreamController}.
 * </p><p>
 * Note: As streams does not allow for random access, calls to {@link #next}
 *       will block until the previous stream has been closed.
 * // TODO: Consider adding a timeout or similar to guard against streams not
 *          being closed.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ZIPParser extends ThreadedStreamParser {
    private static Log log = LogFactory.getLog(ZIPParser.class);

    /**
     * The file pattern inside the ZIP-stream to match.
     * </p><p>
     * This property is optional. Default is ".*\.xml".
     */
    public static final String CONF_FILE_PATTERN =
            "summa.ingest.unzipfilter.filepattern";
    public static final String DEFAULT_FILE_PATTERN = ".*\\.xml";

    /**
     * The maximum amount of milliseconds to wait for the processing of a
     * ZIP-stream entry. If the processing-time exceeds this amount, the parser
     * will perform a close on the current entry and skip to the next.
     * </p><p>
     * Optional. Default is Integer.MAX_VALUE.
     */
    public static final String CONF_PROCESSING_TIMEOUT =
            "summa.ingest.unzipfilter.processingtimeout";
    public static final int DEFAULT_PROCESSING_TIMEOUT = Integer.MAX_VALUE;

    private Pattern filePattern;
    private int processingTimeout = DEFAULT_PROCESSING_TIMEOUT;

    public ZIPParser(Configuration conf) {
        super(conf);
        filePattern = Pattern.compile(conf.getString(
                CONF_FILE_PATTERN, DEFAULT_FILE_PATTERN));
        processingTimeout = conf.getInt(
                CONF_PROCESSING_TIMEOUT, processingTimeout);
        log.info(String.format(
                "Created ZIPParser with file pattern '%s'",
                conf.getString(CONF_FILE_PATTERN, DEFAULT_FILE_PATTERN)));
    }

    @Override
    protected void protectedRun() throws Exception {
        log.debug("Opening ZIP-Stream from " + sourcePayload);
        ZipInputStream zip = new ZipInputStream(
                new BufferedInputStream(sourcePayload.getStream()));
        ZipEntry entry;
        int matching = 0;
        while ((entry = zip.getNextEntry()) != null && running) {
            log.trace("Got entry, checking for compliance");
            if (!filePattern.matcher(entry.getName()).matches()) {
                log.trace(entry.getName() + " not matched. Skipping");
                continue;
            }
            log.trace("Entry " + entry.getName() + " matched. Adding to queue "
                      + "and waiting for close");

            /* Prepare a stream that we'll fill asynchronously while the
             * payload consumer reads from it */
            matching++;
            QueuedInputStream payloadStream = new QueuedInputStream();
            Payload payload = new Payload(payloadStream);
            payload.getData().put(
                    Payload.ORIGIN,
                    sourcePayload.getData(Payload.ORIGIN) + "!"
                    + entry.getName());
            addToQueue(payload);

            /* Fill the stream we gave to the payload. There is no race here
             * because all reads on the payloadStream are blocking until
             * we start filling the queue */
            byte[] buf = new byte[2048];
            int numRead;
            while ((numRead = zip.read(buf)) != -1) {
                for (int i = 0; i < numRead; i++) {
                    payloadStream.enqueue(buf[i]);
                }
            }
            payloadStream.enqueueEOF();
        }
        log.debug("Ending processing of " + sourcePayload + " with running="
                  + running);
        zip.close();
        // TODO: Check if Payload should be closed here
        log.debug(String.format("Processed %d ZIP entries from %s",
                                matching, sourcePayload));
    }

    /**
     * And input stream that reads data from an underlying blocking queue.
     * This facilitates decoupling of IO operations from producer
     * and consumer threads.
     *
     * FIXME: This implementation uses byte autoboxing madness.
     *        An approach based on a structure similar to SBUtil's
     *        CircularCharBuffer, but for bytes instead of chars, would
     *        likely perform a lot better...
     */
    private static class QueuedInputStream extends InputStream {

        BlockingQueue<Object> readQueue;
        private boolean eof;
        private boolean closed;
        private Object eofMarker;

        public QueuedInputStream() {
            readQueue = new ArrayBlockingQueue<Object>(2048);
            eof = false;
            closed = false;
            eofMarker = new Object();
        }

        @Override
        public int read() throws IOException {
            if (eof) {
                return -1;
            }
            checkClosed();

            while (true) {
                try {
                    Object b = readQueue.take();
                    if (b == eofMarker) {
                        eof = true;
                        return -1;
                    }
                    return (Byte)b;
                } catch (InterruptedException e) {
                    // Ignored, we retry
                }
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            if (eof) {
                return -1;
            }
            checkClosed();

            int count = 0;
            while (count < len) {
                int b = read();
                if (b == -1) {
                    return count;
                }

                buf[off + count] = (byte)b;
                count++;
            }

            return count;
        }

        @Override
        public long skip(long n) throws IOException {
            checkClosed();
            int count = 0;
            while (count < n) {
                //noinspection ResultOfMethodCallIgnored
                read();
                count++;
            }
            return count;
        }

        @Override
        public int available() throws IOException {
            checkClosed();
            return readQueue.size();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            readQueue = null;
        }

        @Override
        public void mark(int readlimit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reset() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        public void enqueue(byte b) {
            if (closed) return;
            while (true) {
                try {
                    readQueue.put(b);
                    return;
                } catch (InterruptedException e) {
                    // Ignore; we really want to put this byte
                }
            }
        }

        public void enqueue(byte[] buf) {
            if (closed) return;
            for (byte b : buf) {
                enqueue(b);
            }
        }

        public void enqueue(Iterable<Byte> bytes) {
            if (closed) return;
            for (byte b : bytes) {
                enqueue(b);
            }
        }

        public void enqueueEOF() {
            if (closed) return;
            while (true) {
                try {
                    readQueue.put(eofMarker);
                    return;
                } catch (InterruptedException e) {
                    // Ignore; we really want to put the eofMarker on the queue
                }
            }
        }

        private void checkClosed() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }
    }

}
