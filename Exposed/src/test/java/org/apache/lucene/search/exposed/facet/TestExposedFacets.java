package org.apache.lucene.search.exposed.facet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.exposed.*;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TestExposedFacets extends TestCase {
  private ExposedHelper helper;
  private ExposedCache cache;
  private IndexWriter w = null;

  public TestExposedFacets(String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cache = ExposedCache.getInstance();
    helper = new ExposedHelper();
    helper.deleteIndex();
  }

  @Override

  public void tearDown() throws Exception {
    super.tearDown();
    if (w != null) {
      try {
        w.close();
      } catch (Exception e) {
        System.err.println("Exception auto-closing test IndexWriter");
        e.printStackTrace();
      }
    }
    cache.purgeAllCaches();
//    helper.close();
  }


  public static Test suite() {
    return new TestSuite(TestExposedFacets.class);
  }

  public static final String SIMPLE_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"5\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"id\" order=\"count\">\n" +
          "      <fields>\n" +
          "        <field name=\"id\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "    <group name=\"custom\" order=\"locale\" locale=\"da\" prefix=\"foo\">\n" +
          "      <fields>\n" +
          "        <field name=\"a\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "    <group name=\"random\" order=\"locale\" locale=\"da\" mincount=\"1\">\n" +
          "      <fields>\n" +
          "        <field name=\"evennull\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
//          "    <group name=\"multi\" order=\"index\" prefix=\"B\">\n" +
          "    <group name=\"multi\" order=\"index\" offset=\"-2\" prefix=\"F\">\n" +
          "      <fields>\n" +
          "        <field name=\"facet\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public void testSimpleFacet() throws Exception {
    final int DOCCOUNT = 1000; // Try with 5
    final int TERM_LENGTH = 20;
    final int MIN_SEGMENTS = 2;
    final List<String> FIELDS = Arrays.asList("a", "b");

    helper.createIndex(DOCCOUNT, FIELDS, TERM_LENGTH, MIN_SEGMENTS);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_40, ExposedHelper.EVEN,
        getAnalyzer());
//        new MockAnalyzer(new Random()new Random()));
    Query q = qp.parse("true");
    searcher.search(q, TopScoreDocCollector.create(10, false));
    long preMem = getMem();
    long facetStructureTime = System.currentTimeMillis();

    CollectorPoolFactory poolFactory = new CollectorPoolFactory(2, 4, 2);

    FacetRequest request = FacetRequest.parseXML(SIMPLE_REQUEST);
    CollectorPool collectorPool = poolFactory.acquire(reader, request);
    facetStructureTime = System.currentTimeMillis() - facetStructureTime;

    TagCollector collector;
    FacetResponse response = null;
    String sQuery = request.getQuery();
    for (int i = 0 ; i < 5 ; i++) {
      collector = collectorPool.acquire(sQuery);
      long countStart = System.currentTimeMillis();
      if (collector.getQuery() == null) { // Fresh collector
        searcher.search(q, collector);
//        collector.collectAllValid(reader);
        long countTime = System.currentTimeMillis() - countStart;
        collector.setCountTime(countTime);
      }
      response = collector.extractResult(request);
      if (collector.getQuery() != null) { // Cached count
        response.setCountingCached(true);
      }
      long totalTime = System.currentTimeMillis() - countStart;
      response.setTotalTime(totalTime);
/*      System.out.println("Collection #" + i + " for " + DOCCOUNT
          + " documents in "
          + getTime(System.currentTimeMillis()-countStart));*/
      collectorPool.release(sQuery, collector);
    }
    System.out.println("Document count = " + DOCCOUNT);
    System.out.println("Facet startup time = " + getTime(facetStructureTime));
    System.out.println("Mem usage: preFacet=" + preMem
        + " MB, postFacet=" + getMem() + " MB");
    if (response != null) {
      System.out.println(response.toXML());
    }
    reader.close();
  }

  public static final String DELETE_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest "
          +     "xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" "
          +     "maxtags=\"20\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"facet\" order=\"count\" mincount=\"1\">\n" +
          "      <fields>\n" +
          "        <field name=\"facet\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public void testDelete() throws IOException, XMLStreamException,
                                  ParseException {
    w = ExposedHelper.getWriter();
    {
      ExposedHelper.addDocument(w,
                                ExposedHelper.ID + ":1",
                                ExposedHelper.ALL + ":" + ExposedHelper.ALL,
                                "facet:tag_1");
      ExposedHelper.addDocument(w,
                                ExposedHelper.ID + ":2",
                                ExposedHelper.ALL + ":" + ExposedHelper.ALL,
                                "facet:tag_2");
      w.commit();
      FacetResponse response = getFacetResult(DELETE_REQUEST);
      assertEquals("A search for all should give the right number of hits",
                   2, response.getHits());
      List<String> tags = extractTags(response);
      assertEquals("The tags without deletions should match",
                   Arrays.asList("facet:tag_1(1)", "facet:tag_2(1)"), tags);
    }

    {
      w.deleteDocuments(new Term(ExposedHelper.ID, "1"));
      w.commit();
      FacetResponse response = getFacetResult(DELETE_REQUEST);
      assertEquals("A search for all after delete should give the right " +
                   "number of hits",
                   1, response.getHits());
      List<String> tags = extractTags(response);
      assertEquals("The tags after deletions should match",
                   Arrays.asList("facet:tag_2(1)"), tags);
    }

    {
      ExposedHelper.addDocument(w,
                                ExposedHelper.ID + ":1",
                                ExposedHelper.ALL + ":" + ExposedHelper.ALL,
                                "facet:tag_1");
      w.commit();
      FacetResponse response = getFacetResult(DELETE_REQUEST);
      assertEquals("A search for all after reinsert should give the right " +
                   "number of hits",
                   2, response.getHits());
      List<String> tags = extractTags(response);
      assertEquals("The tags after reinsert should match",
                   Arrays.asList("facet:tag_1(1)", "facet:tag_2(1)"), tags);
    }

    {
      ExposedHelper.addDocument(w,
                                ExposedHelper.ID + ":m",
                                ExposedHelper.ALL + ":" + ExposedHelper.ALL,
                                "facet:maruška šubic kovač");
      w.commit();
      FacetResponse response = getFacetResult(DELETE_REQUEST);
      assertEquals("A search for all after special tag should give the right "
                   + "number of hits",
                   3, response.getHits());
      List<String> tags = extractTags(response);
      assertEquals("The tags after reinsert should match",
                   Arrays.asList("facet:maruška šubic kovač(1)",
                                 "facet:tag_1(1)", "facet:tag_2(1)"),
                   tags);
    }

    {
      w.deleteDocuments(new Term(ExposedHelper.ID, "m"));
      w.commit();
      FacetResponse response = getFacetResult(DELETE_REQUEST);
      assertEquals("A search for all after delete of special document/tag " +
                   "should give the right number of hits",
                   2, response.getHits());
      List<String> tags = extractTags(response);
      assertEquals("The tags after deletion of document with special tag"
                   + " should match",
                   Arrays.asList("facet:tag_1(1)", "facet:tag_2(1)"), tags);
    }
    w.close();
}

