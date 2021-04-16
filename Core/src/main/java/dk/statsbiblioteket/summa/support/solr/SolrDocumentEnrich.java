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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for {@link SolrManipulator} that optionally injects Record meta data into SolrDocumentXML.
 * </p><p>
 * This helper uses simple String manipulation instead of solid XML parse/write for speed. No actual
 * measurements had been performed to verify if this alleged speed-up is substantial or not.
 * </p><p>
 * This class is not thread safe inside of the same instantiation. Using multiple instantiations in separate threads
 * will work.
 */
public class SolrDocumentEnrich implements SolrDocumentAdjustFilter.Adjuster {
    private static Log log = LogFactory.getLog(SolrDocumentEnrich.class);

    public enum ELEMENTS {
        recordID,
        recordBase,
        /** Creation time */
        ctime,
        /** Last modified time */
        mtime,
        /** Index-time (now) */
        itime}

    /**
     * The elements to add. Note that there is no check for existing elements, which might lead to double entries.
     * </p><p>
     * Optional. Default is the empty list.
     */
    public static final String CONF_ELEMENTS = "solrdocumentenrich.elements";
    public static final List<ELEMENTS> DEFAULT_ELEMENTS = Collections.emptyList();

    /**
     * Data entries are retrieved by calling {@link Payload#getData()}.
     * The format for the property is a list of {@code key:field} where {@code key} is used to get the entry by
     * calling {@link Payload#getData()} and {@code field} is the Solr field to receive the content.
     * If the data is a {@link Collection}, it is iterated and each entry is added as a separate {@code field}-entry
     * in the SolrDocumentXML. If it is anything else, the result of {@link Object#toString()} is added.
     * </p><p>
     * Optional. Default is the empty list.
     */
    public static final String CONF_DATA_ENTRIES = "solrdocumentenrich.dataentries";
    public static final List<String> DEFAULT_DATA_ENTRIES = Collections.emptyList();

    /**
     * Convenience list with ctime, mtime and itime.
     */
    public static final List<ELEMENTS> TIME_ELEMENTS = Collections.unmodifiableList(Arrays.asList(
            ELEMENTS.ctime, ELEMENTS.mtime, ELEMENTS.itime));

    private final Matcher docMatcher = Pattern.compile("<doc[^>]*>").matcher(""); // FIXME: Matches <docs...>
    private final SimpleDateFormat solrTime = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final List<ELEMENTS> elements;
    private final String[][] dataEntries;
    private long processedCount = 0;

    public SolrDocumentEnrich(Configuration conf) {
        if (conf.containsKey(CONF_ELEMENTS)) {
            elements = new ArrayList<>();
            for (String e: conf.getStrings(CONF_ELEMENTS)) {
                elements.add(ELEMENTS.valueOf(e));
            }
        } else {
            elements = DEFAULT_ELEMENTS;
        }
        if (conf.containsKey(CONF_DATA_ENTRIES)) {
            List<String> es = conf.getStrings(CONF_DATA_ENTRIES);
            dataEntries = new String[es.size()][];
            for (int i = 0; i < es.size(); i++) {
                String e = es.get(i);
                dataEntries[i] = e.split(":");
                if (dataEntries[i].length != 2) {
                    throw new Configurable.ConfigurationException(
                            "The entry '" + e + "' from the property " + CONF_DATA_ENTRIES +
                            " was not in the key:field format");
                }
            }
        } else {
            dataEntries = new String[0][0];
        }
    }

    @Override
    public boolean adjust(Payload payload) {
        if (payload.getRecord() == null) {
            return false;
        }
        final Record record = payload.getRecord();

        if (elements.isEmpty() && dataEntries.length == 0) {
            return false;
        }
        final String oldContent = RecordUtil.getString(record, RecordUtil.PART.content);
        if (oldContent == null) {
            return false;
        }

        docMatcher.reset(oldContent);
        if (!docMatcher.find()) {
            log.warn("Unable to enrich " + record.getId() + " as '<doc...>' could not be located in content");
            return false;
        }
        int contentStart = docMatcher.end();
        enriched.setLength(0);
        enriched.append(oldContent.substring(0, contentStart));
        enriched.append("\n");
        addElements(record);
        addDataEntries(payload);
        enriched.append(oldContent.substring(contentStart));
        record.setContent(enriched.toString().getBytes(StandardCharsets.UTF_8), false);
        return true;
    }

    private void addDataEntries(Payload payload) {
        for (String[] entry: dataEntries) {
            String key = entry[0];
            String field = entry[1];
            Object value = payload.getData(key);
            if (value == null) {
                Logging.logProcess("SolrDocumentEntrich",
                                   "No Payload data for '" + key + "'",
                                   Logging.LogLevel.DEBUG, payload);
                continue;
            }
            if (value instanceof Collection) {
                Collection col = (Collection)value;
                for (Object o: col) {
                    append(enriched, field, XMLUtil.encode(o.toString()));
                }
                Logging.logProcess("SolrDocumentEntrich",
                                   "Added " + col.size() + " Strings from Payload data '" + key + "'",
                                   Logging.LogLevel.DEBUG, payload);
                continue;
            }
            String single = value.toString();
            append(enriched, field, XMLUtil.encode(single));
            Logging.logProcess("SolrDocumentEntrich",
                               "Added single String of length " + single.length() + " from Payload data '" + key + "'",
                               Logging.LogLevel.DEBUG, payload);
        }
    }

    private void addElements(Record record) {
        for (ELEMENTS element: elements) {
            switch (element) {
                case recordID:
                    append(enriched, "recordID", XMLUtil.encode(record.getId()));
                    break;
                case recordBase:
                    append(enriched, "recordBase", XMLUtil.encode(record.getBase()));
                    break;
                case ctime:
                    append(enriched, "ctime", solrTime.format(new Date(record.getCreationTime())));
                    break;
                case mtime:
                    append(enriched, "mtime", solrTime.format(new Date(record.getModificationTime())));
                    break;
                case itime:
                    append(enriched, "itime", solrTime.format(new Date()));
                    break;
                default:
                    throw new UnsupportedOperationException("The record element '" + element + "' is unknown");
            }
        }
    }

    private void append(StringBuilder enriched, String key, String value) {
        enriched.append("<field name=\"").append(key).append("\">").append(value).append("</field>\n");
    }

    private final StringBuilder enriched = new StringBuilder();

    @Override
    public String toString() {
        return "SolrDocumentEnrich(elements=[" + Strings.join(elements) + "])";
    }
}
