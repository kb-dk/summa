package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;
import org.apache.lucene.util.ELog;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;

/**
 * Sparse collector where search results with #tags below the stated threshold uses a faster extraction and clearing
 * mechanism. If the #tags exceeds this threshold, standard extraction and clearing is used.
 */
public class TagCollectorSparsePacked extends TagCollector {
  private static final ELog log = ELog.getLog(TagCollectorSparsePacked.class);

  public static final int RECOMMENDED_MIN_COUNTER_SIZE = 100000; // Counters below this should not be sparse
  public static final int DIRECT_ITERATE_LIMIT = 10000; // Counter segments below this will be iterates old style
  public static final double RECOMMENDED_MAX_SPARSE_FRACTION = 0.03333; // Fractions above this are not recommended
  public static double DEFAULT_SPARSE_FACTOR = 0.0251; // If nothing is stated, this fraction is used

  private final double sparseFactor;
  private final int sparseSize;

  // Try using PackedInts.Mutables here (extend FacetMap to provide max count for any tag)
  private final PackedInts.Mutable tagCounts;
  private final PackedInts.Mutable updated;
  private int updatePointer = 0;

  /**
   * @param map a FacetMap.
   */
  public TagCollectorSparsePacked(FacetMap map) {
    this(map, DEFAULT_SPARSE_FACTOR);
  }

  /**
   * @param map a plain FacetMap.
   * @param sparseFactor the amount of space used for the sparse structure, as a fraction of tagCount space.
   */
  public TagCollectorSparsePacked(FacetMap map, double sparseFactor) {
    super(map);
    try {
      tagCounts = PackedInts.getMutable(
          map.getTagCount(), PackedInts.bitsRequired(map.getMaxTagOccurrences()), PackedInts.COMPACT);
    } catch (OutOfMemoryError e) {
      throw (OutOfMemoryError)new OutOfMemoryError(String.format(
              "OOM while trying to allocate int[%d] for tag counts ~ %dMB. FacetMap was %s",
              map.getTagCount(), map.getTagCount() / (4*1048576), map.toString())).initCause(e);
    }
    this.sparseFactor = sparseFactor;

    sparseSize = (int) (map.getTagCount()*sparseFactor);
    try {
      updated = PackedInts.getMutable(sparseSize, PackedInts.bitsRequired(map.getTagCount()), PackedInts.COMPACT);
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
    return (int) tagCounts.get(tagID);
  }

  @Override
  public int[] getTagCounts() {
    throw new UnsupportedOperationException("The internal counter structure is not an int[]");
  }
  @Override
  public boolean usesTagCountArray() {
    return false;
  }

  @Override
  public void inc(final int tagID) {
    // Sparse magic below
    long count = tagCounts.get(tagID);
    tagCounts.set(tagID, count+1);
    if (count == 0 && updatePointer != sparseSize) {
      updated.set(updatePointer++, tagID);
    }
  }

  @Override
  public void collectAbsolute(final int absoluteDocID) {
    hitCount++;
    map.updateCounter(this, absoluteDocID);
  }
  @Override
  public final void collect(final int docID) throws IOException { // Optimization
    hitCount++;
    map.updateCounter(this, docBase + docID);
  }

  @Override
  public void iterate(final IteratorCallback callback, final int startPos, final int endPos, final int minCount) {
    final long startTime = System.currentTimeMillis();
    if (updatePointer == sparseSize || (endPos - startPos) < DIRECT_ITERATE_LIMIT) {
      // Either the update tracker is blown or the range is very small
      super.iterate(callback, startPos, endPos, minCount);
    } else {
      // Sparse magic below
      for (int i = 0 ; i < updatePointer ; i++) {
        final int tagID = (int) updated.get(i);
        final int count = (int) tagCounts.get(tagID);
        if (tagID >= startPos && tagID < endPos && count >= minCount) {
          if (!callback.call(tagID, count)) {
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
    // Assigning 0 to a PackedInts is usually very fast
    if (sparseSize == updatePointer || tagCounts.ramBytesUsed()/8 < updatePointer*4) {
      tagCounts.fill(0, tagCounts.size(), 0);
    } else {
      // TODO: Measure is this is really faster than a plain fill (which might be JVM optimized)
      // Update 20140315: Seems to be based on TestSingleSegmentOptimization.testScaleOptimizedCollectorImpl
    // Sparse magic below
      for (int i = 0 ; i < updatePointer ; i++) {
        tagCounts.set((int) updated.get(i), 0);
      }
    }
    updatePointer = 0;
  }

  public String toString() {
    return "TagCollectorSparsePacked(" + getMemoryUsage()/1048576 + "MB, " + tagCounts.size() + " potential tags, "
           + sparseSize + " update counters from " + map.toString()  + ", "
           + (getQuery() == null ? "un" : "") + "cached)";
  }

  public String toString(boolean verbose) {
    if (!verbose) {
      return toString();
    }
    long nonZero = 0;
    long sum = 0;
    // TODO: Optimize the speed of this by using the sparse structure
    for (int i = 0 ; i < tagCounts.size() ; i++) {
      int count = (int) tagCounts.get(i);
      if (count > 0) {
        nonZero++;
      }
      sum += count;
    }
    return String.format("TagCollectorSparsePacked(%dMB, %d potential tags, %d non-zero counts, total sum %d from %s",
                         getMemoryUsage()/1048576, tagCounts.size(), nonZero, sum, map.toString());
  }

  @Override
  public String tinyDesignation() {
    return "TagCollectorSparsePacked(" + getMemoryUsage()/1048576 + "MB, factor=" + sparseFactor + ", "
           + updatePointer + "/" + sparseSize + (updatePointer == sparseSize ? " (full)" : "") + ")";
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
    return tagCounts.ramBytesUsed() + updated.ramBytesUsed();
  }

}
