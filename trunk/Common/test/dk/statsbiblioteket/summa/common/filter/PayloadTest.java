package dk.statsbiblioteket.summa.common.filter;

import java.util.Random;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Payload Tester.
 *
 * @author <Authors name>
 * @since <pre>04/22/2008</pre>
 * @version 1.0
 */
public class PayloadTest extends TestCase {
    public PayloadTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSetGetStream() throws Exception {
        //TODO: Test goes here...
    }

    public void testSetGetRecord() throws Exception {
        //TODO: Test goes here...
    }

    public void testSetGetDocument() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetMeta() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetMeta1() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetId() throws Exception {
        //TODO: Test goes here...
    }

    Random random = new Random();
    public void testSetID() throws Exception {
        Record record = new Record("foo", "bar", new byte[0]);
        Document document = new Document();
        document.add(new Field("whatever",
                               Integer.toString(random.nextInt(1000)),
                               Field.Store.NO, Field.Index.UN_TOKENIZED));
        document.add(new Field("ettu",
                               Integer.toString(random.nextInt(1000)),
                               Field.Store.YES, Field.Index.UN_TOKENIZED));
        Payload payload = new Payload(record);
        payload.getData().put(Payload.LUCENE_DOCUMENT, document);
        Document insertedDocument =
                (Document)payload.getData().get(Payload.LUCENE_DOCUMENT);
        assertNull("The id in the document should be null",
                   insertedDocument.getValues(IndexUtils.RECORD_FIELD));
        payload.setID("baz");
/*        insertedDocument =
                (Document)payload.getData().get(Payload.LUCENE_DOCUMENT);
        System.out.println(insertedDocument);
        assertEquals("The id in the document should now be defined",
                     "baz",                    
                     insertedDocument.getValues(Payload.RECORD_FIELD)[0]);
  */
        //TODO: Better check for id
    }

    public static Test suite() {
        return new TestSuite(PayloadTest.class);
    }
}



