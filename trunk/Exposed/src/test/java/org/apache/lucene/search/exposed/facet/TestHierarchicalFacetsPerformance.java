package org.apache.lucene.search.exposed.facet;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.exposed.*;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.Random;

// TODO: Change this to LuceneTestCase but ensure Flex
public class TestHierarchicalFacetsPerformance extends TestCase {
  public static final String HIERARCHICAL = "deep";
  private static final File TESTDIR = new File("performancetest.delete");
  private static final File INDEX = new File(TESTDIR, "index");
  public static final String MOD = "mods";  // all primes up to 101 where
                                            // docID % prime == 0
  public static final int[] PRIMES = new int[]{
      2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67,
      71, 73, 79, 83, 89, 97, 101};

  private ExposedHelper helper;
  private ExposedCache cache;

  public TestHierarchicalFacetsPerformance(String name) {
    super(name);
//    CodecProvider.setDefaultCodec("Standard");
    TestHierarchicalTermProvider.deleteIndex();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cache = ExposedCache.getInstance();
    helper = new ExposedHelper();
  }

  @Override

  public void tearDown() throws Exception {
    super.tearDown();
    cache.purgeAllCaches();
    helper.close();
  }


  public static Test suite() {
    return new TestSuite(TestHierarchicalFacetsPerformance.class);
  }

  public void testBasicIndexBuild() throws IOException {
    TestHierarchicalTermProvider.createIndex(1000, 3, 4);
  }

  public static final String HIERARCHICAL_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"hierarchical\" order=\"count\" hierarchical=\"true\">\n" +
          "      <fields>\n" +
          "        <field name=\"deep\" />\n" +
          "      </fields>\n" +
          "      <subtags suborder=\"base\" maxtags=\"5\"/>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public static final String HIERARCHICAL_POP_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"hierarchical\" order=\"count\" hierarchical=\"true\">\n" +
          "      <fields>\n" +
          "        <field name=\"deep\" />\n" +
          "      </fields>\n" +
          "      <subtags suborder=\"count\" maxtags=\"5\"/>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  //http://wiki.apache.org/solr/HierarchicalFaceting
  public static final String HIERARCHICAL_COMPARATIVE_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"comparative\" order=\"count\" hierarchical=\"true\">\n" +
          "      <fields>\n" +
          "        <field name=\"level_s\" />\n" +
          "      </fields>\n" +
          "      <subtags suborder=\"count\" maxtags=\"-1\"/>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";
  public static final String HIERARCHICAL_COMPARATIVE_SANE_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"comparative\" order=\"count\" hierarchical=\"true\">\n" +
          "      <fields>\n" +
          "        <field name=\"level_s\" />\n" +
          "      </fields>\n" +
          "      <subtags suborder=\"count\" maxtags=\"20\"/>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";



  //http://wiki.apache.org/solr/HierarchicalFaceting
  public int buildComparativeIndex(int level2num) throws IOException {
    IndexWriter w = ExposedHelper.getWriter();
    int docID = 0;
    for (char c = 'A' ; c <= 'Z' ; c++) {
      for (int i = 1 ; i <= level2num ; i++) {
        ExposedHelper.addDocument(w,
            "all:all",
            "id:" + Integer.toString(docID++),
            "level_s:" + Character.toString(c) + "/" + i,
            "level1_s:" + Character.toString(c),
            "level12_s:" + i);
      }
    }
    w.close(true);
    return docID;
  }

  public void testOrderedWide() throws Exception {
    TestHierarchicalTermProvider.createIndex(1000000, 5, 4);
    ExposedSettings.debug = true;
    testRequest(HIERARCHICAL_REQUEST, 5);
  }

  public void testOrderedDeep() throws Exception {
    TestHierarchicalTermProvider.createIndex(10000, 4, 15);
    ExposedSettings.debug = true;
    testRequest(HIERARCHICAL_REQUEST, 5);
  }

