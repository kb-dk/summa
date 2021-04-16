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
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.index.FieldProvider;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.index.IndexGroup;
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
import java.net.URL;
import java.text.ParseException;
import java.util.*;

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
    public static final String FACET_NAMESPACE = "http://statsbiblioteket.dk/summa/2009/FacetIndexDescriptor";
    public static final String FACET_NAMESPACE_PREFIX = "fa";

    /*private final static String FACET_NODE_EXPR =
            "/" + DESCRIPTOR_NAMESPACE_PREFIX + ":IndexDescriptor/"
            + FACET_NAMESPACE_PREFIX + ":facets";*/
    private final static String FACET_NODE_EXPR = "/IndexDescriptor/facets";
    //private final static String FACET_EXPR = FACET_NAMESPACE_PREFIX + ":facet";
    private final static String FACET_EXPR = "facet";

    private XPath facetXPath = null;

    public FacetIndexDescriptor(URL absoluteLocation) throws IOException {
        super(absoluteLocation);
    }

    public FacetIndexDescriptor(Configuration configuration) throws IOException{
        super(configuration);
    }

    @Override
    public IndexField createNewField() {
        return new FacetField();
    }

    @Override
    public IndexField getFieldWithLocale(String instanceName, String fieldName, String locale)
            throws IllegalArgumentException {
        return getField(fieldName);
    }

    @Override
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
            throw new ParseException(String.format(Locale.ROOT, "The DOM from IndexDescriptor.parse(%s) was null", xml), -1);
        }
        NodeList facetNodes;
        try {
            Node facetNode = (Node)getXPath().evaluate(FACET_NODE_EXPR, dom, XPathConstants.NODE);
            if (facetNode == null) {
                throw new ParseException(String.format(Locale.ROOT,
                        "The XPath expression '%s' gave null. Apparently there are no facets defined in the "
                        + "IndexDescriptor",
                        FACET_NODE_EXPR), -1);
            }
            facetNodes = (NodeList)getXPath().evaluate(FACET_EXPR, facetNode, XPathConstants.NODESET);
        } catch (final XPathExpressionException e) {
            throw new ParseException(String.format(Locale.ROOT, "Expressions '%s' and '%s' were  invalid",
                                                   FACET_NODE_EXPR, FACET_EXPR), -1) {
                private static final long serialVersionUID = 89785489484L;
                { initCause(e); } };
        } catch (final NullPointerException e) {
            log.warn(String.format(Locale.ROOT,
                    "Unable to extracts facets with expressions '%s' and '%s'  with name spaces '%s' and '%s' "
                    + "from xml:\n%s",
                    FACET_NODE_EXPR, FACET_EXPR,
                    DESCRIPTOR_NAMESPACE_PREFIX + ":"
                    + getXPath().getNamespaceContext().getNamespaceURI(DESCRIPTOR_NAMESPACE_PREFIX),
                    FACET_NAMESPACE_PREFIX + ":"
                    + getXPath().getNamespaceContext().getNamespaceURI(FACET_NAMESPACE_PREFIX),
                    xml), e);
            throw new ParseException(String.format(Locale.ROOT,
                    "Got NullPointerException while evaluating node with expressions '%s' and '%s' and dom '%s'",
                    FACET_NODE_EXPR, FACET_EXPR, dom), -1) {
                private static final long serialVersionUID = 79877384469L;
                { initCause(e); }};
        }
        log.trace(String.format(Locale.ROOT, "Located %d facet nodes", facetNodes.getLength()));
        Map<String, FacetStructure> facets = new LinkedHashMap<>(facetNodes.getLength());
        for (int id = 0 ; id < facetNodes.getLength(); id++) {
            FacetStructure facet = parseFacet(facetNodes.item(id), id);
            facets.put(facet.getName(), facet);
            if (log.isDebugEnabled()) {
                log.debug("Extracted facet-structure from IndexDescriptor: " + facet);
            }
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
        boolean reverse = false;

        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0 ; i < attributes.getLength() ; i++) {
            Node attribute = attributes.item(i);
            if ("ref".equals(attribute.getNodeName())) {
                ref = attribute.getNodeValue();
            } else if ("name".equals(attribute.getNodeName())) {
                name = attribute.getNodeValue();
            } else if ("maxTags".equals(attribute.getNodeName())) {
                maxTags = Integer.parseInt(attribute.getNodeValue());
            } else if ("defaultTags".equals(attribute.getNodeName())) {
                defaultTags = Integer.parseInt(attribute.getNodeValue());
            } else if ("reverse".equals(attribute.getNodeName())) {
                reverse = Boolean.parseBoolean(attribute.getNodeValue());
            } else if ("sort".equals(attribute.getNodeName())) {
                sort = attribute.getNodeValue();
                if (!sort.equals(FacetStructure.SORT_ALPHA)
                    && !sort.equals(FacetStructure.SORT_ALPHA_DESC)
                    && !sort.equals(FacetStructure.SORT_POPULARITY)
                    && !sort.equals(FacetStructure.SORT_POPULARITY_ASC)) {
                    log.warn(String.format(Locale.ROOT,
                            "Encountered unknown SORT value '%s' while parsing the node for Facet '%s' for field/group "
                            + "'%s'. Expected %s, %s, %s or %s",
                            sort, name, ref, FacetStructure.SORT_ALPHA, FacetStructure.SORT_ALPHA_DESC,
                            FacetStructure.SORT_POPULARITY, FacetStructure.SORT_POPULARITY_ASC));
                }
            } else //noinspection DuplicateStringLiteralInspection
                if ("sortLocale".equals(attribute.getNodeName())) {
                sortLocale = attribute.getNodeValue();
            } else {
                    log.info(String.format(Locale.ROOT, "Unknown attribute '%s' in facet definition '%s'",
                                           attribute.getNodeName(), name));
                }
        }
        if (ref == null || "".equals(ref)) {
            throw new ConfigurationException(String.format(Locale.ROOT, "No ref specified in facet '%s'", name));
        }
        if (name == null || "".equals(name)) {
            name = ref;
        }
        IndexField field = getField(ref);
        IndexGroup<IndexField> group = getGroup(ref);
        if (field == null && group == null) {
            throw new ConfigurationException(String.format(Locale.ROOT, "No field or group defined for ref '%s' in facet '%s'",
                                                           ref, name));
        }
        List<IndexField> fieldRefs = group == null ? Arrays.asList(field) :
                                     new ArrayList<>(group.getFields());
        List<String> fieldNames = new ArrayList<>(fieldRefs.size());
        for (IndexField fieldRef : fieldRefs) {
            fieldNames.add(fieldRef.getName());
        }
        return new FacetStructure(name, facetID, fieldNames.toArray(new String[fieldNames.size()]), defaultTags,
                                  maxTags, sortLocale, sort, reverse);
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
            nsCon.setNameSpace(DESCRIPTOR_NAMESPACE, DESCRIPTOR_NAMESPACE_PREFIX);
            nsCon.setNameSpace(FACET_NAMESPACE, FACET_NAMESPACE_PREFIX);
            XPathFactory factory = XPathFactory.newInstance();
            facetXPath = factory.newXPath();
            facetXPath.setNamespaceContext(nsCon);
        }
        return facetXPath;
    }
}

