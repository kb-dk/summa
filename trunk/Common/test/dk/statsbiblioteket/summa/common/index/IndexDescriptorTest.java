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

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexDescriptorTest extends TestCase {
    public IndexDescriptorTest(String name) {
        super(name);
    }

    // TODO: Test configuration of freetext and summa_default
    // TODO: Test fall-back to summa_default on inheritance

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
            + "        <field name=\"author_inv\" parent=\"text\" indexed=\"false\" stored=\"false\" multiValued=\"false\" boost=\"2.5\" sortLocale=\"de\" inFreeText=\"false\" required=\"false\"/>\n"
            + "        <field name=\"author_inherit\" parent=\"author\"/>\n"
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

            public IndexField<Object, Object, Object> createNewField() {
                return new IndexField<Object, Object, Object>();
            }

            public IndexField<Object, Object, Object> createNewField(Node node)
                    throws ParseException {
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

        List<String> expectedGroups = Arrays.asList("ti", "au");
        List<String> gotGroups = new ArrayList<String>(expectedGroups.size());
        for (Map.Entry<String, IndexGroup<IndexField<Object, Object, Object>>>
                entry: id.getGroups().entrySet()) {
            gotGroups.add(entry.getValue().getName());
        }
        assertTrue("All expected groups should be present",
                   gotGroups.containsAll(expectedGroups));
        assertTrue("No other groups should be present",
                   expectedGroups.containsAll(gotGroups));

        IndexGroup<IndexField<Object, Object, Object>> group =
                id.getGroups().get("ti");
        assertEquals("The group 'ti' should contain the right number of fields",
                     2, group.getFields().size());
        IndexField<Object, Object, Object> tiField =
                group.getField("title", null);
        assertNotNull("The field 'title' should be in group 'ti'", tiField);

        assertNotNull("Group look-up should work for alias 'tit'",
                      id.getGroup("tit"));
        assertEquals("Field look-up should work for alias 'forfatter'",
                     "author", id.getField("forfatter").getName());

        // TODO: Extend with default groups
        List<String> expectedFields = Arrays.asList("text", "author", "title",
                                                    "titel", "nostore");
        List<String> gotFields = new ArrayList<String>(expectedFields.size());
        for (Map.Entry<String, IndexField<Object, Object, Object>>
                entry: id.getFields().entrySet()) {
            gotFields.add(entry.getValue().getName());
        }
        // It's okay to have more fields than expected (default fields)
        assertTrue("All expected Fields should be present",
                   gotFields.containsAll(expectedFields));

        IndexField a = id.getField("author");
        assertNotNull("The field author should have a parent", a.getParent());
        String intro = "The field author should have the correct ";
        assertEquals(intro + "parent", "text", a.getParent().getName());
        assertEquals(intro + "indexed", true, a.isDoIndex());
        assertEquals(intro + "stored", true, a.isDoStore());
        assertEquals(intro + "multiValued", true, a.isMultiValued());
        assertEquals(intro + "boost", 2.0f, a.getBoost());
        assertEquals(intro + "sortLocale", "da", a.getSortLocale());
        assertEquals(intro + "inFreeText", true, a.isInFreetext());
        assertEquals(intro + "required", true, a.isRequired());
        // TODO: Talk to Kåre about this
        List<IndexAlias> aliases = a.getAliases();
        assertEquals(intro + "alias", "forfatter", aliases.get(0).getName());

        IndexField i = id.getField("author_inv");
        assertNotNull("The field author_inv should have a parent",
                      i.getParent());
        intro = "The field author_inv should have the correct ";
        assertEquals(intro + "parent", "text", i.getParent().getName());
        assertEquals(intro + "indexed", false, i.isDoIndex());
        assertEquals(intro + "stored", false, i.isDoStore());
        assertEquals(intro + "multiValued", false, i.isMultiValued());
        assertEquals(intro + "boost", 2.5f, i.getBoost());
        assertEquals(intro + "sortLocale", "de", i.getSortLocale());
        assertEquals(intro + "inFreeText", false, i.isInFreetext());
        assertEquals(intro + "required", false, i.isRequired());

        IndexField inh = id.getField("author_inherit");
        assertNotNull("The field author_inherit should have a parent",
                      inh.getParent());
        intro = "The field author_inherit should have the correct ";
        assertEquals(intro + "parent", "author", inh.getParent().getName());
        assertEquals(intro + "indexed", true, inh.isDoIndex());
        assertEquals(intro + "stored", true, inh.isDoStore());
        assertEquals(intro + "multiValued", true, inh.isMultiValued());
        assertEquals(intro + "boost", 2.0f, inh.getBoost());
        assertEquals(intro + "sortLocale", "da", inh.getSortLocale());
        assertEquals(intro + "inFreeText", true, inh.isInFreetext());
        assertEquals(intro + "required", true, inh.isRequired());
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


    public static Test suite() {
        return new TestSuite(IndexDescriptorTest.class);
    }
}
