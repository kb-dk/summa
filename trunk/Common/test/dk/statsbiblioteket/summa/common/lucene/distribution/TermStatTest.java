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
package dk.statsbiblioteket.summa.common.lucene.distribution;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class TermStatTest extends TestCase {
//    private static Log log = LogFactory.getLog(TermStatTest.class);

    public TermStatTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (TMP.exists()) {
            Files.delete(TMP);
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        try {
                Files.delete(TMP);
        } catch (IOException e) {
                fail("Unable to delete the folder " + TMP);
        }
    }

    public static Test suite() {
        return new TestSuite(TermStatTest.class);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static File TMP = new File("Common/tmp/", "termstattemp");
    public void testPersistence() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        TermStat ts = new TermStat(conf);
        ts.create(TMP);
        ts.setDocCount(87);
        ts.setSource("foo");
        ts.store();
        ts.close();

        ts.open(TMP);
        assertEquals("The docCount should match", 87, ts.getDocCount());
        assertEquals("The source should match", "foo", ts.getSource());
    }

/*    public void testReplace() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        TermStat ts = new TermStat(conf);
        ts.create(TMP);
        ts.add(new TermEntry("foo", 87));
        ts.add(new TermEntry("bar", 10));
        assertEquals("The initial count for bar should be correct",
                     10, ts.getTermCount("bar"));

        ts.getTermCounts().remove(ts.getTermCounts().indexOf(
                new TermEntry("bar", 0)));
        ts.add(new TermEntry("bar", 12));
        assertEquals("The updated count for bar should be correct",
                     12, ts.getTermCount("bar"));
    }*/

  /*  public void testProtectedAccess() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        TermStat ts = new TermStat(conf);
        ts.create(TMP);

        //noinspection DuplicateStringLiteralInspection
        Method method = TermStat.class.getDeclaredMethod("getTermCounts");

        method.setAccessible(true);
        Object result = method.invoke(ts);
        //noinspection unchecked
//        SortedPool<TermEntry> pool = (SortedPool<TermEntry>)result;
    }
    */

}