  public void testPopularitySimple() throws Exception {
    TestHierarchicalTermProvider.createIndex(10000, 5, 4);
    ExposedSettings.debug = true;
    testRequest(HIERARCHICAL_POP_REQUEST, 3);
  }

  // http://wiki.apache.org/solr/HierarchicalFaceting
  public void testComparative() throws Exception {
    buildComparativeIndex(10000);
    ExposedSettings.debug = true;
    testRequest(HIERARCHICAL_COMPARATIVE_REQUEST, 5);
  }

  public void testComparativeSane() throws Exception {
    buildComparativeIndex(10000);
    ExposedSettings.debug = true;
    testRequest(HIERARCHICAL_COMPARATIVE_SANE_REQUEST, 5);
  }

  public static final String COMPARE_3079 =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"hierarchical\" order=\"count\" hierarchical=\"true\" maxtags=\"5\" levels=\"1\">\n" +
          "      <fields>\n" +
          "        <field name=\"deep\" />\n" +
          "      </fields>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";

  public void testCompare_3079() throws Exception {
    TestHierarchicalTermProvider.createIndex(1000000, 3, 4);
    ExposedSettings.debug = true;
    testRequest(COMPARE_3079, 5);
  }

  public void testCompare_3079_provider() throws Exception {
    createIndex(10000, 10, 4);
    ExposedSettings.debug = true;
    testRequest(COMPARE_3079, 5);
  }

  public void testCompare_3079_big() throws Exception {
    TestHierarchicalTermProvider.createIndex(5000000, 3, 6);
    ExposedSettings.debug = true;
    testRequest(COMPARE_3079, 5);
  }

  public void testCompare_3079_small() throws Exception {
    //TestHierarchicalTermProvider.createIndex(100000, 2, 4);
    createIndex(100, 4, 4);
    ExposedSettings.debug = true;
    testRequest(COMPARE_3079, 5);
  }

  public void testCompare_3079_wide_deep() throws Exception {
    TestHierarchicalTermProvider.createIndex(1000, 7, 10);
    ExposedSettings.debug = true;
    testRequest(COMPARE_3079, 5);
  }

  public void testCompare_3079_tiny() throws Exception {
    TestHierarchicalTermProvider.createIndex(100, 2, 3);
    ExposedSettings.debug = true;
    testRequest(COMPARE_3079, 5);
  }

  public void testWithCorpusGenerator_micro() throws Exception {
    createIndex(100, 2, 3);
    ExposedSettings.debug = true;
    testRequest(COMPARE_3079, 5);
  }


  public void testRequest(String requestXML, int runs) throws Exception {
    //File location = new File("/home/te/projects/index10M10T4L");
    File location = ExposedHelper.INDEX_LOCATION;

    IndexReader reader = ExposedIOFactory.getReader(location);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_40, ExposedHelper.ALL,
        new WhitespaceAnalyzer(Version.LUCENE_40));
      //MockAnalyzer(new Random(), MockTokenizer.WHITESPACE, false));
    Query q = qp.parse(ExposedHelper.ALL);
    TopScoreDocCollector sanityCollector =
        TopScoreDocCollector.create(10, false);
    searcher.search(q, sanityCollector);
    assertEquals("The search for " + q + " should give the right hits",
        reader.maxDoc(), sanityCollector.topDocs().totalHits);
    long preMem = ExposedHelper.getMem();
    long facetStructureTime = System.currentTimeMillis();

    CollectorPoolFactory poolFactory = new CollectorPoolFactory(2, 4, 2);

    FacetRequest request = FacetRequest.parseXML(requestXML);
    CollectorPool collectorPool = poolFactory.acquire(reader, request);
    facetStructureTime = System.currentTimeMillis() - facetStructureTime;

