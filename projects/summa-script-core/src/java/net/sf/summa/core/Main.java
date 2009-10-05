package net.sf.summa.core;

import java.util.List;
import java.util.LinkedList;
import java.io.IOException;
import java.io.FileInputStream;

/**
 * FIXME: Missing class docs for net.sf.summa.core.Main
 *
 * @author mke
 * @since Sep 24, 2009
 */
public class Main {

    public static void main(String[] args) {
        List<Script> scripts = null;
        try {
            scripts = parseArgs(args);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }

        net.sf.summa.core.Template<ScriptRunner> t =
                        net.sf.summa.core.Template.forClass(ScriptRunner.class);

        for (Script s : scripts) {
            System.err.println("Running script: " + s);
            t.put("script", s);
            ScriptRunner runner = t.create();
            runner.run();
            if (runner.getExitCode() != 0) {
                System.err.println("Error running script: " + s);
                System.exit(runner.getExitCode());
            }
        }
    }

    private static List<Script> parseArgs(String[] args) throws IOException {
        Script script =
                new Script("test.js", "js", new FileInputStream("test.js"));
        List<Script> scripts = new LinkedList<Script>();
        scripts.add(script);
        return scripts;
    }

}
