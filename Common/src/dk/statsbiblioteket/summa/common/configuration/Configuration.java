/* $Id: Configuration.java,v 1.16 2007/10/29 11:01:45 mke Exp $
 * $Revision: 1.16 $
 * $Date: 2007/10/29 11:01:45 $
 * $Author: mke $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.common.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.rmi.RMISecurityManager;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.RemoteStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Class needs Javadoc")
// TODO: Use ConvenientMap for convenience
public class Configuration implements Serializable,
                                      Iterable<Map.Entry<String,Serializable>>,
                                      Configurable {
    private static Log log = LogFactory.getLog(Configuration.class);

    private ConfigurationStorage storage;
    public static final String DEFAULT_RESOURCE = "control.properties.xml";

    /** System property defining where to fetch the configuration.
     * This can be a normal URL or an rmi path.*/
    public static final String CONFIGURATION_PROPERTY = "summa.configuration";

    /**
     * Create a {@code Configuraion} with the given {@link ConfigurationStorage}
     * backend.
     * @param storage the storage backend to use
     */
    public Configuration (ConfigurationStorage storage) {
        this.storage = storage;
    }

    /**
     * Create a {@code Configuration} using the same
     * {@link ConfigurationStorage} as a given other {@code Configuration}.
     * @param conf the {@code Configuration} to share storage with.
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
     * @param args <p>the arguments should be {@code String, Serializable} pairs
     *        if the argument count is odd or each even numbered argument
     *        is not a {@code String} an {@code IllegalArgumentException}
     *        is thrown</p>
    *
     *
     * @return A {@code Configuration} mapping the key value pairs provided
     *         in the argument list
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
     * @param key name of property to store.
     * @param value the value to associate with {@code key}.
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
     * @param key     name of the property to store.
     * @param strings the Strings to store.
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
     * @param key the name of the value to look up.
     * @return the value assciated with {@code key}.
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
     * Remove a property from the configuration
     * @param key the name of the proeprty to remove
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
     * Look up a stored {@code String} value.
     * @param key the name of the value to look up.
     * @return the {@code String} representation of the value assciated with
     *         {@code key}. If the value does not exist, an exception is thrown.
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
        return val.toString();
    }

    /**
     * Look up a stored {@code String} value.
     * @param key          the name of the value to look up.
     * @param defaultValue if the value does not exist, return this instead.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     * @return the {@code String} representation of the value associated with
     *         {@code key} or the defaultValue, if the key did not exist.
     */
    public String getString(String key, String defaultValue) {
        Object val = get(key);
        if (val == null) {
            return defaultValue;
        }
        return val.toString();
    }

    /**
     * Look up an integer property
     * @param key the name of the property to look up
     * @return value as an integer
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
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad number format for '" + key
                                               + "': " + e.getMessage());
        }
    }

    /**
     * Look up an integer property. If it is not defined or does not parse
     * as an integer, return {@code defaultValue}.
     * @param key          the name of the property to look up
     * @param defaultValue the value to return if the value for the key could
     *                     not be extracted.
     * @return the value for key as an int.
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
     * @param key the name of the property to look up
     * @return value as a long
     * @throws NullPointerException          if the property is not found
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
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad number format for '" + key
                                               + "': " + e.getMessage());
        }
    }

    /**
     * Look up a long property. If it is not defined or does not parse
     * as a long, return {@code defaultValue}.
     * @param key          the name of the property to look up
     * @param defaultValue the value to return if the value for the key could
     *                     not be extracted.
     * @return the value for key as a long.
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
     * Look up a boolean property
     * @param key the name of the property to look up
     * @return value as a boolean
     * @throws NullPointerException if the property is not found
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
            return Boolean.parseBoolean(val.toString());
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
            return Boolean.parseBoolean(val.toString());
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
     * @param key the name of the property to look up.
     * @return value as a list of Strings.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException if the property is found but does not
     *         parse as a list of Strings
     * @throws ConfigurationStorageException if there is an error communicating
     *         with the storage backend
     */
    public List<String> getStrings(String key) {
        Object val = get (key);
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
        if (val == null) {
            throw new NullPointerException ("No such property: " + key);
        }
        String[] unescaped = val.toString().split(", |,");
        ArrayList<String> result = new ArrayList<String>(unescaped.length);
        for (String s: unescaped) {
            result.add(s.replaceAll("&comma;", ",").
                         replaceAll("&amp;", "&"));
        }
        return result;
    }

    /**
     * Look up a list of Strings, previously stored with {@link #setStrings}.
     * @param key the name of the property to look up.
     * @param defaultValues the values to return if there is no list of Strings
     *                      specified for the given key.
     * @return value as a list of Strings.
     */
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

    /**
     * Look up a stored {@link Class}.
     * @param key the name of the property to look up.
     * @param classType the class of which the return type should be.
     * @return the {@link Class} associated with {@code key}.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException      if the property is found but does
     *                                       not map to a known {@link Class}
     *                                       or subclass of {@code classType}.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     */
    @SuppressWarnings ("unchecked")
    public <T> Class<T> getClass (String key, Class<T> classType) {
        return getClass(key, classType, this);
    }

    public Class getClass(String key) {
        return getClass(key, Object.class);
    }

    public static Class getClass (String key, Configuration conf) {
        return getClass(key, Object.class, conf);
    }

    /**
     * Static version of getClass. Use this to ensure that the classLoader runs
     * locally and not through RMI.
     * @param key the name of the property to look up.
     * @param classType the class of which the return type should be.
     * @param conf the configuration from where the class-name is located.
     * @return the {@link Class} associated with {@code key}.
     * @throws NullPointerException if the property is not found.
     * @throws IllegalArgumentException      if the property is found but does
     *                                       not map to a known {@link Class}
     *                                       or subclass of {@code classType}.
     * @throws ConfigurationStorageException if there is an error communicating
     *                                       with the storage backend.
     */
    public static <T> Class<T> getClass(String key, Class<T> classType,
                                        Configuration conf) {
        Object val = conf.get(key);
        if (val == null) {
            throw new NullPointerException("No such property: " + key);
        }
        if (classType == null) {
            throw new NullPointerException("Class type is null");
        }

        if (val instanceof String) {
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
     * Get a class value resorting to a default class if it is not found
     * in the configuration.
     * @param key the name of the property to look up.
     * @param classType the class of which the return type should be.
     * @param defaultValue default class to return of the requested key is not found
     * @return the {@link Class} associated with {@code key} or the defaultValue
     *         if {@code key} is not found
     */
    public <T> Class<? extends T> getClass (String key,
                                            Class<T> classType,
                                            Class<? extends T> defaultValue) {
        try {
            return getClass(key, classType);
        } catch (NullPointerException e) {
            log.warn ("Unable to find class for property '" + key
                      + "'. Using default '" + defaultValue.getName() + "'");
            return defaultValue;
        }
    }
    /**
     * Queries the underlying storage for the value corresponding to the given
     * key. If a value exists, true is returned.
     * @param key the key for the value.
     * @return true if a value for the key exists.
     * @throws IOException if there was a problem communicating with the
     *                     underlying storage.
     */
    public boolean valueExists(String key) throws IOException {
        return storage.get(key) != null;
    }

    /**
     * Get an {@link Iterator} over all key-value pairs in the
     * {@link ConfigurationStorage} backend of the {@code Configuration}.
     * @return the iterator as described above.
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
     * @return the storage backend of this {@code Configuration}.
     */
    public ConfigurationStorage getStorage () {
        return storage;
    }

    /**
     * Import all properties from a given {@code Configuration} and store them
     * in the {@link ConfigurationStorage} of this {@code Configuration}.
     * @param conf the configuration from which to import properties
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
     *
     * <p>This method is mainly for debugging purposes.
     * @return And array of strings formatted as {@code key=value}
     */
    public String[] dump () {
        ArrayList<String> list = new ArrayList<String>();
        for (Map.Entry pair : this) {
            list.add (pair.getKey() + "=" + pair.getValue());
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Dump the array described in {@link #dump} as string
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
     * Load default config from the {@link #DEFAULT_RESOURCE}
     * if it is found in the classpath.
     * @throws java.io.IOException if there was a problem loading the file
     */
    public void loadDefaults () throws IOException {
        loadFromXML(DEFAULT_RESOURCE);
    }

    /**
     * Load properties from the given file into this config.
     * @param filename name of file in the classpath
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
     * @param configurable the {@code Configurable} class to instantiate.
     * @return an object instantiated from the given class.
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
     *
     * @param configurable the {@code Configurable} class to instantiate.
     * @param conf the configuration to give toth econstructor.
     * @return an object instantiated from the given class.
     * @throws Configurable.ConfigurationException if there is a problem
     *                                   instantiating the {@code Configurable}.
     * @throws IllegalArgumentException if the input class in not a
     *                                  {@code Configurable}.
     */
    public static <T> T create (Class<T> configurable, Configuration conf) {
        checkSecurityManager();
        if (!Configurable.class.isAssignableFrom(configurable)) {
            throw new IllegalArgumentException("Class " + configurable
                                               + " is not a Configurable");
        }

        try {
            Constructor<T> con =
                    configurable.getConstructor(Configuration.class);
            return con.newInstance(conf);

        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "The class " + configurable.getSimpleName()
                    + " does not have a constructor taking a Configuration"
                    + " as its sole argument", e);
        } catch (IllegalAccessException e) {
            throw new Configurable.ConfigurationException(e);
        } catch (InvocationTargetException e) {
            throw new Configurable.ConfigurationException(e);
        } catch (InstantiationException e) {
            throw new Configurable.ConfigurationException(e);
        }
    }

    /**
     * Creates an allow-all if no manager is present.
     */
    private static void checkSecurityManager() {
        if (System.getSecurityManager() == null) {
            log.warn("No security manager found. "
                     + "Setting allow-all security manager");
            System.setSecurityManager(new RMISecurityManager() {
                public void checkPermission(Permission perm) {
                    // Do nothing (allow all)
/*                    if (log.isTraceEnabled()) {
                        log.trace("checkPermission(" + perm + ") called");
                    }*/
                }
                public void checkPermission(Permission perm, Object context) {
                    // Do nothing (allow all)
/*                    if (log.isTraceEnabled()) {
                        log.trace("checkPermission(" + perm + ", " + context
                                  + ") called");
                    }*/
                }
            });
        } else {
            log.info("SecurityManager '" + System.getSecurityManager()
                     + "' present");
        }
    }

    /**
     * Does deep comparison of all key-value pairs
     * @param conf
     * @return
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
     * @param configPropName name of system property containing the address
     *                       of the system configuration
     * @return a newly instantiated configuration
     */
    public static Configuration getSystemConfiguration (String configPropName) {
        return getSystemConfiguration(configPropName, false);
    }

    /**
     * As {@link #getSystemConfiguration(String)} but if the {@code allowUnset}
     * argument is {@code true} an emoty memory based configuration will be
     * returned if the system property {@code configPropName} is not set.
     * @param configPropName name of system property containing the address
     *                       of the system configuration
     * @param allowUnset if {@code true} an empty memory based configuration
     *                   will be returned if {@code configPropName} is not set
     * @return System configuration or an empty config if none is set
     */
    public static Configuration getSystemConfiguration (String configPropName,
                                                        boolean allowUnset) {
        ConfigurationStorage storage;
        String confLocation = System.getProperty(configPropName);

        if (confLocation == null) {
            if (allowUnset) {
                log.debug ("System configuraion property '" + configPropName + "' "
                          + "not set. Using empty configuration.");
                return Configuration.newMemoryBased();
            } else {
                throw new ConfigurationException("Required system property '"
                                                 + configPropName + "' not set");
            }
        }

        return load (confLocation);
    }

    /**
     * Get the default system configuration as specified in the system
     * property {@link #CONFIGURATION_PROPERTY}
     * @return a newly instantiated configuration
     */
    public static Configuration getSystemConfiguration () {
        return getSystemConfiguration(CONFIGURATION_PROPERTY);
    }

    /**
     * <p>Get the default system configuration as specified in the system
     * property {@link #CONFIGURATION_PROPERTY}.</p>
     * <p>If the system configuration is not set, and {@code allowUnset} is
     * true, use an empty memory based configuration instead.</p>
     * @see #getSystemConfiguration(String, boolean)
     * @param allowUnset if {@code true}
     * @return a newly instantiated configuration
     */
    public static Configuration getSystemConfiguration (boolean allowUnset) {
        return getSystemConfiguration(CONFIGURATION_PROPERTY, allowUnset);
    }

    /**
     * <p>Create a configuration given a location to load from.
     * The location can be one of:</p>
     * <b>Examples:</b>
     * <ul>
     *   <li>{@code http://example.com/myConfig.xml}, a URL</li>
     *   <li>{@code //registryhost:port/servicename}, an RMI address</li>
     *   <li>{@code /home/summa/config/foo.xml}, an absolute path</li>
     *   <li>{@code config/foo.xml}, loaded as a resource from the classpath</li>
     * </ul>
     * @param confLocation Location of configuration as specified above
     * @return a newly instantiated configuration object
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
                throw new ConfigurationException("Unable to connect to " + confLocation, e);
            }
        } else if (confLocation.contains("://")) {
            // This is an URL
            log.debug ("Loading configuration from URL " + confLocation);
            try {
                URL storageUrl = new URL (confLocation);
                // TODO: Add XStorage capabilities
                storage = new MemoryStorage(storageUrl);
            } catch (Exception e) {
                throw new ConfigurationException("Unable retrieve configuration from " + confLocation, e);
            }
        } else { /* {if (confLocation.startsWith("/")) {*/
            // Assume this is a regular file
            try {
                if (confLocation.startsWith("/")) {
                    log.debug ("Loading configuration from file "
                               + confLocation);
                    storage = new FileStorage(new File(confLocation));
                    log.trace("Loaded FileStorage configuration");
                } else {
                    log.debug ("Loading configuration from resource "
                               + confLocation);
                    storage = new FileStorage (confLocation);
                    log.trace("Loaded FileStorage configuration from resource");
                }
            } catch (FileNotFoundException e) {
                //noinspection DuplicateStringLiteralInspection
                throw new ConfigurationException("Could not locate "
                                                 + "configuration at '"
                                                 + confLocation + "'");
            } catch (IOException e) {
                log.debug("Failed loading configuration from " + confLocation, e);
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
                                                     + "using XStorage", ex);
                }
                //throw new ConfigurationException("Error reading configuration file " + confLocation, e);
            }
        } /*else {
            // Assume this is a resource
            log.debug ("Loading configuration from resource " + confLocation);
            try {
                storage = new FileStorage (confLocation);
            } catch (IOException e) {
                throw new ConfigurationException("Error reading configuration file " + confLocation, e);
            }
        }   */
        log.trace("Returning storage of type " + storage.getClass());
        return new Configuration(storage);
    }

    /**
     * Determines whether the underlying storage supports sub storages.
     * @return true if sub configurations can be requested.
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
     * @param key the key for the underlying sub storage.
     * @return a sub configuration, based on the sub storage.
     * @throws IOException if the sub configuration could not be created.
     */
    public Configuration createSubConfiguration(String key) throws IOException {
        return new Configuration(storage.createSubStorage(key));
    }

    /**
     * @param key the key for the underlying sub storage.
     * @return a sub configuration, based on the sub storage.
     * @throws IOException if the sub configuration could not be created.
     */
    public Configuration getSubConfiguration(String key) throws IOException {
        return new Configuration(storage.getSubStorage(key));
    }

    /**
     * Creates a list of sub storages under the given key and returns it wrapped
     * as Configurations.
     * @param key   the key for the list sub storages.
     * @param count the number of configurations that the list should contain.
     * @return a list of sub storages wrapped as Configurations.
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
     * @param key the key for the list of sub storages.
     * @return a list of sub storages wrapped as Configurations.
     * @throws IOException if the sub storages could not be retrieved.
     */
    public List<Configuration> getSubConfigurations(String key) throws 
                                                                IOException {
        List<ConfigurationStorage> storages =
                storage.getSubStorages(key);
        List<Configuration> configurations =
                new ArrayList<Configuration>(storages.size());
        for (ConfigurationStorage storage: storages) {
            configurations.add(new Configuration(storage));
        }
        return Collections.unmodifiableList(configurations);
    }
}

