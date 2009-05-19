package dk.statsbiblioteket.summa.common.shell;

import java.io.IOException;

/**
 * Test the Layout class
 */
public class LayoutTest {

    public static void main (String[] args) {
        Layout layout = new Layout("Foo");
        layout.appendColumns("Barisimo", "QQ");
        layout.setDelimiter(" | ");
        layout.appendRow("Foo", "gosh",
                         "Barisimo", "1");
        layout.appendRow("Foo", "Frobnicated!",
                         "QQ", "Peekabo",
                         "KablooeyGabooey", "GarbageDontShowMe!!!!!!!!!!!!!!");
        layout.appendRow("Foo", null);

        System.out.println(layout.toString());

    }
}
