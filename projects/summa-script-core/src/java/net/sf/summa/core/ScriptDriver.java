package net.sf.summa.core;

import sun.misc.Service;

import javax.script.ScriptEngine;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

/**
 * FIXME: Missing class docs for net.sf.summa.core.ScriptDriver
 *
 * @author mke
 * @since Oct 5, 2009
 */
public abstract class ScriptDriver {

    private static List<ScriptDriver> drivers;

    static {
        drivers = new LinkedList<ScriptDriver>();

        Iterator iter = Service.providers(ScriptDriver.class);
        while (iter.hasNext()) {
            ScriptDriver driver = (ScriptDriver)iter.next();
            drivers.add(driver);
        }
    }

    public static ScriptDriver getDriver(Script script) {
        for (ScriptDriver driver : drivers) {
            if (driver.accepts(script)) {
                return driver;
            }
        }
        return null;
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    public abstract boolean accepts(Script script);

    public abstract ScriptEngine createEngine(); 

}
