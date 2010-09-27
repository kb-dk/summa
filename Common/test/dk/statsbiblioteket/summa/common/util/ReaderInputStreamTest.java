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

import dk.statsbiblioteket.util.Streams;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test {@link ReaderInputStream}.
 */
public class ReaderInputStreamTest extends TestCase {
    /** Local logger. */
    private static Log log = LogFactory.getLog(ReaderInputStreamTest.class);
    /** Default encoding. */
    public static final String ENCODING = "utf-8";

    /**
     * Constructor.
     * @param name The name.
     */
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

    /**
     * This unit test will fail on multi-char unicode characters as described
     * in the class documentation for {@link ReaderInputStream}. We keep it
     * enabled to ensure we don't forget about this!
     * @throws Exception always
     */
    public void testMonkey() throws Exception {
        System.out.println("Test monkey");
        final int runs = 5000;
        final int max = 30;
        Random random = new Random(86);
        for (int run = 0; run < runs; run++) {
            int length = random.nextInt(max);
            StringWriter sw = new StringWriter(length);
            for (int i = 0; i < length; i++) {
                sw.append((char) random.nextInt(65535));
            }
            assertConvert("Monkey test #" + run, sw.toString());
        }
        System.out.println("Test monkey end");
    }

    public void testLength() throws Exception {
        final int max = 500;
        for (int length = 0; length < max; length++) {
            StringWriter sw = new StringWriter(length);
            for (int i = 0; i < length; i++) {
                sw.append("A");
            }
            assertConvert("Test of length " + length, sw.toString());
        }
    }

    /**
     * This unit test will fail on multi-char unicode characters as described
     * in the class documentation for {@link ReaderInputStream}. We keep it
     * enabled to ensure we don't forget about this!
     * @throws Exception always
     */
    public void testAll() throws Exception {
        final int part = 30;
        final int max = 65536;
        char c = 0;
        while (c < max) {
            StringWriter sw = new StringWriter(part);
            char startPos = c;
            for (int i = 0; i < part; i++) {
                sw.append(c++);
                if (c == max) {
                    break;
                }
            }
            assertConvert("Pos " + (int) startPos, sw.toString());
        }
    }

    /**
     * This unit test will fail on multi-char unicode characters as described
     * in the class documentation for {@link ReaderInputStream}. We keep it
     * enabled to ensure we don't forget about this!
     * @throws Exception always
     */
    public void test56300() throws Exception {
        final int high = 56300;
        final int length = 30;
        StringWriter sw = new StringWriter(length);
        for (int i = 0; i < length; i++) {
            sw.append((char) (high + i));
        }
        assertConvert("Chars at position " + high + " test", sw.toString());
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
            for (int i = 0; i < Math.max(expected.length, actual.length); i++) {
                sw.append(String.format(" (%s, %s)",
                        expected.length > i ? Byte.toString(expected[i]) : "NA",
                        actual.length > i ? Byte.toString(actual[i]) : "NA"));
            }
            log.info(sw.toString());
            System.out.println(sw.toString());
            // TODO use assert

            sw = new StringWriter(100);
            sw.append("Input chars:");
            for (char c : input.toCharArray()) {
                sw.append(" ").append(Integer.toString(c));
            }
            log.info(sw.toString());
            // TODO use assert
        }
        assertEquals(message + " should result in the right number of bytes.\n"
                     + "Input was '" + input + "'",
                     expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(message + " should have the same content. The bytes at"
                         + " position " + i + " differs. "
                         + "Input was '" + input + "'",
                         expected[i], actual[i]);
        }
    }
}
