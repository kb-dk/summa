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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.*;

/**
 * Maps multiple Strings to multiple Strings, allowing for reverse lookup.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ManyToManyMapper {
    private final Map<String, Set<String>> forward;
    private final Map<String, Set<String>> reverse;

    /**
     * Constructs a many-to-many map with the given rules. Source-destination
     * is delimited with " - " and values with semicolon.
     * </p><p>
     * Example: "a - b", "c;d - e", "f - g;h", "i;j - k;l"
     *          demonstrates 1:1, 2:1, 1:2 and 2:2 mapping.
     * @param rules textual representation of mappings.
     */
    public ManyToManyMapper(List<String> rules) {
        super();
        forward = new HashMap<String, Set<String>>(rules.size() * 2);
        reverse = new HashMap<String, Set<String>>(rules.size() * 2);
        addRules(rules);
    }

    public Map<String, Set<String>> getForward() {
        return forward;
    }

    public Map<String, Set<String>> getReverse() {
        return reverse;
    }

    private void addRules(List<String> rules) {
        for (String rule: rules) {
            String[] parts = rule.split(" * -  *"); // We demand spaces
            if (parts.length != 2) {
                throw new Configurable.ConfigurationException(
                    "Expected two parts by splitting '" + rule
                    + "' with delimiter '-' but got " + parts.length);
            }
            String[] sources = parts[0].split(" *; *");
            String[] destinations = parts[1].split(" *; *");
            putHelper(forward, sources, destinations);
            putHelper(reverse, destinations, sources);
        }
    }

    // TODO: Use a set to guard against duplicate destinations
    @SuppressWarnings({"IfMayBeConditional"})
    private void putHelper(
        Map<String, Set<String>> map, String[] sources, String[] destinations) {
        for (String source: sources) {
            Set<String> values;
            if (map.containsKey(source)) {
                values = map.get(source);
            } else {
                values = new HashSet<String>(10);
                map.put(source, values);
            }
            Collections.addAll(values, destinations);
        }
    }
}
