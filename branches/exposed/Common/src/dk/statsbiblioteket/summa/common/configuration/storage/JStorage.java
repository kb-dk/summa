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
package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorageException;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
@QAInfo(author = "mke",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED)
public class JStorage implements ConfigurationStorage {
    public static final long serialVersionUID = 59786486L;
    private ScriptEngineManager engineManager;
    private ScriptEngine engine;
    private String config;

    /**
     * Creates a JStorage instance, with a given script from a file. This
     * JStorage is created with an empty configuration with a call to
     * {@link JStorage#JStorage()}.
     *
     * @param file A file containing a script to evaluate
     * {@link #eval(java.io.Reader)}.
     * @throws IOException From creation of {@link java.io.FileReader}.
     */
    public JStorage(File file) throws IOException {
        this();
        eval(new FileReader(file));        
    }

    /**
     * Creates a JStorage instance, with a given system resource, this is
     * evaluated {@link JStorage#eval(java.io.Reader)} as a script. Instance is
     * create with an empty configuration {@link JStorage#JStorage()}.
     * 
     * @param resource System resource for initial script.
     * @throws IOException If resource isn't found or there occurs an error
     * reading the file.
     */
    public JStorage(String resource) throws IOException {
        this();
        InputStream _in = ClassLoader.getSystemResourceAsStream(resource);
        if (_in == null) {
            throw new FileNotFoundException("Unable to locate resource: "
                                            + resource);
        }

        try {
            eval(new InputStreamReader(_in));
        } catch (Exception e) {
            throw new ConfigurationStorageException("Error reading resource '"
                                                    + resource + "': "
                                                    + e.getMessage(), e);
        }
    }

    /**
     * Creates a JStorage with an empty configuration. The argument URL is
     * fetched and evaluated as a script.
     * @param url URL that is fetched and evaluated as a script.
     * @throws IOException If error occur while reading content from the URL.
     */
    @SuppressWarnings("unused")
    public JStorage(URL url) throws IOException {
        this();
        try {
            eval(new InputStreamReader(url.openStream()));
        } catch (Exception e) {
            throw new ConfigurationStorageException("Error reading " + url
                                                    + ": " + e.getMessage(), e);
        }
    }

    /**
     * Creates a JStorage with an empty configuration. The argument is
     * interpreted and evaluated as a script.
     *
     * @param in Input reader, content is evaluated as a script.
     */
    @SuppressWarnings("unused")
    public JStorage(Reader in) {
        this();
        try {
            eval(in);
        } catch (Exception e) {
            throw new ConfigurationStorageException("Error reading "
                                                    + "configuration from "
                                                    + "stream: "
                                                    + e.getMessage(), e);
        }
    }

    /**
     * Creates a JStorage instance with the given configuration.
     *
     * @param conf Configuration which should be used for this JStorage instace.
     */
    @SuppressWarnings("unused")
    public JStorage(Configuration conf) {
        this();
        // Magic trick to import the given configuration
        new Configuration(this).importConfiguration(conf);
    }

    /**
     * Create a new standard JStorage, with an empty configuration.
     */
    public JStorage() {
        init();
        config = "config";
        // Create new empty config
        eval(config + " = {}");
    }

    /**
     * Used for creating sub-storage.
     *
     * @param eng The scriptEngine.
     * @param engMan The scriptEngine.
     * @param conf The configuration.
     * @param createNew If {@code true} a new configuration.
     */
    private JStorage(ScriptEngine eng, ScriptEngineManager engMan,
                     String conf, boolean createNew){
        engine = eng;
        engineManager = engMan;
        config = conf;

        if (createNew) {
            eval(config + " = {}");
        }

        if (Boolean.parseBoolean(eval(config + " == null").toString())) {
            throw new RuntimeException("Can not create sub storage of a "
                                       + "non-existing sub configuration: "
                                       + config);
        }
    }

