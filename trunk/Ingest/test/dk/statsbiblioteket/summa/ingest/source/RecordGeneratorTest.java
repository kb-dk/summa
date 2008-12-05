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
package dk.statsbiblioteket.summa.ingest.source;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * RecordGenerator Tester.
 *
 * @author <Authors name>
 * @since <pre>12/04/2008</pre>
 * @version 1.0
 */
public class RecordGeneratorTest extends TestCase {
    public RecordGeneratorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(RecordGeneratorTest.class);
    }

    public void testIncrementalNumber() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        String contentTemplate =
                RecordGenerator.CONTENT_INCREMENTAL_NUMBER + "[a]"
                + RecordGenerator.CONTENT_INCREMENTAL_NUMBER + "[b]"
                + "Foo!$Hey!?\n"
                + RecordGenerator.CONTENT_INCREMENTAL_NUMBER + "[b]";
        conf.set(RecordGenerator.CONF_CONTENT_TEMPLATE, contentTemplate);
        RecordGenerator generator = new RecordGenerator(conf);
        assertEquals("Expansion of incrementalNumber should work",
                     "00Foo!$Hey!?\n1", generator.expand(contentTemplate));
    }
    public void testDumpAll() throws Exception {
        int RUNS = 3;
        Configuration conf = Configuration.newMemoryBased();
        String contentTemplate =
                "Incremental number: $INCREMENTAL_NUMBER[a]\n"
                + "Random number: $RANDOM_NUMBER[5, 10]\n"
                + "Random chars: $RANDOM_CHARS[2, 6, false]\n"
                + "Random chars only letters: $RANDOM_CHARS[2, 6, true]\n"
                + "Random words: $RANDOM_WORDS[1, 3, 5, 7, false]\n"
                + "Random words only letters: $RANDOM_WORDS[1, 3, 5, 7, true]\n"
                + "Word list: $WORD_LIST[2, 3, mylist]";
        conf.set(RecordGenerator.CONF_CONTENT_TEMPLATE, contentTemplate);
        ArrayList<String> myList = new ArrayList<String>(
                Arrays.asList("foo", "bar", "baz", "fighters"));
        conf.set("mylist", myList);
        RecordGenerator generator = new RecordGenerator(conf);
        for (int i = 0 ; i < RUNS ; i ++) {
            System.out.println("*** run " + i + " ***");
            System.out.println(generator.expand(contentTemplate));
        }
    }
}
