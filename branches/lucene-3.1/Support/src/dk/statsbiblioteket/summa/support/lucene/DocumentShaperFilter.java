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
package dk.statsbiblioteket.summa.support.lucene;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import dk.statsbiblioteket.summa.index.lucene.DocumentCreatorBase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.util.SimpleTriple;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.document.Document;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.Serializable;

/**
 * Extends embedded Lucene documents with transient or persistent meta-data
 * from Payload and Record. For each specified pattern, Payload is queried.
 * If Payload does not return anything, Record is tried. If that doesn't give
 * any result, the pattern is ignored for the current Document.
 * Each result is assign to the corresponding field determined from templates.
 * </p><p>
 * Note 1: The DocumentCreator needs an index-description. The setup for
 * retrieving the description must be stored in the sub-property
 * {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor#CONF_DESCRIPTOR}
 * with parameters from
 * {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor}.
 * </p><p>
 * Note 2: Boosts are not part of the syntax for this filter.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocumentShaperFilter extends DocumentCreatorBase {
    private static Log log = LogFactory.getLog(DocumentShaperFilter.class);

    /**
     * A list of regular expressions for the keys to the values to extend the
     * documents with.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_PATTERNS = "document.patterns";

    /**
     * A list of templates for inserting captured groups from the
     * {@link #CONF_PATTERNS} regexps.
     * Group numbering starts at 1 and the {@code N}'th captured group is
     * refered with the {@code $N} symbol.
     * To insert the whole match use {@code $0}
     * </p><p>
     * The length of the list must either be 0 or the same af the length of the
     * CONF_KEYS-list.
     * </p><p>
     * Optional. If the length of the list is 0, $0 (a direct copy of the match)
     *           will be used for each regexp.
     */
    public static final String CONF_FIELD_TEMPLATES = "document.field.templates";
    public static final String DEFAULT_FIELD_TEMPLATE = "$0";

    /**
     * Expert only.
     * </p><p>
     * A list of contents corresponding to the regular expressions and
     * templates. The following strings will be replaced:<br />
     * ${content} the value from the meta data with the key matching regexp.
     * ${key}     the meta data key.
     * ${field}   the name of the field derived from the field template.
     * </p><p>
     * Optional. If the length of the list is 0, ${content} (a direct copy of
     *           the content) will be used for each regexp.
     */
    public static final String CONF_FIELD_CONTENTS = "document.field.contents";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String DEFAULT_FIELD_CONTENT = "${content}";

    // Pattern, template, content
    // Optimized so content == null -> direct copy
    private List<SimpleTriple<Pattern, String, String>> keys;
    private LuceneIndexDescriptor descriptor;

    public DocumentShaperFilter (Configuration conf) throws
                                                        ConfigurationException {
        super(conf);
        descriptor = LuceneIndexUtils.getDescriptor(conf);
        List<String> patterns = conf.getStrings(CONF_PATTERNS);
        List<String> templates = conf.valueExists(CONF_FIELD_TEMPLATES) ?
                                 conf.getStrings(CONF_FIELD_TEMPLATES) :
                                 new ArrayList<String>(0);
        List<String> contents = conf.valueExists(CONF_FIELD_CONTENTS) ?
                                 conf.getStrings(CONF_FIELD_CONTENTS) :
                                 new ArrayList<String>(0);
        if (templates.size() != patterns.size()) {
            log.debug(String.format(
                    "Creating default templates as the length of the "
                    + "patterns-list was %d and the length of the "
                    + "templates-list was %d",
                    patterns.size(), templates.size()));
            templates = new ArrayList<String>(patterns.size());
            //noinspection UnusedDeclaration
            for (String pattern : patterns) {
                templates.add(DEFAULT_FIELD_TEMPLATE);
            }
        }
        if (contents.size() != patterns.size()) {
            log.debug(String.format(
                    "Creating default contents as the length of the"
                    + " patterns-list was %d and the length of the "
                    + "contents-list was %d",
                    patterns.size(), contents.size()));
            contents = new ArrayList<String>(patterns.size());
            //noinspection UnusedDeclaration
            for (String pattern : patterns) {
                contents.add(DEFAULT_FIELD_CONTENT);
            }
        }
        keys = new ArrayList<SimpleTriple<
                Pattern, String, String>>(patterns.size());
        for (int i = 0 ; i < patterns.size() ; i++) {
            keys.add(new SimpleTriple<Pattern, String, String>(
                    Pattern.compile(patterns.get(i)), templates.get(i),
                    DEFAULT_FIELD_CONTENT.equals(contents.get(i)) ?
                    null : contents.get(i)));
        }
        log.debug(String.format("Created filter with %d keys", keys.size()));
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        Object docO = payload.getData(Payload.LUCENE_DOCUMENT);
        if (docO == null) {
            log.warn(String.format("No Lucene Document for %s", payload));
            return false;
        }
        Document lucenedoc = (Document)docO;
        for (SimpleTriple<Pattern, String, String> keyPair: keys) {
            for (Map.Entry<String, Serializable> entry:
                    payload.getData().entrySet()) {
                assign(payload, lucenedoc, keyPair, entry.getKey(),
                       entry.getValue());
            }

            if (payload.getRecord() != null &&
                payload.getRecord().getMeta() != null) {
                for (Map.Entry<String, String> entry:
                        payload.getRecord().getMeta().entrySet()) {
                    assign(payload, lucenedoc, keyPair, entry.getKey(),
                           entry.getValue());
                }
            }
        }
        return true;
    }

    // False if not assigned
    private boolean assign(Payload payload, Document document,
                           SimpleTriple<Pattern, String, String> key,
                           String metaKey, Object content)
                                                       throws PayloadException {
        if (content == null) {
            return false;
        }
        if (log.isTraceEnabled()) {
            log.trace(String.format(
                    "assign(%s, ..., (%s, %s, %s), %s, %s) called",
                    payload, key.getKey(), key.getValue1(), key.getValue2(),
                    metaKey, content.toString()));
        }
        String fieldName = getFieldName(key.getKey(), key.getValue1(), metaKey);
        if (fieldName == null) {
            return false;
        }
        if (log.isTraceEnabled()) {
            log.trace(String.format(
                    "Assigning field '%s' with content '%s' to %s with "
                    + "content-template '%s'",
                    fieldName, content, payload,
                    key.getValue2() == null ? DEFAULT_FIELD_CONTENT :
                    key.getValue2()));
        }
        String c = content.toString();
        if (c == null) {
            log.debug(String.format(
                    "Null from content.toString() in assign for field '%s' to " 
                    + "%s", fieldName, payload));
            return false;
        }
        if ("".equals(c)) {
            log.trace("Empty content");
            return false;
        }
        if (key.getValue2() != null) {
            //noinspection DuplicateStringLiteralInspection
            // TODO: Do we always want to encode here?
            c = key.getValue2().replace("${content}", XMLUtil.encode(c)).
                    replace("${key}", metaKey).
                    replace("${field}", fieldName);
            if (log.isTraceEnabled()) {
                log.trace("Produced new content for field "
                          + fieldName + ": " + c);
            }
        }
        try {
            addFieldToDocument(descriptor, document, fieldName, c, 1.0F);
        } catch (IndexServiceException e) {
            throw new PayloadException(String.format(
                    "Unable to add field '%s' with content '%s' to document", 
                    fieldName, content.toString()), e, payload);
        }
        return true;
    }

    private StringBuffer buffer = new StringBuffer(50);
    /**
     * If the key matches the pattern, template is used to extract the fieldName
     * which is then returned. Else null.
     * @param pattern  used with the key.
     * @param template used for generating the result.
     * @param key      tested against pattern.
     * @return the result of the template or null if there is no match.
     */
    private String getFieldName(Pattern pattern, String template, String key) {
        Matcher matcher = pattern.matcher(key);
        if (!matcher.find()) {
            return null;
        }
        buffer.setLength(0);
        int matchPos = matcher.start();
        matcher.appendReplacement(buffer, template);
        String newText = buffer.toString().substring(matchPos);
        if (newText == null || "".equals(newText)) {
            log.warn(String.format(
                    "'%s' matched '%s' but the template '%s' did not give any "
                    + "result", pattern.pattern(), template, key));
            return null;
        }
        return newText;
    }
}

