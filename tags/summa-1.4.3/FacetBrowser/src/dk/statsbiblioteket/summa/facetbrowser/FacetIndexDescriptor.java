/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.index.IndexGroup;
import dk.statsbiblioteket.summa.common.index.FieldProvider;
import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.net.URL;

/**
 * Extracts Facet-setup from IndexDescriptor-XML.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetIndexDescriptor extends IndexDescriptor<IndexField> {
    private static Logger log = Logger.getLogger(FacetIndexDescriptor.class);

    /* Do not set facets to null here, as they are assigned upon construction
       by the parse-method. Setting to null will override this assignment.
     */
    private Map<String, FacetStructure> facets;
    public static final String FACET_NAMESPACE = 
            "http://statsbiblioteket.dk/summa/2009/FacetIndexDescriptor";
    public static final String FACET_NAMESPACE_PREFIX = "fa";

    private final static String FACET_NODE_EXPR =
            "/" + DESCRIPTOR_NAMESPACE_PREFIX + ":IndexDescriptor/"
            + FACET_NAMESPACE_PREFIX + ":facets";
    private final static String FACET_EXPR = FACET_NAMESPACE_PREFIX + ":facet";

    private XPath facetXPath = null;

    public FacetIndexDescriptor(URL absoluteLocation) throws IOException {
        super(absoluteLocation);
    }

    public FacetIndexDescriptor(Configuration configuration) throws IOException{
        super(configuration);
    }

    public IndexField createNewField() {
        return new FacetField();
    }

    public IndexField createNewField(Node node) throws ParseException {
        return new FacetField(node, this);
    }

    private static class FacetField extends IndexField {
        private FacetField() {
        }
        private FacetField(Node node, FieldProvider fieldProvider) throws ParseException {
            super(node, fieldProvider);
        }
        @Override
        protected Object getDefaultIndexAnalyzer() {
            return new Object();
        }
        @Override
        protected Object getDefaultQueryAnalyzer() {
            return new Object();
        }
    }


    @Override
    public Document parse(String xml) throws ParseException {
        // TODO: Guard against changing facets (subsequent calls to parse)
        Document dom = super.parse(xml);
        if (dom == null) {
            throw new ParseException(String.format(
                    "The DOM from IndexDescriptor.parse(%s) was null", xml),
                                     -1);
        }
        NodeList facetNodes;
        try {
            Node facetNode = (Node)getXPath().evaluate(
                    FACET_NODE_EXPR, dom, XPathConstants.NODE);
            if (facetNode == null) {
                throw new ParseException(String.format(
                        "The XPath expression '%s' gave null. Apparently there " 
                        + "are no facets defined in the IndexDescriptor",
                        FACET_NODE_EXPR), -1);
            }
            facetNodes = (NodeList)getXPath().evaluate(
                    FACET_EXPR, facetNode, XPathConstants.NODESET);
        } catch (final XPathExpressionException e) {
            throw new ParseException(String.format(
                    "Expressions '%s' and '%s' were  invalid",
                    FACET_NODE_EXPR, FACET_EXPR), -1) {
                { initCause(e); } };
        } catch (final NullPointerException e) {
            log.warn(String.format(
                    "Unable to extracts facets with expressions '%s' and '%s' "
                    + " with name spaces '%s' and '%s' from xml:\n%s",
                    FACET_NODE_EXPR, FACET_EXPR,
                    DESCRIPTOR_NAMESPACE_PREFIX + ":"
                    + getXPath().getNamespaceContext().getNamespaceURI(
                            DESCRIPTOR_NAMESPACE_PREFIX),
                    FACET_NAMESPACE_PREFIX + ":"
                    + getXPath().getNamespaceContext().getNamespaceURI(
                            FACET_NAMESPACE_PREFIX),
                    xml), e);
            throw new ParseException(String.format(
                    "Got NullPointerException while evaluating node with "
                    + "expressions '%s' and '%s' and dom '%s'",
                    FACET_NODE_EXPR, FACET_EXPR, dom), -1) {{ initCause(e); }};
        }
        log.trace(String.format("Located %d facet nodes",
                                facetNodes.getLength()));
        Map<String, FacetStructure> facets =
                new LinkedHashMap<String, FacetStructure>(
                        facetNodes.getLength());
        for (int id = 0 ; id < facetNodes.getLength(); id++) {
            FacetStructure facet = parseFacet(facetNodes.item(id), id);
            facets.put(facet.getName(), facet);
        }
        this.facets = facets;
        return dom;
    }

    private FacetStructure parseFacet(Node node, int facetID) {
        String ref = null;
        String name = null;
        int maxTags = FacetStructure.DEFAULT_TAGS_MAX;
        int defaultTags = FacetStructure.DEFAULT_TAGS_WANTED;
        String sort = FacetStructure.DEFAULT_FACET_SORT_TYPE;
        String sortLocale = null;
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0 ; i < attributes.getLength() ; i++) {
            Node attribute = attributes.item(i);
            if (attribute.getLocalName().equals("ref")) {
                ref = attribute.getNodeValue();
            } else if (attribute.getLocalName().equals("name")) {
                name = attribute.getNodeValue();
            } else if (attribute.getLocalName().equals("maxTags")) {
                maxTags = Integer.parseInt(attribute.getNodeValue());
            } else if (attribute.getLocalName().equals("defaultTags")) {
                defaultTags = Integer.parseInt(attribute.getNodeValue());
            } else if (attribute.getLocalName().equals("sort")) {
                sort = attribute.getNodeValue();
                if (!sort.equals(FacetStructure.SORT_ALPHA)
                    && !sort.equals(FacetStructure.SORT_POPULARITY)) {
                    log.warn(String.format(
                            "Encountered unknown SORT value '%s' while parsing "
                            + "the node for Facet '%s' for field/group '%s'. "
                            + "Expected %s or %s",
                            sort, name, ref, FacetStructure.SORT_ALPHA,
                            FacetStructure.SORT_POPULARITY));
                }
            } else //noinspection DuplicateStringLiteralInspection
                if (attribute.getLocalName().equals("sortLocale")) {
                sortLocale = attribute.getNodeValue();
            } else {
                    log.info(String.format(
                            "Unknown attribute '%s' in facet definition '%s'",
                            attribute.getLocalName(), name));
                }
        }
        if (ref == null || "".equals(ref)) {
            throw new ConfigurationException(String.format(
                    "No ref specified in facet '%s'", name));
        }
        if (name == null || "".equals(name)) {
            name = ref;
        }
        IndexField field = getField(ref);
        IndexGroup<IndexField> group = getGroup(ref);
        if (field == null && group == null) {
            throw new ConfigurationException(String.format(
                    "No field or group defined for ref '%s' in facet '%s'",
                    ref, name));
        }
        List<IndexField> fieldRefs =
                group == null ? Arrays.asList(field) :
                new ArrayList<IndexField>(group.getFields());
        List<String> fieldNames = new ArrayList<String>(fieldRefs.size());
        for (IndexField fieldRef : fieldRefs) {
            fieldNames.add(fieldRef.getName());
        }
        return new FacetStructure(name, facetID, fieldNames.toArray(new String[
                fieldNames.size()]), defaultTags, maxTags, sortLocale, sort);
    }

    /**
     * @return an ordered map of the facets extracted from the index descriptor.
     */
    public Map<String, FacetStructure> getFacets() {
        if (facets == null) {
            log.error("getFacets(): Facets has not been parsed. Null returned");
        }
        return facets;
    }

    private XPath getXPath() {
        if (facetXPath == null) {
            log.trace("Creating XPath for FacetIndexDescriptor");
            DefaultNamespaceContext nsCon = new DefaultNamespaceContext();
            nsCon.setNameSpace(DESCRIPTOR_NAMESPACE,
                               DESCRIPTOR_NAMESPACE_PREFIX);
            nsCon.setNameSpace(FACET_NAMESPACE,
                               FACET_NAMESPACE_PREFIX);
            XPathFactory factory = XPathFactory.newInstance();
            facetXPath = factory.newXPath();
            facetXPath.setNamespaceContext(nsCon);

        }
        return facetXPath;
    }
}
