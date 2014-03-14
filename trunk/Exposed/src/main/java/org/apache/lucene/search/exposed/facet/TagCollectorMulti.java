package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;
import org.apache.lucene.util.OpenBitSet;

import java.io.IOException;
import java.util.Arrays;

/**
 * Counts tag occurrences in the given documents. This collector can be used as
 * a standard Lucene collector or it can be filled by specifying the document
 * IDs with an {@link OpenBitSet} or a standard {@code int[]}}. 
 */
public class TagCollectorMulti extends TagCollector {
  private int docBase;
  private final int[] tagCounts;

// TODO: Remember query for caching of results
  public TagCollectorMulti(FacetMap map) {
    super(map);
    try {
      this.tagCounts = new int[map.getTagCount()];
    } catch (OutOfMemoryError e) {
      throw (OutOfMemoryError)new OutOfMemoryError(String.format(
              "OOM while trying to allocate int[%d] for tag counts ~ %dMB. FacetMapMulti was %s",
              map.getTagCount(), map.getTagCount() / 1048576, map.toString())).initCause(e);
    }
  }

  /*
  Final is annoying, but we need all the speed we can get in the inner loop
   */
  @Override
  public final void collect(final int doc) throws IOException {
    hitCount++;
    map.updateCounter(tagCounts, doc + docBase);
  }

/*  @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    this.docBase = docBase;
  }*/

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    docBase = context.docBase;
    newborn = false;
  }

  /**
   * Uses the given bits to update the tag counts. Each bit designates a docID.
   * </p><p>
   * Note: This is different from calling {@link #collect(int)} repeatedly
   * as the single docID collect method adjusts for docBase given in
   * {@link #setNextReader}.
   * @param docIDs the document IDs to use to use for tag counting.
   * @throws java.io.IOException if the bits could not be accessed.
   */
  @Override
  public void collect(OpenBitSet docIDs) throws IOException {
    countTime = System.currentTimeMillis();
    hitCount = 0;
    DocIdSetIterator ids = docIDs.iterator();
    int id;
    while ((id = ids.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
      hitCount++;
      map.updateCounter(tagCounts, id);
    }
    newborn = false;
    countTime = System.currentTimeMillis() - countTime;
  }

  /**
   * Uses the given docIDs to update the tag counts.
   * </p><p>
   * Note: This is different from calling {@link #collect(int)} repeatedly
   * as the single docID collect method adjusts for docBase given in
   * {@link #setNextReader}.
   * @param docIDs the document IDs to use for tag counting.
   */
  @Override
  public void collect(int[] docIDs) {
    countTime = System.currentTimeMillis();
    hitCount = docIDs.length;
    for (int docID: docIDs) {
      map.updateCounter(tagCounts, docID);
    }
    newborn = false;
    countTime = System.currentTimeMillis() - countTime;
  }

  /**
   * Clears the collector making it ready for reuse. It is highly recommended to
   * reuse collectors as it lowers Garbage Collection impact, especially for
   * large scale faceting.
   * </p><p>
   * Consider using {@link #delayedClear()} to improve responsiveness.
   */
  @Override
  public void clear() {
    clearRunning = true;
    Arrays.fill(tagCounts, 0);
    hitCount = 0;
    query = null;
    clearRunning = false;
  }

  public String toString() {
    return "TagCollector(" + tagCounts.length + " potential tags from " + map.toString();
  }

  public String toString(boolean verbose) {
    if (!verbose) {
      return toString();
    }
    long nonZero = 0;
    long sum = 0;
    for (int count: tagCounts) {
      if (count > 0) {
        nonZero++;
      }
      sum += count;
    }
    return String.format("TagCollector(%d potential tags, %d non-zero counts, total sum %d from %s",
                         tagCounts.length, nonZero, sum, map.toString());
  }

  @Override
  public FacetResponse.Group extractResult(FacetRequestGroup requestGroup, int groupID) throws IOException {
/*    if (!FacetRequest.ORDER_COUNT.equals(requestGroup.getOrder())) {
      throw new UnsupportedOperationException("The order '"
          + requestGroup.getOrder() + " is not supported yet for result " +
          "extraction");
    }
  */
    int startTermPos = map.getIndirectStarts()[groupID];   // Inclusive
    int endTermPos =   map.getIndirectStarts()[groupID+1]; // Exclusive
    return new TagExtractor(requestGroup).extract(groupID, map, tagCounts, startTermPos, endTermPos);
  }

  @Override
  public long getMemoryUsage() {
    return tagCounts.length * 4;
  }

}
