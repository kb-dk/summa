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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.ReaderInputStream;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.reader.CircularCharBuffer;
import dk.statsbiblioteket.util.reader.ReplaceFactory;
import dk.statsbiblioteket.util.reader.ReplaceReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        state = QAInfo.State.QA_NEEDED,
        author = "te",
        comment = "JavaDoc needed especially on methods")
public class ReplaceFilter extends ObjectFilterImpl {
    /**
     * Logger.
     */
    private static Log log = LogFactory.getLog(ReplaceFilter.class);

    /**
     * If true, content in Records in Payloads is processed.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_PROCESS_CONTENT = "common.replacefilter.process.content";
    /**
     * Default value for {@link #CONF_PROCESS_CONTENT}.
     */
    public static final boolean DEFAULT_PROCESS_CONTENT = true;

    /**
     * If true, streams in Payloads are processed.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_PROCESS_STREAM = "common.replacefilter.process.stream";
    /**
     * Default value for {@link #CONF_PROCESS_STREAM}.
     */
    public static final boolean DEFAULT_PROCESS_STREAM = true;

    /**
     * The encoding for reading the source data.
     * </p><p>
     * Optional. Default is utf-8.
     */
    public static final String CONF_ENCODING_IN = "common.replacefilter.encoding.in";
    /**
     * Default value for {@link #CONF_ENCODING_IN}.
     */
    public static final String DEFAULT_ENCODING_IN = "utf-8";

    /**
     * The encoding for writing the source data.
     * </p><p>
     * Optional. Default is utf-8.
     */
    public static final String CONF_ENCODING_OUT = "common.replacefilter.encoding.out";
    /**
     * Default value for {@link #CONF_ENCODING_OUT}.
     */
    public static final String DEFAULT_ENCODING_OUT = "utf-8";

    /**
     * Used at the regexp for a pattern {@link Pattern} which will be used to
     * match substrings.
     * </p><p>
     * Important: Performing regexp-replacement can be slow and requires that
     * the full content of streams are loaded into memory.
     * Use of regexp-replacement for streams are discouraged.
     * If possible, use {@link #CONF_RULES} instead as it is faster and less
     * memory-intensive than regexp replacements.
     * </p><p>
     * The .-character matches any character, except newline. If newline should
     * be matched by the .-character, prefix the regexp with TODO with what?
     * Optional. Default is null (no regexp replacement).
     */
    public static final String CONF_PATTERN_REGEXP = "common.replacefilter.pattern.regexp";

    /**
     * Used as the replacement for matches on {@link #CONF_PATTERN_REGEXP}.
     * This uses the same rules as {@link Matcher#replaceAll(String)}.
     * </p><p>
     * Example: input "hello12world", regexp "o(.+)w", destination "$1"
     * result "hell12orld"
     * If {@link #CONF_PATTERN_REGEXP} is defined, CONF_PATTERN_REPLACEMENT must
     * also be defined.
     */
    public static final String CONF_PATTERN_REPLACEMENT = "common.replacefilter.pattern.destination";

    /**
     * String-to-string replacement, using the fast and memory-efficient replace
     * framework from SBUtil. This is the recommended way of specifying
     * replacements.
     * </p><p>
     * The CONF_RULES holds a list of sub-properties that each define a
     * single replacement.
     * </p><p>
     * Optional. Default is null (no rule-based replacement).
     *
     * @see #CONF_RULE_TARGET
     * @see #CONF_RULE_REPLACEMENT
     */
    public static final String CONF_RULES = "common.replacefilter.rules";

    /**
     * The target for a rule aka the String that is used verbatim for matching.
     * This must be defined in each of the sub-properties in the
     * {@link #CONF_RULES} list.
     */
    public static final String CONF_RULE_TARGET = "common.replacefilter.rule.target";

    /**
     * The replacement for a rule aka the String that is used verbatim instead
     * of the matched String. This must be defined in each of the sub-properties
     * in the {@link #CONF_RULES} list.
     */
    public static final String CONF_RULE_REPLACEMENT = "common.replacefilter.rule.replacement";

    private boolean processContent = DEFAULT_PROCESS_CONTENT;
    private boolean processStream = DEFAULT_PROCESS_STREAM;
    private String encodingIn = DEFAULT_ENCODING_IN;
    private String encodingOut = DEFAULT_ENCODING_OUT;

    private Pattern pattern = null;
    private String patternReplacement = null;
    private ReplaceFactory factory = null;
    private ReplaceReader basicReplacer;

