package dk.statsbiblioteket.summa.common.filter;

import java.util.Random;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.Record;
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
        Payload payload = new Payload(record, document);
        assertNull("The id in the document should be null",
                   payload.getDocument().getValues(Payload.RECORD_FIELD));
        payload.setID("baz");
        System.out.println(payload.getDocument());
        assertEquals("The id in the document should now be defined",
                     "baz",                    
                     payload.getDocument().getValues(Payload.RECORD_FIELD)[0]);

        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(PayloadTest.class);
    }
}
