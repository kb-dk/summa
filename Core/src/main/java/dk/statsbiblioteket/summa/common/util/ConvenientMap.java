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

import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorageException;
import dk.statsbiblioteket.util.qa.QAInfo;
import net.sf.json.JSONObject;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A wrapper of a Map which provides convenience-methods for accessing specific
 * types stored in the map. The map is forgiving, so storing an integer with
 * put("foo", "87") and retrieving it with getInt("foo") will work.
 * </p><p>
 * For all the convenience methods it holds true that empty underlying String-
 * values are equaled to null.
 * </p><p>
 * As a deviation from normal map-practice, the convenience methods and
 * {@link #get(String)} throws a NullPointerException if no value can be found
 * for the given key.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te",
        comment = "Some methods needs JavaDoc")
public class ConvenientMap extends HashMap<String, Serializable> {
    public static final long serialVersionUID = 384681319L;
    private static Log log = LogFactory.getLog(ConvenientMap.class);

    public ConvenientMap(Serializable... args) {
        super();
        log.trace("Constructing with " + args.length + " arguments");
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "There must be an even number of arguments. Argument count was " + args.length);
        }
        for (int i = 0 ; i < args.length ; i += 2) {
            put((String)args[i], args[i+1]);
        }
    }

    /**
     * Partial JSON-absorber. Adds all readily available JSON key:value-pairs
     * to the map.
     * @param json a list of key:value-pairs.
     */
    public void addJSON(String json) {
        log.trace("Adding JSON-object " + json);
        JSONObject jsonObject = JSONObject.fromObject(json);
        addJSON(jsonObject);
    }

    private void addJSON(JSONObject jsonObject) {
        for (int i = 0 ; i < jsonObject.size() ; i++) {
            for (Object o: jsonObject.entrySet()) {
                // TODO Make better error messages on cast fail
                ListOrderedMap.Entry<String, Serializable> entry = (ListOrderedMap.Entry<String, Serializable>)o;
                if (log.isTraceEnabled()) {
                    log.trace("Putting " + entry.getKey() + ", " + entry.getValue());
                }
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public Serializable get(String key) {
        Serializable value = super.get(key);
        if (value == null) {
            throw new NullPointerException("Unable to locate key '" + key + "'");
        }
        return value;
    }
    /**
     * In order of priority (highest to lowest), return the value for {@code prefix + key}, {@code key},
     * {@code defaultValue} (if present).
     * @param prefix prepended to key as first lookup attempt. If null, this is ignored.
     * @param key the key for the value to return.
     * @param defaultValue the value to return if the key dod not exist and failOnNotFound==true.
     * @param failOnNotFound if true, an exception is thrown for missing values.
     * @return the value for the key.
     * @throws NullPointerException if the key was not present and failOnNotFound was true.
     */
    public Serializable get(String prefix, String key, Serializable defaultValue, boolean failOnNotFound) {
        Serializable value = prefix != null ? super.get(prefix + key) : null;
        if (value == null) {
            value = super.get(key);
        }
        if (value != null) {
            return value;
        }
        if (failOnNotFound) {
            if (prefix == null) {
                throw new NullPointerException("Unable to locate key '" + key + "'");
            } else {
                throw new NullPointerException("Unable to locate key '" + prefix + key + "' or '" + key + "'");
            }
        }
        return defaultValue;
    }

    private String getStringNONPE(String key) {
        Object o = super.get(key);
        return o == null ? null : "".equals(o) ? null : (String)o;
    }

    public String getString(String key) {
        Object o = get(key);
        if (o instanceof String) {
            return (String)o;
        }
        return o.toString();
    }
    public String getString(String key, String defaultValue) {
        String res = getStringNONPE(key);
        return res == null ? defaultValue : res;
    }

    /**
     * In order of priority (highest to lowest), return the value for
     * {@code prefix + key}, {@code key}, {@code defaultValue}.
     * @param prefix prepended to key as first lookup attempt. If null, this is ignored.
     * @param key the key for the value to return.
     * @param defaultValue if neither {@code prefix + key}, nor {@code key} could be located, return this.
     * @return the value for the key.
     */
    public String getString(String prefix, String key, String defaultValue) {
        return (String)get(prefix, key, defaultValue, false);
    }

    /* Integer */
    public Integer getInt(String key) {
        return toInt(key, get(null, key, null, true));
    }
    public Integer getInt(String key, Integer defaultValue) {
        return toInt(key, get(null, key, defaultValue, false));
    }
    public Integer getInt(String prefix, String key, Integer defaultValue) {
        return toInt(key, get(prefix, key, defaultValue, false));
    }
    private Integer toInt(String key, Serializable value) {
        if (value == null || value instanceof Integer) {
            return (Integer)value;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            log.warn(String.format("Exception extracting int for key '%s', value '%s", key, value), e);
            return null;
        }
    }

    /* Long */
    public Long getLong(String key) {
        return toLong(key, get(null, key, null, true));
    }
    public Long getLong(String key, Long defaultValue) {
        return toLong(key, get(null, key, defaultValue, false));
    }
    public Long getLong(String prefix, String key, Long defaultValue) {
        return toLong(key, get(prefix, key, defaultValue, false));
    }
    private Long toLong(String key, Serializable value) {
        if (value == null || value instanceof Long) {
            return (Long)value;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            log.warn(String.format("Exception extracting Long for key '%s', value '%s", key, value), e);
            return null;
        }
    }

    /* Double */
    public Double getDouble(String key) {
        return toDouble(key, get(null, key, null, true));
    }
    public Double getDouble(String key, Double defaultValue) {
        return toDouble(key, get(null, key, defaultValue, false));
    }
    public Double getDouble(String prefix, String key, Double defaultValue) {
        return toDouble(key, get(prefix, key, defaultValue, false));
    }
    private Double toDouble(String key, Serializable value) {
        if (value == null || value instanceof Double) {
            return (Double)value;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            log.warn(String.format("Exception extracting Double for key '%s', value '%s", key, value), e);
            return null;
        }
    }

    /* Float */
    public Float getFloat(String key) {
        return toFloat(key, get(null, key, null, true));
    }
    public Float getFloat(String key, Float defaultValue) {
        return toFloat(key, get(null, key, defaultValue, false));
    }
    public Float getFloat(String prefix, String key, Float defaultValue) {
        return toFloat(key, get(prefix, key, defaultValue, false));
    }
    private Float toFloat(String key, Serializable value) {
        if (value == null || value instanceof Float) {
            return (Float)value;
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            log.warn(String.format("Exception extracting Float for key '%s', value '%s", key, value), e);
            return null;
        }
    }

    /* Short */
    public Short getShort(String key) {
        return toShort(key, get(null, key, null, true));
    }
    public Short getShort(String key, Short defaultValue) {
        return toShort(key, get(null, key, defaultValue, false));
    }
    public Short getShort(String prefix, String key, Short defaultValue) {
        return toShort(key, get(prefix, key, defaultValue, false));
    }
    private Short toShort(String key, Serializable value) {
        if (value == null || value instanceof Short) {
            return (Short)value;
        }
        try {
            return Short.parseShort(value.toString());
        } catch (Exception e) {
            log.warn(String.format("Exception extracting Short for key '%s', value '%s", key, value), e);
            return null;
        }
    }

    /* Boolean */
    public Boolean getBoolean(String key) {
        return toBoolean(key, get(null, key, null, true));
    }
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return toBoolean(key, get(null, key, defaultValue, false));
    }
    public Boolean getBoolean(String prefix, String key, Boolean defaultValue) {
        return toBoolean(key, get(prefix, key, defaultValue, false));
    }
    private Boolean toBoolean(String key, Serializable value) {
        if (value == null || value instanceof Boolean) {
            return (Boolean)value;
        }
        try {
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            log.warn(String.format("Exception extracting Boolean for key '%s', value '%s", key, value), e);
            return null;
        }
    }

    /**
     * Properly retrieves lists of Strings stored as List<String>, String[] or
     * as a comma-separated list.
     * @param key the name of the property to look up.
     * @return value as a list of Strings.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException if the property is found but does not
     *         parse as a list of Strings
     */
    public List<String> getStrings(String key) {
        Object val = get(key);
        if (val instanceof List) {
            ArrayList<String> result = new ArrayList<>(((List)val).size());
            for (Object o: (List)val) {
                result.add(o.toString());
            }
            return result;
        }
        if (val instanceof String[]) {
            return Arrays.asList((String[]) val);
        }
        // Comma-separated
        String[] unescaped = getString(key).split(" *, *");
        ArrayList<String> result = new ArrayList<>(unescaped.length);
        for (String s: unescaped) {
            //noinspection DuplicateStringLiteralInspection
            result.add(s.replaceAll("&comma;", ",").replaceAll("&amp;", "&"));
        }
        return result;
    }

    public List<String> getStrings(String key, List<String> defaultValues) {
        try {
            return getStrings(key);
        } catch (NullPointerException e) {
            return defaultValues;
        } catch (IllegalArgumentException e) {
            log.warn(String.format(
                    "The property %s was expected to be a list of Strings, but it was not. Using default %s instead",
                    key, defaultValues));
            return defaultValues;
        }
    }

    /**
     * Wrapper for the list-baset {@link #getStrings(String, List)} method.
     * @param key the name of the property to look up.
     * @param defaultValues the values to return if there is no list of Strings
     *                      specified for the given key.
     * @return value as an array of Strings.
     */
    public String[] getStrings(String key, String[] defaultValues) {
        List<String> result = getStrings(key, defaultValues == null ? null : Arrays.asList(defaultValues));
        return result == null ? null : result.toArray(new String[result.size()]);
    }

    public static class Pair<T, U> {
        private T t;
        private U u;
        public Pair(T firstValue, U secondValue) {
            t = firstValue;
            u = secondValue;
        }
        public T getFirst() {
            return t;
        }
        public U getSecond() {
            return u;
        }
    }

    protected Pattern numberPattern = Pattern.compile("(.+)\\( *(\\-?[0-9]+) *\\).*");
    /**
     * Parses the value for the key for Strings and Integers and returns them
     * as a list of Pairs.
     * </p><p>
     * Sample value: a(1),b (2), c(4), d, e(16)
     * @param key          the name of the property to look up.
     * @param defaultValue the Integer that should be used when one of the
     *                     sub-values isn't specified.
     *                     Example: The value in the property is as specified
     *                     above and the defaultValue is 7. This produces
     *                     (a, 1), (b, 2), (c, 4), (d, 7), (e, 16)
     * @return a list of pairs of Strings and Integers.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException if the property is found but does not
     *         parse as a list of Strings
     * @throws ConfigurationStorageException if there is an error communicating
     *         with the storage backend
     */
    @SuppressWarnings({"unchecked"})
    public List<Pair<String, Integer>> getIntValues(String key, Integer defaultValue) {
        Object o = get(key);
        try {
            //noinspection unchecked
            return (List<Pair<String, Integer>>)o;
        } catch (ClassCastException e) {
            // Acceptable behaviour as we switch to parsing
            log.trace("intValues not stored directly for key '" + key + "'");
        }
        List<String> elements = getStrings(key);
        List<Pair<String, Integer>> result = new ArrayList<>(elements.size());
        for (String element: elements) {
            Matcher numberMatcher = numberPattern.matcher(element);
            if (numberMatcher.matches()) {
                result.add(new Pair<>(numberMatcher.group(1), Integer.parseInt(numberMatcher.group(2))));
            } else {
                result.add(new Pair<>(element.trim(), defaultValue));
            }
        }
        return result;
    }

    /**
     * @param verbose if true a full dump of the content of the map is returned.
     * @return a human-readable description of the map.
     */
    public String toString(boolean verbose) {
        if (!verbose) {
            return super.toString();
        }
        StringWriter sw = new StringWriter(1000);
        sw.append("ConvenientMap(");
        boolean later = false;
        for (Map.Entry<String, Serializable> entries: entrySet()) {
            if (later) {
                sw.append(", ");
            }
            later = true;
            sw.append(entries.getKey()).append("=");
            sw.append(entries.getValue().toString());
        }
        sw.append(")");
        return sw.toString();
    }

    // JSON-compliant
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (Map.Entry<String, Serializable> entry: entrySet()) {
            if (sb.length() != 1) {
                sb.append(", ");
            }
            String key = entry.getKey();
            Serializable value = entry.getValue();
            sb.append(key).append(":\"");
            // See https://www.ietf.org/rfc/rfc4627.txt about escaping
            sb.append(value.toString().replace("\\", "\\\\").replace("\"", "\\\""));
            sb.append("\"");
        }

        return sb.append('}').toString();
    }
}
