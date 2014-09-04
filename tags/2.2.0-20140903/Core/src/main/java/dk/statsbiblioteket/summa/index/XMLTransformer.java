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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.GraphFilter;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.util.PayloadMatcher;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.common.xml.SummaEntityResolver;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.NamespaceRemover;
import dk.statsbiblioteket.util.xml.XSLT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Transform arbitrary XML in Payload.Record.content using XSLT. This is
 * normally used to convert source-specific XML to SummaDocumentXML (ready
 * for further transformation to a Lucene Document object by CreateDocument).
 * </p><p>
 * Note that the XMLTransformer uses the JVM's default Transformer for
 * processing of XSLTs. One common problem with XSLT-processing is infinite or
 * just very deep recursion, e.g. for processing of a list of names. For a
 * standard Java 1.6 setup from Sun, this can lead to a StackOverflowError
 * in the JVM.
 * </p><p>
 * The problem with Errors is that the state of the virtual machine is not
 * guaranteed to be stable when an Error has been thrown. If an Error occurs,
 * the offending Payload is logged at FATAL level and further processing is
 * terminated. If such a controlled crash occurs, it is recommended that the
 * offending records be removed from the workflow. One easy way of doing this is
 * to insert a
 * {@link dk.statsbiblioteket.summa.common.filter.object.RegexFilter} in front
 * of the XMLTransformer with a regexp that matches the id of the Payload.
 * </p><p>
 * In addition to the stated parameters, the parameters from
 * {@link PayloadMatcher} can optionally be applied. If none of these parameters
 * are present, all Payloads are accepted.
 * </p><p>
 * this class inherits from GraphFilter so
 * {@link GraphFilter#CONF_VISIT_CHILDREN} and
 * {@link GraphFilter#CONF_VISIT_PARENTS} should be specified if traversal is
 * required.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class XMLTransformer extends GraphFilter<Object> {
    private static Log log = LogFactory.getLog(XMLTransformer.class);

    /**
     * If present, this will contain a list of Configurations with the
     * parameters for XSLT-based transformations. If not present, the parameters
     * will be taken directly from the topmost Configuration, effectively making
     * the transformer single-XSLT.
     * </p><p>
     * Optional.
     */
    public static final String CONF_SETUPS = "summa.xmltransformer.setups";

    /* Parameters below can be used at the topmost level of the Configuration
       and in Configurations under CONF_SETUPS.
     */

    /**
     * The location of the XSLT to use for XML transformation. This is in the
     * form of an URL, typically file: or http: based.
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_XSLT = "summa.xmltransformer.xslt";

    /**
     * The source for the transformation.
     * @see {@link RecordUtil#getString(dk.statsbiblioteket.summa.common.filter.Payload, String)}
     * </p><p>
     * Optional. Default is "content" ({@link RecordUtil#PART_CONTENT}).
     */
    public static final String CONF_SOURCE = "summa.xmltransformer.source";
    public static final String DEFAULT_SOURCE = RecordUtil.PART_CONTENT;

    /**
     * The destination for the transformation.
     * @see {@link RecordUtil#setString(dk.statsbiblioteket.summa.common.Record, String, String)}.
     * </p><p>
     * Optional. Default is "content" ({@link RecordUtil#PART_CONTENT}).
     */
    public static final String CONF_DESTINATION = "summa.xmltransformer.destination";
    public static final String DEFAULT_DESTINATION = RecordUtil.PART_CONTENT;

    /**
     * If true, all namespaces in the XML is stripped prior to transformation.
     * This is not recommended, but in The Real World, there are a lot of XML
     * and XSLT's with garbled namespace matching.
     * </p><p>
     * Note: Using true might have a noticeable impact on processor-load and
     *       temporary object allocation.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_STRIP_XML_NAMESPACES = "summa.xmltransformer.ignorexmlnamespaces";
    public static final boolean DEFAULT_STRIP_XML_NAMESPACES = false;

    /**
     * If specified, the stated EntityResolver class will be instantiated and
     * used by the Transformer.
     * </p><p>
     * Optional.
     * @see dk.statsbiblioteket.summa.common.xml.SummaEntityResolver
     */
    public static final String CONF_ENTITY_RESOLVER = "summa.xmltransformer.entityresolver";

    private List<Changeling> changelings = new ArrayList<>();

    /**
     * Sets up the transformer stated in the configuration.
     * @param conf contains the location of the XSLT to use.
     * @see #CONF_XSLT
     */
    public XMLTransformer(Configuration conf) {
        super(conf);
        Changeling base = new Changeling(conf, false);
        if (base.isValid()) {
            changelings.add(base);
        }
        if (conf.valueExists(CONF_SETUPS)) {
            try {
                for (Configuration subConf: conf.getSubConfigurations(CONF_SETUPS)) {
                    changelings.add(new Changeling(subConf, true));
                }
            } catch (SubConfigurationsNotSupportedException e) {
                throw new ConfigurationException("Could not extract sub configurations", e);
            }
        }
        if (changelings.isEmpty()) {
            throw new ConfigurationException(
                "Unable to extract any transformation setups (key: " + CONF_SETUPS + ") or a XSLT location (key: "
                + CONF_XSLT + ") from configuration");
        }
        log.info("XMLTransformer with " + changelings.size() + " transforming sub-units initialized");
    }


    @Override
    public boolean processRecord(Record record, boolean origin, Object o) throws PayloadException {
        Changeling changeling = getChangeling(record);
        if (changeling == null) {
            Logging.logProcess(
                "XMLTransformer", "Unable to locate a sub transformer",
                Logging.LogLevel.TRACE, record.toString());
            return false;
        }
        if (log.isTraceEnabled()) {
            log.trace("Located changeling for (" + record + ")");
        }
        changeling.transform(record);
        return true;
    }

    @Override
    public Object createState(Payload payload) {
        return null; // Not used for anything in this filter
    }

    @Override
    public boolean finish(Payload payload, Object state, boolean success) throws PayloadException {
        if (!success) {
            Logging.logProcess(getName(), "Unable to transform. discarding", Logging.LogLevel.DEBUG, payload);
        }
        return success;
    }


    private Changeling getChangeling(Record record) {
        for (Changeling changeling: changelings) {
            if (changeling.matches(record)) {
                return changeling;
            }
        }
        return null;
    }

    @Override
    public synchronized void close(boolean success) {
        super.close(success);
        log.info("Closing down XMLTransformer '" + getName() + "'. " + getProcessStats());
    }

    @Override
    public String toString() {
        return "XMLTransformer '" + getName() + "' with " + changelings.size() + " sub transformers";
    }

    private class Changeling {
        private final URL xsltLocation;
        private final boolean stripXMLNamespaces;
        private EntityResolver entityResolver = null;
        // Used if the EntityResolver is not null
        private Transformer transformer;
        private final PayloadMatcher matcher;
        private final String source;
        private final String destination;

        public Changeling(Configuration conf, boolean failOnMissing) {
            String xsltLocationString = conf.getString(CONF_XSLT, null);
            if ((failOnMissing && xsltLocationString == null) || "".equals(xsltLocationString)) {
                throw new ConfigurationException(String.format("The property %s must be defined", CONF_XSLT));
            }
            matcher = new PayloadMatcher(conf, false);
            //noinspection DuplicateStringLiteralInspection
            log.debug(getName() + " extracted XSLT location '" + xsltLocationString + "' from properties");
            xsltLocation = Resolver.getURL(xsltLocationString);
            if (failOnMissing && xsltLocation == null) {
                throw new ConfigurationException(String.format(
                    "The xsltLocation '%s' could not be resolved to a URL", xsltLocationString));
            }
            if (failOnMissing && !conf.valueExists(CONF_STRIP_XML_NAMESPACES)) {
                log.warn(String.format(
                    "The key %s was not defined. It is highly recommended to define it as the wrong value "
                    + "typically wrecks the output. Falling back to default %b",
                    CONF_STRIP_XML_NAMESPACES, DEFAULT_STRIP_XML_NAMESPACES));
            }
            source = conf.getString(CONF_SOURCE, DEFAULT_SOURCE);
            destination = conf.getString(CONF_DESTINATION, DEFAULT_DESTINATION);
            stripXMLNamespaces = conf.getBoolean(CONF_STRIP_XML_NAMESPACES, DEFAULT_STRIP_XML_NAMESPACES);
            if (xsltLocation != null) {
                initTransformer(conf);
            }
            log.info("initialized Changeling for xsltLocation '" + xsltLocation + "'. Namespaces will "
                     + (stripXMLNamespaces ? "" : "not ") + "be stripped from input before transformation");
        }

        private void initTransformer(Configuration conf) throws ConfigurationException {
            try {
                // getLocalTransformer would in principle be better, but is it really safe in our context?
                transformer = XSLT.createTransformer(xsltLocation);
                // We want our extensions to work
                transformer.setParameter(XMLConstants.FEATURE_SECURE_PROCESSING, false);
                if (!conf.valueExists(CONF_ENTITY_RESOLVER)) {
                    log.debug("No entity-resolver specified. Using basic transformation calls");
                    return;
                }
                log.debug("Attempting to assign entity resolver " + conf.get(CONF_ENTITY_RESOLVER));
                Class<? extends EntityResolver> resolver = conf.getClass(
                    CONF_ENTITY_RESOLVER, EntityResolver.class, SummaEntityResolver.class);
                entityResolver = Configuration.create(resolver, conf);
                log.debug("Getting transformer");
            } catch (NullPointerException e) {
                throw new ConfigurationException("Unable to construct Transformer for xslt '" + xsltLocation + "'", e);
            } catch (TransformerException e) {
                throw new ConfigurationException(String.format(
                    "Unable to create transformer based on '%s'", xsltLocation), e);
            }
            if (entityResolver != null && stripXMLNamespaces) {
                log.warn("entityResolver does not support stripXMLNamespaces");
            }
        }

        /**
         * A Changeling is valid if is has an xsltLocation.
         * @return if the Changeling can be used.
         */
        public boolean isValid() {
            return xsltLocation != null;
        }

        public boolean matches(Record record) {
            return !matcher.isMatcherActive() || matcher.isMatch(record);
        }
        private ByteArrayOutputStream out = null;
        public void transform(Record record) throws PayloadException {
            long transformTime = -System.nanoTime();
            innerTransform(record);
            transformTime += System.nanoTime();
            Logging.logProcess(
                "XMLTransformer",
                "Transform for " + record.getId() + " finished in " + (transformTime / 1000000.0) + "ms",
                Logging.LogLevel.TRACE, record.getId());
        }

        private synchronized void innerTransform(Record record) throws PayloadException {
            Reader inner;
            try {
                inner = new InputStreamReader(RecordUtil.getStream(record, source), "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("utf-8 should be supported", e);
            }
            Reader reader = stripXMLNamespaces ? new NamespaceRemover(inner) : inner;

            XMLReader xml;
            try {
                xml = XMLReaderFactory.createXMLReader();
            } catch (SAXException e) {
                throw new PayloadException("Unable to create XMLReader", e);
            }
            if (entityResolver != null) {
                xml.setEntityResolver(entityResolver);
            }
            if (out == null) {
                out = new ByteArrayOutputStream(1000);
            }

            out.reset();
            Result result = new StreamResult(out);
            InputSource is = new InputSource(reader);
            Source source = new SAXSource(xml, is);

            if (log.isTraceEnabled()) {
                log.trace(getName() + " calling transformer for " + getName() + " for " + record);
            }
            try {
                transformer.setParameter(IndexUtils.RECORD_FIELD, record.getId());
                transformer.setParameter("recordBase", record.getBase());
                transformer.transform(source, result);
            } catch (TransformerException e) {
                log.debug("Transformation failed for " + record, e);
                throw new PayloadException("Unable to transform content for '" + record + "'", e);
            }
            RecordUtil.setBytes(record, out.toByteArray(), destination);
            if (log.isTraceEnabled()) {
                log.trace(getName() + " finished transforming " + record);
            }
        }
    }
}
