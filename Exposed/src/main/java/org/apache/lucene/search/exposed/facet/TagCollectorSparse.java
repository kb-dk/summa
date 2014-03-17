package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;
import org.apache.lucene.util.ELog;

import java.io.IOException;
import java.util.Arrays;

/**
 * Sparse collector where search results with #tags below the stated threshold uses a faster extraction and clearing
 * mechanism. If the #tags exceeds this threshold, standard extraction and clearing is used.
 */
public class TagCollectorSparse extends TagCollector {
  private static final ELog log = ELog.getLog(TagCollectorSparse.class);

  // The numbers below are based on wild guessing and zero measurements. TODO: Measure and find better defaults
  public static final int RECOMMENDED_MIN_COUNTER_SIZE = 100000; // Counters below this should not be sparse
  public static final double RECOMMENDED_MAX_SPARSE_FRACTION = 0.03333; // Fractions above this are not recommended
  public static double DEFAULT_SPARSE_FACTOR = 0.0251; // If nothing is stated, this fraction is used

  private final double sparseFactor;
  private final int sparseSize;

  // Try using PackedInts.Mutables here (extend FacetMap to provide max count for any tag)
  private final int[] tagCounts;
  private final int[] updated;
  private int updatePointer = 0;

  /**
   * @param map a FacetMap.
   */
  public TagCollectorSparse(FacetMap map) {
    this(map, DEFAULT_SPARSE_FACTOR);
  }

  /**
   * @param map a plain FacetMap.
   * @param sparseFactor the amount of space used for the sparse structure, as a fraction of tagCount space.
   */
  public TagCollectorSparse(FacetMap map, double sparseFactor) {
    super(map);
    try {
      tagCounts = new int[map.getTagCount()];
    } catch (OutOfMemoryError e) {
      throw (OutOfMemoryError)new OutOfMemoryError(String.format(
              "OOM while trying to allocate int[%d] for tag counts ~ %dMB. FacetMap was %s",
              map.getTagCount(), map.getTagCount() / (4*1048576), map.toString())).initCause(e);
    }
    this.sparseFactor = sparseFactor;

    sparseSize = (int) (map.getTagCount()*sparseFactor);
    try {
      updated = new int[sparseSize];
    } catch (OutOfMemoryError e) {
      throw (OutOfMemoryError)new OutOfMemoryError(String.format(
              "OOM while trying to allocate int[%d] for tag updates ~ %dMB. FacetMap was %s",
              sparseSize, sparseSize / (4*1048576), map.toString())).initCause(e);
    }
    log.debug("Constructed " + this);
  }

  /**
   * Advices whether or not this FacetMap implementation should be used.
   * @param map a plain FacetMap.
   * @param sparseFraction the amount of space used for the sparse structure, as a fraction of tagCount space.
   * @param minMapSize   the minimum number of unique tags in the map for this collector to be recommended.
   * @return true if it is recommended to use this TagCollector with the given parameters.
   */
  // TODO: Weight the field(s) with cardinality > RECOMMENDED_MAX_SPARSE_FRACTION vs. total number of fields
  public static boolean isRecommended(FacetMap map, double sparseFraction, int minMapSize) {
    log.debug("isRecommended: tagCount=" + map.getTagCount() + ", minMapSize=" + minMapSize + ", sparseFactor="
              + sparseFraction + ", recommendedMaxSparseFraction=" + RECOMMENDED_MAX_SPARSE_FRACTION);
    return map.getTagCount() >= minMapSize && sparseFraction <= RECOMMENDED_MAX_SPARSE_FRACTION;
  }
  /**
   * Advices whether or not this FacetMap implementation should be used.
   * @param map a plain FacetMap.
   * @return true if it is recommended to use this TagCollector with the given parameters.
   */
  public static boolean isRecommended(FacetMap map) {
    return isRecommended(map, DEFAULT_SPARSE_FACTOR, RECOMMENDED_MIN_COUNTER_SIZE);
  }

