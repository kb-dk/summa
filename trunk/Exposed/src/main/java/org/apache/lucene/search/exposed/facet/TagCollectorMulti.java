package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;
import org.apache.lucene.util.ELog;
import org.apache.lucene.util.OpenBitSet;

import java.io.IOException;
import java.util.Arrays;

/**
 * Counts tag occurrences in the given documents. This collector can be used as
 * a standard Lucene collector or it can be filled by specifying the document
 * IDs with an {@link OpenBitSet} or a standard {@code int[]}}. 
 */
public class TagCollectorMulti extends TagCollector {
  private static final ELog log = ELog.getLog(TagCollectorMulti.class);

  private final int[] tagCounts;

// TODO: Remember query for caching of results
  public TagCollectorMulti(FacetMap map) {
    super(map);
    try {
      tagCounts = new int[map.getTagCount()];
    } catch (OutOfMemoryError e) {
      throw (OutOfMemoryError)new OutOfMemoryError(String.format(
              "OOM while trying to allocate int[%d] for tag counts ~ %dMB. FacetMapMulti was %s",
              map.getTagCount(), map.getTagCount() / (4*1048576), map.toString())).initCause(e);
    }
    log.debug("Constructed " + this);
  }

  @Override
  public final void collectAbsolute(final int absoluteDocID) throws IOException {
    hitCount++;
    map.updateCounter(tagCounts, absoluteDocID);
  }
  @Override
  public final void collect(final int docID) throws IOException { // Optimization
    hitCount++;
    map.updateCounter(tagCounts, docBase + docID);
  }

  @Override
  public int get(int tagID) {
    return tagCounts[tagID];
  }

  @Override
  public int[] getTagCounts() {
    return tagCounts;
  }

  /**
   * Clears the collector making it ready for reuse. It is highly recommended to
   * reuse collectors as it lowers Garbage Collection impact, especially for
   * large scale faceting.
   * </p><p>
   * Consider using {@link #delayedClear()} to improve responsiveness.
   */
  @Override
  public void clearInternal() {
    Arrays.fill(tagCounts, 0);
  }

  @Override
  public void inc(int tagID) {
    tagCounts[tagID]++;
  }

  public String toString() {
    return "TagCollectorMulti(" + getMemoryUsage()/(4*1048576) + "MB, " + tagCounts.length + " potential tags from "
           + map.toString() + ")";
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
    return String.format("TagCollectorMulti(%d potential tags, %d non-zero counts, total sum %d from %s",
                         tagCounts.length, nonZero, sum, map.toString());
  }
  @Override
  public String tinyDesignation() {
    return "TagCollectorMulti(" + getMemoryUsage()/1048576 + "MB)";
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
    return tagCounts.length * 4;
  }

  @Override
  public boolean hasTagCounts() {
    return true;
  }
}
