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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An {@link ObjectFilter} processing incoming payloads in some scripting
 * language supported by the Java runtime. The prime example here would be
 * JavaScript.
 * <p/>
 * The scripting environment has four global variables injected into the global
 * scope. These are;
 * {@code payload}, {@code allowPayload}, {@code feedbackMessage},
 * and {@code log}.
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
 * Finally the {@code log} variable is a handle to a Commons Logging {@code Log}
 * instance.
 * <p/>
 * <h3>Available Script Engines</h3>
 * You can find a list of supported scripting languages at
 * <a href="https://scripting.dev.java.net/">scripting.dev.java.net</a>.
 * <p/>
 * <h3>Example</h3>
 * Discard all payloads with IDs starting with {@code illegal} and sleep 1s
 * if the payload id starts with {@code sleepy}:
 * <pre>
 *    var id = payload.getId();
 *
 *    if (id.startsWith("illegal")) {
 *        feedbackMessage = "Illegal id prefix";
 *        allowPayload = false;
 *    } else if (id.startsWith("sleepy")) {
 *        // Sleepy payloads are still allowed to pass through
 *       java.lang.Thread.sleep(1000);
 *    }
 * </pre>
 */
@QAInfo(state = QAInfo.State.IN_DEVELOPMENT,
        level = QAInfo.Level.NORMAL,
        author = "mke",
        comment = "JavaDoc needed")
public class ScriptFilter extends ObjectFilterImpl {
    /** Private instance of the logger. */
    private static final Log log = LogFactory.getLog(ScriptFilter.class);

    /**
     * Whether or not to pre compile the script. In almost all cases this
     * should give a performance boost (given that the script engine supports
     * compilation. Default is {@code true}
     */
    public static final String CONF_COMPILE = "filter.script.compile";

    /**
     * The default value for the {@link #CONF_COMPILE} property.
     */
    public static final boolean DEFAULT_COMPILE = true;

    /**
     * URL to fetch the script file from. Alternatively define the script
     * inlined in your configuration by setting the {@link #CONF_SCRIPT_INLINE}
     * instead of using this property.
     * <p/>
     * Either {@code CONF_SCRIPT_URL} or {@link #CONF_SCRIPT_INLINE}
     * <i>must</i> be defined.
     */
    public static final String CONF_SCRIPT_URL = "filter.script.url";

    /**
     * Contains the entire script to be executed. Alternatively fetch
     * the script from an external resource by setting {@link #CONF_SCRIPT_URL}
     * instead of using this property.
     * <p/>
     * Either {@link #CONF_SCRIPT_URL} or {@code CONF_SCRIPT_INLINE}
     * <i>must</i> be defined.
     */
    public static final String CONF_SCRIPT_INLINE = "filter.script.inline";

    /**
     * If the scripting language can not be deducted from the URL supplied
     * in {@link #CONF_SCRIPT_URL}, or if the script is inlined via
     * {@link #CONF_SCRIPT_INLINE}, the scripting language is defined by this
     * property.
     * <p/>
     * Default is <code>js</code> for JavaScript.
     */
    public static final String CONF_SCRIPT_LANG = "filter.script.lang";

    /**
     * Default value for the {@link #CONF_SCRIPT_LANG} property.
     */
    public static final String DEFAULT_SCRIPT_LANG = "js";
    /** The script engine. */
    private ScriptEngine engine;
    /** The script engine manager. */
    private ScriptEngineManager engineManager;
    /** True if we have a copy of the compiled script. */
    private boolean compileScript;
    /** The compiled script object. */
    private CompiledScript compiledScript;
    /** A char array containing the script. */
    private char[] script;

    /**
     * Creates a script filter.
     * @param script The script.
     * @param compileScript This instance hold a copy of the compiled input.
     * @param scriptExtension The script extension.
     * @throws ScriptException If error while compiling script.
     * @throws IOException If error occur reading script.
     */
    public ScriptFilter(Reader script, boolean compileScript,
                        String scriptExtension)
                                           throws ScriptException, IOException {
        super(Configuration.newMemoryBased(CONF_FILTER_NAME,
                                      "ScriptFilter[" + scriptExtension + "]"));
        final int bufferSize = 1024;
        this.compileScript = compileScript;
        engineManager = new ScriptEngineManager();
        engine = engineManager.getEngineByExtension(scriptExtension);

        if (engine == null) {
            throw new ConfigurationException("No script engine for extension: "
                                             + scriptExtension);
        }

        // Invariant: If compileScript is true we also hold a valid
        //            CompiledScript in the compiledScript variable
        if (this.compileScript) {
            if (!(engine instanceof Compilable)) {
                log.error("Script engine does not support compilation. "
                          + "Going on in interpretive mode");
                this.compileScript = false;
            } else {
                log.info("Downloading and compiling script");
                compiledScript = ((Compilable) engine).compile(script);
            }
        } else {
            log.info("Downloading script for interpretive mode");
            CharArrayWriter out = new CharArrayWriter();

            char[] buf = new char[bufferSize];
            int len;
            while ((len = script.read(buf)) != -1) {
                out.write(buf, 0, len);
            }

            this.script = out.toCharArray();
        }
    }

    /**
     * Creates a script filter.
     * @param script The script.
     * @param compileScript This instance hold a copy of the compiled input.
     * @param scriptExtension The script extension.
     * @throws ScriptException If error while compiling script.
     * @throws IOException If error occur reading script.
     */
    public ScriptFilter(InputStream script, boolean compileScript,
                        String scriptExtension)
                                           throws ScriptException, IOException {
        this(new InputStreamReader(script), compileScript, scriptExtension);
    }

