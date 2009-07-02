package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An {@link ObjectFilter} that assigns IDs, base and meta-data to the record
 * based on regular expressions.
 */
public class AssignMetaFilter extends ObjectFilterImpl {
    private static final Log log = LogFactory.getLog(AssignMetaFilter.class);

    /**
     * If true, payloads are discarded when an assigned regexp for content, id
     * or base is not matched properly.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_DISCARD_ON_ERRORS =
            "assignmeta.discardonerrors";
    public static final boolean DEFAULT_DISCARD_ON_ERRORS = true;

    /**
     * If assigned, the result of this regexp will be used with 
     * {@link #CONF_CONTENT_TEMPLATE} to specify the new content for the Record.
     */
    public static final String CONF_CONTENT_REGEXP = "assignmeta.content.regexp";
    /**
     * Uses the match from {@link #CONF_CONTENT_REGEXP} to format the new content.
     * This uses the syntax from {@link String#replaceAll(String, String)}.
     * </p><p>
     * Optional. Default is $0 (a direct copy of the match).
     */
    public static final String CONF_CONTENT_TEMPLATE =
            "assignmeta.content.template";
    public static final String DEFAULT_CONTENT_TEMPLATE = "$0";

    /**
     * If assigned, the result of this regexp will be used with 
     * {@link #CONF_ID_TEMPLATE} to specify the new ID for the Record.
     */
    public static final String CONF_ID_REGEXP = "assignmeta.id.regexp";
    /**
     * Uses the match from {@link #CONF_ID_REGEXP} to format the new ID.
     * This uses the syntax from {@link String#replaceAll(String, String)}.
     * </p><p>
     * Optional. Default is $0 (a direct copy of the match).
     */
    public static final String CONF_ID_TEMPLATE =
            "assignmeta.id.template";
    public static final String DEFAULT_ID_TEMPLATE = "$0";

    /**
     * If assigned, the result of this regexp will be used with 
     * {@link #CONF_BASE_TEMPLATE} to specify the new base for the Record.
     */
    public static final String CONF_BASE_REGEXP = "assignmeta.base.regexp";
    /**
     * Uses the match from {@link #CONF_BASE_REGEXP} to format the new base.
     * This uses the syntax from {@link String#replaceAll(String, String)}.
     * </p><p>
     * Optional. Default is $0 (a direct copy of the match).
     */
    public static final String CONF_BASE_TEMPLATE =
            "assignmeta.base.template";
    public static final String DEFAULT_BASE_TEMPLATE = "$0";

    /**
     * A list of sub-configurations specifying regexps for the extraction of
     * snippets into meta-data. See all CONT_META_*-properties below.
     */
    public static final String CONF_METAS = "assignmeta.metas";

    /**
     * The meta-key to assign.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_META_KEY = "assignmeta.meta.key";

    /**
     * This regexp will be used with {@link #CONF_META_TEMPLATE} to
     * specify the value for the {@link #CONF_META_KEY}.
     */
    public static final String CONF_META_REGEXP = "assignmeta.meta.regexp";

    /**
     * Uses the match from {@link #CONF_META_REGEXP} to format the new base.
     * This uses the syntax from {@link String#replaceAll(String, String)}.
     * </p><p>
     * Optional. Default is $0 (a direct copy of the match).
     */
    public static final String CONF_META_TEMPLATE =
            "assignmeta.meta.template";
    public static final String DEFAULT_META_TEMPLATE = "$0";

    /* regexp -> template */
    private Pair<Pattern, String> assignId;
    private Pair<Pattern, String> assignBase;
    private Pair<Pattern, String> assignContent;
    private boolean discardOnErrors = DEFAULT_DISCARD_ON_ERRORS;

    /* (key, (regexp, template))* */
    private List<Pair<String, Pair<Pattern, String>>> assignMetas =
            new ArrayList<Pair<String, Pair<Pattern, String>>>(10);

    public AssignMetaFilter(Configuration conf) {
        super(conf);
        discardOnErrors = conf.getBoolean(
                CONF_DISCARD_ON_ERRORS, discardOnErrors);
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
        if (conf.valueExists(CONF_METAS)) {
            List<Configuration> confs;
            try {
                confs = conf.getSubConfigurations(CONF_METAS);
            } catch (IOException e) {
                throw new ConfigurationException(String.format(
                        "Unable to extract sub configurations with key %s from"
                        + " configuration", CONF_METAS), e);
            }
            for (Configuration metaConf: confs) {
                assignMetas.add(new Pair<String, Pair<Pattern, String>>(
                        metaConf.getString(CONF_META_KEY),
                        new Pair<Pattern, String>(
                                Pattern.compile(
                                        metaConf.getString(CONF_META_REGEXP)),
                                metaConf.getString(CONF_META_TEMPLATE,
                                                   DEFAULT_META_TEMPLATE))));
            }
        }
        log.info(String.format(
                "Created an assign meta filter with %d meta extractions",
                assignMetas.size()));
    }

    private StringBuffer buffer = new StringBuffer(50);

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        String content = payload.getRecord().getContentAsUTF8();
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
            String newContent = getMatch(
                    assignContent, content, payload, "content");
            if (newContent != null && !"".equals(newContent)) {
                try {
                    payload.getRecord().setContent(
                        newContent.getBytes("utf-8"), false);
                } catch (UnsupportedEncodingException e) {
                    throw new PayloadException(
                            "Exception while conterting content to UTF-8 bytes",
                            e);
                }
            }
        }
        for (Pair<String, Pair<Pattern, String>> assignMeta: assignMetas) {
            String result = getMatch(assignMeta.getValue(), content, payload,
                                     assignMeta.getKey());
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "Got result '%s' for key '%s' with pattern '%s' for %s",
                        result, assignMeta.getKey(),
                        assignMeta.getValue().getKey().pattern(), payload));
            }
            if (result == null || "".equals(result)) {
                continue;
            }
            payload.getRecord().getMeta().put(assignMeta.getKey(), result);
        }
        return true;
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
        matcher.appendReplacement(buffer, assignment.getValue());
        String newText = buffer.toString().substring(matchPos);
        if (newText == null || "".equals(newText)) {
            String message = String.format(
                    "Match found for %s with Pattern '%s' but no text was "
                    + "extracted using template '%s'",
                    type, assignment.getKey(), assignment.getValue());
            if (discardOnErrors) {
                throw new PayloadException(message, payload);
            }
            log.warn(message + " for " + payload);
            return null;
        }
        if (log.isTraceEnabled()) {
            log.debug(String.format("Found new '%s' '%s' for %s",
                                    type, newText, payload));
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