public static final String MISCOUNT_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"5\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"recordBase\"><fields><field name=\"recordBase\" /></fields></group>\n" +
          "    <group name=\"lsubject\"><fields><field name=\"lsubject\" /></fields></group>\n" +
          "  </groups>\n" +
          "</facetrequest>";

  public void testMiscountEmpty()
                        throws IOException, XMLStreamException, ParseException {
    {
      w = ExposedHelper.getWriter();
      ExposedHelper.addDocument(w,
          ExposedHelper.ID + ":1",
          ExposedHelper.ALL + ":" + ExposedHelper.ALL,
          "recordBase:foo",
          "lsubject:bar",
          "lsubject:"
      );
      w.commit();
      w.close();
    }
    FacetResponse response = getFacetResult(MISCOUNT_REQUEST);
    assertEquals("A search for all should give the right number of hits",
        1, response.getHits());
    System.out.println(response.toXML());
    List<String> tags = extractTags(response);
    assertEquals("The tags without deletions should match",
        Arrays.asList("recordBase:foo(1)", "lsubject:(1)", "lsubject:bar(1)"),
        tags);
  }

  public static final String MISCOUNT_EXISTING =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"5\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"recordBase\"><fields><field name=\"recordBase\" /></fields></group>\n" +
          "    <group name=\"lsubject\"><fields><field name=\"lsubject\" /></fields></group>\n" +
          "    <group name=\"author_normalised\"><fields><field name=\"author_normalised\" /></fields></group>\n" +
          "    <group name=\"llfo\"><fields><field name=\"llfo\" /></fields></group>\n" +
          "  </groups>\n" +
          "</facetrequest>";


  // This uses an in house index at SB to locale a specific error
  // If this method is found outside of SB, I forgot to remove it before release
  // - Toke Eskildsen, te@statsbiblioteket.dk
