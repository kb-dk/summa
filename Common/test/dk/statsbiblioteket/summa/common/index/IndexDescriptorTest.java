package dk.statsbiblioteket.summa.common.index;

import java.text.ParseException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;
import org.w3c.dom.Node;

@SuppressWarnings({"ALL"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexDescriptorTest extends TestCase {
    public IndexDescriptorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static final String SIMPLE_DESCRIPTOR =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<IndexDescriptor version=\"1.0\">\n"
            + "    <groups>\n"
            + "        <group name=\"ti\">\n"
            + "            <alias name=\"tit\" lang=\"da\"/>\n"
            + "            <field name=\"title\"/>\n"
            + "            <field name=\"titel\"/>\n"
            + "        </group>\n"
            + "        <group name=\"au\">\n"
            + "            <field name=\"author\"/>\n"
            + "        </group>\n"
            + "    </groups>\n"
            + "    <fields>\n"
            + "        <field name=\"text\" indexed=\"true\" stored=\"true\"/>\n"
            + "        <field name=\"author\" parent=\"text\" indexed=\"true\" stored=\"true\" multiValued=\"true\" boost=\"2.0\" sortLocale=\"da\" inFreeText=\"true\" required=\"true\">\n"
            + "            <alias name=\"forfatter\" lang=\"da\"/>\n"
            + "        </field>\n"
            + "        <field name=\"title\" indexed=\"false\" stored=\"true\"/>\n"
            + "        <field name=\"titel\" indexed=\"false\" stored=\"true\"/>\n"
            + "        <field name=\"nostore\" parent=\"text\" indexed=\"true\" stored=\"false\"/>\n"
            + "    </fields>\n"
            + "    <defaultLanguage>no</defaultLanguage>\n"
            + "    <uniqueKey>superid</uniqueKey>\n"
            + "    <defaultSearchFields>superid nostore</defaultSearchFields>\n"
            + "    <QueryParser defaultOperator=\"AND\"/>\n"
            + "</IndexDescriptor>";

    public static final String SIMPLE_DESCRIPTOR_EMPTY =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<IndexDescriptor version=\"1.0\">\n"
            + "</IndexDescriptor>";

    public void testParse() throws Exception {
        IndexDescriptor<IndexField<Object, Object, Object>> id =
                new IndexDescriptor<IndexField<Object, Object, Object>>(
                        SIMPLE_DESCRIPTOR) {

            public IndexField createNewField() {
                return new IndexField<Object, Object, Object>();
            }

            public IndexField createNewField(Node node) throws ParseException {
                return new IndexField<Object, Object, Object>(node, this);
            }
        };
        assertEquals("The default language should be as specified",
                     "no", id.getDefaultLanguage());
        assertEquals("The unique key should be as specified",
                     "superid", id.getUniqueKey());
        assertEquals("The default fields should be as specified",
                     "superid nostore",
                     Strings.join(id.getDefaultFields(), " "));
        assertEquals("The default operator should be as specified",
                     IndexDescriptor.OPERATOR.and, id.getDefaultOperator());

        List<String> expectedGroups = Arrays.asList(new String[]{"ti", "au"});
        List<String> gotGroups = new ArrayList<String>(expectedGroups.size());
        for (Map.Entry<String, IndexGroup<IndexField<Object, Object, Object>>>
                entry: id.getGroups().entrySet()) {
            gotGroups.add(entry.getValue().getName());
        }
        assertTrue("All expected groups should be present",
                   gotGroups.containsAll(expectedGroups));
        assertTrue("No other groups should be present",
                   expectedGroups.containsAll(gotGroups));

        List<String> expectedFields = Arrays.asList(new String[]{
                // TODO: Extend with default groups
                "text", "author", "title", "titel", "nostore"});
        List<String> gotFields = new ArrayList<String>(expectedFields.size());
        for (Map.Entry<String, IndexField<Object, Object, Object>>
                entry: id.getFields().entrySet()) {
            gotFields.add(entry.getValue().getName());
        }
        assertTrue("All expected Fields should be present",
                   gotFields.containsAll(expectedFields));
        // It's okay to have more fields than expected (default fields)
    }

    public void testEmpty() throws Exception {
        IndexDescriptor id = new IndexDescriptor(SIMPLE_DESCRIPTOR_EMPTY) {

            public IndexField createNewField() {
                return new IndexField<Object, Object, Object>();
            }

            public IndexField createNewField(Node node) throws ParseException {
                return new IndexField<Object, Object, Object>(node, this);
            }
        };
        assertEquals("The default language should be en",
                     "en", id.getDefaultLanguage());
    }

    public void testGetField() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetFieldForIndexing() throws Exception {
        //TODO: Test goes here...
    }

    public void testSetGetDefaultLanguage() throws Exception {
        //TODO: Test goes here...
    }

    public void testSetGetUniqueKey() throws Exception {
        //TODO: Test goes here...
    }

    public void testSetGetDefaultFields() throws Exception {
        //TODO: Test goes here...
    }

    public void testSetGetDefaultOperator() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(IndexDescriptorTest.class);
    }
}
