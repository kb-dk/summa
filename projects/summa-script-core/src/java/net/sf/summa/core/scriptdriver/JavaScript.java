package net.sf.summa.core.scriptdriver;

import net.sf.summa.core.ScriptDriver;
import net.sf.summa.core.Script;
import net.sf.summa.core.ServiceTemplate;
import net.sf.summa.core.ScriptDriverError;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
import java.io.InputStreamReader;
import java.io.InputStream;

/**
 * FIXME: Missing class docs for net.sf.summa.core.scriptdriver.JavaScript
 *
 * @author mke
 * @since Oct 5, 2009
 */
public class JavaScript extends ScriptDriver {
    private ScriptEngineManager scriptMan;
    private Map<String,Map<String,ServiceTemplate>> services;

    public JavaScript() {
        scriptMan = new ScriptEngineManager();
        services = ServiceTemplate.getServices();
    }

    public boolean accepts(Script script) {
        return "js".equals(script.getExtension());
    }

    public ScriptEngine createEngine() {

        ScriptEngine engine =
                scriptMan.getEngineByExtension("js");

        loadServices(engine);
        loadStaticJavascript(engine);

        return engine;
    }

    private void loadServices(ScriptEngine engine) {
        try {
            /* Load the Jumpr namespace */
            engine.eval("Jumpr = Array(" + services.size() + ")");
            for (Map.Entry<String,Map<String,ServiceTemplate>> service :
                    services.entrySet()){
                StringBuilder serviceSnippet = new StringBuilder(
                        "Jumpr['" + service.getKey() + "'] = {");
                boolean isFirst = true;
                for (Map.Entry<String,ServiceTemplate> instance :
                        service.getValue().entrySet()) {
                    if (!isFirst) {
                        serviceSnippet.append(", ");
                    }
                    isFirst = false;

                    serviceSnippet.append ("'")
                            .append(instance.getKey())
                            .append("' : '")
                            .append(instance.getValue().getTemplateClass().getName())
                            .append ("'");

                }

                serviceSnippet.append("};");
                engine.eval(serviceSnippet.toString());
            }
        } catch (ScriptException e) {
            throw new ScriptDriverError(
                    "Unable to prepare service context for script engine", e);
        }
    }

    private void loadStaticJavascript(ScriptEngine engine) {
        InputStream staticJS =
                Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("net/sf/summa/core/scriptdriver/_static_javascript.js");

        if (staticJS == null) {
            throw new ScriptDriverError(
                    "Unable to locate static Javascript context");
        }

        try {
            engine.eval(new InputStreamReader(staticJS));
        } catch (ScriptException e) {
            throw new ScriptDriverError(
                    "Error preparing static context for script engine", e);
        }
    }
}
