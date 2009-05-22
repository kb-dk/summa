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

import java.io.*;
import java.util.Random;

import dk.statsbiblioteket.util.Streams;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ReaderInputStreamTest extends TestCase {
    public static final String ENCODING = "utf-8";

    public ReaderInputStreamTest(String name) {
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
        return new TestSuite(ReaderInputStreamTest.class);
    }

    public void testBasics() throws Exception {
        assertConvert("Simple", "Hello world");
        assertConvert("Empty", "");
        assertConvert("Danish", "æøå");
        assertConvert("Long", "abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRS"
                              + "TUVWXYZÆØÅ!\"#¤%&/()=?`´1234567890 -.,;:_*'¨^½" 
                              + "§<>\\");
    }

    public void testMonkey() throws Exception {
        int RUNS = 5000;
        int MAX = 30;
        Random random = new Random(86);
        for (int run = 0 ; run < RUNS ; run++) {
            int length = random.nextInt(MAX);
            StringWriter sw = new StringWriter(length);
            for (int i = 0 ; i < length ; i++) {
                sw.append((char)random.nextInt(65535));
            }
            assertConvert("Monkey test #" + run, sw.toString());
        }
    }

    public void testAll() throws Exception {
        int PART = 30;
        char c = 0;
        while (c < 65536) {
            StringWriter sw = new StringWriter(PART);
            char startPos = c;
            for (int i = 0 ; i < PART ; i++) {
                sw.append(c++);
                if (c == 65536) {
                    break;
                }
            }
            assertConvert("Pos " + (int)startPos, sw.toString());
        }
    }

    /* The problem here probably lies with Java's handling of unicode-chars
       made up by multiple Java chars.
     */
    public void test56300() throws Exception {
        int LENGTH = 30;
        StringWriter sw = new StringWriter(LENGTH);
        for (int i = 0 ; i < LENGTH ; i++) {
            sw.append((char)(56300 + i));
        }
        assertConvert("Chars at position 56300 test", sw.toString());
    }

    private void assertConvert(String message, String input) throws Exception {
        byte[] expected = input.getBytes(ENCODING);
        Reader in = new StringReader(input);
        ReaderInputStream is = new ReaderInputStream(in, ENCODING);
        ByteArrayOutputStream os = new ByteArrayOutputStream(expected.length);
        Streams.pipe(is, os);
        byte[] actual = os.toByteArray();
        if (expected.length != actual.length) {
            StringWriter sw = new StringWriter(input.length() * 10);
            sw.append("Diff for bytes:");
            for (int i = 0 ;
                 i < Math.max(expected.length, actual.length) ;
                 i++) {
                sw.append(String.format(
                        " (%s, %s)",
                        expected.length > i ? Byte.toString(expected[i]) : "NA",
                        actual.length > i ? Byte.toString(actual[i]) : "NA"));
            }
            System.out.println(sw.toString());

            sw = new StringWriter(100);
            sw.append("Input chars:");
            for (char c: input.toCharArray()) {
                sw.append(" ").append(Integer.toString(c));
            }
            System.out.println(sw.toString());
        }
        assertEquals(message + " should result in the right number of bytes. "
                     + "Input was '" + input + "'",
                     expected.length, actual.length);
        for (int i = 0 ; i < expected.length ; i++) {
            assertEquals(message + " should have the same content. The bytes at"
                         + " position " + i + " differs. " 
                         + "Input was '" + input + "'",
                         expected[i], actual[i]);
        }
    }
}
