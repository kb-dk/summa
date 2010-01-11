package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

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
        author = "mke")
public class PythonBatchJobTest extends StorageTestBase {

    public void testPythonEngine() throws Exception {
        storage.flush(new Record(testId1, testBase1, testContent1));
        String hello = storage.batchJob(
                "test.job.py", null, 0, Long.MAX_VALUE, null
        );
        assertEquals("Hello world!", hello);
    }

}
