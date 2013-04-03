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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAggregatorBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Merges responses with the possibility of prioritizing certain sources over others.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ResponseMerger implements Configurable {
    private static Log log = LogFactory.getLog(ResponseMerger.class);

    /**
     * If true, searches are assumed to have been performed in parallel. If false, they are assumed to have been
     * performed sequentially. This is used for calculating query times.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_PARALLEL_SEARCHES = "responsemerger.parallelsearches";
    public static final boolean DEFAULT_PARALLEL_SEARCHES = true;

    // responseHeaders
    public static final String STATUS = "status";
    public static final String QTIME = "QTime";

    private final boolean parallelSearches;

    public ResponseMerger(Configuration conf) {
        parallelSearches = conf.getBoolean(CONF_PARALLEL_SEARCHES, DEFAULT_PARALLEL_SEARCHES);
        log.info("Created " + this);
    }

    public QueryResponse merge(SolrParams params, List<HubAggregatorBase.NamedResponse> responses) {
        if (responses.size() == 1) {
            log.debug("Only a single response received (" + responses.get(0).getId() + "). No merging performed");
            return responses.get(0).getResponse();
        }
        Set<String> keys = new HashSet<String>();
        for (HubAggregatorBase.NamedResponse responsePair: responses) {
            String id = responsePair.getId();
            QueryResponse response = responsePair.getResponse();
            NamedList raw = response.getResponse();
            for (int i = 0 ; i < raw.size() ; i++) {
                keys.add(raw.getName(i));
            }
        }
        log.debug("Located " + keys.size() + " unique keys in " + responses.size() + " responses. Commencing merging");

        final NamedList<Object> merged = new NamedList<Object>();
        final List<HubAggregatorBase.NamedResponse> defined =
                new ArrayList<HubAggregatorBase.NamedResponse>(responses.size());
        for (String key: keys) {
            // Isolate the responses that contains the given key
            defined.clear();
            for (HubAggregatorBase.NamedResponse response: responses) {
                if (response.getResponse().getResponse().get(key) != null) {
                    defined.add(response);
                }
            }
            if (defined.isEmpty()) {
                log.error("Located 0/" + responses.size() + " responses for key '" + key
                          + "'. This is a program error since at least one response should contain the key");
            } else if (defined.size() == 1) {
                log.debug("Located 1/" + responses.size() + " responses for key '" + key + "'. Storing directly");
                merged.add(key, defined.get(0).getResponse().getResponse().getAll(key));
            } else {
                Object m = merge(params, key, defined);
                if (m != null) {
                    merged.add(key, m);
                }
            }
        }
        // TODO: Assign ID
        // TODO: Locate and extract hit count
        return new QueryResponse(merged, null); // TODO: Check if no Solr server is on
    }

    private Object merge(SolrParams params, String key, List<HubAggregatorBase.NamedResponse> responses) {
        if ("responseHeader".equals(key)) {
            return mergeResponseHeaders(getSimpleOrderedMaps(key, responses));
        }
        if ("response".equals(key)) { // Documents
            return mergeResponses(params, responses);
        }
        log.warn("No merger for key '" + key + "'. Values discarded for " + responses.size() + " responses");
        return null;
    }

    private Object mergeResponses(SolrParams params, List<HubAggregatorBase.NamedResponse> responses) {
        return null;  // TODO: Implement this
    }

    private List<SolrDocumentList> getSolrDocumentList(List<HubAggregatorBase.NamedResponse> responses) {
        List<SolrDocumentList> sdls = new ArrayList<SolrDocumentList>(responses.size());
        for (HubAggregatorBase.NamedResponse response: responses) {
            SolrDocumentList sdl = (SolrDocumentList) response.getResponse().getResponse().get("response");
            if (sdl != null) {
                sdls.add(sdl);
            }
        }
        return sdls;
    }

    private SimpleOrderedMap mergeResponseHeaders(List<SimpleOrderedMap> headers) {
        final Set<String> keys = getKeys(headers);
        final SimpleOrderedMap<Object> merged = new SimpleOrderedMap<Object>();

        for (String key: keys) {
            // status: Anything else than 0 is considered an anomaly and has preference
            if (STATUS.equals(key)) {
                Set<Integer> statuses = new HashSet<Integer>(headers.size());
                for (SimpleOrderedMap som: headers) {
                    Integer status = (Integer) som.get(STATUS);
                    if (status != null) {
                        statuses.add(status);
                    }
                }
                if (statuses.size() == 1) {
                    merged.add(STATUS, statuses.iterator().next());
                } else {
                    log.debug("Got " + statuses.size() + " different responseHeader.status. Returning first != 0");
                    for (Integer status: statuses) {
                        if (0 != status) {
                            merged.add(STATUS, status);
                            break;
                        }
                    }
                }
                continue;
            }

            // QTime: Depending on CONF_PARALLEL_SEARCHES, this is either summed or maxed
            if (QTIME.equals(key)) {
                int endQTime = 0;
                for (SimpleOrderedMap som: headers) {
                    Integer qtime = (Integer) som.get(QTIME);
                    if (qtime != null) {
                        endQTime = parallelSearches ? Math.max(endQTime, qtime) : endQTime + qtime;
                    }
                }
                merged.add(QTIME, endQTime);
                continue;
            }

            // default: Just return the first occurrence TODO: Consider adding all as list
            for (SimpleOrderedMap som: headers) {
                List value = som.getAll(key);
                if (value != null) {
                    merged.add(key, value);
                    break;
                }
            }
        }
        return merged;
    }

    private Set<String> getKeys(List<SimpleOrderedMap> maps) {
        Set<String> keys = new HashSet<String>();
        for (SimpleOrderedMap map: maps) {
            for (int i = 0 ; i < map.size() ; i++) {
                keys.add(map.getName(i));
            }
        }
        return keys;
    }

    private List<SimpleOrderedMap> getSimpleOrderedMaps(String key, List<HubAggregatorBase.NamedResponse> responses) {
        List<SimpleOrderedMap> soms = new ArrayList<SimpleOrderedMap>(responses.size());
        for (HubAggregatorBase.NamedResponse response: responses) {
            SimpleOrderedMap som = (SimpleOrderedMap) response.getResponse().getResponse().get(key);
            if (som != null) {
                soms.add((SimpleOrderedMap) response.getResponse().getResponse().get(key));
            }
        }
        return soms;
    }

    @Override
    public String toString() {
        return "ResponseMerger(not properly implemented)";
    }
}
