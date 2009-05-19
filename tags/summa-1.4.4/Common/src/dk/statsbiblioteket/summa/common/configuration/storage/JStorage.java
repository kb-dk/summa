package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorageException;
import dk.statsbiblioteket.util.Strings;

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

        if (Boolean.parseBoolean(eval(config + " == null").toString())) {
            throw new RuntimeException("Can not create sub storage of a "
                                       + "non-existing sub configuration: "
                                       + config);
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

    public void put(String key, Serializable value) throws IOException {
        if (value instanceof List) {
            List list = (List)value;
            if (list.isEmpty()) {
                eval(config+"['"+key+"'] = []");
            } else if (list.get(0) instanceof String) {
                eval(config+"['"+key+"'] = "+ asStringList(list));
            } else {
                eval(config+"['"+key+"'] = "+ asRawList(list));
            }
        } else if (value instanceof String) {
            eval(config+"['"+key+"'] = '" + value.toString() + "'");
        } else {
            eval(config+"['"+key+"'] = " + value.toString());
        }
    }

    public Serializable get(String key) throws IOException {
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
            return  (Serializable)
                      eval(query.replace("KEY", key).replace("CONFIG", config));
        } finally {
            engine.put("__ext_storage", null);
        }
    }

    public Iterator<Map.Entry<String, Serializable>> iterator() throws IOException {
        // The only way to do this is to extract the configuration level level
        // from the JS engine into a hashmap...
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

    public void purge(String key) throws IOException {
        eval(config+"['"+key+"'] = null");
    }

    public int size() throws IOException {
        return (int) Double.parseDouble(eval(config+".__ext_size()").toString());
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
        String list = config + "['" + key + "']";

        eval(list +" = new Array()");

        for (int i = 0; i < count; i++) {
            eval(list + ".push({})");
            storages.add(new JStorage(engine, engineManager,
                                      list+"[" + i + "]", false));
        }

        return storages;
    }

    public List<ConfigurationStorage> getSubStorages(String key) throws IOException {
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
     * @param s the script to evaluate
     * @return the exit value of the script converted to a string
     */
    public Object eval(String s) {
        try {
            Object val = engine.eval(s);
            if (val != null) {
                return val;
            }
            return null;
        } catch (ScriptException e) {
            throw new RuntimeException("Unexpected error executing:\n"
                                       +s+"\nError: " + e.getMessage(), e);
        }
    }

    public Object eval(Reader in) {
        try {
            Object val = engine.eval(in);
            if (val != null) {
                return val;
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
        String val = eval(s).toString();
        return (int)Double.parseDouble(val == null ? "0" : val);
    }

    public static void main (String[] args) {
        if (args.length == 0) {
            System.err.println("You must specify a .js resource to load!");
            System.exit(1);
        } else {
            System.out.println("Welcome to the interactive JStorage shell. "
                               + "Type 'quit' to exit");
        }

        try {
            JStorage js = new JStorage(args[0]);
            BufferedReader in = new BufferedReader(
                                           new InputStreamReader(System.in));
            String cmd = "";
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

    private String asStringList(List list) {
        StringBuilder b = new StringBuilder();

        b.append("[");

        for (Object o : list) {

            if (b.length() > 1) {
                b.append (", ");
            }

            if (o == null) {
                b.append("null");
            } else {
                b.append("'").append(o.toString()).append("'");
            }

        }

        b.append("]");

        return b.toString();
    }

    private String asRawList(List list) {
        StringBuilder b = new StringBuilder();

        b.append("[");

        for (Object o : list) {

            if (b.length() > 1) {
                b.append (", ");
            }

            if (o == null) {
                b.append("null");
            } else {
                b.append(o.toString());
            }

        }

        b.append("]");

        return b.toString();
    }

    public String toString () {
        StringBuilder buf = new StringBuilder();
        buf.append(config + " = {\n");
        serialize("  ", buf);
        buf.append("}");
        return buf.toString();
    }

    private String indent(String prefix, String subject) {
        return subject.replace("\n", "\n"+prefix);
    }

    /**
     * Write this JStorage (without config header) to the StringBuilder and
     * return the StringBuilder again
     * @param prefix
     * @param buf
     * @return
     */
    protected StringBuilder serialize(String prefix, StringBuilder buf) {
        try {
            Iterator<Map.Entry<String,Serializable>> iter = iterator();

            while (iter.hasNext()) {
                Map.Entry<String,Serializable> entry = iter.next();
                Serializable val = entry.getValue();
                String key = entry.getKey();

                if (val instanceof String) {
                    buf.append(prefix)
                        .append(key)
                        .append(" : \"")
                        .append(indent(prefix, val.toString()))
                        .append("\"");
                } else if (val instanceof Double || val instanceof Boolean) {
                    buf.append(prefix)
                        .append(key)
                        .append(" : ")
                        .append(indent(prefix, val.toString()));
                } else if (val instanceof JStorage) {
                    buf.append(prefix)
                        .append(key)
                        .append(" : ")
                        .append("{\n");
                        ((JStorage)val).serialize(prefix + "  ", buf)
                        .append(prefix)
                        .append("}");
                } else if (val instanceof List) {
                    List list = (List)val;

                    buf.append(prefix)
                       .append(key)
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
                                buf.append(", ");
                            } else {
                                buf.append(prefix);
                                buf.append("  ");
                            }
                            buf.append("{\n")
                               .append(prefix);
                            ((JStorage)list.get(i)).serialize(prefix+"  ", buf)
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

        } catch (IOException e) {
            buf.append("JStorage serialization error: ")
               .append(e.getMessage())
               .append("\n")
               .append(Strings.getStackTrace(e));
        }

        return buf;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JStorage)) {
            return false;
        } else if (this == o) {
            return true;
        }

        JStorage other = (JStorage)o;

        try {
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
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}
