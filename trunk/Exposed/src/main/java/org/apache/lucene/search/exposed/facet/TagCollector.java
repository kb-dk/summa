package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
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

  protected String query = null;
  protected boolean newborn = true;
  protected long countTime = -1;
  protected long hitCount = 0;

  protected boolean clearRunning = false;

  protected TagCollector(FacetMap map) {
    this.map = map;
  }

  /**
   * Collect the given docIDs.
   * @param docIDs index-wide docIDs.
   */
  public abstract void collect(OpenBitSet docIDs) throws IOException;

  /**
   * Collect the given docIDs.
   * @param docIDs index-wide IDs.
   */
  public abstract void collect(int[] docIDs);

  /**
   * Clear the collected information, readying the TagCollector for new collections.
   */
  public abstract void clear();

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
