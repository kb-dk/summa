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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.summa.common.index.IndexCommon;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexControllerTest extends TestCase {
    public IndexControllerTest(String name) {
        super(name);
    }
    File root = new File(System.getProperty("java.io.tmpdir"), "subdirtest");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        root.mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (root.exists()) {
            Files.delete(root);
        }
    }

    public static Test suite() {
        return new TestSuite(IndexControllerTest.class);
    }

    public void testLocateFiles() throws Exception {
        new File(root, "20080417-212800").mkdir();
        File lastValid = new File(root, "20080417-213400");
        lastValid.mkdir();
        new File(root, "20080417-212900").mkdir();
        new File(root, "20080417-212900").setWritable(false);
        new File(root, "20080417-213200").createNewFile();
        new File(root, "0080417-212800").mkdir();
        new File(root, "20080417-212500").mkdir();
        new File(root, "20080417-213000-foo").mkdir();

        File[] subs = root.listFiles(IndexCommon.SUBFOLDER_FILTER_WRITE);
        Arrays.sort(subs);
        assertEquals("The number of valid folders should match. Got "
                     + Arrays.toString(subs),
                     3, subs.length);
        assertEquals("The last folder should be the right one",
                     lastValid, subs[subs.length-1]);
    }

    public void testTimestampFormatting() throws Exception {
        Calendar t = new GregorianCalendar(2008, 3, 17, 21, 50, 54);
        assertEquals("The timestamp should be properly formatted",
                     "20080417-215054", 
                     String.format(Locale.ROOT, IndexCommon.TIMESTAMP_FORMAT, t));
    }
}




