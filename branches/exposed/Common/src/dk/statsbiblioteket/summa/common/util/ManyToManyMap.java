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
public class ManyToManyMap extends HashMap<String, String[]> {
    private final Map<String, String[]> reverse;

    /**
     * Constructs a many-to-many map with the given rules. Rules are delimited
     * with comma, source-destination with " - " and values with semicolon.
     * </p><p>
     * Example: "a - b, c;d - e, f - g;h, i;j - k;l"
     *          demonstrates 1:1, 2:1, 1:2 and 2:2 mapping.
     * @param rules textual representation of mappings.
     */
    public ManyToManyMap(String rules) {
        super();
        reverse = new HashMap<String, String[]>();
        addRules(rules);
    }

    /**
     * Constructs a many-to-many map with the given rules. Source-destination
     * is delimited with " - " and values with semicolon.
     * </p><p>
     * Example: "a - b", "c;d - e", "f - g;h", "i;j - k;l"
     *          demonstrates 1:1, 2:1, 1:2 and 2:2 mapping.
     * @param rules textual representation of mappings.
     */
    public ManyToManyMap(List<String> rules) {
        super();
        reverse = new HashMap<String, String[]>();
        addRules(rules);
    }

    /**
     * @return a wrapper that presents a reversed view of this map. Changes to
     * the returned map is reflected in this.
     */
    public HashMap<String, String[]> reverse() {
        return new HashMap<String, String[]>() {
            @Override
            public int size() {
                return ManyToManyMap.this.reverseSize();
            }

            @Override
            public boolean isEmpty() {
                return ManyToManyMap.this.isEmpty();
            }

            @Override
            public String[] get(Object key) {
                return ManyToManyMap.this.reverseGet(key);
            }

            @Override
            public boolean containsKey(Object key) {
                return ManyToManyMap.this.reverseContainsKey(key);
            }

            @Override
            public String[] put(String key, String[] value) {
                String[] keys = new String[]{key};
                String[] returned = null;
                for (String v: value) {
                    returned = ManyToManyMap.this.put(v, keys);
                }
                return returned;
            }

            @Override
            public String[] remove(Object key) {
                throw new UnsupportedOperationException("remove not allowed");
            }

            @Override
            public void putAll(Map<? extends String, ? extends String[]> m) {
                for (Map.Entry<? extends String, ? extends String[]> e:
                    m.entrySet()) {
                    put(e.getKey(), e.getValue());
                }
            }

            @Override
            public void clear() {
                ManyToManyMap.this.clear();
            }

            // TODO: Make the rest of the operations work of throw exceptions
        };
    }

    /**
     * Adds the given rules to the existing ones. Rules are delimited
     * with comma, source-destination with " - " and values with semicolon.
     * </p><p>
     * Example: "a - b, c;d - e, f - g;h, i;j - k;l"
     *          demonstrates 1:1, 2:1, 1:2 and 2:2 mapping.
     * @param rules textual representation of mappings.
     */
    private void addRules(String rules) {
        String[] tokens = rules.split(" *, *");
        addRules(Arrays.asList(tokens));
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
            putHelper(this, sources, destinations);
            putHelper(reverse, destinations, sources);
        }
    }


    public String[] reverseGet(Object key) {
        //noinspection SuspiciousMethodCalls
        return reverse.get(key);
    }

    public int reverseSize() {
        return reverse.size();
    }

    public boolean reverseContainsKey(Object key) {
        //noinspection SuspiciousMethodCalls
        return reverse.containsKey(key);
    }

    @Override
    public String[] put(String source, String[] destinations) {
        final String[] old = get(source);
        final String[] sources = new String[]{source};
        putHelper(this, sources, destinations);
        putHelper(reverse, destinations, sources);
        return old;
    }

    /**
     * Adds the mapping from the given sources to the given destinations.
     * @param sources      map from these.
     * @param destinations map to these.
     */
    public void put(String[] sources, String[] destinations) {
        putHelper(this, sources, destinations);
        putHelper(reverse, destinations, sources);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String[]> m) {
        for (Map.Entry<? extends String, ? extends String[]> entry:
            m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String[] remove(Object key) {
        throw new IllegalArgumentException(
            "Due to unclear semantics, remove is not allowed. Use clear() "
            + "instead and rebuild");
    }

    @Override
    public void clear() {
        super.clear();
        reverse.clear();
    }

    // TODO: Use a set to guard against duplicate destinations
    private void putHelper(
        Map<String, String[]> map, String[] sources, String[] destinations) {
        for (String source: sources) {
            if (map.containsKey(source)) {
                final String[] values =
                    new String[map.get(source).length + destinations.length];
                System.arraycopy(map.get(source), 0, values,
                                 0, map.get(source).length);
                System.arraycopy(destinations, 0, values,
                                 map.get(source).length, destinations.length);
                destinations = values;
            }
            if (map == this) {
                super.put(source, destinations);
            } else {
                map.put(source, destinations);
            }
        }
    }
}
