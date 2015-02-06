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
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
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
    private static Log log = LogFactory.getLog(SearchClientTest.class);

    public static final String REMOTE = "//mars:56700/aviser-searcher";

    public void testRemoteRMI() throws IOException {
        SummaSearcher searcher =
                new SearchClient(Configuration.newMemoryBased(ConnectionConsumer.CONF_RPC_TARGET, REMOTE));
        ResponseCollection responses = searcher.search(new Request(DocumentSearcher.SEARCH_QUERY, "hest"));
        assertFalse("There should be at least one response", responses.isEmpty());
        searcher.close();
    }
}
