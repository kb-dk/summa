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
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.common.configuration.Resolver;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.StringWriter;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te",
        comment = "Needs testing of a mix of readLine and the read-methods")
public class LineInputStreamTest extends TestCase {
    public LineInputStreamTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(LineInputStreamTest.class);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void testRealFile() throws Exception {
        FileInputStream is = new FileInputStream(Resolver.getFile(
                "data/white.xml"));
        LineInputStream lis = new LineInputStream(is);
        String line;
        int lc = 0;
        int empty = 0;
        String rest = null;
        while ((line = lis.readLine()) != null) {
            lc++;
            if ("".equals(line)) {
                empty++;
                if (empty == 1) {
                    assertEquals(
                            "The first empty line should be the expected one",
                            11, lc);
                    rest = Strings.flush(lis);
                    break;
                }
            }
        }
        assertNull("EOF should be reached", lis.readLine());
        lis.close();
        System.out.println(String.format(
                "Got %d lines before the first empty line", lc));
        System.out.println("The rest of the file was\n" + rest);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void testBasics() throws Exception {
        String LF = "" + (char)LineInputStream.LF;
        String CR = "" + (char)LineInputStream.CR;
        // Source, expected
        String[][][] TESTS = new String[][][] {
                {{"FooLF\nBar"}, {"FooLF", "Bar"}},
                {{"FooCRLF" + CR + LF + "Bar"}, {"FooCRLF", "Bar"}},
                {{"FooLFCR" + LF + CR + "Bar"}, {"FooLFCR", "", "Bar"}},
                {{"FooCR" + CR + "Bar"}, {"FooCR", "Bar"}},
                {{"FooCRCR" + CR + CR + "Bar"}, {"FooCRCR", "", "Bar"}},
                {{"FooLFLFCRLF" + LF + LF + CR + LF + "Bar"},
                        {"FooLFLFCRLF", "", "", "Bar"}},
                {{"FooLFLF\n\nBar"}, {"FooLFLF", "", "Bar"}},
                {{"Foo\nBar"}, {"Foo", "Bar"}},
                {{""}, {""}},
                {{"Foo"}, {"Foo"}}
        };
        for (String[][] test: TESTS) {
            String source = test[0][0];
            String[] expected = test[1];
            assertLines(source, expected);
        }
    }

    private void assertLines(String source, String[] expected) throws Exception{
        ByteArrayInputStream in = new ByteArrayInputStream(
                source.getBytes("utf-8"));
        LineInputStream lis = new LineInputStream(in);
        String line;
        int pos = 0;
        while ((line = lis.readLine()) != null) {
            assertEquals("Testing '" + source + "'", expected[pos++], line);
        }
    }
}

