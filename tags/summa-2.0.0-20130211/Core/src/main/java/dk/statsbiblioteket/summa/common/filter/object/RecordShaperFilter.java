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
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An {@link ObjectFilter} that assigns or modifies ID, base, and meta tags on
 * {@link Record}s embedded in the incoming {@link Payload}s. The changes are
 * done based on regular expressions.
 *
 * <h3>Template Syntax</h3>
 * The syntax of the ID-, base-, and meta templates used by this filter follows
 * that used by Java's
 * <a href="http://java.sun.com/javase/6/docs/api/java/util/regex/Matcher.html#replaceAll(java.lang.String)">Matcher.replaceAll(String)</a>
 * method.
 * <p/>
 * This means that the matches of captured groups are replaced in the template
 * string where ever a dollar sign ({@code $}) is encountered. To insert
 * the match of the {@code N}'th captured group use {@code $N}. The group
 * numbers start at 1, so to insert the first captured group use
 * {@code $1}. The {@code $0} symbol will insert the entire match.
 * This is also the default value for all templates.
 * <p/>
 * The escape character for the templates is backslash ({@code \}). To insert
 * dollar sign literals in your templates you must write {@code \$}.
 * </p><p>
 * Hint: See the list of embedded options for regexp matching at
 * {link http://java.sun.com/docs/books/tutorial/essential/regex/pattern.html}
 * Hint 2: When using multiline patterns, prepending ?s to the regexp
 * sets the pattern matcher in DOTALL-mode. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class RecordShaperFilter extends ObjectFilterImpl {
    private static final Log log = LogFactory.getLog(RecordShaperFilter.class);

    /**
     * If true, payloads are discarded when an assigned regexp for content, id
     * or base is not matched properly.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_DISCARD_ON_ERRORS = "record.discardonerrors";
    public static final boolean DEFAULT_DISCARD_ON_ERRORS = true;

    /**
     * If assigned, the result of this regexp, matched against the record
     * content, will be used with  {@link #CONF_CONTENT_TEMPLATE} to specify
     * the new content for the Record.
     */
    public static final String CONF_CONTENT_REGEXP = "record.content.regexp";
    /**
     * Template for inserting captured groups from {@link #CONF_CONTENT_REGEXP}.
     * Group numbering starts at 1 and the {@code N}'th captured group is
     * refered with the {@code $N} symbol.
     * To insert the whole match use {@code $0}
     * </p><p>
     * Optional. Default is $0 (a direct copy of the match).
     */
    public static final String CONF_CONTENT_TEMPLATE = "record.content.template";
    public static final String DEFAULT_CONTENT_TEMPLATE = "$0";

    /**
     * If assigned, the result of this regexp, matched against the record
     * content, will be used with  {@link #CONF_ID_TEMPLATE} to specify
     * the new ID for the Record.
     */
    public static final String CONF_ID_REGEXP = "record.id.regexp";
    /**
     * Template for inserting captured groups from {@link #CONF_ID_REGEXP}.
     * Group numbering starts at 1 and the {@code N}'th captured group is
     * refered with the {@code $N} symbol.
     * To insert the whole match use {@code $0}.
     * </p><p>
     * Optional. Default is $0 (a direct copy of the match).
     */
    public static final String CONF_ID_TEMPLATE =
            "record.id.template";
    public static final String DEFAULT_ID_TEMPLATE = "$0";

    /**
     * If defined, the ID is hashed with the given function. This takes place
     * after the optional ID regexp.
     * </p><p>
     * Optional. Valid values are undefined or md5.
     */
    public static final String CONF_ID_HASH = "record.id.hash";

    /**
     * Prefixes hashed IDs.
     * </p><p>
     * Optional.
     */
    public static final String CONF_ID_HASH_PREFIX = "record.id.hash.prefix";

    /**
     * The minimum length of the existing ID before hashing of the ID is
     * activated. This requires {@link #CONF_ID_HASH} to be defined.
     * </p><p>
     * Optional. Default is 0 (all IDs are hashed).
     */
    public static final String CONF_ID_HASH_MINLENGTH = "record.id.hash.minlength";
    public static final int DEFAULT_ID_HASH_MINLENGTH = 0;

    /**
     * If assigned, the result of this regexp, matched against the record
     * content, will be used with  {@link #CONF_BASE_TEMPLATE} to specify
     * the new base for the Record.
     */
    public static final String CONF_BASE_REGEXP = "record.base.regexp";
    /**
     * Template for inserting captured groups from {@link #CONF_BASE_REGEXP}.
     * Group numbering starts at 1 and the {@code N}'th captured group is
     * refered with the {@code $N} symbol.
     * To insert the whole match use {@code $0}.
     * </p><p>
     * Optional. Default is $0 (a direct copy of the match).
     */
    public static final String CONF_BASE_TEMPLATE = "record.base.template";
    public static final String DEFAULT_BASE_TEMPLATE = "$0";

    /**
     * If {@link #CONF_META} is specified, the requirement can be optionally
     * specified. Valid values are all, none or one.
     * </p><p>
     * Important: The legacy {@link #CONF_DISCARD_ON_ERRORS} must be true for
     * this setting to have effect.
     * </p><p>
     * Optional. Default is all.
     */
    public static final String CONF_META_REQUIREMENT =
        "record.meta.requirement";
    public static final String DEFAULT_META_REQUIREMENT =
        REQUIREMENT.all.toString();

    public static enum REQUIREMENT {all, none, one}


    /**
     * A list of sub-configurations specifying regexps for the extraction of
     * snippets into meta-data. The regular expression will be matched against
     * the record contents. See all CONT_META_*-properties below.
     */
    public static final String CONF_META = "record.meta";

    /**
     * The destination to assign the result of the regexp to. This follows the
     * same rules as {@link #CONF_META_SOURCE} except that fallback is to use
     * the stated value directly to lookup in record.meta.
     * </p><p>
     * See {@link RecordUtil#setString(Record, String, String)} for details.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_META_KEY = "record.meta.key";

    /**
     * Where tot ake the source for the regexp from. Valid values are
     * "id", "base", "content" and "meta.metakey". If "meta.metakey" is
     * specified, the "meta."-part will be stripped and the metakey-part will
     * be used as key for getting the meta-value.
     * </p><p>
     * See {@link RecordUtil#getString(Record, String)} for details.
     * </p><p>
     * Optional. Default is "content".
     */
    public static final String CONF_META_SOURCE = "record.meta.source";
    public static final String DEFAULT_META_SOURCE = "content";

    /**
     * The maximum number of characters to fetch from the source. For large
     * amounts of text, regexps might perform poorly. If the relevant data are
     * known to be in the beginning of the text, limiting the amount of
     * characters to match against can yield a considerable increase in speed.
     * </p><p>
     * Note: For small amounts of text, the extra logistics will slow down
     * processing a little bit, compared to not specifying a limit.
     * </p><p>
     * Warning: The states limit is used as the initial size of an internal
     * buffer. Specifying a very large number will have a serious impact on
     * memory allocation and subsequent garbage collection.
     * </p><p>
     * Optional. Default is 0 (unlimited).
     */
    public static final String CONF_META_LIMIT = "record.meta.limit";
    public static final int DEFAULT_META_LIMIT = 0;

    /**
     * This regexp will be used with {@link #CONF_META_TEMPLATE} to
     * specify the value for the {@link #CONF_META_KEY}.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_META_REGEXP = "record.meta.regexp";

    /**
     * Template for inserting captured groups from {@link #CONF_META_REGEXP}
     * into a record meta tag.
     * Group numbering starts at 1 and the {@code N}'th captured group is
     * refered with the {@code $N} symbol.
     * To insert the whole match use {@code $0}.
     * </p><p>
     * Optional. Default is $0 (a direct copy of the match).
     */
    public static final String CONF_META_TEMPLATE = "record.meta.template";
    public static final String DEFAULT_META_TEMPLATE = "$0";

    /**
     * If true. String key/value pairs from Payload meta data are copied to
     * Record meta data. This copying takes place before other manipulations.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_COPY_META = "record.meta.copy";
    public static final boolean DEFAULT_COPY_META = false;

    /* regexp -> template */
    private Pair<Pattern, String> assignId;
    private Pair<Pattern, String> assignBase;
    private Pair<Pattern, String> assignContent;
    private boolean discardOnErrors = DEFAULT_DISCARD_ON_ERRORS;
    private String idHash = null;
    private String idHashPrefix = "";
    private int idHashMinLength = DEFAULT_ID_HASH_MINLENGTH;
    private boolean copyMeta = DEFAULT_COPY_META;
    private final REQUIREMENT metaRequirement;

    private List<Shaper> metas = new ArrayList<Shaper>();

    public RecordShaperFilter(Configuration conf) {
        super(conf);
        discardOnErrors = conf.getBoolean(
                CONF_DISCARD_ON_ERRORS, discardOnErrors);
        copyMeta = conf.getBoolean(CONF_COPY_META, copyMeta);
        if (conf.valueExists(CONF_CONTENT_REGEXP)) {
            assignContent = new Pair<Pattern, String>(
                    Pattern.compile(conf.getString(CONF_CONTENT_REGEXP)),
                    conf.getString(CONF_CONTENT_TEMPLATE, DEFAULT_CONTENT_TEMPLATE));
        }
        if (conf.valueExists(CONF_ID_REGEXP)) {
            assignId = new Pair<Pattern, String>(
                    Pattern.compile(conf.getString(CONF_ID_REGEXP)),
                    conf.getString(CONF_ID_TEMPLATE, DEFAULT_ID_TEMPLATE));
        }
        if (conf.valueExists(CONF_BASE_REGEXP)) {
            assignBase = new Pair<Pattern, String>(
                    Pattern.compile(conf.getString(CONF_BASE_REGEXP)),
                    conf.getString(CONF_BASE_TEMPLATE, DEFAULT_BASE_TEMPLATE));
        }
        parseMetas(conf);
        idHash = conf.getString(CONF_ID_HASH, idHash);
        if ("".equals(idHash)) {
            idHash = null;
        }
        if (idHash != null && !"md5".equals(idHash)) {
            idHash = null;
            log.warn("ID hash '" + idHash + "' function not supported");
        }
        idHashPrefix = conf.getString(CONF_ID_HASH_PREFIX, idHashPrefix);
        idHashMinLength = conf.getInt(CONF_ID_HASH_MINLENGTH, idHashMinLength);
        metaRequirement = REQUIREMENT.valueOf(conf.getString(CONF_META_REQUIREMENT, DEFAULT_META_REQUIREMENT));
        if (metaRequirement == null) {
            throw new ConfigurationException(
                "Unable to resolve " + CONF_META_REQUIREMENT + " value '" + conf.getString(CONF_META_REQUIREMENT)
                + "' to any known requirement. Valid values are all, none and one");
        }
        if (metaRequirement == RecordShaperFilter.REQUIREMENT.one && metas.isEmpty()) {
            throw new ConfigurationException(
                "There were 0 metas specified with a requirement of 1 in order for a Payload to pass. Set requirements "
                + "to something else or add at least 1 meta");
        }
        log.info(String.format("Created an assign meta filter with %d meta extractions", metas.size()));
    }

    private void parseMetas(Configuration conf) {
        if (!conf.valueExists(CONF_META)) {
            return;
        }
        List<Configuration> confs;
        try {
            confs = conf.getSubConfigurations(CONF_META);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException("Storage must support sub configurations", e);
        } catch (NullPointerException e) {
            throw new ConfigurationException(String.format(
                "Unable to extract sub configurations with key %s from configuration", CONF_META), e);
        }
        for (Configuration metaConf: confs) {
            metas.add(new Shaper(metaConf));
        }
    }

    private class Shaper {
        private final String  source;
        private final String  destination;
        private final Pattern regexp;
        private final String  template;
        private final int limit;

        private Shaper(Configuration conf) {
            if (!conf.valueExists(CONF_META_KEY)) {
                throw new ConfigurationException(String.format(
                    "No value for mandatory key '%s' present", CONF_META_KEY));
            }
            if (!conf.valueExists(CONF_META_REGEXP)) {
                throw new ConfigurationException(String.format(
                    "No value for mandatory key '%s' present",
                    CONF_META_REGEXP));
            }

            source = conf.getString(CONF_META_SOURCE, DEFAULT_META_SOURCE);
            destination = conf.getString(CONF_META_KEY);
            try {
                regexp = Pattern.compile(conf.getString(CONF_META_REGEXP));
            } catch (PatternSyntaxException e) {
                throw new ConfigurationException(String.format(
                    "Unable to parse pattern '%s'",
                    conf.getString(CONF_META_REGEXP)), e);
            }
            template = conf.getString(CONF_META_TEMPLATE, DEFAULT_META_TEMPLATE);
            limit = conf.getInt(CONF_META_LIMIT, DEFAULT_META_LIMIT);
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                    "Shaper created with source='%s', destination='%s', limit=%d, regexp='%s', template='%s'",
                    source, destination, limit, regexp.pattern(), template));
            }
        }

        @Override
        public String toString() {
            return String.format(
                    "Shaper(source='%s', destination='%s', limit=%d, regexp='%s', template='%s')",
                    source, destination, limit, regexp.pattern(), template);
        }

        public void shape(Payload payload) throws PayloadException {
            final Record record = payload.getRecord();
            String sourceString;
            if (limit == 0) {
                sourceString = RecordUtil.getString(record, source);
            } else {
                log.trace("Extracting with limit " + limit + " from " + record.getId() + " with source " + source);
                sourceString = RecordUtil.getString(record, source, limit);
            }
            if (sourceString == null) {
                String message = String.format("Unable to get String from source '%s'", source);
                if (discardOnErrors) {
                    throw new PayloadException(message, payload);
                }
                Logging.logProcess(getName(), message, Logging.LogLevel.DEBUG, payload);
                return;
            }

            Matcher matcher = regexp.matcher(sourceString);
            if (!matcher.find()) {
                String message = String.format("Unable to match String with Pattern '%s'", regexp.pattern());
                if (discardOnErrors) {
                    throw new PayloadException(message, payload);
                }
                Logging.logProcess(getName(), message, Logging.LogLevel.TRACE, payload);
                return;
            }
            buffer.setLength(0);
            int matchPos = matcher.start();
            matcher.appendReplacement(buffer, template);
            String newText = buffer.toString().substring(matchPos);
            if (newText == null || "".equals(newText)) {
                String message = String.format(
                        "Match found with Pattern '%s' but no text was extracted using template '%s'",
                        regexp.pattern(), template);
                if (discardOnErrors) {
                    throw new PayloadException(message, payload);
                }
                Logging.logProcess(getName(), message, Logging.LogLevel.DEBUG, payload);
                return;
            }
            if (log.isTraceEnabled()) {
                log.debug(String.format(
                    "Transformed %s to %s stored in %s for %s",
                    source, newText, destination, payload));
            }

            RecordUtil.setString(payload.getRecord(), newText, destination);
        }
    }

    private StringBuffer buffer = new StringBuffer(50);

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        String content = payload.getRecord().getContentAsUTF8();
        if (copyMeta && payload.getRecord() != null && payload.hasData()) {
            for (Map.Entry entry: payload.getData().entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    payload.getRecord().getMeta().put((String)entry.getKey(), (String)entry.getValue());
                }
            }
        }
        if (assignId != null) {
            String newId = getMatch(assignId, content, payload, "ID");
            if (newId != null && !"".equals(newId)) {
                payload.setID(newId);
            }
        }
                
        if (assignBase != null) {
            String newBase = getMatch(assignBase, content, payload, "base");
            if (newBase != null && !"".equals(newBase)) {
                payload.getRecord().setBase(newBase);
            }
        }
        if (assignContent != null) {
            //noinspection DuplicateStringLiteralInspection
            String newContent = getMatch(assignContent, content, payload, "content");
            if (newContent != null && !"".equals(newContent)) {
                try {
                    payload.getRecord().setContent(newContent.getBytes("utf-8"), false);
                } catch (UnsupportedEncodingException e) {
                    throw new PayloadException("Exception while converting content to UTF-8 bytes", e);
                }
            }
        }
        int count = 0;
        PayloadException lastException = null;
        for (Shaper meta: metas) {
            try {
                meta.shape(payload);
                count++;
            } catch (PayloadException e) {
                if (metaRequirement == RecordShaperFilter.REQUIREMENT.all) {
                    throw new PayloadException(
                        "The requirement of passed metas was 'all'. Payload did not pass '" + meta
                        + "' and will be discarded", e, payload);
                }
                lastException = e;
            }
        }
        if (count != metas.size()) {
            if (metaRequirement == REQUIREMENT.one && count == 0) {
                throw new PayloadException("The requirement of passed metas was 'one'. Payload did not pass any metas",
                                           lastException, payload);
            }
            Logging.logProcess(getName(), "Payload passed " + count + "/" + metas.size() + " metas",
                               Logging.LogLevel.TRACE, payload);
        } else {
            Logging.logProcess(getName(), "Payload passed all " + metas.size() + " metas",
                               Logging.LogLevel.TRACE, payload);
        }
        if (idHash != null && payload.getId().length() > idHashMinLength) {
            String oldID = payload.getId();
            if ("md5".equals(idHash)) {
                payload.setID(idHashPrefix + md5sum(oldID));
            } else {
                Logging.logProcess(getName(), "ID hash function '" + idHash + "' not supported",
                                   Logging.LogLevel.WARN, payload);
            }
            log.trace("Hashed id '" + oldID + "' to " + payload.getId());
        }
        return true;
    }

    static MessageDigest md = null;
    private static String md5sum (String text) {
        try {
            if (md == null) {
                md = MessageDigest.getInstance("MD5");                
            }
            md.reset();
            md.update(text.getBytes("UTF-8"));
            return new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(),e);
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(),e);
        }
        return text;
    }

    protected String getMatch(
            Pair<Pattern, String> assignment, String input,
            Payload payload, String type) throws PayloadException {
        Matcher matcher = assignment.getKey().matcher(input);
        if (!matcher.find()) {
            String message = String.format(
                    "Unable to match new %s with Pattern '%s'",
                    type, assignment.getKey());
            if (discardOnErrors) {
                throw new PayloadException(message, payload);
            }
            log.warn(message + " for " + payload);
            return null;
        }
        buffer.setLength(0);
        int matchPos = matcher.start();
        try {
            matcher.appendReplacement(buffer, assignment.getValue());
        } catch (IndexOutOfBoundsException e) {
            throw new PayloadException(
                getName() + " IndexOutOfBounds while processing " + payload + " with regexp '"
                + assignment.getKey().pattern() + "' and destination '" + assignment.getValue(), e, payload);
        }
        String newText = buffer.toString().substring(matchPos);
        if (newText == null || "".equals(newText)) {
            String message = String.format(
                    getName() + "Match found for %s with Pattern '%s' but no text was extracted using template '%s'",
                    type, assignment.getKey(), assignment.getValue());
            if (discardOnErrors) {
                throw new PayloadException(message, payload);
            }
            log.warn(message + " for " + payload);
            return null;
        }
        if (log.isTraceEnabled()) {
            log.debug(String.format("Found new '%s' '%s' for %s", type, newText, payload));
        }
        return newText;
    }

    private static class Pair <T , S> {
        protected T key;
        protected S value;

        public Pair(T key, S value) {
            this.key = key;
            this.value = value;
        }
        public T getKey() {
            return key;
        }
        public S getValue() {
            return value;
        }
    }
}
