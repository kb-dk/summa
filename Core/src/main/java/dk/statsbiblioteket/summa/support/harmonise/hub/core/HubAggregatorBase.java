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


import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Base for aggregation of multiple HubComponents. Note that this aggregator will fail if there is more than 1
 * response. It is highly recommended to override {@link #merge}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class HubAggregatorBase extends HubCompositeImpl {
    private static Log log = LogFactory.getLog(HubAggregatorBase.class);

    /**
     * A list of IDs for the components to used when performing a search. If the list is empty, all components are
     * used. If the list contains one or more IDs, only components with an ID that is in the list are considered as
     * valid candidates for an aggregated search.
     */
    public static final String PARAM_ENABLED_COMPONENTS = "active_components";

    /**
     * If true, requests to sub components are threaded. If false, sub components are processed sequentially.
     * </p><p>
     * Optional. Default is true;
     */
    public static final String CONF_THREADED = "aggregator.threaded";
    public static final boolean DEFAULT_THREADED = true;

    /**
     * The maximum number of milliseconds to wait for a response from sub components.
     * </p><p>
     * Optional. Default is 1 minute.
     */
    public static final String CONF_TIMEOUT = "aggregator.timeout";
    public static final int DEFAULT_TIMEOUT = 1 * 1000 * 60;

    private boolean threaded;
    private ExecutorService executor = null;
    private int timeout;

    public HubAggregatorBase(Configuration conf) {
        super(conf);
        threaded = conf.getBoolean(CONF_THREADED, DEFAULT_THREADED);
        timeout = conf.getInt(CONF_TIMEOUT, DEFAULT_TIMEOUT);
        log.info("Created " + toString());
    }

    @Override
    public QueryResponse barrierSearch(Limit limit, SolrParams params) throws Exception {
        List<HubComponent> subs = getComponents(limit);
        String [] acceptableIDs = params.getParams(PARAM_ENABLED_COMPONENTS);
        if (acceptableIDs != null) {
            List<HubComponent> pruned = new ArrayList<HubComponent>(subs.size());
            for (int i = subs.size()-1 ; i >= 0 ; i++) {
                String currentID = subs.get(i).getID();
                for (String acceptable: acceptableIDs) {
                    if (currentID.equals(acceptable)) {
                        pruned.add(subs.get(i));
                    }
                }
            }
            subs = pruned;
        }
        if (subs.isEmpty()) {
            log.debug(getID() + ": No fitting sub components, returning null");
            return null;
        }
        List<NamedResponse> responses = search(subs, limit, params);
        if (responses.isEmpty()) {
            return null;
        }
        if (responses.size() == 1) {
            return responses.get(0).getResponse();
        }
        return merge(responses);
    }

    private List<NamedResponse> search(
            List<HubComponent> subs, final Limit limit, final SolrParams params) throws Exception {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(getComponents().size());
        }
        List<FutureTask<NamedResponse>> futures = new ArrayList<FutureTask<NamedResponse>>(subs.size());
        for (HubComponent sub : subs) {
            FutureTask<NamedResponse> future = new FutureTask<NamedResponse>(
                    new ComponentCallable(sub, limit, params));
            futures.add(future);
            executor.submit(future);
            if (!threaded) {
                try {
                    if (!executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                        throw new Exception(getID() + ": Exceeded timeout " + timeout + "ms while waiting for " + sub);
                    }
                } catch (InterruptedException e) {
                    throw new InterruptedException(getID() + ": Interrupted while performing search on " + sub);
                }
            }
        }
        if (threaded) {
            try {
                if (!executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                    throw new Exception(getID() + ": Exceeded timeout " + timeout + "ms while waiting for "
                                        + futures.size() + " sub-searchers");
                }
            } catch (InterruptedException e) {
                throw new InterruptedException(getID() + ": Interrupted while performing search on " + futures.size()
                                               + " sub-searchers");
            }
        }
        List<NamedResponse> responses = new ArrayList<NamedResponse>(futures.size());
        for (FutureTask<NamedResponse> future : futures) {
            responses.add(future.get());
        }
        return responses;
    }

    public class ComponentCallable implements Callable<NamedResponse> {
        private final HubComponent component;
        private final Limit limit;
        private final SolrParams params;

        public ComponentCallable(HubComponent component, Limit limit, SolrParams params) {
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
    }

    /**
     * Merges the given responses before they are returned after a search.
     * @param responses the raw responses from the searches.
     * @return a single response merged from the full set of responses.
     */
    public abstract QueryResponse merge(List<NamedResponse> responses);

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

    @Override
    protected int maxComponents() {
        return Integer.MAX_VALUE;
    }

    public String toString() {
        return "HubAggregatorBase(" + super.toString() + ", threaded=" + threaded + ", timeout=" + timeout + ")";
    }
}
