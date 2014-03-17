package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.util.ELog;

import java.util.*;

/**
 * Holds a number of {@link TagCollector}s tied to a single {@link FacetMapMulti}
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
// TODO: Consider introducing an allowance for extra allocations above pool size
public class CollectorPool {
  private static final ELog log = ELog.getLog(CollectorPool.class);

  public enum AVAILABILITY {hasFresh, hasFilled, mightCreateNew, mustCreateNew, mustWait}

  private final FacetMap map;
  private final String key;

  private final List<TagCollector> fresh;
  private final Map<String, TagCollector> filled;

  private int maxFresh;
  private int maxFilled;

  /**
   * The number of delivered collectors that has not been returned yet.
   */
  private int activeCollectors = 0;
  private long activeMem = 0;

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
   * @param key the unique id for the pool.
   * @param map the map to use the collectors with.
   * @param filledCollectors the maximum number of previously filled collectors.
   * @param freshCollectors the maximum number of fresh collectors.
   */
  public CollectorPool(String key, FacetMap map, int filledCollectors, int freshCollectors) {
    this.map = map;
    this.key = key;

    maxFresh = freshCollectors;
    fresh = new ArrayList<TagCollector>();

    maxFilled = filledCollectors;
    filled = new LinkedHashMap<String, TagCollector>(filledCollectors) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, TagCollector> eldest) {
        if (size() > maxFilled) {
          releaseFresh(remove(eldest.getKey())); // Send it to the fresh cache
        }
        return false; // We handle the removal ourselves
      }
    };
  }

  /**
   * @deprecated use {@link CollectorPool(String, FacetMapMulti , int, int)}.
   */
  public CollectorPool(FacetMap map, int filledCollectors, int freshCollectors) {
    this("N/A", map, filledCollectors, freshCollectors);
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
      if (query != null && filled.containsKey(query)) {
        TagCollector collector = filled.remove(query);
        activeCollectors++;
        activeMem += collector.getMemoryUsage();
        return collector;
      }

      for (int i = 0 ; i < fresh.size() ; i++) {
        if (!fresh.get(i).isClearRunning()) {
          TagCollector collector = fresh.remove(i);
          activeCollectors++;
          activeMem += collector.getMemoryUsage();
          return collector;
        }
      }

      if (!enforceLimits || maxFresh == 0 || activeCollectors + filled.size() + fresh.size() < maxFilled + maxFresh) {
        log.debug("acquire(" + query + "): Creating new TagCollector with enforceLimits=" + enforceLimits
                  + ", maxFresh=" + maxFresh + ", activeCollectors(" + activeCollectors + ") + filled.size("
                  + filled.size() + ") + fresh.size(" + fresh.size() + ") < maxFilled(" + maxFilled + " + maxFresh("
                  + maxFresh + ") == " + (activeCollectors + filled.size() + fresh.size() < maxFilled + maxFresh));
        // It would be great to have standardized logging available here
        // as creating a TagCollector is potentially a very costly process
        TagCollector collector = TagCollectorFactory.getCollector(map);
        activeCollectors++;
        activeMem += collector.getMemoryUsage();
        return collector;
      }

      try {
        log.trace("acquire(");
        Thread.sleep(retryDelay);
      } catch (InterruptedException e) {
        // Ignore the error as it is just a simple wait
      }
    } while (retries++ < maxRetries);
    if (retries == maxRetries) {
      throw new MissingResourceException(
          String.format(
              "Tried acquiring TagCollector from %s %d times @ %dms. Filled collectors: %d/%d, fresh collectors: %d/%d,"
              + " active collectors: %d",
              this, retries, retryDelay,
              filled.size(), maxFilled, fresh.size(), maxFresh,
              activeCollectors),
          CollectorPool.class.getSimpleName(), query);
    }
    return TagCollectorFactory.getCollector(map);
  }

  private synchronized boolean isFurtherAllocationAllowed() {
    return !enforceLimits || activeCollectors + filled.size() + fresh.size() < maxFilled + maxFresh;
  }

  /**
   * Probes the caches and properties to determine what the result of a
   * call to {@link #acquire(String)} will be at this point in time.
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
    if (fresh.isEmpty()) {
      // No fresh so there is either no free collectors allocated or
      // they are in use
      return isFurtherAllocationAllowed() ?
             AVAILABILITY.mustCreateNew : AVAILABILITY.mustWait;
    }
    // Some fresh but uncleared so new ones will be created if allowed
    return isFurtherAllocationAllowed() ?
           AVAILABILITY.mightCreateNew : AVAILABILITY.mustWait;
  }

  /**
   * Probes the caches to determine how a release of the given collector
   * will affect them.
   * @param query     the query to use as storage key. null is allowed.
   * @param collector the collector to release.
   * @return true if a release will result is a TagCollector being freed.
   */
  public synchronized boolean releaseWillFree(String query, TagCollector collector){
      if (query != null) {
          // If filled overflows, it will spill over in fresh
          return filled.size() >= maxFilled && fresh.size() >= maxFresh;
      }
      return fresh.size() >= maxFresh;
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
   * @return true if the release meant that the collector was freed (claimable
   *         for the garbage collector). Freeing should be avoided. Too many
   *         of those indicates that the pool size should be increased.
   */
  public synchronized boolean release(String query, TagCollector collector) {
    boolean willFree = releaseWillFree(query, collector);
    if (maxFilled > 0 && query != null) {
      activeCollectors = Math.max(0, --activeCollectors);
      activeMem -= collector.getMemoryUsage();
      collector.setQuery(query);
      filled.put(query, collector);
      return willFree;
    }
    releaseFresh(collector);
    return willFree;
  }

  private synchronized void releaseFresh(TagCollector collector) {
    activeCollectors = Math.max(0, --activeCollectors);
    activeMem -= collector.getMemoryUsage();
    if (fresh.size() >= maxFresh) {
      return;
    }
    collector.delayedClear();
    fresh.add(collector);
  }

  public synchronized void clear() {
    activeCollectors = 0;
    activeMem = 0; // Problematic as we might have running
    fresh.clear();
    filled.clear();
  }

  public synchronized String toString() {
    return "CollectorPool(" + map.toString() + ", #fresh counters = " + fresh.size() + ", #filled counters = " 
           + filled.size() + ", active counters = " + activeCollectors + ", total counter size = " 
           + getMem() / 1024 + " KB)";
  }

  /**
   * @return approximate memory usage in bytes.
   */
  public long getMem() {
    long total = 0;
    for (Map.Entry<String, TagCollector> entry: filled.entrySet()) {
      total += entry.getValue().getMemoryUsage();
    }
    for (TagCollector collector: fresh) {
      total += collector.getMemoryUsage();
    }
    total += activeMem;
    return total;
  }

  public boolean isEnforceLimits() {
    return enforceLimits;
  }

  /**
   * @param enforceLimits if true, the limits for TagCollector
   *                      allocation will be obeyed.
   */
  public void setEnforceLimits(boolean enforceLimits) {
    this.enforceLimits = enforceLimits;
  }

  /**
   * @return the unique designation for this pool.
   * Usable for HashMaps etc.
   */
  public String getKey() {
    return key;
  }

  public int getMaxFresh() {
    return maxFresh;
  }

  public synchronized void setMaxFresh(int maxFresh) {
    this.maxFresh = maxFresh;
    fresh.clear();
  }

  public int getMaxFilled() {
    return maxFilled;
  }

  public synchronized void setMaxFilled(int maxFilled) {
    this.maxFilled = maxFilled;
    filled.clear();
  }

  public FacetMap getMap() {
    return map;
  }
}
