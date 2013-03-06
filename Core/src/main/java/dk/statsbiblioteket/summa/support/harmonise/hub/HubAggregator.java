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
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponent;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubCompositeImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.common.params.SolrParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregation of multiple HubComponents.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HubAggregator extends HubCompositeImpl {
    private static Log log = LogFactory.getLog(HubAggregator.class);

    public HubAggregator(Configuration conf) {
        super(conf);
        log.info("Created " + toString());
    }

    @Override
    public SolrResponse search(Limit limit, SolrParams params) throws Exception {
        List<HubComponent> subs = getComponents(limit);
        List<SolrResponse> responses = new ArrayList<SolrResponse>(subs.size());
        for (HubComponent node: subs) {
            responses.add(node.search(limit, params));
        }
        if (responses.isEmpty()) {
            return null;
        }
        if (responses.size() == 1) {
            return responses.get(0);
        }
        throw new UnsupportedOperationException("Merging not implemented yet");
    }


    @Override
    protected int maxComponents() {
        return Integer.MAX_VALUE;
    }

    public String toString() {
        return "HubAggregator(" + super.toString() + ")";
    }
}