/*  public void testMiscountExisting()
                        throws IOException, XMLStreamException, ParseException {
    File INDEX = new File(
        "/home/te/tmp/sumfresh/sites/sb/index/sb/20110830-142012/lucene");
    FacetResponse response = getFacetResult(
        MISCOUNT_EXISTING, INDEX, "freetext", "\"bog\"");
    System.out.println(response.toXML());
    List<String> tags = extractTags(response);
//    assertEquals("The tags without deletions should match",
//        Arrays.asList("recordBase:foo(1)", "lsubject:(1)", "lsubject:bar(1)"),
//        tags);
  }
  */


  public static final String MULTI_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"20\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"facet1\" order=\"count\" mincount=\"1\">\n" +
          "      <fields>\n" +
          "        <field name=\"facet1\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "    <group name=\"facet2\" order=\"count\" mincount=\"1\">\n" +
          "      <fields>\n" +
          "        <field name=\"facet2\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public void testMultiGroup() throws IOException, XMLStreamException, ParseException {
    w = ExposedHelper.getWriter();
    {
      ExposedHelper.addDocument(w,
          ExposedHelper.ID + ":1",
          ExposedHelper.ALL + ":" + ExposedHelper.ALL,
          "facet1:tag_1.1",
          "facet2:tag_2.1");
      ExposedHelper.addDocument(w,
          ExposedHelper.ID + ":2",
          ExposedHelper.ALL + ":" + ExposedHelper.ALL,
          "facet1:tag_1.2",
          "facet2:tag_2.2");
      w.commit();
      FacetResponse response = getFacetResult(MULTI_REQUEST);
      assertEquals("A search for all should give the right number of hits",
          2, response.getHits());
      List<String> tags = extractTags(response);
      assertEquals("The tags without deletions should match",
          Arrays.asList("facet1:tag_1.1(1)", "facet1:tag_1.2(1)",
              "facet2:tag_2.1(1)", "facet2:tag_2.2(1)"), tags);
    }

    {
      w.deleteDocuments(new Term(ExposedHelper.ID, "1"));
      w.commit();
      FacetResponse response = getFacetResult(MULTI_REQUEST);
      assertEquals("A search for all after delete should give the right " +
          "number of hits",
          1, response.getHits());
      List<String> tags = extractTags(response);
      assertEquals("The tags after deletions should match",
          Arrays.asList("facet1:tag_1.2(1)", "facet2:tag_2.2(1)"), tags);
    }
    w.close();
  }

  public void testMultiScale() throws Exception {
    // Only works with even numbers
    final int[] DOCS = new int[]{2, 4, 8, 10, 100, 1000, 10000, 24562};
    for (int docs: DOCS) {
      helper.deleteIndex();
      testMultiScale(docs);
    }
  }

  public static final String MULTI_SCALE_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"3\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"facet1\" order=\"count\" mincount=\"1\">\n" +
          "      <fields>\n" +
          "        <field name=\"facet1\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "    <group name=\"even\" order=\"count\" mincount=\"1\">\n" +
          "      <fields>\n" +
          "        <field name=\"facetEven\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  /*
   * Production has problems with tags that should not be in the result and
   * with unrealistically high count.
   */
  public void testMultiScale(int DOCS) throws Exception {
    final int HALF = DOCS / 2;
    // Let's set the buffer sizes very low to search for one-off errors
    FieldTermProvider.minSortCacheSize = 100;
    FieldTermProvider.maxSortCacheSize = 200;
    FieldTermProvider.iteratorCacheSize = 100;
    FieldTermProvider.iteratorReadAhead = 10;
    w = ExposedHelper.getWriter();
    { // Plain build
      for (int docID = 0 ; docID < DOCS ; docID++) {
        String even = (docID % 2 == 0 ? "true" : "false");
        ExposedHelper.addDocument(w,
            ExposedHelper.ID + ":" + Integer.toString(docID),
            ExposedHelper.ALL + ":" + ExposedHelper.ALL,
            ExposedHelper.EVEN + ":" + even,
            "facet1:tag_1." + Integer.toString(docID),
            "facetEven:" + even);
      }
      w.commit();
      FacetResponse response = getFacetResult(MULTI_SCALE_REQUEST);
      assertEquals("A search for all should give the right " +
          "number of hits with docs " + DOCS,
          DOCS, response.getHits());
      List<String> tags = extractTags(response);
      assertEquals("The tags after initial build should match with docs "
          + DOCS,
          Arrays.asList(
              //"facet1:tag_1.0(1)", "facet1:tag_1.1(1)", "facet1:tag_1.10(1)",
              "facetEven:false(" + HALF + ")", "facetEven:true(" + HALF + ")"),
          tags.subList(tags.size()-2, tags.size()));
      System.out.println("Docs " + DOCS + " before delete: " + response.getHits()
          + " with tags " + tags);
    }

    { // Delete
      w.deleteDocuments(new Term(ExposedHelper.EVEN, "true"));
      w.commit();
      FacetResponse response = getFacetResult(MULTI_SCALE_REQUEST);
      assertEquals("A search for all after delete should give the right " +
          "number of hits",
          DOCS / 2, response.getHits());
      List<String> tags = extractTags(response);
      assertEquals("The tags after deletions should match with docs " + DOCS,
          Arrays.asList(
          //    "facet1:tag_1.1(1)", "facet1:tag_1.101(1)", "facet1:tag_1.103(1)",
              "facetEven:false(" + HALF + ")"),
          tags.subList(tags.size()-1, tags.size()));
      System.out.println("Docs " + DOCS + "  after delete: " + response.getHits()
          + " with tags " + tags);
    }
    w.close();
  }

  private List<String> extractTags(FacetResponse facetResponse) {
    List<String> tags = new ArrayList<String>();
    for (FacetResponse.Group group: facetResponse.getGroups()) {
      String field = group.getFieldsStr();
      for (FacetResponse.Tag tag: group.getTags().getTags()) {
        tags.add(field + ":" + tag.getTerm() + "(" + tag.getCount() + ")");
      }
    }
    return tags;
  }

  private FacetResponse getFacetResult(String facetRequest)
                        throws IOException, ParseException, XMLStreamException {
    return getFacetResult(facetRequest, ExposedHelper.INDEX_LOCATION,
        ExposedHelper.ALL, ExposedHelper.ALL);
  }

  private FacetResponse getFacetResult(
      String facetRequest, File index, String defaultField, String query)
                        throws IOException, ParseException, XMLStreamException {
    IndexReader reader = ExposedIOFactory.getReader(index);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_40, defaultField, getAnalyzer());
    Query q = qp.parse(query);
    searcher.search(q, TopScoreDocCollector.create(10, false));

    CollectorPoolFactory poolFactory = new CollectorPoolFactory(2, 4, 2);
    FacetRequest request = FacetRequest.parseXML(facetRequest);
    CollectorPool collectorPool = poolFactory.acquire(reader, request);

    TagCollector collector;
    FacetResponse response = null;
    String sQuery = request.getQuery();
    collector = collectorPool.acquire(sQuery);
    long countStart = System.currentTimeMillis();
    searcher.search(q, collector);
    long countTime = System.currentTimeMillis() - countStart;
    collector.setCountTime(countTime);
    response = collector.extractResult(request);
    if (collector.getQuery() != null) { // Cached count
      response.setCountingCached(true);
    }
    long totalTime = System.currentTimeMillis() - countStart;
    response.setTotalTime(totalTime);
    collectorPool.release(sQuery, collector);
    return response;
  }

  private Analyzer getAnalyzer() {
    return new WhitespaceAnalyzer(Version.LUCENE_40);
//    return new MockAnalyzer(new Random(), MockTokenizer.WHITESPACE, false);
  }
  
  public static final String GROUP_REQUEST_ABC =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"20\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"multi\" order=\"count\">\n" +
          "      <fields>\n" +
          "        <field name=\"a\" />\n" +
          "        <field name=\"b\" />\n" +
          "        <field name=\"c\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public static final String GROUP_ABC_EXPECTED = // TODO: Avoid hardcoding
      "      <tag count=\"3\" term=\"c0\" />\n" +
          "      <tag count=\"3\" term=\"c1\" />\n" +
          "      <tag count=\"2\" term=\"b0\" />\n" +
          "      <tag count=\"2\" term=\"b1\" />\n" +
          "      <tag count=\"2\" term=\"b2\" />\n" +
          "      <tag count=\"1\" term=\"a0\" />\n" +
          "      <tag count=\"1\" term=\"a1\" />\n" +
          "      <tag count=\"1\" term=\"a2\" />\n" +
          "      <tag count=\"1\" term=\"a3\" />\n" +
          "      <tag count=\"1\" term=\"a4\" />\n" +
          "      <tag count=\"1\" term=\"a5\" />";
  public void testMultiFacet() throws Exception {
    final int DOCCOUNT = 6;
    FacetRequest request = FacetRequest.parseXML(GROUP_REQUEST_ABC);
    FacetResponse response = testMultiFacetHelper(request, DOCCOUNT);
    //System.out.println(response.toXML());
    assertTrue("The response should contain \n" + GROUP_ABC_EXPECTED
        + ", but was\n" + response.toXML(),
        response.toXML().contains(GROUP_ABC_EXPECTED));
  }

  public static final String GROUP_REQUEST_AB =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"20\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"multi\" order=\"count\">\n" +
          "      <fields>\n" +
          "        <field name=\"a\" />\n" +
          "        <field name=\"b\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public static final String GROUP_AB_EXPECTED = // TODO: Avoid hardcoding
      "      <tag count=\"2\" term=\"b0\" />\n"
      + "      <tag count=\"1\" term=\"a0\" />\n"
      + "      <tag count=\"1\" term=\"a1\" />";
  public void testMultiFacet2() throws Exception {
    final int DOCCOUNT = 2;
    FacetRequest request = FacetRequest.parseXML(GROUP_REQUEST_AB);
    FacetResponse response = testMultiFacetHelper(request, DOCCOUNT);
    //System.out.println(response.toXML());
    assertTrue("The response should contain \n" + GROUP_AB_EXPECTED
        + ", but was\n" + response.toXML(),
        response.toXML().contains(GROUP_AB_EXPECTED));
    System.out.println(response.toXML());
  }

  public static final String MINCOUNT_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"20\" mincount=\"0\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"multi\" order=\"index\" offset=\"-5\" mincount=\"2\">\n" +
          "      <fields>\n" +
          "        <field name=\"a\" />\n" +
          "        <field name=\"b\" />\n" +
          "        <field name=\"c\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public void testMinCountFacet() throws Exception {
    final int DOCCOUNT = 6;
    FacetRequest request = FacetRequest.parseXML(MINCOUNT_REQUEST);
