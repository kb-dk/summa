package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Streams;

import javax.script.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An {@link ObjectFilter} processing incoming payloads in some scripting
 * language supported by the Java runtime. The prime example here would be
 * Javascript.
 */
public class ScriptFilter extends ObjectFilterImpl {

    private static final Log log = LogFactory.getLog(ScriptFilter.class);

    public static final String CONF_COMPILE = "filter.script.compile";
    public static final boolean DEFAULT_COMPILE = true;

    public static final String CONF_SCRIPT_URL = "filter.script.url";

    private ScriptEngine engine;
    private ScriptEngineManager engineManager;
    private boolean compileScript;
    private CompiledScript compiledScript;
    private char[] script;

    public ScriptFilter(Reader script,
                        boolean compileScript,
                        String scriptExtension)
                                           throws ScriptException, IOException {
        super(Configuration.newMemoryBased(CONF_FILTER_NAME,
                                           "ScriptFilter["+scriptExtension+"]"));
        this.compileScript = compileScript;
        engineManager = new ScriptEngineManager();
        engine = engineManager.getEngineByExtension(scriptExtension);

        if (engine == null) {
            throw new ConfigurationException("No script engine for extension: "
                                             + scriptExtension);
        }

        // Invariant: If compileScript is true we also hold a valid
        //            CompiledScript in the compiledScript variable
        if (compileScript) {
            if (!(engine instanceof Compilable)) {
                log.error("Script engine does not support compilation. "
                          + "Going on in interpretive mode");
                compileScript = false;
            } else {
                log.info("Downloading and compiling script");
                compiledScript = ((Compilable)engine).compile(script);
            }
        } else {
            log.info("Downloading script for interpretive mode");
            CharArrayWriter out = new CharArrayWriter();

            char[] buf = new char[1024];
            int len;
            while ((len = script.read(buf)) != -1) {
                out.write(buf, 0, len);
            }

            this.script = out.toCharArray();
        }
    }

    public ScriptFilter(InputStream script,
                        boolean compileScript,
                        String scriptExtension)
                                           throws ScriptException, IOException {
        this(new InputStreamReader(script), compileScript, scriptExtension);
    }

    public ScriptFilter(Reader script) throws ScriptException, IOException {
        this(script, true, "js");
    }

    public ScriptFilter(InputStream script) throws ScriptException, IOException {
        this(new InputStreamReader(script), true, "js");
    }

    public ScriptFilter(Configuration conf) throws ScriptException, IOException {
        this(readScript(conf),
             conf.getBoolean(CONF_COMPILE, true),
             getScriptExtension(conf));
    }

    private static String getScriptExtension(Configuration conf) {
        if (!conf.valueExists(CONF_SCRIPT_URL)) {
            throw new ConfigurationException("No script URL defined in "
                                             + "configuration. Please set the "
                                             + CONF_SCRIPT_URL
                                             + " property for this filter");
        }

        try {
            URL url = new URL(conf.getString(CONF_SCRIPT_URL));
            String urlString = url.toString();

            if (urlString.indexOf('.') == -1) {
                throw new ConfigurationException("Unable to detect script "
                                                 + "extension from: "
                                                 + urlString);
            }

            return urlString.substring(urlString.lastIndexOf('.'));

        } catch (MalformedURLException e) {
            throw new ConfigurationException("Malformed URL in "
                                             + CONF_SCRIPT_URL
                                             + ":" + e.getMessage(), e);
        }
    }

    private static InputStream readScript(Configuration conf) {
        if (!conf.valueExists(CONF_SCRIPT_URL)) {
            throw new ConfigurationException("No script URL defined in "
                                             + "configuration. Please set the "
                                             + CONF_SCRIPT_URL
                                             + " property for this filter");
        }

        try {
            URL url = new URL(conf.getString(CONF_SCRIPT_URL));
            return url.openStream();
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Malformed URL in "
                                             + CONF_SCRIPT_URL
                                             + ":" + e.getMessage(), e);
        } catch (IOException e) {
            throw new ConfigurationException("Unable to read script data from"
                                             + " URL "
                                             + conf.getString(CONF_SCRIPT_URL)
                                             + ": " + e.getMessage(), e);
        }
    }

    protected boolean processPayload(Payload payload) throws PayloadException {
        // Put the payload into the engine so the script can access it
        engine.put("payload", payload);
        engine.put("returnValue", Boolean.TRUE);

        if (compiledScript != null) {
            log.debug("Processing " + payload.getId()
                      + " with compiled script");
            try {
                compiledScript.eval();
            } catch (ScriptException e) {
                throw new PayloadException("Error evaluating compiled script: "
                                           + e.getMessage(), e);
            }

        } else {
            log.debug("Processing " + payload.getId()
                      + " with interpreted script");
            try {
                engine.eval(new CharArrayReader(script));
            } catch (ScriptException e) {
                throw new PayloadException("Error evaluating interpreted "
                                           + "script: " + e.getMessage(), e);
            }

        }

        try {
                return (Boolean) engine.get("returnValue");
            } catch (ClassCastException e) {
                throw new PayloadException("Script did not return a boolean, "
                                            + "but a "
                                            + engine.get("returnValue"));
            } catch (NullPointerException e) {
                throw new PayloadException("Script returned null. It must "
                                           + "return a boolean");
        }
    }
}
