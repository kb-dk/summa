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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Handles ZIP-streams by uncompressing and splitting the content into separate
 * streams. the ZIPHandler is normally used by a {@link StreamController}.
 * </p><p>
 * Note: As streams does not allow for random access, calls to {@link #next}
 * will block until the previous stream has been closed.
 * // TODO: Consider adding a timeout or similar to guard against streams not
 * being closed.
 * Note: The filter {@link ArchiveReader} supports reading compressed content
 * directly from the file system and is preferable if the source is files.
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
    public static final String CONF_FILE_PATTERN = "summa.ingest.unzipfilter.filepattern";
    public static final String DEFAULT_FILE_PATTERN = ".*\\.xml";

    /**
     * The maximum amount of milliseconds to wait for the processing of a
     * ZIP-stream entry. If the processing-time exceeds this amount, the parser
     * will perform a close on the current entry and skip to the next.
     * </p><p>
     * Note: currently (20121218) this has no effect.
     * </p><p>
     * Optional. Default is Integer.MAX_VALUE.
     */
    public static final String CONF_PROCESSING_TIMEOUT = "summa.ingest.unzipfilter.processingtimeout";
    public static final int DEFAULT_PROCESSING_TIMEOUT = Integer.MAX_VALUE;

    private Pattern filePattern;

    public ZIPParser(Configuration conf) {
        super(conf);
        filePattern = Pattern.compile(conf.getString(CONF_FILE_PATTERN, DEFAULT_FILE_PATTERN));
//        processingTimeout = conf.getInt(CONF_PROCESSING_TIMEOUT, DEFAULT_PROCESSING_TIMEOUT);
        log.info(String.format("Created ZIPParser with file pattern '%s'", conf.getString(CONF_FILE_PATTERN,
                                                                                          DEFAULT_FILE_PATTERN)));
    }

    @Override
    protected void protectedRun(Payload source) throws Exception {
        log.debug("Opening ZIP-Stream from " + source);
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(source.getStream()));
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
            final MonitoredPipedInputStream payloadIn = new MonitoredPipedInputStream();
            PipedOutputStream payloadPipe = new PipedOutputStream();
            payloadPipe.connect(payloadIn);
            String origin = source.getId() == null ?
                    "origin '" + source.getData(Payload.ORIGIN) + "'" :
                    "Payload " + source.getId();
            Payload payload = new Payload(payloadIn, "Copied from " + origin);
            payload.getData().put(Payload.ORIGIN, source.getData(Payload.ORIGIN) + "!" + entry.getName());
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
        log.debug("Ending processing of " + source + " with running=" + running);
        zip.close();
        // TODO: Check if Payload should be closed here
        log.debug(String.format("Processed %d ZIP entries from %s", matching, source));
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
