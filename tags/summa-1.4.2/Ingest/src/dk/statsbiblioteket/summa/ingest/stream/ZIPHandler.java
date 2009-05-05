/* $Id:$
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
import dk.statsbiblioteket.summa.ingest.split.StreamParser;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.ReentrantLock;
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
public class ZIPHandler implements StreamParser {
    private static Log log = LogFactory.getLog(ZIPHandler.class);

    /**
     * The file pattern to match.
     * </p><p>
     * This property is optional. Default is ".*\.xml".
     */
    public static final String CONF_FILE_PATTERN =
            "summa.ingest.unzipfilter.filepattern";
    public static final String DEFAULT_FILE_PATTERN = ".*\\.xml";

    private Pattern filePattern;
    private Payload payload = null;
    private ZipInputStream zip = null;

    public ZIPHandler(Configuration conf) {
        filePattern = Pattern.compile(conf.getString(
                CONF_FILE_PATTERN, DEFAULT_FILE_PATTERN));
        log.info(String.format(
                "Created ZIPHandler with file pattern '%s'",
                conf.getString(CONF_FILE_PATTERN, DEFAULT_FILE_PATTERN)));
    }

    public void open(Payload streamPayload) {
        payload = streamPayload;
        if (payload == null) {
            log.warn("null Payload received in open. Ignoring");
            return;
        }

        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void stop() {

        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean hasNext() {
        return payload != null; // TODO: More checks
    }

    public Payload next() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void remove() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private ReentrantLock entryLock = new ReentrantLock(true);
    /**
     * An encapsulation of an entry in a ZipInputStream that holds a lock,
     * making it impossible to create new ZipEntryInputStreams any old
     * instance has been closed.
     */
    private class ZipEntryInputStream extends InputStream {
        private ZipEntry entry;
        private ZipInputStream zip;

        private ZipEntryInputStream(ZipEntry entry, ZipInputStream zip) {
            this.zip = zip;
            this.entry = entry;
//            entryLock
        }



        public int read() throws IOException {
  return -1;
        }
    }
}
