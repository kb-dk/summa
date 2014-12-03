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
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.util.ConvenientMap;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.common.util.SimplePair;
import dk.statsbiblioteket.summa.facetbrowser.api.*;
import dk.statsbiblioteket.summa.facetbrowser.browse.IndexRequest;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.api.DidYouMeanKeys;
import dk.statsbiblioteket.summa.support.api.DidYouMeanResponse;
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.summa.support.solr.SolrSearchNode;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLStepper;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.exposed.ExposedIndexLookupParams;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Takes a Solr response and converts it to a {@link dk.statsbiblioteket.summa.search.api.document.DocumentResponse} and
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
     * Specifying a recordBase results in the field 'recordBase' being added to records in DocumentResponses if there
     * was no recordBase extracted during document processing.
     * </p><p>
     * Optional. Default is 'solr'. Specifying the empty String means that no recordBase will be assigned.
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

    /**
     * The field containing the unique ID in the Solr response.
     * </p><p>
     * Optional. Default is recordID. If the field is not present in the Solr response, an error will be logged.
     */
    public static final String CONF_ID_FIELD = "solr.field.id";
    public static final String DEFAULT_ID_FIELD = IndexUtils.RECORD_FIELD;

    /**
     * The field containing the recordBase in the Solr response.
     * </p><p>
     * Optional. Default is recordBase. If the field is not present in the Solr response, the generated result will
     * be assigned the base from {@link #CONF_RECORDBASE}.
     * </p>
     */
    public static final String CONF_BASE_FIELD = "solr.field.base";
    public static final String DEFAULT_BASE_FIELD = IndexUtils.RECORD_BASE;

    /**
     * If grouping is active and there are no group value (no vale for the group.field), setting this to true means that each such entry
     * will be delivered as a separate group. If false, they will be collapsed in a "null"-group.
     * </p><p>
     * Optional. Default is true;
     */
    public static final String CONF_GROUP_NULL_AS_SEPARATE = "group.nullasseparate";
    public static final boolean DEFAULT_GROUP_NULL_AS_SEPARATE = true;

    protected XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // No resolving of external DTDs
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }

    protected final String recordBase;
    protected final String searcherID;
    protected Map<String, String> sortRedirect;
    protected Set<String> nonescapedFields = new HashSet<>(10);
    protected IndexRequest defaultIndexRequest;
    protected final String idField;
    protected final String baseField;
    protected final boolean groupNullAsSeparate;

    public SolrResponseBuilder(Configuration conf) {
        recordBase = "".equals(conf.getString(CONF_RECORDBASE, DEFAULT_RECORDBASE)) ?
                     null : conf.getString(CONF_RECORDBASE, DEFAULT_RECORDBASE);
        idField = conf.getString(CONF_ID_FIELD, DEFAULT_ID_FIELD);
        baseField = conf.getString(CONF_BASE_FIELD, DEFAULT_BASE_FIELD);
        searcherID = conf.getString(SearchNodeImpl.CONF_ID, recordBase);
        List<String> rules = conf.getStrings(CONF_SORT_FIELD_REDIRECT, new ArrayList<String>());
        sortRedirect = new HashMap<>(rules.size());
        for (String rule: rules) {
            String[] tokens = rule.split(" *- *");
            if (tokens.length != 2) {
                throw new ConfigurationException(
                    "Error parsing sort field redirect rule '" + rule + "'. "
                    + "Expected a rule with the format 'source - destination'");
            }
            sortRedirect.put(tokens[0], tokens[1]);
        }
        defaultIndexRequest = new IndexRequest(conf);
        groupNullAsSeparate = conf.getBoolean(CONF_GROUP_NULL_AS_SEPARATE, DEFAULT_GROUP_NULL_AS_SEPARATE);
        log.info("Created SolrResponseBuilder " + searcherID + " with base '" + recordBase + "' and sort field "
                 + "redirect rules '" + Strings.join(rules, ", ") + "'");
    }

    public long buildResponses(final Request request, final SolrFacetRequest facets, final ResponseCollection responses,
                               String solrResponse, String solrTiming) throws XMLStreamException {
/*        System.out.println("***");
        System.err.println(request);
        System.out.println("***");
        System.out.println(solrResponse.replace(">", ">\n"));
*/
        long startTime = System.currentTimeMillis();
        log.debug("buildResponses(...) called");
        XMLStreamReader xml;
        try {
            xml = xmlFactory.createXMLStreamReader(new StringReader(solrResponse));
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Unable to construct a reader from input for request " + request, e);
        }
        if (!XMLStepper.findTagStart(xml, "response")) {
            log.warn("Could not locate start tag 'response', exiting parsing of response for " + request);
            return 0;
        }
        /*
        <response>
        <lst name="responseHeader">...</lst>
        <result name="response" numFound="1" start="0" maxScore="0.5291085">
          <doc>*
        </result>
        </response>
         */
        final DocumentResponse documentResponse = createBasicDocumentResponse(request);
        documentResponse.addTiming(solrTiming); // No matter what, we want the timing
        final boolean mlt = request.getBoolean(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID, null) != null;
        XMLStepper.jumpToNextTagStart(xml); // Inside response
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(
                XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                // MLT-results are just plain results(?)
                if (mlt && "lst".equals(current) && "moreLikeThis".equals(XMLStepper.getAttribute(xml, "name", null))) {
                    log.debug("Parsing MoreLikeThis response");
                    parseMLTResponse(xml, documentResponse);
                    // TODO: Remove query-id
                    return true;
                }
                if ("result".equals(current)) {
                    if (mlt) {
                        log.debug("Skipping plain document response as mlt=true");
                        XMLStepper.findTagEnd(xml, "result");
                        return true;
                    }
                    log.debug("Parsing document response");
                    String name = XMLStepper.getAttribute(xml, "name", null);
                    if (name == null) {
                        log.warn("Expected attribute 'name' in tag result. Skipping content for result");
                        XMLStepper.skipSubTree(xml);
                        return true;
                    }
                    if ("response".equals(name)) {
                        parseResponse(xml, documentResponse);
                        // Cursor is at end of sub tree after parseHeader
                        return true;
                    }
                    log.debug("Encountered unsupported name in response '" + name + "'. Skipping element");
                    XMLStepper.skipSubTree(xml);
                    return true;
                }
                if ("lst".equals(current)) {
                    String name = XMLStepper.getAttribute(xml, "name", null);
                    xml.next();
                    if (name == null) {
                        log.warn("Expected attribute 'name' in tag lst. Skipping content for lst");
                        XMLStepper.skipSubTree(xml);
                        return true;
                    }
                    if ("grouped".equals(name)) {
                        parseGrouped(xml, documentResponse);
                        // Cursor is at end of sub tree after parseHeader
                        return true;
                    }
                    if ("responseHeader".equals(name)) {
                        parseHeader(xml, documentResponse);
                        // Cursor is at end of sub tree after parseHeader
                        return true;
                    }
                    if ("facet_counts".equals(name) || "efacet_counts".equals(name)) {
                        if (facets == null) {
                            XMLStepper.skipSubTree(xml);
                            return true;
                        }
                        parseFacets(xml, facets, responses);
                        // Cursor is at end of sub tree after parseFacets
                        return true;
                    }
                    if ("elookup".equals(name)) {
                        parseLookup(xml, request, responses);
                        // Cursor is at end of sub tree after parseLookup
                        return true;
                    }
                    if ("spellcheck".equals(name)) {
                        parseSpellCheck(xml, request, responses);
                        // Cursor is at end of sub tree after parseLookup
                        return true;
                    }
                    log.debug("Encountered unsupported name in lst '" + name + "' in Solr response. Skipping element");
                    XMLStepper.skipSubTree(xml);
                    return true;
                }
                log.warn("Encountered unexpected tag '" + current + "' in Solr response. Skipping element");
                return false;
            }
        });

        addRecordBase(responses, documentResponse.getHitCount());
        documentResponse.addTiming("reportedtime", documentResponse.getSearchTime());
        documentResponse.addTiming("buildresponses.total", System.currentTimeMillis() - startTime);
        responses.add(documentResponse);
        return documentResponse.getHitCount();
    }

    /* If responses containg a FacetResponse without recordBase, add the facet recordBase and assign
     * {@link #CONF_RECORDBASE} as tag with hitCount as count..
     */
    protected void addRecordBase(ResponseCollection responses, long hitCount) {
        if (hitCount == 0) {
            return;
        }
        final String RECORD_BASE = "recordBase";
        for (Response response: responses) {
            if (response instanceof FacetResultExternal) {
                FacetResultExternal fr = (FacetResultExternal)response;
                for (Map.Entry<String, List<FacetResultImpl.Tag<String>>> entry: fr.getMap().entrySet()) {
                    if (RECORD_BASE.equals(entry.getKey())) {
                        if (entry.getValue() != null && entry.getValue().isEmpty()) {
                            fr.addTag(RECORD_BASE, recordBase, (int)hitCount, FacetResult.Reliability.PRECISE);
                        }
                        return;
                    }
                }
                // No recordBase, add it!
                fr.addTag(RECORD_BASE, recordBase, (int)hitCount, FacetResult.Reliability.PRECISE);
                return;
            }
        }
        log.debug("No FacetResultExternal found in response. Skipping recordBase addition");
    }

    /*
    <lst name="elookup"><lst name="fields"><lst name="freetext"><lst name="terms">
    <int name="deer">1</int>
    <int name="elephant">2</int>
    <int name="fox">1</int>
    ...
    */
    private void parseLookup(
        XMLStreamReader xml, Request request, final ResponseCollection responses) throws XMLStreamException {
        IndexRequest indexRequest = getLookupRequest(request);
        if (indexRequest == null) {
            log.warn("Unable to extract an IndexRequest even though an index lookup structure was found in the result");
            XMLStepper.findTagEnd(xml, "elookup");
            return;
        }
        final IndexResponse lookups = new IndexResponse(indexRequest);
        if (!XMLStepper.findTagStart(xml, "lst") || !"fields".equals(XMLStepper.getAttribute(xml, "name", null))) {
            throw new XMLStreamException("Unable to locate start tag 'lst' with name 'fields' inside 'elookup'");
        }
        xml.next();
        if (!XMLStepper.findTagStart(xml, "lst")) { // We only look at the first field
            throw new XMLStreamException("Unable to locate start tag 'lst' inside 'lst#fields'");
        }
        String fieldName = XMLStepper.getAttribute(xml, "name", null);
        xml.next();
        if (log.isTraceEnabled()) {
            log.trace("Found lst#" + fieldName + " in lst#fields in lst#elookup");
        }
        if (!XMLStepper.findTagStart(xml, "lst") || !"terms".equals(XMLStepper.getAttribute(xml, "name", null))) {
            throw new XMLStreamException("Unable to locate start tag 'lst' with name 'terms' inside 'lst#" + fieldName
                                         + "' in 'lst#fields' in 'lst#elookup''");
        }
        xml.next();
        XMLStepper.iterateElements(xml, "lst", "int", new XMLStepper.XMLCallback() {
            @Override
            public void execute(XMLStreamReader xml) throws XMLStreamException {
                String term = XMLStepper.getAttribute(xml, "name", null);
                if (term == null) {
                    throw new XMLStreamException(
                        "Encountered element 'int' in list 'terms' in 'elookup' without attribute 'name'");
                }
                String content = xml.getElementText();
                try {
                    int count = Integer.parseInt(content);
                    lookups.addTerm(new Pair<>(term, count));
                    log.trace("Added lookup " + term + "(" + count + ")");
                } catch (NumberFormatException e) {
                    log.warn("Ignoring lookup '" + term + "' with un-parsable count '" + content);
                }
            }
        });
        XMLStepper.findTagEnd(xml, "elookup");
        responses.add(lookups);
    }

    /*
    <lst name="spellcheck">
    <lst name="suggestions">
    <lst*</lst>
      <int name="numFound">2</int>
      <int name="startOffset">0</int>
      <int name="endOffset">5</int>
      <int name="origFreq">0</int>
      <arr name="suggestion">
      <lst>
        <str name="word">egense</str>
        <int name="freq">1</int>
      </lst>
      <lst>
        <str name="word">hansen</str>
        <int name="freq">1</int>
      </lst>
      </arr>
      </lst>
    <lst name="ekildsen">
    <int name="numFound">2</int>
    <int name="startOffset">6</int>
    <int name="endOffset">14</int>
    <int name="origFreq">0</int>
    <arr name="suggestion">
    <lst>
    <str name="word">eskildsen</str>
    <int name="freq">1</int>
    </lst>
    <lst>
    <str name="word">eskilsen</str>
    <int name="freq">1</int>
    </lst>
    </arr>
    </lst>
    <bool name="correctlySpelled">false</bool>
    </lst>
    </lst>
   <lst name="collation">
     <str name="collationQuery">thomas egense</str>
     <int name="hits">1</int>
   </lst>
   </lst>
     */
    private void parseSpellCheck(
        XMLStreamReader xml, Request request, ResponseCollection responses) throws XMLStreamException {
        String query = request.getString(DocumentKeys.SEARCH_QUERY, null);
        // TODO: Add SolrParam fallback
        query = request.getString(DidYouMeanKeys.SEARCH_QUERY, query);
        if (query == null) {
            query = "N/A";
        }
        final DidYouMeanResponse dym = new DidYouMeanResponse(query);

        if (!XMLStepper.findTagStart(xml, "lst") || !"suggestions".equals(XMLStepper.getAttribute(xml, "name", null))) {
            throw new XMLStreamException(
                "Unable to locate start tag 'lst' with name 'suggestions' inside 'spellcheck'");
        }
        xml.next();
        // We iterate until we find a collation
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                //  <lst name="collation">
                //    <str name="collationQuery">thomas egense</str>
                //    <int name="hits">1</int>
                //  </lst>
                if (!"lst".equals(current) || !"collation".equals(XMLStepper.getAttribute(xml, "name", null))) {
                    return false;
                }
                xml.next();

                if (log.isTraceEnabled()) {
                    log.trace("Found collation in lst#suggestions in lst#spellcheck");
                }
                if (!XMLStepper.findTagStart(xml, "str")
                    || !"collationQuery".equals(XMLStepper.getAttribute(xml, "name", null))) {
                    throw new XMLStreamException("Unable to locate collationQuery inside 'lst#collation' in "
                                                 + "'lst#suggestions' in 'lst#spellcheck''");
                }
                String collation = xml.getElementText();
                xml.next();
                if (!XMLStepper.findTagStart(xml, "int")
                    || !"hits".equals(XMLStepper.getAttribute(xml, "name", null))) {
                    throw new XMLStreamException("Unable to locate 'int#hits' inside 'lst#collation' in "
                                                 + "'lst#suggestions' in 'lst#spellcheck''");
                }
                int hits = Integer.parseInt(xml.getElementText());
                dym.addResult(collation, 1.0d, hits); // Artificial score
                xml.next();
                return true;
            }
        });

        XMLStepper.findTagEnd(xml, "spellcheck");
        responses.add(dym);
    }

    protected IndexRequest getLookupRequest(Request request) {
        final String PRE = SolrSearchNode.CONF_SOLR_PARAM_PREFIX;
        // Did we even request an index lookup?
        if (!request.containsKey(IndexKeys.SEARCH_INDEX_FIELD)
            && !request.getBoolean(PRE + ExposedIndexLookupParams.ELOOKUP, false)) {
            // No such luck, just give up
            return null;
        }

        // ExposedIndexLookupParams takes precedence over Summa params. Convert to IndexRequest parameters
        Request local = new Request();
        local.putAll(request);
        // TODO: What is IndexKeys.SEARCH_INDEX_QUERY used for?
        if (!putIfExists(request, local, IndexKeys.SEARCH_INDEX_QUERY, IndexKeys.SEARCH_INDEX_QUERY)) {
            if (!putIfExists(request, local, DocumentKeys.SEARCH_QUERY, IndexKeys.SEARCH_INDEX_QUERY)) {
                putIfExists(request, local, "q", IndexKeys.SEARCH_INDEX_QUERY);
            }
        }
        // TODO: Add support for locale in setup
        putIfExists(request, local, PRE + ExposedIndexLookupParams.ELOOKUP_DELTA, IndexKeys.SEARCH_INDEX_DELTA);
        putIfExists(request, local, PRE + ExposedIndexLookupParams.ELOOKUP_FIELD, IndexKeys.SEARCH_INDEX_FIELD);
        putIfExists(request, local, PRE + ExposedIndexLookupParams.ELOOKUP_LENGTH, IndexKeys.SEARCH_INDEX_LENGTH);
        putIfExists(request, local, PRE + ExposedIndexLookupParams.ELOOKUP_MINCOUNT, IndexKeys.SEARCH_INDEX_MINCOUNT);
        putIfExists(request, local, PRE + ExposedIndexLookupParams.ELOOKUP_TERM, IndexKeys.SEARCH_INDEX_TERM);
        putIfExists(
            request, local, PRE + ExposedIndexLookupParams.ELOOKUP_SORT_LOCALE_VALUE, IndexKeys.SEARCH_INDEX_LOCALE);
        putIfExists(request, local,
                    PRE + ExposedIndexLookupParams.ELOOKUP_CASE_SENSITIVE, IndexKeys.SEARCH_INDEX_CASE_SENSITIVE);
        //        String sort = request.getString(PRE + ExposedIndexLookupParams.ELOOKUP_SORT, null);
//        if (sort != null && !(PRE + ExposedIndexLookupParams.ELOOKUP_SORT_BYINDEX).equals(sort)) {
//            lookupR.setLocale(new Locale(sort));
//        }
        return defaultIndexRequest.createRequest(local);
    }
    private boolean putIfExists(Request sourceRequest, Request destRequest, String sourceKey, String destKey) {
        if (sourceRequest.containsKey(sourceKey)) {
            destRequest.put(destKey, sourceRequest.get(sourceKey));
            return true;
        }
        return false;
    }

    private void parseFacets(
        XMLStreamReader xml, SolrFacetRequest facets, ResponseCollection responses) throws XMLStreamException {
        long startTime = System.currentTimeMillis();
        HashMap<String, Integer> facetIDs = new HashMap<>(facets.getFacets().size());
        // 1 facet = 1 field in Solr-world
        HashMap<String, String[]> fields = new HashMap<>(facets.getFacets().size());
        for (int i = 0 ; i < facets.getFacets().size() ; i++) {
            SolrFacetRequest.Facet facet = facets.getFacets().get(i);
            facetIDs.put(facet.getField(), i);
            // TODO: Consider displayname
            fields.put(facet.getField(), new String[]{facet.getField()});
        }
        final FacetResultExternal facetResult = new FacetResultExternal(
            facets.getMaxTags(), facetIDs, fields, facets.getOriginalStructure());
        facetResult.setPrefix(searcherID + ".");
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if ("facet_fields".equals(XMLStepper.getAttribute(xml, "name", null))
                    || "efacet_fields".equals(XMLStepper.getAttribute(xml, "name", null))) {
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
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if (!"lst".equals(current)) {
                    log.warn("Encountered tag '" + current + "' in facet_fields. Expected 'lst'. Ignoring element");
                    return false;
                }
                final String facetName = XMLStepper.getAttribute(xml, "name", null);
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
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                final String tagName = XMLStepper.getAttribute(xml, "name", null);
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

    private void parseHeader(XMLStreamReader xml, final DocumentResponse response) throws XMLStreamException {
        log.trace("parseHeader(...) called");
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if ("int".equals(current) && "QTime".equals(XMLStepper.getAttribute(xml, "name", null))) {
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

    /*
      <lst name="moreLikeThis">
          <result name="sb_pure_ddfmxd:42657" numFound="2169" start="0">
              <doc>
                  <str name="recordID">sb_pure_ddfmxd:42663</str>
     */
    private boolean mltWarnFired = false;
    private void parseMLTResponse(XMLStreamReader xml, final DocumentResponse documentResponse)
            throws XMLStreamException {
        log.debug("parseXMLResponse called");
        final AtomicLong hitCount = new AtomicLong(0);
        final AtomicInteger responseCount = new AtomicInteger(0);
        xml.next();
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if ("result".equals(current)) {
                    String currentName;
                    String currentHitCount;
                    if ((currentName = XMLStepper.getAttribute(xml, "name", null)) == null ||
                        (currentHitCount = XMLStepper.getAttribute(xml, "numFound", null)) == null) {
                        if (!mltWarnFired) {
                            log.warn("While stepping through the MoreLikeThis result, the element 'result' did not "
                                     + "contain the attributes name and NumFound. This warning will not be repeated");
                            mltWarnFired = true;
                        }
                        return false;
                    }
                    hitCount.addAndGet(Long.parseLong(currentHitCount));
                    log.debug("Encountered result group " + responseCount.getAndIncrement() + " with id='" + currentName
                              + "' and numFound=" + currentHitCount + " in MLT");
                    if (Long.parseLong(currentHitCount) == 0) {
                        log.debug("No MoreLikeThis for '" + currentName + "'. Skipping document extraction");
                        return false;
                    }
                    parseDocumentsInResponse(xml, documentResponse);
                    return true;
                }
                return false;
            }
        });
        documentResponse.setHitCount(hitCount.get());
    }
    /*
       <doc>*
           <str name="recordID">sb_pure_ddfmxd:42657</str>*
     */
    private void parseResponse(XMLStreamReader xml, final DocumentResponse response) throws XMLStreamException {
        log.trace("parseResponse(...) called");
        response.setHitCount(Long.parseLong(XMLStepper.getAttribute(xml, "numFound", "-1")));
        parseDocumentsInResponse(xml, response);

    }

    /*
    <lst name="grouped">
      <lst name="sort_year_desc">
        <int name="matches">3569</int>
        <arr name="groups">
          <lst>
            <str name="groupValue">2012</str>
            <result name="doclist" numFound="70" start="0" maxScore="19.005508">
              <doc>
                <str name="recordID">sb_5588484</str>
     */
    private void parseGrouped(XMLStreamReader xml, final DocumentResponse docResponse) throws XMLStreamException {
        XMLStepper.findTagStart(xml, "lst");
        final String groupField = XMLStepper.getAttribute(xml, "name", null);
        if (groupField == null) {
            log.warn("Unable to locate <lst name=\"...\"> in grouped response");
            return;
        }
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                if ("lst".equals(current)) { // start tag
                    return false;
                }
                // <int name="matches">3569</int>
                if ("int".equals(current) && "matches".equals(XMLStepper.getAttribute(xml, "name", null))) {
                    docResponse.setHitCount(Long.parseLong(xml.getElementText()));
                    return true;
                }
                /*
                  <arr name="groups">
                    <lst>*
                      <str name="groupValue">2012</str>
                        <result name="doclist" numFound="70" start="0" maxScore="19.005508">
                         <doc>*
                           <str name="recordID">sb_5588484</str>*
                 */
                if ("arr".equals(current) && "groups".equals(XMLStepper.getAttribute(xml, "name", null))) {
                    xml.next(); // Inside arr name=groups
                    XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
                        @Override
                        public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                                throws XMLStreamException {
                            if ("lst".equals(current)) {
                                parseGroupedInnerList(xml, docResponse, groupField);
                                return true;
                            }
                            log.debug("parseGrouped: Unexpected tag '" + current + "' while iterating groups");
                            return false;
                        }
                    });
                    return true; // Is this always correct? Should iterateTags respond whether the mark has moved?
                }
                log.debug("parseGrouped: Unexpected tag in group " + groupField + ": " + current);
                return false;
            }
        });
    }

    /*
    <lst>
      <str name="groupValue">2012</str>
      <result name="doclist" numFound="70" start="0" maxScore="19.005508">
        <doc>*
          <str name="recordID">sb_5588484</str>*

     If there are no value in the group field, the group will be
     <null name="groupValue"/>
     */
    private void parseGroupedInnerList(XMLStreamReader xml, final DocumentResponse docResponse, final String groupName)
            throws XMLStreamException {
        xml.next(); // Inside lst
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            private String groupValue = null;
            private long numFound = -1;
            private List<DocumentResponse.Record> records = null;
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                log.debug("parseGroupedInnerList: Current tag: '" + current + "'");
                // <str name="groupValue">2012</str>
                if ("str".equals(current) && "groupValue".equals(XMLStepper.getAttribute(xml, "name", null))) {
                    groupValue = xml.getElementText();
                    return true;
                }
                // <null name="groupValue"/>
                if ("null".equals(current) && "groupValue".equals(XMLStepper.getAttribute(xml, "name", null))) {
                    groupValue = DocumentResponse.NULL_GROUP;
                    return false;
                }
                // What about maxScore? Inferred from first record in the group?
                if ("result".equals(current) && "doclist".equals(XMLStepper.getAttribute(xml, "name", null))) {
                    numFound = Long.parseLong(XMLStepper.getAttribute(xml, "numFound", "-1"));
                    records = getDocumentsInResponse(xml, docResponse);
                    return true;
                }
                log.debug("parseGroupedInnerList: Unexpected tag '" + current + "'");
                return false;
            }

            @Override
            public void end() {
                log.debug("parseGroupedInnerList: End lst reached");
                if (groupValue == null) {
                    log.warn("parseGroupedInnerList: Unable to locate groupValue for group '" + groupName + "'");
                } else if (records == null || records.isEmpty()) {
                    log.warn(String.format(
                            "parseGroupedInnerList: Got groupValue '%s' for group '%s' but no records",
                            groupValue, groupName));
                } else {
                    if (groupNullAsSeparate && DocumentResponse.NULL_GROUP.equals(groupValue)) {
                       for (DocumentResponse.Record record: records) {
                           docResponse.createAndAddGroup(record.getId(), 1, Arrays.asList(record));
                       }
                    } else {
                        docResponse.createAndAddGroup(groupValue, numFound, records);
                    }
                }
            }
        });
    }

    private void parseDocumentsInResponse(XMLStreamReader xml, final DocumentResponse response)
            throws XMLStreamException {
        for (DocumentResponse.Record record: getDocumentsInResponse(xml, response)) {
            response.addRecord(record);
        }
    }

    /*
      <result name="doclist" numFound="70" start="0" maxScore="19.005508">
        <doc>*
          <str name="recordID">sb_5588484</str>*

     */
    private List<DocumentResponse.Record> getDocumentsInResponse(XMLStreamReader xml, final DocumentResponse response)
            throws XMLStreamException {
        final List<DocumentResponse.Record> records = new ArrayList<>(); // Contains only a single record
        xml.next(); // Inside result
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {

            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                if ("doc".equals(current)) {
                    DocumentResponse.Record record = extractRecord(xml, response);
                    if (record != null) {
                        records.add(record);
                    }
                    return true;
                }
                return false;
            }
        });
        return records;
    }
    /*
        <doc>
          <str name="recordID">sb_5588484</str>*
     */
    private DocumentResponse.Record extractRecord(XMLStreamReader xml, final DocumentResponse response)
            throws XMLStreamException {
        final String sortKey = response.getSortKey() == null || response.getSortKey().equals(DocumentKeys.SORT_ON_SCORE)
                               ? null : response.getSortKey();
        xml.next(); // Move caret to inside doc element
        final List<DocumentResponse.Record> records = new ArrayList<>(); // Contains only a single record
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            float score = 0.0f;
            String id = null;
            String base = null;
            List<SimplePair<String, String>> fields = new ArrayList<>(100);
            String lastArrName = null; // For <arr name="foo"><str>term1</str><str>term1</str></arr> structures

            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                if ("arr".equals(current)) {
                    lastArrName = XMLStepper.getAttribute(xml, "name", null);
                    return false;
                }
                String name = tags.size() > 1 && "arr".equals(tags.get(tags.size() - 2)) ?
                        // We're inside a list of terms for the same multi value field so use the name from the arr
                        lastArrName :
                        // Single value field, so the name must be specified
                        XMLStepper.getAttribute(xml, "name", null);
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
                if (content.isEmpty()) {
                    log.debug("Content for " + current + "#" + name + " for document " + id + " was empty. Skipping");
                    return true;
                }

                if ("score".equals(name)) {
                    try {
                        score = Float.parseFloat(content);
                    } catch (NumberFormatException e) {
                        log.warn("Unable to parse float score '" + content + "' from " + current + "#" + name
                                 + " for document " + id + ". Score will be set to " + score);
                    }
                } else if (idField.equals(name)) {
                    id = content;
                } else if (baseField.equals(name)) {
                    base = content;
                }
                fields.add(new SimplePair<>(name, content));
                return true;
            }

            @Override
            public void end() {
                if (id == null) {
                    log.warn("No ID field '" + idField + "' found in document from query " + response.getQuery());
                    id = "not_defined_in_" + idField;
                }
                if (!IndexUtils.RECORD_FIELD.equals(idField)) {
                    fields.add(new SimplePair<>(DocumentKeys.RECORD_ID, id));
                }
                // Add base if it does not exist or was collected using another name
                if (base == null && recordBase != null || base != null && !IndexUtils.RECORD_BASE.equals(baseField)) {
                    fields.add(new SimplePair<>(DocumentKeys.RECORD_BASE, base));
                }
                // TODO: Cons
                String sortValue = null;
                if (sortKey == null) {
                    sortValue = Float.toString(score);
                } else {
                    for (SimplePair<String, String> field : fields) {
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
                for (SimplePair<String, String> field : fields) {
                    record.add(new DocumentResponse.Field(
                            field.getKey(), field.getValue(), !nonescapedFields.contains(field.getKey())));
                }
                if (log.isTraceEnabled()) {
                    log.trace("constructed and added " + record);
                }
                records.add(record);
            }
        });
        return records.isEmpty() ? null : records.get(0);
    }

    public String getRecordBase() {
        return recordBase;
    }

    protected DocumentResponse createBasicDocumentResponse(Request request) {
        String query =    request.getString(DocumentKeys.SEARCH_QUERY, null);
        String filter =   request.getString(DocumentKeys.SEARCH_FILTER, null);
        int startIndex =  request.getInt(DocumentKeys.SEARCH_START_INDEX, 0);
        int maxRecords =  request.getInt(DocumentKeys.SEARCH_MAX_RECORDS, DocumentKeys.DEFAULT_MAX_RECORDS);
        String sortKey =  request.getString(DocumentKeys.SEARCH_SORTKEY, null);
        boolean reverse = request.getBoolean(DocumentKeys.SEARCH_REVERSE, false);
        boolean grouped = request.getBoolean(DocumentKeys.GROUP, false);
        int rows =        request.getInt(DocumentKeys.ROWS,
                                         request.getInt(DocumentKeys.SEARCH_MAX_RECORDS, DocumentKeys.DEFAULT_ROWS));
        int groupLimit =  request.getInt(DocumentKeys.GROUP_LIMIT, DocumentKeys.DEFAULT_GROUP_LIMIT);
        String groupSort =request.getString(DocumentKeys.GROUP_SORT, null);
        String groupField=request.getString(DocumentKeys.GROUP_FIELD, "");
        DocumentResponse response = grouped ?
                new DocumentResponse(filter, query, startIndex, maxRecords, sortKey, reverse, new String[0], -1, -1, groupField, rows, groupLimit, groupSort
                ) :
                new DocumentResponse(filter, query, startIndex, maxRecords, sortKey, reverse, new String[0], -1, -1);
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
                append(XMLUtil.encode(extracted.getString("Date", "???"))).
                append("</dc:date>\n");
        shortformat.append("        <dc:format></dc:format>\n");
        if (extracted.containsKey("openUrl")) {
            shortformat.append("        <dc:identifier>").
                    append(XMLUtil.encode(extracted.getString("openUrl"))).
                    append("</dc:identifier>\n");
        }
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

    public void setNonescapedFields(Set<String> nonescapedFields) {
        this.nonescapedFields = new HashSet<>(nonescapedFields);
        log.debug("setNonescapedFields(" + Strings.join(nonescapedFields, ", ") + ")");
    }

    @Override
    public String toString() {
        return "SolrResponseBuilder(, recordBase='" + recordBase + '\'' +
               ", searcherID='" + searcherID + "', nonescapedFields=" + nonescapedFields
               + ", defaultIndexRequest=" + defaultIndexRequest + ", idField='" + idField
               + "', baseField='" + baseField + '\'' + ')';
    }
}
