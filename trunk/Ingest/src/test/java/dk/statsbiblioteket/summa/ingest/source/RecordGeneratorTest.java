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
package dk.statsbiblioteket.summa.ingest.source;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * RecordGenerator Tester.
 *
 * @author <Authors name>
 * @since <pre>12/04/2008</pre>
 * @version 1.0
 */
public class RecordGeneratorTest extends TestCase {
    private static Log log = LogFactory.getLog(RecordGeneratorTest.class);
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
                "$INCREMENTAL_NUMBER[a]$INCREMENTAL_NUMBER[b]Foo!$Hey!?\n"
                + "$INCREMENTAL_NUMBER[b]";
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
                + "Timestamp (ms): $TIMESTAMP[ms]\n"
                + "Timestamp (iso): $TIMESTAMP[iso]\n"
                + "Random words: $RANDOM_WORDS[1, 3, 5, 7, false]\n"
                + "Random words only letters: $RANDOM_WORDS[1, 3, 5, 7, true]\n"
                + "Word list: $WORD_LIST[2, 3, mylist]";
        conf.set(RecordGenerator.CONF_CONTENT_TEMPLATE, contentTemplate);
        ArrayList<String> myList = new ArrayList<String>(
                Arrays.asList("foo", "bar", "baz", "fighters"));
        conf.set("mylist", myList);
        RecordGenerator generator = new RecordGenerator(conf);
        for (int i = 0 ; i < RUNS ; i ++) {
            log.info("*** run " + i + " ***");
            log.info(generator.expand(contentTemplate));
            // TODO assert
        }
    }
}