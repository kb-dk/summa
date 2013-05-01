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
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Base for aggregation of multiple HubComponents. Note that this aggregator will fail if there is more than 1
 * response. Relevant methods to implement and/or overwrite are {@link #merge} and {@link #adjustRequests}.
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
    public QueryResponse barrierSearch(Limit limit, ModifiableSolrParams params) throws Exception {
        List<HubComponent> subs = getComponents(limit);

        String [] acceptableIDs = params.getParams(PARAM_ENABLED_COMPONENTS);
        List<ComponentCallable> pruned = new ArrayList<ComponentCallable>(subs.size());
        for (int i = subs.size()-1 ; i >= 0 ; i--) {
            String currentID = subs.get(i).getID();
            if (acceptableIDs != null) {
                for (String acceptable: acceptableIDs) {
                    if (currentID.equals(acceptable)) {
                        pruned.add(new ComponentCallable(subs.get(i), limit, params));
                    }
                }
            } else {
                pruned.add(new ComponentCallable(subs.get(i), limit, params));
            }
        }
        if (pruned.isEmpty()) {
            log.debug(getID() + ": No fitting sub components, returning null");
            return null;
        }
        pruned = adjustRequests(params, pruned);
        List<NamedResponse> responses = search(pruned);
        if (responses.isEmpty()) {
            return null;
        }
        if (responses.size() == 1) {
            return responses.get(0).getResponse();
        }
        return merge(params, responses);
    }

    private List<NamedResponse> search(List<ComponentCallable> subs) throws Exception {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(getComponents().size());
        }
        List<FutureTask<NamedResponse>> futures = new ArrayList<FutureTask<NamedResponse>>(subs.size());
        for (ComponentCallable sub: subs) {
            FutureTask<NamedResponse> future = new FutureTask<NamedResponse>(sub);
            futures.add(future);
            executor.submit(future);
            if (!threaded) {
                try {
                    future.get(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new InterruptedException(getID() + ": Interrupted while performing search on " + sub);
                } catch (ExecutionException e) {
                    throw new ExecutionException(getID() + ": exception while performing search on " + sub, e);
                } catch (TimeoutException e) {
                    throw new Exception(getID() + ": Exceeded timeout " + timeout + "ms while waiting for " + sub, e);
                }
            }
        }

        List<NamedResponse> responses = new ArrayList<NamedResponse>(futures.size());
        for (FutureTask<NamedResponse> future: futures) {
            try {
                responses.add(future.get(timeout, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                throw new InterruptedException(getID() + ": Interrupted while waiting for answer from " + future);
            } catch (ExecutionException e) {
                throw new ExecutionException(getID() + ": exception while waiting for answer from " + future, e);
            } catch (TimeoutException e) {
                throw new Exception(getID() + ": Exceeded timeout " + timeout + "ms while waiting for " + future, e);
            }
        }
        return responses;
    }

    /**
     * Optional adjustment of params and limits before calling sub component.
     * </p><p>
     * Important: If any Limits or SolrParams are changed, they must be deep-copies first as they are shared between
     * the components.
     * @param params the originating request.
     * @param components the components that will be searched.
     * @return the components to search, optionally with adjusted Limits and SolrParams.
     */
    public List<ComponentCallable> adjustRequests(ModifiableSolrParams params, List<ComponentCallable> components) {
        return components;
    }

    /**
     * Merges the given responses before they are returned after a search.
     * @param params the original parameters given to {@link #search(Limit, SolrParams)}.
     * @param responses the raw responses from the searches.
     * @return a single response merged from the full set of responses.
     */
    public abstract QueryResponse merge(SolrParams params, List<NamedResponse> responses);

    public class ComponentCallable implements Callable<NamedResponse> {
        private final HubComponent component;
        private Limit limit;
        private ModifiableSolrParams params;

        public ComponentCallable(HubComponent component, Limit limit, ModifiableSolrParams params) {
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

        public Limit getLimit() {
            return limit;
        }

        public ModifiableSolrParams getParams() {
            return params;
        }

        public void setParams(ModifiableSolrParams params) {
            this.params = params;
        }

        public void setLimit(Limit limit) {
            this.limit = limit;
        }

        @Override
        public String toString() {
            return "ComponentCallable(component=" + component + ')';
        }
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

    @Override
    protected int maxComponents() {
        return Integer.MAX_VALUE;
    }

    public String toString() {
        return "HubAggregatorBase(" + super.toString() + ", threaded=" + threaded + ", timeout=" + timeout + ")";
    }
}
