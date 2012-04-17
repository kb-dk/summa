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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ConvenientMap;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.summon.search.api.RecommendationResponse;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.search.exposed.facet.FacetResponse;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;

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
public class SummonResponseBuilder implements Configurable {
    private static Log log = LogFactory.getLog(SummonResponseBuilder.class);

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
    public static final String CONF_RECORDBASE = "summonresponsebuilder.recordbase";
    public static final String DEFAULT_RECORDBASE = "summon";

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
    public static final String CONF_SORT_FIELD_REDIRECT = "summonresponsebuilder.sort.field.redirect";
    public static final String DEFAULT_SORT_FIELD_REDIRECT = "PublicationDate - PublicationDate_xml_iso";

    /**
     * If true, only the year is usen when generating shortformat. If false,
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


    private XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // No resolving of external DTDs
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }

    private String recordBase = DEFAULT_RECORDBASE;
    private final Map<String, String> sortRedirect;
    private final boolean shortDate;
    private final boolean xmlOverrides;

    public SummonResponseBuilder(Configuration conf) {
        recordBase = conf.getString(CONF_RECORDBASE, recordBase);
        shortDate = conf.getBoolean(CONF_SHORT_DATE, DEFAULT_SHORT_DATE);
        xmlOverrides = conf.getBoolean(CONF_XML_OVERRIDES_NONXML, DEFAULT_XML_OVERRIDES_NONXML);
        if ("".equals(recordBase)) {
            recordBase = null;
        }
        List<String> rules = conf.getStrings(
            CONF_SORT_FIELD_REDIRECT, new ArrayList<String>(Arrays.asList(DEFAULT_SORT_FIELD_REDIRECT)));
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
        log.info("Created SummonResponseBuilder with base '" + recordBase + "' and sort field redirect rules '"
                 + Strings.join(rules, ", ") + "'");
    }

    private boolean rangeWarned = false;
    public long buildResponses(
        Request request, SolrFacetRequest facets,
        ResponseCollection responses,
        String summonResponse, String summonTiming) throws XMLStreamException {
//        System.out.println(summonResponse.replace(">", ">\n"));
        long startTime = System.currentTimeMillis();
        boolean collectdocIDs = request.getBoolean(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        XMLStreamReader xml;
        try {
            xml = xmlFactory.createXMLStreamReader(new StringReader(summonResponse));
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Unable to construct a reader from input", e);
        }

        String query = request.getString(DocumentKeys.SEARCH_QUERY, null);
        String filter = request.getString(DocumentKeys.SEARCH_FILTER, null);
        int startIndex = request.getInt(DocumentKeys.SEARCH_START_INDEX, 0) + 1;
        String sortKey = request.getString(DocumentKeys.SEARCH_SORTKEY, null);
        boolean reverse = request.getBoolean(
            DocumentKeys.SEARCH_REVERSE, false);

        if (!findTagStart(xml, "response")) {
            log.warn("Could not locate start tag 'response', exiting");
            return 0;
        }
        long searchTime = Long.parseLong(
            getAttribute(xml, "totalRequestTime", "0"));
        long hitCount = Long.parseLong(
            getAttribute(xml, "recordCount", "0"));
        if (!findTagStart(xml, "response")) {
            log.warn("Could not locate start tag 'response', exiting");
            return 0;
        }
        String summonQueryString = "N/A";
        int maxRecords = -1;
        List<DocumentResponse.Record> records = null;
        // Seek to queries, facets or documents
        String currentTag;
//        long recordTime = 0;
        while ((currentTag = jumpToNextTagStart(xml)) != null) {
            if ("query".equals(currentTag)) {
                maxRecords = Integer.parseInt(getAttribute(xml, "pageSize", Integer.toString(maxRecords)));
                summonQueryString = getAttribute(xml, "queryString", summonQueryString);
                continue;
            }
            if ("rangeFacetFields".equals(currentTag) && collectdocIDs
                && !rangeWarned) {
                log.warn("buildResponses(...) encountered facet range from summon. Currently there is no support for "
                         + "this. Further encounters of range facets will not be logged");
                rangeWarned = true;
                // TODO: Implement range facets from Summon
            }
            if ("facetFields".equals(currentTag) && collectdocIDs) {
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
                records = extractRecords(xml, sortKey);
//                recordTime += System.currentTimeMillis();
            }
        }
        if (records == null) {
            log.warn("No records extracted from request " + request + ". Returning 0 hits");
            return 0;
        }
        // Start index reduced by 1 to match general contract of starting at 0.
        DocumentResponse documentResponse = new DocumentResponse(
            filter, query, startIndex-1, maxRecords, sortKey, reverse,
            new String[0], searchTime, hitCount);
        documentResponse.setPrefix("summon.");
        documentResponse.addTiming("reportedtime", searchTime);
        for (DocumentResponse.Record record: records) {
            documentResponse.addRecord(record);
        }
        documentResponse.addTiming(summonTiming);
        documentResponse.addTiming("buildresponses.documents", System.currentTimeMillis() - startTime);
        responses.add(documentResponse);
        responses.addTiming("summon.buildresponses.total", System.currentTimeMillis() - startTime);
        return documentResponse.getHitCount();
    }

    private RecommendationResponse extractRecommendations(XMLStreamReader xml) throws XMLStreamException {
        long startTime = System.currentTimeMillis();
        final RecommendationResponse response = new RecommendationResponse();
        iterateElements(xml, "recommendationLists", "recommendationList",
            new XMLCallback() {
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
        XMLStreamReader xml, RecommendationResponse response)
                                                     throws XMLStreamException {
        String type = getAttribute(xml, "type", null);
        if (type == null) {
            throw new IllegalArgumentException(
                "Type required for recommendationList");
        }
        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        final RecommendationResponse.RecommendationList recList =
            response.newList(type);
        iterateElements(xml, "recommendationList", "recommendation",
            new XMLCallback() {
                @Override
                public void execute(XMLStreamReader xml)
                                                     throws XMLStreamException {
                    String title = getAttribute(xml, "title", null);
                    if (title == null) {
                        throw new IllegalArgumentException(
                            "Title required for recommendationList");
                    }
                    String description = getAttribute(xml, "description", "");
                    String link = getAttribute(xml, "link", "");
                    recList.addResponse(title, description, link);
                }
            });
    }

    /**
     * Extracts all facet responses.
     * @param xml    the stream to extract Summon facet information from.
     *               Must be positioned at 'facetFields'.
     * @param facets a definition of the facets to extract.
     * @return a Summa FacetResponse.
     * @throws javax.xml.stream.XMLStreamException if there was an error
     * accessing the xml stream.
     */
    private FacetResult<String> extractFacetResult(XMLStreamReader xml, SolrFacetRequest facets)
        throws XMLStreamException {
        long startTime = System.currentTimeMillis();
        HashMap<String, Integer> facetIDs =
            new HashMap<String, Integer>(facets.getFacets().size());
        // 1 facet = 1 field in Summon-world
        HashMap<String, String[]> fields =
            new HashMap<String, String[]>(facets.getFacets().size());
        for (int i = 0 ; i < facets.getFacets().size() ; i++) {
            SolrFacetRequest.Facet facet = facets.getFacets().get(i);
            facetIDs.put(facet.getField(), i);
            // TODO: Consider displayname
            fields.put(facet.getField(), new String[]{facet.getField()});
        }
        final FacetResultExternal summaFacetResult = new FacetResultExternal(
            facets.getMaxTags(), facetIDs, fields, facets.getOriginalStructure());
        summaFacetResult.setPrefix("summon.");
        iterateElements(xml, "facetFields", "facetField", new XMLCallback() {
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
        final String facetName = getAttribute(xml, "displayName", null);
        iterateElements(xml, "facetField", "facetCount", new XMLCallback() {
            @Override
            public void execute(XMLStreamReader xml)  {
                String tagName = getAttribute(xml, "value", null);
                Integer tagCount = Integer.parseInt(getAttribute(xml, "count", "0"));
                // <facetCount value="Newspaper Article" count="27" isApplied="true" isNegated="true"
                // isFurtherLimiting="false" removeCommand="removeFacetValueFilter(ContentType,Newspaper Article)"
                // negateCommand="negateFacetValueFilter(ContentType,Newspaper Article)"/>

                boolean isApplied = Boolean.parseBoolean(getAttribute(xml, "isApplied", "false"));
                boolean isNegated = Boolean.parseBoolean(getAttribute(xml, "isNegated", "false"));
                if (!(isApplied && isNegated)) { // Signifies negative facet value filter
                    summaFacetResult.addTag(facetName, tagName, tagCount);
                }
            }
        });
    }

    private abstract static class XMLCallback {
        public abstract void execute(XMLStreamReader xml) throws XMLStreamException;
        public void close() { } // Called when iteration has finished
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
    private void iterateElements(XMLStreamReader xml, String endElement, String actionElement, XMLCallback callback)
        throws XMLStreamException {
        while (true) {
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT
                || (xml.getEventType() == XMLStreamReader.END_ELEMENT
                    && xml.getLocalName().equals(endElement))) {
                break;
            }
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT && xml.getLocalName().equals(actionElement)) {
                callback.execute(xml);
            }
            xml.next();
        }
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
    private List<DocumentResponse.Record> extractRecords(XMLStreamReader xml, final String sortKey)
        throws XMLStreamException {
        // Positioned at documents
        final List<DocumentResponse.Record> records = new ArrayList<DocumentResponse.Record>(50);
        iterateElements(xml, "documents", "document", new XMLCallback() {
            float lastScore = 0f;
            @Override
            public void execute(XMLStreamReader xml) throws XMLStreamException {
                DocumentResponse.Record record = extractRecord(xml, sortKey, lastScore);
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
    private DocumentResponse.Record extractRecord(XMLStreamReader xml, String sortKey, float lastScore)
        throws XMLStreamException {
    // http://api.summon.serialssolutions.com/help/api/search/response/documents
        String openUrl = getAttribute(xml, "openUrl", null);
        if (openUrl == null) {
            log.warn("Encountered a document without openUrl. Discarding");
            return null;
        }
        String availibilityToken = getAttribute(xml, "availabilityToken", null);
        String hasFullText =       getAttribute(xml, "hasFullText", "false");
        String inHoldings =        getAttribute(xml, "inHoldings", "false");

        final Set<String> wanted = new HashSet<String>(Arrays.asList(
            "ID", "Score", "Title", "Subtitle", "Author", "ContentType", "PublicationDate_xml", "Author_xml"));
        // PublicationDate_xml is a hack
        final String[] sortValue = new String[1]; // Hack to make final mutable
        final ConvenientMap extracted = new ConvenientMap();
        final List<DocumentResponse.Field> fields =
            new ArrayList<DocumentResponse.Field>(50);
        final String sortField = sortRedirect.containsKey(sortKey) ?
                                 sortRedirect.get(sortKey) : sortKey;

        iterateElements(xml, "document", "field", new XMLCallback() {
            @Override
            public void execute(XMLStreamReader xml) throws XMLStreamException {
                DocumentResponse.Field field = extractField(xml);
                if (field!= null) {
                    if (wanted.contains(field.getName())) {
                        extracted.put(field.getName(), field.getContent());
                        if ("PublicationDate_xml".equals(field.getName())) {
                            // The iso-thing is a big kludge. If we move the
                            // sort code outside of this loop, it would be
                            // cleaner.
                            extracted.put("PublicationDate_xml_iso", field.getContent());
                            fields.add(new DocumentResponse.Field(
                                "PublicationDate_xml_iso", field.getContent(), false));
                            if (sortField != null &&
                                sortField.equals("PublicationDate_xml_iso")) {
                                sortValue[0] = field.getContent();
                            }
                        }
                    }
                    fields.add(field);
                    if (sortField != null && sortField.equals(field.getName())) {
                        sortValue[0] = field.getContent();
                    }
                }
            }

        });

        if (xmlOverrides) {
            // Author
            for (DocumentResponse.Field field: fields) {
                if ("Author_xml".equals(field.getName())) {
                    for (int i = fields.size() - 1 ; i >= 0 ; i--) {
                        if ("Author".equals(fields.get(i).getName())) {
                            fields.remove(i);
                        }
                    }
                    field.setName("Author");

                    if (extracted.containsKey("Author")) {
                        extracted.put("Author", field.getContent());
                    }

                    break;
                }
            }
        }

        String id = extracted.getString("ID", null);

        if (id == null) {
            log.warn("Unable to locate field 'ID' in Summon document. Skipping document");
            return null;
        }
        fields.add(new DocumentResponse.Field("availibilityToken", availibilityToken, true));
        fields.add(new DocumentResponse.Field("hasFullText", hasFullText, true));
        fields.add(new DocumentResponse.Field("inHoldings", inHoldings, true));

        fields.add(new DocumentResponse.Field("shortformat", createShortformat(extracted), false));

        if (recordBase != null) {
            fields.add(new DocumentResponse.Field("recordBase", recordBase, false));
        }

        String sortV = sortKey == null || sortValue[0] == null ? null : sortValue[0];
        if (!extracted.containsKey("Score")) {
            log.debug("The record '" + id + "' did not contain a Score. Assigning " + lastScore);
        }
        DocumentResponse.Record record = new DocumentResponse.Record(
            id, "Summon", extracted.getFloat("Score", lastScore), sortV);
        for (DocumentResponse.Field field: fields) {
            record.addField(field);
        }
        return record;
    }

    private String createShortformat(ConvenientMap extracted) {
      // TODO: Incorporate this properly instead of String-hacking
        final StringBuffer shortformat = new StringBuffer(500);
        shortformat.append("  <shortrecord>\n");
        shortformat.append("    <rdf:RDF xmlns:dc=\"http://purl.org/dc/element"
                           + "s/1.1/\" xmlns:rdf=\"http://www.w3.org/1999/02/"
                           + "22-rdf-syntax-ns#\">\n");
        shortformat.append("      <rdf:Description>\n");

        shortformat.append("        <dc:title>").
            append(XMLUtil.encode(extracted.getString("Title", "")));
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
        String date = extracted.getString("PublicationDate_xml", "????");
        date = shortDate && date.length() > 4 ? date.substring(0, 4) : date;
        shortformat.append("        <dc:date>").
            append(date).append("</dc:date>\n");


        shortformat.append("        <dc:format></dc:format>\n");
        shortformat.append("      </rdf:Description>\n");
        shortformat.append("    </rdf:RDF>\n");
        shortformat.append("  </shortrecord>\n");
        return shortformat.toString();
    }

    private void addMultiple(ConvenientMap extracted, StringBuffer shortformat,
                             String indent, String tag, String field) {
        String[] elements = extracted.getString(field, "").split("\n");
        for (String element: elements) {
            if (!"".equals(element)) {
                shortformat.append(String.format(
                    "%s<%s>%s</%s>\n",
                    indent, tag, XMLUtil.encode(element), tag));
            }
        }
    }

    private boolean warnedOnMissingFullname = false;
    private boolean xmlFieldsWarningFired = false;
    /**
     * Extracts a Summon document field and converts it to
     * {@link }DocumentResponse#Field}.
     * </p><p>
     * While this implementation tries to produce fields for all inputs,
     * it is not guaranteed that it will be usable as no authoritative list
     * of possible micro formats used by Summon has been found.
     * @param xml the stream to extract the field from. Must be positioned at
     * ELEMENT_START for "field".
     * @return a field or null if no field could be extracted.
     * @throws javax.xml.stream.XMLStreamException if there was an error
     * accessing the xml stream.
     */
    private DocumentResponse.Field extractField(XMLStreamReader xml) throws XMLStreamException {
        String name = getAttribute(xml, "name", null);
        if (name == null) {
            log.warn("Could not extract name for field. Skipping field");
            return null;
        }
        // TODO: Implement XML handling and multiple values properly
        if (name.endsWith("_xml")) {
            if ("PublicationDate_xml".equals(name)) {
                /*
                  <field name="PublicationDate_xml">
                    <datetime text="20081215" month="12" year="2008" day="15"/>
                  </field>
                 */
                findTagStart(xml, "datetime");
                String year = getAttribute(xml, "year", null);
                if (year == null) {
                    return null;
                }
                String month = getAttribute(xml, "month", null);
                String day = getAttribute(xml, "day", null);
                return new DocumentResponse.Field(
                    name, year + (month == null ? "" : month + (day == null ? "" : day)), false);
            }

            if ("Author_xml".equals(name)) {
                /*
                  <field name="Author_xml">
                    <contributor middlename="A" givenname="CHRISTY" surname="VISHER" fullname="VISHER, CHRISTY A"/>
                    <contributor middlename="L" givenname="RICHARD" surname="LINSTER" fullname="LINSTER, RICHARD L"/>
                    <contributor middlename="K" givenname="PAMELA" surname="LATTIMORE" fullname="LATTIMORE, PAMELA K"/>
                  </field>
                 */
                final StringBuffer value = new StringBuffer(50);
                iterateElements(xml, "field", "contributor", new XMLCallback() {
                    @Override
                    public void execute(XMLStreamReader xml) throws XMLStreamException {
                        boolean found = false;
                        for (int i = 0 ; i < xml.getAttributeCount() ; i++) {
                            if ("fullname".equals(xml.getAttributeLocalName(i))) {
                                if (value.length() != 0) {
                                    value.append("\n");
                                }
                                value.append(xml.getAttributeValue(i));
                                found = true;
                                break;
                            }
                        }
                        if (!found && !warnedOnMissingFullname) {
                            log.warn("Unable to locate attribute 'fullname' in 'contributor' element in 'Author_xml'. "
                                     + "This warning will not be repeated");
                            warnedOnMissingFullname = true;
                        }
                    }
                });
                if (value.length() == 0) {
                    log.debug("No value for field '" + name + "'");
                    return null;
                } else if (log.isTraceEnabled()) {
                    log.trace("Extracted Author_xml: " + value.toString().replace("\n", ", "));
                }
//                System.out.println(value);
                return new DocumentResponse.Field(name, value.toString(), true);
            }

            if (!xmlFieldsWarningFired) {
                log.warn("XML fields are not supported yet. Skipping '" + name + "'");
                xmlFieldsWarningFired = true;
            }
            return null;
        }
        final StringBuffer value = new StringBuffer(50);
        iterateElements(xml, "field", "value", new XMLCallback() {
            @Override
            public void execute(XMLStreamReader xml) throws XMLStreamException {
                value.append(value.length() == 0 ? xml.getElementText() : "\n" + xml.getElementText());
            }
        });
        if (value.length() == 0) {
            log.debug("No value for field '" + name + "'");
            return null;
        }
        return new DocumentResponse.Field(name, value.toString(), true);
    }

    /**
     * Skips everything until a start tag is reacted or the readers is depleted.
     * @param xml the stream to iterate over.
     * @return the name of the start tag or null if EOD.
     * @throws javax.xml.stream.XMLStreamException if there was an error
     * accessing the xml stream.
     */
    private String jumpToNextTagStart(XMLStreamReader xml)
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
    private String getAttribute(
        XMLStreamReader xml, String attributeName, String defaultValue) {
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
    private boolean findTagStart(
        XMLStreamReader xml, String startTagName) throws XMLStreamException {
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
                throw new XMLStreamException(
                    "Error seeking to start tag for element '" + startTagName + "'", e);
            }
        }
    }
}
