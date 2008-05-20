package dk.statsbiblioteket.summa.index;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocumentCreatorTest extends TestCase implements ObjectFilter {
    public DocumentCreatorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(DocumentCreatorTest.class);
    }

    public static final String SIMPLE_DESCRIPTOR =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<IndexDescriptor version=\"1.0\">\n"
            + "    <fields>\n"
            + "        <field name=\"mystored\" parent=\"stored\" indexed=\"true\"/>\n"
            + "    </fields>\n"
            + "    <defaultSearchFields>freetext mystored</defaultSearchFields>\n"
            + "</IndexDescriptor>";

    public static final String SIMPLE_RECORD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<SummaDocument version=\"1.0\" id=\"mybase:grimme_aellinger\" xmlns=\"http://statsbiblioteket.dk/2008/Index\">\n"
            + "    <fields>\n"
            + "        <field name=\"mystored\" boost=\"2.0\">Foo bar</field>\n"
            + "        <field name=\"mystored\" boost=\"2.0\">Kazam</field>\n"
            + "        <field name=\"keyword\">Flim flam</field>\n"
            + "        <field name=\"nonexisting\">Should be default</field>\n"
            + "    </fields>\n"
            + "</SummaDocument>";

    public static final String CREATOR_SETUP =
            "<xproperties>\n"
            + "    <xproperties>\n"
            + "        <entry>\n"
            + "            <key>" + DocumentCreator.CONF_DESCRIPTOR + "</key>\n"
            + "            <value class=\"xproperties\">\n"
            + "                <entry>\n"
            + "                    <key>" + IndexDescriptor.CONF_ABSOLUTE_LOCATION + "</key>\n"
            + "                    <value class=\"string\">%s</value>\n"
            + "                </entry>\n"
            + "            </value>\n"
            + "        </entry>\n"
            + "    </xproperties>\n"
            + "</xproperties>";

    public void testSimpleTransformation() throws Exception {
        File descriptorLocation = File.createTempFile("descriptor", ".xml");
        descriptorLocation.deleteOnExit();
        Files.saveString(SIMPLE_DESCRIPTOR, descriptorLocation);
        File confLocation = File.createTempFile("configuration", ".xml");
        confLocation.deleteOnExit();
        Files.saveString(String.format(
               CREATOR_SETUP,
               "file://" + descriptorLocation.getAbsoluteFile().toString()),
               confLocation);
        assertTrue("The configuration should exist",
                   descriptorLocation.getAbsoluteFile().exists());

        Configuration conf = new Configuration(new XStorage(confLocation));

        LuceneIndexDescriptor id = new LuceneIndexDescriptor(
                    conf.getSubConfiguration(DocumentCreator.CONF_DESCRIPTOR));
        assertNotNull("A descriptor should be created", id);

        DocumentCreator creator = new DocumentCreator(conf);
        creator.setSource(this);
        Payload processed = creator.next();
        assertNotNull("Payload should have a document", 
                      processed.getData(Payload.LUCENE_DOCUMENT));
        Document doc = (Document)processed.getData(Payload.LUCENE_DOCUMENT);
        assertTrue("The document should have some fields",
                   doc.getFields().size() > 0);
        for (Object fieldObject: doc.getFields()) {
            System.out.println(((Field)fieldObject).stringValue());
        }
        for (String fieldName: new String[]{"mystored", "freetext",
                                            IndexUtils.RECORD_FIELD}) {
            assertNotNull("The document should contain the field " + fieldName,
                          doc.getField(fieldName));
        }
    }

    /* ObjectFilter implementation */

    public boolean hasNext() {
        return true;
    }

    public Payload next() {
        try {
            return new Payload(new Record("dummy", "fooBase",
                                          SIMPLE_RECORD.getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void remove() {
    }

    public void setSource(Filter filter) {
    }

    public boolean pump() throws IOException {
        return next() != null;
    }

    public void close(boolean success) {
    }
}
