package net.sf.summa.core;

import javax.script.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * FIXME: Missing class docs for net.sf.summa.core.ScriptCore
 *
 * @author mke
 * @since Sep 24, 2009
 */
public class ScriptCore implements Runnable {

    private ScriptEngineManager scriptMan;

    @Property(name="script", type=Script.class, mandatory=true)
    private Script script;
    private int exitCode;

    public static class Template extends ServiceTemplate<ScriptCore> {
        public Template() {
            super(ScriptCore.class, "ScriptCore", "Default");
        }
    }

    public ScriptCore() {
        scriptMan = new ScriptEngineManager();
        exitCode = -1;
    }

    public synchronized void run() {
        ScriptEngine engine =
                scriptMan.getEngineByExtension(script.getExtension());

        if (engine == null) {
            exitCode = -2;
            return;
        }

        prepareBindings(engine);
        try {
            engine.eval(new InputStreamReader(script.getSourceCode()));
        } catch (ScriptException e) {
            e.printStackTrace();
            exitCode = 1;
        }

        if (exitCode == -1) exitCode = 0;
    }

    private void prepareBindings(ScriptEngine engine) {
        engine.put("Jumpr", ServiceTemplate.getServices());
        engine.put("foo", 23);

    }

    public synchronized int getExitCode() {
        return exitCode;
    }
}
