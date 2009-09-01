package dk.statsbiblioteket.summa.common.filter;

import java.util.Random;
import java.io.UnsupportedEncodingException;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.util.Strings;
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
        assertEquals("The id in the document should be empty", 0,
                   insertedDocument.getValues(IndexUtils.RECORD_FIELD).length);
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

    public void testIDFix() throws Exception {
        String inContent =
                "<rec id=\"1\">\n"
                + "<ti><CI>Myid</CI></ti>\n"
                + "<auths><au>Art, David C</au></auths>\n"
                + "<an>200516850</an>\n"
                + "</rec>";
        Payload payload = new Payload(new Record(
                "csa_something_other", "csa", inContent.getBytes("utf-8")));
        payload.getData().put(
                Payload.ORIGIN, "/home/example/data/csa/ssa/csu/hello.xml");
        String id = payload.getRecord().getId();
        String origin = (String)payload.getData(Payload.ORIGIN);
        if (origin == null) {
            // Panic
        } else {
            String[] originSubs = origin.split("/|\\\\");
            String subCSA = originSubs[originSubs.length-2].split("/")[0];
            String[] idTokens = id.split("_", 2);
            String newID = idTokens[0] + "_" + subCSA + "_" + idTokens[1];
            payload.getRecord().setId(newID);
            payload.getData().put("recordID", newID);

            String content = payload.getRecord().getContentAsUTF8();
            content = content.replace("<an>", "<an>" + subCSA + "_");
            content = content.replace("<CI>", "<CI>" + subCSA + "_");
            payload.getRecord().setContent(content.getBytes("utf-8"), true);
        }
        String expected = "csa_csu_something_other";
        assertEquals("The ID should be as expected",
                     expected, payload.getRecord().getId());
        String expectedContent =
                "<rec id=\"1\">\n"
                + "<ti><CI>csu_Myid</CI></ti>\n"
                + "<auths><au>Art, David C</au></auths>\n"
                + "<an>csu_200516850</an>\n"
                + "</rec>";
        assertEquals("The content should be as expected",
                     expectedContent, payload.getRecord().getContentAsUTF8());
    }
}
