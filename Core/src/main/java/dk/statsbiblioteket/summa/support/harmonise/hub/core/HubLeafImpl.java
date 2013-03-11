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
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;

import java.util.ArrayList;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class HubLeafImpl extends  HubComponentImpl {
    private static Log log = LogFactory.getLog(HubLeafImpl.class);

    /**
     * The bases for this node as a list of Strings. The list is dynamic so changes can be made after construction.
     * </p><p>
     * Optional. Default is the empty list.
     */
    public static final String CONF_BASES = "node.bases";

    /**
     * The mode for the node. This can be changed after construction.
     * </p><p>
     * Optional. Default is {@link dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponent.MODE#onlyMatch}.
     */
    public static final String CONF_MODE = "node.mode";
    public static final String DEFAULT_MODE = MODE.onlyMatch.toString();

    private final List<String> bases = new ArrayList<String>();
    protected MODE mode;

    public HubLeafImpl(Configuration conf) {
        super(conf);
        if (conf.containsKey(CONF_BASES)) {
            bases.addAll(conf.getStrings(CONF_BASES));
        }
        mode = MODE.valueOf(conf.getString(CONF_MODE, DEFAULT_MODE));
        log.info("Created " + toString());
    }

    @Override
    public QueryResponse barrierSearch(Limit limit, SolrParams params) throws Exception {
        return limitOK(limit) ? search(params) : null;
    }

    /**
     * @param params HubComponent-specific parameters.
     * @return the result from a search.
     * @throws Exception if an error occurred.
     */
    public abstract QueryResponse search(SolrParams params) throws Exception;

    @Override
    public boolean limitOK(Limit limit) {
        if (limit == null) {
            return true;
        }
        if (getMode() == MODE.always || getMode() == MODE.fallback) {
            return true;
        }
        if (limit.getIds() != null && !limit.getIds().isEmpty() && limit.getIds().contains(getID())) {
            return false;
        }
        if (limit.getBases() != null && getBases() != null && !getBases().isEmpty()) {
            for (String base: getBases()) {
                if (limit.getBases().contains(base)) {
                    return true;

                }
            }
            return false;
        }
        log.debug("No match for request");
        return false;
    }

    @Override
    public List<String> getBases() {
        return bases;
    }

    @Override
    public MODE getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return "HubLeafImpl(id='" + getID() + '\'' + ", bases=["
                + (getBases() == null ? "" : Strings.join(getBases())) + "], mode=" + getMode() + ')';
    }
}