  @Override
  public int get(int tagID) {
    return tagCounts[tagID];
  }

  @Override
  public int[] getTagCounts() {
    return tagCounts;
  }

  @Override
  public void inc(final int tagID) {
    if (sparseSize == updatePointer) {
      tagCounts[tagID]++;
      return;
    }
    // Sparse magic below
    if (tagCounts[tagID]++ == 0) { // Add a pointer if this is the first time the count is increased or the tagID
      updated[updatePointer++] = tagID;
    }
  }

  @Override
  public void collectAbsolute(final int absoluteDocID) {
    hitCount++;
    if (updatePointer == sparseSize) {
      map.updateCounter(tagCounts, absoluteDocID);
      return;
    }
    map.updateCounter(this, absoluteDocID);
  }
  @Override
  public final void collect(final int docID) throws IOException { // Optimization
    hitCount++;
    if (updatePointer == sparseSize) {
      map.updateCounter(tagCounts, docBase + docID);
      return;
    }
    map.updateCounter(this, docBase + docID);
  }

  @Override
  public void iterate(final IteratorCallback callback, final int startPos, final int endPos, final int minCount) {
    final long startTime = System.currentTimeMillis();
    if (updatePointer == sparseSize) {
      // TODO: Also call this for ranges < a certain size
      super.iterate(callback, startPos, endPos, minCount);
    } else {
      // Sparse magic below
      for (int i = 0 ; i < updatePointer ; i++) {
        final int tagID = updated[i];
        if (tagID >= startPos && tagID < endPos && tagCounts[tagID] >= minCount) {
          if (!callback.call(tagID, tagCounts[tagID])) {
            return;
          }
        }
      }
    }
    log.trace("iterate(callback, startPos=" + startPos + ", endPos" + endPos + ", minCount=" + minCount
              + ") completed with sparse=" + (updatePointer != sparseSize) + " and " + updatePointer
              + " update pointers in " + (System.currentTimeMillis()-startTime) + "ms");
  }

  @Override
  public void clearInternal() {
//    log.trace("Clearing state (updatePointer=" + updatePointer + ", sparseSize=" + sparseSize);
    if (sparseSize == updatePointer) { // Exceeded (or spot on)
      Arrays.fill(tagCounts, 0);
    } else {
      // TODO: Measure is this is really faster than a plain fill (which might be JVM optimized)
      // Update 20140315: Seems to be based on TestSingleSegmentOptimization.testScaleOptimizedCollectorImpl
    // Sparse magic below
      for (int i = 0 ; i < updatePointer ; i++) {
        tagCounts[updated[i]] = 0;
      }
    }
    updatePointer = 0;
  }

  public String toString() {
    return "TagCollectorSparse(" + getMemoryUsage()/1048576 + "MB, " + tagCounts.length + " potential tags, "
           + sparseSize + " update counters from " + map.toString() + ")";
  }

  public String toString(boolean verbose) {
    if (!verbose) {
      return toString();
    }
    long nonZero = 0;
    long sum = 0;
    // TODO: Optimize the speed of this by using the sparse structure
    for (int count: tagCounts) {
      if (count > 0) {
        nonZero++;
      }
      sum += count;
    }
    return String.format("TagCollectorSparse(%dMB, %d potential tags, %d non-zero counts, total sum %d from %s",
                         getMemoryUsage()/1048576, tagCounts.length, nonZero, sum, map.toString());
  }

  @Override
  public String tinyDesignation() {
    return "TagCollectorSparse(" + getMemoryUsage()/1048576 + "MB, factor=" + sparseFactor + ")";
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
    return new TagExtractor(requestGroup, map).extract(groupID, this, startTermPos, endTermPos);
  }

  @Override
  public long getMemoryUsage() {
    return (tagCounts.length + updated.length) * 4;
  }

  @Override
  public boolean hasTagCounts() {
    return true;
  }
}
