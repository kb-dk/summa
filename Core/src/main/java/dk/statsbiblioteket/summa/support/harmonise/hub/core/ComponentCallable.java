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
package dk.statsbiblioteket.summa.support.harmonise.hub.core;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;

import java.util.concurrent.Callable;

/**
 * Used by {@link HubAggregatorBase} to hold sub-component setups.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ComponentCallable  implements Callable<ComponentCallable.NamedResponse> {
    private final HubComponent component;
    private HubComponent.Limit limit;
    private ModifiableSolrParams params;

    public ComponentCallable(HubComponent component, HubComponent.Limit limit, ModifiableSolrParams params) {
        this.component = component;
        this.limit = limit;
        this.params = params;
    }

    @Override
    public NamedResponse call() throws Exception {
        return new NamedResponse(component.getID(), component.search(limit, params));
    }

    public HubComponent getComponent() {
        return component;
    }

    public HubComponent.Limit getLimit() {
        return limit;
    }

    public ModifiableSolrParams getParams() {
        return params;
    }

    public void setParams(ModifiableSolrParams params) {
        this.params = params;
    }

    public void setLimit(HubComponent.Limit limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return "ComponentCallable(component=" + component + ')';
    }

    public static class NamedResponse {
        private final String id;
        private final QueryResponse response;

        public NamedResponse(String id, QueryResponse response) {
            this.id = id;
            this.response = response;
        }

        public String getId() {
            return id;
        }

        public QueryResponse getResponse() {
            return response;
        }
    }
}
