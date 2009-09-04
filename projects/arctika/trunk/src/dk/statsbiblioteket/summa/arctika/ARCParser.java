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
import java.io.*;

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

    /**
     * If true, ARC-content that starts with "http" is expected to start with
     * HTTP-headers. These headers will be trimmed and added to the meta-data
     * for the payload, with the key-prefix "http-header.".
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_REMOVE_HTTP_HEADERS =
            "arcparser.removehttpheaders";
    public static final boolean DEFAULT_REMOVE_HTTP_HEADERS = true;

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String[] ARC_FIELDS = new String[]{
            "arc.arcname", "arc.arcoffset", "arc.contentLength", "arc.date",
            "arc.digest", "arc.primaryType", "arc.title", "arc.tstamp",
            "arc.url",
            // custom additions below
            "arc.site" };
    public static final String HTTP_PREFIX = "http-header.";

    // TODO: Add timeout
    private boolean useFileHack = false;
    private boolean removeHTTPHeaders = DEFAULT_REMOVE_HTTP_HEADERS;

    public ARCParser(Configuration conf) {
        super(conf);
        useFileHack = conf.getBoolean("usefilehack", useFileHack);
        removeHTTPHeaders = conf.getBoolean(
                CONF_REMOVE_HTTP_HEADERS, removeHTTPHeaders);
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
/*            for (Object field: ar.getHeader().getHeaderFields().entrySet().toArray()) {
                System.out.println(field);
            }*/
            handleHTTPHeaders(payload);
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

    // Leaved the stream at the beginning of the real content
    private void handleHTTPHeaders(Payload payload) throws IOException {
        if (!removeHTTPHeaders) {
            log.trace("RemoveHTTPHeaders not enabled");
            return;
        }
        String url = payload.getData("arc.url").toString();
        if (url.length() < 4 || !url.startsWith("http")) {
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "Skipped handleHTTPHeaders for %s as content did not"
                        + " start with http", payload));
            }
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("Extracting HTTP headers from " + payload);
        }
        BufferedReader in;
        try {
            // Buffer needs to be 1 as the BufferedReader is only used for header
            in = new BufferedReader(new InputStreamReader(
                    payload.getStream(), "utf-8"), 1);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("utf-8 not supported", e);
        }
        String line;
        int counter = 0;
        while (!"".equals(line = in.readLine())) {
            //System.out.println("*** " + line);
            String[] tokens = line.split(": ", 2);
            if (tokens.length == 2 && !"".equals(tokens[0])) {
                payload.getData().put(HTTP_PREFIX + tokens[0], tokens[1]);
                if (log.isTraceEnabled()) {
                    log.trace(HTTP_PREFIX + tokens[0] + " = " + tokens[1]);
                }
                counter++;
            }
        }
//        System.out.println("*Extra* " + in.readLine());
//        System.out.println("*Extra* " + in.readLine());
        log.trace("Extracted " + counter + " HTTP headers");
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private void fillPayloadFromHeader(
            Payload payload, ArchiveRecordHeader header, String arcFilename) {
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
        payload.getData().put("arc.site", extractSite(header.getUrl()));
    }

    // https://foo/bar... => foo
    // dns:foo/bar... => foo
    private String extractSite(String url) {
        try {
            String[] colonTokens = url.split(":", 2);
            if (colonTokens.length < 2) {
                return null;
            }
            if ("filedesc".equals(colonTokens[0])) {
                return null;
            }
            // Hackety hack
            String rest = colonTokens[1];
            while (rest.startsWith("/")) {
                rest = rest.substring(1, rest.length());
            }
            String first = rest.split("/")[0];
//        System.out.println(slashtokens[0]);
            if (first.startsWith("www.")) {
                first = first.substring(4, first.length());
            }
            return first;
        } catch (Exception e) {
            log.warn("Exception extracting site from " + url, e);
            return null;
        }
    }
}
