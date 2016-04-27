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
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAdjusterBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

/**
 * Delegator for {@link QueryReducer} with a single sub node.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryReducerComposite extends HubAdjusterBase {
    private static Log log = LogFactory.getLog(QueryReducerComposite.class);

    private final QueryReducer reducer;

    public QueryReducerComposite(Configuration conf) {
        super(conf);
        reducer = new QueryReducer(conf);
        checkSubComponents();
        log.info("Created " + this);
    }

    private void checkSubComponents() {
        if (getComponents().size() != 1) {
            throw new IllegalStateException(
                    "The QueryReducerComposite must have exactly 1 sub component but had " + getComponents().size());
        }
    }

    @Override
    public SolrParams adjustRequest(ModifiableSolrParams request) {
        return reducer.reduce(getComponents().get(0).getID(), request);
    }

    @Override
    public QueryResponse adjustResponse(SolrParams request, QueryResponse response) {
        return response;
    }

    @Override
    protected QueryResponse barrierSearch(Limit limit, ModifiableSolrParams params) throws Exception {
        return getComponents().get(0).search(limit, params);
    }

    @Override
    public String toString() {
        return "QueryReducerComposite(reducer=" + reducer + ')';
    }
}
