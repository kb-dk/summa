/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.search.document;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;

/**
 * Default implementation of {@link DocumentSearcher} that handles
 * transformation of a {@link Request} to a method call with specific arguments.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class DocumentSearcherImpl extends SearchNodeImpl implements
                                                              DocumentSearcher {
    private static Log log = LogFactory.getLog(DocumentSearcherImpl.class);

    private String[] resultFields = DEFAULT_RESULT_FIELDS;
    private String[] fallbackValues = DEFAULT_FALLBACK_VALUES;
    /**
     * The result-fields that should not be entity-escaped (normally used for
     * inline XML).
     */
    protected Set<String> nonescapedFields = new HashSet<String>(10);
    private String sortKey = DEFAULT_DEFAULT_SORTKEY;
    private long maxRecords = DEFAULT_MAX_NUMBER_OF_RECORDS;
    private long startIndex = DEFAULT_START_INDEX;
    private long records = DEFAULT_RECORDS;
    private boolean collectDocIDs = DEFAULT_COLLECT_DOCIDS;

    protected ArrayBlockingQueue<DocIDCollector> collectors;

    public DocumentSearcherImpl(Configuration conf) throws RemoteException {
        super(conf);
        log.trace("Constructing DocumentSearcherImpl");
        resultFields = conf.getStrings(CONF_RESULT_FIELDS, resultFields);
        if (conf.valueExists(CONF_NONESCAPED_FIELDS)) {
            nonescapedFields.addAll(conf.getStrings(CONF_NONESCAPED_FIELDS));
        }
        fallbackValues = conf.getStrings(CONF_FALLBACK_VALUES, fallbackValues);
        sortKey = conf.getString(CONF_DEFAULT_SORTKEY, sortKey);
        if (fallbackValues != null
            && resultFields.length != fallbackValues.length) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException(String.format(
                    "The number of fallback-values(%s) was not equal to the "
                    + "number of result-fields(%s)", fallbackValues.length,
                                                     resultFields.length));
        }
        maxRecords = conf.getLong(CONF_MAX_RECORDS, maxRecords);
        if (maxRecords <= 0) {
            log.warn(String.format(
                    "The property %s must be >0. It was %s. Resetting to "
                    + "default %s",
                    CONF_MAX_RECORDS, maxRecords,
                    DEFAULT_MAX_NUMBER_OF_RECORDS == Long.MAX_VALUE ?
                    "Long.MAX_VALUE" :
                    DEFAULT_MAX_NUMBER_OF_RECORDS));
            maxRecords = DEFAULT_MAX_NUMBER_OF_RECORDS;
        }
        startIndex = conf.getLong(CONF_START_INDEX, DEFAULT_START_INDEX);
        records = conf.getLong(CONF_RECORDS, DEFAULT_RECORDS);
        collectDocIDs = conf.getBoolean(CONF_COLLECT_DOCIDS, collectDocIDs);
        if (getMaxConcurrentSearches() == 0) {
            throw new RemoteException("The number of maxConcurrentSearches is "
                                      + "0. No searches can be performed");
        }
        collectors = new ArrayBlockingQueue<DocIDCollector>(
                getMaxConcurrentSearches());
        for (int i = 0 ; i < getMaxConcurrentSearches() ; i++) {
            new DocIDCollector(collectors);
        }
    }

    public String simpleSearch(String query, long startIndex,
                               long maxRecords) throws RemoteException {
        return fullSearch(null, query, startIndex, maxRecords, null, false,
                          null, null).toXML();
    }

    /**
     * Parses the request for SEARCH*-key/value pairs and constructs arguments
     * for {@link #fullSearch}. The result from FullSearch is added to
     * responses.
     * </p><p>
     * If no {@link #SEARCH_QUERY} of {@link #SEARCH_FILTER} is defined, a
     * warning is logged and no search is performed.
     * @param request   the search request.
     * @param responses the responses from searches.
     * @throws RemoteException if the search failed.
     */
    protected void managedSearch(Request request, ResponseCollection responses)
                                                        throws RemoteException {
        String query = request.getString(DocumentKeys.SEARCH_QUERY, null);
        String filter = request.getString(DocumentKeys.SEARCH_FILTER, null);
        if ((query == null || "".equals(query)) &&
            (filter == null || "".equals(filter))) {
            log.warn("No query and no filter specified for document search, "
                     + "skipping search");
            return;
        }
        //noinspection OverlyBroadCatchBlock
        try {
            long startIndex = request.getLong(DocumentKeys.SEARCH_START_INDEX,
                                              this.startIndex);
            long records = request.getLong(DocumentKeys.SEARCH_MAX_RECORDS,
                                              this.records);
            if (records > maxRecords) {
                log.debug("requested records was " + records
                          + ", while fixed maxrecords was " + maxRecords +
                          ". Adjusting records to max");
                records = maxRecords;
            }
            String sortKey = request.getString(DocumentKeys.SEARCH_SORTKEY, this.sortKey);
            Boolean reverse = request.getBoolean(DocumentKeys.SEARCH_REVERSE, false);
            String[] resultFields = request.getStrings(DocumentKeys.SEARCH_RESULT_FIELDS,
                                                       this.resultFields);
            String[] fallbackValues = request.getStrings(DocumentKeys.SEARCH_FALLBACK_VALUES,
                                                         this.fallbackValues);
            responses.add(fullSearch(filter, query, startIndex, records,
                                     sortKey, reverse,
                                     resultFields, fallbackValues));
        } catch (Exception e) {
            throw new RemoteException(String.format(
                    "Unable to perform search for query '%s' with filter '%s'",
                    query, filter), e);
        }
        if (collectDocIDs) {
            try {
                responses.getTransient().put(DOCIDS,
                                             collectDocIDs(query, filter));
            } catch (IOException e) {
                throw new RemoteException(String.format(
                        "Unable to collect doc ids for query '%s', filter '%s'",
                        query, filter), e);
            }
        }
    }

    protected abstract DocIDCollector collectDocIDs(String query, String filter)
                                                            throws IOException;


    /* Mutators */

    public long getMaxRecords() {
        return maxRecords;
    }
    public void setMaxRecords(long maxRecords) {
        log.debug(String.format("setMaxRecords(%d) called", maxRecords));
        this.maxRecords = maxRecords;
    }
    public String getSortKey() {
        return sortKey;
    }
    public void setSortKey(String sortKey) {
        log.debug(String.format("setSortKey(%s) called", sortKey));
        this.sortKey = sortKey;
    }

    public String[] getResultFields() {
        return resultFields;
    }
    public void setResultFields(String[] resultFields) {
        log.debug(String.format("setResultFields(%s) called",
                                Arrays.toString(resultFields)));
        this.resultFields = resultFields;
    }

    public String[] getFallbackValues() {
        return fallbackValues;
    }
    public void setFallbackValues(String[] fallbackValues) {
        log.debug(String.format("setFallbackValues(%s) called",
                                Arrays.toString(fallbackValues)));
    }

    public long getRecords() {
        return records;
    }

    public void setRecords(long records) {
        this.records = records;
    }

    public long getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(long startIndex) {
        this.startIndex = startIndex;
    }

    public boolean isCollectDocIDs() {
        return collectDocIDs;
    }

    public void setCollectDocIDs(boolean collectDocIDs) {
        this.collectDocIDs = collectDocIDs;
    }

    public Set<String> getNonescapedFields() {
        return nonescapedFields;
    }

    public void setNonescapedFields(Set<String> nonescapedFields) {
        this.nonescapedFields = nonescapedFields;
    }
}