    /**
     * Constructs a new replace filter, with the specified configuration.
     *
     * @param conf The configuration to construct this filter.
     */
    public ReplaceFilter(Configuration conf) {
        super(conf);
        feedback = false;
        processContent = conf.getBoolean(CONF_PROCESS_CONTENT, processContent);
        processStream = conf.getBoolean(CONF_PROCESS_STREAM, processStream);
        encodingIn = conf.getString(CONF_ENCODING_IN, encodingIn);
        encodingOut = conf.getString(CONF_ENCODING_OUT, encodingOut);
        if (conf.valueExists(CONF_PATTERN_REGEXP)) {
            if (!conf.valueExists(CONF_PATTERN_REPLACEMENT)) {
                throw new ConfigurationException(String.format(Locale.ROOT, "Property %s was defined but %s was not",
                                                               CONF_PATTERN_REGEXP, CONF_PATTERN_REPLACEMENT));
            }
            pattern = Pattern.compile(conf.getString(CONF_PATTERN_REGEXP));
            patternReplacement = conf.getString(CONF_PATTERN_REPLACEMENT);
            log.debug(String.format(Locale.ROOT, "Added pattern with regexp '%s' and replacement '%s'",
                                    pattern.pattern(), patternReplacement));
        }

        if (conf.valueExists(CONF_RULES)) {
            List<Configuration> ruleConfs;
            try {
                ruleConfs = conf.getSubConfigurations(CONF_RULES);
                log.debug(String.format(Locale.ROOT, "%d rules found", ruleConfs.size()));
            } catch (SubConfigurationsNotSupportedException e) {
                throw new ConfigurationException("Storage doesn't support sub configurations", e);
            } catch (NullPointerException e) {
                throw new ConfigurationException(String.format(Locale.ROOT, "IOException while extracting sub properties for %s",
                                                               CONF_RULES), e);
            }
            Map<String, String> rules = new LinkedHashMap<>(ruleConfs.size());
            int count = 0;
            log.debug(String.format(Locale.ROOT, "Located %d rules. Extracting...", ruleConfs.size()));
            for (Configuration ruleConf : ruleConfs) {
                if (!ruleConf.valueExists(CONF_RULE_TARGET) || !ruleConf.valueExists(CONF_RULE_REPLACEMENT)) {
                    throw new ConfigurationException(String.format(Locale.ROOT,
                            "For each sub-configuration in the list %s, a value for the key %s and the key %s must "
                            + "exist. Rule #%d (counting from 0) did not comply",
                            CONF_RULES, CONF_RULE_TARGET, CONF_RULE_REPLACEMENT, count));
                }
                if (log.isTraceEnabled()) {
                    log.trace(String.format(Locale.ROOT, "Adding rule with target '%s' and replacement '%s'",
                                            ruleConf.getString(CONF_RULE_TARGET),
                                            ruleConf.getString(CONF_RULE_REPLACEMENT)));
                }
                rules.put(ruleConf.getString(CONF_RULE_TARGET), ruleConf.getString(CONF_RULE_REPLACEMENT));
                count++;
            }
            factory = new ReplaceFactory(rules);
            basicReplacer = factory.getReplacer();
        }
        if (pattern == null && factory == null) {
            log.info("ReplaceFilter created without any rules. Payloads will pass through untouched");
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
                throw new PayloadException(String.format(Locale.ROOT,
                        "Unable to perform replacement on payload due to unsupported encoding. Encoding for read was "
                        + "'%s', encoding for store was '%s'",
                        encodingIn, encodingOut), e, payload);
            }
        }
        return true;
    }

    /**
     * Process the payload according to the pattern.
     *
     * @param payload The payload to process.
     * @throws PayloadException If error occur with the processing.
     */
    private void processPattern(Payload payload) throws PayloadException {
        final int bufferSize = 1000;
        if (payload.getStream() != null) {
            log.debug("Copying Stream content into memory in order to perform Pattern replacement");
            CircularCharBuffer cb = new CircularCharBuffer(bufferSize, Integer.MAX_VALUE);
            Reader in;
            try {
                in = new InputStreamReader(payload.getStream(), encodingIn);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "The encoding '%s' for the InputStream is not supported on this platform", encodingIn), e);
            }
            try {
                int c;
                while ((c = in.read()) != -1) {
                    cb.add((char) c);
                }
            } catch (IOException e) {
                throw new PayloadException("Unable to read character from Stream", e, payload);
            }
            String result = pattern.matcher(cb).replaceAll(patternReplacement);
            //noinspection UnusedAssignment
            cb = null; // Make the memory available ASAP
            byte[] resultBytes;
            try {
                resultBytes = result.getBytes(encodingOut);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "The encoding '%s' for output is not supported on this platform", encodingOut));
            }
            //noinspection UnusedAssignment
            result = null; // Make the memory available ASAP
            payload.setStream(new ByteArrayInputStream(resultBytes));
        }
        if (payload.getRecord() != null && payload.getRecord().getContent(false) != null) {
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess("ReplaceFilter '" + getName() + "'", "Performing pattern replace on Record content",
                               Logging.LogLevel.TRACE, payload);
            // We hack a bit to guess if content is compressed
            byte[] in = payload.getRecord().getContent();
            boolean compressed = in.length != payload.getRecord().getContent(false).length;
            String replaced;
            try {
                replaced = pattern.matcher(new String(in, encodingIn)).
                        replaceAll(patternReplacement);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Encoding '%s' not supported for reading content",
                                                                 encodingIn), e);
            }
            try {
                payload.getRecord().setContent(replaced.getBytes(encodingOut), compressed);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Encoding '%s' not supported for storing content",
                                                                 encodingOut), e);
            }
        }
    }

    /**
     * Factory process for a given payload.
     *
     * @param payload The payload.
     * @throws UnsupportedEncodingException If the encoding is unsupported.
     */
    private void processFactory(Payload payload) throws UnsupportedEncodingException {
        if (payload.getStream() != null) {
            log.debug("Wrapping stream in replacer");
            payload.setStream(new ReaderInputStream(
                    factory.getReplacer(new InputStreamReader(payload.getStream(), encodingIn)), encodingOut));
        }
        if (payload.getRecord() != null && payload.getRecord().getContent(false) != null) {
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess(getName(), "Performing replace on Record content", Logging.LogLevel.TRACE, payload);
            // We hack a bit to guess if content is compressed
            byte[] in = payload.getRecord().getContent();
            boolean compressed = in.length != payload.getRecord().getContent(false).length;
            String replaced = basicReplacer.transform(new String(in, encodingIn));
            payload.getRecord().setContent(replaced.getBytes(encodingOut), compressed);
        }
    }
}