    /**
     * Private helper init method. This is responsible for:
     * - setting up the {@link javax.script.ScriptEngineManager} used for this
     * class.
     * <ul>
     *  <li>Installing __ext_size() method.</li>
     *  <li>Installing __ext_type(value) method.</li>
     *  <li>Installing __ext_new_list(list) method.</li>
     * </ul>
     */
    private void init() {
        engineManager = new ScriptEngineManager();
        engine = engineManager.getEngineByName("js");

        if (engine == null) {
            throw new RuntimeException("Script engine 'js'"
                                       + " not supported by JRE");
        }

        /* Install __ext_size() method on all objects */
        eval(
        "Object.prototype.__ext_size = function () {     \n" +
        "  var len = 0;                                  \n" +
        "  for (var k in this) {                         \n" +
        "      if ( k.substr(0,6) != '__ext_' )          \n" +
        "          len++;                                \n" +
        "  }                                             \n" +
        "  return len;                                   \n" +
        "}                                               \n"
        );

        /* Install __ext_type function in the global scope. We use this to
         * differentiate between objects and arrays */
        eval(
        "function __ext_type(value) {               \n" +
        "    var s = typeof value;                  \n" +
        "    if (s === 'object') {                  \n" +
        "        if (value) {                       \n" +
        "            if (value instanceof Array) {  \n" +
        "                s = 'array';               \n" +
        "            }                              \n" +
        "        } else {                           \n" +
        "            s = 'null';                    \n" +
        "        }                                  \n" +
        "    }                                      \n" +
        "    return s;                              \n" +
        "}"
        );

        /* Create a Java ArrayList out of a JS list */
        eval(
        "function __ext_new_list(list) {               \n" +
        "    var result = new java.util.ArrayList()    \n" +
        "    for (var elt in list) {                   \n" +
        "        if (elt.toString().substr(0,6) != '__ext_')\n" +
        "            result.add(list[elt].toString())  \n" +
        "    }                                         \n" +
        "    return result                             \n" +
        "}                                             \n"
        );
    }

    /**
     * {@inheritDoc}
     *
     * @param key The name used to access the stored object.
     * @param value The actual value to store.
     */
    @Override
    public void put(String key, Serializable value) {
        eval(config+"['"+key+"'] = "+ parseObject(value));
    }

    /**
     * {@inheritDoc}
     * @param key Name of object to look up.
     * @return A serializable object which corresponds to the key.
     */
    @Override
    public Serializable get(String key) {
        engine.put("__ext_storage", this);

        String query =
                "var val = CONFIG['KEY']                             \n"+
                "if( typeof(val) == 'function' )                     \n" +
                "    val = val()                                     \n" +
                "else if ( typeof(val) == 'object' && val != null) { \n" +
                "    if (val instanceof Array) {                     \n" +
                "        if (val.length > 0 && typeof(val[0]) == 'string')\n" +
                "           val = __ext_new_list(val)                \n" +
                "        else                                        \n" +
                "            val = __ext_storage.getSubStorages('KEY')\n" +
                "    }                                               \n" +
                "    else                                            \n" +
                "        val = __ext_storage.getSubStorage('KEY')    \n" +
                "}                                                   \n" +
                "val                                                 \n";

        try {
            return (Serializable)
                      eval(query.replace("KEY", key).replace("CONFIG", config));
        } finally {
            engine.put("__ext_storage", null);
        }
    }

    /**
     * {@inheritDoc}
     * @return An iterator over keys in the configuration attached to this
     * JStorage.
     */
    @Override
    public Iterator<Map.Entry<String, Serializable>> iterator() {
        // The only way to do this is to extract the configuration level level
        // from the JS engine into a hash map...
        Map<String,Serializable> map = new HashMap<String,Serializable>();

        // Install context
        engine.put("__ext_map", map);
        engine.put("__ext_storage", this);

        try {
            eval((
              "for (var key in CONFIG) {                                  \n" +
              "  if ( key.substr(0,6) == '__ext_' )                       \n" +
              "      continue                                             \n" +
              "  var val = CONFIG[key]                                    \n" +
              "  if ( typeof(val) == 'function' )                         \n" +
              "      __ext_map.put(key, val.toString())                   \n" +
              "  else if ( typeof(val) == 'object' && val != null) {      \n" +
              "      if ( val instanceof Array ) {                        \n" +
              "          if (val.length > 0 && typeof(val[0]) == 'string')\n" +
              "              __ext_map.put(key, __ext_new_list(val))      \n" +
              "           else                                            \n" +
              "              __ext_map.put(key, __ext_storage.getSubStorages(key))\n" +
              "      }                                                    \n" +
              "      else                                                 \n" +
              "          __ext_map.put(key, __ext_storage.getSubStorage(key))\n" +
              "  }                                                        \n" +
              "  else if (typeof(val) == 'number' || typeof(val) == 'boolean')\n" +
              "      __ext_map.put(key, val)                              \n" +
              "  else                                                     \n" +
              "      __ext_map.put(key, val.toString())                   \n" +
              "}                                                          \n").
              replace("CONFIG", config));
        } finally {
            // Remove context
            engine.put("__ext_map", null);
            engine.put("__ext_storage", null);
        }

        return map.entrySet().iterator();
    }

    /**
     * {@inheritDoc}
     * @param key The name of the value to remove from the storage.
     */
    @Override
    public void purge(String key) {
        eval(config+"['"+key+"'] = null");
    }

    /**
     * {@inheritDoc}
     * @return The number of kyes in the configuration.
     */
    @Override
    public int size() {
        return (int) Double.parseDouble(eval(config+".__ext_size()").toString());
    }

