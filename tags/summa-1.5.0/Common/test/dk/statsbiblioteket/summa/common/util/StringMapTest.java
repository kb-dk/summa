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
package dk.statsbiblioteket.summa.common.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

/**
 * StringMap Tester.
 *
 * @author <Authors name>
 * @since <pre>03/26/2008</pre>
 * @version 1.0
 */
public class StringMapTest extends TestCase {
    public StringMapTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testEscape() throws Exception {
        for (String[] entry: entries) {
            assertEquals("Escape and unescape should be reflective for key",
                         entry[0],
                         StringMap.unescape(StringMap.escape(entry[0])));
            assertEquals("Escape and unescape should be reflective for value", 
                         entry[1],
                         StringMap.unescape(StringMap.escape(entry[1])));
        }
    }

    // FIXME: StringMap should handle the empty key and value
    private static final String[][] entries = new String[][]{
            {"=/e/s\n///n/ee/s", "\n"},
            {"", ""},
            {"d", ""},
            {"=", "="},
            {"hello world", "!"}
    };

    public void testFormalString() throws Exception {
        StringMap original = new StringMap(10);
        for (String[] entry: entries) {
            original.put(entry[0], entry[1]);
        }
        for (String[] entry: entries) {
            assertEquals("The original value should match",
                         entry[1], original.get(entry[0]));
        }

        String formal = original.toFormal();
        StringMap fromFormal = StringMap.fromFormal(formal);
        assertEquals("The map should survive to and from formal String",
                     original, fromFormal);

        for (String[] entry: entries) {
            assertEquals("The extracted value should match",
                         entry[1], fromFormal.get(entry[0]));
        }
    }


    public static Test suite() {
        return new TestSuite(StringMapTest.class);
    }
}




