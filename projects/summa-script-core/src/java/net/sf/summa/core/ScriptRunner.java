package net.sf.summa.core;

import javax.script.*;
import java.util.List;
import java.io.InputStreamReader;

/**
 * FIXME: Missing class docs for net.sf.summa.core.ScriptRunner
 *
 * @author mke
 * @since Sep 24, 2009
 */
public class ScriptRunner implements Runnable {

    @Property(name="script", type=Script.class, mandatory=true)
    private Script script;

    private int exitCode;

    public static class Template extends ServiceTemplate<ScriptRunner> {
        public Template() {
            super(ScriptRunner.class, "ScriptRunner", "Default");
        }
    }

    public ScriptRunner() {
        exitCode = -1;
    }

    public synchronized void run() {
        ScriptDriver driver = ScriptDriver.getDriver(script);

        if (driver == null) {
            exitCode = -2;
            return;
        }

        ScriptEngine engine = driver.createEngine();
        try {
            engine.eval(new InputStreamReader(script.getSourceCode()));
        } catch (ScriptException e) {
            e.printStackTrace();
            exitCode = 1;
        }

        if (exitCode == -1) exitCode = 0;
    }

    public synchronized int getExitCode() {
        return exitCode;
    }
}