    /**
     * This supports sub storage, so this returns {@code true}.
     * @return True.
     */
    @Override
    public boolean supportsSubStorage() {
        return true;
    }

    /**
     * {@inheritDoc}
     * @param key The name of the sub storage.
     * @return A new storage which are a sub storage of this.
     */
    @Override
    public JStorage createSubStorage(String key) {
        return new JStorage(engine, engineManager,
                            config + "['" + key+ "']", true);
    }

    /**
     * {@inheritDoc}
     * @param key The name of the sub storage.
     * @return The sub storage associated with the key.
     */
    @Override
    public JStorage getSubStorage(String key) {
        return new JStorage(engine, engineManager,
                            config + "['" + key+ "']", false);
    }

    /**
     * {@inheritDoc}
     * @param key The key for the list of storage.
     * @param count The number of storage to create.
     * @return List containing all sub storage created.
     */
    @Override
    public List<ConfigurationStorage> createSubStorages(String key, int count) {
        List<ConfigurationStorage> storage =
                                     new ArrayList<ConfigurationStorage>(count);
        String list = config + "['" + key + "']";

        eval(list +" = new Array()");

        for (int i = 0; i < count; i++) {
            eval(list + ".push({})");
            storage.add(new JStorage(engine, engineManager,
                                      list+"[" + i + "]", false));
        }
        return storage;
    }

    /**
     * {@inheritDoc}
     * @param key The key for the list of storage.
     * @return List containing all storage associated with the key.
     */
    @Override
    public List<ConfigurationStorage> getSubStorages(String key) {
        String subConf = config + "['" + key + "']";
        int count = evalInt(subConf + ".__ext_size()");
        List<ConfigurationStorage> storages =
                                     new ArrayList<ConfigurationStorage>();
        
        for (int i = 0; i < count; i++) {
            storages.add(new JStorage(engine, engineManager,
                            subConf + "[" +i+ "]", false));
        }

        return storages;
    }

    /**
     * Evaluate a Javascript string on the storage contents. The root node of
     * the configuration is stored in the {@code config} variable.
     * 
     * @param s The script to evaluate.
     * @return The exit value of the script converted to a string.
     */
    public Object eval(String s) {
        try {
            return engine.eval(s);
        } catch (ScriptException e) {
            throw new RuntimeException("Unexpected error executing:\n"
                                       +s+"\nError: " + e.getMessage(), e);
        }
    }

    /**
     * Responsible for evaluating incoming scripts.
     * 
     * @param in The script to evaluate as a reader. 
     * @return The value returned by the script.
     */
    public Object eval(Reader in) {
        try {
            return engine.eval(in);
        } catch (ScriptException e) {
            throw new RuntimeException("Unexpected error executing "
                                       + "streamed script: " + e.getMessage(),
                                       e);
        }
    }

    /**
     * Evaluate a Javascript string on the storage contents. The root node of
     * the configuration is stored in the {@code config} variable.
     * <p/>
     * If the script evaluates to {@code null} this method returns {@code 0}.
     *
     * @param s The script to evaluate.
     * @return The exit value of the script as an integer, or 0 in case the
     *         script returns {@code null}.
     */
    private int evalInt(String s) {
        String val = eval(s).toString();
        return (int)Double.parseDouble(val == null ? "0" : val);
    }

    /**
     * Main method for starting JStorage. This JStorage comes with a interactive
     * shell.
     * @param args First argument is use4d to start storage and should point at
     * a system resource.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("You must specify a .js resource to load!");
            System.exit(1);
        } else {
            System.out.println("Welcome to the interactive JStorage shell. "
                               + "Type 'quit' to exit");
        }
        // TODO maybe used shell from Common module.
        try {
            JStorage js = new JStorage(args[0]);
            BufferedReader in = new BufferedReader(
                                           new InputStreamReader(System.in));
            String cmd;
            while (true) {
                System.out.print("> ");
                System.out.flush();
                cmd = in.readLine();
                if ("quit".equals(cmd)) {
                    break;
                } else {
                    try {
                        js.eval(cmd);
                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    /**
     * Creates a string representation of the a list, this string is in JSON
     * format.
     * @param list List to transform to JSON.
     * @return JSON string representation of input list.
     */
    private String parseList(List list) {
        StringBuilder b = new StringBuilder();

        b.append("[");
        for (Object o : list) {
            if (b.length() > 1) {
                b.append (", ");
            }
            b.append(parseObject(o));
        }
        b.append("]");
        return b.toString();
    }

