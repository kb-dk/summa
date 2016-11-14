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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLStepper;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.tools.ant.filters.StringInputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Stream-oriented replacer for XML that performs search-replace in the text for specified fields.
 * </p><p>
 * The default is to replace spaces with blanks (effectively remove spaces). This can be controlled with
 * {@link #CONF_REGEXP} and {@link #CONF_REPLACEMENT}.
 */
// TODO: Make this also copy headers such as <?xml version="1.0" encoding="UTF-8"?>
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLReplaceFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(XMLReplaceFilter.class);

    /**
     * The stated regexp will be used for a Java Pattern, which will perform a replace on the field texts.
     * </p><p>
     * Optional. Default is {@code  +} (one or more spaces).
     */
    public static String CONF_REGEXP = "xmlreplacefilter.regexp";
    public static String DEFAULT_REGEXP = " +";

    /**
     * The stated replacement will be inserted where the regexp from {@link #CONF_REGEXP} matches.
     * </p><p>
     * Optional. Default is the empty String.
     */
    public static String CONF_REPLACEMENT = "xmlreplacefilter.replacement";
    public static String DEFAULT_REPLACEMENT = "";

    /**
     * A list of MARC fields with the format {@code field*subfield} to remove spaces from.
     * </p><p>
     * Mandatory. Sample: ["001*a", "002*a", "002*c"]
     */
    public static final String CONF_ID_FIELDS = "marcidadjuster.idfields";

    private final Map<String, Set<String>> idFields = new HashMap<>();
    private final Pattern pattern;
    private final String replacement;
    private final XMLStepper.ContentReplaceCallback replacer;

    public XMLReplaceFilter(Configuration conf) {
        super(conf);
        pattern = Pattern.compile(conf.getString(CONF_REGEXP, DEFAULT_REGEXP));
        replacement = conf.getString(CONF_REPLACEMENT, DEFAULT_REPLACEMENT);
        List<String> ifs = conf.getStrings(CONF_ID_FIELDS);
        for (String rule: ifs) {
            String[] tokens = rule.split("[*]");
            if (tokens.length != 2) {
                throw new ConfigurationException(
                        "The replacement rule '" + rule + "' did not follow the format field*subfield");
            }
            final String field = tokens[0];
            final String subField = tokens[1];

            Set<String> subs = idFields.containsKey(field) ? idFields.get(field) : new HashSet<String>();
            subs.add(subField);
            idFields.put(field, subs);
        }
        replacer = new XMLStepper.ContentReplaceCallback() {
            long startTime = System.nanoTime();
            long checked = 0 ;
            long matched = 0;

            String inDataField = null;

            @Override
            protected void setOut(XMLStreamWriter out) {
                super.setOut(out);
                startTime = System.nanoTime();
                checked = 0;
                matched = 0;
            }

            @Override
            protected String replace(List<String> tags, String current, String originalText) {
                matched++;
                return pattern.matcher(originalText).replaceAll(replacement);
            }

            @Override
            protected boolean match(XMLStreamReader xml, List<String> tags, String current) {
                checked++;
                // <datafield tag="001" ind1="0" ind2="0">
                if ("datafield".equals(current)) {
                    inDataField = XMLStepper.getAttribute(xml, "tag", "N/A");
                    return false;
                }
                // <subfield code="a">ssib0 045 55 195</subfield>
                if (!"subfield".equals(current)) {
                    return false;
                }
                Set<String> subFields = idFields.get(inDataField);
                return subFields != null && subFields.contains(XMLStepper.getAttribute(xml, "code", "N/A"));
            }

            @Override
            public void elementEnd(String element) {
                super.elementEnd(element);
                if ("datafield".equals(element)) {
                    inDataField = null;
                }
            }

            @Override
            public void end() {
                log.debug("Finished replacing " + matched + "/" + checked + " matched elements in " +
                          (System.nanoTime()-startTime)/1000000 + " ms");
            }
        };
        log.info("Created " + this);
    }

    private int wraps = 0;
    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getStream() != null) { // Streaming mode (yay)
            try {
                payload.setStream(XMLStepper.streamingReplaceElementText(payload.getStream(), replacer));
                log.debug("Wrapped Payload Stream from " + payload.getData(Payload.ORIGIN) + " in XML text replacer");
            } catch (Exception e) {
                throw new PayloadException("Exception setting up streaming replacer", e);
            }
            return true;
        }
        try {
            String replaced = XMLStepper.replaceElementText(RecordUtil.getString(payload), replacer);
            payload.setStream(new StringInputStream(replaced));
        } catch (XMLStreamException e) {
            throw new PayloadException("Exception performing XML-aware content replacement", payload);
        }
        return true;
    }

/*    @Override
    public String toString() {
        return "XMLReplaceFilter(" + (idFields.isEmpty() ? "..." : Strings.join(idFields.entrySet()));
    }*/
}
