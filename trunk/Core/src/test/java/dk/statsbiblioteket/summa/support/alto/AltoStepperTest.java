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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.support.alto.hp.HPAltoAnalyzer;
import dk.statsbiblioteket.summa.support.alto.hp.HPAltoAnalyzerTest;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.util.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoStepperTest extends TestCase {
//    private static Log log = LogFactory.getLog(HPAltoParserTest.class);

    public AltoStepperTest(String name) {
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
        return new TestSuite(AltoStepperTest.class);
    }

    public void testTopX() throws Exception {
        final String SOURCE = HPAltoAnalyzerTest.alto1934;
        final int X = 10;

        Alto alto = new Alto(new File(SOURCE));

        // Collect stats
        final Map<String, Integer> words = new HashMap<String, Integer>();
        AltoStepper stepper = new AltoStepper() {
            @Override
            public Alto.TextString process(Alto.TextString textString) {
                Integer word = words.get(textString.getContent());
                if (word == null) {
                    word = 0;
                }
                words.put(textString.getContent(), ++word);
                return textString;
            }
        };

        stepper.step(alto);

        // Get top X
        final Set<Pair<String, Integer>> counters =
                new TreeSet<Pair<String, Integer>>(new Comparator<Pair<String, Integer>>() {
                    @Override
                    public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                        int c = -o1.getValue().compareTo(o2.getValue());
                        return c == 0 ? o1.getKey().compareTo(o2.getKey()) : c;
                    }
                });
        for (Map.Entry<String, Integer> entry: words.entrySet()) {
            counters.add(new Pair<String, Integer>(entry.getKey(), entry.getValue()));
        }

        System.out.println("Unique words in '" + SOURCE + "': " + counters.size());
        int counter = 1;
        for (Pair<String, Integer> c: counters) {
            System.out.println(counter++ + ": " + c.getKey() + "(" + c.getValue() + ")");
            if (counter == X+1) {
                break;
            }
        }
    }

}
