/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.common.filter.object;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.reader.ReplaceFactory;
import dk.statsbiblioteket.util.reader.ReplaceReader;
import dk.statsbiblioteket.util.reader.CircularCharBuffer;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.util.ReaderInputStream;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.io.*;

/**
 * Replaces characters in streams or content. If possible, streams are
 * wrapped in an {@link ReplaceReader} which ensures fast and memory-efficient
 * replacing of characters.
 * </p><p>
 * Two different replacement-methods are provided: Pattern af String.
 * The Pattern-based replacement is defined by {@link #CONF_PATTERN_REGEXP} and
 * the String-based by {@link #CONF_RULES}. If both are defined, they will be
 * evaluated in the order Pattern, String.
 * </p><p>
 * Note: Pattern-based replacement taxes the garbage collector and, if used on
 * streams, requires the while stream to be loaded into memory. If possible,
 * use only the String-based method.
 * </p><p>
 * Important: This filter does not support unicode characters represented by
 * two or more Java characters due to a shortcoming (aka bug) in
 * ReaderInputStream.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReplaceFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(ReplaceFilter.class);

    /**
     * If true, content in Records in Payloads is processed.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_PROCESS_CONTENT =
            "common.replacefilter.process.content";
    public static final boolean DEFAULT_PROCESS_CONTENT = true;

    /**
     * If true, streams in Payloads are processed.
     * </p><p>
     * Optional. Defa   ult is true.
     */
    public static final String CONF_PROCESS_STREAM =
            "common.replacefilter.process.stream";
    public static final boolean DEFAULT_PROCESS_STREAM = true;

    /**
     * The encoding for reading the source data.
     * </p><p>
     * Optional. Default is utf-8.
     */
    public static final String CONF_ENCODING_IN =
            "common.replacefilter.encoding.in";
    public static final String DEFAULT_ENCODING_IN = "utf-8";

    /**
     * The encoding for writing the source data.
     * </p><p>
     * Optional. Default is utf-8.
     */
    public static final String CONF_ENCODING_OUT =
            "common.replacefilter.encoding.out";
    public static final String DEFAULT_ENCODING_OUT = "utf-8";

    /**
     * Used at the regexp for a pattern {@link Pattern} wich will be used to
     * match substrings.
     * </p><p>
     * Important: Performing regexp-replacement can be slow and requires that
     * the full content of streams are loaded into memory.
     * Use of regexp-replacement for streams are discouraged.
     * If possible, use {@link #CONF_RULES} instead as it is faster and less
     * memory-intensive than regexp replacements.
     * </p><p>
     * Optional. Default is null (no regexp replacement).
     */
    public static final String CONF_PATTERN_REGEXP =
            "common.replacefilter.pattern.regexp";

    /**
     * Used as the replacement for matches on {@link #CONF_PATTERN_REGEXP}.
     * This uses the same rules as {@link Matcher#replaceAll(String)}.
     * </p><p>
     * If {@link #CONF_PATTERN_REGEXP} is defined, CONF_PATTERN_REPLACEMENT must
     * also be defined.
     */
    public static final String CONF_PATTERN_REPLACEMENT =
            "common.replacefilter.pattern.destination";

    /**
     * String-to-string replacement, using the fast and memory-efficient replace
     * framework from SBUtil. This is the recommended way of specifying
     * replacements.
     * </p><p>
     * The CONF_RULES holds a list of sub-properties that each define a
     * single replacement.
     * </p><p>
     * Optional. Default is null (no rule-based replacement).
     * @see {@link #CONF_RULE_TARGET} and {@link #CONF_RULE_REPLACEMENT}.
     */
    public static final String CONF_RULES = "common.replacefilter.rules";

    /**
     * The target for a rule aka the String that is used verbatim for matching.
     * This must be defined in each of the sub-properties in the
     * {@link #CONF_RULES} list.
     */
    public static final String CONF_RULE_TARGET =
            "common.replacefilter.rule.target";

    /**
     * The replacement for a rule aka the String that is used verbatim instead
     * of the matched String. This must be defined in each of the sub-properties
     * in the {@link #CONF_RULES} list.
     */
    public static final String CONF_RULE_REPLACEMENT =
            "common.replacefilter.rule.replacement";

    private boolean processContent = DEFAULT_PROCESS_CONTENT;
    private boolean processStream =  DEFAULT_PROCESS_STREAM;
    private String encodingIn =      DEFAULT_ENCODING_IN;
    private String encodingOut =     DEFAULT_ENCODING_OUT;

    private Pattern pattern =           null;
    private String patternReplacement = null;
    private ReplaceFactory factory =    null;
    private ReplaceReader basicReplacer;

    public ReplaceFilter(Configuration conf) {
        super(conf);
        processContent = conf.getBoolean(CONF_PROCESS_CONTENT, processContent);
        processStream =  conf.getBoolean(CONF_PROCESS_STREAM,  processStream);
        encodingIn =     conf.getString( CONF_ENCODING_IN,     encodingIn);
        encodingOut =    conf.getString( CONF_ENCODING_OUT,    encodingOut);
        if (conf.valueExists(CONF_PATTERN_REGEXP)) {
            if (!conf.valueExists(CONF_PATTERN_REPLACEMENT)) {
                throw new ConfigurationException(String.format(
                        "Property %s was defined but %s was not",
                        CONF_PATTERN_REGEXP, CONF_PATTERN_REPLACEMENT));
            }
            pattern = Pattern.compile(conf.getString(CONF_PATTERN_REGEXP));
            patternReplacement = conf.getString(CONF_PATTERN_REPLACEMENT);
            log.debug(String.format(
                    "Added pattern with regexp '%s' and replacement '%s'",
                    CONF_PATTERN_REGEXP, CONF_PATTERN_REPLACEMENT));
        }

        if (conf.valueExists(CONF_RULES)) {
            List<Configuration> ruleConfs;
            try {
                 ruleConfs = conf.getSubConfigurations(CONF_RULES);
                log.debug(String.format("%d rules found", ruleConfs.size()));
            } catch (IOException e) {
                throw new ConfigurationException(String.format(
                        "IOException while extracting sub properties for %s",
                        CONF_RULES), e);
            }
            Map<String, String> rules =
                    new LinkedHashMap<String, String>(ruleConfs.size());
            int count = 0;
            log.debug(String.format("Located %d rules. Extracting...",
                                    ruleConfs.size()));
            for (Configuration ruleConf: ruleConfs) {
                if (!ruleConf.valueExists(CONF_RULE_TARGET) ||
                    !ruleConf.valueExists(CONF_RULE_REPLACEMENT)) {
                    throw new ConfigurationException(String.format(
                            "For each sub-configuration in the list %s, a value"
                            + " for the key %s and the key %s must exist. " 
                            + "Rule #%d (counting from 0) did not comply",
                            CONF_RULES, CONF_RULE_TARGET,
                            CONF_RULE_REPLACEMENT, count));
                }
                if (log.isTraceEnabled()) {
                    log.trace(String.format(
                            "Adding rule with target '%s' and replacement '%s'",
                            ruleConf.getString(CONF_RULE_TARGET),
                            ruleConf.getString(CONF_RULE_REPLACEMENT)));
                }
                rules.put(ruleConf.getString(CONF_RULE_TARGET),
                          ruleConf.getString(CONF_RULE_REPLACEMENT));
                count++;
            }
            factory = new ReplaceFactory(rules);
            basicReplacer = factory.getReplacer();
        }
        if (pattern == null && factory == null) {
            log.info("ReplaceFilter created without any rules. Payloads will "
                     + "pass through untouched");
        }
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (pattern != null) {
            processPattern(payload);
        }
        if (factory != null) {
            try {
                processFactory(payload);
            } catch (UnsupportedEncodingException e) {
                throw new PayloadException(String.format(
                        "Unable to perform replacement on payload due to "
                        + "unsupported encoding. Encoding for read was '%s', "
                        + "encoding for store was '%s'",
                        encodingIn, encodingOut), e, payload);
            }
        }
        return true;
    }

    private void processPattern(Payload payload) throws PayloadException {
        if (payload.getStream() != null) {
            log.debug("Copying Stream content into memory in order to perform "
                      + "Pattern replacement");
            CircularCharBuffer cb =
                    new CircularCharBuffer(1000, Integer.MAX_VALUE);
            Reader in;
            try {
                in = new InputStreamReader(payload.getStream(), encodingIn);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(String.format(
                        "The encoding '%s' for the InputStream is not supported"
                        + " on this platform", encodingIn));
            }
            try {
                int c;
                while ((c = in.read()) != -1) {
                    cb.add((char)c);
                }
            } catch (IOException e) {
                throw new PayloadException(
                        "Unable to read character from Stream", payload);
            }

            String result = pattern.matcher(cb).replaceAll(patternReplacement);
            //noinspection UnusedAssignment
            cb = null; // Make the memory available ASAP
            byte [] resultBytes;
            try {
                resultBytes = result.getBytes(encodingOut);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(String.format(
                        "The encoding '%s' for output is not supported on this "
                        + "platform", encodingOut));
            }
            //noinspection UnusedAssignment
            result = null; // Make the memory available ASAP
            payload.setStream(new ByteArrayInputStream(resultBytes));
        }
        if (payload.getRecord() != null &&
            payload.getRecord().getContent(false) != null) {
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess("ReplaceFilter '" + getName() + "'",
                               "Performing pattern replace on Record content",
                               Logging.LogLevel.TRACE, payload);
            // We hack a bit to guess if content is compressed
            byte[] in = payload.getRecord().getContent();
            boolean compressed =
                    in.length != payload.getRecord().getContent(false).length;
            String replaced;
            try {
                replaced = pattern.matcher(new String(in, encodingIn)).
                        replaceAll(patternReplacement);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(String.format(
                        "Encoding '%s' not supported for reading content",
                        encodingIn));
            }
            try {
                payload.getRecord().setContent(
                    replaced.getBytes(encodingOut), compressed);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(String.format(
                        "Encoding '%s' not supported for storing content",
                        encodingOut));
            }
        }
    }

    private void processFactory(Payload payload) throws
                                                 UnsupportedEncodingException {
        if (payload.getStream() != null) {
            log.debug("Wrapping stream in replacer");
            payload.setStream(new ReaderInputStream(
                    factory.getReplacer(new InputStreamReader(
                            payload.getStream(), encodingIn)),
                    encodingOut));
        }
        if (payload.getRecord() != null &&
            payload.getRecord().getContent(false) != null) {
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess("ReplaceFilter '" + getName() + "'",
                               "Performing replace on Record content",
                               Logging.LogLevel.TRACE, payload);
            // We hack a bit to guess if content is compressed
            byte[] in = payload.getRecord().getContent();
            boolean compressed =
                    in.length != payload.getRecord().getContent(false).length;
            String replaced =
                    basicReplacer.transform(new String(in, encodingIn));
            payload.getRecord().setContent(
                    replaced.getBytes(encodingOut), compressed);
        }
    }
}
