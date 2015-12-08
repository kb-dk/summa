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

import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A HubCompoennt represents one or more searchers that communicate both requests and responses using Solr's NamedList.
 * </p><p>
 * Core concepts for a HubComponent are id, which represent the specific node and bases, which represents groupings of
 * material that the HubComponent supports.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface HubComponent {

    // First class:
    // Bases
    // IDs (sites)
    // Response format


    /**
     * The mode determines whether the HubComponent should be used for a request to a given base.<br/>
     * always:    Use the HubComponent no matter what the base is.<br/>
     * onlyMatch: Use the HubComponent only if it {@link #getBases} contains the base that is wanted.<br/>
     * fallback:  Use the HubComponent if no other HubComponents matches the request.
     */
    public enum MODE {always, onlyMatch, fallback}

    /**
     * IDs need not be unique, but if an ID is shared between HubComponents, it is assumed that they have the same role
     * and are functionally duplicates of each other.
     * @return the machine-usable id for this specific HubComponent.
     */
    String getID();

    /**
     * @return a list of all the bases that the HubComponent supports or the empty list if no such list can be made.
     * @see
     */
    List<String> getBases();

    /**
     * @return the mode the the specific HubComponent.
     */
    MODE getMode();

    /**
     * @param limit a limit for a search to be performed.
     * @return true if the component conforms to the given limit.
     */
    boolean limitOK(Limit limit);

    /**
     * @param limit optional limitation on the HubComponents and bases that are to be searched.
     * @param params HubComponent-specific parameters.
     * @return the result from a search or null if the request is not applicable.
     * @throws Exception if an error occurred.
     */
    QueryResponse search(Limit limit, SolrParams params) throws Exception;

    public static class Limit {
        /**
         * Comma-separated list of IDs for HubComponents to be searched.
         */
        public static final String CONF_IDS = "limit.ids";
        /**
         * Comma-separated list of bases to be searched.
         */
        public static final String CONF_BASES = "limit.bases";

        private static final Pattern SPLITTER = Pattern.compile(" *, *");

        private final List<String> ids;
        private final List<String> bases;

        /**
         * @param ids the IDs for the HubLeafs that should handle the request or null if leaf IDs are not limiting.
         * @param bases the bases that should be searched or null if bases are not limiting.
         */
        public Limit(List<String> ids, List<String> bases) {
            this.ids = ids;
            this.bases = bases;
        }

        public Limit(NamedList<Object> limitParams) {
            ids = getStrings(limitParams, CONF_IDS);
            bases = getStrings(limitParams, CONF_BASES);
        }

        private List<String> getStrings(NamedList<Object> limitParams, String key) {
            List<Object> values = limitParams.getAll(key);
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> results = new ArrayList<>();
            for (Object o: values) {
                if (!(o instanceof String)) {
                    throw new IllegalArgumentException(
                            "Limit only accepts String for " + key + " but got " + o.getClass());
                }
                String[] parts = SPLITTER.split((String)o);
                Collections.addAll(results, parts);
            }
            return results;
        }

        public List<String> getIds() {
            return ids;
        }

        public List<String> getBases() {
            return bases;
        }

        /**
         * @param limitParams setup for thhe Limit.
         * @return a limit base don the limitParams or null if the params had no effect on limit.
         */
        public static Limit createLimit(NamedList<Object> limitParams) {
            Limit limit = new Limit(limitParams);
            return limit.getIds().isEmpty() && limit.getBases().isEmpty() ? null : limit;
        }

        public boolean isEmpty() {
            return (getIds() == null || getIds().isEmpty()) && (getBases() == null || getBases().isEmpty());
        }

        public String toString() {
            return "Limit(ids=[" + Strings.join(ids) + "], bases=[" + Strings.join(bases) + "])";
        }
    }
}
