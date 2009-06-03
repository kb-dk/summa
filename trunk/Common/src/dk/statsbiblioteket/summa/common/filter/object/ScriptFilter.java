package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;

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
 * <p/>
 * The scripting environment has three global varibles injected into the global
 * scope. These are;
 * {@code payload}, {@code allowPayload}, and {@code feedbackMessage}.
 * <p/>
 * The {@code payload} variable holds the raw {@link Payload} object to be
 * processed. The two other variables {@code allowPayload} and
 * {@code feedbackMessage} basically holds the return values of the script.
 * Most important is {@code allowPayload} - set this to a boolean value
 * indicating whether this payload should dropped from the filter chain or not.
 * If {@code allowPayload} is {@code false} it will be dropped from the
 * processing chain. If it is {@code true} it will be kept and passed on for
 * further processing. The default value for {@code allowPayload} is
 * {@code true}.
 * <p/>
 * The {@code feedbackMessage} is mainly for debugging purposes. It may
 * optionally be set to a string which will be printed in the log when the
 * script completes.
 * <p/>
 * You can find a list of supported scripting languages at
 * <a href="https://scripting.dev.java.net/">scripting.dev.java.net</a>.
 */
public class ScriptFilter extends ObjectFilterImpl {

    private static final Log log = LogFactory.getLog(ScriptFilter.class);

    /**
     * Whether or not to precompile the script. In almost all cases this
     * should give a performance boost (gievn that the script engine supports
     * compilation. Default is {@code true}
     */
    public static final String CONF_COMPILE = "filter.script.compile";

    /**
     * The default value for the {@link #CONF_COMPILE} property
     */
    public static final boolean DEFAULT_COMPILE = true;

    /**
     * URL to fetch the script file from. Alternatively define the script
     * inlined in your configuration by setting the {@link #CONF_SCRIPT_INLINE}
     * instead of using this property.
     * <p/>
     * Either {@link #CONF_SCRIPT_URL} or {@link #CONF_SCRIPT_INLINE}
     * <i>must</i> be defined.
     */
    public static final String CONF_SCRIPT_URL = "filter.script.url";

    /**
     * Contains the entire script to be executed. Alternatively fetch
     * the script from an external resource by setting {@link #CONF_SCRIPT_URL}
     * instead of using this property.
     * <p/>
     * Either {@link #CONF_SCRIPT_URL} or {@link #CONF_SCRIPT_INLINE}
     * <i>must</i> be defined.
     */
    public static final String CONF_SCRIPT_INLINE = "filter.script.inline";

    /**
     * If the scripting language can not be deducted from the URL supplied
     * in {@link #CONF_SCRIPT_URL}, or if the script is inlined via
     * {@link #CONF_SCRIPT_INLINE}, the scripting language is defined by this
     * property.
     * <p/>
     * Default is <code>js</code> for Javascript.
     */
    public static final String CONF_SCRIPT_LANG = "filter.script.lang";

    /**
     * Default value for the {@link #CONF_SCRIPT_LANG} property.
     */
    public static final String DEFAULT_SCRIPT_LANG = "js";

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
        if (conf.valueExists(CONF_SCRIPT_URL)) {
            if (conf.valueExists(CONF_SCRIPT_LANG)) {
                return conf.getString(CONF_SCRIPT_LANG);
            }

            try {
                URL url = new URL(conf.getString(CONF_SCRIPT_URL));
                String urlString = url.toString();

                if (urlString.indexOf('.') == -1) {
                    log.warn("Unable to detect script extension from: "
                              + urlString + ". Using default: "
                              + DEFAULT_SCRIPT_LANG);
                    return DEFAULT_SCRIPT_LANG;
                }

                return urlString.substring(urlString.lastIndexOf('.'));

            } catch (MalformedURLException e) {
                throw new ConfigurationException("Malformed URL in "
                                                 + CONF_SCRIPT_URL
                                                 + ":" + e.getMessage(), e);
            }
        } else if (conf.valueExists(CONF_SCRIPT_INLINE)) {
            return conf.getString(CONF_SCRIPT_LANG, DEFAULT_SCRIPT_LANG);
        } else {
            throw new ConfigurationException("No URL or inlined script defined."
                                             + " Please set one of the "
                                             + CONF_SCRIPT_URL + " or "
                                             + CONF_SCRIPT_INLINE
                                             + " properties for this filter");
        }        
    }

    private static InputStream readScript(Configuration conf) {
        if (!conf.valueExists(CONF_SCRIPT_URL)
            && !conf.valueExists(CONF_SCRIPT_INLINE)) {
            throw new ConfigurationException("No URL or inlined script defined." +
                                             " Please set one of the "
                                             + CONF_SCRIPT_URL + " or "
                                             + CONF_SCRIPT_INLINE
                                             + " properties for this filter");
        }

        if (conf.valueExists(CONF_SCRIPT_URL)) {
            if (conf.valueExists(CONF_SCRIPT_INLINE)) {
                log.error("Both an inlined script and a script URL are defined."
                          + " Please use only one of " + CONF_SCRIPT_URL
                          + " or " + CONF_SCRIPT_INLINE + ". Using script" +
                          "from " + conf.getString(CONF_SCRIPT_URL));
            }

            try {
                log.info("Reading script from "
                         + conf.getString(CONF_SCRIPT_URL));
                URL url = Resolver.getURL(conf.getString(CONF_SCRIPT_URL));

                if (url == null) {
                    throw new ConfigurationException(
                                             "Unable to locate script: "
                                             + conf.getString(CONF_SCRIPT_URL));
                } else {
                    log.debug("Script resolved to: " + url);
                }

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
        } else {
            assert conf.valueExists(CONF_SCRIPT_INLINE);
            log.info("Using inlined script");
            return new ByteArrayInputStream(
                                 conf.getString(CONF_SCRIPT_INLINE).getBytes());
        }
    }

    protected boolean processPayload(Payload payload) throws PayloadException {
        long time = System.nanoTime();

        // Put the payload into the engine so the script can access it
        engine.put("payload", payload);
        engine.put("allowPayload", Boolean.TRUE);
        engine.put("feedbackMessage", null);

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

        // Calc processing time in ns
        time = System.nanoTime() - time;

        try {
            Boolean result = (Boolean) engine.get("allowPayload");

            if (result == null) {
                throw new PayloadException("Script returned null. It must "
                                           + "return a boolean");
            }

            String message = (String) engine.get("feedbackMessage");
            if (result) {
                log.debug("Processed " + payload.getId() + " in "
                          + (time/1000000D) + "ms"
                          + (message == null ? "" : (": " + message)));
            } else {
                log.debug("Discarded " + payload.getId() + " in "
                          + (time/1000000D) + "ms"
                          + (message == null ? "" : (": " + message)));
            }

            return result;
            
        } catch (ClassCastException e) {
            throw new PayloadException("Script did not return a boolean, "
                                       + "but a "
                                       + engine.get("allowPayload"));
        }
    }
}
