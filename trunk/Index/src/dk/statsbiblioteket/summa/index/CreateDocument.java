/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.index;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import dk.statsbiblioteket.summa.common.lucene.index.FieldType;
import dk.statsbiblioteket.summa.common.lucene.index.IndexDefaults;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexField;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaKeywordAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaRepeatAnalyzer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Field;
import org.xml.sax.SAXException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Converter from SummaDocumentXML to Lucene Documents. As part of the
 * conversion, a SearchDescriptor for the single Document is created and
 * added as Payload data.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Port boost from stable
public class CreateDocument implements ObjectFilter {
    private static Log log = LogFactory.getLog(CreateDocument.class);

    private ObjectFilter source;

    /**
     * Builder for building DOM objects out of SummaDocumentXML.
     */
    private DocumentBuilder domBuilder;

    /* Where to locate fields in the SummaDocumentXML */
    private XPathExpression singleFieldXPathExpression;
    /* Where to locate groups in the SummaDocumentXML */
    private XPathExpression groupFieldXPathExpression;
    /* How to extract the fields in a group in the SummaDocumentXML.
     * This is changed dynamically at usage. */
    private static XPath groupFields;

    /* The namespace for SummaDocumentXML */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final String SUMMA_NAMESPACE =
            "http://statsbiblioteket.dk/2004/Index";

    /* The Field in Documents containing free text */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final String FREETEXT = "freetext";

