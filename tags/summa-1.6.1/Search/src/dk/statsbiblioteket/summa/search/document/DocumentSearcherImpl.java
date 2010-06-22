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
package dk.statsbiblioteket.summa.search.document;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

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
            List<String> nonEscaped = conf.getStrings(CONF_NONESCAPED_FIELDS);
            nonescapedFields.addAll(nonEscaped);
            log.debug("The following fields are unescaped: "
                      + Strings.join(nonEscaped, ", "));
        }
        fallbackValues = conf.getStrings(CONF_FALLBACK_VALUES, fallbackValues);

        if (fallbackValues != null
            && resultFields.length != fallbackValues.length) {
            log.error (String.format(
                    "The number of fallback-values(%s) was not equal to the "
                    + "number of result-fields(%s)", fallbackValues.length,
                                                     resultFields.length));
        }

        // Make sure that fallback values and result fields line up
        fallbackValues = fixFallbackValues(resultFields, fallbackValues);

        sortKey = conf.getString(CONF_DEFAULT_SORTKEY, sortKey);

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

    /**
     * Make sure that {@code resultFields} and {@code fallbackValues} has the
     * same length.
     * <p></p>
     * @param resultFields the fields that will be returned for request to this
     *                     searcher
     * @param fallbackValues the fallback values that should be checked for
     *                       validity
     * @return if {@code resultFields.length == fallbackValues.length} simply
     *         returns {@code fallbackValues}. If not returns a truncated or
     *         padded version of {@code fallbackValues}.
     *         If {@code fallbackValues == null} {@code null} is returned.
     */
    private String[] fixFallbackValues(String[] resultFields,
                                   String[] fallbackValues) {
        if (fallbackValues == null) {
            log.debug ("No fallback field values defined");
            return null;
        }

        if (resultFields.length == fallbackValues.length) {
            log.trace("Fallback field values configured correctly");
        }

        String[] newFallbacks = new String[resultFields.length];

        if (resultFields.length > fallbackValues.length) {
            // Pad fallbacks with the default fallback value
            System.arraycopy(fallbackValues, 0,
                             newFallbacks,  0,
                             fallbackValues.length);
            for (int i = 0; i < newFallbacks.length; i++) {
                newFallbacks[i] = null;
            }
        } else {
            // resultFields.length < fallbackValues.length, truncate fallbacks
            System.arraycopy(fallbackValues, 0,
                             newFallbacks,  0,
                             newFallbacks.length);
        }

        return newFallbacks;
    }

    public String simpleSearch(String query, long startIndex,
                               long maxRecords) throws RemoteException {
        return fullSearch(null, null, query, startIndex, maxRecords, null, false,
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
    @Override
    protected void managedSearch(Request request, ResponseCollection responses)
                                                        throws RemoteException {
        if (!isRequestUsable(request)) {
            return;
        }
        long startTime = System.currentTimeMillis();

        String query = request.getString(DocumentKeys.SEARCH_QUERY, null);
        String filter = request.getString(DocumentKeys.SEARCH_FILTER, null);
        //noinspection OverlyBroadCatchBlock
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
        String sortKey = request.getString(
                DocumentKeys.SEARCH_SORTKEY, this.sortKey);
        Boolean reverse = request.getBoolean(
                DocumentKeys.SEARCH_REVERSE, false);
        String[] resultFields = request.getStrings(
                DocumentKeys.SEARCH_RESULT_FIELDS, this.resultFields);
        String[] fallbackValues = request.getStrings(
                DocumentKeys.SEARCH_FALLBACK_VALUES, this.fallbackValues);

        boolean doCollectDocIDs =
                request.getBoolean(SEARCH_COLLECT_DOCIDS, collectDocIDs);

        if (records == 0 && !doCollectDocIDs) {  // Only hit count
            log.trace("Requested 0 records in search and no faceting. "
                      + "Performing fast hit counting");
            try {
                responses.add(new DocumentResponse(
                        filter, query, startIndex, records, sortKey,
                        reverse, resultFields,
                        System.currentTimeMillis() - startTime,
                        getHitCount(request, query, filter)));
            } catch (Exception e) {
                throw new RemoteException(String.format(
                        "Unable to perform fast hit counting for query "
                        + "'%s' with filter '%s'",
                        query, filter), e);
            }
        } else if (records > 0) { // Standard search
            log.trace("Performing standard search");
            try {
                if (fallbackValues != null &&
                    resultFields.length != fallbackValues.length) {
                    log.debug(String.format(
                            "Incoming request uses mistmatching result fields "
                            + "and fallback values %s(%s) and %s(%s)",
                            DocumentKeys.SEARCH_RESULT_FIELDS,
                            resultFields.length,
                            DocumentKeys.SEARCH_FALLBACK_VALUES,
                            fallbackValues.length));
                }
                fallbackValues =
                        fixFallbackValues(resultFields, fallbackValues);

                responses.add(fullSearch(
                        request, filter, query, startIndex, records, sortKey,
                        reverse, resultFields, fallbackValues));
            } catch (Exception e) {
                throw new RemoteException(String.format(
                        "Unable to perform search for query '%s' with filter "
                        + "'%s'", query, filter), e);
            }
        }
        if (doCollectDocIDs) { // Collect docIDs for faceting et al
            try {
                DocIDCollector collector = collectDocIDs(
                        request, query, filter);
                responses.getTransient().put(DOCIDS, collector);
                if (records == 0) {
                    responses.add(new DocumentResponse(
                            filter, query, startIndex, records, sortKey,
                            reverse, resultFields,
                            System.currentTimeMillis() - startTime,
                            collector.getBits().cardinality()));
                }
            } catch (IOException e) {
                throw new RemoteException(String.format(
                        "Unable to collect doc ids for query '%s', filter '%s'",
                        query, filter), e);
            }
        }
    }

    /**
     * Calculate the total number of hits for the given query and filter.
     * @param request the original request.
     * @param query   the search query.
     * @param filter  the search filter.
     * @return the total number of hits for a search with the given parameters.
     * @throws java.io.IOException if the search failed due to I/O errors.
     */
    protected abstract long getHitCount(
            Request request, String query, String filter) throws IOException;



    /**
     * Makes a quick test of the given request to see if a proper result should
     * be expected from a full processing.
     * @param request the request to judge.
     * @return true if this Searcher expects to be able to handle this request.
     */
    protected boolean isRequestUsable(Request request) {
        String query = request.getString(DocumentKeys.SEARCH_QUERY, null);
        String filter = request.getString(DocumentKeys.SEARCH_FILTER, null);
        if ((query == null || "".equals(query)) &&
            (filter == null || "".equals(filter))) {
            log.trace("No query, no filter, returning false");
            return false;
        }
        return true;
    }

    protected abstract DocIDCollector collectDocIDs(
            Request request, String query, String filter) throws IOException;


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




