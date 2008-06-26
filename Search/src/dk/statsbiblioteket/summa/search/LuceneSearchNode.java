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
import java.util.HashSet;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.index.IndexException;
import dk.statsbiblioteket.summa.common.util.ParseUtil;
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

    private String[] resultFields = DEFAULT_RESULT_FIELDS;
    private String[] fallbackValues = DEFAULT_FALLBACK_VALUES;
    private String sortKey = DEFAULT_DEFAULT_SORTKEY;

    private SummaSearcherMBean master = null;
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

    /**
     * Constructs a Lucene search node from the given configuration. It uses the
     * given descriptor and relies on master for default values. This is the
     * recommended constructor as it enables Java Bean based tweaks of the
     * default values.
     * @param master     the holder for default values.
     * @param conf         the setup for the node.
     * @param descriptor   the description of the index.
     * @throws IOException if the node could not be initialized.
     */
    public LuceneSearchNode(SummaSearcherMBean master, Configuration conf,
                            LuceneIndexDescriptor descriptor) throws
                                                              IOException {
        this.descriptor = descriptor;
        parser = new SummaQueryParser(descriptor);
        this.master = master;
        if (master == null) {
            init(conf);
        }
    }

    private void init(Configuration conf) {
        resultFields = conf.getStrings(CONF_RESULT_FIELDS, resultFields);
        fallbackValues = conf.getStrings(CONF_FALLBACK_VALUES, fallbackValues);
        sortKey = conf.getString(CONF_DEFAULT_SORTKEY, sortKey);
        log.debug("init with resultFields "
                  + Arrays.toString(resultFields)
                  + " and sort '" + sortKey + "'");
        if (fallbackValues != null
            && resultFields.length != fallbackValues.length) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException(String.format(
                    "The number of fallback-values(%s) was not equal to the "
                    + "number of result fields(%s)", fallbackValues.length,
                                              resultFields.length));
        }
    }

    public void open(String location) throws IOException {
        log.debug("Open called for location '" + location
                  + "'. Appending /" + LuceneIndexUtils.LUCENE_FOLDER);
        location +=  "/" + LuceneIndexUtils.LUCENE_FOLDER;
        this.location = location;
        close();
        if (location == null || "".equals(location)) {
            log.warn("open(null) called, no index available");
            return;
        }
        URL urlLocation = Resolver.getURL(location);
        if (urlLocation == null) {
            log.warn("Could not resolve URL for location '" + location
                     + "', no index available");
            return;
        }
        if ("".equals(urlLocation.getFile())) {
            throw new IOException(String.format(
                    // TODO: Consider if the exception should be eaten
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
                       fields, getFallbackValues());
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

    public String simpleSearch(String query, long startIndex,
                               long maxRecords) throws RemoteException {
        return fullSearch(null, query, startIndex, maxRecords,
                          null, false, null, null, true);
    }

    private String fullSearch(String filter, String query, long startIndex,
                              long maxRecords, String sortKey,
                              boolean reverseSort, String[] fields,
                              String[] fallbacks, boolean doLog) throws
                                                               RemoteException {
        if (searcher == null) {
            throw new IndexException("No searcher available", location, null);
        }
        if (startIndex > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "The Lucene search node does not support start indexes "
                    + "above Integer.MAX_VALUE. startIndex was " + startIndex);
        }
        if (maxRecords > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "The Lucene search node does not support max records "
                    + "above Integer.MAX_VALUE. max records was " + maxRecords);
        }
        if (startIndex + maxRecords > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "The Lucene search node does not support that start index +"
                    + " max records is above Integer.MAX_VALUE. "
                    + "start index was" + startIndex + " and max records was "
                    + maxRecords);
        }
        if (sortKey == null) {
            sortKey = getSortKey();
        }
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
                fields = getResultFields();
                fallbacks = getFallbackValues();
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
            sw.append(ParseUtil.XML_HEADER + "\n");
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
            sw.append("\"");
            sw.append(" hitCount=\"");
            sw.append(Integer.toString(topDocs.totalHits));
            sw.append("\">\n");
            for (int i = 0 ; i < topDocs.scoreDocs.length ; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                sw.append("<record score=\"");
                sw.append(Float.toString(scoreDoc.score)).append("\"");
                // TODO: Get the sortValue - how? It is bad to require stored
                if (SORT_ON_SCORE.equals(sortKey)) {
                    sw.append(" sortValue=\"");
                    sw.append(Float.toString(scoreDoc.score)).append("\">\n");
                } else {
                    sw.append(" sortValue=\"NA\">\n");
                }
                Document doc =
                     searcher.getIndexReader().document(scoreDoc.doc, selector);
                for (int f = (int)startIndex ;
                     f < fields.length && f < (int)(startIndex + maxRecords) ;
                     f++) {
                    sw.append("<field name=\"").append(fields[f]).append("\">");
                    Field iField = doc.getField(fields[f]);
                    String value = iField.stringValue();
                    sw.append(encode(value != null ? value :
                                     fallbacks == null || fallbacks.length == 0
                                     ? "" : fallbacks[f]));
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
            throw new IndexException(String.format(
                    "ParseException during search for query '%s'", query),
                                     location, e);
        } catch (CorruptIndexException e) {
            throw new IndexException(String.format(
                    "CorruptIndexException during search for query '%s'",
                    query), location, e);
        } catch (RemoteException e) {
            throw new RemoteException(String.format(
                    "Inner RemoteException during search for query '%s'",
                    query), e);
        } catch (IOException e) {
            throw new IndexException(String.format(
                    "IOException during search for query '%s'", query),
                                     location, e);
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


    /* Getters */

    public String[] getResultFields() throws RemoteException {
        if (master == null) {
            return resultFields;
        }
        return master.getResultFields();
    }
    public String[] getFallbackValues() throws RemoteException {
        if (master == null) {
            return fallbackValues;
        }
        return master.getFallbackValues();
    }
    public String getSortKey() throws RemoteException {
        if (master == null) {
            return sortKey;
        }
        return master.getSortKey();
    }

}
