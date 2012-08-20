package org.apache.solr.exposed;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.solr.exposed.ExposedIndexLookupParams.*;

public class TestExposedLookupQueryComponent extends SolrTestCaseJ4 {
  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig-exposed.xml", "schema-exposed.xml");
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    clearIndex();
    commit();
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  public void testSimpleLookup() throws Exception {
    addContent(Arrays.asList(
      "c", "c", "c", "b", "b", "b", "b", "d", "e", "f", "f", "g"));
    String response = h.query(req(
        "qt", "lookup",
        "q", "*:*",
        ELOOKUP, "true",
        ELOOKUP_SORT, ELOOKUP_SORT_BYINDEX,
        ELOOKUP_DELTA, Integer.toString(-2),
        ELOOKUP_LENGTH, Integer.toString(5),
        ELOOKUP_FIELD, "text"));
//    System.out.println(response.replace(">", ">\n"));

    assertEquals("The extracted terms should be as expected for\n" + response,
                 "b(4) c(3) d(1) e(1) f(2)", getTerms(response));
//    System.out.println(response.replace("><", ">\n<"));
  }

  @Test
  public void testLocaleLookup() throws Exception {
    addContent(Arrays.asList("æble", "østers", "å", "ærbar", "ødsel"));
    {
      String response = h.query(req(
        "qt", "lookup",
        "q", "*:*",
        ELOOKUP, "true",
        ELOOKUP_DELTA, Integer.toString(0),
        ELOOKUP_FIELD, "text"));

      assertEquals("The extracted terms should be in index order for\n"
                   + response.replace(">", ">\n"),
                   "å(1) æble(1) ærbar(1) ødsel(1) øster(1)",
                   getTerms(response));
    }

    {
      String response = h.query(req(
        "qt", "lookup",
        "q", "*:*",
        ELOOKUP, "true",
        ELOOKUP_DELTA, Integer.toString(0),
        ELOOKUP_SORT, "locale",
        ELOOKUP_SORT_LOCALE_VALUE, "da",
        ELOOKUP_FIELD, "text"));
//    System.out.println(response.replace(">", ">\n"));

      assertEquals("The extracted terms should be in locale(da)-order for\n"
                   + response.replace(">", ">\n"),
                   "æble(1) ærbar(1) ødsel(1) øster(1) å(1)",
                   getTerms(response));
    }
  }

  private String getTerms(String response) { // Beware: Not XML parsing at all
    final Pattern RESPONSE = Pattern.compile(
      "<lst name=\"terms\">(.+?)</lst>", Pattern.DOTALL);
    Matcher reMatch = RESPONSE.matcher(response);
    if (!reMatch.find()) {
      return "";
    }
    response = reMatch.group(1);
    //  <int name="å">1</int><int name="æble">1</int>...

    final Pattern TERMS = Pattern.compile(
      "<int name=\"([^\"]+)\">([0-9]+)</int>");
    StringWriter result = new StringWriter();
    Matcher matcher = TERMS.matcher(response);
    boolean subsequent = false;
    while (matcher.find()) {
      if (subsequent) {
        result.append(" ");
      }
      subsequent = true;
      result.append(matcher.group(1));
      result.append("(").append(matcher.group(2)).append(")");
    }
    return result.toString();
  }

  private void addContent(List<String> terms) {
    for (int id = 0 ; id < terms.size(); id++) {
      assertU(adoc(
        "id", Integer.toString(id),
        "text", terms.get(id)
      ));
    }
    assertU(commit());
  }

}
