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
 * </p><p>
 * Important: It is essential that used collectors are released back after use!
 * If collectors are not released, the pool fail til deliver collectors when
 * maximum amount of live collectors is reached.
 * It is highly recommended to acquire a pool, do processing inside a try and
 * release the pool in a finally-statement.
 */
// TODO: Consider making a limit on the number of collectors to create
public class CollectorPool {
  public enum AVAILABILITY {hasFresh, hasFilled, mightCreateNew, mustCreateNew}

  private final FacetMap map;

  private final List<TagCollector> fresh;
  private final Map<String, TagCollector> filled;

  private final int maxFresh;
  private final int maxFilled;

  /**
   * The number of delivered collectors that has not been returned yet.
   */
  private int activeCollectors = 0;

  /**
   * Is true, maxfresh and maxFilled are guaranteed. If false, temporary
   * allocation of TagCollectors above these limits is possible.
   */
  private boolean enforceLimits = true;
  /**
   * The maximum number of retries before giving up on TacCollector acquirement.
   * Only relevant when enforceLimits == true.
   */
  private int maxRetries = 10;

  private long retryDelay = 100; // ms

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

    this.maxFresh = freshCollectors;
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
  public synchronized TagCollector acquire(String query) {
    int retries = 0;
    do {
      if (query != null && maxFilled != 0 && filled.containsKey(query)) {
        activeCollectors++;
        return filled.remove(query);
      }

      for (int i = 0 ; i < fresh.size() ; i++) {
        if (!fresh.get(i).isClearRunning()) {
          activeCollectors++;
          return fresh.remove(i);
        }
      }

      if (!enforceLimits
          || filled.size() + fresh.size() < maxFilled + maxFilled) {
        activeCollectors++;
        // It would be great to have standardized logging available here
        // as creating a TagCollector is potentially a very costly process
        return new TagCollector(map);
      }

      try {
        Thread.sleep(retryDelay);
      } catch (InterruptedException e) {
        // Ignore the error as it is just a simple wait
      }
    } while (retries++ < maxRetries);
    if (retries == maxRetries) {
      throw new MissingResourceException(
          String.format(
              "Tried acquiring TagCollector from %s %d times @ %dms. "
              + "Filled collectors: %d/%d, fresh collectors: %d/%d, "
              + "active collectors: %d",
              this, retries, retryDelay,
              filled.size(), maxFilled, fresh.size(), maxFresh,
              activeCollectors),
          CollectorPool.class.getSimpleName(), query);
    }
    return new TagCollector(map);
  }

  /**
   * Probes the caches and properties to determine what the result of a call to {@link #acquire(String)} will be at this
   * point in time.
   * @param query the query for the collector. This might be null.
   * @return the expected result of a call til acquire.
   */
  public synchronized AVAILABILITY getAvailability(String query) {
    if (query != null && maxFilled != 0 && filled.containsKey(query)) {
      return AVAILABILITY.hasFilled;
    }
    for (TagCollector aFresh : fresh) {
      if (!aFresh.isClearRunning()) {
        return AVAILABILITY.hasFresh;
      }
    }
    if (fresh.size() == 0) {
      return AVAILABILITY.mustCreateNew;
    }
    return AVAILABILITY.mightCreateNew;
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
  public synchronized void release(String query, TagCollector collector) {
    if (maxFilled > 0 && query != null) {
      activeCollectors = Math.max(0, --activeCollectors);
      collector.setQuery(query);
      filled.put(query, collector);
      return;
    }
    releaseFresh(collector);
  }

  private synchronized void releaseFresh(TagCollector collector) {
    activeCollectors = Math.max(0, --activeCollectors);
    if (fresh.size() >= maxFresh) {
      return;
    }
    collector.delayedClear();
    fresh.add(collector);
  }

  public synchronized void clear() {
    activeCollectors = 0;
    fresh.clear();
    filled.clear();
  }

  public synchronized String toString() {
    long total = 0;
    for (Map.Entry<String, TagCollector> entry: filled.entrySet()) {
      total += entry.getValue().getMemoryUsage();
    }
    for (TagCollector collector: fresh) {
      total += collector.getMemoryUsage();
    }
    return "CollectorPool(" + map.toString() + ", #fresh counters = "
        + fresh.size() + ", #filled counters = " + filled.size()
        + ", active counters = " + activeCollectors
        + ", total cached counter size = " + total / 1024 + " KB)";
  }
}
