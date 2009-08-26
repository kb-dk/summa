package net.sf.summa.core;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 *
 */
public class TemplateTest {

    public static class Foo {
        @Property(value="27", name="integer", type = Integer.class)
        public int i;
    }

    public static class Bar {
        @Property(value="27", name="integer", type = Integer.class)
        private int i;        
    }

    @Test
    public void singlePublicInt() {
        Template<Foo> t = Template.forClass(Foo.class);

        // We must have exactly one property
        assertEquals(t.size(), 1, "Number of properties must match");

        t.put("integer", 1);
        Foo f = t.create();
        assertNotNull(f);
        assertEquals(f.i, 1);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void singlePublicIntUnknownProperty() {
        Template<Foo> t = Template.forClass(Foo.class);
        t.put("bleh", 1);        
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void singlePublicIntFailInvalidPropertyType() {
        Template<Foo> t = Template.forClass(Foo.class);
        t.put("bleh", "one");        
    }

    @Test
    public void singlePrivateInt() {
        Template<Bar> t = Template.forClass(Bar.class);

        assertEquals(t.size(), 1, "Number of properties must match");

        t.put("integer", 1);
        Bar b = t.create();
        assertNotNull(b);
        assertEquals(b.i, 1);
    }

}