    TagCollector collector;
    FacetResponse response = null;
    String sQuery = null;//request.getQuery();
    for (int i = 0 ; i < runs ; i++) {
      collector = collectorPool.acquire(sQuery);
      long countStart = System.currentTimeMillis();
      if (collector.getQuery() == null) { // Fresh collector
        searcher.search(q, collector);
        if (i == 0) {
          System.out.println(collector.toString(true));
        }
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
      System.out.println("Collection and extraction #" + i + " for "
          + collector.getHitCount() + " documents in "
          + ExposedHelper.getTime(System.currentTimeMillis()-countStart));
      collectorPool.release(sQuery, collector);
    }
    System.out.println("Document count = " + reader.maxDoc());
    System.out.println("Hierarchical facet startup time = "
        + ExposedHelper.getTime(facetStructureTime));
    System.out.println("Mem usage: preFacet=" + preMem
        + " MB, postFacet=" + ExposedHelper.getMem() + " MB");
    if (response != null) {
      long xmlTime = -System.currentTimeMillis();
      String r = response.toXML();
      xmlTime += System.currentTimeMillis();
      System.out.println("Generated XML with " + r.length() + " characters in "
          + xmlTime + " ms");
      System.out.println(r.length() > 10000 ? r.substring(0, 10000) : r);
    }
  }

  private long createIndex(int docs, int maxTagsPerLevel, int maxLevel)
                                                            throws IOException {
    Random random = new Random(87);
    CorpusGenerator corpus = new CorpusGenerator();
    corpus.setDepths(
        new CorpusGenerator.SimpleRandom(random, 0, maxLevel));
    corpus.setPaths(
        new CorpusGenerator.SimpleRandom(random, 0, maxTagsPerLevel));
    corpus.setTags(
        new CorpusGenerator.SimplePathElementProducer(
            "", new CorpusGenerator.SimpleRandom(random, 1, 58)));
    return createIndex(docs, corpus);
  }
  private long createIndex(int docCount, CorpusGenerator corpus)
      throws IOException {
    if (!INDEX.exists()) {
      //noinspection ResultOfMethodCallIgnored
      INDEX.mkdirs();
    }
    if (INDEX.listFiles().length > 0) {
      System.out.println(
          "Index already exists at '" + INDEX + "'. Skipping creation");
      return 0;
    }

    long startTime = System.nanoTime();
    long references = 0;
    File location = ExposedHelper.INDEX_LOCATION;
    if (location.listFiles().length > 0) {
      System.out.println("Index already exists, skipping creation");
      return 0;
    }

    IndexWriter writer = ExposedIOFactory.getWriter(location);

    int every = docCount > 100 ? docCount / 100 : 1;
    int next = every;
    for (int docID = 0 ; docID < docCount ; docID++) {
      if (docID == next) {
        System.out.print(".");
        next += every;
      }
      Document doc = new Document();

      references += addHierarchicalTags(doc, docID, corpus);

      doc.add(new Field(ExposedHelper.ID, ExposedHelper.ID_FORMAT.format(docID),
          Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field(ExposedHelper.EVEN, docID % 2 == 0 ? "true" : "false",
          Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field(ExposedHelper.ALL, ExposedHelper.ALL,
          Field.Store.YES, Field.Index.NOT_ANALYZED));
      for (int prime: PRIMES) {
        if (docID % prime == 0) {
          doc.add(new Field(MOD, Integer.toString(prime),
              Field.Store.YES, Field.Index.NOT_ANALYZED));
        }
      }
      writer.addDocument(doc);
    }
    writer.close();
    System.out.println("");
    System.out.println(String.format(
        "Created %d document index with " + references
            + " tag references in %sms at %s", docCount,
        (System.nanoTime() - startTime) / 1000000, location.getAbsolutePath()));
    return references;
  }

  private StringBuffer sb = new StringBuffer(100);
  private long addHierarchicalTags(
      Document doc, int docID, CorpusGenerator corpus) {
    String[][] paths = corpus.getPaths(docID);
    for (String[] elements: paths) {
      sb.setLength(0);
      for (String element : elements) {
        if (sb.length() != 0) {
          sb.append("/");
        }
        sb.append(element);
      }
      if (sb.length() != 0) {
        doc.add(new Field(HIERARCHICAL, sb.toString(),
                          Field.Store.NO, Field.Index.NOT_ANALYZED));
      }
    }
    return paths.length;
  }

}