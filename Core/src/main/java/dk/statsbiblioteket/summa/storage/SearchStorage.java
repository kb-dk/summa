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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.summa.search.api.*;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Proxy storage that either uses an embedded SearchNode or a remote SummaSearcher for requesting Records by issuing
 * ID-based searches and transforming the result. Note that this Storage only supports the methods {@link #getRecord}
 * and {@link #getRecord}.
 * </p><p>
 * Using an embedded SearchNode is done by specifying
 * {@link dk.statsbiblioteket.summa.search.api.SummaSearcher#CONF_CLASS}, which is used by {@link SearchNodeFactory}.
 * Normally it will also be necessary to specify SearchNode specific properties.
 * </p><p>
 * Using a remote Searcher is done by specifying
 * {@link dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer#CONF_RPC_TARGET}, which is used by
 * {@link SearchClient}. Normally it is not necessary to specify more properties.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchStorage implements Storage {
    private static Log recordlog = LogFactory.getLog("storagequeries");
    private static Log log = LogFactory.getLog(SearchStorage.class);

    private final SearchNode searchNode;
    private final SearchClient searchClient;

    /**
     * The maximum number of IDs that will be requested from the SearchNode at the same time in {@link #getRecords}.
     * </p><p>
     * Warning: Do not set this insanely high as the batch size is part of the request to the remote searcher.
     *          Some searchers, notably Lucene and Solr, has practical limits for the number of results to return.
     * </p><p>
     * Optional. Default is 50.
     */
    public static final String CONF_BATCH_SIZE = "searchstorage.batchsize";
    public static final int DEFAULT_BATCH_SIZE = 50;

    /**
     * The field used for unique ID in the backing searcher.
     * </p><p>
     * Optional. Default is recordID.
     */
    public static final String CONF_ID_FIELD = "searchstorage.id";
    public static final String DEFAULT_ID_FIELD = IndexUtils.RECORD_FIELD;

    /**
     * The recordBase to assign to returned Records.
     * </p><p>
     * Optional. Default is 'searchstorage'.
     */
    public static final String CONF_RECORD_BASE = "searchstorage.recordbase";
    public static final String DEFAULT_RECORD_BASE = "searchstorage";

    /**
     * If true the remote searcher is expected to support {@link DocumentKeys#SEARCH_IDS}. If false, non-optimized
     * Lucene queries are used.
     * </p><p>
     * Optional. Default is true as all Summa-searchers supports this.
     */
    public static final String CONF_EXPLICIT_ID_SUPPORTED = "searchstorage.explicitidsearch";
    public static final boolean DEFAULT_EXPLICIT_ID_SUPPORTED = true;

    private final int batchSize;
    private final String idField;
    private final String recordBase;
    private final boolean explicitIDSearch;
    private final Profiler profiler = new Profiler(Integer.MAX_VALUE, 100);

    public SearchStorage(Configuration conf) throws RemoteException {
        this(conf,
             conf.valueExists(SummaSearcher.CONF_CLASS) ? SearchNodeFactory.createSearchNode(conf) : null,
             conf.valueExists(ConnectionConsumer.CONF_RPC_TARGET) ? new SearchClient(conf) : null);
    }

    public SearchStorage(Configuration conf, SearchNode searchNode) throws RemoteException {
        this(conf, searchNode, null);
    }

    public SearchStorage(Configuration conf, SearchClient searchClient) throws RemoteException {
        this(conf, null, searchClient);
    }

    SearchStorage(Configuration conf, SearchNode searchNode, SearchClient searchClient) throws RemoteException {
        this.searchNode = searchNode;
        this.searchClient = searchClient;
        batchSize = conf.getInt(CONF_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        idField = conf.getString(CONF_ID_FIELD, DEFAULT_ID_FIELD);
        recordBase = conf.getString(CONF_RECORD_BASE, DEFAULT_RECORD_BASE);
        explicitIDSearch = conf.getBoolean(CONF_EXPLICIT_ID_SUPPORTED, DEFAULT_EXPLICIT_ID_SUPPORTED);
        log.info("Created SearchStorage with batchSize=" + batchSize + ", idField='" + idField
                 + "', recordBase='" + recordBase + "', explicitIDSearch=" + explicitIDSearch + ", backed by "
                 + (searchNode == null ? "SearchClient(" + conf.getString(ConnectionConsumer.CONF_RPC_TARGET) + ")" :
                    "embedded SearchNode(" + conf.getString(SummaSearcher.CONF_CLASS, "N/A") + ")"));
    }

    /**
     * breaks down the IDs in batches of the size specified by {@link #CONF_BATCH_SIZE} and issues an OR-based search
     * for the IDs in each chunk, converting the search result into Records.
     * @param ids List of ids to fetch.
     * @param options ignored by this Storage.
     * @return Records corresponding to the given IDs.
     * @throws IOException if the searcher did not answer as expected.
     */
    @Override
    public List<Record> getRecords(List<String> ids, QueryOptions options) throws IOException {
        return getRecords(ids, options, true);
    }

    public List<Record> getRecords(List<String> ids, QueryOptions options, boolean doLog) throws IOException {
        return explicitIDSearch ?
                getRecordsExplicitIDSearch(ids, options, doLog) :
                getRecordsNoExplicitIDSearch(ids, options, doLog);
    }
    protected List<Record> getRecordsExplicitIDSearch(List<String> ids, QueryOptions options, boolean doLog)
            throws IOException {
        final long startTime = System.currentTimeMillis();
        DocumentResponse documents = getDocumentResponse(new Request(
                DocumentKeys.SEARCH_IDS, new ArrayList<>(ids)));
        List<Record> result = documents != null ? convertDocuments(documents) : new ArrayList<Record>(0);
        profiler.beat();
        String message = "Finished getRecords(" + (ids.size() == 1 ? ids.get(0) : ids.size() + " record ids") + ") "
                       + "-> " + result.size() + " records in " + (System.currentTimeMillis() - startTime) + "ms. "
                       + getRequestStats();
        log.debug(message);
        if (doLog) {
            recordlog.info(message);
        }
        return result;

    }
    // TODO: Use the first class document retrieval instead
    protected List<Record> getRecordsNoExplicitIDSearch(List<String> ids, QueryOptions options, boolean doLog)
            throws IOException {
        final long startTime = System.currentTimeMillis();
        List<Record> result = new ArrayList<>(ids.size());
        if (ids.size() > batchSize) {
            List<String> subIDs = new ArrayList<>(batchSize);
            for (String id: ids) {
                subIDs.add(id);
                if (subIDs.size() == batchSize) {
                    result.addAll(getRecords(subIDs, options, doLog));
                    subIDs.clear();
                }
            }
            if (!subIDs.isEmpty()) {
                result.addAll(getRecords(subIDs, options, doLog));
            }
            return result;
        }

        StringWriter sw = new StringWriter();
        for (int i = 0 ; i < ids.size() ; i++) {
            if (i != 0) {
                sw.append(" OR ");
            }
            sw.append(makeQuery(ids.get(i)));
        }
        DocumentResponse documents = getDocumentResponse(sw.toString());
        if (documents != null) { // Null means no hits
            result = convertDocuments(documents);
        }
        String message = "Finished getRecords(" + (ids.size() == 1 ? ids.get(0) : ids.size() + " record ids") + ") "
                       + "-> " + result.size() + " records in " + (System.currentTimeMillis() - startTime) + "ms. "
                       + getRequestStats();
        log.debug(message);
        if (doLog) {
            recordlog.info(message);
        }
        return result;
    }

    private List<Record> convertDocuments(DocumentResponse documents) {
        List<Record> records = new ArrayList<>(batchSize);
        for (DocumentResponse.Record doc: documents.getRecords()) {
            StringWriter sw = new StringWriter();
            doc.toXML(sw, "");
            records.add(new Record(doc.getId(), recordBase, sw.toString().getBytes(StandardCharsets.UTF_8)));
        }
        return records;
    }

    private DocumentResponse getDocumentResponse(String query) throws IOException {
        Request request = new Request(
            DocumentKeys.SEARCH_QUERY, query,
            DocumentKeys.SEARCH_MAX_RECORDS, batchSize,
            DocumentKeys.SEARCH_COLLECT_DOCIDS, false
        );
        return getDocumentResponse(request);
    }

    private DocumentResponse getDocumentResponse(Request request) throws IOException {
        ResponseCollection responses;
        if (searchClient == null) {
            responses = new ResponseCollection();
            searchNode.search(request, responses);
        } else {
            responses = searchClient.search(request);
        }
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                return (DocumentResponse)response;
            }
        }
        return null;
    }

    @Override
    public Record getRecord(String id, QueryOptions options) throws IOException {
        long startTime = System.currentTimeMillis();
        List<Record> records = getRecords(Arrays.asList(id), options, false);
        log.debug("getRecord(" + id + ", ...) returned " + records.size() + " records");
        String m = "Finished getRecord(" + id + ", ...) " + (records.isEmpty() ? "without" : "with")
                   + " result in " + (System.currentTimeMillis() - startTime) + "ms. " + getRequestStats();
        recordlog.info(m);
        return records.isEmpty() ? null : records.get(0);
    }

    private String getRequestStats() {
        return "Stats(#getRecords=" + profiler.getBeats()
               + ", q/s(last " + profiler.getBpsSpan() + ")=" + profiler.getBps(true);
    }

    private String makeQuery(String id) {
        return idField + ":\"" + id + "\"";
    }

    @Override
    public long getRecordsModifiedAfter(long time, String base, QueryOptions options) throws IOException {
        throw new UnsupportedOperationException("The SearchStorage only supports getRecord(s)");
    }

    @Override
    public long getModificationTime(String base) throws IOException {
        throw new UnsupportedOperationException("The SearchStorage only supports getRecord(s)");
    }

    @Override
    public Record next(long iteratorKey) throws IOException {
        throw new UnsupportedOperationException("The SearchStorage only supports getRecord(s)");
    }

    @Override
    public List<Record> next(long iteratorKey, int maxRecords) throws IOException {
        throw new UnsupportedOperationException("The SearchStorage only supports getRecord(s)");
    }

    @Override
    public void flush(Record record, QueryOptions options) throws IOException {
        throw new UnsupportedOperationException("The SearchStorage only supports getRecord(s)");
    }

    @Override
    public void flush(Record record) throws IOException {
        throw new UnsupportedOperationException("The SearchStorage only supports getRecord(s)");
    }

    @Override
    public void flushAll(List<Record> records, QueryOptions options) throws IOException {
        throw new UnsupportedOperationException("The SearchStorage only supports getRecord(s)");
    }

    @Override
    public void flushAll(List<Record> records) throws IOException {
        throw new UnsupportedOperationException("The SearchStorage only supports getRecord(s)");
    }

    @Override
    public void close() throws IOException {
        // Do nothing
    }

    @Override
    public void clearBase(String base) throws IOException {
        throw new UnsupportedOperationException("The SearchStorage only supports getRecord(s)");
    }

    @Override
    public String batchJob(
        String jobName, String base, long minMtime, long maxMtime, QueryOptions options) throws IOException {
        throw new UnsupportedOperationException("The SearchStorage only supports getRecord(s)");
    }
}
