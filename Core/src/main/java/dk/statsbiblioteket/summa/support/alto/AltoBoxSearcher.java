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
import dk.statsbiblioteket.util.Strings;
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

    /**
     * If defined, extracted IDs are matched with the given regexp and {@link #CONF_ID_TEMPLATE} applied to the result.
     * </p><p>
     * Note: This is only applied to <em>extracted</em> IDs, not IDs specified with {@link #SEARCH_RECORDIDS}.
     * </p><p>
     * Optional. Default is "" (no adjustment of IDs).
     */
    public static final String CONF_ID_REGEXP = "box.idadjust.regexp";
    public static final String SEARCH_ID_REGEXP = CONF_ID_REGEXP;
    public static final String DEFAULT_ID_REGEXP = "";

    /**
     * If {@link #CONF_ID_REGEXP} is defined, this template is used to control the output.
     * </p><p>
     * Optional. Default is {@code $0} (output full match).
     */
    public static final String CONF_ID_TEMPLATE = "box.idadjust.template";
    public static final String SEARCH_ID_TEMPLATE = CONF_ID_TEMPLATE;
    public static final String DEFAULT_ID_TEMPLATE = "$0";

    /**
     * If true, x, y, width and height of the boxes are relative to the overall page.
     * If false, they are stated in absolute pixels.
     * </p><p>
     * Optional. Default is true;
     */
    public static final String CONF_COORDINATES_RELATIVE = "box.coordinates.relative";
    public static final String SEARCH_COORDINATES_RELATIVE = CONF_COORDINATES_RELATIVE;
    public static final boolean DEFAULT_COORDINATES_RELATIVE = true;

    /**
     * If {@link #CONF_COORDINATES_RELATIVE} is true, this determines if the y and height are relative
     * to page width (true) or relative to page height (false).
     * </p><p>
     * Optional. Default is true (relative to page width).
     */
    public static final String CONF_COORDINATES_YISX = "box.coordinates.yisx";
    public static final String SEARCH_COORDINATES_YISX = CONF_COORDINATES_YISX;
    public static final boolean DEFAULT_COORDINATES_YISX = false;

    /**
     * ALTO String content considered for highlighting is trimmed with this prefix and a template of "$1" applied.
     * </p><p>
     * Optional. Default is {@code ^[,.-/!? ]*(.*?)[,.-/!? ]*$}.
     */
    public static final String CONF_ALTO_STRING_TRIM_REGEXP = "box.altostringtrim.regexp";
    public static final String DEFAULT_ALTO_STRING_TRIM_REGEXP =
            "^[-—•«»|'„“‘’,./!?\" :;()^]*(.*?)[-—•«»|'„“‘’,./!?\" :;()^]*$";

    /**
     * After ALTO String content has been trimmed, a split on the given regexp is attempted.
     * The parts are used highlight-term matching.
     */
    public static final String CONF_ALTO_STRING_SPLIT_REGEXP = "box.altostringsplit.regexp";
    public static final String DEFAULT_ALTO_STRING_SPLIT_REGEXP = "[-—•|/]";

    private final StorageReaderClient storage;
    private final boolean defaultBox;
    private final String defaultIDField;
    private final String defaultHighlightTag;
    private final String defaultIDAdjustRegexp;
    private final String defaultIDAdjustTemplate;
    private final boolean defaultRelativeCoordinates;
    private final boolean defaultYisx;
    private final Pattern altoStringTrimmer;
    private final Pattern altoStringSplitter;

    public AltoBoxSearcher(Configuration conf) throws RemoteException {
        super(conf);
        storage = new StorageReaderClient(conf);
        defaultBox = conf.getBoolean(CONF_BOX, DEFAULT_BOX);
        defaultIDField = conf.getString(CONF_ID_FIELD, DEFAULT_ID_FIELD);
        defaultHighlightTag = conf.getString(CONF_HIGHLIGHT_TAG, DEFAULT_HIGHLIGHT_TAG);
        defaultIDAdjustRegexp = conf.getString(CONF_ID_REGEXP, DEFAULT_ID_REGEXP);
        defaultIDAdjustTemplate = conf.getString(CONF_ID_TEMPLATE, DEFAULT_ID_TEMPLATE);
        defaultRelativeCoordinates = conf.getBoolean(CONF_COORDINATES_RELATIVE, DEFAULT_COORDINATES_RELATIVE);
        defaultYisx = conf.getBoolean(CONF_COORDINATES_YISX, DEFAULT_COORDINATES_YISX);
        altoStringTrimmer = Pattern.compile(
                conf.getString(CONF_ALTO_STRING_TRIM_REGEXP, DEFAULT_ALTO_STRING_TRIM_REGEXP));
        altoStringSplitter = Pattern.compile(
                conf.getString(CONF_ALTO_STRING_SPLIT_REGEXP, DEFAULT_ALTO_STRING_SPLIT_REGEXP));
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
        boxResponse.setRelativeCoordinates(request.getBoolean(SEARCH_COORDINATES_RELATIVE, defaultRelativeCoordinates));
        boxResponse.setYisx(request.getBoolean(SEARCH_COORDINATES_YISX, defaultYisx));
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
        log.debug("Resolved " + boxResponse.getLookupRecordIDs().size() + " recordIDs and " +
                  boxResponse.getLookupTerms().size() + " terms from highlighting: [" +
                  Strings.join(boxResponse.getLookupTerms(), 10) + "]");
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
        String pattern = request.getString(SEARCH_ID_REGEXP, defaultIDAdjustRegexp);
        final Pattern idPattern = pattern.isEmpty() ? null : Pattern.compile(pattern);
        final String idTemplate = request.getString(SEARCH_ID_TEMPLATE, defaultIDAdjustTemplate);

        for (Response response: responses) {
            if (response instanceof HighlightResponse) {
                String highlightTag = request.getString(SEARCH_HIGHLIGHT_TAG, defaultHighlightTag);
                Pattern highlightPattern = Pattern.compile(
                        "[^ ]*<" + highlightTag + ">([^<]+)</" + highlightTag + ">");

                HighlightResponse docResponse = (HighlightResponse)response;
                    // Map<id, Map<field, List<content>>>
                for (Map.Entry<String, Map<String, List<String>>> hlEntry: docResponse.getHighlights().entrySet()) {
                    if ("".equals(idField)) {
                        if (idPattern == null) {
                            hlDocIDs.add(hlEntry.getKey());
                        } else {
                            String transformedID = transform(hlEntry.getKey(), idPattern, idTemplate);
                            if (transformedID.isEmpty()) {
                                log.warn(String.format(Locale.ROOT,
                                        "Discarding recordID '%s' as regexp '%s' and template '%s' gave empty result",
                                        hlEntry.getKey(), idPattern.pattern(), idTemplate));
                            } else {
                                hlDocIDs.add(transformedID);
                            }
                        }
                    }
                    for (Map.Entry<String, List<String>> fieldEntry: hlEntry.getValue().entrySet()) {
                        for (String content: fieldEntry.getValue()) {
                            // Why the spaces?
                            Matcher hlMatcher = highlightPattern.matcher(" " + content + " ");
                            while (hlMatcher.find()) {
                                // The highlighter can return things like 'Overskrift:', where ':' must be removed
                                terms.add(altoStringTrimmer.matcher(hlMatcher.group(1)).replaceAll("$1"));
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
            return;
        }
        Set<String> returnedIDs = getRecordIDs(request, responses, boxResponse);
        log.debug("idField '" + idField + "' not equal to default Solr ID field: All " + returnedIDs.size()
                  + " recordIDs from DocumentResponse will be used for lookup");
        boxResponse.addAllResolvedRecordIDs(returnedIDs);
    }

    private final StringBuffer buffer = new StringBuffer(50);
    private synchronized String transform(String id, Pattern pattern, String template) {
        buffer.setLength(0);
        Matcher matcher = pattern.matcher(id);
        if (!matcher.find()) {
            return "";
        }
        buffer.setLength(0);
        int matchPos = matcher.start();
        matcher.appendReplacement(buffer, template);
        return buffer.toString().substring(matchPos);
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
                if (record == null) {
                    log.warn("Unable to resolve " + recordID + " from " + storage.getVendorId());
                    continue;
                }
                log.debug("Got " + record + ", iterating and extracting boxes from " +
                          boxResponse.getLookupTerms().size() + " terms");
                //System.out.println("Storing in /tmp/sum_debug.xml");
                //Files.saveString(record.getContentAsUTF8(), new File("/tmp/sum_debug.xml"));
                XMLStreamReader xml =
                        xmlFactory.createXMLStreamReader(RecordUtil.getReader(record, RecordUtil.PART.content));
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
            int pageWidth = -1;
            int pageHeight = -1;
            boolean warnedNoPageSize = false;

            final List<String> contents = new ArrayList<String>() {
                @Override
                public boolean add(String o) {
                    return o != null && super.add(o);
                }
            };
            AltoBoxResponse.Box lastHyphenationBox = null;
            boolean firstTextOnLine = false;

            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) {

                // <Page ID="PAGE2" HEIGHT="11408" WIDTH="9304" PHYSICAL_IMG_NR="2" QUALITY="OK" POSITION="Single" PROCESSING="OCR1"  ACCURACY="85.29" PC="0.852900">
                if ("Page".equals(current)) {
                    pageWidth = Integer.parseInt(XMLStepper.getAttribute(xml, "WIDTH", Integer.toString(pageWidth)));
                    pageHeight = Integer.parseInt(XMLStepper.getAttribute(xml, "HEIGHT", Integer.toString(pageHeight)));
                }

                if ("TextLine".equals(current)) {
                    firstTextOnLine = true;
                }

                // <TextLine ID="LINE3" STYLEREFS="TS20" HEIGHT="276" WIDTH="3740" HPOS="756" VPOS="1292">
                // <String ID="S17" CONTENT="Den" WC="0.852" CC="7 8 8" HEIGHT="244" WIDTH="624" HPOS="756" VPOS="1316"/>

                if ("String".equals(current) && tags.size() > 1 && "TextLine".equals(tags.get(tags.size()-2))) {
                    assignContents(xml);

                    if (firstTextOnLine && lastHyphenationBox != null) { // We've got a hyphenation candidate
                        // Permutate with the hyphenation prefix and add if the result matches the terms
                        for (String postfix: contents) {
                            addHighlightBoxIfMatch(lastHyphenationBox, lastHyphenationBox.content + postfix);
                        }
                    }
                    firstTextOnLine = false;

                    // Standard non-lenient processing
                    for (String rawContent : contents) {
                        String content = altoStringTrimmer.matcher(rawContent).replaceAll("$1");
                        if (boxResponse.isRelativeCoordinates() && (pageWidth == -1 || pageHeight == -1) &&
                            !warnedNoPageSize) {
                            log.warn("Relative coordinated requested, but no page size has been determined. "
                                     + "Using absolute coordinates");
                            warnedNoPageSize = true;
                        }

                        addHighlightBoxIfMatch(xml, content);

                        // If the text ends with "-" then create a box as it might be the first part of a hyphenation
                        // Consider if this should be a regexp with other dashes
                        if (rawContent.endsWith("-") && rawContent.length() > 1) {
                            lastHyphenationBox = getBox(xml, rawContent.substring(0, rawContent.length() - 1));
                        } else {
                            lastHyphenationBox = null;
                        }
                    }
                }
                return false;
            }

            // There are either 0, 1 or 2 contents. 2 contents happens with hyphenated words
            private void assignContents(XMLStreamReader xml) {
                contents.clear();
                // <String CC="2 2 2 2 3 3" CONTENT="forfat" HEIGHT="116" HPOS="3596" ID="S326" SUBS_CONTENT="forfattede" SUBS_TYPE="HypPart1" VPOS="7160" WC="0.259" WIDTH="348"/>
                contents.add(XMLStepper.getAttribute(xml, "CONTENT", null));

                String subsType = XMLStepper.getAttribute(xml, "SUBS_TYPE", null);
                if ("HypPart1".equals(subsType) || "HypPart2".equals(subsType)) {
                    contents.add(XMLStepper.getAttribute(xml, "SUBS_CONTENT", null));
                }
            }

            private void addHighlightBoxIfMatch(XMLStreamReader xml, String content) {
                if (highlightTermMatchMatches(content)) {
                    boxResponse.add(recordID, getBox(xml, content));
                }
            }

            private void addHighlightBoxIfMatch(AltoBoxResponse.Box box, String content) {
                if (highlightTermMatchMatches(content)) {
                    AltoBoxResponse.Box boxClone = new AltoBoxResponse.Box(
                            box.hpos, box.vpos, box.width, box.height, content, box.wc, box.cc, box.relative);
                    boxResponse.add(recordID, boxClone);
                }
            }

            private boolean highlightTermMatchMatches(String content) {
                if (boxResponse.getLookupTerms().contains(content)) {
                    return true;
                }
                for (String token : altoStringSplitter.split(content)) {
                    if (boxResponse.getLookupTerms().contains(token)) {
                        return true;
                    }
                }
                return false;
            }

            private AltoBoxResponse.Box getBox(XMLStreamReader xml, String content) {
                if (!boxResponse.isRelativeCoordinates() || pageWidth == -1 || pageHeight == -1) {
                    return new AltoBoxResponse.Box(
                            getInt(xml, "HPOS"), getInt(xml, "VPOS"),
                            getInt(xml, "WIDTH"), getInt(xml, "HEIGHT"),
                            content,
                            XMLStepper.getAttribute(xml, "WC", "N/A"),
                            XMLStepper.getAttribute(xml, "CC", "N/A"), false);
                }

                int ph = boxResponse.isYisx() ? pageHeight : pageWidth;
                return new AltoBoxResponse.Box(
                        getRel(xml, "HPOS", pageWidth), getRel(xml, "VPOS", ph),
                        getRel(xml, "WIDTH", pageWidth), getRel(xml, "HEIGHT", ph),
                        content,
                        XMLStepper.getAttribute(xml, "WC", "N/A"),
                        XMLStepper.getAttribute(xml, "CC", "N/A"), true);
            }

            private double getRel(XMLStreamReader xml, String attributeName, int page) {
                return Double.parseDouble(XMLStepper.getAttribute(xml, attributeName, "-1")) / page;
            }

            private int getInt(XMLStreamReader xml, String attributeName) {
                return Integer.parseInt(XMLStepper.getAttribute(xml, attributeName, "-1"));
            }
        });
        log.debug("Finished extracting at least " + boxResponse.getBoxes().size() + " boxes for " + recordID);
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
