package org.apache.solr.exposed;

import org.apache.lucene.index.Term;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;

import static org.apache.solr.exposed.ExposedFacetParams.*;

public class TestExposedFacetQueryComponent extends SolrTestCaseJ4 {
  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig-exposed.xml","schema-exposed.xml");
  }

  @After
  @Override
  public void tearDown() throws Exception {
    close();
    super.tearDown();
  }

  String t(int tnum) {
    return String.format(Locale.US, "%08d", tnum);
  }

  void createIndex(int nTerms) {
    assertU(delQ("*:*"));
    for (int i=0; i<nTerms; i++) {
      assertU(adoc("id", Float.toString(i), proto.field(), t(i) ));
    }
    assertU(optimize()); // squeeze out any possible deleted docs
  }

  Term proto = new Term("field_s","");
  SolrQueryRequest req; // used to get a searcher
  void close() {
    if (req!=null) req.close();
    req = null;
  }

  public void testSimple() throws Exception {
    addContent(100);
    String response = h.query(req(
        "qt", "exprh",
        "q", "modulo2_s:mod1",
        EFACET, "true",
        EFACET_MINCOUNT, "1",
        EFACET_FIELD, "modulo5_s"));

    assertTrue("The response '" + response
        + "' should contain a count of 10 for tag mod1",
        response.contains("<int name=\"mod1\">10</int>"));

//    System.out.println(response.replace("><", ">\n<"));
  }

  public void testPathField() throws Exception {
    addHierarchicalContent(100, 3, 3);
    String response = h.query(req(
        "qt", "exprh",
        "q", "*:*",
        EFACET, "true",
        EFACET_MINCOUNT, "1",
        EFACET_FIELD, "path_ss"));

    assertTrue("The facet response '" + response
        + "' should contain tags for field 'path'",
        response.contains("<lst name=\"path_ss\">"));

//    System.out.println(response.replace("><", ">\n<"));
  }

  @Test
  public void testSorted() throws Exception {
    addContent(100);
    String response = h.query(req(
        "qt", "exprh",
        "q", "modulo2_s:mod1",
        EFACET, "true",
        EFACET_MINCOUNT, "1",
        EFACET_SORT, EFACET_SORT_INDEX,
        EFACET_FIELD, "modulo5_s"));

    int previousPos = -1;
    for (int i = 0 ; i < 5 ; i++) {
      String mod = "mod" + i;
      int newPos = response.indexOf("<int name=\"" + mod + "\">");
      assertTrue("The index for '" + mod + "' should be larger than the " +
          "previous index " + previousPos + " but was " + newPos,
          previousPos < newPos);
      previousPos = newPos;
    }
  }

  public void testHierarchical() throws Exception {
    addHierarchicalContent(100, 3, 3);
    String response = h.query(req(
        "qt", "exprh",
        "q", "*:*",
        EFACET, "true",
        EFACET_MINCOUNT, "1",
        EFACET_HIERARCHICAL, "true",
        EFACET_LIMIT, "2",
        EFACET_HIERARCHICAL_LEVELS, "2",
        EFACET_FIELD, "path_ss"));

/*    assertTrue("The facet response '" + response
        + "' should contain tags for field 'path'",
        response.contains("<lst name=\"path_ss\">"));
  */
    System.out.println(response.replace("><", ">\n<"));
  }

  public void testGen() {
    addHierarchicalContent(2, new int[]{1, 2}, new int[]{2, 3});
  }

  // We have a problem here: Document per document index update with adoc is
  // too slow for proper scale testing
  public void disabledtestSmallScalePerformance() throws Exception {
//    System.out.println(
//        "Changing level to SEVERE for Solrcore and UpdateRequestProcessor");
    // Logging each update is a major performance drain
    java.util.logging.Logger.getLogger(SolrCore.log.getName()).
        setLevel(Level.SEVERE);
    java.util.logging.Logger.getLogger(UpdateRequestProcessor.class.getName()).
        setLevel(Level.SEVERE);

    long buildTime = -System.currentTimeMillis();
    addHierarchicalContent(2600, new int[]{1, 1}, new int[]{26, 100});
    buildTime += System.currentTimeMillis();

    long facetTime = -System.currentTimeMillis();
    String response = h.query(req(
        "qt", "exprh",
        "q", "*:*",
        "indent", "on",
        EFACET, "true",
        EFACET_MINCOUNT, "1",
        EFACET_HIERARCHICAL, "true",
        EFACET_LIMIT, "10",
        EFACET_HIERARCHICAL_LEVELS, "2",
        EFACET_FIELD, "path_ss"));
    facetTime += System.currentTimeMillis();
    System.out.println(response);
    System.out.println(
        "Build time: " + buildTime + "ms, facet time: " + facetTime + "ms");
  }

  // Mimics facet_samples.tcl from SOLR-2412

  private void addHierarchicalContent(int docs, int[] elements, int[] uniques) {
    int[] countersExp = new int[elements.length];
    Arrays.fill(countersExp, 1);
    int[] countersPivot = new int[elements.length];
    Arrays.fill(countersPivot, 1);

    if (uniques == null) {
      uniques = new int[elements.length];
      Arrays.fill(uniques, Integer.MAX_VALUE);
    }

    int paths = 1;
    int levels = 0;
    for (int element: elements) {
      paths *= element; // exp
      levels += element;
    }
    String s = paths == 1 ? "_s" : "_ss";
    String pathHeader = "path" + s;

    String[] values = new String[2 + (paths + levels) * 2 ];
    int pos = 0;
    values[pos++] = "id"; values[pos++] = "dummy";
    // path_s
    for (int heading = 0 ; heading < paths ; heading++) {
      values[pos++] = pathHeader; values[pos++] = "dummy";
    }
    // level_s
    for (int level = 0 ; level < elements.length ; level++) {
      for (int i = 0 ; i < elements[level] ; i++) {
        values[pos++] = "level" + level + s; values[pos++] = "dummy";
      }
    }

    long genTime = 0;
    long addTime = 0;

    for (int docID = 1 ; docID <= docs ; docID++) {
      genTime -= System.nanoTime();
      values[1] = Integer.toString(docID);
      pos = addExp(values, elements, uniques, countersExp);
      addPivot(values, pos, elements, uniques, countersPivot);
      genTime += System.nanoTime();

      addTime -= System.nanoTime();
      assertU(adoc(values));
      addTime += System.nanoTime();
/*      for (String val: values) {
        System.out.print(" " + val);
      }
      System.out.println("");*/
      if (docID >>> 10 << 10 == docID) {
        System.out.println(docID + "/" + docs + ". Gentime: " + genTime/1000000
            + "ms, addtime: " + addTime/1000000 + "ms");
      }
    }
    assertU(commit());
  }

  private int addExp(String[] values,
                     int[] elements, int[] uniques, int[] counters) {
    return addExp(values, 3, "", 0, elements, uniques, counters);
  }
  private int addExp(String[] values, int pos, String path, int level,
                      int[] elements, int[] uniques, int[] counters) {
    if (level == elements.length) {
      values[pos] = path;
      return pos+2;
    }
    if (level > 0) {
      path = path + "/";
    }
    for (int e = 0 ; e < elements[level] ; e++) {
      String val = "L" + level + "_T" + incGet(counters, uniques, level);
      pos = addExp(
          values, pos, path + val, level+1, elements, uniques, counters);
    }
    return pos;
  }

  private void addPivot(String[] values, int pos,
                        int[] elements, int[] uniques, int[] counters) {
    int combos = 1;
    for (int level = 0 ; level < elements.length ; level++) {
      int c = elements[level];
      combos *= c;
      String path = "L" + level + "_T";
      for (int tag = 0 ; tag < combos ; tag++) {
        values[pos] = path + incGet(counters, uniques, level);
        pos += 2;
      }
    }
  }

  private int incGet(int[] counters, int[] uniques, int level) {
    final int result = counters[level];
    counters[level]++;
    if (counters[level] > uniques[level]) {
      counters[level] = 1;
    }
    return result;
  }

  public void testSort() throws Exception {
    assertU(adoc("id", "1",
        "level1_s", "1A",
        "level2_ss", "2A",
        "level2_ss", "2B"
    ));
    assertU(adoc("id", "2",
        "level1_s", "2A",
        "level2_ss", "2A",
        "level2_ss", "2B",
        "level2_ss", "2C"
    ));
    assertU(commit());
    String response = h.query(req(
        "q", "*:*",
        "indent", "on",
        "rows", "1",
        "fl", "id",
        "facet", "on",
        "facet.pivot", "level1_s,level2_ss",
        "facet.limit", "1",
        "facet.sort", "count"
    ));
    System.out.println(response);
  }


  // The TagExtractor used optimized extraction of relevant Tags when
  // non-count-order is used. This code needs to be updated to support reverse