    /**
     * Creates a JSON string representation of a map.
     * @param map Input map which should be transformed to a JSON string.
     * @return JSON string representatio of map.
     */
    private String parseMap(Map<String,Serializable> map) {
        StringBuilder b = new StringBuilder();

        b.append("{");

        for (Map.Entry<String,Serializable> e : map.entrySet()) {
            String key = e.getKey();
            Serializable val = e.getValue();

            if (b.length() > 1) {
                b.append (", ");
            }

            b.append("'").append(key).append("'").append(" : ");
            b.append(parseObject(val));
        }

        b.append("}");

        return b.toString();
    }

    /**
     * Create a string representation of a object.
     * @param o Object to transform to string.
     * @return String representation of object.
     */
    @SuppressWarnings({"unchecked"})
    private String parseObject(Object o) {
        if (o == null) {
            return "null";
        } else if (o instanceof String){
            return "'" + o + "'";
        } else if (o instanceof List) {
            return parseList((List)o);
        } else if (o instanceof Map) {
             return parseMap((Map<String,Serializable>)o);
        } else {
            return o.toString();
        }
    }

    /**
     * {@inheritDoc}
     * @return String representation of this object.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("var ").append(config).append(" = {\n");
        serialize("  ", buf);
        buf.append("}");
        return buf.toString();
    }

    /**
     * Transform a string, so all lines shift are followed by the same
     * indentation.
     *
     * @param prefix Prefix, used as indentation.
     * @param subject String which should be indented.
     * @return String indented after all lineshift. This does not ident the
     * first line.
     */
    private String indent(String prefix, String subject) {
        return subject.replace("\n", "\n"+prefix);
    }

    /**
     * Write this JStorage (without config header) to the StringBuilder and
     * return the StringBuilder again.
     *
     * @param prefix The prefix to use.
     * @param buf The String builder.
     * @return the string builder, containing a text representation of this
     * JStorage.
     */
    protected StringBuilder serialize(String prefix, StringBuilder buf) {
        Iterator<Map.Entry<String,Serializable>> iter = iterator();

        while (iter.hasNext()) {
            Map.Entry<String,Serializable> entry = iter.next();
            Serializable val = entry.getValue();
            String key = entry.getKey();

            if (val instanceof String) {
                buf.append(prefix)
                    .append("'").append(key).append("'")
                    .append(" : \"")
                    .append(indent(prefix, val.toString()))
                    .append("\"");
            } else if (val instanceof Double || val instanceof Boolean) {
                buf.append(prefix)
                    .append("'").append(key).append("'")
                    .append(" : ")
                    .append(indent(prefix, val.toString()));
            } else if (val instanceof JStorage) {
                buf.append(prefix)
                    .append("'").append(key).append("'")
                    .append(" : ")
                    .append("{\n");
                    ((JStorage)val).serialize(prefix + "  ", buf)
                    .append(prefix)
                    .append("}");
            } else if (val instanceof List) {
                List list = (List)val;

                buf.append(prefix)
                   .append("'").append(key).append("'")
                   .append(" : ");

                if (list.isEmpty()) {
                    buf.append("[]");
                } else if (list.get(0) instanceof String) {
                    buf.append("[");
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) {
                            buf.append(", ");
                        }
                        buf.append("\"")
                           .append(list.get(i).toString())
                           .append("\"");
                    }
                    buf.append("]");
                } else if (list.get(0) instanceof JStorage) {
                    buf.append("[\n");
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) {
                            buf.append(", {\n");
                        } else {
                            buf.append(prefix)
                               .append("  {\n");
                        }
                        ((JStorage)list.get(i)).serialize(prefix+"    ", buf)
                           .append(prefix)
                           .append("  ")
                           .append("}");
                    }
                    buf.append("\n")
                       .append(prefix)
                       .append("]");
                }
            } else {
                buf.append("Unable to serialize: ")
                   .append(val)
                   .append(" (")
                   .append(val.getClass().getName())
                   .append(")");
            }

            if (iter.hasNext()) {
                buf.append(",");
            }

            buf.append("\n");
        }
        return buf;
    }


    /**
     * Check equality with this, for local state and all storage key-value
     * pairs.
     *
     * @param o Object on which we check equality.
     * @return True <b>iff</b> object o is equal to this, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JStorage)) {
            return false;
        } else if (this == o) {
            return true;
        }

        JStorage other = (JStorage)o;

        if (size() != other.size()) {
            return false;
        }

        Iterator<Map.Entry<String,Serializable>> iter = iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Serializable> entry = iter.next();
            if (!entry.getValue().equals(other.get(entry.getKey()))) {
                return false;
            }
        }    
        return true;
    }

    /**
     * Create hashCode depended on {@code this} state and the key-value pairs in
     * this storage.
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = 47 * size();

        Iterator<Map.Entry<String,Serializable>> iter = iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Serializable> entry = iter.next();
                hashCode += entry.hashCode();
        }
        return hashCode;
    }
}