//    System.out.println("Requesting\n" + request.toXML());

    FacetResponse response = testMultiFacetHelper(request, DOCCOUNT);

    assertFalse("The result should not contain any 'count=\"1\"' but was\n"
        + response.toXML(), response.toXML().contains("count=\"1\""));
    assertTrue("The result should contain a 'count=\"2\"' but had\n"
        + response.toXML(), response.toXML().contains("count=\"2\""));
    assertTrue("The result should contain a 'count=\"3\"' but had\n"
        + response.toXML(), response.toXML().contains("count=\"3\""));
//    System.out.println(response.toXML());

  }

  public static final String MINCOUNT_PREFIX_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"20\" mincount=\"0\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"multi\" order=\"index\" offset=\"-5\" mincount=\"1\" prefix=\"a\">\n" +
          "      <fields>\n" +
          "        <field name=\"a\" />\n" +
          "        <field name=\"b\" />\n" +
          "        <field name=\"c\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public void testMinCountPrefixFacet() throws Exception {
    final int DOCCOUNT = 6;
    FacetRequest request = FacetRequest.parseXML(MINCOUNT_PREFIX_REQUEST);
    System.out.println("Requesting\n" + request.toXML());

    FacetResponse response = testMultiFacetHelper(request, DOCCOUNT);

    assertTrue("The result should contain a 'count=\"1\"' but had\n"
        + response.toXML(), response.toXML().contains("count=\"1\""));
