package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorageException;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.*;
import java.util.*;
import java.net.URL;

/**
 *
 */
public class JStorage implements ConfigurationStorage {

    private ScriptEngineManager engineManager;
    private ScriptEngine engine;
    private String config;

    public JStorage (File file) throws IOException {
        this();

        eval(new FileReader(file));        
    }

    public JStorage (String resource) throws IOException {
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

    public JStorage(URL url) throws IOException {
        this();
        try {
            eval(new InputStreamReader(url.openStream()));
        } catch (Exception e) {
            throw new ConfigurationStorageException("Error reading " + url
                                                    + ": " + e.getMessage(), e);
        }
    }

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

    public JStorage(Configuration conf) {
        this();

        // Magic trick to import the given conf
        new Configuration(this).importConfiguration(conf);
    }

    public JStorage() {
        init();
        config = "config";


        // Create new empty config
        eval(config + " = {}");
    }

    /**
     * Used for creating substorages
     * @param eng
     * @param engMan
     * @param conf
     */
    private JStorage(ScriptEngine eng, ScriptEngineManager engMan,
                     String conf, boolean createNew){
        engine = eng;
        engineManager = engMan;
        config = conf;

        if (createNew) {
            eval(config + " = {}");
        }
    }

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
        "  var len = this.length ? --this.length : -1;   \n" +
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
    }

    public void put(String key, Serializable value) throws IOException {
        eval(config+"['"+key+"'] = '" + value.toString() + "'");
    }

    public Serializable get(String key) throws IOException {
        final String query =
                "var val = " + config+"['%s']     \n"+
                "if( typeof(val) == 'function' ) {\n" +
                "    val = val()                  \n" +
                "}                                \n" +
                "val                              \n";

        return  eval(String.format(query, key));
    }

    public Iterator<Map.Entry<String, Serializable>> iterator() throws IOException {
        // The only way to do this is to extract the configuration level level
        // from the JS engine into a hashmap...
        Map<String,Serializable> map = new HashMap<String,Serializable>();
        engine.put("__ext_map", map);
        eval(("for (var key in CONFIG) {                 \n" +
              "  if ( key.substr(0,6) == '__ext_' )      \n" +
              "      continue                            \n" +
              "  if ( typeof(CONFIG[key]) == 'function' )\n" +
              "      __ext_map.put(key, CONFIG[key]())   \n" +
              "  else                                    \n" +
              "      __ext_map.put(key, CONFIG[key])     \n" +
              "}                                         \n").
              replace("CONFIG", config));
        eval("__ext_map = null");

        return map.entrySet().iterator();
    }

    public void purge(String key) throws IOException {
        eval(config+"['"+key+"'] = null");
    }

    public int size() throws IOException {
        return (int) Double.parseDouble(eval(config+".__ext_size()"));
    }

    public boolean supportsSubStorage() throws IOException {
        return true;
    }

    public JStorage createSubStorage(String key) throws IOException {
        return new JStorage(engine, engineManager,
                            config + "['" + key+ "']", true);
    }

    public JStorage getSubStorage(String key) throws IOException {
        return new JStorage(engine, engineManager,
                            config + "['" + key+ "']", false);
    }

    public List<ConfigurationStorage> createSubStorages(String key, int count) throws IOException {
        List<ConfigurationStorage> storages =
                                     new ArrayList<ConfigurationStorage>(count);
        String list = config + "[" + key + "]";

        eval(list +" = new Array()");

        for (int i = 0; i < count; i++) {
            eval(list + ".push({})");
            storages.add(new JStorage(engine, engineManager,
                                      list+"[" + i + "]", false));
        }

        return storages;
    }

    public List<ConfigurationStorage> getSubStorages(String key) throws IOException {
        int count = evalInt(config + "[" + key + "].__ext_size()");
        JStorage container = getSubStorage(key);
        List<ConfigurationStorage> storages =
                                     new ArrayList<ConfigurationStorage>(count);

        for (int i = 0; i < count; i++) {
            storages.add(container.getSubStorage("" + i));
        }

        return storages;
    }

    /**
     * Evaluate a Javascript string on the storage contents. The root node of
     * the configuration is stored in the {@code config} variable.
     * @param s the script to evaluate
     * @return the exit value of the script converted to a string
     */
    public String eval(String s) {
        try {
            Object val = engine.eval(s);
            if (val != null) {
                return val.toString();
            }
            return null;
        } catch (ScriptException e) {
            throw new RuntimeException("Unexpected error executing\n:"
                                       +s+"\nError: " + e.getMessage(), e);
        }
    }

    public String eval(Reader in) {
        try {
            Object val = engine.eval(in);
            if (val != null) {
                return val.toString();
            }
            return null;
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
     * If the script evaluates to {@code null} this method returns {@code 0}
     * @param s the script to evaluate
     * @return the exit value of the script as an integer, or 0 in case the
     *         script returns {@code null}
     */
    private int evalInt(String s) {
        String val = eval(s);
        return Integer.parseInt(val == null ? "0" : val);
    }
}
