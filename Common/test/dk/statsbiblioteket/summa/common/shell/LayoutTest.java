/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
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