    /**
     * Initialize the underlying parser and set up internal structures.
     * {@link #setSource} must be called before further use.
     * @param conf the configuration for CreateDocument.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public CreateDocument(Configuration conf) {
        // No prooperties as of now
        initXPaths();
        createDocumentBuilder();
    }

    private void initXPaths() {
        log.debug("initXPaths() called");
        DefaultNamespaceContext nsCon = new DefaultNamespaceContext();
        nsCon.setNameSpace(SUMMA_NAMESPACE, "in");

        XPathFactory xpfac = XPathFactory.newInstance();
        XPath singleFields = xpfac.newXPath();
        singleFields.setNamespaceContext(nsCon);
        XPath group = xpfac.newXPath();
        group.setNamespaceContext(nsCon);
        groupFields = xpfac.newXPath();
        groupFields.setNamespaceContext(nsCon);
        try {
            //noinspection DuplicateStringLiteralInspection
            singleFieldXPathExpression =
                    singleFields.compile("/in:document/in:fields/in:field");
            //noinspection DuplicateStringLiteralInspection
            groupFieldXPathExpression =
                    groupFields.compile("/in:document/in:fields/in:group");

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
    public void addDocument(Payload payload) {
        //noinspection DuplicateStringLiteralInspection
        log.debug("addDocument(" + payload + ") called");
        long startTime = System.currentTimeMillis();
        if (payload.getRecord() == null) {
            throw new IllegalArgumentException(payload + " has no Record");
        }
        Collection<String> existingFields = new HashSet<String>(20);
        PerFieldAnalyzerWrapper indexAnalyzer =
                new PerFieldAnalyzerWrapper(FieldType.freetext.getAnalyzer());
        SearchDescriptor descriptor = new SearchDescriptor("nowhere");
        Document dom;
        try {
            dom = domBuilder.parse(new ByteArrayInputStream(
                    payload.getRecord().getContent()));
        } catch (SAXException e) {
            throw new IllegalArgumentException("could not parse record content "
                                               + "in " + payload, e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to process "
                                            + "ByteArrayInputStream for "
                                            + payload, e);
        }

        NamedNodeMap att = dom.getDocumentElement().getAttributes();
        IndexDefaults defaults = IndexUtils.getDefaults(att, SUMMA_NAMESPACE);

        // TODO: Use a pool of Documents so that they can be reused
        org.apache.lucene.document.Document luceneDoc =
                new org.apache.lucene.document.Document();
        // TODO: Check whether resolver is used for anything
        //noinspection DuplicateStringLiteralInspection
        luceneDoc.add(new Field("resolver", defaults.getResolver(),
                                Field.Store.YES, Field.Index.NO_NORMS));
        log.trace("Adding single fields to Lucene Document for " + payload);
        try {
            NodeList singleFields = (NodeList)
                    singleFieldXPathExpression.evaluate(dom,
                                                        XPathConstants.NODESET);
            makeIndexFields(existingFields, indexAnalyzer, descriptor,
                            singleFields, defaults, luceneDoc, null);
        } catch (XPathExpressionException e) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("Could not make single fields for " + payload
                     + ". The Lucene Document is probably incomplete");
        } catch (IndexServiceException e) {
            log.warn("Got IndexServiceException while processing single fields "
                     + "for " + payload);
        }

        log.trace("Adding grouped fields to Lucene Document for " + payload);
        try {
            NodeList group = (NodeList)
                    groupFieldXPathExpression.evaluate(dom,
                                                       XPathConstants.NODESET);
            makeGroups(existingFields, indexAnalyzer, descriptor, group,
                       defaults, luceneDoc, dom);
        } catch(XPathExpressionException e){
            //noinspection DuplicateStringLiteralInspection
            log.warn("Could not make grouped fields for " + payload
                     + ". The Lucene Document is probably incomplete");
        } catch (IndexServiceException e) {
            log.warn("Got IndexServiceException while processing group fields "
                     + "for " + payload);
        }
        payload.getData().put(Payload.LUCENE_DOCUMENT, luceneDoc);
        payload.getData().put(Payload.SEARCH_DESCRIPTOR, luceneDoc);
        log.debug("Added Lucene Document and SearchDescriptor to payload "
                  + payload + ". Processing time was "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    /* Taken and modified from IndexServiceImpl */
    private void makeIndexFields(Collection<String> existingFields,
                                 PerFieldAnalyzerWrapper indexAnalyzer,
                                 SearchDescriptor descriptor,
                                 NodeList fields, IndexDefaults defaults,
                                 org.apache.lucene.document.Document luceneDoc,
                                 String groupName) throws IndexServiceException{
        int len = fields.getLength();
        log.trace("makeIndexFields called with " + len + " fields");

        for (int i = 0; i < len; i++) {
            Node fieldNodes = fields.item(i);
            IndexField f = IndexUtils.makeField(fieldNodes, defaults, groupName,
                                                SUMMA_NAMESPACE);
            String value = IndexUtils.getElementNodeValue(fieldNodes);
            if (value == null) {
                continue;
            }
            value = value.trim();
            if ("".equals(value)) {
                continue;
            }
            Field newField = newField(existingFields, indexAnalyzer,
                                      f.getName(), value,
                                      f.getType(), f.isRepeat());
            newField.setBoost(f.getBoost());
            luceneDoc.add(newField);

            if (f.getGroup() != null && !"".equals(f.getGroup())){
                descriptor.addFieldToGroup(f, f.getGroup());
            } else {
                descriptor.addUnGroupedField(f);
            }
            if (f.isFreetext()){
                luceneDoc.add(newField(existingFields, indexAnalyzer,
                                       FREETEXT, " " + value,
                                       FieldType.freetext, true));
            }
        }
    }

    /* Taken and modified from IndexServiceImpl */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private Field newField(Collection<String> existingFields,
                           PerFieldAnalyzerWrapper indexAnalyzer,
                           String name, String val,
                           FieldType fieldType, boolean isRepeat) {
        if (!existingFields.contains(name)) {
            if (isRepeat && !(fieldType.getAnalyzer() instanceof
                    SummaKeywordAnalyzer)){
                indexAnalyzer.addAnalyzer(name, new SummaRepeatAnalyzer(
                        fieldType.getAnalyzer()));
                log.debug("newField: Adding name '" + name + "' using "
                          + fieldType.getAnalyzer().getClass().getName()
                          + " (analyzer wrapped in SummaRepeatAnalyzer): "
                          + val);
            } else {
                log.debug("newField: Adding name '" + name + "' using "
                          + fieldType.getAnalyzer().getClass().getName() + ": "
                          + val);
                indexAnalyzer.addAnalyzer(name, fieldType.getAnalyzer());
            }
            existingFields.add(name);
        }
        Field f = new Field(name, val, fieldType.getStore(),
                            fieldType.getIndex(), fieldType.getVector());
        if (log.isTraceEnabled()) {
            log.trace("Created new lucene field:" + f);
        }
        return f;
    }

