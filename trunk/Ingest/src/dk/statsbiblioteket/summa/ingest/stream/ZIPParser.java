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
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParser;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
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
     * Optional. Default is 1 hour.
     */
    public static final String CONF_PROCESSING_TIMEOUT =
            "summa.ingest.unzipfilter.processingtimeout";
    public static final int DEFAULT_PROCESSING_TIMEOUT = 60 * 60 * 1000; // 1h

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
        ZipInputStream zip = new ZipInputStream(sourcePayload.getStream());
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
            matching++;
            ZipEntryInputStream zipStream = new ZipEntryInputStream(zip);
            Payload payload = new Payload(zipStream);
            payload.getData().put(Payload.ORIGIN, entry.getName());
            addToQueue(payload);
            long startTime = System.currentTimeMillis();
            try {
                synchronized (zipStream.waiter) {
                    zipStream.waiter.wait(processingTimeout);
                }
            } catch (InterruptedException e) {
                log.warn(String.format(
                        "Interrupted while waiting for entry %s from %s",
                        entry.getName(), sourcePayload));
            }
            if (System.currentTimeMillis() - startTime >=
                processingTimeout) {
                Logging.logProcess("ZIPParser", String.format(
                        "Timeout occured while waiting for the processing "
                        + "of %s. The entry will be skipped",
                        entry.getName()),
                                   Logging.LogLevel.DEBUG, sourcePayload);
            }
            if (!zipStream.isClosed()) {
                zipStream.close();
            }
        }
        zip.close();
        // TODO: Check if Payload should be closed here
        log.debug(String.format("Processed %d ZIP entries from %s",
                                matching, sourcePayload));
    }

    /**
     * An encapsulation of a ZipInputStream that notifies the attribute waiter
     * upon close and changes close-behaviour to closeEntry.
     */
    private class ZipEntryInputStream extends InputStream {
        private ZipInputStream zip;
        public final Object waiter = new Object();
        private boolean closed = false;

        private ZipEntryInputStream(ZipInputStream zip) {
            this.zip = zip;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                zip.closeEntry();
                closed = true;
            }
            synchronized (waiter) {
                waiter.notifyAll();
            }
        }

        /**
         * Checks is this ZIP-Stream entry has already been closed.
         * @throws IOException if the ZIP-stream entry is closed.
         */
        private void checkClose() throws IOException {
            if (closed) {
                throw new IOException(
                        "The entry is closed. No further action is possible");
            }
        }

        public boolean isClosed() {
            return closed;
        }

        /* Delegation */

        @Override
        public int read() throws IOException {
            checkClose();
            return zip.read();
        }

        @Override
        public boolean markSupported() {
            return zip.markSupported();
        }

        @Override
        public void mark(int readlimit) {
            if (closed) {
                log.warn("Mark attempted on closed ZIP-Stream entry");
            }
            zip.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            checkClose();
            zip.reset();
        }

        @Override
        public int read(byte[] b) throws IOException {
            checkClose();
            return zip.read(b);
        }

        public ZipEntry getNextEntry() throws IOException {
            checkClose();
            return zip.getNextEntry();
        }

        public void closeEntry() throws IOException {
            if (closed) {
                log.debug("Close called on already closed ZIP-Stream entry");
                return;
            }
            zip.closeEntry();
        }

        @Override
        public int available() throws IOException {
            checkClose();
            return zip.available();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkClose();
            return zip.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            checkClose();
            return zip.skip(n);
        }

    }
}