    /**
     * Creates a script filter. Script is being pre compiled.
     * @param script The script.
     * @throws ScriptException If error while compiling script.
     * @throws IOException If error occur reading script.
     */
    public ScriptFilter(Reader script) throws ScriptException, IOException {
        this(script, true, "js");
    }

    /**
     * Creates a script filter. Script is being pre compiled.
     * @param script The script.
     * @throws ScriptException If error while compiling script.
     * @throws IOException If error occur reading script.
     */
    @SuppressWarnings("unused")
    public ScriptFilter(InputStream script)
                                           throws ScriptException, IOException {
        this(new InputStreamReader(script), true, "js");
    }

    /**
     * Creates a script filer base on given configuration. Script is being pre
     * compiled.
     * @param conf The configuration.
     * @throws ScriptException  If error while compiling script.
     * @throws IOException If error while reading script.
     */
    public ScriptFilter(Configuration conf)
                                           throws ScriptException, IOException {
        this(readScript(conf), conf.getBoolean(CONF_COMPILE, DEFAULT_COMPILE),
             getScriptExtension(conf));
    }

    /**
     * Return script extension based on the {@link #CONF_SCRIPT_URL}.
     * @param conf The configuration.
     * @return Script extension.
     */
    private static String getScriptExtension(Configuration conf) {
        if (conf.valueExists(CONF_SCRIPT_URL)) {
            if (conf.valueExists(CONF_SCRIPT_LANG)) {
                return conf.getString(CONF_SCRIPT_LANG);
            }

            URL url = Resolver.getURL(conf.getString(CONF_SCRIPT_URL));
            String urlString = url.toString();

            if (urlString.indexOf('.') == -1) {
                log.warn("Unable to detect script extension from: "
                         + urlString + ". Using default: "
                         + DEFAULT_SCRIPT_LANG);
                return DEFAULT_SCRIPT_LANG;
            }

            return urlString.substring(urlString.lastIndexOf('.') + 1);
        } else if (conf.valueExists(CONF_SCRIPT_INLINE)) {
            return conf.getString(CONF_SCRIPT_LANG, DEFAULT_SCRIPT_LANG);
        } else {
            throw new ConfigurationException(String.format(
                    "No URL or inlined script defined. Please set one of the %s"
                    + " or %s properties for this filter",
                    CONF_SCRIPT_URL, CONF_SCRIPT_INLINE));
        }
    }

    /**
     * Read script specified in the configuration as {@link #CONF_SCRIPT_URL}.
     * @param conf The configuration.
     * @return Input stream with content of script.
     */
    private static InputStream readScript(Configuration conf) {
        if (!conf.valueExists(CONF_SCRIPT_URL)
            && !conf.valueExists(CONF_SCRIPT_INLINE)) {
            throw new ConfigurationException(String.format(
                    "No URL or inlined script defined. Please set one of the %s"
                    + " or %s properties for this filter",
                    CONF_SCRIPT_URL, CONF_SCRIPT_INLINE));
        }

        if (conf.valueExists(CONF_SCRIPT_URL)) {
            if (conf.valueExists(CONF_SCRIPT_INLINE)) {
                log.error(String.format(
                        "Both an inlined script and a script URL are defined."
                        + " Please use only one of %s or %s. Using script from"
                        + " %s",
                        CONF_SCRIPT_URL, CONF_SCRIPT_INLINE,
                        conf.getString(CONF_SCRIPT_URL)));
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
                throw new ConfigurationException(String.format(
                        "Malformed URL in %s: %s",
                        CONF_SCRIPT_URL, e.getMessage()), e);
            } catch (IOException e) {
                throw new ConfigurationException(String.format(
                        "Unable to read script data from URL '%s': %s",
                        conf.getString(CONF_SCRIPT_URL), e.getMessage()), e);
            }
        } else {
            assert conf.valueExists(CONF_SCRIPT_INLINE);
            log.info("Using inlined script");
            return new ByteArrayInputStream(
                                 conf.getString(CONF_SCRIPT_INLINE).getBytes());
        }
    }

    @Override
    protected final boolean processPayload(Payload payload)
                                                       throws PayloadException {
        long time = System.nanoTime();
        final double oneSecond = 1000000D;

        // Put the payload into the engine so the script can access it
        engine.put("payload", payload);
        engine.put("allowPayload", Boolean.TRUE);
        engine.put("feedbackMessage", null);
        engine.put("log", log);

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
                throw new PayloadException(
                        "Script returned null. It must return a boolean");
            }

            String message = (String) engine.get("feedbackMessage");
            String mes = (time / oneSecond) + "ms" + (message == null ? "" : " (" + message + ")");
            if (result) {
                Logging.logProcess("ScriptFilter", "Processed in " + mes, Logging.LogLevel.DEBUG, payload);
                log.debug("Processed " + payload.getId() + " in " + mes);
            } else {
                Logging.logProcess("ScriptFilter", "Discarded in " + mes, Logging.LogLevel.DEBUG, payload);
                log.debug("Discarded " + payload.getId() + " in " + mes);
            }

            return result;
        } catch (ClassCastException e) {
            throw new PayloadException(
                    "Script did not return a boolean, but a "
                    + engine.get("allowPayload"));
        }
    }
}
