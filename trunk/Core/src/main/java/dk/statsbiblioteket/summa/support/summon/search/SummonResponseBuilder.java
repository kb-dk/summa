/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ConvenientMap;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.summon.search.api.RecommendationResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLStepper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.exposed.facet.FacetResponse;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Takes a Summon response as per
 * http://api.summon.serialssolutions.com/help/api/search/response
 * and converts it to a {@link DocumentResponse} and a {@link FacetResponse}.
 * Mapping between field & facet names as well as score adjustments or tag
 * tweaking is not in the scope of this converter.
 * </p><p>
 * The Summon API does not return namespace-aware XML so the parser is equally
 * relaxed.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonResponseBuilder extends SolrResponseBuilder {
    private static Log log = LogFactory.getLog(SummonResponseBuilder.class);

    /**
     * The raw summon response is stored transient in the ResponseCollection for debug and error handling.
     */
    public static final String SUMMON_RESPONSE = "summon.rawresponse";


    public static final String DEFAULT_SUMMON_SORT_FIELD_REDIRECT = "PublicationDate - PublicationDate_xml_iso";

    /**
     * If true, only the year is used when generating shortformat. If false,
     * month and day is included iso-style, if they are available.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_SHORT_DATE = "summonresponsebuilder.shortformat.shortdate";
    public static final boolean DEFAULT_SHORT_DATE = true;

    /**
     * If a field name and the same field name with "_xml" both exists, attempt to convert the "_xml"-version to
     * a String and override the non-"_xml"-version.
     * </p><p>
     * Note: This is not guaranteed to catch all xml-variations.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_XML_OVERRIDES_NONXML = "summonresponsebuilder.xmloverrides";
    public static final boolean DEFAULT_XML_OVERRIDES_NONXML = true;

    public static final String DEFAULT_SUMMON_RECORDBASE = "summon";

    /**
     * If true, the raw XML-result from Summon is logged at INFO level. Used for debugging.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String SEARCH_DUMP_RAW = "summonresponsebuilder.dumpraw";

    /**
     * If true, fields with multiple values are collapsed into a single newline-delimited value.
     * If false, such multi-value fields are represented as multiple fields, each containing a single value.
     */
    public static final String CONF_COLLAPSE_MULTI_FIELDS = "summonresponsebuilder.multifields.collapse";
    public static final boolean DEFAULT_COLLAPSE_MULTI_FIELDS = true;

    public static final String CONF_XML_FIELD_HANDLING = "summonresponsebuilder.xmlhandling";
    public static final String SEARCH_XML_FIELD_HANDLING = CONF_XML_FIELD_HANDLING;
    public static final String DEFAULT_XML_FIELD_HANDLING = XML_MODE.selected.toString();

    /**
     * How to handle "fieldname_xml"-fields.<br/>
     * skip: Skip all _xml fields<br/>
     * selected: Explicit handling of date and author XML fields, skip of other _xml fields.<br/>
     * mixed: Explicit handling of date and author XML fields, pass through of other _xml fields.<br/>
     * full: Pass through of all _xml fields by direct copying.<br/>
     */
    public static enum XML_MODE {skip, selected, mixed, full}

    private final boolean shortDate;
    private final boolean xmlOverrides;
    private final String defaultXmlHandling;
    private final boolean collapseMultiValue;

    // TODO: isFullTextHit, boolean in document -> field

    public SummonResponseBuilder(Configuration conf) {
        super(adjust(conf));
        shortDate = conf.getBoolean(CONF_SHORT_DATE, DEFAULT_SHORT_DATE);
        xmlOverrides = conf.getBoolean(CONF_XML_OVERRIDES_NONXML, DEFAULT_XML_OVERRIDES_NONXML);
        collapseMultiValue = conf.getBoolean(CONF_COLLAPSE_MULTI_FIELDS, DEFAULT_COLLAPSE_MULTI_FIELDS);
        defaultXmlHandling = XML_MODE.valueOf( // To enum and back to fail early
                                               conf.getString(CONF_XML_FIELD_HANDLING, DEFAULT_XML_FIELD_HANDLING)).toString();
        log.info("Created " + this);
    }

    private static Configuration adjust(Configuration conf) {
        if (!conf.valueExists(CONF_SORT_FIELD_REDIRECT)) {
            conf.set(CONF_SORT_FIELD_REDIRECT, DEFAULT_SUMMON_SORT_FIELD_REDIRECT);
        }
        if (!conf.valueExists(CONF_RECORDBASE)) {
            conf.set(CONF_RECORDBASE, DEFAULT_SUMMON_RECORDBASE);
        }
        // conversion of legacy properties
        legacyCopy(conf, "summonresponsebuilder.recordbase", CONF_RECORDBASE);
        legacyCopy(conf, "summonresponsebuilder.sort.field.redirect", CONF_SORT_FIELD_REDIRECT);
        return conf;
    }
    private static void legacyCopy(Configuration conf, String oldKey, String newKey) {
        if (conf.valueExists(oldKey) && !conf.valueExists(newKey)) {
            log.warn("The key '" + oldKey + "' is deprecated in favor of '" + newKey
                     + "'. Please adjust settings accordingly to avoid this warning");
            conf.set(newKey, conf.getString(oldKey));
        }
    }

    private boolean rangeWarned = false;
    @Override
    public long buildResponses(Request request, SolrFacetRequest facets, ResponseCollection responses,
                               String solrResponse, String solrTiming) throws XMLStreamException {
        //System.out.println(solrResponse.replace(">", ">\n"));
        long startTime = System.currentTimeMillis();
        if (request.getBoolean(SEARCH_DUMP_RAW, false)) {
            log.info("Raw summon result:\n" + solrResponse.replace(">", ">\n"));
        }
        final boolean facetingEnabled = request.getBoolean(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        final XML_MODE xmlMode = XML_MODE.valueOf(
                request.getString(SEARCH_XML_FIELD_HANDLING, defaultXmlHandling));

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
        long searchTime = Long.parseLong(XMLStepper.getAttribute(xml, "totalRequestTime", "0"));
        long hitCount = Long.parseLong(XMLStepper.getAttribute(xml, "recordCount", "0"));
        String summonQueryString = "N/A";
        int maxRecords = -1;
        List<DocumentResponse.Record> records = null;
        // Seek to queries, facets or documents
        String currentTag;
//        long recordTime = 0;
        while ((currentTag = XMLStepper.jumpToNextTagStart(xml)) != null) {
            if ("query".equals(currentTag)) {
                maxRecords = Integer.parseInt(XMLStepper.getAttribute(xml, "pageSize", Integer.toString(maxRecords)));
                summonQueryString = XMLStepper.getAttribute(xml, "queryString", summonQueryString);
                continue;
            }
            if ("rangeFacetFields".equals(currentTag) && facetingEnabled && !rangeWarned) {
                log.warn("buildResponses(...) encountered facet range from summon. Currently there is no support for "
                         + "this. Further encounters of range facets will not be logged");
                rangeWarned = true;
                // TODO: Implement range facets from Summon
            }
            if ("facetFields".equals(currentTag) && facetingEnabled) {
                FacetResult<String> facetResult = extractFacetResult(xml, facets);
                if (facetResult != null) {
                    responses.add(facetResult);
                }
            }
            if ("recommendationLists".equals(currentTag)) {
                RecommendationResponse recommendation = extractRecommendations(xml);
                if (recommendation != null) {
                    responses.add(recommendation);
                }
            }
            if ("documents".equals(currentTag)) {
//                recordTime = -System.currentTimeMillis();
                String sortKey = request.getString(DocumentKeys.SEARCH_SORTKEY, null);
                records = extractRecords(xml, sortKey, xmlMode);
//                recordTime += System.currentTimeMillis();
            }
        }
        if (records == null) {
            log.warn("No records extracted from request " + request + ". Returning 0 hits");
            return 0;
        }
        DocumentResponse documentResponse = createBasicDocumentResponse(request);
        documentResponse.setSearchTime(searchTime);
        if (hitCount < records.size()) {
            log.warn("Encountered hitCount=" + hitCount + " with " + records.size() + " records for " + request);
            documentResponse.setHitCount(records.size());
        } else if (records.isEmpty()) { // Handles summon 1-off bug for empty search results
            documentResponse.setHitCount(0);
        } else {
            documentResponse.setHitCount(hitCount);
        }
        documentResponse.setPrefix(searcherID + ".");
        documentResponse.addTiming("reportedtime", searchTime);
        for (DocumentResponse.Record record: records) {
            documentResponse.addRecord(record);
        }
        addRecordBase(responses, documentResponse.getHitCount());
        documentResponse.addTiming(solrTiming);
        documentResponse.addTiming("buildresponses.documents", System.currentTimeMillis() - startTime);
        responses.add(documentResponse);
        responses.addTiming(searcherID + ".buildresponses.total", System.currentTimeMillis() - startTime);
        responses.getTransient().put(SUMMON_RESPONSE, solrResponse);
        return documentResponse.getHitCount();
    }

    private RecommendationResponse extractRecommendations(XMLStreamReader xml) throws XMLStreamException {
        long startTime = System.currentTimeMillis();
        final RecommendationResponse response = new RecommendationResponse();
        XMLStepper.iterateElements(xml, "recommendationLists", "recommendationList",
                                   new XMLStepper.XMLCallback() {
                                       @Override
                                       public void execute(XMLStreamReader xml) throws XMLStreamException {
                                           extractRecommendationList(xml, response);
                                       }
                                   });
        if (response.isEmpty()) {
            return null;
        }
        response.addTiming("buildresponses.recommendations", System.currentTimeMillis() - startTime);
        return response;
    }

    private void extractRecommendationList(
            XMLStreamReader xml, RecommendationResponse response) throws XMLStreamException {
        String type = XMLStepper.getAttribute(xml, "type", null);
        if (type == null) {
            throw new IllegalArgumentException("Type required for recommendationList");
        }
        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        final RecommendationResponse.RecommendationList recList = response.newList(type);
        XMLStepper.iterateElements(
                xml, "recommendationList", "recommendation",
                new XMLStepper.XMLCallback() {
                    @Override
                    public void execute(XMLStreamReader xml)
                            throws XMLStreamException {
                        String title = XMLStepper.getAttribute(xml, "title", null);
                        if (title == null) {
                            throw new IllegalArgumentException("Title required for recommendationList");
                        }
                        String description = XMLStepper.getAttribute(xml, "description", "");
                        String link = XMLStepper.getAttribute(xml, "link", "");
                        recList.addResponse(title, description, link);
                    }
                });
    }

    /**
     * Extracts all facet responses.
     * @param xml    the stream to extract Summon facet information from. Must be positioned at 'facetFields'.
     * @param facets a definition of the facets to extract.
     * @return a Summa FacetResponse.
     * @throws javax.xml.stream.XMLStreamException if there was an error accessing the xml stream.
     */
    private FacetResult<String> extractFacetResult(XMLStreamReader xml, SolrFacetRequest facets)
            throws XMLStreamException {
        long startTime = System.currentTimeMillis();
        HashMap<String, Integer> facetIDs = new HashMap<>(facets.getFacets().size());
        // 1 facet = 1 field in Summon-world
        HashMap<String, String[]> fields = new HashMap<>(facets.getFacets().size());
        for (int i = 0 ; i < facets.getFacets().size() ; i++) {
            SolrFacetRequest.Facet facet = facets.getFacets().get(i);
            facetIDs.put(facet.getField(), i);
            // TODO: Consider displayname
            fields.put(facet.getField(), new String[]{facet.getField()});
        }
        final FacetResultExternal summaFacetResult = new FacetResultExternal(
                facets.getMaxTags(), facetIDs, fields, facets.getOriginalStructure());
        summaFacetResult.setPrefix(searcherID + ".");
        XMLStepper.iterateElements(xml, "facetFields", "facetField", new XMLStepper.XMLCallback() {
            @Override
            public void execute(XMLStreamReader xml) throws XMLStreamException {
                extractFacet(xml, summaFacetResult);
            }
        });
        summaFacetResult.sortFacets();
        summaFacetResult.addTiming("buildresponses.facets", System.currentTimeMillis() - startTime);
        return summaFacetResult;
    }

    /**
     * Extracts a single facet response and adds it to the facet result.
     * @param xml    the stream to extract Summon facet information from.
     *               Must be positioned at 'facetField'.
     * @param summaFacetResult where to store the extracted facet.
     * @throws javax.xml.stream.XMLStreamException if there was an error
     * accessing the xml stream.
     */
    private void extractFacet(XMLStreamReader xml, final FacetResultExternal summaFacetResult)
            throws XMLStreamException {
        // TODO: Consider fieldname and other attributes?
        final String facetName = XMLStepper.getAttribute(xml, "displayName", null);
        XMLStepper.iterateElements(xml, "facetField", "facetCount", new XMLStepper.XMLCallback() {
            @Override
            public void execute(XMLStreamReader xml) {
                String tagName = XMLStepper.getAttribute(xml, "value", null);
                Integer tagCount = Integer.parseInt(XMLStepper.getAttribute(xml, "count", "0"));
                // <facetCount value="Newspaper Article" count="27" isApplied="true" isNegated="true"
                // isFurtherLimiting="false" removeCommand="removeFacetValueFilter(ContentType,Newspaper Article)"
                // negateCommand="negateFacetValueFilter(ContentType,Newspaper Article)"/>

                boolean isApplied = Boolean.parseBoolean(XMLStepper.getAttribute(xml, "isApplied", "false"));
                boolean isNegated = Boolean.parseBoolean(XMLStepper.getAttribute(xml, "isNegated", "false"));
//                System.out.println("Facet " + facetName + ", tag " + tagName + ", count " + tagCount);
                if (tagCount != 0 && !(isApplied && isNegated)) { // Signifies negative facet value filter
                    summaFacetResult.addTag(facetName, tagName, tagCount);
                }
            }
        });
    }

    /**
     * Extracts all Summon documents and converts them to
     * {@link }DocumentResponse#Record}.
     *
     * @param xml the stream to extract records from. Must be positioned at
     * ELEMENT_START for "documents".
     * @param sortKey if not null, the sort key is assigned to the Record if
     *                it is encountered in the XML.
     * @return an array of record or the empty list if no documents were found.
     * @throws javax.xml.stream.XMLStreamException if there was an error
     * during stream access.
     */
    private List<DocumentResponse.Record> extractRecords(XMLStreamReader xml, final String sortKey,
                                                         final XML_MODE xmlMode) throws XMLStreamException {
        // Positioned at documents
        final List<DocumentResponse.Record> records = new ArrayList<>(50);
        XMLStepper.iterateElements(xml, "documents", "document", new XMLStepper.XMLCallback() {
            float lastScore = 0f;

            @Override
            public void execute(XMLStreamReader xml) throws XMLStreamException {
                DocumentResponse.Record record = extractRecord(xml, sortKey, lastScore, xmlMode);
                if (record != null) {
                    records.add(record);
                    lastScore = record.getScore();
                }
            }

        });
//        fixMissingScores(records);
        return records;
    }
  /*
    final static float ZERO = 0.0f;
    private void fixMissingScores(List<DocumentResponse.Record> records) {
        for (int i = 0 ; i < records.size() ; i++) {
            if (records.get(i).getScore() == ZERO) {
                float newScore
                if (i > 0 && i < records.size() -1) { // Previous and next
                }
            }
        }
    }
    */

    /**
     * Extracts a Summon document and converts it to
     * {@link }DocumentResponse#Record}. The compact representation
     * "shortformat" is generated on the fly and added to the list of fields.
     *
     *
     * @param xml the stream to extract the record from. Must be positioned at
     * ELEMENT_START for "document".
     * @param sortKey if not null, the sort key is assigned to the Record if
     *                it is encountered in the XML.
     * @param lastScore the score for the previous Record. Used if the record does not contain any score.
     * @return a record or null if no document could be extracted.
     * @throws javax.xml.stream.XMLStreamException if there was an error
     * accessing the xml stream.
     */
    private DocumentResponse.Record extractRecord(
            XMLStreamReader xml, String sortKey, float lastScore, final XML_MODE xmlMode) throws XMLStreamException {
        // http://api.summon.serialssolutions.com/help/api/search/response/documents
        String openUrl = XMLStepper.getAttribute(xml, "openUrl", null);
        if (openUrl == null) {
            log.warn("Encountered a document without openUrl. Discarding");
            // TODO: Log with ID
            return null;
        }

        final Set<String> wanted = new HashSet<>(Arrays.asList(
                "ID", "Score", "Title", "Subtitle", "Author", "ContentType", "PublicationDate_xml", "Author_xml",
                "openUrl"));
        final ConvenientMap extracted = new ConvenientMap();

        final List<DocumentResponse.Field> fields = new ArrayList<>(50);
        // We transfer all document-start-tag attributes to fields
        for (int i = 0 ; i < xml.getAttributeCount() ; i++) {
//            <document hasFullText="true" isFullTextHit="false" inHoldings="true"
//            openUrl="ctx_ver=Z39.88-2004&amp;ct..."
//            link="http://statsbiblioteket.summon.serialssolutions.com/2.0.0/link/0/eLvH...">
            final String key = xml.getAttributeLocalName(i);
            final String value = xml.getAttributeValue(i);
            if (log.isTraceEnabled()) {
                log.trace("Document attribute " + key + "=\"" + value + "\"");
            }
            fields.add(new DocumentResponse.Field(key, value, true));
            if (wanted.contains(key)) {
                extracted.put(key, value);
            }
        }

//        String availibilityToken = XMLStepper.getAttribute(xml, "availabilityToken", null);
//        String hasFullText =       XMLStepper.getAttribute(xml, "hasFullText", "false");
//        String isFullTextHit =     XMLStepper.getAttribute(xml, "isFullTextHit", "false");
//        String inHoldings =        XMLStepper.getAttribute(xml, "inHoldings", "false");

//        fields.add(new DocumentResponse.Field("availibilityToken", availibilityToken, true));
//        fields.add(new DocumentResponse.Field("hasFullText", hasFullText, true));
//        fields.add(new DocumentResponse.Field("isFullTextHit", isFullTextHit, true));
//        fields.add(new DocumentResponse.Field("inHoldings", inHoldings, true));
//        fields.add(new DocumentResponse.Field("openUrl", openUrl, true));

        // PublicationDate_xml is a hack
        final String[] sortValue = new String[1]; // Hack to make final mutable
        final String sortField = sortRedirect.containsKey(sortKey) ? sortRedirect.get(sortKey) : sortKey;

        XMLStepper.iterateElements(xml, "document", "field", false, new XMLStepper.XMLCallback() {
            @Override
            public void execute(XMLStreamReader xml) throws XMLStreamException {
                List<DocumentResponse.Field> rawFields = extractFields(xml, xmlMode, wanted, extracted);
                if (rawFields != null) {
                    for (DocumentResponse.Field field: rawFields) {
                        fields.add(field);
                        if (sortField != null && (sortField.equals(field.getName()) ||
                                                  "PublicationDate_xml_iso".equals(field.getName()) &&
                                                  "PublicationDate_xml".equals(sortField))) {
                            sortValue[0] = field.getContent();
                        }
                    }
                }
            }
        });

        if (xmlOverrides) {
            // Author_xml
            for (DocumentResponse.Field field: fields) {
                if ("Author_xml_name".equals(field.getName())) {
                    for (int i = fields.size() - 1 ; i >= 0 ; i--) {
                        if ("Author".equals(fields.get(i).getName())) {
                            fields.remove(i);
                        }
                    }
                    for (DocumentResponse.Field afield: fields) {
                        if ("Author_xml_name".equals(afield.getName())) {
                            afield.setName("Author");
                        }
                    }
                    break;
                }
            }
        }
        if (!extracted.containsKey("Author")) {
            for (DocumentResponse.Field field: fields) {
                if ("Author".equals(field.getName())) {
                    extracted.put("Author", field.getContent());
                    break; // First one only
                }
            }
        }

        String recordID = extracted.getString("ID", null);
        if (recordID == null) {
            log.warn("Unable to locate field 'ID' in Summon document. Skipping document");
            return null;
        }
        fields.add(0, new DocumentResponse.Field("shortformat", createShortformat(extracted), false));
        if (recordBase != null) {
            fields.add(0, new DocumentResponse.Field("recordBase", recordBase, false));
        }
        fields.add(0, new DocumentResponse.Field(DocumentKeys.RECORD_ID, recordID, true));

        String sortV = sortKey == null || sortValue[0] == null ? null : sortValue[0];
        if (!extracted.containsKey("Score")) {
            log.debug("The record '" + recordID + "' did not contain a Score. Assigning " + lastScore);
        }
        DocumentResponse.Record record = new DocumentResponse.Record(
                recordID, searcherID, extracted.getFloat("Score", lastScore), sortV);
        for (DocumentResponse.Field field: fields) {
            record.addField(field);
        }
        return record;
    }
    @Override
    protected String createShortformat(ConvenientMap extracted) {
        String date = extracted.getString("PublicationDate_xml_iso", "????");
        date = shortDate && date.length() > 4 ? date.substring(0, 4) : date;
        extracted.put("Date", date);
        return super.createShortformat(extracted);
    }

    private boolean warnedOnMissingFullname = false;

    /**
     * Extracts a Summon document field and converts it to 0 or more {@link }DocumentResponse#Field}s.
     * </p><p>
     * While this implementation tries to produce fields for all inputs, it is not guaranteed that it will be usable
     * as no authoritative list of possible micro formats used by Summon has been found.
     * @param xml the stream to extract the field from. Must be positioned at ELEMENT_START for "field".
     * @param wanted fields requested for specific processing, such af record ID.
     * @param extracted the wanted fields and their content will be added to this.
     * @return fields or null if no fields could be extracted.
     * @throws javax.xml.stream.XMLStreamException if there was an error accessing the xml stream.
     */
    private List<DocumentResponse.Field> extractFields(
            XMLStreamReader xml, XML_MODE xmlMode, final Set<String> wanted, final ConvenientMap extracted)
            throws XMLStreamException {
        final String name = XMLStepper.getAttribute(xml, "name", null);
        if (name == null) {
            log.warn("Could not extract name for field. Skipping field");
            xml.next();
            return null;
        }
        final List<DocumentResponse.Field> fields = new ArrayList<>();

        if (!name.endsWith("_xml")) { // Not an XML field, so we just copy, extract & return
            final StringBuffer value = new StringBuffer(50);
            XMLStepper.iterateElements(xml, "field", "value", new XMLStepper.XMLCallback() {
                @Override
                public void execute(XMLStreamReader xml) throws XMLStreamException {
                    final String text = xml.getElementText();
                    if (!collapseMultiValue) {
                        fields.add(new DocumentResponse.Field(name, text, true));
                    }
                    value.append(value.length() == 0 ? text : "\n" + text);
                    if (wanted.contains(name) && !extracted.containsKey(name)) { // Only the first one
                        extracted.put(name, text);
                    }
                }
            });
            xml.next(); // Not really needed due to callers iterate on start tag, but it is the clean thing to do
            if (value.length() == 0) {
                log.debug("No value for field '" + name + "'");
                return null;
            }

            if (collapseMultiValue) {
                fields.add(new DocumentResponse.Field(name, value.toString(), true));
            }
            return fields;
        }

        // Entering embedded XML land
        XMLStreamReader subXML = xml;

        if ("PublicationDate_xml".equals(name) || "Author_xml".equals(name)) {
            // We need specific as well as general extraction so we extract the snippet and duplicate it
            String snippet = XMLStepper.getSubXML(xml, true);

            extractSpecific(
                    xmlFactory.createXMLStreamReader(new StringReader(snippet)), xmlMode, name, fields, extracted);

            if (xmlMode == XML_MODE.selected) { // Only these two XML fields
                return fields;
            }
            // TODO: Check skip

            // The subXML is passed onwards
            subXML = xmlFactory.createXMLStreamReader(new StringReader(snippet));
            subXML.next();
        }

        // Handle

        if (xmlMode == XML_MODE.skip) {
            log.trace("Skipping _xml field " + name + " as XML_MODE == skip");
            return null;
        }
        // xmlMode == XML_MODE.full
        log.trace("Direct pipe of _xml field " + name);
        fields.add(pipe(name, subXML));
        return fields;
    }

    private void extractSpecific(
            XMLStreamReader xml, final XML_MODE xmlMode, final String fieldName, final List<DocumentResponse.Field> fields,
            final ConvenientMap extracted) throws XMLStreamException {
        if ("PublicationDate_xml".equals(fieldName)) {
            /*
                  <field name="PublicationDate_xml">
                    <datetime text="20081215" month="12" year="2008" day="15"/>
                  </field>
                 */
            XMLStepper.findTagStart(xml, "datetime");
            String year = XMLStepper.getAttribute(xml, "year", null);
            if (year != null) {
                String month = XMLStepper.getAttribute(xml, "month", null);
                String day = XMLStepper.getAttribute(xml, "day", null);
                String isodate = year + (month == null ? "" : month + (day == null ? "" : day));
                fields.add(new DocumentResponse.Field("PublicationDate_xml_iso", isodate, false));
                extracted.put("PublicationDate_xml_iso", isodate);

                switch (xmlMode) {
                    case selected:
                    case mixed: {
                        fields.add(new DocumentResponse.Field("PublicationDate", isodate, false));
                    }
                    case full:
                    case skip: break;
                    default: throw new IllegalStateException("Unhandled switch case " + xmlMode);
                }
                xml.next();
            }
            return;
        }

        if ("Author_xml".equals(fieldName)) {
            /*
                  <field name="Author_xml">
                    <contributor middlename="A" givenname="CHRISTY" surname="VISHER" fullname="VISHER, CHRISTY A"/>
                    <contributor middlename="L" givenname="RICHARD" surname="LINSTER" fullname="LINSTER, RICHARD L"/>
                    <contributor middlename="K" givenname="PAMELA" surname="LATTIMORE" fullname="LATTIMORE, PAMELA K"/>
                  </field>
                 */
            final StringBuffer value = new StringBuffer(50);
            XMLStepper.iterateElements(xml, "field", "contributor", new XMLStepper.XMLCallback() {
                private final AtomicBoolean found = new AtomicBoolean(false);
                @Override
                public void execute(XMLStreamReader xml) throws XMLStreamException {
                    String fullname = XMLStepper.getAttribute(xml, "fullname", null);
                    if (fullname == null && !warnedOnMissingFullname) {
                        log.warn("Unable to locate attribute 'fullname' in 'contributor' element in 'Author_xml'. "
                                 + "This warning will not be repeated");
                        warnedOnMissingFullname = true;
                        return;
                    }
                    if (!found.get()) { // First
                        extracted.put("Author", fullname);
                        found.set(true);
                    }
                    if (!collapseMultiValue) {
                        fields.add(new DocumentResponse.Field("Author_xml_name", fullname, true));
                        if (xmlMode == XML_MODE.mixed || xmlMode == XML_MODE.selected) {
                            fields.add(new DocumentResponse.Field("Author_xml", fullname, true));
                        }
                    }
                    if (value.length() != 0) {
                        value.append("\n");
                    }
                    value.append(fullname);
                }
            });
            if (value.length() == 0) {
                log.debug("No value for field '" + fieldName + "'");
                return;
            }
            if (log.isTraceEnabled()) {
                log.trace("Extracted Author_xml: " + value.toString().replace("\n", ", "));
            }
            if (collapseMultiValue) {
                if (xmlMode == XML_MODE.mixed || xmlMode == XML_MODE.selected) {
                    fields.add(new DocumentResponse.Field("Author_xml", value.toString(), true));
                }
                fields.add(new DocumentResponse.Field("Author_xml_name", value.toString(), true));
            }
        }
    }

    private boolean xmlFieldAttributeWarningFired = false;
    private DocumentResponse.Field pipe(String name, XMLStreamReader xml) throws XMLStreamException {
        if (xml.getAttributeCount() != 0 && !xmlFieldAttributeWarningFired) {
            log.warn("Encountered " + xml.getAttributeCount() + " attributes on _xml field " + name + " where 0 was "
                     + "expected. These attributes are ignored. Further warnings of this type will be skipped");
            xmlFieldAttributeWarningFired = true;
        }
/*        xml.next();
        if (xml.getEventType() == XMLStreamReader.END_ELEMENT) {
            log.debug("The element " + name + " had empty content");
            return new DocumentResponse.Field(name, "", false);
        }*/
        return new DocumentResponse.Field(name, XMLStepper.getSubXML(xml, false, true), false);
    }


    @Override
    public String toString() {
        return "SummonResponseBuilder(shortDate=" + shortDate + ", xmlOverrides=" + xmlOverrides
               + ", defaultXmlHandling='" + defaultXmlHandling + "', collapseMultiValue=" + collapseMultiValue
               + ", " + super.toString() + ')';
    }
}
