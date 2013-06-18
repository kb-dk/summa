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

import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAggregatorBase;
import org.apache.solr.common.params.SolrParams;

import java.util.List;

/**
 * Modifies incoming requests.
 */
public interface RequestAdjuster {

    /**
     * @param params     original parameters for the calling HubComposite.
     * @param components the components that will be called, including component-specific parameters.
     * @return the adjusted component list.
     */
    List<HubAggregatorBase.ComponentCallable> adjustRequests(
            SolrParams params, List<HubAggregatorBase.ComponentCallable> components);
}
