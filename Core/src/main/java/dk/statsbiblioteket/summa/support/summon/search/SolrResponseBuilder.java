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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ConvenientMap;
import dk.statsbiblioteket.summa.common.util.SimplePair;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;

/**
 * Takes a Sold response and converts it to a {@link dk.statsbiblioteket.summa.search.api.document.DocumentResponse} and
 * a {@link org.apache.lucene.search.exposed.facet.FacetResponse}.
 * Mapping between field & facet names as well as score adjustments or tag tweaking is not in the scope of this
 * converter.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrResponseBuilder implements Configurable {
    private static Log log = LogFactory.getLog(SolrResponseBuilder.class);
    /**
     * Recommended behaviour for a Summa document searcher is to provide a
     * recordBase as this makes backtracking easier and allows for optimized
     * lookups.
     * </p><p>
     * Specifying a recordBase results in the field 'recordBase' being added
     * to records in DocumentResponses.
     * </p><p>
     * Optional. Default is 'summon'.
     */
    public static final String CONF_RECORDBASE = "solrresponsebuilder.recordbase";
    public static final String DEFAULT_RECORDBASE = "solr";
    /**
     * A 1:1 map of field names to redirect. If sorting of field A is requested,
     * specifying "A - B" means that the content of field B will be used as the
     * sort value instead of the content from field A.
     * </p><p>
     * The format is a list of rules (comma-separation works) with each rule
     * defines as "A - B", where A is the sort field and B is the field to
     * extract sort values from.
     * </p><p>
     * Optional. Default is "PublicationDate - PublicationDate_xml_iso".
     * </p><p>
     * Note: The default mapping is chosen because summon provides date-sorting
     * with sort-option "PublicationDate", but the field "PublicationDate" is
     * multi valued and not normalised. The field PublicationDate_xml_iso is
     * an extracted ISO-date from that authoritative XML in the single value
     * field PublicationDate_xml.
     */
    public static final String CONF_SORT_FIELD_REDIRECT = "solrresponsebuilder.sort.field.redirect";

    protected XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // No resolving of external DTDs
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }

    protected String recordBase = DEFAULT_RECORDBASE;
    protected String searcherID;
    protected Map<String, String> sortRedirect;
    protected Set<String> nonescapedFields = new HashSet<String>(10);

    public SolrResponseBuilder(Configuration conf) {
        recordBase = conf.getString(CONF_RECORDBASE, recordBase);
        searcherID = conf.getString(SearchNodeImpl.CONF_ID, recordBase);
        if ("".equals(recordBase)) {
            recordBase = null;
        }
        List<String> rules = conf.getStrings(CONF_SORT_FIELD_REDIRECT, new ArrayList<String>());
        sortRedirect = new HashMap<String, String>(rules.size());
        for (String rule: rules) {
            String[] tokens = rule.split(" *- *");
            if (tokens.length != 2) {
                throw new ConfigurationException(
                    "Error parsing sort field redirect rule '" + rule + "'. "
                    + "Expected a rule with the format 'source - destination'");
            }
            sortRedirect.put(tokens[0], tokens[1]);
        }
        log.info("Created SolrResponseBuilder " + searcherID + " with base '" + recordBase + "' and sort field "
                 + "redirect rules '" + Strings.join(rules, ", ") + "'");
    }

    public long buildResponses(Request request, final SolrFacetRequest facets, final ResponseCollection responses,
                               String solrResponse, String solrTiming) throws XMLStreamException {
//        System.out.println(solrResponse.replace(">", ">\n"));
        long startTime = System.currentTimeMillis();
        log.debug("buildResponses(...) called");
        XMLStreamReader xml;
        try {
            xml = xmlFactory.createXMLStreamReader(new StringReader(solrResponse));
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Unable to construct a reader from input for request " + request, e);
        }
        if (!findTagStart(xml, "response")) {
            log.warn("Could not locate start tag 'response', exiting parsing of response for " + request);
            return 0;
        }
        xml.next();
        final DocumentResponse documentResponse = createBasicDocumentResponse(request);
        final boolean mlt = request.getBoolean(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID, null) != null;
        iterateTags(xml, new Callback() {
            @Override
            public boolean tagStart(
                XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if (mlt && "lst".equals(current) && "moreLikeThis".equals(getAttribute(xml, "name", null))) {
                    log.debug("Parsing MoreLikeThis response");
                    parseResponse(xml, documentResponse);
                    // TODO: Remove query-id
                    return true;
                }
                if (!mlt && "result".equals(current)) {
                    String name = getAttribute(xml, "name", null);
                    if (name == null) {
                        log.warn("Expected attribute 'name' in tag result. Skipping content for result");
                        skipSubTree(xml);
                        return true;
                    }
                    if ("response".equals(name)) {
                        parseResponse(xml, documentResponse);
                        // Cursor is at end of sub tree after parseHeader
                        return true;
                    }
                    log.debug("Encountered unsupported name in response '" + name + "'. Skipping element");
                    skipSubTree(xml);
                    return true;
                }
                if ("lst".equals(current)) {
                    String name = getAttribute(xml, "name", null);
                    xml.next();
                    if (name == null) {
                        log.warn("Expected attribute 'name' in tag lst. Skipping content for lst");
                        skipSubTree(xml);
                        return true;
                    }
                    if ("responseHeader".equals(name)) {
                        parseHeader(xml, documentResponse);
                        // Cursor is at end of sub tree after parseHeader
                        return true;
                    }
                    if ("facet_counts".equals(name)) {
                        parseFacets(xml, facets, responses);
                        // Cursor is at end of sub tree after parseHeader
                        return true;
                    }
                    log.debug("Encountered unsupported name in lst '" + name + "' in Solr response. Skipping element");
                    skipSubTree(xml);
                    return true;
                }
                log.warn("Encountered unexpected tag '" + current + "' in Solr response. Skipping element");
                return false;
            }
        });

        documentResponse.addTiming("reportedtime", documentResponse.getSearchTime());
        documentResponse.addTiming("buildresponses.total", System.currentTimeMillis() - startTime);
        documentResponse.addTiming(solrTiming);
        responses.add(documentResponse);
        return documentResponse.getHitCount();
    }

    private void parseFacets(
        XMLStreamReader xml, SolrFacetRequest facets, ResponseCollection responses) throws XMLStreamException {
        long startTime = System.currentTimeMillis();
        HashMap<String, Integer> facetIDs = new HashMap<String, Integer>(facets.getFacets().size());
        // 1 facet = 1 field in Solr-world
        HashMap<String, String[]> fields = new HashMap<String, String[]>(facets.getFacets().size());
        for (int i = 0 ; i < facets.getFacets().size() ; i++) {
            SolrFacetRequest.Facet facet = facets.getFacets().get(i);
            facetIDs.put(facet.getField(), i);
            // TODO: Consider displayname
            fields.put(facet.getField(), new String[]{facet.getField()});
        }
        final FacetResultExternal facetResult = new FacetResultExternal(
            facets.getMaxTags(), facetIDs, fields, facets.getOriginalStructure());
        facetResult.setPrefix(searcherID + ".");
        iterateTags(xml, new Callback() {
            @Override
            public boolean tagStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if ("facet_fields".equals(getAttribute(xml, "name", null))) {
                    xml.next();
                    parseFacets(xml, facetResult);
                    return true;
                }
                // TODO: <lst name="facet_dates"/> <lst name="facet_ranges"/>
                return false;
            }
        });
        facetResult.sortFacets();
        facetResult.addTiming("buildresponses.facets", System.currentTimeMillis() - startTime);
        responses.add(facetResult);
    }

    private void parseFacets(XMLStreamReader xml, final FacetResultExternal facets) throws XMLStreamException {
        iterateTags(xml, new Callback() {
            @Override
            public boolean tagStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if (!"lst".equals(current)) {
                    log.warn("Encountered tag '" + current + "' in facet_fields. Expected 'lst'. Ignoring element");
                    return false;
                }
                final String facetName = getAttribute(xml, "name", null);
                if (facetName == null) {
                    log.warn("Encountered tag 'lst' inside facet_fields without attribute name. Ignoring element");
                    return false;
                }
                xml.next();
                parseFacet(xml, facets, facetName);
                return true;
            }
        });
    }

    private void parseFacet(
        XMLStreamReader xml, final FacetResultExternal facets, final String facetName) throws XMLStreamException {
        iterateTags(xml, new Callback() {
            @Override
            public boolean tagStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                final String tagName = getAttribute(xml, "name", null);
                if (tagName == null) {
                    log.warn("Encountered tag type '" + current + "' inside facet '" + facetName
                             + "' without attribute name. Ignoring element");
                    return false;
                }
                String content = xml.getElementText();
                try {
                    int count = Integer.parseInt(content);
                    facets.addTag(facetName, tagName, count);
                    log.trace("Added tag " + facetName + ":" + tagName + "(" + count + ")");
                } catch (NumberFormatException e) {
                    log.warn("Encountered tag '" + tagName + "' inside facet '" + facetName
                             + "' with un-parsable count '" + content + "'. Ignoring tag");
                }
                return true;
            }
        });
    }

    private void skipSubTree(XMLStreamReader xml) throws XMLStreamException {
        iterateTags(xml, new Callback() {
            @Override
            public boolean tagStart(
                XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                return false; // Ignore everything until end of sub tree
            }
        });
    }

    private void parseHeader(XMLStreamReader xml, final DocumentResponse response) throws XMLStreamException {
        log.trace("parseHeader(...) called");
        iterateTags(xml, new Callback() {
            @Override
            public boolean tagStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if ("int".equals(current) && "QTime".equals(getAttribute(xml, "name", null))) {
                    String content = xml.getElementText();
                    try {
                        response.setSearchTime(Long.parseLong(content));
                    } catch (NumberFormatException e) {
                        log.warn("Unable to parse '" + content + " as long in lst#responseHeader/int#QTime");
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void parseResponse(XMLStreamReader xml, final DocumentResponse response) throws XMLStreamException {
        log.trace("parseResponse(...) called");
        response.setHitCount(Long.parseLong(getAttribute(xml, "numFound", "-1")));
        xml.next();
        iterateTags(xml, new Callback() {
            @Override
            public boolean tagStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if ("doc".equals(current)) {
                    xml.next();
                    parseDoc(xml, response);
                    return true;
                }
                return false;
            }
        });
    }

    private void parseDoc(XMLStreamReader xml, final DocumentResponse response) throws XMLStreamException {
        final String sortKey = response.getSortKey() == null || response.getSortKey().equals(DocumentKeys.SORT_ON_SCORE)
                               ? null : response.getSortKey();

        log.trace("parseDoc(...) called");
        iterateTags(xml, new Callback() {
            float score = 0.0f;
            String id = null;
            List<SimplePair<String, String>> fields = new ArrayList<SimplePair<String, String>>(100);

            @Override
            public boolean tagStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                String name = getAttribute(xml, "name", null);
                if (name == null) {
                    log.warn("Encountered tag '" + current + "' without expected attribute 'name'. Skipping");
                    return false;
                }
                String content;
                try {
                    content = xml.getElementText();
                } catch (XMLStreamException e) {
                    log.warn("Exception while reading text in element '" + name + "' for document '" + id
                             + "'. Probable cause is embedded XML", e);
                    return true;

                }
                if (content.length() == 0) {
                    log.debug("Content for " + current + "#" + name + " for document " + id + " was empty. Skipping");
                    return true;
                }

                if ("score".equals(name)) {
                    try {
                        score = Float.parseFloat(content);
                    } catch (NumberFormatException e) {
                        log.warn("Unable to parse float score '" + content + "' from "+ current + "#" + name
                                 + " for document " + id + ". Score will be set to " + score);
                    }
                } else if ("id".equals(name)) {
                    id = content;
                }
                fields.add(new SimplePair<String, String>(name, content));
                return true;
            }

            @Override
            public void end() {
                if (id == null) {
                    log.warn("id was undefined after ");
                }
                fields.add(new SimplePair<String, String>(DocumentKeys.RECORD_ID, id));
                // TODO: Cons
/*                if (recordBase != null) {
                    fields.add(new SimplePair<String, String>(DocumentKeys.RECORD_BASE, recordBase));
                }*/
                String sortValue = null;
                if (sortKey == null) {
                    sortValue = Float.toString(score);
                } else {
                    for (SimplePair<String, String> field: fields) {
                        if (sortKey.equals(field.getKey())) {
                            sortValue = field.getValue();
                            break;
                        }
                    }
                    if ("".equals(sortValue)) {
                        log.debug("Unable to extract sortValue for key '" + sortKey + "' from document " + id);
                    }
                }
                DocumentResponse.Record record = new DocumentResponse.Record(id, searcherID, score, sortValue);
                for (SimplePair<String, String> field: fields) {
                    record.addField(new DocumentResponse.Field(
                        field.getKey(), field.getValue(), !nonescapedFields.contains(field.getKey())));
                }
                if (log.isTraceEnabled()) {
                    log.debug("constructed and added " + record);
                }
                response.addRecord(record);
            }
        });
    }

    // Iterates through the start tags in the stream until the current sub tree in the DOM is depleted
    // Leaves the cursor after END_ELEMENT
    protected void iterateTags(XMLStreamReader xml, Callback callback) throws XMLStreamException {
        List<String> tagStack = new ArrayList<String>(10);
        while (true) {
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT) {
                String currentTag = xml.getLocalName();
                tagStack.add(currentTag);
                if (!callback.tagStart(xml, tagStack, currentTag)) {
                    xml.next();
                }
                continue;
            }
            if (xml.getEventType() == XMLStreamReader.END_ELEMENT) {
                String currentTag = xml.getLocalName();
                if (tagStack.size() == 0) {
                    callback.end();
                    return;
                }
                if (!currentTag.equals(tagStack.get(tagStack.size()-1))) {
                    throw new IllegalStateException(String.format(
                        "Encountered end tag '%s' where '%s' from the stack %s were expected",
                        currentTag, tagStack.get(tagStack.size()-1), Strings.join(tagStack, ", ")));
                }
                tagStack.remove(tagStack.size()-1);
            } else if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                callback.end();
                return;
            }
            xml.next();
        }
    }

    private abstract static class Callback {
        /**
         * Called for each encountered START_ELEMENT in the part of the xml that is within scope. If the implementation
         * calls {@code xml.next()} or otherwise advances the position in the stream, it must ensure that the list of
         * tags is consistent with the position in the DOM.
         *
         * @param xml        the Stream.
         * @param tags       the start tags encountered in the current sub tree.
         * @param current    the local name of the current tag.
         * @return true if the implementation called {@code xml.next()} one or more times, else false.
         */
        public abstract boolean tagStart(
            XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException;

        /**
         * Called when the last END_ELEMENT is encountered.
         */
        public void end() { }
    }

    /**
     * Skips everything until a start tag is reacted or the readers is depleted.
     * @param xml the stream to iterate over.
     * @return the name of the start tag or null if EOD.
     * @throws javax.xml.stream.XMLStreamException if there was an error
     * accessing the xml stream.
     */
    protected String jumpToNextTagStart(XMLStreamReader xml)
        throws XMLStreamException {
        if (xml.getEventType() == XMLStreamReader.START_ELEMENT) {
            xml.next(); // Skip if already located at a start
        }
        while (true) {
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT) {
                return xml.getLocalName();
            }
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                return null;
            }
            xml.next();
        }
    }

    /**
     *
     * @param xml stream positioned at a start tag.
     * @param attributeName the wanted attribute
     * @param defaultValue the value to return if the attributes is not present.
     * @return the attribute content og the default value.
     */
    protected String getAttribute(XMLStreamReader xml, String attributeName, String defaultValue) {
        for (int i = 0 ; i < xml.getAttributeCount() ; i++) {
            if (xml.getAttributeLocalName(i).equals(attributeName)) {
                return xml.getAttributeValue(i);
            }
        }
        return defaultValue;
    }

    /**
     * Iterates over the xml until a start tag with startTagName is reached.
     * @param xml          the stream to iterate over.
     * @param startTagName the name of the tag to locate.
     * @return true if the tag was found, else false.
     * @throws javax.xml.stream.XMLStreamException if there were an error
     * seeking the xml stream.
     */
    protected boolean findTagStart(XMLStreamReader xml, String startTagName) throws XMLStreamException {
        while (true)  {
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT && startTagName.equals(xml.getLocalName())) {
                return true;
            }
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                return false;
            }
            try {
                xml.next();
            } catch (XMLStreamException e) {
                throw new XMLStreamException("Error seeking to start tag for element '" + startTagName + "'", e);
            }
        }
    }

    /**
     * Iterates over elements in the stream until end element is encountered
     * or end of document is reached. For each element matching actionElement,
     * callback is called.
     * @param xml        the stream to iterate.
     * @param endElement the stopping element.
     * @param actionElement callback is activated when encountering elements
     *                   with this name.
     * @param callback   called for each encountered element.
     * @throws javax.xml.stream.XMLStreamException if the stream could not
     * be iterated or an error occured during callback.
     */
    protected void iterateElements(XMLStreamReader xml, String endElement, String actionElement, XMLCallback callback)
        throws XMLStreamException {
        while (true) {
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT ||
                (xml.getEventType() == XMLStreamReader.END_ELEMENT && xml.getLocalName().equals(endElement))) {
                break;
            }
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT && xml.getLocalName().equals(actionElement)) {
                callback.execute(xml);
            }
            xml.next();
        }
    }

    protected DocumentResponse createBasicDocumentResponse(Request request) {
        String query =    request.getString( DocumentKeys.SEARCH_QUERY, null);
        String filter =   request.getString( DocumentKeys.SEARCH_FILTER, null);
        int startIndex =  request.getInt(    DocumentKeys.SEARCH_START_INDEX, 0);
        int maxRecords =  request.getInt(    DocumentKeys.SEARCH_MAX_RECORDS, 0);
        String sortKey =  request.getString( DocumentKeys.SEARCH_SORTKEY, null);
        boolean reverse = request.getBoolean(DocumentKeys.SEARCH_REVERSE, false);
        DocumentResponse response = new DocumentResponse(
            filter, query, startIndex, maxRecords, sortKey, reverse, new String[0], -1, -1);
        response.setPrefix(searcherID + ".");
        return response;
    }

    /**
     * Creates a Summa shortformat based on the given values.
     * @param extracted values extracted from a Solr response. Recognized keys are Title, Subtitle, Author, contentType
     *                  and Date. All values are single except Author which can contain multiple Authors, delimited by
     *                  newline.
     * @return a shortformat conforming to Summa standard.
     */
    protected String createShortformat(ConvenientMap extracted) {
      // TODO: Incorporate this properly instead of String-hacking
        final StringBuffer shortformat = new StringBuffer(500);
        shortformat.append("  <shortrecord>\n");
        shortformat.append("    <rdf:RDF xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
                           + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n");
        shortformat.append("      <rdf:Description>\n");
        shortformat.append("        <dc:title>").append(XMLUtil.encode(extracted.getString("Title", "")));
        String subTitle = extracted.getString("Subtitle", "");
        if (!"".equals(subTitle)) {
            shortformat.append(" : ").append(XMLUtil.encode(subTitle));
        }
        shortformat.append("</dc:title>\n");
        addMultiple(extracted, shortformat, "        ", "dc:creator", "Author");
        shortformat.append("        <dc:type xml:lang=\"da\">").
            append(XMLUtil.encode(extracted.getString("ContentType", ""))).
            append("</dc:type>\n");
        shortformat.append("        <dc:type xml:lang=\"en\">").
            append(XMLUtil.encode(extracted.getString("ContentType", ""))).
            append("</dc:type>\n");
        shortformat.append("        <dc:date>").
            append(XMLUtil.encode(extracted.getString("Date", "???"))).append("</dc:date>\n");
        shortformat.append("        <dc:format></dc:format>\n");
        shortformat.append("      </rdf:Description>\n");
        shortformat.append("    </rdf:RDF>\n");
        shortformat.append("  </shortrecord>\n");
        return shortformat.toString();
    }

    protected void addMultiple(
        ConvenientMap extracted, StringBuffer shortformat, String indent, String tag, String field) {
        String[] elements = extracted.getString(field, "").split("\n");
        for (String element: elements) {
            if (!"".equals(element)) {
                shortformat.append(String.format("%s<%s>%s</%s>\n", indent, tag, XMLUtil.encode(element), tag));
            }
        }
    }

    protected abstract static class XMLCallback {
        public abstract void execute(XMLStreamReader xml) throws XMLStreamException;
        public void close() { } // Called when iteration has finished
    }

    public void setNonescapedFields(Set<String> nonescapedFields) {
        this.nonescapedFields = new HashSet<String>(nonescapedFields);
        log.debug("setNonescapedFields(" + Strings.join(nonescapedFields, ", ") + ")");
    }
}
