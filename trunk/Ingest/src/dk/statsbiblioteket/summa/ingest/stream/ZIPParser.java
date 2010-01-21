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

import java.io.*;
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
@QAInfo(level = QAInfo.Level.PEDANTIC,
        state = QAInfo.State.QA_NEEDED,
        author = "te, mke")
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
                      + "and waiting for close or depleted ZIP entry");

            /* Prepare a stream that we'll fill asynchronously while the
             * payload consumer reads from it */
            matching++;
            MonitoredPipedInputStream payloadIn =
                                            new MonitoredPipedInputStream();
            PipedOutputStream payloadPipe = new PipedOutputStream();
            payloadPipe.connect(payloadIn);
            Payload payload = new Payload(payloadIn);
            payload.getData().put(
                    Payload.ORIGIN,
                    sourcePayload.getData(Payload.ORIGIN) + "!"
                    + entry.getName());
            addToQueue(payload);

            /* Pipe the stream we gave to the payload */
            try {
                byte[] buf = new byte[2048];
                int numRead;
                while ((numRead = zip.read(buf)) != -1 && running &&
                       !payloadIn.isClosed()) {
                    payloadPipe.write(buf, 0, numRead);
                }
            } finally {
                payloadPipe.flush();
                payloadPipe.close();
            }
        }
        log.debug("Ending processing of " + sourcePayload + " with running="
                  + running);
        zip.close();
        // TODO: Check if Payload should be closed here
        log.debug(String.format("Processed %d ZIP entries from %s",
                                matching, sourcePayload));
    }

    /**
     * A piped input stream where you can check if it has been closed
     */
    private static class MonitoredPipedInputStream extends PipedInputStream {
        private boolean closed = false;

        public synchronized boolean isClosed() {
            return closed;
        }

        @Override
        public synchronized void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
