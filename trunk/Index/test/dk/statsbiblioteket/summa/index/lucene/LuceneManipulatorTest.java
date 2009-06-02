package dk.statsbiblioteket.summa.index.lucene;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.unittest.LuceneTestHelper;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class LuceneManipulatorTest extends TestCase implements ObjectFilter {
    private static Log log = LogFactory.getLog(LuceneManipulatorTest.class);
    public LuceneManipulatorTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (location.exists()) {
            Files.delete(location);
        }
    }

    @Override
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
        LuceneTestHelper.verifyContent(
                new File(location, LuceneIndexUtils.LUCENE_FOLDER), ids);
    }

    // TODO: Randomized deletions, additions and updated
    // TODO: Verify segment merging avoidance

    public void test1000Additions() throws Exception {
        testLargeIndex(1000);
    }

    public void testLargeIndex(int docCount) throws Exception {
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(docCount);
        LuceneManipulator manipulator = openIndex(102);
        String[] ids = new String[docCount];
        for (int i = 0 ; i < docCount ; i++) {
            ids[i] = "doc" + i;
            manipulator.update(getPayload(ids[i], false));
            profiler.beat();
        }
        manipulator.close();
        LuceneTestHelper.verifyContent(
                new File(location, LuceneIndexUtils.LUCENE_FOLDER), ids);
        System.out.println("Spend " + profiler.getSpendTime() + " on "
                           + docCount + " additions. Mean speed: "
                           + profiler.getBps() + " additions/second");

        profiler.reset();
        manipulator = openIndex(100);
        for (int i = 0 ; i < docCount ; i++) {
            String id = "doc" + i;
            manipulator.update(getPayload(id, true)); // A delete and an add
            manipulator.update(getPayload(id, false)); // A delete and an add
            profiler.beat();
        }
        manipulator.close();
        LuceneTestHelper.verifyContent(
                new File(location, LuceneIndexUtils.LUCENE_FOLDER), ids);
        System.out.println("Spend " + profiler.getSpendTime() + " on "
                           + docCount + " updates. Mean speed: "
                           + profiler.getBps() + " updates/second");
    }

    public void testDeletionsIndex() throws Exception {
        LuceneManipulator manipulator = openIndex(2);

        {
            Payload payloadB = getPayload("b", false);
            manipulator.update(payloadB);
            assertEquals("The ID for the first Payload should be correct",
                         0, payloadB.getData(LuceneIndexUtils.META_ADD_DOCID));
        }

        {
            Payload payloadA = getPayload("a", false);
            manipulator.update(payloadA);
            assertEquals("The ID for the second Payload should be correct",
                         1, payloadA.getData(LuceneIndexUtils.META_ADD_DOCID));
        }

        {
            Payload deleted = getPayload("b", true);
            manipulator.update(deleted);
            assertEquals("The ID for the deleted Payload should be correct",
                        0, deleted.getData(LuceneIndexUtils.META_DELETE_DOCID));
        }

        {
            Payload deleted2 = getPayload("b", true);
            manipulator.update(deleted2);
            assertEquals("The ID for the redeleted Payload should be unchanged",
                       0, deleted2.getData(LuceneIndexUtils.META_DELETE_DOCID));
        }

        {
            Payload reAdd = getPayload("b", false);
            manipulator.update(reAdd);
            assertEquals("The ID for the readded Payload should be correct",
                         2, reAdd.getData(LuceneIndexUtils.META_ADD_DOCID));
        }

        {
            Payload last = getPayload("c", false);
            manipulator.update(last);
            assertEquals("The ID for the last Payload should be correct",
                         3, last.getData(LuceneIndexUtils.META_ADD_DOCID));
        }

        manipulator.close();
        logIndex();
        String[] expected = new String[]{"a", "b", "c"};
        LuceneTestHelper.verifyContent(
                new File(location, LuceneIndexUtils.LUCENE_FOLDER), expected);
    }

    @SuppressWarnings({"OverlyBroadCatchBlock"})
    private void logIndex() throws Exception {
        try {
            IndexReader reader = IndexReader.open(
                    new File(location, LuceneIndexUtils.LUCENE_FOLDER));
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
                               Field.Store.NO, Field.Index.NOT_ANALYZED));
        if (deleted) {
            record.setDeleted(true);
        }
        Payload payload = new Payload(record);
        payload.getData().put(Payload.LUCENE_DOCUMENT, document);
        return payload;
    }

    public void testProperIndexCreation() throws Exception {
        String descLocation =
                "file://"
                + Thread.currentThread().getContextClassLoader().getResource(
                        "data/fagref/fagref_IndexDescriptor.xml").getFile();
        File manConfLocation = File.createTempFile("configuration", ".xml");
        manConfLocation.deleteOnExit();
        Files.saveString(String.format(
                DocumentCreatorTest.CREATOR_SETUP, descLocation),
                         manConfLocation);
        Configuration conf = new Configuration(new XStorage(manConfLocation));

        LuceneManipulator manipulator = new LuceneManipulator(conf);
        manipulator.open(location);

        StreamingDocumentCreator creator = new StreamingDocumentCreator(conf);
        creator.setSource(this);

        while (creator.hasNext()) {
            manipulator.update(creator.next());
        }
        manipulator.close();
        log.info("Index created at '" + location + "'. Opening index...");

        logIndex();        
        IndexReader reader = IndexReader.open(
                new File(location, LuceneIndexUtils.LUCENE_FOLDER));
        assertEquals("The number of documents in the index should match",
                     1, reader.maxDoc());
        assertEquals("The recordID of the single indexed document should match",
                     DOC_ID, reader.document(0).getField(
                IndexUtils.RECORD_FIELD).stringValue());
        // Check for analyzer
        // Search for Jens with default / different prefixes
    }

    public boolean hasNext() {
        return hasMore;
    }

    private boolean hasMore = true;
    private String jens = null;
    private String DOC_ID = "fagref:jh@statsbiblioteket.invalid";
    public Payload next() {
        if (!hasMore) {
            return null;
        }
        if (jens == null) {
            try {
                jens = Streams.getUTF8Resource(
                        "data/fagref/jens.hansen.newstyle.xml");
            } catch (IOException e) {
                throw new RuntimeException("Could not load jens hansen data",
                                           e);
            }
        }
        hasMore = false;
        try {
            return new Payload(new Record(DOC_ID,
                                       "dummy", jens.getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported", e);
        }
    }

    public void remove() {
    }

    public void setSource(Filter filter) {
    }

    public boolean pump() throws IOException {
        return next() != null;
    }

    public void close(boolean success) {
        hasMore = false;
    }
}



