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
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLStepper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.*;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Treats the content of incoming Records as XML and reduces it according to setup.
 * Intended as a patch for very large XML files that cannot be handled by later steps in the chain.
 * The processing is stream based and memory usage is approximately double the size of the Record content
 * as chars (i.e. no Object DOM creation).
 * </p><p>
 * @see {@link XMLStepper#limitXML} for details on setup.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class ReduceXMLFilter extends ObjectFilterImpl {
    /**
     * Logger.
     */
    private static Log log = LogFactory.getLog(ReduceXMLFilter.class);

    /**
     * Rules for XML limits as a list of Strings. Each rule has the format {@code max regexp}, where a max value
     * of -1 means no limit. Every tag and every attribute (optional) is matched against the limits. Tags are
     * represented as {@code /rootelement/subelement}, attributes as
     * {@code /rootelement/subelement#attributename=value}.
     * Namespaces are not part of the representation.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_LIMITS = "common.reducexml.limits";

    /**
     * If true, the limit applies to matched patterns. If false, the limit if for each regexp.
     * If this is false and the limit is {@code 10 .*}, only 10 elements in total is kept.
     * If this is true and the limit is {@code 10 .*}, 10 elements of each kind is kept.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_COUNT_PATTERNS = "common.reducexml.countpatterns";
    public static final boolean DEFAULT_COUNT_PATTERNS = false;

    /**
     * If true, only element names are matched, not attributes.  Setting this to true speeds up processing.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_ONLY_CHECK_ELEMENT_PATHS = "common.reducexml.onlycheckelementpaths";
    public static final boolean DEFAULT_ONLY_CHECK_ELEMENT_PATHS = true;

    /**
     * If true, paths that are not matched by any limit are discarded.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_DISCARD_NONMATCHED = "common.reducexml.discardnonmatched";
    public static final boolean DEFAULT_DISCARD_NONMATCHED = false;

    private final Map<Pattern, Integer> limits = new HashMap<Pattern, Integer>();
    private final boolean countPatterns;
    private final boolean onlyCheckElementPaths;
    private final boolean discardNonMatched;

    private final XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // No resolving of external DTDs
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }
    private final XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();

    /**
     * Constructs a new XML reducing filter, with the specified configuration.
     * @param conf The configuration to construct this filter.
     */
    public ReduceXMLFilter(Configuration conf) {
        super(conf);

        for (String limitConf: conf.getStrings(CONF_LIMITS)) {
            String[] tokens = limitConf.split(" ", 2);
            if (tokens.length != 2) {
                throw new ConfigurationException(
                        "The property " + CONF_LIMITS + " contained the limit '" + limitConf + "' which did not conform"
                        + " to the format 'max regexp'");
            }
            int max;
            try {
                max = Integer.parseInt(tokens[0]);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(
                        "The property " + CONF_LIMITS + " contained the limit '" + limitConf + "' which did not conform"
                        + " to the format 'max (integer) regexp'. Alleged integer was '" + tokens[0] + "'", e);
            }
            Pattern regexp;
            try {
                regexp = Pattern.compile(tokens[1]);
            } catch (PatternSyntaxException e) {
                throw new ConfigurationException(
                        "The property " + CONF_LIMITS + " contained the limit '" + limitConf + "' which did not conform"
                        + " to the format 'max regexp'. Offending regexp was '" + tokens[1] + "'", e);
            }
            limits.put(regexp, max);
        }
        countPatterns = conf.getBoolean(CONF_COUNT_PATTERNS, DEFAULT_COUNT_PATTERNS);
        onlyCheckElementPaths = conf.getBoolean(CONF_ONLY_CHECK_ELEMENT_PATHS, DEFAULT_ONLY_CHECK_ELEMENT_PATHS);
        discardNonMatched = conf.getBoolean(CONF_DISCARD_NONMATCHED, DEFAULT_DISCARD_NONMATCHED);
        log.info("Constructed " + this);
    }

    private ByteArrayOutputStream os = new ByteArrayOutputStream();
    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        XMLStreamReader in;
        try {
            in = xmlFactory.createXMLStreamReader(RecordUtil.getReader(payload));
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to create an XMLStreamReader", e, payload);
        }
        os.reset();
        XMLStreamWriter out;
        try {
            out = xmlOutFactory.createXMLStreamWriter(os);
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to create an XMLStreamWriter", e, payload);
        }
        try {
            XMLStepper.limitXML(in, out, limits, countPatterns, onlyCheckElementPaths, discardNonMatched);
        } catch (XMLStreamException e) {
            throw new PayloadException("Exception reducung xml", e, payload);
        }
        payload.getRecord().setContent(os.toByteArray(), payload.getRecord().isContentCompressed());
        return true;
    }

    @Override
    public String toString() {
        return "ReduceXMLFilter(limits=" + Strings.join(limits.entrySet()) + ", countPatterns=" + countPatterns
               + ", onlyCheckElementPaths=" + onlyCheckElementPaths + ", discardNonMatched=" + discardNonMatched + ')';
    }
}