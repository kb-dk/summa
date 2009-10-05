package net.sf.summa.core.scriptdriver;

import net.sf.summa.core.ScriptDriver;
import net.sf.summa.core.Script;
import net.sf.summa.core.ServiceTemplate;
import net.sf.summa.core.ScriptDriverError;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

/**
 * FIXME: Missing class docs for net.sf.summa.core.scriptdriver.JavaScript
 *
 * @author mke
 * @since Oct 5, 2009
 */
public class JavaScript extends ScriptDriver {
    private ScriptEngineManager scriptMan;

    public JavaScript() {
        scriptMan = new ScriptEngineManager();
    }

    public boolean accepts(Script script) {
        return "js".equals(script.getExtension());
    }

    public ScriptEngine createEngine() {
        Map<String,Map<String,ServiceTemplate>> services =
                                                  ServiceTemplate.getServices();
        ScriptEngine engine =
                scriptMan.getEngineByExtension("js");

        try {
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
                                  .append(instance.getValue().getClass().getName())
                                  .append ("'");

                }

                serviceSnippet.append("};");                
                engine.eval(serviceSnippet.toString());
            }
        } catch (ScriptException e) {
            throw new ScriptDriverError(
                    "Unable to prepare bindings for script engine", e);
        }

        return engine;
    }
}
