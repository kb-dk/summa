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
package dk.statsbiblioteket.summa.index.lucene;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.util.ParseUtil;
import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;

/**
 * Converter from SummaDocumentXML to Lucene Documents. The generated Document
 * is added to the Payload's data under the key {@link Payload#LUCENE_DOCUMENT}.
 * </p><p>
 * see SummaDocumentXMLSample.xml.
 * </p><p>
 * Note: The DocumentCreator need an index-description. The setup for retrieving
 * the description must be stored in the sub-property 
 * {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor#CONF_DESCRIPTOR} with parameters from
 * {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor}.
 * @deprecated
 * {@link dk.statsbiblioteket.summa.index.lucene.StreamingDocumentCreator}
 * has same functionality and significantly better performance.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.QA_OK, // due to deprecation
        author = "te")
// TODO: Consider adding base as a standard field - stored as well as indexed
public class DocumentCreator extends DocumentCreatorBase {
    @SuppressWarnings({"deprecation"})
    private static Log log = LogFactory.getLog(DocumentCreator.class);

    // TODO: Entity-encode fields

    // TODO: Reconsider this namespace
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
//    public static final String SUMMA_NAMESPACE =
//            "http://statsbiblioteket.dk/2008/Index";
    public static final String SUMMA_NAMESPACE =
            "http://statsbiblioteket.dk/summa/2008/Document";
    public static final String SUMMA_NAMESPACE_PREFIX = "Index";
    // TODO: Make DocumentCreator support namespace qualified attributes
    /**
     * Builder for building DOM objects out of SummaDocumentXML.
     */
    private DocumentBuilder domBuilder;

    /* Where to locate fields in the SummaDocumentXML */
    private XPathExpression singleFieldXPathExpression;

    private LuceneIndexDescriptor descriptor;

    /**
     * Initialize the underlying parser and set up internal structures.
     * {@link #setSource} must be called before further use.
     * @param conf the configuration for CreateDocument.
     * @throws ConfigurationException if there was a problem with the
     *                                configuration.
     */
    public DocumentCreator(Configuration conf) throws ConfigurationException {
        super(conf);
        log.warn("The " + getClass().getName() + " is deprecated. Unless there "
                 + "is a special reason not to, the "
                 + StreamingDocumentCreator.class.getName() + " should be used"
                 + " instead as it is much faster");
        descriptor = LuceneIndexUtils.getDescriptor(conf);
        initXPaths();
        createDocumentBuilder();
    }

    XPath xPath;
    private void initXPaths() {
        log.debug("initXPaths() called");
        DefaultNamespaceContext nsCon = new DefaultNamespaceContext();
        nsCon.setNameSpace(SUMMA_NAMESPACE, SUMMA_NAMESPACE_PREFIX);

        XPathFactory xpfac = XPathFactory.newInstance();
        XPath singleFields = xpfac.newXPath();
        singleFields.setNamespaceContext(nsCon);

        xPath = xpfac.newXPath();
        xPath.setNamespaceContext(nsCon);
        try {
            //noinspection DuplicateStringLiteralInspection
            singleFieldXPathExpression = singleFields.compile(
                    "/Index:SummaDocument/Index:fields/Index:field");
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Could not compile XPaths", e);
        }
        log.debug("initXPaths() finished");
    }

    private void createDocumentBuilder() {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        builderFactory.setNamespaceAware(true); // Optional?
        builderFactory.setValidating(false);    // Optional?

        try {
            domBuilder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to create DocumentBuilder",
                                            e);
        }
    }

    /**
     * Convert the content of the Record embedded in the payload to a Lucene
     * Document. The content must be in the format SummaDocumentXML.
     * @param payload the container for the Record-content to convert.
     */
    // TODO: If not added, mark meta-data with unadded and continue gracefully
    @Override
    public boolean processPayload(Payload payload) throws PayloadException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("processPayload(" + payload + ") called");
        long startTime = System.currentTimeMillis();
        if (payload.getRecord() == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new PayloadException("Payload has no Record", payload);
        }
        Document dom;
        try {
            dom = domBuilder.parse(new ByteArrayInputStream(
                    payload.getRecord().getContent()));
        } catch (SAXException e) {
            throw new PayloadException("could not parse record content",
                                       e, payload);
        } catch (IOException e) {
            throw new PayloadException(
                    "Unable to process ByteArrayInputStream for " + payload, e);
        }

