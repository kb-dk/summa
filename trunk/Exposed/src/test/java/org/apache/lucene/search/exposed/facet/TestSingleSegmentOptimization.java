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

  public void testScaleOptimizedSparseCollector() throws Exception {
    System.out.println("Warning: This test _must_ be executed as the only test in the current JVM invocation to produce"
                       + " results comparable to testScaleOptimizedMultiCollector");
    testScaleOptimizedSpecificCollector(true, "sparse(" + TagCollectorSparse.DEFAULT_SPARSE_FACTOR + ")");
  }

  public void testScaleOptimizedSparseNoneCollector() throws Exception {
    System.out.println("Warning: This test _must_ be executed as the only test in the current JVM invocation to produce"
                       + " results comparable to testScaleOptimizedMultiCollector");
    ExposedSettings.forceSparseCollector = true;
    double oldFactor = TagCollectorSparse.DEFAULT_SPARSE_FACTOR;
    TagCollectorSparse.DEFAULT_SPARSE_FACTOR = 0.0;

    testScaleOptimizedSpecificCollector(true, "sparse(none)");

    ExposedSettings.forceSparseCollector = false;
    TagCollectorSparse.DEFAULT_SPARSE_FACTOR = oldFactor;
  }

  public void testScaleOptimizedSparseAllCollector() throws Exception {
    System.out.println("Warning: This test _must_ be executed as the only test in the current JVM invocation to produce"
                       + " results comparable to testScaleOptimizedMultiCollector");
    ExposedSettings.forceSparseCollector = true;
    double oldFactor = TagCollectorSparse.DEFAULT_SPARSE_FACTOR;
    TagCollectorSparse.DEFAULT_SPARSE_FACTOR = 1.0;

    testScaleOptimizedSpecificCollector(true, "sparse(all)");

    ExposedSettings.forceSparseCollector = false;
    TagCollectorSparse.DEFAULT_SPARSE_FACTOR = oldFactor;
  }

  public void testScaleOptimizedMultiCollector() throws Exception {
    System.out.println("Warning: This test _must_ be executed as the only test in the current JVM invocation to produce"
                       + " results comparable to testScaleOptimizedFacetCollector");
    testScaleOptimizedSpecificCollector(false, "multi");
  }

  public void testScaleOptimizedSpecificCollector(boolean useSparse, String designation) throws Exception {
    final int RUNS = 5;
    final int[] FRACTIONS = new int[]{2, 5, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 5000};
    final IndexSearcher searcher = getTagCollectorTestSearcher();
    final CollectorPoolFactory poolFactory = new CollectorPoolFactory(6, 0, 1);

    StringBuffer sb = new StringBuffer();
    sb.append("Index with " + COLLECTOR_TEST_DOCS + " documents, tag collector: " + designation + "\n");
    for (int run = 1 ; run <= RUNS ; run++) {
      sb.append("fraction  count  extract  clear  total (test run #").append(run).append(")\n");
      for (int fraction: FRACTIONS) {
        printTestScaleOptimizedSparseFacet(sb, poolFactory, searcher, useSparse, run, fraction);
      }
      sb.append("\n");
      System.out.println("\n********* Temporary output\n" + sb);
    }
    System.out.println(ExposedCache.getInstance());
    System.out.println(CollectorPoolFactory.getLastFactory());
    System.out.println("\n******************************************************************************************");
    System.out.println(sb);
    System.out.println("Note: All reported times are in ms and the minimum from 4 runs to compensate for random GC.");
  }

  private void printTestScaleOptimizedSparseFacet(
      StringBuffer output, CollectorPoolFactory poolFactory, IndexSearcher searcher, boolean useSparse, int run,
      int matchFraction) throws Exception {
    long[] tim = testScaleOptimizedCollectorImpl(poolFactory, searcher, useSparse, matchFraction);
    output.append(String.format(
        "%8d %6d %8d %6d %6d\n", matchFraction, tim[0], tim[1], tim[2], tim[0] + tim[1] + tim[2]));
  }

  public long[] testScaleOptimizedCollectorImpl(
      CollectorPoolFactory poolFactory, IndexSearcher searcher, boolean sparse, int matchFraction) throws Exception {
    long totalTime = -System.currentTimeMillis();

    ExposedSettings.useSparseCollector = sparse;
    ExposedSettings.priority = ExposedSettings.PRIORITY.memory;
    ExposedSettings.debug = true;
    FacetMapFactory.defaultImpl = FacetMapFactory.IMPL.pass2;

    final String QUERY = ExposedHelper.EVERY + ":every_" + matchFraction;
    QueryParser qp = new QueryParser(Version.LUCENE_46, ExposedHelper.EVEN, getAnalyzer());
    Query q = qp.parse(QUERY);

    StringWriter sw = new StringWriter(10000);
    TopScoreDocCollector collector = TopScoreDocCollector.create(10, false);
    searcher.search(q, collector);
    assertTrue("There should be 1/" + matchFraction + " maxhits for test search",
               Math.abs((searcher.getIndexReader().maxDoc() / matchFraction - 1) - collector.getTotalHits()) <= 1);
    sw.append("Used heap after loading index and performing a simple search: " + getMem() + " MB\n");
    sw.append("Maximum possible memory (Runtime.getRuntime().maxMemory()): "
              + Runtime.getRuntime().maxMemory() / 1048576 + " MB\n");
    sw.append("\n");

    qp = new QueryParser(Version.LUCENE_46, ExposedHelper.EVEN, getAnalyzer());
    q = qp.parse(QUERY);

    long[] timing = testFaceting(poolFactory, searcher, q, QUERY, sw, SCALE_MANY_DOCS_REQUEST);

    System.out.println("**************************\n");

    sw.append("\nUsed memory with sort, facet and index lookup structures intact: " + getMem() + " MB\n");
    totalTime += System.currentTimeMillis();
    sw.append("Total test time: " + getTime(totalTime));

    System.out.println("");
    System.out.println(sw.toString());
    return timing;
  }

  final int COLLECTOR_TEST_DOCS = 50 * 1000000;
  public IndexSearcher getTagCollectorTestSearcher() throws IOException {
    final int TERM_LENGTH = 20;
    final List<String> FIELDS = Arrays.asList("a");
    final File LOCATION = PREFERRED_ROOT.exists() ?
        new File(PREFERRED_ROOT, "indexSparse") :
        ExposedHelper.INDEX_LOCATION;

    if (!LOCATION.exists()) {
      System.err.println("No index at " + LOCATION + ". A test index with " + COLLECTOR_TEST_DOCS
                         + " documents will be build");
      helper.createIndex(LOCATION, COLLECTOR_TEST_DOCS, FIELDS, 1, TERM_LENGTH, 1, 1, 1);
      helper.optimize(LOCATION);
    }
    assertTrue("No index at " + LOCATION.getAbsolutePath() + ". Please build a test index (you can use one from on of " +
               "the other JUnit tests in TestExposedFacets) and correct the path", LOCATION.exists());

    return new IndexSearcher(ExposedIOFactory.getReader(LOCATION));
  }

  private long[] testFaceting(CollectorPoolFactory poolFactory, IndexSearcher searcher, Query q, String sQuery,
                            StringWriter result, String requestString) throws XMLStreamException, IOException {
    final int RUNS = 5;

    IndexReader reader = searcher.getIndexReader();
    FacetResponse response = null;
    int DOCCOUNT = reader.maxDoc();
    assertTrue("There must be at least 10 documents in the index at ", DOCCOUNT >= 10);
    final int span = DOCCOUNT / 10;
    long firstTime = -1;
    //long subsAcquireTime = Long.MAX_VALUE;
    long subsCountTime = Long.MAX_VALUE;
    long subsExtractTime = Long.MAX_VALUE;
    long subsClearTime = Long.MAX_VALUE;

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

      //long acquireTime = -System.currentTimeMillis();
      TagCollector collector = collectorPool.acquire(null); // No caching
      //acquireTime += System.currentTimeMillis();

      long countTime = -System.currentTimeMillis();
      if (collector.getQuery() == null) { // Fresh collector
        searcher.search(q, collector);
      } else {
        System.out.println("Note: Using cached collector for query " + q);
      }
      countTime += System.currentTimeMillis();
      collector.setCountTime(countTime);

      long extractTime = -System.currentTimeMillis();
      response = collector.extractResult(request);
      extractTime += System.currentTimeMillis();
      assertNotNull("Extracting XML response should work", response.toXML());
      if (collector.getQuery() != null) { // Cached count
        response.setCountingCached(true);
      }

      long clearTime = -System.currentTimeMillis();
      collector.clear();
      clearTime += System.currentTimeMillis();
      if (clearTime == 0) {
        System.out.println("Why!?");
      }

      long totalTime = countTime + extractTime + clearTime;
      //long totalTime = acquireTime + countTime + extractTime + clearTime;
      if (firstTime == -1) {
        firstTime = totalTime;
        result.append("First faceting for " + sQuery + ": " + getTime(totalTime) + "\n");
      } else {
        subCount++;
        //subsAcquireTime = Math.min(subsAcquireTime, acquireTime);
        subsCountTime = Math.min(subsCountTime, countTime);
        subsExtractTime = Math.min(subsExtractTime, extractTime);
        subsClearTime = Math.min(subsClearTime, clearTime);
      }
      response.setTotalTime(totalTime);
/*        System.out.println("Collection for prefix " + prefix + " and offset "
            + offset + " for " + DOCCOUNT + " documents took "
            + (System.currentTimeMillis()-countStart) + "ms");*/
      collectorPool.release(null, collector); // No caching
    }
    result.append("Subsequent " + subCount + " faceting calls (count caching disabled) response times: "
                  + getTime(subsClearTime + subsExtractTime + subsClearTime) + "\n");
            //      + getTime(subsAcquireTime + subsClearTime + subsExtractTime + subsClearTime) + "\n");
    assertNotNull("There should be a response", response);
    result.append(response.toXML()).append("\n");
    return new long[]{subsCountTime, subsExtractTime, subsClearTime};
    //return new long[]{subsAcquireTime, subsCountTime, subsExtractTime, subsClearTime};
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
