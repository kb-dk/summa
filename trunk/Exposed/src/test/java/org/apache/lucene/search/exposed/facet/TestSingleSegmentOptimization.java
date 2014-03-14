package org.apache.lucene.search.exposed.facet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.exposed.ExposedCache;
import org.apache.lucene.search.exposed.ExposedHelper;
import org.apache.lucene.search.exposed.ExposedIOFactory;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.util.Version;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

public class TestSingleSegmentOptimization extends TestCase {
  private ExposedHelper helper;
  private ExposedCache cache;
  private IndexWriter w = null;

  public TestSingleSegmentOptimization(String name) {
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
    helper.close();
  }


  public static Test suite() {
    return new TestSuite(TestSingleSegmentOptimization.class);
  }

  private Analyzer getAnalyzer() {
    return new WhitespaceAnalyzer(Version.LUCENE_46);
//    return new MockAnalyzer(new Random(), MockTokenizer.WHITESPACE, false);
  }

  // MB
  private long getMem() {
    System.gc();
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
  }

  public static final String SCALE_MANY_DOCS_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
      "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"5\">\n" +
      "  <query>even:true</query>\n" +
      "  <groups>\n" +
      "    <group name=\"count\" order=\"count\" mincount=\"1\">\n" +
      "      <fields>\n" +
      "        <field name=\"a\" />\n" +
      "      </fields>\n" +
      "    </group>\n" +
      "  </groups>\n" +
      "</facetrequest>";
  private final File PREFERRED_ROOT = new File("/home/te/tmp/");
  public void testScaleOptimizedFacet() throws Exception {
    long totalTime = -System.currentTimeMillis();

    ExposedSettings.priority = ExposedSettings.PRIORITY.memory;
    ExposedSettings.debug = true;
    FacetMapFactory.defaultImpl = FacetMapFactory.IMPL.pass2;

    final int DOCCOUNT = 5000000;
    final int TERM_LENGTH = 20;
    final List<String> FIELDS = Arrays.asList("a");
    final File LOCATION = PREFERRED_ROOT.exists() ?  new File(PREFERRED_ROOT, "index50M") : ExposedHelper.INDEX_LOCATION;

    if (!LOCATION.exists()) {
      System.err.println("No index at " + LOCATION + ". A test index with " + DOCCOUNT + " documents will be build");
      helper.createIndex(LOCATION, DOCCOUNT, FIELDS, 1, TERM_LENGTH, 1, 1, 1);
      helper.optimize(LOCATION);
    }
    assertTrue("No index at " + LOCATION.getAbsolutePath() + ". Please build a test index (you can use one from on of " +
               "the other JUnit tests in TestExposedFacets) and correct the path", LOCATION.exists());

    IndexReader reader = ExposedIOFactory.getReader(LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(Version.LUCENE_46, ExposedHelper.EVEN, getAnalyzer());
    Query q = qp.parse("true");

    StringWriter sw = new StringWriter(10000);
    sw.append("Index = " + LOCATION.getAbsolutePath() + " (" + reader.maxDoc() + " documents)\n");

    searcher.search(q, TopScoreDocCollector.create(10, false));
    sw.append("Used heap after loading index and performing a simple search: " + getMem() + " MB\n");
    sw.append("Maximum possible memory (Runtime.getRuntime().maxMemory()): "
              + Runtime.getRuntime().maxMemory() / 1048576 + " MB\n");
    System.out.println("Index " + LOCATION + " opened and test search performed successfully");

    sw.append("\n");

    // Tests
    CollectorPoolFactory poolFactory = new CollectorPoolFactory(6, 0, 1);

    qp = new QueryParser(Version.LUCENE_46, ExposedHelper.EVEN, getAnalyzer());
    q = qp.parse("true");
    String sQuery = "even:true";

    testFaceting(poolFactory, searcher, q, sQuery, sw, SCALE_MANY_DOCS_REQUEST);

    System.out.println("**************************\n");

    sw.append("\nUsed memory with sort, facet and index lookup structures intact: " + getMem() + " MB\n");
    totalTime += System.currentTimeMillis();
    sw.append("Total test time: " + getTime(totalTime));

    System.out.println("");
    System.out.println(sw.toString());
    System.out.println(ExposedCache.getInstance());
    System.out.println(CollectorPoolFactory.getLastFactory());
  }
  public void testScaleOptimizedSparseFacet() throws Exception {
    String avgTrue1= testScaleOptimizedSparseFacet(true);
    String avgFalse1= testScaleOptimizedSparseFacet(false);
    String avgTrue2= testScaleOptimizedSparseFacet(true);
    String  avgFalse2= testScaleOptimizedSparseFacet(false);
    System.out.println(String.format("sparse1=%s, multi2=%s, sparse2==%s, multi2==%s",
                                     avgTrue1, avgFalse1, avgTrue2, avgFalse2));
  }

  public String testScaleOptimizedSparseFacet(boolean sparse) throws Exception {
    long totalTime = -System.currentTimeMillis();

    ExposedSettings.useSparseCollector = sparse;
    ExposedSettings.priority = ExposedSettings.PRIORITY.memory;
    ExposedSettings.debug = true;
    FacetMapFactory.defaultImpl = FacetMapFactory.IMPL.pass2;

    final String QUERY = ExposedHelper.EVERY50 + ":true";
    final int DOCCOUNT = 2000000;
    final int TERM_LENGTH = 20;
    final List<String> FIELDS = Arrays.asList("a");
    final File LOCATION = PREFERRED_ROOT.exists() ?
        new File(PREFERRED_ROOT, "indexSparse") :
        ExposedHelper.INDEX_LOCATION;

    if (!LOCATION.exists()) {
      System.err.println("No index at " + LOCATION + ". A test index with " + DOCCOUNT + " documents will be build");
      helper.createIndex(LOCATION, DOCCOUNT, FIELDS, 1, TERM_LENGTH, 1, 1, 1);
      helper.optimize(LOCATION);
    }
    assertTrue("No index at " + LOCATION.getAbsolutePath() + ". Please build a test index (you can use one from on of " +
               "the other JUnit tests in TestExposedFacets) and correct the path", LOCATION.exists());

    IndexReader reader = ExposedIOFactory.getReader(LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(Version.LUCENE_46, ExposedHelper.EVEN, getAnalyzer());
    Query q = qp.parse(QUERY);

    StringWriter sw = new StringWriter(10000);
    sw.append("Index = " + LOCATION.getAbsolutePath() + " (" + reader.maxDoc() + " documents)\n");

    TopScoreDocCollector collector = TopScoreDocCollector.create(10, false);
    searcher.search(q, collector);
    assertEquals("There should be 1/50 maxhits for test search", DOCCOUNT/50, collector.getTotalHits());
    sw.append("Used heap after loading index and performing a simple search: " + getMem() + " MB\n");
    sw.append("Maximum possible memory (Runtime.getRuntime().maxMemory()): "
              + Runtime.getRuntime().maxMemory() / 1048576 + " MB\n");

    sw.append("\n");

    // Tests
    CollectorPoolFactory poolFactory = new CollectorPoolFactory(6, 0, 1);

    qp = new QueryParser(Version.LUCENE_46, ExposedHelper.EVEN, getAnalyzer());
    q = qp.parse(QUERY);
    String sQuery = QUERY;

    testFaceting(poolFactory, searcher, q, sQuery, sw, SCALE_MANY_DOCS_REQUEST);

    System.out.println("**************************\n");

    sw.append("\nUsed memory with sort, facet and index lookup structures intact: " + getMem() + " MB\n");
    totalTime += System.currentTimeMillis();
    sw.append("Total test time: " + getTime(totalTime));

    System.out.println("");
    System.out.println(sw.toString());
    System.out.println(ExposedCache.getInstance());
    System.out.println(CollectorPoolFactory.getLastFactory());

    ExposedCache.getInstance().purgeAllCaches();
    CollectorPoolFactory.getLastFactory().purgeAllCaches();
    return uglyHackMS;
  }

  private String uglyHackMS = ""; // TODO: Remove this
  private void testFaceting(CollectorPoolFactory poolFactory, IndexSearcher searcher, Query q, String sQuery,
                            StringWriter result, String requestString) throws XMLStreamException, IOException {
    final int RUNS = 5;

    IndexReader reader = searcher.getIndexReader();
    FacetResponse response = null;
    int DOCCOUNT = reader.maxDoc();
    assertTrue("There must be at least 10 documents in the index at ", DOCCOUNT >= 10);
    final int span = DOCCOUNT / 10;
    long firstTime = -1;
    long subsequents = 0;
    int subCount = 0;
    long poolTime = -1;
    for (int i = 0 ; i < RUNS ; i++) {
      FacetRequest request = FacetRequest.parseXML(requestString);
      request.setQuery(sQuery);
      if (firstTime == -1) {
        poolTime = System.currentTimeMillis();
      }
      CollectorPool collectorPool = poolFactory.acquire(reader, request);
      if (firstTime == -1) {
        poolTime = System.currentTimeMillis() - poolTime;
        result.append("Facet pool acquisition for \"" + sQuery + "\" with structure "
                      + request.getGroupKey() + ": " + getTime(poolTime) + "\n");
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
        result.append("First faceting for " + sQuery + ": " + getTime(totalTime) + "\n");
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
    uglyHackMS = getTime(subsequents / subCount);
  }

  private String getTime(long ms) {
    if (ms < 2999) {
      return ms + " ms";
    }
    long seconds = Math.round(ms / 1000.0);
    long minutes = seconds / 60;
    return String.format("%d:%02d minutes", minutes, seconds - minutes*60);
  }
}
