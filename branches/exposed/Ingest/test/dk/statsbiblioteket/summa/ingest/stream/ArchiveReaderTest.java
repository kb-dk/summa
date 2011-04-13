package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.security.SignedObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ArchiveReaderTest extends TestCase {
    public ArchiveReaderTest(String name) {
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
        return new TestSuite(ArchiveReaderTest.class);
    }

    public void testDeep() throws Exception {
        String SOURCE = Resolver.getFile("data/zip/").getAbsolutePath();
        List<String> EXPECTED = Arrays.asList("foo", "bar");

        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, SOURCE);
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".finito");
        ArchiveReader reader = new ArchiveReader(conf);

        ArrayList<String> actual = new ArrayList<String>(EXPECTED.size());
        while (reader.hasNext()) {
            Payload payload = reader.next();
            actual.add(new File((String)
                    payload.getData(Payload.ORIGIN)).getName());
            payload.close();
        }
        for (String a: actual) {
            System.out.println(a);
        }
        ExtraAsserts.assertEquals(
            "The resulting files should be as expected", EXPECTED, actual);
    }

}
