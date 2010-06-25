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
package dk.statsbiblioteket.summa.control.service;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.InvalidServiceStateException;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.ClientConnection;

import javax.script.*;
import java.rmi.RemoteException;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link Service} implementation delegating all work to an underlying
 * {@link ScriptEngine}. Scripts can either be inlined in the configuration
 * by setting the {@link #CONF_SCRIPT_INLINE} property to contain the entire
 * script, or by setting the script as an external resource via the
 * {@link #CONF_SCRIPT_URL} property.
 * <p/>
 * The default scripting language is Javascript, but this can be tweaked to any
 * script engine supported by the runtime by setting the
 * {@link #CONF_SCRIPT_LANG} property.
 * <p/>
 * The scripting environment has two variables injected into it, namely
 * {@code stopped} and {@code log}.
 * <p/>
 * The boolean value {@code stopped} indicates whether the script should stop
 * executing and is usually checked in a looping condition.
 * <p/>
 * The {@code log} variable is a reference to a Commons Logging {@code Log}
 * instance.
 * <p/>
 * <h3>Available Script Engines<h3/>
 * You can find a list of supported scripting languages at
 * <a href="https://scripting.dev.java.net/">scripting.dev.java.net</a>.
 * <p/>
 * <h3>Example</h3>
 * <pre>
 *     while (!stopped) {
 *         log.debug("Wee another iteration!");
 *         java.lang.Thread.sleep(1000);
 *     }
 * </pre>
 */
public class ScriptService extends ServiceBase {

    private static final Log log = LogFactory.getLog(ScriptService.class);

    /**
     * Whether or not to precompile the script. In almost all cases this
     * should give a performance boost (gievn that the script engine supports
     * compilation. Default is {@code true}
     */
    public static final String CONF_COMPILE =
                                    "summa.control.service.compilescript";

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
    public static final String CONF_SCRIPT_URL =
                                    "summa.control.service.scripturl";

    /**
     * Contains the entire script to be executed. Alternatively fetch
     * the script from an external resource by setting {@link #CONF_SCRIPT_URL}
     * instead of using this property.
     * <p/>
     * Either {@link #CONF_SCRIPT_URL} or {@link #CONF_SCRIPT_INLINE}
     * <i>must</i> be defined.
     */
    public static final String CONF_SCRIPT_INLINE =
                                       "summa.control.service.inlinescript";

    /**
     * If the scripting language can not be deducted from the URL supplied
     * in {@link #CONF_SCRIPT_URL}, or if the script is inlined via
     * {@link #CONF_SCRIPT_INLINE}, the scripting language is defined by this
     * property.
     * <p/>
     * Default is <code>js</code> for Javascript.
     */
    public static final String CONF_SCRIPT_LANG =
                                          "summa.control.service.scriptlang";

    /**
     * Default value for the {@link #CONF_SCRIPT_LANG} property.
     */
    public static final String DEFAULT_SCRIPT_LANG = "js";

    private ScriptEngine engine;
    private ScriptEngineManager engineManager;
    private boolean compileScript;
    private CompiledScript compiledScript;
    private char[] script;
    private String scriptExtension;
    private ScriptRunner scriptRunner;

    public ScriptService(Configuration conf) throws RemoteException {
        super(conf);

        setStatus(Status.CODE.startingUp, "Configuring script service",
                  Logging.LogLevel.INFO);

        Reader script = new InputStreamReader(readScript(conf));

        this.compileScript = conf.getBoolean(CONF_COMPILE, DEFAULT_COMPILE);
        scriptExtension = conf.getString(CONF_SCRIPT_LANG, DEFAULT_SCRIPT_LANG);

        engineManager = new ScriptEngineManager();
        engine = engineManager.getEngineByExtension(scriptExtension);

        if (engine == null) {
            throw new ConfigurationException("No script engine for extension: "
                                             + scriptExtension);
        }

        // Invariant: If compileScript is true we also hold a valid
        //            CompiledScript in the compiledScript variable
        try {
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
        } catch (ScriptException e) {
            throw new ConfigurationException("Unable to parse or compile "
                                             + "script: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ConfigurationException("I/O error loading script: "
                                             + e.getMessage(), e);
        }

        scriptRunner = new ScriptRunner();

        setStatus(Status.CODE.constructed, "Setup complete",
                  Logging.LogLevel.INFO);
    }

    public void start() throws RemoteException {
        if (scriptRunner.isRunning()) {
            throw new InvalidServiceStateException(
                    getClientId(), getId(), "start", "Already running");
        }

        setStatusRunning("Running script");
        scriptRunner.runInThread();
        setStatusIdle();
    }

    public void stop() throws RemoteException {
        if (!scriptRunner.isRunning()) {
            throw new InvalidServiceStateException(
                    getClientId(), getId(), "stop", "Not running");
        }

        setStatus(Status.CODE.stopping, "Stopping script",
                  Logging.LogLevel.INFO);
        scriptRunner.stop();
    }



    private static InputStream readScript(Configuration conf) {
        if (!conf.valueExists(CONF_SCRIPT_URL)
            && !conf.valueExists(CONF_SCRIPT_INLINE)) {
            throw new ConfigurationException("No URL or inlined script defined."
                                             + " Please set one of the "
                                             + CONF_SCRIPT_URL + " or "
                                             + CONF_SCRIPT_INLINE
                                             + " properties for this service");
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
                throw new ConfigurationException(
                   "Malformed URL in " + CONF_SCRIPT_URL + ":" + e.getMessage(),
                   e);
            } catch (IOException e) {
                throw new ConfigurationException(
                                  "Unable to read script data from URL "
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

    private class ScriptRunner implements Runnable {

        private Thread thread;

        public synchronized void runInThread() {
            thread = new Thread(this, "ScriptRunner");
            thread.start();
        }

        public synchronized boolean isRunning() {
            return thread != null;
        }

        public synchronized void stop() {
            engine.put("stopped", true);
            if (thread == null) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for thread "
                             + thread.getName());
                }
            }
            thread = null;
            setStatus(Status.CODE.stopped,
                      "Script stopped" ,
                      Logging.LogLevel.INFO);
        }

        public void run() {
            long time = System.nanoTime();

            // Set up environment
            engine.put("stopped", false);
            engine.put("log", log);

            if (compiledScript != null) {
                log.debug("Running compiled script");
                try {
                    compiledScript.eval();
                } catch (ScriptException e) {
                    setStatus(Status.CODE.crashed,
                              "Error executing compiled script",
                              Logging.LogLevel.ERROR);
                    throw new RuntimeException("Error executing script: "
                                               + e.getMessage(), e);
                }

            } else {
                log.debug("Running interpreted script");
                try {
                    engine.eval(new CharArrayReader(script));
                } catch (ScriptException e) {
                    setStatus(Status.CODE.crashed,
                              "Error executing interpreted script",
                              Logging.LogLevel.ERROR);
                    throw new RuntimeException("Error executing script: "
                                               + e.getMessage(), e);
                }

            }

            // Calc processing time in ns
            time = System.nanoTime() - time;
            log.debug("Script exited after " + time/1000000D + "ms");
        }
    }
}

