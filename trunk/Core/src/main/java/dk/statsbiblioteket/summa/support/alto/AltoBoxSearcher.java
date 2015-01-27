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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.api.document.HighlightResponse;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLStepper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Special purpose searcher to deliver bounding boxes for search terms.
 * </p><p>
 * The Searcher relies on a backing Storage containing records with ALTO content as well as
 * a previous searcher in the chain that delivers results with highlighting.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoBoxSearcher extends SearchNodeImpl {
    private static Log log = LogFactory.getLog(AltoBoxSearcher.class);
    private static final long M = 1000000;
    private final XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // No resolving of external DTDs
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }

    /**
     * Whether or not boxes should be returned.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_BOX = "box";
    public static final String SEARCH_BOX = CONF_BOX;
    public static final boolean DEFAULT_BOX = false;

    /**
     * If stated, this will disable standard term extraction from {@link HighlightResponse}s.
     * </p><p>
     * Optional. Default is undefined.
     */
    public static final String SEARCH_LOOKUP_TERMS = "box.terms";

    /**
     * If stated, these records will be used for resolving terms as well as boxes.
     * If not stated, records will be inferred from responses earlier in the response collection.
     * </p><p>
     * Optional.
     */
    public static final String SEARCH_RECORDIDS = "box.recordIDs";

    /**
     * If stated, the given field is expected to hold the IDs for the records given in {@link #SEARCH_RECORDIDS}.
     * </p><p>
     * Optional. Default is empty (ID will match the primary ID given in the DocumentResult).
     */
    public static final String CONF_ID_FIELD = "box.idfield";
    public static final String SEARCH_ID_FIELD = CONF_ID_FIELD;
    public static final String DEFAULT_ID_FIELD = "";

    /**
     * The tag used by Solr to designate highlights.
     * </p><p>
     * Optional. Default is {@code em}.
     */
    public static final String CONF_HIGHLIGHT_TAG = "box.highlighttag";
    public static final String SEARCH_HIGHLIGHT_TAG = CONF_HIGHLIGHT_TAG;
    public static final String DEFAULT_HIGHLIGHT_TAG = "em";

    private final StorageReaderClient storage;
    private final boolean defaultBox;
    private final String defaultIDField;
    private final String defaultHighlightTag;

    public AltoBoxSearcher(Configuration conf) throws RemoteException {
        super(conf);
        storage = new StorageReaderClient(conf);
        defaultBox = conf.getBoolean(CONF_BOX, DEFAULT_BOX);
        defaultIDField = conf.getString(CONF_ID_FIELD, DEFAULT_ID_FIELD);
        defaultHighlightTag = conf.getString(CONF_HIGHLIGHT_TAG, DEFAULT_HIGHLIGHT_TAG);
        readyWithoutOpen();
        log.info("Created " + this);
    }

    @Override
    protected void managedSearch(Request request, ResponseCollection responses) throws RemoteException {
        if (!request.getBoolean(SEARCH_BOX, defaultBox)) {
            log.trace("No box requested");
            return;
        }
        AltoBoxResponse boxResponse = new AltoBoxResponse(
                request.getStrings(SEARCH_LOOKUP_TERMS, Collections.<String>emptyList()),
                request.getStrings(SEARCH_RECORDIDS, Collections.<String>emptyList()));
        responses.add(boxResponse);
        log.debug("Created " + boxResponse);
        final long stringStart = System.nanoTime();
        if (boxResponse.getRequestTerms().isEmpty()) {
            resolveHighlights(request, responses, boxResponse);
        }
        boxResponse.addTiming("box.termresolve", (System.nanoTime()-stringStart)/M);

        if (boxResponse.getLookupTerms().isEmpty()) {
            log.debug("No terms resolved for " + request);
            return;
        }
        if (boxResponse.getLookupRecordIDs().isEmpty()) {
            log.debug("No recordIDs resolved for " + request);
            return;
        }
        resolveBoxes(boxResponse);
    }

    /*
    Iterates previous responses and tries to locate a DocumentResponse, from which to extract highlighted terms.
     */
    private void resolveHighlights(Request request, ResponseCollection responses, AltoBoxResponse boxResponse) {
        log.debug("Resolving highlights for query " + request.getString(DocumentKeys.SEARCH_QUERY, ""));
        final String idField = request.getString(SEARCH_ID_FIELD, defaultIDField);
        // Only extract docIDs if the ID field is not equal to Solr's ID field
        // docIDs extracted from the highlight if the idField is equal to Solr default ID (i.e. "")
        final Set<String> hlDocIDs = new HashSet<>();
        final Set<String> terms = new HashSet<>();

        for (Response response: responses) {
            if (response instanceof HighlightResponse) {
                String highlightTag = request.getString(SEARCH_HIGHLIGHT_TAG, defaultHighlightTag);
                Pattern highlightPattern = Pattern.compile("<" + highlightTag + ">([^<]+)</" + highlightTag + ">");

                HighlightResponse docResponse = (HighlightResponse)response;
                    // Map<id, Map<field, List<content>>>
                for (Map.Entry<String, Map<String, List<String>>> hlEntry: docResponse.getHighlights().entrySet()) {
                    if ("".equals(idField)) {
                        hlDocIDs.add(hlEntry.getKey());
                    }
                    for (Map.Entry<String, List<String>> fieldEntry: hlEntry.getValue().entrySet()) {
                        for (String content: fieldEntry.getValue()) {
                            Matcher hlMatcher = highlightPattern.matcher(content);
                            while (hlMatcher.find()) {
                                terms.add(hlMatcher.group(1));
                            }
                        }
                    }
                }
            }
        }
        if (terms.isEmpty()) {
            log.debug("Unable to extract any terms");
            return;
        }
        boxResponse.addAllResolvedTerms(terms);
        if (!boxResponse.getLookupRecordIDs().isEmpty()) {
            log.debug("No resolving of recordIDs as " + boxResponse.getLookupRecordIDs().size()
                      + " specific IDs has been requested");
            return;
        }
        if ("".equals(idField)) {
            log.debug("Adding " + hlDocIDs + " recordIDs resolved from highlight response");
            boxResponse.addAllResolvedRecordIDs(hlDocIDs);
        }
        Set<String> returnedIDs = getRecordIDs(request, responses, boxResponse);
        log.debug("idField '" + idField + "' not equal to default Solr ID field: All " + returnedIDs.size()
                  + " recordIDs from DocumentResponse will be used for lookup");
        boxResponse.addAllResolvedRecordIDs(returnedIDs);
    }

    // Extract custom document IDs from the DocumentResponse (if available).
    private Set<String> getRecordIDs(Request request, ResponseCollection responses, AltoBoxResponse boxResponse) {
        final String recordField = request.getString(SEARCH_ID_FIELD, defaultIDField);
        Set<String> recordIDs = new HashSet<>();
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                DocumentResponse docResponse = (DocumentResponse)response;
                for (DocumentResponse.Record record: docResponse.getRecords()) {
                    String recordID = "".equals(recordField) ? record.getId() : record.getFieldValue(recordField, null);
                    if (recordID == null) {
                        log.warn("Unable to locate recordID from " + record + " with field '" + recordField + "'");
                    } else {
                        recordIDs.add(recordID);
                    }
                }
            }
        }
        return recordIDs;
    }

    /*
    Requests records from Storage, using ALTO-xml to resolve boxes for highlighted terms.
     */
    // TODO: Request more than one record at a time, but use paging as the single records can be quite large
    private void resolveBoxes(AltoBoxResponse boxResponse) {
        final long resolveStart = System.nanoTime();
        for (String recordID: boxResponse.getLookupRecordIDs()) {
            try {
                log.debug("Requesting record " + recordID);
                Record record = storage.getRecord(recordID, null);
                log.debug("Got " + record);
                XMLStreamReader xml =
                        xmlFactory.createXMLStreamReader(RecordUtil.getReader(record, RecordUtil.PART_CONTENT));
                resolveBoxes(xml, recordID, boxResponse);
            } catch (IOException e) {
                log.warn("Exception requesting record '" + recordID + "' from " + storage, e);
            } catch (XMLStreamException e) {
                log.warn("Exception processing XML for record '" + recordID + "'", e);
            }
        }
        boxResponse.addTiming("boxresolve", (System.nanoTime()-resolveStart)/M);
    }

    private void resolveBoxes(XMLStreamReader xml, final String recordID, final AltoBoxResponse boxResponse)
            throws XMLStreamException {
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                // <TextLine ID="LINE3" STYLEREFS="TS20" HEIGHT="276" WIDTH="3740" HPOS="756" VPOS="1292">
                // <String ID="S17" CONTENT="Den" WC="0.852" CC="7 8 8" HEIGHT="244" WIDTH="624" HPOS="756" VPOS="1316"/>

                if ("String".equals(current) && tags.size() > 1 && "TextLine".equals(tags.get(tags.size()-2))) {
                    String content = XMLStepper.getAttribute(xml, "CONTENT", null);
                    if (boxResponse.getLookupTerms().contains(content)) {
                        boxResponse.add(recordID, new AltoBoxResponse.Box(
                                getInt(xml, "HPOS"), getInt(xml, "VPOS"), getInt(xml, "WIDTH"), getInt(xml, "HEIGHT"),
                                content,
                                XMLStepper.getAttribute(xml, "WC", "N/A"), XMLStepper.getAttribute(xml, "CC", "N/A")));
                    }
                }
                return false;
            }

            private int getInt(XMLStreamReader xml, String attributeName) {
                return Integer.parseInt(XMLStepper.getAttribute(xml, attributeName, "-1"));
            }
        });
    }

    @Override
    protected void managedWarmup(String request) { } // Intentionally blank
    @Override
    protected void managedOpen(String location) throws RemoteException { } // Intentionally blank
    @Override
    protected void managedClose() throws RemoteException { } // Intentionally blank

    @Override
    public String toString() {
        return "AltoBoxSearcher(storage=" + storage + ", defaultBox=" + defaultBox
               + ", idField=" + defaultIDField + ")";
    }
}
