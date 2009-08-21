/* $Id: ARCParser.java 1673 2009-08-14 22:11:05Z toke-sb $
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
package dk.statsbiblioteket.summa.arctika;

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
import java.io.File;

/**
 * Receives a stream in the ARC file format and extracts the content, along with
 * meta-data.
 * </p><p>
 * This filter is a wrapper for the ARCParser from the heritrix project.
 * Meta-data from the ARC container and records are provided at Payload-meta
 * prefixed with "arc".
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ARCParser extends ThreadedStreamParser {
    private static Log log = LogFactory.getLog(ARCParser.class);

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public String[] ARC_FIELDS = new String[]{
            "arc.arcname", "arc.arcoffset", "arc.contentLength", "arc.date",
            "arc.digest", "arc.primaryType", "arc.title", "arc.tstamp",
            "arc.url"};

    // TODO: Add timeout
    private boolean useFileHack = false;

    public ARCParser(Configuration conf) {
        super(conf);
        useFileHack = conf.getBoolean("usefilehack", useFileHack);
        log.debug("ARCParser constructed"
                  + (useFileHack ? " with filehack enabled" : ""));
    }

    private long runCount = 0;
    @Override
    protected void protectedRun() throws Exception {

// 1839 records, 57 sec
        log.trace("Starting protected run " + ++runCount + " for "
                  + sourcePayload);
        /*
        The file hack is truly horrible, but the ARCReaderFactory will always
        expect streams to be GZIPped and we need to experiment with uncompressed
        ARC files as there seems to be an incompatibility between the GZIP
        that Ubuntu uses and the heritrix ARCParser.
        // TODO: Locate and eliminate the GZIP incompatability problem
         */
        ArchiveReader archiveReader =
                useFileHack
                ? ARCReaderFactory.get(new File(sourcePayload.getData(
                        Payload.ORIGIN).toString()), false, 0)
                : ARCReaderFactory.get("Foo", sourcePayload.getStream(), true);

        Iterator<ArchiveRecord> archiveRecords = archiveReader.iterator();
        // TODO: Consider skipping the first record (meta-data for the ARC file)
        int internalCount = 0;
        if (!archiveRecords.hasNext()) {
            String message = "No record present in ARC";
            log.debug(message + " for " + sourcePayload);
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess("ARCParser", message, Logging.LogLevel.DEBUG,
                               sourcePayload);
        }
        while (archiveRecords.hasNext() && running) {
            log.trace("Extracting record " + ++internalCount);
            ArchiveRecord ar = archiveRecords.next();
            ArchiveRecordHeader header = ar.getHeader();
            FutureInputStream arStream = new FutureInputStream(ar);
            Payload payload = new Payload(arStream);
            fillPayloadFromHeader(payload, header, archiveReader.getFileName());
            addToQueue(payload);

            arStream.waitForClose();
            if (!arStream.isClosed()) {
                //noinspection DuplicateStringLiteralInspection
                log.warn("Timeout while waiting for close of record from ARC "
                         + "from " + sourcePayload);
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
        log.debug("Ending protected run " + runCount + " with " + internalCount
                  + " extracted records. running=" + running);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private void fillPayloadFromHeader(Payload payload,
                                       ArchiveRecordHeader header,
                                       String arcFilename) {
        payload.setID(header.getUrl());
        payload.getData().put("arc.arcname", arcFilename);
        payload.getData().put("arc.arcoffset", header.getOffset());
//        payload.getData().put("arc.boost", header.);
//        payload.getData().put("arc.collection", header.);
        payload.getData().put("arc.contentLength", header.getLength());
        payload.getData().put("arc.date", header.getDate()); // Epoch
        payload.getData().put("arc.digest", header.getDigest()); // ?
        payload.getData().put("arc.primaryType", header.getMimetype());
//        payload.getData().put("arc.segment", header.);
//        payload.getData().put("arc.subType", header.);
        payload.getData().put("arc.title", header.getRecordIdentifier());
        payload.getData().put("arc.tstamp", header.getDate());
        payload.getData().put("arc.url", header.getUrl());
    }
}
