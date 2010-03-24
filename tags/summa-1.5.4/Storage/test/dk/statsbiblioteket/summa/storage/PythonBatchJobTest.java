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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.*;

/**
 * Test running batch jobs with Jython. Note that Summa requires
 * Jython 2.5.1 or later on the class path. Jython <= 2.5 does not implement
 * the Java ScriptEngine SPI. Download Jython from http://jython.org
 *
 * @author mke
 * @since Jan 11, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke, hbk")
public class PythonBatchJobTest extends StorageTestBase {

    public void testPythonEngine() throws Exception {
        storage.flush(new Record(testId1, testBase1, testContent1));
        try {
            String hello = storage.batchJob(
                "test.job.py", null, 0, Long.MAX_VALUE, null
            );
            assertEquals("Hello world!", hello);
            fail("Python isn't a know scripting language yet.");
        } catch(IOException e) {
            // expected;
        }

    }

}

