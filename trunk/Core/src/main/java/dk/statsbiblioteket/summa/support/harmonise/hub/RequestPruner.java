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
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAdjusterBase;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The RequestPruner checks all request parameters against a whitelist and a blacklist. The order is allowNone,
 * whitelist, blacklist. It is recommended to create a detailed whitelist and no blacklist as this ensures that
 * setup errors or omissions will result in the parameter not being accepted.
 */
@QAInfo(level = QAInfo.Level.PEDANTIC,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RequestPruner extends HubAdjusterBase {
    private static Log log = LogFactory.getLog(RequestPruner.class);

    // TODO: Add option for returning a list of disabled options

    /**
     * A list of regexps. All incoming parameters must match at least one entry from this list to be passed on.
     * </p><p>
     * All arguments are tested one at a time and passed as {@code key=value} to the regexp-list.
     * Arguments with 1 key and x values are recombined to x key-value pairs.
     * </p><p>
     * Optional but highly recommended. If not specified, no parameters are allowed.
     */
    public static final String CONF_WHITELIST = "pruner.whitelist";

    /**
     * A list of regexps. If an incoming parameter matches an entry from this list, it is not passed on.
     * </p><p>
     * All arguments are tested one at a time and passed as {@code key=value} to the regexp-list.
     * Arguments with 1 key and x values are recombined to x key-value pairs.
     * </p><p>
     * Optional. If not specified, all parameters matching an entry from the whitelist is passed on.
     */
    public static final String CONF_BLACKLIST = "pruner.blacklist";

    private final List<String> whitelist;
    private final List<String> blacklist;

    private final List<Pattern> whitelistPatterns;
    private final List<Pattern> blacklistPatterns;

    public RequestPruner(Configuration conf) {
        super(conf);
        whitelist = conf.getStrings(CONF_WHITELIST, (List<String>)null);
        whitelistPatterns = toPatterns(CONF_WHITELIST, whitelist);
        blacklist = conf.getStrings(CONF_BLACKLIST, (List<String>) null);
        blacklistPatterns = toPatterns(CONF_BLACKLIST, blacklist);

        log.info("Created " + this);
    }

    private List<Pattern> toPatterns(String confKey, List<String> regexps) {
        if (regexps == null) {
            log.debug("No regexps for " + confKey);
            return null;
        }
        List<Pattern> patterns = new ArrayList<Pattern>(regexps.size());
        for (String regexp: regexps) {
            try {
                patterns.add(Pattern.compile(regexp));
            } catch (PatternSyntaxException e) {
                throw new ConfigurationException("Unable to compile " + confKey + " regexp '" + regexp + "'", e);
            }
        }
        log.debug("Successfully compiled " + patterns.size() + " " + confKey + " regexps");
        return patterns;
    }

    @Override
    public SolrParams adjustRequest(SolrParams request) {
        checkSubComponents();
        ModifiableSolrParams pruned = new ModifiableSolrParams();
        if (whitelist == null) {
            log.debug("adjustRequest: No whitelist regexps. All parameters are discarded");
            return pruned;
        }
        int inputCount = 0;
        int prunedCount = 0;
        Iterator<String> keys = request.getParameterNamesIterator();
        while (keys.hasNext()) {
            String key = keys.next();
            String[] values = request.getParams(key);
            valueLoop:
            for (String value: values) {
                final String pair = key + "=" + value;
                inputCount++;

                // Whitelist check
                boolean matches = false;
                for (Pattern white: whitelistPatterns) {
                    if (white.matcher(pair).matches()) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) {
                    log.debug("Discarding parameter pair '" + pair + "' as it did not match any whitelist regexp");
                    continue;
                }

                if (blacklist != null) {
                    for (Pattern black: blacklistPatterns) {
                        if (black.matcher(pair).matches()) {
                            log.debug("Discarding parameter pair '" + pair + "' as it did not match blacklist regexp '"
                                      + black.pattern() + "'");
                            continue valueLoop;
                        }
                    }
                }

                prunedCount++;
                pruned.add(key, value);
            }
        }
        log.debug("Pruning retained " + prunedCount + "/" + inputCount + " parameter pairs");
        return pruned;
    }

    private void checkSubComponents() {
        if (getComponents().size() != 1) {
            throw new IllegalStateException(
                    "The RequestPruner must have exactly 1 sub component but had " + getComponents().size());
        }
    }

    // We do not want bypassing of a white lister
    @Override
    protected boolean adjustmentDisablingPossible() {
        return false;
    }

    @Override
    public QueryResponse adjustResponse(SolrParams request, QueryResponse response) {
        return response;
    }

    @Override
    public QueryResponse barrierSearch(Limit limit, ModifiableSolrParams params) throws Exception {
        return getComponents().get(0).search(limit, params);
    }

    @Override
    public String toString() {
        return "RequestPruner(whitelist=[" + (whitelist == null ? "" : "\"" + Strings.join(whitelist, "\", \"") + "\"")
               + "], blacklist=" + (blacklist == null ? "" : "\"" + Strings.join(blacklist, "\", \"") + "\"") + "])";
    }
}
