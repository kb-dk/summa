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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.io.IOException;
import java.rmi.RemoteException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PagingSearchNodeTest extends TestCase {

    public void testBasicSearch() throws Exception {
        ResponseCollection responses = search(true, 50, 20, new Request(
                DocumentKeys.SEARCH_QUERY, "foo"
        ));
        assertResponse("Base search", responses, 0, 20, 20, 20);
    }

    public void testLargeSearch() throws Exception {
        ResponseCollection responses = search(true, 50, 20, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 40
        ));
        assertResponse("Base search", responses, 0, 40, 40, 40);
    }

    public void testExceedingMaxPage() throws Exception {
        ResponseCollection responses = search(true, 50, 20, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 60
        ));
        assertResponse("Base search", responses, 0, 60, 60, 60);
    }

    public void testDummyNode() {
        new DummyNode(Configuration.newMemoryBased());
    }

    private void assertResponse(String message, ResponseCollection responses,
                                int startIndex, int maxRecords, int hitCount, int recordCount) {
        DocumentResponse docs = null;
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                docs = (DocumentResponse)response;
            }
        }
        if (docs == null) {
            fail("No DocumentResponse in " + responses);
        }
        assertEquals(message + ". startIndex should be correct", startIndex, docs.getStartIndex());
        assertEquals(message + ". maxRecords should be correct", maxRecords, docs.getMaxRecords());
        assertEquals(message + ". hitCount should be correct", hitCount, docs.getHitCount());
        assertEquals(message + ". recordCount should be correct", recordCount, docs.getRecords().size());
        int id = startIndex;
        for (DocumentResponse.Record record: docs.getRecords()) {
            assertEquals(message + ". Record ID should be correct ", "index_" + id++, record.getId());
        }
    }

    private ResponseCollection search(boolean sequential, int maxPagesize, int guiPagesize, Request request)
            throws IOException {
        SearchNode pager = getPager(sequential, maxPagesize, guiPagesize);
        ResponseCollection responses = new ResponseCollection();
        pager.search(request, responses);
        return responses;
    }

    private SearchNode getPager(boolean sequential, int maxPagesize, int guiPagesize) throws IOException {
        Configuration conf = Configuration.newMemoryBased(
                PagingSearchNode.CONF_SEQUENTIAL, sequential,
                PagingSearchNode.CONF_MAXPAGESIZE, maxPagesize,
                PagingSearchNode.CONF_GUIPAGESIZE, guiPagesize,
                SearchNodeFactory.CONF_NODE_CLASS, DummyNode.class);
        return new PagingSearchNode(conf);
    }

    public static class DummyNode implements SearchNode {
        public DummyNode(Configuration conf) {

        }

        @Override
        public void search(Request request, ResponseCollection responses) throws RemoteException {
            int start = request.getInt(DocumentKeys.SEARCH_START_INDEX, 0);
            int maxRecords = request.getInt(DocumentKeys.SEARCH_MAX_RECORDS, 20);
            DocumentResponse docs = new DocumentResponse(
                    request.getString(DocumentKeys.SEARCH_FILTER, null),
                    request.getString(DocumentKeys.SEARCH_QUERY, null),
                    request.getInt(DocumentKeys.SEARCH_START_INDEX, 0),
                    maxRecords,
                    request.getString(DocumentKeys.SEARCH_SORTKEY, null),
                    request.getBoolean(DocumentKeys.SEARCH_REVERSE, false),
                    request.getStrings(DocumentKeys.SEARCH_RESULT_FIELDS, new String[0]),
                    0L, // searchTime
                    maxRecords);
            for (int i = start ; i < start + maxRecords ; i++) {
                DocumentResponse.Record record = new DocumentResponse.Record(
                        "index_" + i, "dummy", 0.001f + i, Integer.toString(i));
                record.addField(new DocumentResponse.Field("recordID", "index_" + i, false));
                record.addField(new DocumentResponse.Field("recordBase", "dummy", false));
                docs.addRecord(record);
            }
            responses.add(docs);
        }

        @Override
        public void warmup(String request) { }

        @Override
        public void open(String location) throws RemoteException { }

        @Override
        public void close() throws RemoteException { }

        @Override
        public int getFreeSlots() {
            return Integer.MAX_VALUE;
        }
    }
}
