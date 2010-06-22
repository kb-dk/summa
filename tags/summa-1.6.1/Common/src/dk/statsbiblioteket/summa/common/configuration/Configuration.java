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
package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.JStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.RemoteStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.util.Environment;
import dk.statsbiblioteket.summa.common.util.Security;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Class needs Javadoc")
// TODO: Use ConvenientMap for convenience
public class Configuration implements Serializable,
                                      Iterable<Map.Entry<String,Serializable>>,
                                      Configurable {
    private final static Log log = LogFactory.getLog(Configuration.class);

    /**
     * Fail-fast check for Java version.
     */
    //private static transient String version = Environment.checkJavaVersion();

    private ConfigurationStorage storage;

    /**
     * Set of system resource names used to look for a configuration if none
     * can be found under {@link #CONF_CONFIGURATION_PROPERTY}.
     */
    public static final String[] DEFAULT_RESOURCES = {
                                              "configuration.xml",
                                              "configuration.js",
                                              "config.xml",
                                              "config.js",
                                              "properties.xml",
                                              "properties.js",
                                              "configuration.properties",
                                              "config.properties",
                                              "config/configuration.xml",
                                              "config/configuration.js",
                                              "config/config.xml",
                                              "config/config.js",
                                              "config/properties.xml",
                                              "config/properties.js",
                                              "config/configuration.properties",
                                              "config/config.properties"
                                                     };

    /** System property defining where to fetch the configuration.
     * This can be a normal URL or an rmi path.*/
    public static final String CONF_CONFIGURATION_PROPERTY =
                                                          "summa.configuration";

    /** Optional system property defining which version of the Summa API
     * applications should adhere to if it can not be determined at compile
     * time.
     * Default value is {@link this#DEFAULT_API_VERSION */
     public static final String CONF_API_VERSION = "summa.api.version";

    /** API version of the Summa release. */
     public static final String DEFAULT_API_VERSION =
                      "@summa.api.version@"; // Auto-expanded in compile target

    /** System property defining the root directory from which persistent
     * data should be read and stored to. The default value is
     * {@code $HOME/summa-control/persistent. */
    public static final String CONF_PERSISTENT_DIR = "summa.persistent";

    /**
     * Create a {@code Configuraion} with the given {@link ConfigurationStorage}
     * backend.
     *
     * @param storage The storage backend to use.
     */
    public Configuration (ConfigurationStorage storage) {
        this.storage = storage;
    }

    /**
     * Create a {@code Configuration} using the same
     * {@link ConfigurationStorage} as a given other {@code Configuration}.
     *
     * @param conf The {@code Configuration} to share storage with.
     */
    public Configuration (Configuration conf) {
        storage = conf.getStorage();
    }

    /**
     * <p>Create a new memory based configuration with a given set of
     * key-value pairs.</p>
     * <p>This method is primarily intended to create simple
     * configurations on the fly.</p>
     * <p>If any value-argument is a {@link List} instance it will
     * be assumed to be a {@code List<String>} and added to the
     * configuration via {@link Configuration#setStrings}, and should be
     * retrieved with {@link Configuration#getStrings}</p>
     * <p>Likewise any {@code String[]} value argument will also be set via
     * {@code Configuration.setStrings}.
     *
     * @param args <p>The arguments should be {@code String, Serializable} pairs
     *        if the argument count is odd or each even numbered argument
     *        is not a {@code String} an {@code IllegalArgumentException}
     *        is thrown.</p>
     * @return A {@code Configuration} mapping the key value pairs provided
     *         in the argument list.
     */
    @SuppressWarnings("unchecked")   // to cast arg to List<String>
    public static Configuration newMemoryBased (Serializable... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of arguments. "
                                             + "Arguments should be key-value "
                                             + "pairs");
        }

        Configuration conf = new Configuration (new MemoryStorage());

        for (int i = 0; i < args.length; i = i + 2) {
            if (!(args[i] instanceof String)) {
                throw new IllegalArgumentException("Every even number arg must"
                                                 + " be a String");
            }
            if (args[i+1] instanceof List) {
                conf.setStrings(args[i].toString(), (List<String>)args[i+1]);
            } else if (args[i+1] instanceof String[]) {
                conf.setStrings(args[i].toString(),
                                Arrays.asList((String[])args[i+1]));
            } else {
                conf.set(args[i].toString(), args[i+1]);
            }

        }
        return conf;
    }

    /**
     * Store a key-value pair in the {@link ConfigurationStorage}.
     *
     * @param key Name of property to store.
     * @param value The value to associate with {@code key}.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     */
    public void set (String key, Serializable value) {
        try {
            storage.put(key, value);
        } catch (IOException e) {
            throw new ConfigurationStorageException("Unable to set property '"
                                                    + key + "=" + value + "'",
                                                    e);
        }
    }

    /**
     * Store a list of Strings in the properties. The strings are converted to
     * an internal format, so they should be retrieved with getStrings instead
     * of just get.
     *
     * @param key     Name of the property to store.
     * @param strings The Strings to store.
     * @throws ConfigurationStorageException if there was an error communicating
     *                                       with the storage backend.
     */
    public void setStrings(String key, List<String> strings) {
        StringWriter sw = new StringWriter(strings.size() * 100);
        for (int i = 0 ; i < strings.size() ; i++) {
            String current = strings.get(i);
            current = current.replaceAll("&", "&amp;"); 
            current = current.replaceAll(",", "&comma;");
            sw.append(current);
            if (i < strings.size()-1) {
                sw.append(", ");
            }
        }
        try {
            storage.put(key, sw.toString());
        } catch (IOException e) {
            throw new ConfigurationStorageException("Unable to set property \""
                                                    + key + "=" + sw.toString()
                                                    + "\"", e);
        }
    }

    /**
     * Get a stored value by name.
     *
     * @param key The name of the value to look up.
     * @return The value associated with {@code key}.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     */
    public Serializable get (String key) {
        try {
            return storage.get (key);
        } catch (IOException e) {
            throw new ConfigurationStorageException("Unable to get property '"
                                                    + key + "'", e);
        }
    }

    /**
     * Remove a property from the configuration.
     *
     * @param key the name of the property to remove.
     */
    public void purge(String key) {
        try {
            storage.purge (key);
        } catch (IOException e) {
            throw new ConfigurationStorageException("Unable to purge property '"
                                                    + key + "'", e);
        }
    }

    /**
     * Look up a stored {@code String} value. Any Ant-style references to system
     * properties, like<br/>
     * <pre>
     *   ${user.home}
     * </pre>
     * will be expanded in the returned string. This expansion will not affect
     * the string value stored in the configuration. It is not an error if
     * the string contains a reference to an unknown property.
     *
     * @param key The name of the value to look up.
     * @return The {@code String} representation of the value associated with
     *         {@code key}. If the value does not exist, an exception is thrown.
     *         The returned string will not containing leading or trailing white
     *         space. As described above any references to system properties
     *         enclosed in <code>${prop.name}</code> will be escaped.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     * @throws NullPointerException          if there was no value corresponding
     *                                       to the key.
     */
    public String getString (String key) {
        Object val = get(key);
        if (val == null) {
            throw new NullPointerException("No such property: " + key);
        }
        return Environment.escapeSystemProperties(val.toString().trim());
    }

    /**
     * Look up a stored {@code String} value with an optional fallback value.
     * Any Ant-style references to system properties, like<br/>
     * <pre>
     *   ${user.home}
     * </pre>
     * will be expanded in the returned string. This expansion will not affect
     * the string value stored in the configuration. It is not an error if
     * the string contains a reference to an unknown property.
     * <p/>
     * System properties referenced in the fallback value will also be expanded
     *
     * @param key          The name of the value to look up.
     * @param defaultValue Ff the value does not exist, return this instead.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     * @return the {@code String} representation of the value associated with
     *         {@code key} or the defaultValue, if the key did not exist.
     *         The returned string will not containing leading or trailing white
     *         space. As described above any references to system properties
     *         enclosed in <code>${prop.name}</code> will be escaped. System
     *         property references in the default value will also be expanded.
     */
    public String getString(String key, String defaultValue) {
        Object val = get(key);
        if (val == null) {
            log.debug("Unable to find property '" + key + "', using default "
                      + defaultValue);
            return Environment.escapeSystemProperties(defaultValue);
        }
        return Environment.escapeSystemProperties(val.toString().trim());
    }

    /**
     * Look up an integer property.
     * @param key The name of the property to look up.
     * @return Value as an integer.
     * @throws NullPointerException if the property is not found
     * @throws IllegalArgumentException      if the property is found but does
     *                                       not parse as an integer.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     */
    public int getInt (String key) {
        Object val = get(key);
        if (val == null) {
            throw new NullPointerException ("No such property: " + key);
        }

        String sval = Environment.escapeSystemProperties(val.toString()).trim();
        try {
            return Integer.parseInt(sval);
        } catch (NumberFormatException e) {
            try {
                return (int) Double.parseDouble(sval);
            } catch (NumberFormatException ee) {
            throw new IllegalArgumentException("Bad number format for '" + key
                                               + "': " + e.getMessage());
            }
        }
    }

    /**
     * Look up an integer property. If it is not defined or does not parse
     * as an integer, return {@code defaultValue}.
     *
     * @param key          The name of the property to look up.
     * @param defaultValue The value to return if the value for the key could
     *                     not be extracted.
     * @return The value for key as an int.
     */
    public int getInt (String key, int defaultValue) {
        try {
            return getInt(key);
        } catch (NullPointerException e) {
            log.debug("Unable to find property '" + key + "', using default "
                      + defaultValue);
            return defaultValue;
        } catch (NumberFormatException e) {
            log.warn("Bad number format for property '" + key + "': "
                     + e.getMessage() + ". Using default " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Look up a long property
     *
     * @param key The name of the property to look up.
     * @return Value as a long.
     * @throws NullPointerException          if the property is not found.
     * @throws IllegalArgumentException      if the property is found but does
     *                                       not parse as a long.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     */
    public long getLong (String key) {
        Object val = get(key);
        if (val == null) {
            throw new NullPointerException ("No such property: " + key);
        }

        String sval = Environment.escapeSystemProperties(val.toString()).trim();
        try {
            return Long.parseLong(sval);
        } catch (NumberFormatException e) {
            try {
                return (long) Double.parseDouble(sval);
            } catch (NumberFormatException ee) {
                throw new IllegalArgumentException("Bad number format for '"
                                                   + key + "': "
                                                   + e.getMessage());
            }
        }
    }

    /**
     * Look up a long property. If it is not defined or does not parse
     * as a long, return {@code defaultValue}.
     *
     * @param key          The name of the property to look up.
     * @param defaultValue The value to return if the value for the key could
     *                     not be extracted.
     * @return The value for key as a long.
     */
    public long getLong(String key, long defaultValue) {
        try {
            return getLong(key);
        } catch (NullPointerException e) {
            log.debug("Unable to find property '" + key + "', using default "
                      + defaultValue);
            return defaultValue;
        } catch (NumberFormatException e) {
            log.warn("Bad number format for property '" + key + "': "
                     + e.getMessage() + ". Using default " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Look up a boolean property.
     *
     * @param key The name of the property to look up.
     * @return Value as a boolean.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException      if the property is found but does
     *                                       not parse as a boolean.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     */
    public boolean getBoolean (String key) {
        Object val = get (key);
        if (val == null) {
            throw new NullPointerException ("No such property: " + key);
        }
        try {
            return Boolean.parseBoolean(
                    Environment.escapeSystemProperties(val.toString()).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The property " + key
                                               + " is not a boolean, but a "
                                               + val.getClass());
        }
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object val = get(key);
        if (val == null) {
            log.debug("Unable to locate value for key '" + key
                      + "'. Defaulting to " + defaultValue);
            return defaultValue;
        }
        try {
            return Boolean.parseBoolean(
                    Environment.escapeSystemProperties(val.toString()).trim());
        } catch (NumberFormatException e) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("Bad format for property '" + key + "' with value '"
                     + val + "'. Expected boolean, got " + val.getClass()
                     + ". Defaulting to " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Look up a list of Strings, previously stored with {@link #setStrings}.
     * All string elements will be trimmed for leading and trailing white space.
     * <p/>
     * Any references to system properties will be expanded. System properties
     * a referenced in standard Ant-style syntax, eg:<br/>
     * <pre>
     *   ${user.home}
     * </pre>
     * This expansion will not change the actual value stored in the
     * configuration.
     *
     * @param key The name of the property to look up.
     * @return Aalue s a list of trimmed Strings.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException if the property is found but does not
     *         parse as a list of Strings.
     * @throws ConfigurationStorageException if there is an error communicating
     *         with the storage backend.
     */
    public List<String> getStrings(String key) {
        Object val = get (key);
        if (val instanceof List) {
            ArrayList<String> result =
                    new ArrayList<String>(((List)val).size());
            for (Object o: (List)val) {
                result.add(
                       Environment.escapeSystemProperties(o.toString().trim()));
            }
            return result;
        }
        if (val instanceof String[]) {
            String[] val_a = (String[])val;
            for (int i = 0; i < val_a.length; i++) {
                val_a[i] = Environment.escapeSystemProperties(val_a[i].trim());
            }
            return Arrays.asList((String[]) val);
        }
        if (val == null) {
            throw new NullPointerException ("No such property: " + key);
        }
        String[] unescaped = val.toString().split(", |,");
        ArrayList<String> result = new ArrayList<String>(unescaped.length);
        for (String s: unescaped) {
            String escaped = s.replaceAll("&comma;", ",").
                                                replaceAll("&amp;", "&").trim();
            escaped = Environment.escapeSystemProperties(escaped);
            result.add(escaped);
        }
        return result;
    }

    /**
     * Look up a list of Strings, previously stored with {@link #setStrings}.
     * <p/>
     * Any references to system properties will be expanded. System properties
     * are referenced in standard Ant-style syntax, eg:<br/>
     * <pre>
     *   ${user.home}
     * </pre>
     * This expansion will not change the actual value stored in the
     * configuration.
     * <p/>
     * System property references inside the {@code defaultValues} will also be
     * escaped if need be. This will be done in a copy of {@code defaultValues}
     * and the contents of {@code defaultValues} will not be changed.
     *
     * @param key The name of the property to look up.
     * @param defaultValues The values to return if there is no list of Strings
     *                      specified for the given key.
     * @return Value as a list of Strings.
     */
    public List<String> getStrings(String key, List<String> defaultValues) {
        try {
            return getStrings(key);
        } catch (NullPointerException e) {
            return Environment.escapeSystemProperties(defaultValues);
        } catch (IllegalArgumentException e) {
            log.warn(String.format(
                    "The property %s was expected to be a list of Strings, but "
                    + "it was not. Using default %s instead",
                    key, defaultValues));
            return Environment.escapeSystemProperties(defaultValues);
        }
    }

    /**
     * Wrapper for the list-baset {@link #getStrings(String, List)} method.
     * <p/>
     * Any references to system properties will be expanded. System properties
     * are referenced in standard Ant-style syntax, eg:<br/>
     * <pre>
     *   ${user.home}
     * </pre>
     * This expansion will not change the actual value stored in the
     * configuration.
     * <p/>
     * System property references inside the {@code defaultValues} will also be
     * escaped if need be. This will be done in a copy of {@code defaultValues}
     * and the contents of {@code defaultValues} will not be changed.
     *
     * @param key The name of the property to look up.
     * @param defaultValues The values to return if there is no list of Strings
     *                      specified for the given key.
     * @return Aalue as an array of Strings.
     */
    public String[] getStrings(String key, String[] defaultValues) {
        List<String> result = getStrings(key, defaultValues == null ? null :
                                              Arrays.asList(defaultValues));

        // Sys props are already escaped from the getStrings() call,
        // so we don't have to do that
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

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair)) {
                return false;
            }
            Pair p = (Pair)o;
            return t.equals(p.getFirst()) && u.equals(p.getSecond()); 
        }
    }
    protected Pattern numberPattern =
            Pattern.compile("(.+)\\( *(\\-?[0-9]+) *\\).*");

    /**
     * Parses the value for the key for Strings and Integers and returns them
     * as a list of Pairs.
     * </p><p>
     * Sample value: a(1),b (2), c(4), d, e(16).
     *
     * @param key          The name of the property to look up.
     * @param defaultValue The Integer that should be used when one of the
     *                     sub-values isn't specified.
     *                     Example: The value in the property is as specified
     *                     above and the defaultValue is 7. This produces
     *                     (a, 1), (b, 2), (c, 4), (d, 7), (e, 16).
     * @return A list of pairs of Strings and Integers.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException if the property is found but does not
     *         parse as a list of Strings.
     * @throws ConfigurationStorageException if there is an error communicating
     *         with the storage backend.
     */
    public List<Pair<String, Integer>> getIntValues(String key,
                                                    Integer defaultValue) {
        List<String> elements = getStrings(key);
        List<Pair<String, Integer>> result =
                new ArrayList<Pair<String, Integer>>(elements.size());
        for (String element: elements) {
            Matcher numberMatcher = numberPattern.matcher(element);
            if (numberMatcher.matches()) {
                result.add(new Pair<String, Integer>(
                     Environment.escapeSystemProperties(numberMatcher.group(1)),
                     Integer.parseInt(
                                      Environment.escapeSystemProperties(
                                                     numberMatcher.group(2)))));
            } else {
                result.add(new Pair<String, Integer>(element.trim(),
                                                     defaultValue));
            }
        }
        return result;
    }

    /**
     * Look up a stored {@link Class}.
     *
     * @param key The name of the property to look up.
     * @param classType The class of which the return type should be.
     * @return The {@link Class} associated with {@code key}.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException      if the property is found but does
     *                                       not map to a known {@link Class}
     *                                       or subclass of {@code classType}.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     */
    @SuppressWarnings ("unchecked")
    public <T> Class<? extends T> getClass (String key, Class<T> classType) {
        return getClass(key, classType, this);
    }

    public Class getClass(String key) {
        return getClass(key, Object.class);
    }

    public static Class getClass (String key, Configuration conf) {
        return Configuration.getClass(key, Object.class, conf);
    }

    /**
     * Static version of getClass. Use this to ensure that the classLoader runs
     * locally and not through RMI.
     *
     * @param key The name of the property to look up.
     * @param classType The class of which the return type should be.
     * @param conf The configuration from where the class-name is located.
     * @return The {@link Class} associated with {@code key}.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException      if the property is found but does
     *                                       not map to a known {@link Class}
     *                                       or subclass of {@code classType}.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     */
    public static <T> Class<? extends T> getClass(String key,
                                                  Class<T> classType,
                                                  Configuration conf) {
        Object val = conf.get(key);
        if (val == null) {
            throw new NullPointerException("No such property: " + key);
        }
        if (classType == null) {
            throw new NullPointerException("Class type is null");
        }

        if (val instanceof String) {
            val = ((String)val).trim();
            try {
                val = Class.forName((String)val);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("The property " + key
                                                   + " with value '" + val
                                                   + "' does not map to an "
                                                   + "existing class", e);
            }
        }

        try {
            return (Class<T>) val;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The property " + key
                                             + " does not map to a subclass"
                                             + " of " + classType, e);
        }
    }

    /**
     * Like {@link Configuration#getClass(String,Class,Configuration)}. Use this
     * static variant of {@link #getClass} to avoid loading the class over RMI.
     *
     * @param key The property name to look up.
     * @param classType The class to cast the return value to.
     * @param defaultClass Default class to return if {@code key} was not found
     *                     in {@code conf}.
     * @param conf The {@code Configuration} used to look up {@code key}.
     * @return The class defined for the given key or {@code defaultClass} if
     *         {@code key} is not found in {@code conf}.
     */
    public static <T> Class<? extends T> getClass(String key,
                                                Class<T> classType,
                                                Class<? extends T> defaultClass,
                                                Configuration conf) {
        try {
            return Configuration.getClass(key, classType, conf);
        } catch (NullPointerException e) {
            log.info ("No class defined for property' " + key
                        + "'. Using default: " + defaultClass.getName());
            return defaultClass;
        }
    }

    /**
     * Get a class value resorting to a default class if it is not found
     * in the configuration.
     *
     * @param key The name of the property to look up.
     * @param classType The class of which the return type should be.
     * @param defaultValue Default class to return of the requested key is not
     *                     found.
     * @return The {@link Class} associated with {@code key} or the defaultValue
     *         if {@code key} is not found.
     */
    public <T> Class<? extends T> getClass (String key,
                                            Class<T> classType,
                                            Class<? extends T> defaultValue) {
        try {
            return getClass(key, classType);
        } catch (NullPointerException e) {
            log.info ("Unable to find class for property '" + key
                      + "'. Using default '" + defaultValue.getName() + "'");
            return defaultValue;
        }
    }
    /**
     * Queries the underlying storage for the value corresponding to the given
     * key. If a value exists, true is returned.
     *
     * @param key The key for the value.
     * @return True if a value for the key exists. False is returned if error
     *         occour in underlaying storage.
     */
    public boolean valueExists(String key) {
        try {
            return storage.get(key) != null;
        } catch (IOException e) {
            log.error ("Failed to detect existence of value '" + key + "': "
                       + e.getMessage (), e);
            return false;
        }
    }

    /**
     * Get an {@link Iterator} over all key-value pairs in the
     * {@link ConfigurationStorage} backend of the {@code Configuration}.
     *
     * @return The iterator as described above.
     */
    public Iterator<Map.Entry<String,Serializable>> iterator () {
        try {
            return storage.iterator();
        } catch (IOException e) {
            throw new ConfigurationStorageException("Failed to create "
                                                    + "configuration iterator",
                                                    e);
        }
    }

    /**
     * Return a reference to the {@link ConfigurationStorage} backend of this
     * {@code Configuration}.
     *
     * @return The storage backend of this {@code Configuration}.
     */
    public ConfigurationStorage getStorage () {
        return storage;
    }

    /**
     * Import all properties from a given {@code Configuration} and store them
     * in the {@link ConfigurationStorage} of this {@code Configuration}.
     *
     * @param conf The configuration from which to import properties.
     */
    public void importConfiguration (Configuration conf) {
        for (Map.Entry<String, Serializable> entry : conf) {
            try {
                storage.put (entry.getKey(), entry.getValue());
            } catch (IOException e) {
                throw new ConfigurationStorageException("Unable to import "
                                                        + "property '"
                                                        + entry.getKey() + "="
                                                        + entry.getValue()
                                                        + "'", e);
            }
        }
    }

    /**
     * Get an array of {@link String}s formatted like {@code key=value}.
     * Beware that stored objects that does not a have sane string
     * representation will <i>not</i> use their serialized form.
     * </p><p>This method is mainly for debugging purposes.
     *
     * @return And array of strings formatted as {@code key=value}.
     */
    public String[] dump () {
        ArrayList<String> list = new ArrayList<String>();
        for (Map.Entry pair : this) {
            list.add (pair.getKey() + "=" + pair.getValue());
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Dump the array described in {@link #dump} as string.
     *
     * @return A string dump of the array described in {@link #dump} with each
     * property on a new line.
     */
    public String dumpString () {
        String result = "";
        for (String s : dump()) {
            result += s + "\n";
        }
        return result;
    }

    /**
     * Same as {@link #dumpString}
     * @return Same as {@link #dumpString}
     */
    public String toString () {
        return dumpString();
    }

    /**
     * Load properties from the given file into this config.
     *
     * @param filename Name of file in the classpath
     * @throws IOException if there was a problem loading the file
     */
    public void loadFromXML (String filename) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Properties p = new Properties();
        p.loadFromXML(loader.getResourceAsStream(filename));

        for (Object prop : p.keySet()) {
            set (prop.toString(), (Serializable)p.get(prop));
        }

    }

    /**
     * Instantiate a {@link Configurable} class with {@code this} as argument
     * to the Configurable's constructor.
     *
     * @param configurable he {@code Configurable} class to instantiate.
     * @return An object instantiated from the given class.
     * @throws Configurable.ConfigurationException if there is a problem
     *                                   instantiating the {@code Configurable}.
     * @throws IllegalArgumentException if the input class in not a
     *                                  {@code Configurable}.
     * @deprecated use the static method instead, in order to guard against
     *             unintentional RMI-based creation.
     */
    public <T> T create(Class<T> configurable) {
        return create(configurable, this);
    }

    /**
     * A static version of create. Use this when the configuration is expected
     * to be remote, in order to avoid a RMI-bases class-loading and
     * instantiation.
     * </p><p>
     * If no Configuration-taking constructor was found, an attempt is made to
     * instantiate the class with the empty constructor.
     *
     * @param configurable The {@code Configurable} class to instantiate.
     * @param conf The configuration to give to the constructor.
     * @return An object instantiated from the given class.
     * @throws Configurable.ConfigurationException if there is a problem
     *                                   instantiating the {@code Configurable}.
     * @throws IllegalArgumentException if the input class in not a
     *                                  {@code Configurable}.
     */
    public static <T> T create (Class<T> configurable, Configuration conf) {
        Security.checkSecurityManager();

        try {
            Constructor<T> con =
                    configurable.getConstructor(Configuration.class);
            return con.newInstance(conf);

        } catch (NoSuchMethodException e) {
            log.debug(String.format(
                    "No constructor taking Configuration in %s."
                    + " Creating object with empty constructor instead",
                    configurable.getSimpleName()));
            return createNonConfigurable(configurable);
        } catch (IllegalAccessException e) {
            throw new Configurable.ConfigurationException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new Configurable.ConfigurationException(e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new Configurable.ConfigurationException(e.getMessage(), e);
        }
    }

    private static <T> T createNonConfigurable (Class<T> configurable) {
        Security.checkSecurityManager();

        try {
            return configurable.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format(
                    "No empty constructor in  %s",
                    configurable.getSimpleName()), e);
        } catch (IllegalAccessException e) {
            throw new Configurable.ConfigurationException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new Configurable.ConfigurationException(e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new Configurable.ConfigurationException(e.getMessage(), e);
        }
    }

    /**
     * Does deep comparison of all key-value pairs.
     *
     * @param conf the configuration to compare {@link this} with.
     * @return tur if configurations matches on all key-value pairs.
     */
    public boolean equals (Configuration conf) {
        try {
            if (conf.getStorage().size() != this.storage.size()) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        for (Map.Entry<String, Serializable> entry : conf) {
            if (! this.get(entry.getKey()).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a {@link Configuration} object as defined by the system property
     * {@code summa.control.configuration}. The value of this property
     * can be an URL or an rmi service path.
     * Use of URL means that the configuration is loaded only once and this
     * is static. Use of RMI means that every lookup in the configuration will
     * result in a RMI call.
     *
     * @param configPropName Name of system property containing the address
     *                       of the system configuration.
     * @return a newly instantiated configuration.
     */
    public static Configuration getSystemConfiguration (String configPropName) {
        return getSystemConfiguration(configPropName, false);
    }

    /**
     * As {@link #getSystemConfiguration(String)} but if the {@code allowUnset}
     * argument is {@code true} and the system property {@code configPropName}
     * is not set try hard to look up a configuration anywhere and return
     * an empty one if none can be found.
     * <p></p>
     * The configurations tested if {@code allowUnset == true} can be found
     * in {@link #DEFAULT_RESOURCES}.
     *
     * @param configPropName Name of system property containing the address
     *                       of the system configuration.
     * @param allowUnset If {@code true} scavenge the system for anything
     *                   looking like a configuration and return an empty one if
     *                   none where found.
     * @return System configuration or an empty config if none where found.
     * @throws ConfigurationException if {@code allowUnset == false} and
     *                                and {@code configPropName} was not set
     *                                as a system property.
     */
    public static Configuration getSystemConfiguration (String configPropName,
                                                        boolean allowUnset) {
        log.trace ("Getting system config for property '" + configPropName + "'"
                   + ". Allowing unset: " + allowUnset);

        String confLocation = System.getProperty(configPropName);

        if (confLocation == null) {
            if (allowUnset) {
                log.debug ("System configuraion property '" + configPropName
                     + "' " + "not set. Looking for configuration resource...");

                ClassLoader loader =
                                 Thread.currentThread().getContextClassLoader();
                String confResource = null;

                for (String res : DEFAULT_RESOURCES) {
                    confResource = res;
                    if (loader.getResource (res) == null) {
                        log.trace ("Configuration resource '" + res + "' not " +
                                   "found");
                    } else {
                        log.debug ("Found configuration resource '" + res
                                + "'");
                        break;
                    }
                }

                /* Use an empty conf, if we did not find one */
                if (confResource == null) {
                    log.info ("Did not find any system configuration. " +
                              "Using empty configuration");
                    return Configuration.newMemoryBased();
                }

                /* We have a resource, now try and load it however we can */
                ConfigurationStorage storage;

                // Does this look like a Javascript resource?
                if (confResource.endsWith(".js")) {
                    try {
                        storage = new JStorage(confResource);
                    } catch (IOException e) {
                        throw new ConfigurationStorageException(
                                    "Failed to load Javascript configuration '"
                                    + confLocation + "': " + e.getMessage(), e);
                    }
                    return new Configuration(storage);
                }

                try {
                    storage = new XStorage(confResource);
                    log.debug("Loaded '" + confResource + "' as XProperties");
                    return new Configuration (storage);
                } catch (Exception e) {
                    log.info("Failed to load '" + confResource
                              + "' as XProperties:" + e.getMessage ());
                    log.debug("Failed to load '" + confResource
                               + "' as XProperties:" + e.getMessage (), e);
                }

                try {
                    storage = new FileStorage (confResource);
                    log.debug("Loaded '" + confResource + "' as standard " +
                               "properties");
                    return new Configuration(storage);
                } catch (Exception e) {
                    log.info("Failed to load '" + confResource + "'", e);
                }

                log.info("Failed to load any configuration." +
                          "Using empty configuration");
                return Configuration.newMemoryBased ();
            } else {
                throw new ConfigurationException("Required system property '"
                                                + configPropName + "' not set");
            }
        }
        return load(confLocation);
    }

    /**
     * Get the default system configuration as specified in the system
     * property {@link #CONF_CONFIGURATION_PROPERTY}.
     *
     * @return A newly instantiated configuration.
     */
    public static Configuration getSystemConfiguration () {
        return getSystemConfiguration(CONF_CONFIGURATION_PROPERTY);
    }

    /**
     * <p>Get the default system configuration as specified in the system
     * property {@link #CONF_CONFIGURATION_PROPERTY}.</p>
     * <p>If the system configuration is not set, and {@code allowUnset} is
     * true, use an empty memory based configuration instead.</p>
     * @see #getSystemConfiguration(String, boolean).
     *
     * @param allowUnset If {@code true}.
     * @throws ConfigurationException if {@code allowUnset == false} and the
     *                                system property
     *                                {@code summa.configuration} is not set.
     * @return a newly instantiated configuration.
     */
    public static Configuration getSystemConfiguration (boolean allowUnset) {
        return getSystemConfiguration(CONF_CONFIGURATION_PROPERTY, allowUnset);
    }

    /**
     * <p>Create a configuration given a location to load from.
     * The location can be one of:</p>
     * <b>Examples:</b>
     * <ul>
     *   <li>{@code http://example.com/myConfig.xml}, a URL</li>
     *   <li>{@code //registryhost:port/servicename}, an RMI address</li>
     *   <li>{@code /home/summa/config/foo.xml}, an absolute path</li>
     *   <li>{@code config/foo.xml}, loaded as a resource from the
     *       classpath</li>
     * </ul>
     *
     * @param confLocation Location of configuration as specified above.
     * @return A newly instantiated configuration object.
     */
    public static Configuration load (String confLocation) {
        ConfigurationStorage storage;

        if (confLocation == null) {
            throw new NullPointerException("confLocation is null");
        }

        if (confLocation.startsWith("//")) {
            // This is an rmi path
            log.debug ("Loading configuration from RMI " + confLocation);
            try {
                storage = RemoteStorage.getRemote(confLocation);
            } catch (Exception e) {
                //noinspection DuplicateStringLiteralInspection
                throw new ConfigurationException(
                        "Unable to connect to " + confLocation, e);
            }
        } else if (confLocation.contains("://")) {
            // This is an URL
            log.debug ("Loading configuration from URL " + confLocation);
            try {
                URL storageUrl = new URL (confLocation);
                // TODO: Add XStorage and JStorage capabilities
                storage = new MemoryStorage(storageUrl);
            } catch (Exception e) {
                throw new ConfigurationException(
                       "Unable retrieve configuration from " + confLocation, e);
            }
        } else { /* {if (confLocation.startsWith("/")) {*/
            // Assume this is a regular file
            try {
                if (confLocation.startsWith("/")) {
                    log.debug ("Loading configuration from file "
                               + confLocation);
                    if (confLocation.endsWith(".js")) {
                        storage = new JStorage(new File(confLocation));
                        log.trace("Loaded JStorage configuration");
                    } else {
                        storage = new FileStorage(new File(confLocation));
                        log.trace("Loaded FileStorage configuration");
                    }
                } else {
                    log.debug ("Loading configuration from resource "
                               + confLocation);
                    if (confLocation.endsWith(".js")) {
                        storage = new JStorage(confLocation);
                        log.trace("Loaded JStorage from resource");
                    } else {
                        storage = new FileStorage (confLocation);
                        log.trace(
                              "Loaded FileStorage configuration from resource");
                    }
                }
            } catch (FileNotFoundException e) {
                //noinspection DuplicateStringLiteralInspection
                throw new ConfigurationException("Could not locate "
                                                 + "configuration at '"
                                                 + confLocation + "'");
            } catch (IOException e) {
                if (log.isTraceEnabled()) {
                    log.debug("Failed loading configuration from "
                              + confLocation, e);
                } else {
                    log.debug("Failed loading configuration from "
                              + confLocation + " (enable trace to get "
                              + "Exception)");
                }
                    storage = null;
            } catch (ConfigurationStorageException e) {
                log.debug("Configuration storage for " + confLocation
                         + " encountered a problem", e);
                storage = null;
            }
            if (storage == null) {
                log.debug("Could not load configuration using FileStorage. "
                          + "Switching to XStorage");

                try {
                    storage = new XStorage(confLocation);
                    log.trace("Loaded XStorage configuration");
                } catch (IOException ex) {
                    throw new ConfigurationException("Error reading "
                                                     + "configuration file '"
                                                     + confLocation
                                                     + "' using XStorage", ex);
                }
                //throw new ConfigurationException("Error reading configuration"
                // + " file " + confLocation, e);
            }
        } /*else {
            // Assume this is a resource
            log.debug ("Loading configuration from resource " + confLocation);
            try {
                storage = new FileStorage (confLocation);
            } catch (IOException e) {
                throw new ConfigurationException("Error reading configuration "
                +"file " + confLocation, e);
            }
        }   */
        log.trace("Returning storage of type " + storage.getClass());
        return new Configuration(storage);
    }

    /**
     * Determines whether the underlying storage supports sub storages.
     *
     * @return True if sub configurations can be requested.
     */
    public boolean supportsSubConfiguration() {
        try {
            return storage.supportsSubStorage();
        } catch (IOException e) {
            log.error("Error querying configuration " + this
                      + " for supportsSubConfiguration(), returning false.");
            return false;
        }
    }

    /**
     * Constructs a new Configuration around an underlying sub storage.
     *
     * @param key The key for the underlying sub storage.
     * @return A sub configuration, based on the sub storage.
     * @throws IOException if the sub configuration could not be created.
     */
    public Configuration createSubConfiguration(String key) throws IOException {
        return new Configuration(storage.createSubStorage(key));
    }

    /**
     * Get configuration for given 'key'.
     *
     * @param key The key for the underlying sub storage.
     * @return A sub configuration, based on the sub storage.
     * @throws NullPointerException if the sub configuration could not be
     * extracted.
     * @throws SubConfigurationsNotSupportedException if sub configuration isn't
     * supported.
     */
    public Configuration getSubConfiguration(String key) 
          throws NullPointerException, SubConfigurationsNotSupportedException {
        try {
            if(!storage.supportsSubStorage()) {
                throw new SubConfigurationsNotSupportedException("Storage '"
                        + storage.toString()
                        + "' doesn't support sub configuration");
            }
            return new Configuration(storage.getSubStorage(key));
        } catch (IOException e) {
            throw new NullPointerException(String.format(
                    "Unable to extract subConfiguration '%s'", key));
        }
    }

    /**
     * Creates a list of sub storages under the given key and returns it wrapped
     * as Configurations.
     *
     * @param key The key for the list sub storages.
     * @param count The number of configurations that the list should contain.
     * @return A list of sub storages wrapped as Configurations.
     * @throws IOException if the list could not be created.
     */
    public List<Configuration> createSubConfigurations(String key, int count)
                                                            throws IOException {
        List<ConfigurationStorage> storages =
                storage.createSubStorages(key, count);
        List<Configuration> configurations =
                new ArrayList<Configuration>(storages.size());
        for (ConfigurationStorage storage: storages) {
            configurations.add(new Configuration(storage));
        }
        return Collections.unmodifiableList(configurations);
    }

    /**
     * Return a list of configurations for sub storages.
     * 
     * @param key The key for the list of sub storages.
     * @return A list of sub storages wrapped as Configurations.
     * @throws NullPointerException if the sub storages could not be retrieved.
     * @throws SubConfigurationsNotSupportedException if one of the storages
     * doesn't support sub configurations.
     */
    public List<Configuration> getSubConfigurations(String key) throws 
                  NullPointerException, SubConfigurationsNotSupportedException {
        try {
            List<ConfigurationStorage> storages =
                    storage.getSubStorages(key);
            List<Configuration> configurations =
                    new ArrayList<Configuration>(storages.size());
            for (ConfigurationStorage storage: storages) {
                if(!storage.supportsSubStorage()) {
                    throw new SubConfigurationsNotSupportedException("Storage '"
                        + storage.toString()
                        + "' doesn't support sub configurations");
                }
                configurations.add(new Configuration(storage));
            }
            return Collections.unmodifiableList(configurations);
        } catch (IOException e) {
            throw new NullPointerException(String.format(
                    "Unable to extract sub-configurations for key '%s'", key));
        }
    }

    /**
     * Convenience method to look up the system global directory for persistent
     * data. This is defined by the property {@link #CONF_PERSISTENT_DIR}.
     * <p></p>
     * If the configuration does not define the property the system property
     * with the same name will be checked. If that fails too the default
     * of {@code $HOME/summa-control/persistent will be used.
     *
     * @return A {@code File} pointing to the root directory where persistent
     *         data should be stored.
     */
    public File getPersistentDir () {
        try {
            return new File (getString(CONF_PERSISTENT_DIR));
        } catch (NullPointerException e) {
            log.debug (CONF_PERSISTENT_DIR + " not defined in configuration");
            String loc = System.getProperty(CONF_PERSISTENT_DIR);
            if (loc == null) {
                loc = System.getProperty("user.home") + File.separator
                        + "summa-control" + File.separator + "persistent";
                log.debug ("System property " + CONF_PERSISTENT_DIR + "not"
                           + " defined. Using default: " + loc);
                return new File (loc);
            }
            log.debug ("Using system property " + CONF_PERSISTENT_DIR + " "
                       +  "for persistent data: " + loc);
            return new File (loc);
        }
    }
}





