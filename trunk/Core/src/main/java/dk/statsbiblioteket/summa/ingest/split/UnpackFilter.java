/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.ingest.stream.ZIPParser;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A Strategy Pattern unpacker that uses file endings to perform appropriate
 * unpacking. All supported compressed files (only ZIP right now) will be
 * unpacked recursively. If the input is not a supported packed file, it will
 * be passed along non-modified.
 * </p><p>
 * The unpacker supports Stream-based Payloads.
 * The file name is taken from {@code payload.getData(Payload.ORIGIN)}.
 * </p><p>
 * Due to the do-the-right-thing nature of this filter, it is recommended to
 * put it at the beginning of ingest chains, right after a
 * {@link dk.statsbiblioteket.summa.ingest.stream.FileReader}
 * or similar Stream producer.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class UnpackFilter implements ObjectFilter {
    private static Log log = LogFactory.getLog(UnpackFilter.class);

    /**
     * Only files with this ending will be passed on.
     * </p><p>
     * This property is optional. Default is ".*\.xml".
     */
    public static final String CONF_FILE_PATTERN =
            "summa.ingest.unpackfilter.filepattern";
    public static final String DEFAULT_FILE_PATTERN = ".*\\.xml|.*\\.XML";

    /**
     * If no Payload.ORIGIN is specified in the Payload from the source, this
     * ending is used for selecting the initial unpacker.
     * </p><p>
     * Optional. Default is zip.
     */
    public static final String CONF_EMPTY_ORIGIN_POSTFIX =
        "summa.ingest.unpackfilter.emptyoriginpostfix";
    public static final String DEFAULT_EMPTY_ORIGIN_POSTFIX = "zip";


    /**
     * Supported unpackers.
     */
    private static final Map<String, Class<? extends StreamParser>> unpackers;
    static {
        unpackers = new HashMap<String, Class<? extends StreamParser>>(5);
        unpackers.put("zip", ZIPParser.class);
    }


    /**
     * The active unpackers. The last unpacker in the list is the current
     * producer. When the last unpacker is depleted, is is returned to
     * {@link #passive}. When the active list is empty, the next Payload from
     * source is processed.
     */
    private final Deque<Pair<String, StreamParser>> active =
        new ArrayDeque<Pair<String, StreamParser>>();

    /**
     * Instantiated unpackers that are not in use. The key is the supported
     * file ending.
     */
    private final List<Pair<String, StreamParser>> passive =
        new ArrayList<Pair<String, StreamParser>>();

    /**
     * The source for the Payloads.
     */
    private ObjectFilter source;

    /**
     * The current Payload ready for delivery.
     */
    private Payload payload = null;

    private Configuration conf;
    private Pattern filePattern;
    private String emptyOriginPostfix = DEFAULT_EMPTY_ORIGIN_POSTFIX;

    // TODO: Extract file ending regexp


    public UnpackFilter(Configuration conf) {
        this.conf = conf;
        emptyOriginPostfix = conf.getString(
            CONF_EMPTY_ORIGIN_POSTFIX, emptyOriginPostfix);
        filePattern = Pattern.compile(conf.getString(
                CONF_FILE_PATTERN, DEFAULT_FILE_PATTERN));

        StringWriter sw = new StringWriter();
        boolean first = true;
        for (Map.Entry<String, Class<? extends StreamParser>> entry:
            unpackers.entrySet()) {
            if (first) {
                first = false;
            } {
                sw.append(", ");
            }
            sw.append(entry.getKey()).append(": ");
            sw.append(entry.getValue().getSimpleName());
        }
        log.debug("Creating UnpackFilter for files with pattern '"
                  + filePattern.pattern() + "'. Supported unpackers: " + sw);
    }

    @Override
    public Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        if (payload != null) {
            log.trace("Payload already assigned");
        } else {
            makePayload();
        }
        Payload newPayload = payload;
        //noinspection AssignmentToNull
        payload = null;
        if (Logging.processLog.isTraceEnabled()) { // Cheating here
            Logging.logProcess("UnpackFilter", "Unpacked file",
                               Logging.LogLevel.TRACE, newPayload);
        }
        //  We cannot calls stop yet, as newPayload hasn't been finished
        /*if (payload == null) {
            log.debug("hasNext() is false, calling stop on parser");
            parser.stop();
        } */
        return newPayload;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported");
    }

    /**
     * The core method. Extracts the next Payload from the chain of
     * {@link #active} until the chain is depleted after which a new chain is
     * build from the Payload delivered by source.
     */
    private void makePayload() {
        log.trace("makePayload() called");
        if (payload != null) {
            if (log.isTraceEnabled()) {
                log.trace("makePayload: Payload already assigned with "
                          + payload);
            }
            return;
        }
        // The existing parsers
        while (payload == null) {
            while (payload == null && !active.isEmpty()) {
                StreamParser last = active.peek().getValue();
                try {
                    if (last.hasNext()) {
                        payload = last.next();
                    } else {
                        passive.add(active.pop());
                    }
                } catch (Exception e) {
                    log.warn("Exception requesting payload from parser, "
                             + "skipping to next stream payload", e);
                    last.stop();
                    passive.add(active.pop());
                }
            }
            // If no Payload, try source
            if (payload == null && source.hasNext()) {
                payload = source.next();
                if (payload.getData(Payload.ORIGIN) == null &&
                    !"".equals(emptyOriginPostfix)) {
                    payload.getData().put(
                        Payload.ORIGIN, "dummyname." + emptyOriginPostfix);
                }
            }
            if (payload == null) {
                log.debug("makePayload: No more stream payloads available");
                return;
            }
            wrap();
        }
    }

    /**
     * Wraps the payload in an unpacker if the name ending matches.
     */
    private void wrap() {
        // Check for origin existence
        String origin = payload.getStringData(Payload.ORIGIN);
        if (origin == null) {
            Logging.logProcess(
                "UnpackFilter",
                "No " + Payload.ORIGIN + " defined in meta data. Unable to "
                + "determine unpacker", Logging.LogLevel.WARN, payload);
            return;
        }
        // Wrap Payload in packer if necessary
        String[] tokens = origin.split("\\.");
        String postfix = tokens[tokens.length -1].toLowerCase();
        if (unpackers.containsKey(postfix)) {
            if (log.isTraceEnabled()) {
                log.trace("Ending '" + postfix + "' matches an unpacker");
            }
            for (int i = 0 ; i < passive.size() ; i++) {
                Pair<String, StreamParser> unpacker = passive.get(i);
                if (unpacker.getKey().equals(postfix)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Located passive unpacker for ending '"
                                  + postfix + "'. Reusing unpacker");
                    }
                    unpacker.getValue().open(payload);
                    payload = null;
                    if (!passive.remove(unpacker)) {
                        log.warn("Unable to remove '" + unpacker
                                 + "' from passive list");
                    }
                    active.push(unpacker);
                    return;
                }
            }
            // No passive unpacker. Create a new one
            Class<? extends StreamParser> parserClass = unpackers.get(postfix);
            log.debug("Creating unpacker '" + parserClass.getName() + "'");
            StreamParser unpacker;
            if ("zip".equals(postfix)) {
                Configuration zConf = Configuration.newMemoryBased(
                    ZIPParser.CONF_FILE_PATTERN, ".*");
                unpacker = Configuration.create(unpackers.get(postfix), zConf);
            } else {
                unpacker = Configuration.create(unpackers.get(postfix), conf);
            }
            log.debug("Created unpacker '" + parserClass.getName()
                      + "' for ending '" + postfix + "'");
            Pair<String, StreamParser> pair =
                new Pair<String, StreamParser>(postfix, unpacker);
            active.push(pair);
            unpacker.open(payload);
            payload = null;
        } else if (!filePattern.matcher(origin).matches()) {
            Logging.logProcess(
                "UnpackFilter",
                "Payload origin did not match '" + filePattern.pattern()
                + "'. Discarding Payload", Logging.LogLevel.DEBUG, payload);
            payload = null;
        }
    }

    @Override
    public boolean hasNext() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("hasNext() called");
        checkSource();
        if (payload == null) {
            makePayload();
        }
        log.trace("hasNext() resolved to " + (payload != null));
        return payload != null;
    }

    @Override
    public void setSource(Filter filter) {
        if (!(filter instanceof ObjectFilter)) {
            throw new IllegalArgumentException(
                    "UnpackFilter can only be chained to ObjectFilters");
        }
        log.debug("Assigning source " + source);
        source = (ObjectFilter)filter;
    }

    @Override
    public void close(boolean success) {
        if (source == null) {
            log.warn(String.format(
                    "close(%b): Cannot close as no source is specified",
                    success));
        } else {
            while (!active.isEmpty()) {
                active.pop().getValue().stop();
            }
            source.close(success);
        }
    }

    @Override
    public boolean pump() throws IOException {
        if (!hasNext()) {
            return false;
        }
        Payload next = next();
        if (next == null) {
            log.warn("pump: Got null as next after hasNext() was true");
            return false;
        }
        //noinspection DuplicateStringLiteralInspection
        Logging.logProcess("UnpackFilter",
                           "Calling close for Payload as part of pump()",
                           Logging.LogLevel.TRACE, payload);
        next.close();
        return true;
    }

    private void checkSource() {
        if (source == null) {
            throw new NoSuchElementException(
                    "No source specified for UnpackFilter");
        }
    }
}
