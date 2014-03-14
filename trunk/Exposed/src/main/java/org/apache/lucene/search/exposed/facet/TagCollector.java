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
import java.util.List;

/**
 * Counts tag occurrences in the given documents. This collector can be used as
 * a standard Lucene collector or it can be filled by specifying the document
 * IDs with an {@link org.apache.lucene.util.OpenBitSet} or a standard {@code int[]}}.
 */
public abstract class TagCollector extends Collector {

  protected final FacetMap map;

  protected int docBase;
  protected String query = null;
  protected boolean newborn = true;
  protected long countTime = -1;
  protected long hitCount = 0;

  protected boolean clearRunning = false;

  protected TagCollector(FacetMap map) {
    this.map = map;
  }



  /**
   * Update the facet counter structures for the given docID.
   * </p><p>
   * It is the responsibility of the TagCollector implementation to increment {@link #hitCount} when this is called.
   * @param docID an ID relative to the full index.
   */
  public abstract void collectAbsolute(int docID) throws IOException;

  /**
   * Increment the counter for the given tagID.
   * @param tagID a tagID relative to the full range of tagIDs for the tag counter.
   */
  public abstract void inc(int tagID);

  /**
   * @param tagID a tagID relative to the full range of tagIDs for the tag counter.
   * @return the count for the given tagID.
   */
  public abstract int get(int tagID);

  /**
   * Implementation-specific clear, readying the TagCollector for new collections.
   */
  protected abstract void clearInternal();

  /**
   * If supported by the implementation, this exposes the internal tag counter structures.
   * @return the internal tag counters. 1 entry/tag.
   */
  public abstract int[] getTagCounts();

  /**
   * @return true if the implementation supports {@link }
   */
  public abstract boolean hasTagCounts();

  /**
   * Iterates the counter and performs a callback for each entry satisfying the requirements.
   * @param startPos starting position for the iterator. Inclusive.
   * @param endPos end position for the iterator. Exclusive.
   * @param minCount count must be >= this in order to call call.
   */
  public void iterate(final IteratorCallback callback, final int startPos, final int endPos, final int minCount) {
    if (hasTagCounts()) {
      final int[] tagCounts = getTagCounts();
      for (int tagID = startPos ; tagID < endPos ; tagID++) {
        if (tagCounts[tagID] >= minCount) {
          if (!callback.call(tagID, tagCounts[tagID])) {
            break;
          }
        }
      }
    } else {
      for (int tagID = startPos ; tagID < endPos ; tagID++) {
        final int count = get(tagID);
        if (count >= minCount) {
          if (!callback.call(tagID, count)) {
            break;
          }
        }
      }
    }
  }

  /**
   * Iterates the counter and performs a callback for each entry satisfying the requirements.
   * @param minCount count must be >= this in order to call call.
   */
  public void iterate(IteratorCallback callback, int minCount) {
    iterate(callback, 0, map.getTagCount(), minCount);
  }

  public static interface IteratorCallback {
    /**
     * @param tagID a tagID relative to the full range of tagIDs for the tag counter.
     * @param count the count for the tagID.
     * @return true if iteration should continue.
     */
    boolean call(int tagID, int count);
  }

  /**
   * Update the facet counter structures for the given docID.
   * @param docID an ID relative to the last segment given in {@link #setNextReader(AtomicReaderContext)}.
   * @throws IOException if the structures could not be updated.
   */
  @Override
  public void collect(final int docID) throws IOException {  // TODO: Consider making this final as it is tightly looped
    collectAbsolute(docID + docBase);
  }

  /**
   * Uses the given docIDs to update the tag counts.
   * </p><p>
   * Note: This is different from calling {@link #collect(int)} repeatedly as the single docID collect method adjusts
   * for docBase given in {@link #setNextReader}.
   * @param docIDs the document IDs to use for tag counting.
   */
  public void collect(int[] docIDs) throws IOException {
    countTime = System.currentTimeMillis();
    hitCount = docIDs.length;
    for (int docID: docIDs) {
      collectAbsolute(docID);
    }
    newborn = false;
    countTime = System.currentTimeMillis() - countTime;
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
  public void collect(OpenBitSet docIDs) throws IOException {
    countTime = System.currentTimeMillis();
    hitCount = 0;
    DocIdSetIterator ids = docIDs.iterator();
    int id;
    while ((id = ids.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
      collectAbsolute(id);
    }
    newborn = false;
    countTime = System.currentTimeMillis() - countTime;
  }

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    docBase = context.docBase;
    newborn = false;
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

  /**
   * Clears the collector making it ready for reuse. It is highly recommended to
   * reuse collectors as it lowers Garbage Collection impact, especially for
   * large scale faceting.
   * </p><p>
   * Consider using {@link #delayedClear()} to improve responsiveness.
   */
  public void clear() {
    clearRunning = true;
    hitCount = 0;
    query = null;
    clearInternal();
    clearRunning = false;
  }
  /**
   * @return true if the collector is currently being cleared.
   */
  public boolean isClearRunning() {
    return clearRunning;
  }

  /**
   * Extraction of a specific requestGroup. This does not update countTime and totalTime.
   * This is primarily a helper function for {@link #extractResult(FacetRequest)}.
   * @param requestGroup a specific group.
   * @param groupID the ID of the group, relative to the Map structure.
   * @return a single group for a facet result.
   * @throws IOException if the facet mapper could not deliver terms.
   */
  public abstract FacetResponse.Group extractResult(FacetRequestGroup requestGroup, int groupID) throws IOException;

  /**
   * @return approximate memory usage of this collector.
   */
  public abstract long getMemoryUsage();

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
          "The number of term providers in the FacetMapMulti was " + (map.getIndirectStarts().length-1)
              + ", while the number of groups in the request was " + request.getGroups().size()
              + ". The two numbers must match");
    }
    List<FacetResponse.Group> responseGroups = new ArrayList<FacetResponse.Group>(request.getGroups().size());
    // TODO: Consider threading larger facets, but beware of cache blowout
    for (int i = 0 ; i < request.getGroups().size() ; i++) {
      FacetRequestGroup requestGroup = request.getGroups().get(i);
//      System.out.println("Extracting for " + requestGroup.getGroup().getName() + ": " + startTermPos + " -> " + endTermPos);
      responseGroups.add(extractResult(requestGroup, i));
    }
    FacetResponse response = new FacetResponse(request, responseGroups, hitCount);
    response.setCountingTime(countTime);
    return response;
  }


  /**
   * Collect all docIDs except those marked as deleted.
   * @param reader a reader for the index.
   * @throws java.io.IOException if the bits could not be accessed.
   */
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

  @Override
  public boolean acceptsDocsOutOfOrder() {
    return true;
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    //Ignore
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