/*  @Test
  public void testSortedReverse() throws Exception {
    addContent(100);
    String response = h.query(req(
        "qt", "exprh",
        "q", "modulo2_s:mod1",
        EFACET, "true",
        EFACET_MINCOUNT, "1",
        EFACET_SORT, EFACET_SORT_INDEX,
        EFACET_REVERSE, "true",
        EFACET_FIELD, "modulo5_s"));

    int previousPos = Integer.MAX_VALUE;
    for (int i = 0 ; i < 5 ; i++) {
      String mod = "mod" + i;
      int newPos = response.indexOf("<int name=\"" + mod + "\">");
      assertTrue("The index for '" + mod + "' should be smaller than the " +
          "previous index " + previousPos + " but was " + newPos,
          previousPos > newPos);
      previousPos = newPos;
    }
  }*/

  private void addContent(int num) {
    for (int id = 0 ; id < num ; id++) {
      assertU(adoc("id", Integer.toString(id),
          "number_s", "num" + Integer.toString(id),
          "text", Integer.toString(id),
          "many_ws", Integer.toString(id),
          "modulo2_s", "mod" + Integer.toString(id % 2),
          "modulo5_s", "mod" + Integer.toString(id % 5)));
    }
    assertU(commit());
  }

  /**
   * @param docs  the number of documents to add.
   * @param paths the number of paths for each document.
   * @param depth the depth of the paths to add. Last entry in the hierarchy
   *              will be the document id.
   */
  private void addHierarchicalContent(int docs, int paths, int depth) {
    StringBuffer sb = new StringBuffer(100);
    for (int doc = 0 ; doc < docs ; doc++) {
      ArrayList<String> content = new ArrayList<String>(2 + paths * 2);
      content.add("id");
      content.add(Integer.toString(doc));
      for (int path = 0 ; path < paths ; path++) {
        sb.setLength(0);
        for (int d = 0 ; d < depth ; d++) {
          if (d != 0) {
            sb.append("/");
          }
        // 0_0/0_1/0_2
        // 1_0/1_1/1_2
          if (d == depth-1) {
            sb.append(doc);
          } else {
            sb.append(path).append("_").append(d);
          }
        }
        content.add("path_ss");
        content.add(sb.toString());
      }
      content.add("modulo2_s");
      content.add("mod" + Integer.toString(doc % 2));
      content.add("modulo5_s");
      content.add("mod" + Integer.toString(doc % 5));
      String[] array = new String[content.size()];
      array = content.toArray(array);
      assertU(adoc(array));
    }
    assertU(commit());
  }
}
