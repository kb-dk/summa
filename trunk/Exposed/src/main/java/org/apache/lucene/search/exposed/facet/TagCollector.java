package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.IndexUtil;
import org.apache.lucene.util.OpenBitSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Counts tag occurrences in the given documents. This collector can be used as
 * a standard Lucene collector or it can be filled by specifying the document
 * IDs with an {@link OpenBitSet} or a standard {@code int[]}}. 
 */
public class TagCollector extends Collector {
  private String query = null;
  private long countTime = -1;
  private int docBase;
  private final int[] tagCounts;
  private final FacetMap map;
  private boolean clearRunning = false;
  private long hitCount = 0;
  private boolean newborn = true;

// TODO: Remember query for caching of results
  public TagCollector(FacetMap map) {
    this.map = map;
    try {
      this.tagCounts = new int[map.getTagCount()];
    } catch (OutOfMemoryError e) {
      throw (OutOfMemoryError)new OutOfMemoryError(String.format(
              "OOM while trying to allocate int[%d] for tag counts ~ %dMB. FacetMap was %s",
              map.getTagCount(), map.getTagCount() / 1048576, map.toString())).initCause(e);
    }
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    //Ignore
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
  public void setNextReader(AtomicReaderContext context)
      throws IOException {
    docBase = context.docBase;
    newborn = false;
  }

  @Override
  public boolean acceptsDocsOutOfOrder() {
    return true;
  }

  /**
   * Uses the given bits to update the tag counts. Each bit designates a docID.
   * </p><p>
   * Note: This is different from calling {@link #collect(int)} repeatedly
   * as the single docID collect method adjusts for docBase given in
   * {@link #setNextReader}.
   * @param bits the document IDs to use to use for tag counting.
   * @throws java.io.IOException if the bits could not be accessed.
   */
  public void collect(OpenBitSet bits) throws IOException {
    countTime = System.currentTimeMillis();
    hitCount = 0;
    DocIdSetIterator ids = bits.iterator();
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
   * Collect all docIDs except those marked as deleted.
   * @param reader a reader for the index.
   * @throws java.io.IOException if the bits could not be accessed.
   */
  // TODO: Rewrite this to avoid allocation of the large bitset
  public void collectAllValid(IndexReader reader) throws IOException {
    newborn = false;
    countTime = System.currentTimeMillis();
    OpenBitSet bits = new OpenBitSet(reader.maxDoc());
    if (reader instanceof AtomicReader) {
      AtomicReader atomic = (AtomicReader)reader;
      if (atomic.getLiveDocs() == null) { // All live
          bits.flip(0, reader.maxDoc());
      } else {
        Bits live = atomic.getLiveDocs();
        if (live instanceof OpenBitSet) {
          collect((OpenBitSet)live);
        } else {
          for (int i = 0 ; i < live.length() ; i++) {
            if (live.get(i)) {
                bits.set(i);
            }
          }
          collect(bits);
        }
      }
      return;
    }
    // CompositeReader

    int docBase = 0;
    List<? extends IndexReader> subs = IndexUtil.flatten(reader);
    for (IndexReader sub: subs) {
      AtomicReader atomic = (AtomicReader)sub;
      remove(bits, docBase, atomic.getLiveDocs());
      //    sub.getTopReaderContext().docBaseInParent
      docBase += sub.maxDoc();
    }
    collect(bits);
    countTime = System.currentTimeMillis() - countTime;
  }
  private void remove(OpenBitSet prime, int base, Bits keepers) {
    if (keepers == null) {
      return;
    }
    // TODO: Optimize for OpenBitSet
    for (int i = 0 ; i < keepers.length() ; i++) {
      if (!keepers.get(i)) {
        prime.clear(base + i);
      }
    }
  }

  /**
   * Clears the collector making it ready for reuse. It is highly recommended to
   * reuse collectors as it lowers Garbage Collection impact, especially for
   * large scale faceting.
   * </p><p>
   * Consider using {@link #delayedClear()} to improve responsiveness.
   */
  public void clear() {
    clearRunning = true;
    Arrays.fill(tagCounts, 0);
    hitCount = 0;
    query = null;
    clearRunning = false;
  }

  /**
   * Starts a Thread that clears the collector and exits immediately. Make sure
   * that {@link #isClearRunning()} returns false before using the collector
   * again.
   */
  public void delayedClear() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        clear();
      }
    }, "TagCollector clear").start();
  }

  public boolean isClearRunning() {
    return clearRunning;
  }

  public String toString() {
    return "TagCollector(" + tagCounts.length + " potential tags from "
        + map.toString();
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
    return "TagCollector(" + tagCounts.length + " potential tags, " + nonZero 
        + " non-zero counts, total sum " + sum + " from " + map.toString();
  }

  /**
   * After collection (and implicit reference counting), a response can be
   * extracted. Note that countTime and totalTime is not set in the response.
   * It is recommended that these two time measurements are updated before
   * the response is delivered.
   * @param request a request matching the one used for counting. Note that
   *        minor details, such as offset and maxTags need not match.
   * @return a fully resolved facet structure.
   * @throws IOException if the facet mapper could not deliver terms.
   */
  public FacetResponse extractResult(FacetRequest request) throws IOException {
    newborn = false;
    if (map.getIndirectStarts().length-1 != request.getGroups().size()) {
      throw new IllegalStateException(
          "The number of term providers in the FacetMap was "
              + (map.getIndirectStarts().length-1)
              + ", while the number of groups in the request was "
              + request.getGroups().size() + ". The two numbers must match");
    }
    List<FacetResponse.Group> responseGroups =
        new ArrayList<FacetResponse.Group>(request.getGroups().size());
    // TODO: Consider threading larger facets, but beware of cache blowout
    for (int i = 0 ; i < request.getGroups().size() ; i++) {
      FacetRequestGroup requestGroup = request.getGroups().get(i);
//      System.out.println("Extracting for " + requestGroup.getGroup().getName() + ": " + startTermPos + " -> " + endTermPos);
      responseGroups.add(extractResult(requestGroup, i));
    }
    FacetResponse response = new FacetResponse(
        request, responseGroups, hitCount);
    response.setCountingTime(getCountTime());
    return response;
  }

  private FacetResponse.Group extractResult(FacetRequestGroup requestGroup, int groupID) throws IOException {
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


  /**
   * The query is typically used for cache purposes.
   * @param query the query used for the collector filling search.
   */
  public void setQuery(String query) {
    newborn = false;
    this.query = query;
  }

  /**
   * @return the query used for filling this collector. Must be set explicitly
   * by the user of this class with {@link #setQuery(String)}.
   */
  public String getQuery() {
    newborn = false;
    return query;
  }

  public long getHitCount() {
    newborn = false;
    return hitCount;
  }

  /**
   * This is set implicitely by calls to {@link #collect(int[])} and
   * {@link #collect(OpenBitSet)} but must be set explicitly with
   * {@link #setCountTime(long)} if the TagCollector is used as a Lucene
   * collector with a standard search.
   * @return the number of milliseconds used for filling the tag counter.
   */
  public long getCountTime() {
    return countTime;
  }

  /**
   * @param countTime the number of milliseconds used for filling the counter.
   */
  public void setCountTime(long countTime) {
    this.countTime = countTime;
    newborn = false;
  }

  // Approximate
  public long getMemoryUsage() {
    return tagCounts.length * 4;
  }

  /**
   * Note: Although the implementation tries its best to ensure the validity
   * of this marker, the marker will not be updated in the method
   * {@link #collect(int)} for performance reasons. All other relevant methods
   * will update the marker to false.
   * @return true if the object is freshly allocated.
   */
  public boolean isNewborn() {
    return newborn;
  }

}
