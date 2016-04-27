package org.apache.lucene.search.exposed.facet;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

public class TestFacetRequest extends TestCase {
  public TestFacetRequest(String name) {
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

  public static Test suite() {
    return new TestSuite(TestFacetRequest.class);
  }

  public static final String SAMPLE =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
          "<!--\n" +
          "  Sample facet request.\n" +
          "-->\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"30\" mincount=\"1\">\n" +
          "    <query>freetext:\"foo &lt;&gt; bar &amp; zoo\"</query>\n" +
          "    <groups>\n" +
          "        <group name=\"title\" order=\"locale\" locale=\"da\">\n" +
          "            <fields>\n" +
          "                <field name=\"title\"/>\n" +
          "                <field name=\"subtitle\"/>\n" +
          "            </fields>\n" +
          "        </group>\n" +
          "        <group name=\"author\" order=\"index\">\n" +
          "            <fields>\n" +
          "                <field name=\"name\"/>\n" +
          "            </fields>\n" +
          "        </group>\n" +
          "        <group name=\"material\" order=\"count\" mincount=\"0\" maxtags=\"-1\">\n" +
          "            <fields>\n" +
          "                <field name=\"materialetype\"/>\n" +
          "                <field name=\"type\"/>\n" +
          "            </fields>\n" +
          "        </group>\n" +
          "        <group name=\"place\">\n" +
          "            <fields>\n" +
          "                <field name=\"position\"/>\n" +
          "            </fields>\n" +
          "        </group>\n" +
          "    </groups>\n" +
          "</facetrequest>";

  public void testParseXML() throws Exception {
//    System.out.println(FacetRequest.parseXML(SAMPLE).toXML());
    // TODO: Check identity
  }


}
