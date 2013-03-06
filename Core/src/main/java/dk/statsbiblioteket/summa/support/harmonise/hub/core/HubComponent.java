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
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.common.params.SolrParams;

import java.util.List;

/**
 * A HubCompoennt represents one or more searchers that communicate both requests and responses using Solr's NamedList.
 * </p><p>
 * Core concepts for a SearchNode are id, which represent the specific node and bases, which represents groupings of
 * material that the SearchNode supports.
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
     * The mode determines whether the SearchNode should be used for a request to a given base.<br/>
     * always:    Use the SearchNode no matter what the base is.<br/>
     * onlyMatch: Use the SearchNode only if it {@link #getBases} contains the base that is wanted.<br/>
     * fallback:  Use the SearchNode if no other SearchNodes matches the request.
     */
    public enum MODE {always, onlyMatch, fallback}

    /**
     * IDs need not be unique, but if an ID is shared between SearchNodes, it is assumed that they have the same role
     * and are functionally duplicates of each other.
     * @return the machine-usable id for this specific SearchNode.
     */
    String getID();

    /**
     * @return a list of all the bases that the SearchNode supports or the empty list if no such list can be made.
     * @see
     */
    List<String> getBases();

    /**
     * @return the mode the the specific SearchNode.
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
    SolrResponse search(Limit limit, SolrParams params) throws Exception;

    public static class Limit {
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

        public List<String> getIds() {
            return ids;
        }

        public List<String> getBases() {
            return bases;
        }
    }

}
