/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
