package net.sf.summa.core.examples;

import net.sf.summa.core.ServiceTemplate;
import net.sf.summa.core.Property;

/**
 * FIXME: Missing class docs for net.sf.summa.core.examples.FoobarPingable
 *
 * @author mke
 * @since Sep 11, 2009
 */
public class FoobarPingable implements Pingable {

    @Property(name="ping", value = "FoobarPong")
    private String ping;

    public static class Template extends ServiceTemplate<FoobarPingable> {
        public Template() {
            super(FoobarPingable.class, "Pingable", "Foobar");
        }
    }

    public String ping(String msg) {
        return this.ping+msg;
    }
}
