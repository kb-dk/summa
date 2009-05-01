package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.Serializable;
import java.io.IOException;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 *
 */
public class JStorage implements ConfigurationStorage {

    private ScriptEngineManager engineManager;
    private ScriptEngine engine;
    private String config;

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
        return eval(config+"['" + key + "']");
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

    private String eval(String s) {
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

    private int evalInt(String s) {
        return Integer.parseInt(eval(s));        
    }
}
