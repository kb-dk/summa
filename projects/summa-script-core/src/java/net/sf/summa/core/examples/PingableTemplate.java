package net.sf.summa.core.examples;

import net.sf.summa.core.ServiceTemplate;

/**
 * FIXME: Missing class docs for net.sf.summa.core.examples.PingableTemplate
 *
 * @author mke
 * @since Sep 11, 2009
 */
public class PingableTemplate extends ServiceTemplate implements Pingable {
    public PingableTemplate() {
        super(Pingable.class, "pingable", "bleh");
    }

    public String ping(String msg) {
        return "bleh+"+msg;
    }
}