//    System.out.println(response.toXML());

  }

  private FacetResponse testMultiFacetHelper(
      FacetRequest request, int doccount) throws Exception {
    ExposedHelper helper = new ExposedHelper();
    File location = helper.buildMultiFieldIndex(doccount);

    DirectoryReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_40, ExposedHelper.ALL,
        getAnalyzer());
    Query q = qp.parse(ExposedHelper.ALL);

    CollectorPoolFactory poolFactory = new CollectorPoolFactory(2, 4, 2);

    CollectorPool collectorPool = poolFactory.acquire(reader, request);

    TagCollector collector = collectorPool.acquire(null);
    searcher.search(q, collector);
    FacetResponse response = collector.extractResult(request);
    collectorPool.release(null, collector);

    reader.close();
    helper.close(); // Cleanup
    return response;
  }

  public static final String SOLR_COMPARISON_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"5\" mincount=\"1\">\n" +
          "  <query>replacable</query>\n" +
          "  <groups>\n" +
          "    <group name=\"someFacet\" order=\"count\">\n" +
          "      <fields>\n" +
          "        <field name=\"f1000000_5_t\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  /*
  This attempts to re-create the Solr test for faceting found at
  https://issues.apache.org/jira/browse/SOLR-475?focusedCommentId=12650071&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_12650071
  as details are sparse in the JIRA issue, comparisons cannot be made directly.
   */
  public void testSolrComparisonFacet() throws Exception {
    final int DOCCOUNT = 50000;
    ExposedSettings.priority = ExposedSettings.PRIORITY.memory;

    File LOCATION = new File("/home/te/projects/index50M");
    if (!LOCATION.exists()) {
      System.err.println("No index at " + LOCATION + ". A test index with " +
          DOCCOUNT + " documents will be build");
      helper.createFacetIndex(DOCCOUNT);
      LOCATION = ExposedHelper.INDEX_LOCATION;
    }
    IndexReader reader = ExposedIOFactory.getReader(LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    System.out.println("Index = " + LOCATION.getAbsolutePath()
        + " (" + reader.maxDoc() + " documents)\n");

    String FIELD = "hits10000";

    QueryParser qp = new QueryParser(
        Version.LUCENE_40, FIELD,
        getAnalyzer());
    Query q = qp.parse("true");
    searcher.search(q, TopScoreDocCollector.create(10, false));
    System.out.println("Used heap after loading index and performing a " +
        "simple search: " + getMem() + " MB\n");
    long preMem = getMem();
    long facetStructureTime = System.currentTimeMillis();

    CollectorPoolFactory poolFactory = new CollectorPoolFactory(2, 4, 2);

    FacetRequest request = FacetRequest.parseXML(SOLR_COMPARISON_REQUEST);
    CollectorPool collectorPool = poolFactory.acquire(reader, request);
    facetStructureTime = System.currentTimeMillis() - facetStructureTime;

/*    long bMem = getMem();
    ExposedCache.getInstance().transitiveReleaseCaches(true);
    System.out.println("Releasing leaf caches freed "
        + (getMem() - bMem) + " MB");
*/

    TagCollector collector;
    FacetResponse response = null;
    request.setQuery(FIELD + ":true");
    String sQuery = null;// request.getQuery(); No caching
    for (int i = 0 ; i < 5 ; i++) {
      long countStart = System.currentTimeMillis();
      collector = collectorPool.acquire(sQuery);
      if (collector.getQuery() == null) { // Fresh collector
        searcher.search(q, collector);
        long countTime = System.currentTimeMillis() - countStart;
        collector.setCountTime(countTime);
      }
      response = collector.extractResult(request);
      if (collector.getQuery() != null) { // Cached count
        response.setCountingCached(true);
      }
      long totalTime = System.currentTimeMillis() - countStart;
      collectorPool.release(sQuery, collector);
      response.setTotalTime(totalTime);
      System.out.println("Facet collection #" + i + " for " + DOCCOUNT
          + " documents in "
          + getTime(System.currentTimeMillis()-countStart));
      Thread.sleep(50); // Real world simulation or cheating?
    }
    System.out.println("Document count = " + DOCCOUNT);
    System.out.println("Facet startup time = " + getTime(facetStructureTime));
    long fMem = getMem();
    System.out.println("Mem usage: preFacet = " + preMem
        + " MB, postFacet = " + fMem + " MB. "
        + "Facet overhead (approximate) = " + (fMem - preMem) + " MB.");
    System.out.println(CollectorPoolFactory.getLastFactory());
    if (response != null) {
      System.out.println(response.toXML());
    }
  }

  private void dumpStats() {
    System.out.println(CollectorPoolFactory.getLastFactory());
    System.out.println(ExposedCache.getInstance());
  }

  public static final String LOOKUP_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest "
          + "xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" "
          + "maxtags=\"5\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"custom\" order=\"index\" locale=\"da\" "
          + "offset=\"myoffset\" prefix=\"myprefix\">\n" +
          "      <fields>\n" +
          "        <field name=\"a\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public void testIndexLookup() throws Exception {
    final int DOCCOUNT = 200000;
    final int TERM_LENGTH = 20;
    final int MIN_SEGMENTS = 2;
    final List<String> FIELDS = Arrays.asList("a", "b");
    helper.createIndex(DOCCOUNT, FIELDS, TERM_LENGTH, MIN_SEGMENTS);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_40, ExposedHelper.EVEN,
        getAnalyzer());
    Query q = qp.parse("true");
    searcher.search(q, TopScoreDocCollector.create(10, false));
    System.out.println(
        "Index opened and test-sort performed, commencing faceting...");
    long preMem = getMem();

    CollectorPoolFactory poolFactory = new CollectorPoolFactory(2, 4, 2);

    FacetResponse response = null;
    assertTrue("There must be at least 10 documents in the index",
        DOCCOUNT >= 10);
    final int span = DOCCOUNT / 10;
    long firstTime = -1;
    long subsequentMin = 0;
    int subCount = 0;
    long poolTime = -1;
    for (char prefix = 'A' ; prefix < 'X' ; prefix++) {
      for (int offset = -span ; offset < span ; offset += span / 2) {
        FacetRequest request = FacetRequest.parseXML(
            LOOKUP_REQUEST.replace("myprefix", Character.toString(prefix)).
                replace("myoffset", Integer.toString(offset)));
        if (firstTime == -1) {
          poolTime = System.currentTimeMillis();
        }
        CollectorPool collectorPool = poolFactory.acquire(reader, request);
        if (firstTime == -1) {
          poolTime = System.currentTimeMillis() - poolTime;
          System.out.println("Facet structure build finished in "
              + getTime(poolTime) + ". Running requests...\n");
        }

        long countStart = System.currentTimeMillis();
        String sQuery = request.getQuery();
        TagCollector collector = collectorPool.acquire(sQuery);
        if (collector.getQuery() == null) { // Fresh collector
          searcher.search(q, collector);
          long countTime = System.currentTimeMillis() - countStart;
          collector.setCountTime(countTime);
        }
        response = collector.extractResult(request);
        assertNotNull("Extracting XML response should work", response.toXML());
        if (collector.getQuery() != null) { // Cached count
          response.setCountingCached(true);
        }
        long totalTime = System.currentTimeMillis() - countStart;
        if (firstTime == -1) {
          firstTime = totalTime;
        } else {
          subCount++;
          subsequentMin = Math.min(subsequentMin, totalTime);
        }
        response.setTotalTime(totalTime);
/*        System.out.println("Collection for prefix " + prefix + " and offset "
            + offset + " for " + DOCCOUNT + " documents took "
            + (System.currentTimeMillis()-countStart) + "ms");*/
        collectorPool.release(sQuery, collector);
      }
    }
    System.out.println("Document count = " + DOCCOUNT);
    System.out.println("Facet structure build time = " + getTime(poolTime));
    System.out.println("First facet call = " + getTime(firstTime));
    System.out.println("Min of " + subCount + " subsequent calls = "
        + getTime(subsequentMin));
    System.out.println("Mem usage: preFacet=" + preMem
        + " MB, postFacet=" + getMem() + " MB");
    if (response != null) {
      System.out.println(response.toXML());
    }

  }

  public void testBasicSearch() throws IOException, ParseException {

    File LOCATION = new File("/home/te/projects/index1M");
    if (!LOCATION.exists()) {
      final int DOCCOUNT = 20000;
      final int TERM_LENGTH = 20;
      final int MIN_SEGMENTS = 2;
      final List<String> FIELDS = Arrays.asList("a", "b");
      System.err.println("No index at " + LOCATION + ". A test index with " +
          DOCCOUNT + " documents will be build at "
          + ExposedHelper.INDEX_LOCATION );
      helper.createIndex(DOCCOUNT, FIELDS, TERM_LENGTH, MIN_SEGMENTS);
      LOCATION = ExposedHelper.INDEX_LOCATION;
    }

    final String field = "facet";
    final String term = "A";

    assertTrue("No index at " + LOCATION.getAbsolutePath()
        + ". Please build a test index (you can use one from on of the other" +
        " JUnit tests in TestExposedFacets) and correct the path",
        LOCATION.exists());

    IndexReader reader = ExposedIOFactory.getReader(LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_40, field,
        getAnalyzer());
    Query q = qp.parse(term);
    System.out.println(q.toString());
    String sQuery = field + ":" + term;

    TopDocsCollector collector = TopScoreDocCollector.create(10, false);
    searcher.search(q, collector);
    assertTrue("There should be some hits for '" + sQuery + "'",
        collector.getTotalHits() >0);

  }

  // MB
  private long getMem() {
    System.gc();
    return (Runtime.getRuntime().totalMemory()
        - Runtime.getRuntime().freeMemory()) / 1048576;
  }

  public static final String SCALE_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"5\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"sorted\" order=\"index\" locale=\"da\" mincount=\"0\">\n" +
          "      <fields>\n" +
          "        <field name=\"a\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "    <group name=\"count\" order=\"count\" mincount=\"1\">\n" +
          "      <fields>\n" +
          "        <field name=\"a\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "    <group name=\"multi\" order=\"count\">\n" +
          "      <fields>\n" +
          "        <field name=\"facet\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  /*
  Performs sorted search, faceting and index lookup
   */
  public void testScale() throws Exception {
    long totalTime = -System.currentTimeMillis();

    ExposedSettings.priority = ExposedSettings.PRIORITY.memory;

    File LOCATION = new File("/home/te/projects/index1M");
    if (!LOCATION.exists()) {
      final int DOCCOUNT = 100000;
      final int TERM_LENGTH = 20;
      final int MIN_SEGMENTS = 2;
      final List<String> FIELDS = Arrays.asList("a", "b");
      System.err.println("No index at " + LOCATION + ". A test index with " +
          DOCCOUNT + " documents will be build at "
          + ExposedHelper.INDEX_LOCATION );
      helper.createIndex(DOCCOUNT, FIELDS, TERM_LENGTH, MIN_SEGMENTS);
      LOCATION = ExposedHelper.INDEX_LOCATION;
    }
    assertTrue("No index at " + LOCATION.getAbsolutePath()
        + ". Please build a test index (you can use one from on of the other" +
        " JUnit tests in TestExposedFacets) and correct the path",
        LOCATION.exists());

    IndexReader reader = ExposedIOFactory.getReader(LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_40, ExposedHelper.EVEN,
        getAnalyzer());
    Query q = qp.parse("true");
    String sQuery = "even:true";

    StringWriter sw = new StringWriter(10000);
    sw.append("Index = " + LOCATION.getAbsolutePath() + " (" + reader.maxDoc()
        + " documents)\n");

    searcher.search(q, TopScoreDocCollector.create(10, false));
    sw.append("Used heap after loading index and performing a simple search: "
        + getMem() + " MB\n");
    sw.append("Maximum possible memory (Runtime.getRuntime().maxMemory()): "
        + Runtime.getRuntime().maxMemory() / 1048576 + " MB\n");
    System.out.println("Index " + LOCATION
        + " opened and test search performed successfully");

    sw.append("\n");

    // Tests
    CollectorPoolFactory poolFactory = new CollectorPoolFactory(6, 4, 2);

    qp = new QueryParser(
        Version.LUCENE_40, ExposedHelper.EVEN,
        getAnalyzer());
    q = qp.parse("true");
    sQuery = "even:true";
    testScale(poolFactory, searcher, q, sQuery, sw);

/*  TODO: Check if this disabling works with the tests without re-computing
    dumpStats();
    System.out.println("Disabling re-open optimization");
    ExposedCache.getInstance().transitiveReleaseCaches(true);
    dumpStats();
  */

    qp = new QueryParser(
        Version.LUCENE_40, ExposedHelper.MULTI,
        getAnalyzer());
    q = qp.parse("A");
    sQuery = "multi:A"; // Strictly it's "facet:A", but that is too confusing
    testScale(poolFactory, searcher, q, sQuery, sw);


    System.out.println("**************************\n");
    
    sw.append("\nUsed memory with sort, facet and index lookup structures " +
        "intact: " + getMem() + " MB\n");
    totalTime += System.currentTimeMillis();
    sw.append("Total test time: " + getTime(totalTime));

    System.out.println("");
    System.out.println(sw.toString());
  }

  private void testScale(CollectorPoolFactory poolFactory,
      IndexSearcher searcher, Query query, String sQuery, StringWriter result)
                                        throws IOException, XMLStreamException {
    System.out.println("- Testing sorted search for " + sQuery);
    testSortedSearch(searcher, "b", query, sQuery, null, result);
    result.append("\n");

    // Faceting
    System.out.println("- Testing faceting for " + sQuery);
    testFaceting(poolFactory, searcher, query, sQuery, result);

    // Index lookup
    System.out.println("- Testing index lookup for " + sQuery);
    TestIndexLookup(poolFactory, searcher, query, sQuery, result);

  }

  public static final String FACET_SCALE_SIMPLE_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest " +
              "xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" " +
              "maxtags=\"5\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"many\" order=\"count\">\n" +
          "      <fields>\n" +
          "        <field name=\"a\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public void testFacetScale()
      throws XMLStreamException, IOException, ParseException {
    FacetMapFactory.defaultImpl = FacetMapFactory.IMPL.pass1packed;
    //CodecProvider.setDefaultCodec("Standard");

    long totalTime = -System.currentTimeMillis();
    ExposedSettings.debug = true;
    ExposedSettings.priority = ExposedSettings.PRIORITY.memory;

    //new ExposedHelper().close(); // Deletes old index
    final File LOCATION = ExposedHelper.INDEX_LOCATION;
    if (!LOCATION.exists()|| LOCATION.listFiles().length == 0) {
      final int DOCCOUNT = 1000000;
      final int TERM_LENGTH = 20;
      final int MIN_SEGMENTS = 2;
      final List<String> FIELDS = Arrays.asList("a");
      System.err.println("No index at " + LOCATION + ". A test index with " +
          DOCCOUNT + " documents will be build at "
          + ExposedHelper.INDEX_LOCATION );
      helper.createIndex(DOCCOUNT, FIELDS, TERM_LENGTH, MIN_SEGMENTS);
    }
/*    if (!LOCATION.exists() || LOCATION.listFiles().length == 0) {
      System.out.println("No index at " + LOCATION.getAbsolutePath()
                          + ". Please build a test index (you can use one from on of the other" +
                          " JUnit tests in TestExposedFacets) and correct the path. " +
                          "Alternatively: Delete the folder and a test index will be created" +
                          "automatically");
        return;
    }*/

    IndexReader reader = ExposedIOFactory.getReader(LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_40, ExposedHelper.EVEN,
        getAnalyzer());
    Query q = qp.parse("true");

    StringWriter sw = new StringWriter(10000);
    sw.append("\nIndex = " + LOCATION.getAbsolutePath() + " (" + reader.maxDoc()
              + " documents)\n");

    searcher.search(q, TopScoreDocCollector.create(10, false));
    sw.append("Used heap after loading index and performing a simple search: "
        + getMem() + " MB\n");

    sw.append("\n");

    // Tests
    CollectorPoolFactory poolFactory = new CollectorPoolFactory(6, 4, 2);

    qp = new QueryParser(
        Version.LUCENE_40, ExposedHelper.EVEN,
        getAnalyzer());
    q = qp.parse("true");
    String sQuery = "even:true";

    System.out.println("- Testing faceting for " + sQuery);
    testFaceting(
        poolFactory, searcher, q, sQuery, sw, FACET_SCALE_SIMPLE_REQUEST);
    System.out.println(sw.toString());
    System.out.println("Instances: " + ExposedTuple.instances);
    totalTime += System.currentTimeMillis();
    System.out.println("Total test time: " + totalTime + "ms");
  }

