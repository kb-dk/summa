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
import dk.statsbiblioteket.summa.plugins.SaxonXSLT;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.NamespaceRemover;
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
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
     * Optional. Default is "content" ({@link RecordUtil.PART#content}).
     */
    public static final String CONF_SOURCE = "summa.xmltransformer.source";
    public static final String DEFAULT_SOURCE = RecordUtil.PART.content.toString();

    /**
     * The destination for the transformation.
     * @see {@link RecordUtil#setString(dk.statsbiblioteket.summa.common.Record, String, String)}.
     * </p><p>
     * Optional. Default is "content" ({@link RecordUtil.PART#content}).
     */
    public static final String CONF_DESTINATION = "summa.xmltransformer.destination";
    public static final String DEFAULT_DESTINATION = RecordUtil.PART.content.toString();

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

    /**
     * XSLT processing is by nature recursive and it it hard to guard against recursion that blows the stack.
     * Although the Java API states that Errors signifies that the JVM is in a bad state and should be terminated,
     * this works poorly when working with dirty data and the Transformer (and all data are dirty).
     * </p><p>
     * If true, too-deep recursion in XSLT will be caught and an error will be logged, but the JVM will not exit.
     * If false, the JVM will terminate if a too-deep recursion is encountered.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_CATCH_STACK_OVERFLOW = "summa.xmltransformer.stackoverflow.catch";
    public static final boolean DEFAULT_CATCH_STACK_OVERFLOW = true;

    /**
     * Heavy debug mode: Dumps the incoming document in the log at INFO-level.
     * Only enable this temporarily.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_FULL_DEBUG_DUMP = "summa.xmltransformer.fulldebugdump";
    public static final boolean DEFAULT_FULL_DEBUG_DUMP = false;

    private final boolean topLevelStackOverflowCatch;

    /**
     * Set with {@link RecordUtil#CONF_ESCAPE_CONTENT}. Can be overwritten inside Changelings.
     * Default is false.
     */
    private final boolean defaultEscapeContentOnXmlFull;
    private final boolean fullDebugDump;

    private List<Changeling> changelings = new ArrayList<>();

    /**
     * Sets up the transformer stated in the configuration.
     * @param conf contains the location of the XSLT to use.
     * @see #CONF_XSLT
     */
    public XMLTransformer(Configuration conf) {
        super(conf);
        topLevelStackOverflowCatch = conf.getBoolean(CONF_CATCH_STACK_OVERFLOW, DEFAULT_CATCH_STACK_OVERFLOW);
        defaultEscapeContentOnXmlFull = conf.getBoolean(RecordUtil.CONF_ESCAPE_CONTENT, false);
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
        fullDebugDump = conf.getBoolean(CONF_FULL_DEBUG_DUMP, DEFAULT_FULL_DEBUG_DUMP);
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
            Logging.logProcess(getName(), "Unable to transform. Discarding", Logging.LogLevel.DEBUG, payload);
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
        return "XMLTransformer '" + getName() + "' with " + (changelings == null ? "null" : changelings.size())
               + " sub transformers";
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
        private final boolean stackOverflowCatch;
        private final boolean escapeContentOnXmlFull;

        public Changeling(Configuration conf, boolean failOnMissing) {
            String xsltLocationString = conf.getString(CONF_XSLT, null);
            if (xsltLocationString == null || xsltLocationString.isEmpty()) {
                if (failOnMissing) {
                    throw new ConfigurationException(String.format(Locale.ROOT, "The property %s must be defined", CONF_XSLT));
                }
                log.debug("No " + CONF_XSLT + " defined, but failOnMissing=false. Skipping all other properties");
                // Set everything to null as the Changeling will be discarded anyway
                xsltLocation = null;
                stripXMLNamespaces = false;
                matcher = null;
                source = null;
                destination = null;
                stackOverflowCatch = false;
                escapeContentOnXmlFull = false;
                return;
            }
            matcher = new PayloadMatcher(conf, false);
            //noinspection DuplicateStringLiteralInspection
            log.debug(getName() + " extracted XSLT location '" + xsltLocationString + "' from properties");
            xsltLocation = Resolver.getURL(xsltLocationString);
            if (failOnMissing && xsltLocation == null) {
                throw new ConfigurationException(String.format(
                        Locale.ROOT, "The xsltLocation '%s' could not be resolved to a URL", xsltLocationString));
            }
            if (failOnMissing && !conf.valueExists(CONF_STRIP_XML_NAMESPACES)) {
                log.warn(String.format(Locale.ROOT,
                    "The key %s was not defined. It is highly recommended to define it as the wrong value "
                    + "typically wrecks the output. Falling back to default %b",
                    CONF_STRIP_XML_NAMESPACES, DEFAULT_STRIP_XML_NAMESPACES));
            }
            source = conf.getString(CONF_SOURCE, DEFAULT_SOURCE);
            destination = conf.getString(CONF_DESTINATION, DEFAULT_DESTINATION);
            stripXMLNamespaces = conf.getBoolean(CONF_STRIP_XML_NAMESPACES, DEFAULT_STRIP_XML_NAMESPACES);
            stackOverflowCatch = conf.getBoolean(CONF_CATCH_STACK_OVERFLOW, DEFAULT_CATCH_STACK_OVERFLOW);
            escapeContentOnXmlFull = conf.getBoolean(RecordUtil.CONF_ESCAPE_CONTENT, defaultEscapeContentOnXmlFull);
            if (xsltLocation != null) {
                initTransformer(conf);
            }
            log.info("Created " + this);
        }

        private void initTransformer(Configuration conf) throws ConfigurationException {
            try {
                // getLocalTransformer would in principle be better, but is it really safe in our context?
                //transformer = XSLT.createTransformer(xsltLocation);
                transformer = SaxonXSLT.createTransformer(xsltLocation);
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
                log.debug("Constructed Transformer with class " + transformer.getClass());
            } catch (NullPointerException e) {
                throw new ConfigurationException("Unable to construct Transformer for xslt '" + xsltLocation + "'", e);
            } catch (TransformerException e) {
                throw new ConfigurationException(String.format(Locale.ROOT,
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
            Reader reader = getReader(record);

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
                out = new ByteArrayOutputStream(5000);
            }

            out.reset();
            Result result = new StreamResult(out);
            InputSource is = new InputSource(reader);
            Source source = new SAXSource(xml, is);

            if (log.isTraceEnabled()) {
                log.trace(getName() + " calling transformer for " + getName() + " for " + record);
            }
            if (stackOverflowCatch) {
                catchTransform(record, source, result);
            } else {
                nonCatchTransform(record, source, result);
            }
            RecordUtil.setBytes(record, out.toByteArray(), destination);
            if (fullDebugDump) {
                try {
                    log.info(String.format(Locale.ROOT,
                            "Finished transforming record %s using XSLT %s ans stripNamespaces=%b from\n" +
                            "%s\n**************** to ****************\n%s",
                            record.getId(), xsltLocation, stripXMLNamespaces,
                            Strings.flush(getReader(record)),
                            RecordUtil.getString(record, RecordUtil.PART.content)));
                } catch (IOException e) {
                    throw new PayloadException("Unable to stream content from record " + record.getId(), e);
                }
            }
            if (log.isTraceEnabled()) {
                log.trace(getName() + " finished transforming " + record);
            }
        }

        private Reader getReader(Record record) throws PayloadException {
            Reader inner;
            try { // Special processing as RecordUtil.getStream does not support controlling content escaping
                inner = RecordUtil.PART.xmlfull.toString().equals(source) && !escapeContentOnXmlFull ?
                        new StringReader(RecordUtil.toXML(record, false)) :
                        new InputStreamReader(RecordUtil.getStream(record, source), StandardCharsets.UTF_8);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("utf-8 should be supported", e);
            } catch (IOException e) {
                throw new PayloadException("IOException while XML-serializing content", e);
            }
            return stripXMLNamespaces ? new NamespaceRemover(inner) : inner;
        }


        // Don't init cause with the StackOverflowError as it is by nature excessive and not very telling
        @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
        private void catchTransform(Record record, Source source, Result result) throws PayloadException {
            try {
                transformer.setParameter(IndexUtils.RECORD_FIELD, record.getId());
                transformer.setParameter("recordBase", record.getBase());
                // TODO: Seems that DTMManagerDefault.getDTM & DTMStringPool.stringToIndex takes most of the time
                transformer.transform(source, result);
            } catch (StackOverflowError e) {
                String error = e.getMessage();
                error = error == null ? "N/A" : error;
                error = error.length() > 1000 ? error.substring(0, 1000) : error;
                log.error("Stack overflow when processing " + record.getId() + ": " + error);
                throw new PayloadException(
                        "Unable to transform content for '" + record + "' due to stack overflow: " + error);
            } catch (TransformerException e) {
                log.debug("Transformation failed for " + record, e);
                throw new PayloadException("Unable to transform content for '" + record + "'", e);
            }
        }

        private void nonCatchTransform(Record record, Source source, Result result) throws PayloadException {
            try {
                transformer.setParameter(IndexUtils.RECORD_FIELD, record.getId());
                transformer.setParameter("recordBase", record.getBase());
                transformer.transform(source, result);
            } catch (TransformerException e) {
                log.debug("Transformation failed for " + record, e);
                throw new PayloadException("Unable to transform content for '" + record + "'", e);
            }
        }

        @Override
        public String toString() {
            return "Changeling(" +
                   "xslt=" + xsltLocation +
                   ", stripXMLNamespaces=" + stripXMLNamespaces +
                   ", matcher=" + matcher +
                   ", source='" + source + '\'' +
                   ", escapeContent='" + escapeContentOnXmlFull + '\'' +
                   ", destination='" + destination + '\'' +
                   ", stackOverflowCatch=" + stackOverflowCatch +
                   ')';
        }
    }
}
