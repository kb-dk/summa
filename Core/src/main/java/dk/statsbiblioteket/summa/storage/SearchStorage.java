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
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
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

    private final int batchSize;
    private final String idField;
    private final String recordBase;

    public SearchStorage(Configuration conf) throws RemoteException {
        if (conf.valueExists(SummaSearcher.CONF_CLASS)) {
            log.debug("Creating an embedded SearchNode of class " + conf.getString(SummaSearcher.CONF_CLASS));
            searchNode = SearchNodeFactory.createSearchNode(conf);
            searchClient = null;
        } else if (conf.valueExists(ConnectionConsumer.CONF_RPC_TARGET)) {
            log.debug("Connecting to a remote SummaSearcher at " + conf.getString(ConnectionConsumer.CONF_RPC_TARGET));
            searchNode = null;
            searchClient = new SearchClient(conf);
        } else {
            throw new ConfigurationException("Either " + SummaSearcher.CONF_CLASS + " or "
                                             + ConnectionConsumer.CONF_RPC_TARGET + " must be specified");
        }
        batchSize = conf.getInt(CONF_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        idField = conf.getString(CONF_ID_FIELD, DEFAULT_ID_FIELD);
        recordBase = conf.getString(CONF_RECORD_BASE, DEFAULT_RECORD_BASE);
        log.info("Created SearchStorage with batchSize=" + batchSize + ", idField='" + idField
                 + "', recordBase='" + recordBase + "', backed by "
                 + (searchNode == null ? "SearchClient(" + conf.getString(ConnectionConsumer.CONF_RPC_TARGET) + ")" :
                    "embedded SearchNode(" + conf.getString(SummaSearcher.CONF_CLASS) + ")"));
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
        List<Record> result = new ArrayList<Record>(ids.size());
        if (ids.size() > batchSize) {
            List<String> subIDs = new ArrayList<String>(batchSize);
            for (String id: ids) {
                subIDs.add(id);
                if (subIDs.size() == batchSize) {
                    result.addAll(getRecords(subIDs, options));
                    subIDs.clear();
                }
            }
            if (subIDs.size() > 0) {
                result.addAll(getRecords(subIDs, options));
            }
            return result;
        } else {
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
        }
        log.debug("Returning " + result.size() + " Records from " + ids.size() + " IDs");
        return result;
    }

    private List<Record> convertDocuments(DocumentResponse documents) {
        List<Record> records = new ArrayList<Record>(batchSize);
        for (DocumentResponse.Record doc: documents.getRecords()) {
            StringWriter sw = new StringWriter();
            doc.toXML(sw);
            try {
                records.add(new Record(doc.getId(), recordBase, sw.toString().getBytes("utf-8")));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("utf-8 not supported", e);
            }
        }
        return records;
    }

    private DocumentResponse getDocumentResponse(String query) throws IOException {
        Request request = new Request(
            DocumentKeys.SEARCH_QUERY, query,
            DocumentKeys.SEARCH_MAX_RECORDS, batchSize,
            DocumentKeys.SEARCH_COLLECT_DOCIDS, false
        );
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
        List<Record> records = getRecords(Arrays.asList(id), options);
        log.debug("getRecord(" + id + ", ...) returned " + records.size() + " records");
        return records.size() == 0 ? null : records.get(0);
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