/*  public void testP() {
    final int COUNT = 60000000;
    ArrayList<ExposedTuple> tuples = new ArrayList<ExposedTuple>(COUNT / 100);
    long tupleTime = -System.currentTimeMillis();
    BytesRef b = new BytesRef("ss");
    for (int i = 0 ; i < COUNT ; i++) {
      b.copy("ss");
      tuples.add(new ExposedTuple("foo", b, 0, 0));
      if (i % (COUNT / 100) == 0) {
        tuples.clear();
        System.out.print(".");
      }
    }
    tupleTime += System.currentTimeMillis();
    System.out.println("\nGot " + ExposedUtil.time("tuples", COUNT, tupleTime));
    tuples.clear();
  }
  */
  private void testFaceting(
      CollectorPoolFactory poolFactory, IndexSearcher searcher, Query q,
      String sQuery, StringWriter result)
                                        throws XMLStreamException, IOException {
    testFaceting(poolFactory, searcher, q, sQuery, result, SCALE_REQUEST);
  }
  private void testFaceting(
      CollectorPoolFactory poolFactory, IndexSearcher searcher, Query q,
      String sQuery, StringWriter result, String requestString)
                                        throws XMLStreamException, IOException {
    IndexReader reader = searcher.getIndexReader();
    long facetStructureTime = -System.currentTimeMillis();

    FacetResponse response = null;
    int DOCCOUNT = reader.maxDoc();
    assertTrue("There must be at least 10 documents in the index",
        DOCCOUNT >= 10);
    final int span = DOCCOUNT / 10;
    long firstTime = -1;
    long subsequents = 0;
    int subCount = 0;
    long poolTime = -1;
    for (int i = 0 ; i < 5 ; i++) {
      FacetRequest request = FacetRequest.parseXML(requestString);
      request.setQuery(sQuery);
      if (firstTime == -1) {
        poolTime = System.currentTimeMillis();
      }
      CollectorPool collectorPool = poolFactory.acquire(reader, request);
      if (firstTime == -1) {
        poolTime = System.currentTimeMillis() - poolTime;
        result.append("Facet pool acquisition for " +
            "for \"" + sQuery + "\" with structure " + request.getGroupKey()
            + ": " + getTime(poolTime) + "\n");
      }

      long countStart = System.currentTimeMillis();
      TagCollector collector = collectorPool.acquire(null); // No caching
      if (collector.getQuery() == null) { // Fresh collector
        searcher.search(q, collector);
        long countTime = System.currentTimeMillis() - countStart;
        collector.setCountTime(countTime);
      }
      response = collector.extractResult(request);
      assertNotNull("Extracting XML response should work", response.toXML());
      if (collector.getQuery() != null) { // Cached count
        response.setCountingCached(true);
      }
      long totalTime = System.currentTimeMillis() - countStart;
      if (firstTime == -1) {
        firstTime = totalTime;
        result.append("First faceting for " + sQuery + ": "
            + getTime(totalTime) + "\n");
      } else {
        subCount++;
        subsequents += totalTime;
      }
      response.setTotalTime(totalTime);
/*        System.out.println("Collection for prefix " + prefix + " and offset "
            + offset + " for " + DOCCOUNT + " documents took "
            + (System.currentTimeMillis()-countStart) + "ms");*/
      collectorPool.release(null, collector); // No caching
    }
    result.append("Subsequent " + subCount + " faceting calls (count caching " +
        "disabled) response times: " + getTime(subsequents / subCount) + "\n");
    assertNotNull("There should be a response", response);
    result.append(response.toXML()).append("\n");
  }

  private void TestIndexLookup(
      CollectorPoolFactory poolFactory, IndexSearcher searcher, Query q,
      String sQuery, StringWriter result)
                                        throws XMLStreamException, IOException {
    IndexReader reader = searcher.getIndexReader();
    long facetStructureTime = -System.currentTimeMillis();

    FacetResponse response = null;
    int DOCCOUNT = reader.maxDoc();
    assertTrue("There must be at least 10 documents in the index",
        DOCCOUNT >= 10);
    final int span = DOCCOUNT / 10;
    long firstTime = -1;
    long subsequents = 0;
    int subCount = 0;
    long poolTime = -1;
    for (char prefix = 'A' ; prefix < 'X' ; prefix++) {
      for (int offset = -span ; offset < span ; offset += span / 2) {
        FacetRequest request = FacetRequest.parseXML(
            LOOKUP_REQUEST.replace("myprefix", Character.toString(prefix)).
                replace("myoffset", Integer.toString(offset)));
        request.setQuery(sQuery);
        if (firstTime == -1) {
          poolTime = System.currentTimeMillis();
        }
        CollectorPool collectorPool = poolFactory.acquire(reader, request);
        if (firstTime == -1) {
          poolTime = System.currentTimeMillis() - poolTime;
          result.append("Initial lookup pool request (might result in structure" +
              " building): " + getTime(poolTime) + "\n");
        }

        long countStart = System.currentTimeMillis();
        TagCollector collector = collectorPool.acquire(sQuery);
        if (collector.getQuery() == null) { // Fresh collector
          searcher.search(q, collector);
          long countTime = System.currentTimeMillis() - countStart;
          collector.setCountTime(countTime);
        }
        response = collector.extractResult(request);
        assertNotNull("Extracting XML response should work", response.toXML());
        if (collector.getQuery() != null) { // Cached count
          response.setCountingCached(true);
        }
        long totalTime = System.currentTimeMillis() - countStart;
        if (firstTime == -1) {
          firstTime = totalTime;
          result.append("First index lookup for \"" + sQuery + "\": "
              + getTime(firstTime) + "\n");
        } else {
          subCount++;
          subsequents += totalTime;
        }
        response.setTotalTime(totalTime);
/*        System.out.println("Collection for prefix " + prefix + " and offset "
            + offset + " for " + DOCCOUNT + " documents took "
            + (System.currentTimeMillis()-countStart) + "ms");*/
        collectorPool.release(sQuery, collector);
      }
    }
    result.append("Subsequent " + subCount + " index lookups average " +
        "response times: " + getTime(subsequents / subCount) + "\n");
    result.append(response.toXML()).append("\n");
  }

  public void testDumpNaturalSortedSearch() throws IOException, ParseException {

    File LOCATION = new File("/home/te/projects/index10M");
    if (!LOCATION.exists()) {
      final int DOCCOUNT = 20000;
      final int TERM_LENGTH = 20;
      final int MIN_SEGMENTS = 2;
      final List<String> FIELDS = Arrays.asList("a", "b");
      System.err.println("No index at " + LOCATION + ". A test index with " +
          DOCCOUNT + " documents will be build at "
          + ExposedHelper.INDEX_LOCATION );
      helper.createIndex(DOCCOUNT, FIELDS, TERM_LENGTH, MIN_SEGMENTS);
      LOCATION = ExposedHelper.INDEX_LOCATION;
    }
    StringWriter sw = new StringWriter();
    IndexReader reader = ExposedIOFactory.getReader(LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_40, ExposedHelper.EVEN,
        getAnalyzer());
    Query q = qp.parse("true");
    String sQuery = "even:true";
    testSortedSearch(searcher, "a", q, sQuery, null, sw);
    System.out.println(sw);
  }

  public static final String UPDATE_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"5\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"id\" order=\"count\">\n" +
          "      <fields>\n" +
          "        <field name=\"id\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public void testIndexUpdate() throws Exception {
    final int TERM_LENGTH = 20;
    final int MIN_SEGMENTS = 2;
    final List<String> FIELDS = Arrays.asList("a", "b");
    final String ID4 = "00000004";

    helper.createIndex(3, FIELDS, TERM_LENGTH, MIN_SEGMENTS);
    DirectoryReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    CollectorPoolFactory poolFactory = new CollectorPoolFactory(2, 4, 2);
    {
      FacetResponse response = performFaceting(
        poolFactory, reader, "true", UPDATE_REQUEST);
      assertFalse("The initial response should not contain the id " + ID4,
                  response.toXML().contains(
                    "<tag count=\"1\" term=\"" + ID4 + "\" />"));
    }

    helper.createIndex(5, FIELDS, TERM_LENGTH, MIN_SEGMENTS);
    DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
    if (newReader != null) {
      reader.close();
      reader = newReader;
    }
    {
      FacetResponse response = performFaceting(
        poolFactory, reader, "true", UPDATE_REQUEST);
      assertTrue("The response after update should contain the id " + ID4,
                 response.toXML().contains(
                   "<tag count=\"1\" term=\"" + ID4 + "\" />"));
    }
    reader.close();
  }

  private FacetResponse performFaceting(
    CollectorPoolFactory poolFactory, IndexReader reader, String query,
    String requestXML) throws ParseException, IOException, XMLStreamException {
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
      Version.LUCENE_40, ExposedHelper.EVEN, getAnalyzer());
    Query q = qp.parse(query);
    searcher.search(q, TopScoreDocCollector.create(10, false));
    long preMem = getMem();
    long facetStructureTime = System.currentTimeMillis();

    FacetRequest request = FacetRequest.parseXML(requestXML);
    CollectorPool collectorPool = poolFactory.acquire(reader, request);
    facetStructureTime = System.currentTimeMillis() - facetStructureTime;

    TagCollector collector;
    FacetResponse response;
    String sQuery = request.getQuery();
    collector = collectorPool.acquire(sQuery);
    long countStart = System.currentTimeMillis();
    if (collector.getQuery() == null) { // Fresh collector
      searcher.search(q, collector);
//        collector.collectAllValid(reader);
      long countTime = System.currentTimeMillis() - countStart;
      collector.setCountTime(countTime);
    }
    response = collector.extractResult(request);
    if (collector.getQuery() != null) { // Cached count
      response.setCountingCached(true);
    }
    long totalTime = System.currentTimeMillis() - countStart;
    response.setTotalTime(totalTime);
