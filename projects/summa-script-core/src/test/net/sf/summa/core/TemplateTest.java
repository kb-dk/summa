package net.sf.summa.core;

import static net.sf.summa.core.Property.*;
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

    public static class Many {
        @Property(value="27", name="integer", type = Integer.class)
        private int i;

        @Property(value="java", name="string", type = String.class)
        private String s;

        @Property(value="true", name="boolean", type = Boolean.class)
        private boolean b;

        @Property(value="3.14", name="float", type = Float.class)
        private float f;
    }

    public static class Manda {
        @Property(name="integer",
                  type = Integer.class, mandatory = true)
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

    @Test(expectedExceptions = InvalidPropertyAssignment.class)
    public void missingMandatory() {
        Template<Manda> t = Template.forClass(Manda.class);
        assertEquals(t.size(), 1, "Number of properties must match");
        Manda b = t.create();
    }

    @Test
    public void manyPrivateWithDefaults() {
        Template<Many> t = Template.forClass(Many.class);

        assertEquals(t.size(), 4, "Number of properties must match");
        Many m = t.create();
        assertNotNull(m);
        assertEquals(m.i, 27);
        assertEquals(m.s, "java");
        assertEquals(m.b, true);
        assertTrue(m.f < 3.15 && m.f > 3.13);
    }

    @Test
    public void twoIdenticalFromSameTemplate() {
        Template<Bar> t = Template.forClass(Bar.class);

        assertEquals(t.size(), 1, "Number of properties must match");

        t.put("integer", 1);
        Bar b1 = t.create();
        Bar b2 = t.create();
        assertNotNull(b1);
        assertNotNull(b2);
        assertEquals(b1.i, 1);
        assertEquals(b1.i, 1);
    }

    @Test
    public void twoDifferentFromSameTemplate() {
        Template<Bar> t = Template.forClass(Bar.class);

        assertEquals(t.size(), 1, "Number of properties must match");

        t.put("integer", 1);
        Bar b1 = t.create();
        t.put("integer", 2);
        Bar b2 = t.create();
        assertNotNull(b1);
        assertNotNull(b2);
        assertEquals(b1.i, 1);
        assertEquals(b2.i, 2);
    }

}
