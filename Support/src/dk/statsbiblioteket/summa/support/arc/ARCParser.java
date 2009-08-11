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
package dk.statsbiblioteket.summa.support.arc;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParser;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.util.FutureInputStream;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;

import java.util.Iterator;

/**
 * Receives a stream in the ARC file format and extracts the content, along with
 * meta-data.
 * </p><p>
 * This filter is a wrapper for the ARCParser from the heritrix project.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ARCParser extends ThreadedStreamParser {
    private static Log log = LogFactory.getLog(ARCParser.class);

    // TODO: Add timeout

    public ARCParser(Configuration conf) {
        super(conf);
        log.debug("ARCParser constructed");
    }

    @Override
    protected void protectedRun() throws Exception {
        ArchiveReader archiveReader =
                ARCReaderFactory.get("Foo", sourcePayload.getStream(), true);
        Iterator<ArchiveRecord> archiveRecords = archiveReader.iterator();
        // TODO: Consider skipping the first record (meta-data for the ARC file)
        while (archiveRecords.hasNext() && running) {
            ArchiveRecord ar = archiveRecords.next();
            ArchiveRecordHeader header = ar.getHeader();
            FutureInputStream arStream = new FutureInputStream(ar);
            Payload payload = new Payload(arStream);
            payload.setID(header.getUrl());
            addToQueue(payload);

            arStream.waitForClose();
            if (!arStream.isClosed()) {
                //noinspection DuplicateStringLiteralInspection
                Logging.logProcess(
                        "ARCParser",
                        "Stopped parsing as the handler of the last generated "
                        + "Payload did not close the Stream",
                        Logging.LogLevel.DEBUG, sourcePayload);
                break;
            }
        }
        if (!running) {
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess(
                    "ARCParser",
                    "Stopped parsing  due to the running-flag being false",
                    Logging.LogLevel.DEBUG, sourcePayload);
        }
    }
}
