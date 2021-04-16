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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.nio.charset.StandardCharsets;
import java.util.Random;

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

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
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
                               Field.Store.NO, Field.Index.NOT_ANALYZED));
        document.add(new Field("ettu",
                               Integer.toString(random.nextInt(1000)),
                               Field.Store.YES, Field.Index.NOT_ANALYZED));
        Payload payload = new Payload(record);
//        payload.getData().put(Payload.LUCENE_DOCUMENT, document);
//        Document insertedDocument =
//                (Document)payload.getData().get(Payload.LUCENE_DOCUMENT);
        assertEquals("The id in the document should be empty", 0,
                   document.getValues(IndexUtils.RECORD_FIELD).length);
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
                "csa_something_other", "csa", inContent.getBytes(StandardCharsets.UTF_8)));
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
            payload.getData().put(IndexUtils.RECORD_FIELD, newID);

            String content = payload.getRecord().getContentAsUTF8();
            content = content.replace("<an>", "<an>" + subCSA + "_");
            content = content.replace("<CI>", "<CI>" + subCSA + "_");
            payload.getRecord().setContent(content.getBytes(StandardCharsets.UTF_8), true);
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

