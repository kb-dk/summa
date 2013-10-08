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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes ALTO-content and extracts statistics to the log (INFO level) upon close.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoStatFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(AltoStatFilter.class);

    /**
     * Outputs a complete list of matches from the given regexps applied to each line in the given ALTOs.
     * Groups are concatenated unless {@link #CONF_REPLACMENT} are defined.
     * </p><p>
     * Example: "(?:^| )(ca[.] [^ ]+)", "(?:^| )(cirka [^ ]+)" ->
     * "ca. midnat (54)"
     * "ca. middag (12)"
     * "cirka midnat (12)"
     * </p><p>
     * List of regexps. Optional. Default is not defined.
     */
    public static final String CONF_REGEXPS = "altostatfilter.regexps";

    /**
     * Must match the number of {@link #CONF_REGEXPS} Strings exactly if defined.
     * </p><p>
     * List of replacements for the regexps. Optional. Default is not defined (all groups are concatenated).
     */
    public static final String CONF_REPLACMENT = "altostatfilter.replacements";

    private int altoCount = 0;
    private int blockCount = 0;
    private int lineCount = 0;
    private final List<Pattern> regexps;
    private final List<String> replacements;
    private final HashMap<Pattern, HashMap<String, Integer>> matches;

    private final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    {
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    public AltoStatFilter(Configuration conf) {
        super(conf);
        String[] regStrs = conf.getStrings(CONF_REGEXPS, (String[])null);
        regexps = new ArrayList<Pattern>(regStrs == null ? 0 : regStrs.length);
        if (regStrs != null) {
            String[] repStrs = conf.getStrings(CONF_REPLACMENT, (String[])null);
            replacements = repStrs == null ? null : Arrays.asList(repStrs);
            if (repStrs != null && repStrs.length != regStrs.length) {
                throw new ConfigurationException("The number of replacements was " + repStrs.length + " but there was "
                                                 + regStrs.length + " regexps");
            }
            for (String regStr: regStrs) {
                Pattern pattern = Pattern.compile(regStr);
                regexps.add(pattern);
            }
        } else {
            replacements = null;
        }
        matches = new HashMap<Pattern, HashMap<String, Integer>>(regexps.size());
        for (Pattern pattern: regexps) {
            matches.put(pattern, new HashMap<String, Integer>());
        }
        log.info("Created " + this);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        try {
            Alto alto = new Alto(RecordUtil.getReader(payload), (String) payload.getData(Payload.ORIGIN));
            extractStats(alto);
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to parse presumed ALTO XML", e, payload);
        }
        return true;
    }

    @Override
    public void close(boolean success) {
        super.close(success);
        log.info(getStats());
    }

    private void extractStats(Alto alto) {
        altoCount++;
        for (Map.Entry<String, List<Alto.TextBlock>> groups: alto.getTextBlockGroups().entrySet()) {
            for (Alto.TextBlock block: groups.getValue()) {
                blockCount++;
                for (Alto.TextLine line: block.getLines()) {
                    lineCount++;
                    extract(line);
                }
            }
        }
    }

    private void extract(Alto.TextLine line) {
        for (int i = 0 ; i < regexps.size() ; i++) {
            extract(regexps.get(i), replacements == null ? null : replacements.get(i), line.getAllText());
        }
    }

    private void extract(Pattern regexp, String replacement, String text) {
        Matcher matcher = regexp.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            result.setLength(0);
            if (replacement != null) {
                matcher.appendReplacement(result, replacement);
            } else if (matcher.groupCount() == 0) {
                result.append(matcher.group());
            } else {
                result.setLength(0);
                for (int i = 0 ; i < matcher.groupCount() ; i++) {
                    if (result.length() > 0) {
                        result.append(" ");
                    }
                    result.append(matcher.group(i + 1));
                }
            }
            HashMap<String, Integer> countMap = matches.get(regexp);
            String r = result.toString();
            Integer count = countMap.get(r);
            countMap.put(r, count == null ? 1 : count + 1);
        }
    }

    public String getStats() {
        StringBuilder sb = new StringBuilder(500);
        sb.append("Processed ALTOs: ").append(altoCount).append("\n");
        sb.append("Processed blocks: ").append(blockCount).append("\n");
        sb.append("Processed lines: ").append(lineCount).append("\n");
        sb.append("Matches:\n");
        for (Map.Entry<Pattern, HashMap<String, Integer>> entry: matches.entrySet()) {
            sb.append("  Regexp ").append(entry.getKey().pattern()).append("\n");
            for (Map.Entry<String, Integer> match: entry.getValue().entrySet()) {
                sb.append("    ").append(match.getKey()).append(": ").append(match.getValue()).append("\n");
            }
        }
        return sb.toString();
    }
}
