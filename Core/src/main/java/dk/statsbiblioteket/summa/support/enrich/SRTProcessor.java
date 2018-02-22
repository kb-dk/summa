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
package dk.statsbiblioteket.summa.support.enrich;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.xml.XMLStepper;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Locates SRT content (https://en.wikipedia.org/wiki/SubRip) from the Payload, prepares it for indexing and
 * stores the result in the Payload's ephemeral object store for later processing in the current process chain.
 *
 * Normally used with {@link dk.statsbiblioteket.summa.support.solr.SolrDocumentEnrich} later in the chain.
 *
 * The index-prepared String is chosen to work with common analyzers & tokenizers. the sample input
 * <pre>
 * 168
 * 00:20:41,150 --> 00:20:45,109
 * - How did he do that?
 * - Made him an offer he couldn't refuse.
 * </pre>
 * becomes
 * <pre>
 *     sub002041150e002045109i168
 *     - How did he do that?
 *     - Made him an offer he couldn't refuse.
 * </pre>
 * The String {@code sub002041150e002045109i168} hopefully survives the analyze/tokenize process intact.
 */
public class SRTProcessor extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(SRTProcessor.class);

    public static final int MAX_SRTS = 10; // We expect only one, but more is not a problem as such.
    public static final String SRT_KEY = "SRT";

    /**
     * The XPath to SRTs in the incoming XML.
     */
    public static final String CONF_SRT_FAKE_XPATH = "srt.fakexpath";

    private final String srtFakeXpath;

    public SRTProcessor(Configuration conf) {
        super(conf);
        if (!conf.containsKey(CONF_SRT_FAKE_XPATH)) {
            throw new ConfigurationException(CONF_SRT_FAKE_XPATH + " was not specified");
        }
        srtFakeXpath = conf.getString(CONF_SRT_FAKE_XPATH);
    }

    /*
    Extracts the SRTs in the Payload and passes them to processSRTs.
     */
    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        try {
            List<String> srts = XMLStepper.evaluateFakeXPaths(
                    RecordUtil.getString(payload), Collections.singletonList(srtFakeXpath), MAX_SRTS).get(0);
            if (srts.isEmpty()) {
                Logging.logProcess("SRTProcessor", "No SRT", Logging.LogLevel.DEBUG, payload);
                return true;
            }
            processSRTs(payload, srts);
        } catch (XMLStreamException e) {
            // TODO: Consider logging to process
            log.warn("Exception extracting SRT from " + payload, e);
        }
        return true;
    }

    /*
    Iterates all the SRTs and passes them one by one to processSRT.
     */
    private void processSRTs(Payload payload, List<String> srts) {
        log.debug("Processing " + srts.size() + " SRT blocks from " + payload.getId());
        List<String> solrSRT = new ArrayList<>();
        for (String srt: srts) {
            processSRT(payload, srt, solrSRT);
        }
        Logging.logProcess("SRTProcessor", "Processed " + srts.size() + " SRT blocks for a total of " +
                                           solrSRT.size() + " entries", Logging.LogLevel.DEBUG, payload);
        if (!solrSRT.isEmpty()) {
            payload.getObjectData().put(SRT_KEY, solrSRT);
        }
    }

    /*
    Iterate entries in a single SRT and passes the single entries to processEntry.
     */
    private void processSRT(Payload payload, String srt, List<String> solrSRT) {
        String[] entries = DOUBLE_NEWLINE.split(srt);
        for (String entry : entries) {
            processEntry(payload, entry, solrSRT);
        }
    }
    private final Pattern DOUBLE_NEWLINE = Pattern.compile("(?s)\n\n");

    /*
     https://en.wikipedia.org/wiki/SubRip
168
00:20:41,150 --> 00:20:45,109
- How did he do that?
- Made him an offer he couldn't refuse.
    */
    // Filters are not guaranteed to be thread-safe, so synchronization (due to the use of sb) is just paranoia
    private synchronized void processEntry(Payload payload, String entry, List<String> solrSRT) {
        if (entry.isEmpty()) {
            return;
        }
        String[] lines = SINGLE_NEWLINE.split(entry);
        if (lines.length < 2) {
            Logging.logProcess("SRTProcessor",
                               "Expected at least " + lines.length + " lines in SRT entry, but found only " +
                               lines.length + ". Entry: " + entry,
                               Logging.LogLevel.WARN, payload);
            return;
        }

        // 168
        int entryID;
        try {
            entryID = Integer.parseInt(lines[0]);
        } catch (NumberFormatException e) {
            Logging.logProcess("SRTProcessor",
                               "Expected the first line '" + lines[0] + "' in the SRT entry to be an integer",
                               Logging.LogLevel.WARN, payload);
            return;
        }

        // 00:20:41,150 --> 00:20:45,109
        Matcher ts = TIMESTAMPS.matcher(lines[1]);
        if (!ts.find()) {
            Logging.logProcess("SRTProcessor",
                               "Expected the second line '" + lines[1] +
                               "' in the SRT entry to match the pattern " + TIMESTAMPS.pattern(),
                               Logging.LogLevel.WARN, payload);
            return;
        }

        sb.setLength(0);
        int i = 1;
        sb.append("sub").
                append(ts.group(i++)).append(ts.group(i++)).append(ts.group(i++)).append(ts.group(i++)) // begin
                .append("e")
                .append(ts.group(i++)).append(ts.group(i++)).append(ts.group(i++)).append(ts.group(i))  // end
                .append("i")
                .append(Integer.toString(entryID)); // entryID
        for (int l = 2 ; l < lines.length ; l++) {  // Text
            sb.append("\n").append(lines[l]);
        }
        solrSRT.add(sb.toString());
    }
    private final StringBuilder sb = new StringBuilder();
    private final Pattern SINGLE_NEWLINE = Pattern.compile("(?s)\n");
    private final Pattern TIMESTAMPS = Pattern.compile(
            "([0-9][0-9]):([0-9][0-9]):([0-9][0-9]),([0-9][0-9][0-9]) --> " +
            "([0-9][0-9]):([0-9][0-9]):([0-9][0-9]),([0-9][0-9][0-9])");
}