    /* Taken and modified from IndexServiceImpl */
    private void makeGroups(Collection<String> existingFields,
                            PerFieldAnalyzerWrapper indexAnalyzer,
                            SearchDescriptor descriptor,
                            NodeList group,
                            IndexDefaults defaults,
                            org.apache.lucene.document.Document luceneDocument,
                            Document doc)
            throws IndexServiceException {
        int groupCount = group.getLength();

        for (int i = 0; i < groupCount; i++) {
            String gruppeNavn = null;
            String groupName;
            IndexDefaults gdef = new IndexDefaults();
            gdef.setBoost(defaults.getBoost());
            gdef.setFieldType(defaults.getFieldType());
            gdef.setFreeText(defaults.isFreeText());
            gdef.setGroup(defaults.isGroup());
            gdef.setIndex(defaults.isIndex());
            gdef.setSuggest(defaults.isSuggest());
            gdef.setType(defaults.getType());
            gdef.setResolver(defaults.getResolver());

            Node groupNode = group.item(i);
            NamedNodeMap attributes = groupNode.getAttributes();
            //noinspection DuplicateStringLiteralInspection
            Node suggest =
                    attributes.getNamedItemNS(SUMMA_NAMESPACE, "suggest");
            Node navn =
                    attributes.getNamedItemNS(SUMMA_NAMESPACE, "navn");
            Node name =
                    attributes.getNamedItemNS(SUMMA_NAMESPACE, "name");

            if (suggest != null) {
                gdef.setSuggest(Boolean.parseBoolean(suggest.getNodeValue()));
            }
            if (navn != null) {
                gruppeNavn = navn.getNodeValue();
            }

            if (name != null) {
                groupName = name.getNodeValue();
            } else {
                //noinspection DuplicateStringLiteralInspection
                throw new IndexServiceException("Found a group without a name");
            }

            //log.debug("got name for the group:" + groupName);

            if (gruppeNavn != null) {
                log.debug("makeGroups: generating group with name: " + groupName
                          + " and alias: " + gruppeNavn);
                IndexField.Alias alias = new IndexField.Alias(gruppeNavn, "da");
                ArrayList<IndexField.Alias> li =
                        new ArrayList<IndexField.Alias>(5);
                li.add(alias);
                descriptor.createGroup(groupName, li);
                //log.debug("done generating group");
            } else {
                log.debug("makeGroups: Generating non-aliased group with name: "
                          + groupName);

                descriptor.createGroup(groupName,
                                       new ArrayList<IndexField.Alias>(5));
                //log.debug("done generating group");
            }

            try {
                //noinspection DuplicateStringLiteralInspection
                NodeList nodes = (NodeList)
                        groupFields.evaluate(
                                "/in:document/in:fields/in:group[@in:name = '"
                                + groupName + "']/in:field",
                                doc, XPathConstants.NODESET);
                makeIndexFields(existingFields, indexAnalyzer, descriptor,
                                nodes, gdef, luceneDocument,groupName);
            } catch (XPathExpressionException e) {
                log.error(e);
            }
        }

    }

    /* ObjectFilter interface implementation */

    public boolean hasNext() {
        checkSource();
        return source.hasNext();
    }

    public Payload next() {
        checkSource();
        Payload payload = source.next();
        addDocument(payload);
        return payload;
    }

    public void remove() {
        log.warn("Remove() is unsupported");
    }

    public void setSource(Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Source filter was null");
        }
        if (!(filter instanceof ObjectFilter)) {
            throw new IllegalArgumentException("Only ObjectFilters accepted as "
                                               + "source. The filter provided "
                                               + "was of class "
                                               + filter.getClass());
        }
        source = (ObjectFilter)filter;
    }

    public boolean pump() throws IOException {
        checkSource();
        return hasNext() && next() != null;
    }

    public void close(boolean success) {
        checkSource();
    }

    private void checkSource() {
        if (source == null) {
            throw new IllegalStateException("No source defined for "
                                            + "CreateDocument filter");
        }
    }
}
