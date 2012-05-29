package org.apache.lucene.search.exposed.facet;

import java.util.*;

/**
 * Holds a number of {@link TagCollector}s tied to a single {@link FacetMap}
 * and ensures that they are cleared and ready for use. If there are no
 * collectors in the pool when a new one is acquired, a new collector will be
 * created.
 * </p><p>
 * The pool holds a mix of cleared and ready for use {@link TagCollector}s as
 * well as updated collectors from previous queries. When a collector is
 * released back into the pool it is stored in the non-cleared cache. If the
 * non-cleared cache is full, the oldest entry is cleared and moved into the
 * cleared cache.
 * </p><p>
 * The pool is thread-safe and thread-efficient.
 */
// TODO: Consider making a limit on the number of collectors to create
public class CollectorPool {
  private final List<TagCollector> fresh;
  private final Map<String, TagCollector> filled;
  private FacetMap map;
  private final int freshCollectors;
  private final int maxFilled;

  /**
   * When a TagCollector is released with a query, it is stored as filled. If
   * the cache for filled is full, the oldest entry is cleared and stored in
   * the fresh pool.
   * @param map the map to use the collectors with.
   * @param filledCollectors the maximum number of previously filled collectors.
   * @param freshCollectors the maximum number of fresh collectors.
   */
  public CollectorPool(
      FacetMap map, int filledCollectors, int freshCollectors) {
    this.map = map;
    this.freshCollectors = freshCollectors;
    fresh = new ArrayList<TagCollector>();
    this.maxFilled = filledCollectors;
    filled = new LinkedHashMap<String, TagCollector>(filledCollectors) {
      @Override
      protected boolean removeEldestEntry(
          Map.Entry<String, TagCollector> eldest) {
        if (size() > maxFilled) {
          releaseFresh(remove(eldest.getKey())); // Send it to the fresh cache
        }
        return false; // We handle the removal ourselves
      }
    };
  }

  /**
   * @param query the query associated with the collector. If the pool contains
   * a collector filled from the query, it is returned. Else a cleared collector
   * is returned. null is a valid query and will always return a cleared
   * collector.
   * </p><p>
   * Note: If a non-null query is provided, the caller must check the returned
   * collector with the call {@link TagCollector#getQuery()}. If the result is
   * not null, the collector was taken from cache and is already filled. In that
   * case, the caller should not make a new search with the collector, but
   * instead call {@link TagCollector#extractResult(org.apache.lucene.search.exposed.facet.request.FacetRequest)} directly.
   * @return a recycled collector if one is available, else a new collector.
   */
  public TagCollector acquire(String query) {
    synchronized (filled) {
      if (query != null && maxFilled != 0 && filled.containsKey(query)) {
        return filled.remove(query);
      }
    }

    synchronized (fresh) {
      for (int i = 0 ; i < fresh.size() ; i++) {
        if (!fresh.get(i).isClearRunning()) {
          return fresh.remove(i);
        }
      }
    }
    return new TagCollector(map);
  }

  /**
   * Releasing a collector sends it to the filled cache. If the filled cache is
   * full, the oldest entry is cleared with a delayedClear and sent to the fresh
   * cache. If the maximum capacity of the fresh cache has been reached, the
   * collector is discarded.
   * </p><p>
   * This method exits immediately.
   * @param query     the query used for filling this collector. If null, the
   *                  collector is always going to the fresh cache.
   * @param collector the collector to put back into the pool for later reuse.
   */
  public void release(String query, TagCollector collector) {
    synchronized (filled) {
      if (maxFilled > 0 && query != null) {
        collector.setQuery(query);
        filled.put(query, collector);
        return;
      }
    }
    releaseFresh(collector);
  }

  private void releaseFresh(TagCollector collector) {
    synchronized (fresh) {
      if (fresh.size() >= freshCollectors) {
        return;
      }
      collector.delayedClear();
      fresh.add(collector);
    }
  }

  public void clear() {
    synchronized (fresh) {
      fresh.clear();
    }
    synchronized (filled) {
      filled.clear();
    }
  }

  public String toString() {
    long total = 0;
    for (Map.Entry<String, TagCollector> entry: filled.entrySet()) {
      total += entry.getValue().getMemoryUsage();
    }
    for (TagCollector collector: fresh) {
      total += collector.getMemoryUsage();
    }
    return "CollectorPool(" + map.toString() + ", #fresh counters = "
        + fresh.size() + ", #filled counters = " + filled.size()
        + ", total counter size = " + total / 1024 + " KB)";
  }
}
