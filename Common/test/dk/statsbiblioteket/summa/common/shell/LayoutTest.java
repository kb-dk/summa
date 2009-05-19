package dk.statsbiblioteket.summa.common.shell;

import java.io.IOException;

/**
 * Test the Layout class
 */
public class LayoutTest {

    public static void main (String[] args) {
        Layout layout = new Layout("Foo", "Barisimo", "QQ");
        layout.appendRow("Foo", "gosh",
                         "Barisimo", "1");
        layout.appendRow("Foo", "Frobnicated!",
                         "QQ", "Peekabo",
                         "KablooeyGabooey", "GarbageDontShowMe!!!!!!!!!!!!!!");

        StringBuilder buf = new StringBuilder();
        try {
            layout.print(buf);
            System.out.println(buf.toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
