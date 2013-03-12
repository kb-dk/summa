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
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAggregatorBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;

import java.util.List;

/**
 * Delegator for {@link TermStatRewriter} and {@link ResponseMerger}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatAggregator extends HubAggregatorBase {
    private static Log log = LogFactory.getLog(TermStatAggregator.class);

    private final ResponseMerger merger;
    private final TermStatRewriter rewriter;

    public TermStatAggregator(Configuration conf) {
        super(conf);
        merger = new ResponseMerger(conf);
        rewriter = new TermStatRewriter(conf);
        log.info("Created " + this);
    }


    @Override
    public QueryResponse merge(SolrParams params, List<NamedResponse> responses) {
        return merger.merge(params, responses);
    }

    @Override
    public List<ComponentCallable> adjustRequests(SolrParams params, List<ComponentCallable> components) {
        return rewriter.adjustRequests(params, components);
    }

    @Override
    public String toString() {
        return "TermStatAggregator(merger=" + merger + ", rewriter=" + rewriter + '}';
    }
}