/*      System.out.println("Collection #" + i + " for " + DOCCOUNT
          + " documents in "
          + getTime(System.currentTimeMillis()-countStart));*/
    collectorPool.release(sQuery, collector);
    System.out.println("Facet startup time = " + getTime(facetStructureTime));
    System.out.println("Facet count time = "+ getTime(response.getTotalTime()));
    System.out.println("Mem usage: preFacet=" + preMem
        + " MB, postFacet=" + getMem() + " MB");
    return response;
  }

  private void testSortedSearch(IndexSearcher searcher, String field, Query q,
                                String sQuery, Locale locale,
                                StringWriter result) throws IOException {
    // Sorted search
    long firstSearch = -System.currentTimeMillis();
    ExposedFieldComparatorSource exposedFCS =
        new ExposedFieldComparatorSource(searcher.getIndexReader(), locale);
    Sort sort = new Sort(new SortField(field, exposedFCS));
    TopFieldDocs docs = searcher.search(q, null, 5, sort);
    firstSearch += System.currentTimeMillis();

    result.append("First natural order sorted search for \""+ sQuery
        + "\" with " + docs.totalHits + " hits: "
        + getTime(firstSearch) + "\n");

    long subSearchMS = 0;
    final int RUNS = 5;
    final int MAXHITS = 5;
    TopFieldDocs topDocs = null;
    for (int i = 0 ; i < RUNS ; i++) {
      subSearchMS -= System.currentTimeMillis();
      topDocs = searcher.search(q, MAXHITS, sort);
      subSearchMS += System.currentTimeMillis();
    }

    result.append("Subsequent " + RUNS
        + " sorted searches average response time: "
        + getTime(subSearchMS / RUNS) + "\n");
    for (int i = 0 ; i < Math.min(topDocs.totalHits, MAXHITS) ; i++) {
      int docID = topDocs.scoreDocs[i].doc;
      result.append(String.format(
          "Hit #%d was doc #%d with field " + field + " %s\n",
          i, docID,
          ((BytesRef)((FieldDoc)topDocs.scoreDocs[i]).fields[0]).
              utf8ToString()));
    }
  }

  private String getTime(long ms) {
    if (ms < 2999) {
      return ms + " ms";
    }
    long seconds = Math.round(ms / 1000.0);
    long minutes = seconds / 60;
    return String.format("%d:%02d minutes", minutes, seconds - (minutes * 60));
  }
}
