package dk.statsbiblioteket.summa.index;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.unittest.LuceneUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class LuceneManipulatorTest extends TestCase {
    private static Log log = LogFactory.getLog(LuceneManipulatorTest.class);
    public LuceneManipulatorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        if (location.exists()) {
            Files.delete(location);
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (location.exists()) {
            Files.delete(location);
        }
    }

    public static Test suite() {
        return new TestSuite(LuceneManipulatorTest.class);
    }

    public File location =
            new File(System.getProperty("java.io.tmpdir"), "tempindex");

    private LuceneManipulator openIndex(int buffer) throws IOException {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(LuceneManipulator.CONF_BUFFER_SIZE_PAYLOADS, buffer);
        LuceneManipulator manipulator = new LuceneManipulator(conf);
        manipulator.open(location);
        return manipulator;
    }

    public void testSimpleIndex() throws Exception {
        LuceneManipulator manipulator = openIndex(2);
        String[] ids = new String[]{"foo", "bar", "zoo"};
        for (String id: ids) {
            manipulator.update(getPayload(id, false));
        }
        manipulator.close();
        logIndex();
        LuceneUtils.verifyContent(
                new File(location, LuceneManipulator.LUCENE_FOLDER), ids);
    }

    // TODO: Randomized deletions, additions and updated
    // TODO: Verify segment merging avoidance

    public void test1000Additions() throws Exception {
        testLargeIndex(1000);
    }

    public void testLargeIndex(int docCount) throws Exception {
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(docCount);
        LuceneManipulator manipulator = openIndex(100);
        String[] ids = new String[docCount];
        for (int i = 0 ; i < docCount ; i++) {
            ids[i] = "doc" + i;
            manipulator.update(getPayload(ids[i], false));
            profiler.beat();
        }
        manipulator.close();
        LuceneUtils.verifyContent(
                new File(location, LuceneManipulator.LUCENE_FOLDER), ids);
        System.out.println("Spend " + profiler.getSpendTime() + " on "
                           + docCount + " additions. Mean speed: "
                           + profiler.getBps() + " additions/second");

        profiler.reset();
        manipulator = openIndex(100);
        for (int i = 0 ; i < docCount ; i++) {
            String id = "doc" + i;
            manipulator.update(getPayload(id, false)); // A delete and an add
            profiler.beat();
        }
        manipulator.close();
        LuceneUtils.verifyContent(
                new File(location, LuceneManipulator.LUCENE_FOLDER), ids);
        System.out.println("Spend " + profiler.getSpendTime() + " on "
                           + docCount + " updates. Mean speed: "
                           + profiler.getBps() + " updates/second");
    }

    public void testDeletionsIndex() throws Exception {
        LuceneManipulator manipulator = openIndex(2);
        String[] expected = new String[]{"a", "b", "c"};

        manipulator.update(getPayload("b", false));
        manipulator.update(getPayload("a", false));
        manipulator.update(getPayload("b", true));
        manipulator.update(getPayload("b", true));
        manipulator.update(getPayload("b", false));
        manipulator.update(getPayload("c", false));

        manipulator.close();
        logIndex();
        LuceneUtils.verifyContent(
                new File(location, LuceneManipulator.LUCENE_FOLDER), expected);
    }

    @SuppressWarnings({"OverlyBroadCatchBlock"})
    private void logIndex() throws Exception {
        try {
            IndexReader reader = IndexReader.open(
                    new File(location, LuceneManipulator.LUCENE_FOLDER));
            for (int i = 0 ; i < reader.maxDoc() ; i++) {
                if (!reader.isDeleted(i)) {
                    try {
                        log.debug("id(" + i + "): "
                                  + reader.document(i).getValues(
                                IndexUtils.RECORD_FIELD)[0]);
                    } catch (Exception e) {
                        log.warn("Could not extract id(" + i + ")", e);
                    }
                } else {
                    log.debug("id(" + i + ") is deleted");
                }
            }
            reader.close();
        } catch (Exception e) {
            log.warn("Could not log ids from index", e);
        }
    }

    private Random random = new Random();
    private Payload getPayload(String id, boolean deleted) {
        Record record = new Record(id, "testbase", new byte[0]);
        Document document = new Document();
        document.add(new Field("mynumber",
                               Integer.toString(random.nextInt(1000)),
                               Field.Store.NO, Field.Index.UN_TOKENIZED));
        if (deleted) {
            record.setDeleted(true);
        }
        Payload payload = new Payload(record);
        payload.getData().put(Payload.LUCENE_DOCUMENT, document);
        return payload;
    }
}
