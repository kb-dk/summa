package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.XProperties;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Structure Tester.
 *
 * @author <Authors name>
 * @since <pre>08/01/2008</pre>
 * @version 1.0
 */
public class StructureTest extends TestCase {
    public StructureTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void dumpConfigXML() throws Exception {
        XProperties xp = new XProperties();

        ArrayList<XProperties> facets =
                new ArrayList<XProperties>(5);
        XProperties facet = new XProperties();
        facet.put(Structure.CONF_FACET_NAME, "foo");
        facet.put(Structure.CONF_FACET_TAGS_MAX, "100");
        facets.add(facet);
        xp.put(Structure.CONF_FACETS, facets);

        System.out.println(xp.getXStream().toXML(xp));
    }

    public void testBasicConfigParse() throws Exception {
        String config =
                "<xproperties>\n"
                + "<xproperties>\n"
                + "  <entry>\n"
                + "    <key>summa.facet.facets</key>\n"
                + "    <value class=\"list\">\n"
                + "      <xproperties>\n"
                + "        <entry>\n"
                + "          <key>summa.facet.tags.max</key>\n"
                + "          <value class=\"int\">100</value>\n"
                + "        </entry>\n"
                + "        <entry>\n"
                + "          <key>summa.facet.name</key>\n"
                + "          <value class=\"string\">foo</value>\n"
                + "        </entry>\n"
                + "      </xproperties>\n"
                + "    </value>\n"
                + "  </entry>\n"
                + "</xproperties>"
                + "</xproperties>";
        File tmp = File.createTempFile("config", ".xml");
        Files.saveString(config, tmp);
        assertTrue("The saved configuration should exist", tmp.exists());
        Configuration conf = Configuration.load(tmp.toString());
        assertTrue("Should contain a list",
                   conf.valueExists(Structure.CONF_FACETS));
        //noinspection unchecked
        List facets = (List<XProperties>)conf.get(Structure.CONF_FACETS);
        assertEquals("The list should have the right length", 1, facets.size());
    }

    public void testConfig() throws Exception {
        URL confLocation = Resolver.getURL(
                "dk/statsbiblioteket/summa/facetbrowser/StructureTestData.xml");
        Configuration conf = Configuration.load(confLocation.getFile());
        Structure structure = new Structure(conf);

        assertEquals("The number of FacetStructures should match",
                     3, structure.getFacets().size());

        Structure.FacetStructure mini = structure.getFacets().get("mini");
        assertEquals("The mini should contain default value for max tags",
                     Structure.DEFAULT_TAGS_MAX, mini.getMaxTags());
        Structure.FacetStructure bad = structure.getFacets().get("bad_sort");
        assertEquals("The bad sort should be corrected",
                     Structure.DEFAULT_FACET_SORT_TYPE, bad.getSortType());

        Structure.FacetStructure full = structure.getFacets().get("full");
        assertEquals("The fields should be as expected",
                     "foo, bar",
                     Strings.join(Arrays.asList(full.getFields()), ", "));
        assertEquals("maxTags should be as expected", 87, full.getMaxTags());
        assertEquals("wantedTags should be as expected",
                     30, full.getWantedTags());
        assertEquals("sortType should be as expected",
                     Structure.SORT_ALPHA, full.getSortType());
        assertEquals("locale shuld be as expected", "de", full.getLocale());
    }

    public void testGetFacets() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetFields() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetLocale() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetMaxTags() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetName() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetSortType() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetWantedTags() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetRequestFacet() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(StructureTest.class);
    }
}
