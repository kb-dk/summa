package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorageException;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.*;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;

/**
 *
 */
public class JStorage implements ConfigurationStorage {

    private ScriptEngineManager engineManager;
    private ScriptEngine engine;
    private String config;

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
        throw new UnsupportedOperationException();
    }

    public void purge(String key) throws IOException {
        eval(config+"['"+key+"'] = null");
    }

    public int size() throws IOException {
        return Integer.parseInt(eval(config+".length"));
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
        JStorage container = createSubStorage(key);

        for (int i = 0; i < count; i++) {
            storages.add(container.createSubStorage("" + i));
        }

        return storages;
    }

    public List<ConfigurationStorage> getSubStorages(String key) throws IOException {
        int count = evalInt(config + "[" + key + "].length");
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
            throw new RuntimeException("Unexpected error executing: '" +s+"'");
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
                                       + "streamed script");
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