//        NamedNodeMap att = dom.getDocumentElement().getAttributes();
//        IndexDefaults defaults = IndexUtils.getDefaults(att, SUMMA_NAMESPACE);

        // TODO: Use a pool of Documents so that they can be reused
        org.apache.lucene.document.Document luceneDoc =
                new org.apache.lucene.document.Document();
        // TODO: Check whether resolver is used for anything
        Float docBoost = DEFAULT_BOOST;
        String boostExpr = "/" + SUMMA_NAMESPACE_PREFIX
                           + ":SummaDocument/@boost";
        try {
            docBoost = ParseUtil.getValue(xPath, dom, boostExpr,
                                          DEFAULT_BOOST);
        } catch (ParseException e) {
            log.debug("processPayload: Could not extract boost from document '"
            + "' with XPath-expression '" + boostExpr + "'", e);
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding fields to Lucene Document for " + payload);
        try {
            NodeList singleFields = (NodeList)
                    singleFieldXPathExpression.evaluate(dom,
                                                        XPathConstants.NODESET);
            makeIndexFields(payload, singleFields, descriptor, luceneDoc);
        } catch (XPathExpressionException e) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("Could not make single fields for " + payload
                     + ". The Lucene Document is probably incomplete");
        } catch (IndexServiceException e) {
            log.warn("Got IndexServiceException while processing single fields "
                     + "for " + payload);
        }

        //noinspection NumberEquality
        if (docBoost != DEFAULT_BOOST) {
            // Document-boost for Lucene means that all Field-boosts are
            // multiplied with the docBoost.
            log.trace("Setting document boost to " + docBoost + " for "
                      + payload.getId());
            luceneDoc.setBoost(docBoost);
        }

        payload.getData().put(Payload.LUCENE_DOCUMENT, luceneDoc);
        // TODO: Consider if the SearchDescriptor is needed
//        payload.getData().put(Payload.SEARCH_DESCRIPTOR, descriptor);
        //noinspection DuplicateStringLiteralInspection
        log.debug("Added Lucene Document and SearchDescriptor to payload "
                  + payload + ". Processing time was "
                  + (System.currentTimeMillis() - startTime) + " ms");
        return true;
    }

    private static final Float DEFAULT_BOOST = 1.0f;

    private static final String FIELD_NAME_PATH = "@name";
    private void makeIndexFields(Payload payload, NodeList fields,
                                 LuceneIndexDescriptor descriptor,
                                 org.apache.lucene.document.Document luceneDoc)
                                                   throws IndexServiceException{
        long startTime = System.currentTimeMillis();
        int len = fields.getLength();
        //noinspection DuplicateStringLiteralInspection
        log.trace("makeIndexFields called with " + len + " field nodes");

        for (int i = 0; i < len; i++) {
            Node fieldNode = fields.item(i);
            String name;
            try {
                name = ParseUtil.getValue(xPath, fieldNode, FIELD_NAME_PATH,
                                          (String)null);
            } catch (ParseException e) {
                log.warn("makeIndexField: Could not extract name from field '"
                + fieldNode.getLocalName() + "' with expression '"
                + FIELD_NAME_PATH + "'", e);
                continue;
            }
            if (name == null || "".equals(name)) {
                log.warn("makeIndexField: Name not specified for field '"
                         + fieldNode.getLocalName() + "' from "
                         + payload + ". Skipping field");
                continue;
            }
            Float boost;
            try {
                boost = ParseUtil.getValue(xPath, fieldNode, "@boost",
                                           DEFAULT_BOOST);
            } catch (ParseException e) {
                log.debug("makeIndexField: Could not extract boost from field '"
                          + fieldNode.getLocalName()
                          + "' with expression '@boost'", e);
                continue;
            }
            String content = DOM.getElementNodeValue(fieldNode);
            if (content != null) {
                // TODO: Perform a more complete trim (newline et al)
                content = content.trim();
            }
            if (content == null || "".equals(content)) {
                log.debug("No content for field '" + name
                          + "'. Skipping to next field");
                continue;
            }

            LuceneIndexField indexField =
                    addFieldToDocument(descriptor, luceneDoc,
                                       name, content, boost);

            if (indexField.isInFreetext()) {
                addToFreetext(descriptor, luceneDoc, name, content);
            }
        }
        log.trace("Finished document creation (" + luceneDoc.getFields().size()
                  + " fields was added) in "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    @Override
    public synchronized void close(boolean success) {
        super.close(success);
        log.info("Closing down Documentcreator. " + getProcessStats());
    }
}

