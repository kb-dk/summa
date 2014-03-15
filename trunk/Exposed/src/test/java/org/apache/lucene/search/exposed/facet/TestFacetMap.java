/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package org.apache.lucene.search.exposed.facet;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestFacetMap extends TestCase {
  private static Log log = LogFactory.getLog(TestFacetMap.class);

  // Strange observation: Updating the counts (the collect phase) seems to be faster for TagCollectorSparse
  public void testCountDifference() throws IOException {
    final int TAGS = 50000000;
    final int COLLECTS = TAGS / 50;
    final int RUNS = 10;

    FacetMap map = new FakeFacetMapSingle(TAGS);

    TagCollector sparse = new TagCollectorSparse(map);
    TagCollector multi = new TagCollectorMulti(map);

    for (int i = 1 ; i <= RUNS ; i += 2) {
      // Severe problem with micro-benchmarking
      measure(i, multi, map, COLLECTS);
      measure(i, sparse, map, COLLECTS);
      System.out.println("");
    }
    System.out.println("Warning: Due to (assumed) JIT issues, this performance test is not valid.");
    System.out.println("Instead run testSparsePerformance and testMultiPerformance alone and");
    System.out.println("on separate JVM invocations.");
  }

  public void testSparsePerformance() throws IOException {
    testSpecificPerformance(true);
  }
  public void testMultiPerformance() throws IOException {
    testSpecificPerformance(false);
  }
  private void testSpecificPerformance(boolean sparse) throws IOException {
    final int TAGS = 100000000;
    final int COLLECTS = TAGS / 50;
    final int RUNS = 10;

    FacetMap map = new FakeFacetMapSingle(TAGS);
    TagCollector collector = sparse ? new TagCollectorSparse(map) : new TagCollectorMulti(map);

    for (int i = 1 ; i <= RUNS ; i += 2) {
      measure(i, collector, map, COLLECTS);
    }
    System.out.println("Warning: This test must be run as the only test for the current JVM invocation");
  }

  private void measure(int run, TagCollector collector, FacetMap map, int collects) throws IOException {
    long collectTime = collect(collector, map, collects);
    long clearTime = -System.currentTimeMillis();
    collector.clear();
    clearTime += System.currentTimeMillis();
    System.out.println(String.format(
        "Run %2d (sparse=%5b): count=%4dms, clearAvg=%4dms, totalAvg=%4d",
        run, collector instanceof TagCollectorSparse, collectTime, clearTime, collectTime + clearTime));
  }

  private long collect(TagCollector tagCollector, FacetMap map, int collects) throws IOException {
    int jump = map.getTagCount() / collects;
    long startTime = System.currentTimeMillis();
    for (int i = 0 ; i < map.getTagCount() ; i += jump) {
      tagCollector.collect(i);
    }
    return System.currentTimeMillis() - startTime;
  }

  public static final class FakeFacetMapSingle implements FacetMap {
    private final int[] indirectStarts = new int[]{1, 1};

    private final int tags;

    public FakeFacetMapSingle(int tags) {
      this.tags = tags;
      indirectStarts[1] = tags;
    }

    @Override
    public int getTagCount() {
      return tags;
    }

    @Override
    public void updateCounter(final int[] tagCounts, int docID) {
      tagCounts[docID]++;
    }

    @Override
    public void updateCounter(final TagCollector collector, int docID) {
      try {
        final int index = docID;
        if (index == 0) {
          return;
        }
        collector.inc(index);
      } catch (Exception ex) {
        System.err.println("Ouchie!");
      }
    }

    @Override
    public BytesRef getOrderedTerm(int termIndirect) throws IOException {
      return new BytesRef(Integer.toHexString(termIndirect));
    }

    @Override
    public BytesRef getOrderedDisplayTerm(int termIndirect) throws IOException {
      return new BytesRef(Integer.toHexString(termIndirect));
    }

    @Override
    public BytesRef[] getTermsForDocID(int docID) throws IOException {
      return new BytesRef[] {new BytesRef(Integer.toHexString(docID))};
    }

    @Override
    public int[] getIndirectStarts() {
      return indirectStarts;
    }

    @Override
    public List<TermProvider> getProviders() {
      return new ArrayList<TermProvider>();
    }
  }
}
