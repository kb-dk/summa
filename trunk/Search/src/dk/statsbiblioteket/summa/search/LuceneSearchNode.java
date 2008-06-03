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
package dk.statsbiblioteket.summa.search;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;

/**
 * Lucene-specific search node.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneSearchNode implements SearchNode, Configurable {
    private static Log log = LogFactory.getLog(LuceneSearchNode.class);

    public static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private String[] defaultResultFields = DEFAULT_RESULT_FIELDS;
    private String[] defaultFallbackValues = DEFAULT_FALLBACK_VALUES;

    private LuceneIndexDescriptor descriptor;
    private SummaQueryParser parser;
    private IndexSearcher searcher;
    private String location = null;
    private static final long WARMUP_MAX_HITS = 50;

    /**
     * Constructs a Lucene search node from the given configuration. This
     * involves the creation of an index descriptor. It is recommended to
     * use {@link LuceneSearchNode(Configuration, LuceneIndexDescriptor)}
     * instead, as reuse of the descriptor lowers resource use.
     * @param conf         the setup for the node.
     * @throws IOException if the node could not be initialized.
     */
    public LuceneSearchNode(Configuration conf) throws IOException {
        descriptor = LuceneIndexUtils.getDescriptor(conf);
        parser = new SummaQueryParser(descriptor);
        init(conf);
    }

    /**
     * Constructs a Lucene search node from the given configuration and
     * uses the given descriptor. This is the recommended constructor.
     * @param conf         the setup for the node.
     * @param descriptor   the description of the index.
     * @throws IOException if the node could not be initialized.
     */
    public LuceneSearchNode(Configuration conf,
                            LuceneIndexDescriptor descriptor) throws
                                                              IOException {
        this.descriptor = descriptor;
        parser = new SummaQueryParser(descriptor);
        init(conf);
    }

    private void init(Configuration conf) {
        defaultResultFields = getStrings(conf, CONF_RESULT_FIELDS,
                                         defaultResultFields, "result-fields");
        defaultFallbackValues = getStrings(conf, CONF_FALLBACK_VALUES,
                                           defaultFallbackValues,
                                           "fallback-values");
        if (defaultFallbackValues != null
            && defaultResultFields.length != defaultFallbackValues.length) {
            throw new IllegalArgumentException(String.format(
                    "The number of fallback-values(%s) was not equal to the "
                    + "number of fields(%s)", defaultFallbackValues.length,
                                              defaultResultFields.length));
        }
    }

    public void open(String location) throws IOException {
        log.debug("Open called for location '" + location + "'");
        this.location = location;
        close();
        URL urlLocation = Resolver.getURL(location);
        if ("".equals(urlLocation.getFile())) {
            throw new IOException(String.format(
                    "Could not resolve file from location '%s'", location));
        }
        searcher = new IndexSearcher(
                FSDirectory.getDirectory(urlLocation.getFile()));
        log.debug("Open finished for location '" + location + "'");
    }

    public void close() {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (IOException e) {
                log.warn(String.format(
                        "Could not close index-connection to location '%s'. "
                        + "This will probably result in a resource-leak",
                        location), e);
            }
            //noinspection AssignmentToNull
            searcher = null;
        }
    }

    public void warmup(String query, String sortKey, String[] fields) {
        //noinspection OverlyBroadCatchBlock
        try {
            fullSearch(null, query, 0, WARMUP_MAX_HITS, sortKey, false,
                       fields, defaultFallbackValues);
        } catch (Throwable t) {
            log.warn("Throwable caught in warmup", t);
        }
    }

    public String fullSearch(String filter, String query, long startIndex,
                             long maxRecords, String sortKey,
                             boolean reverseSort, String[] fields,
                             String[] fallbacks) throws RemoteException {
        return fullSearch(filter, query, startIndex, maxRecords,
                          sortKey, reverseSort, fields, fallbacks, true);
    }

    public String fullSearch(String filter, String query, long startIndex,
                             long maxRecords, String sortKey,
                             boolean reverseSort, String[] fields,
                             String[] fallbacks, boolean doLog) throws
                                                               RemoteException {
        try {
            if (log.isTraceEnabled() && doLog) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("fullSearch('" + filter + "', '" + query + "', "
                          + startIndex + ", " + maxRecords + ", '" + sortKey
                          + "', " + reverseSort + ", " + Arrays.toString(fields)
                          + ", " + Arrays.toString(fallbacks) + ") called");
            }
            long startTime = System.currentTimeMillis();
            if (fields == null) {
                fields = defaultResultFields;
                fallbacks = defaultFallbackValues;        
            }
            Filter filterO = filter == null || "".equals(filter) ? null :
                             new QueryWrapperFilter(parser.parse(filter));
            Query queryO = parser.parse(query);
            // TODO: Port sorting from Stable
            TopFieldDocs topDocs = searcher.search(
                    queryO, filterO,
                    (int)(startIndex + maxRecords), Sort.RELEVANCE);

            FieldSelector selector = new SetBasedFieldSelector(
                    new HashSet<String>(Arrays.asList(fields)),
                    new HashSet(5));

            StringWriter sw = new StringWriter(500);
            sw.append(XML_HEADER + "\n");
            sw.append("<searchresult");
            sw.append(filterO == null ? "" :
                      " filter=\"" + encode(filter) + "\"");
            sw.append(" query=\"").append(encode(query)).append("\"");
            sw.append(" startIndex=\"").append(Long.toString(startIndex));
            sw.append("\"");
            sw.append(" maxRecords=\"").append(Long.toString(maxRecords));
            sw.append("\"");
            sw.append(sortKey == null || "".equals(sortKey) ? "" :
                      " sortKey=\"" + encode(sortKey) + "\"");
            sw.append(" reverseSort=\"").append(Boolean.toString(reverseSort));
            sw.append("\"");
            sw.append(" fields=\"");
            for (int i = 0 ; i < fields.length ; i++) {
                sw.append(encode(fields[i]));
                if (i < fields.length-1) {
                    sw.append(", ");
                }
            }
            sw.append("\"");
            sw.append(" searchTime=\"");
            sw.append(Long.toString(System.currentTimeMillis()-startTime));
            sw.append("\">\n");
            for (int i = 0 ; i < topDocs.scoreDocs.length ; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                sw.append("<record score=\"");
                sw.append(Float.toString(scoreDoc.score)).append("\"");
                // TODO: Get the sortValue - how? To require stored is bad
                sw.append(" sortValue=\"NA\">\n");
                Document doc =
                     searcher.getIndexReader().document(scoreDoc.doc, selector);
                for (int f = 0 ; f < fields.length ; f++) {
                    sw.append("<field name=\"").append(fields[f]).append("\">");
                    Field iField = doc.getField(fields[f]);
                    sw.append(encode(iField == null ?
                                     fallbacks == null || fallbacks.length == 0
                                     ? "" : fallbacks[f] : fields[f]));
                    sw.append("</field>\n");
                }
                sw.append("</record>\n");
            }
            sw.append("</searchresult>\n");
            if (doLog) {
                log.debug("fullSearch(..., '" + query + "', ...) done in "
                          + (System.currentTimeMillis()-startTime) + " ms");
            }
            return sw.toString();
        } catch (ParseException e) {
            throw new RemoteException(String.format(
                    "ParseException during search for query '%s'", query), e);
        } catch (CorruptIndexException e) {
            throw new RemoteException(String.format(
                    "CorruptIndexException during search for query '%s'",
                    query), e);
        } catch (IOException e) {
            throw new RemoteException(String.format(
                    "IOException during search for query '%s'", query), e);
        } catch (Throwable t) {
            throw new RemoteException(String.format(
                    "Exception during search for query '%s'", query), t);
        }
    }

    private static String encode(String in) {
        in = in.replaceAll("&", "&amp;");
        in = in.replaceAll("\"", "&quot;");
        in = in.replaceAll("<", "&lt;");
        return in.replaceAll(">", "&gt;");
    }

    // TODO: Consider moving this to Configuration
    private String[] getStrings(Configuration conf, String key,
                                String[] defaultValues, String type) {
        String[] result;
        try {
            List<String> fields = conf.getStrings(key);
            result = fields.toArray(new String[fields.size()]);
            log.debug("Assigning " + type + " " + Arrays.toString(result));
            return result;
        } catch (NullPointerException e) {
            log.debug("Result-fields not specified in configuration. "
                      + "Using default " + type + " "
                      + Arrays.toString(defaultValues));
        } catch (IllegalArgumentException e) {
            log.warn(String.format(
                    "The property %s was expected to be a list of Strings, but "
                    + "it was not. Using default %s %s instead",
                    key, type, Arrays.toString(defaultValues)));
        }
        return defaultValues;
    }

}
