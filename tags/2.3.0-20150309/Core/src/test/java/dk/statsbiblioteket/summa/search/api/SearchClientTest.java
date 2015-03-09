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
package dk.statsbiblioteket.summa.search.api;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.search.rmi.SummaRest;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchClientTest extends TestCase {
    private static final String SID = "mysearcher";
    private static Log log = LogFactory.getLog(SearchClientTest.class);

    public static final String REMOTE = "//mars:56700/aviser-searcher";

    @Override
    public void setUp() throws Exception {
        SummaRest.getInstance().register(SID, new SummaSearcher() {
            @Override
            public ResponseCollection search(Request request) throws IOException {
                ResponseCollection responses = new ResponseCollection();
                DocumentResponse docResponse = new DocumentResponse(
                        request.getString(DocumentKeys.SEARCH_FILTER), request.getString(DocumentKeys.SEARCH_QUERY),
                        0, 10, null, false, null, 123, 456);
                docResponse.addRecord(new DocumentResponse.Record("myrecord", "dummy", 0.5f, null));
                responses.add(docResponse);
                return responses;
            }

            @Override
            public void close() throws IOException {}
        });
        SummaRest.getInstance().checkContainer();
    }

    @Override
    public void tearDown() throws Exception {
        SummaRest.getInstance().unregister(SID);
    }

    public void testRemoteRMI() throws IOException {
        SummaSearcher searcher =
                new SearchClient(Configuration.newMemoryBased(ConnectionConsumer.CONF_RPC_TARGET, REMOTE));
        ResponseCollection responses = searcher.search(new Request(DocumentSearcher.SEARCH_QUERY, "hest"));
        assertFalse("There should be at least one response", responses.isEmpty());
        searcher.close();
    }

    public void testRemoteRest() throws IOException {
        SummaSearcher searcher =
                new SearchClient(Configuration.newMemoryBased(SearchClient.CONF_SERVER, "http://localhost:8081"));
        ResponseCollection responses = searcher.search(new Request(DocumentSearcher.SEARCH_QUERY, "hest"));
        assertFalse("There should be at least one response", responses.isEmpty());
        searcher.close();
    }

}
