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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponentImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.*;

/**
 * Test HubComponent that maintains a static list of all requests.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DummyLeaf extends HubComponentImpl {
    private static Log log = LogFactory.getLog(DummyLeaf.class);

    private static final Map<String, List<SolrParams>> receivedParams =
            Collections.synchronizedMap(new HashMap<String, List<SolrParams>>());

    public DummyLeaf(Configuration conf) {
        super(conf);
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public QueryResponse barrierSearch(Limit limit, ModifiableSolrParams params) throws Exception {
        List<SolrParams> existing = receivedParams.get(getID());
        if (existing == null) {
            existing = new ArrayList<>();
            receivedParams.put(getID(), existing);
        }
        existing.add(params);
        log.info("Received request " + params);
        return null;
    }

    @Override
    public List<String> getBases() {
        return Collections.emptyList();
    }

    @Override
    public MODE getMode() {
        return MODE.always;
    }

    @Override
    public boolean limitOK(Limit limit) {
        return true;
    }

    // We make this static as we do not always have the instantiation available
    public static Map<String, List<SolrParams>> getReceivedParams() {
        return receivedParams;
    }
}
