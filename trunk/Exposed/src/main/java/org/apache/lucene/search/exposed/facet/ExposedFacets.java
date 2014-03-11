package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;

import java.io.IOException;

/**
 * ExposedFacets relies on the ordinal-oriented structures from the exposed
 * framework. It shares structures with sorters whenever possible.
 * </p><p>
 * The overall goal of exposed faceting is to minimize memory usage for
 * faceting. This comes at a slight cost in processing speed as PackedInts
 * are used to hold arrays of integers instead of int[]. The faceting system
 * used delayed resolving of tags, meaning that only the returned tags are
 * resolved from internal IDs to Strings. The resolve process taxes the
 * IO-system and will likely be slow on a setup with a small disk cache,
 * large index and spinning disks. For those not in-the-know, switching to
 * SSDs for the index is the current silver bullet in the year 2010 for search
 * responsiveness in general and works very well with the exposed framework.
 * </p><p>
 * For each facet, a {@link org.apache.lucene.search.exposed.TermProvider}
 * handles the ordinal to term mapping.
 * This allows for locale based sorting of tags and similar. A supermap is
 * created from document id to TermProvider entries by treating the TermProvider
 * entry positions as logically increasing. Example: TermProvider A has 10
 * entries, provider B has 20 entries. When a document is mapped to entry 5
 * from provider B, the value 10 + 5 is stored in the map.
 * </p><p>
 * The supermap is logically a two-dimensional integer array where
 * supermap[docID] contains an array of TermProvider entry positions.
 * As Java's implementation of multidimensional arrays is not memory efficient,
 * this implementation uses indirection where one array designates the starting
 * and ending entry in another array.
 * </p><p>
 * The memory usage for 10M documents with 5M unique tags with 9M references in
 * facet A, 15M unique tags with 15M references in facet B and 10 unique tags
 * with 20M references in facet C is thus {@code
(9M + 15M + 20M) * log2(5M + 15M + 10) bits = for tag references
10M * log2(9M + 15M + 20M) bits for the doc2tagref array
5M * log2(5M) + 15M * log2(15M) + 10 * log2(10) bits TermProviders
} which gives a total of 220 MB. Depending on setup, sorting and the need for
 * fast re-opening of the index, an extra copy of the TermProvider-data might
 * be needed, bringing the total memory usage up to 275 MB. In order to actually
 * use the structure for faceting, a counting array is needed. This is an int[]
 * with an entry for each unique tag (20000010 or about 20M in this example).
 * This adds an extra 80 MB for each concurrent thread.
 * </p><p>
 * There is some temporary memory overhead while building the structures, but
 * this is less than the total size of the final structures so a rule of thumb
 * is to double that size to get the upper bound. Thus 550 MB of heap is
 * required for the whole shebang, which leaves (550-275)/80MB ~= 3 concurrent
 * faceting searches.
 * </p><p>
 * Scaling down, 1M documents with a single facet with 500K unique tags and
 * 1M occurrences takes about 20 MB with 3 concurrent searches.
 * </p><p>
 * Scaling up, 100M documents with 3 facets with 100M unique tags and
 * 1000M occurrences each can be done on a 24GB machine. However, counting
 * the tags from a search result would take a while.
 */
public class ExposedFacets {
  /**
   * The maximum number of collectors to hold in the collector pool.
   * Each collector takes up 4 * #documents bytes in the index.
   */
  private static final int MAX_COLLECTORS = 10;

  private CollectorPool collectorPool;

  public void facet(FacetRequest request, IndexSearcher searcher, Query query) throws IOException {
    TagCollector collector = collectorPool.acquire(request.getQuery());
    try {
      searcher.search(query, collector);
    } finally {
      collectorPool.release(request.getQuery(), collector);
    }
  }

  // TODO: Support match-all without search 
}
