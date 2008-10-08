/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorageException;

/**
 * A wrapper of a Map which provides convenience-methods for accessing specific
 * types stored in the map. The map is forgiving, so storing an integer with
 * put("foo", "87") and retrieving it with getInt("foo") will work.
 * </p><p>
 * For all the convenience methods it holds true that empty underlying String-
 * values are equaled to null.
 * </p><p>
 * As a deviation of normal map-practice, the convenience methods and
 * {@link #get(String)} throws a NullPointerException if no value can be found
 * for the given key.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Some methods needs JavaDoc")
public class ConvenientMap extends HashMap<String, Serializable> {
    private static Log log = LogFactory.getLog(ConvenientMap.class);

    public Serializable get(String key) {
        Serializable value = super.get(key);
        if (value == null) {
            throw new NullPointerException("Unable to locate key '" + key
                                           + "'");
        }
        return value;
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

    public Integer getInt(String key) {
        Object o = get(key);
        //noinspection OverlyBroadCatchBlock
        try {
            if (o instanceof Integer) {
                return (Integer)o;
            }
            /* Converting to a String and parsing that is a catch-all method
             * for cases where the value is an Long or Character */
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            log.warn(String.format(
                    "Exception extracting int for key '%s'", key), e);
            return null;
        }
    }
    public Integer getInt(String key, Integer defaultValue) {
        try {
            return getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Long getLong(String key) {
        Object o = get(key);
        //noinspection OverlyBroadCatchBlock
        try {
            if (o instanceof Long) {
                return (Long)o;
            }
            /* Converting to a String and parsing that is a catch-all method
             * for cases where the value is an Integer or Character */
            return Long.parseLong(o.toString());
        } catch (Exception e) {
            log.warn(String.format(
                    "Exception extracting long for key '%s'", key), e);
            return null;
        }
    }
    public Long getLong(String key, Long defaultValue) {
        try {
            return getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Boolean getBoolean(String key) {
        Object o = get(key);
        try {
            if (o instanceof Boolean) {
                return (Boolean)o;
            }
            /* Converting to a String and parsing that is a catch-all method
             * for cases where the value is an Integer or Character */
            return Boolean.parseBoolean(o.toString());
        } catch (Exception e) {
            log.warn(String.format(
                    "Exception extracting boolean for key '%s'", key), e);
            return null;
        }
    }
    public Boolean getBoolean(String key, Boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    /**
     * Properly reteieves lists of Strings stored as List<String>, String[] or
     * as a comma-separated list.
     * @param key the name of the property to look up.
     * @return value as a list of Strings.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException if the property is found but does not
     *         parse as a list of Strings
     * @see {link #setStrings}.
     */
    public List<String> getStrings(String key) {
        Object val = get(key);
        if (val instanceof List) {
            ArrayList<String> result =
                    new ArrayList<String>(((List)val).size());
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
        ArrayList<String> result = new ArrayList<String>(unescaped.length);
        for (String s: unescaped) {
            result.add(s.replaceAll("&comma;", ",").
                         replaceAll("&amp;", "&"));
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
                    "The property %s was expected to be a list of Strings, but "
                    + "it was not. Using default %s instead",
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
        List<String> result = getStrings(key, defaultValues == null ? null :
                                              Arrays.asList(defaultValues));
        return result == null ? null :
               result.toArray(new String[result.size()]);
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

    protected Pattern numberPattern =
            Pattern.compile("(.+)\\( *(\\-?[0-9]+) *\\).*");
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
    public List<Pair<String, Integer>> getIntValues(String key,
                                                    Integer defaultValue) {
        Object o = get(key);
        try {
            //noinspection unchecked
            return (List<Pair<String, Integer>>)o;
        } catch (ClassCastException e) {
            // Acceptable behaviour as we switch to parsing
            log.trace("intValues not stored directly for key '" + key + "'");
        }
        List<String> elements = getStrings(key);
        List<Pair<String, Integer>> result =
                new ArrayList<Pair<String, Integer>>(elements.size());
        for (String element: elements) {
            Matcher numberMatcher = numberPattern.matcher(element);
            if (numberMatcher.matches()) {
                result.add(new Pair<String, Integer>(numberMatcher.group(1),
                                     Integer.parseInt(numberMatcher.group(2))));
            } else {
                result.add(new Pair<String, Integer>(element.trim(),
                                                     defaultValue));
            }
        }
        return result;
    }

}



