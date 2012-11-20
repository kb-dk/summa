package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.exposed.ExposedCache;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Constructs {@link CollectorPool}s based on
 * {@link org.apache.lucene.search.exposed.facet.request.FacetRequest}s.
 * As the construction of a pool is costly, pools are kept in a LRU-cache.
 * </p><p>
 * The factory connects to the {@link ExposedCache} and the cache is
 * automatically updated (aka cleared) when the index is reloaded and IndexCache
 * is notified about it.
 */
public class CollectorPoolFactory implements ExposedCache.PurgeCallback,
                                             IndexReader.ReaderClosedListener {
  private Map<String, CollectorPool> poolMap;
  // Readers used by the pools
  private Set<IndexReader> readers = new HashSet<IndexReader>();
  /**
   * The maximum number of filled collectors to cache for each CollectorPool.
   */
  private final int filledCollectors;

  /**
   * The maximum number of fresh collectors to cache for each CollectorPool.
   */
  private final int freshCollectors;

  private static CollectorPoolFactory lastFactory = null;

  /**
   * Constructing a CollectorPool is very costly, while constructing a collector
   * in a collector pool only impacts garbage collections. In order to avoid
   * excessive CPU-load, it is highly recommended to tweak maxSize,
   * freshCollectors and filledCollectors so that maxSize is never reached.
   * </p><p>
   * A new CollectorPool is constructed every time the group in the
   * {@link org.apache.lucene.search.exposed.facet.request.FacetRequest} is
   * modified. A new collector is constructed when a facet request is made and
   * all current collectors for the given pool are active.
   * @param maxSize       the maximum number of CollectorPools to keep cached.
   * @param filledCollectors the maximum number of fresh collectors in each
   *                      pool.
   * @param freshCollectors the maximum number of fresh collectors in each pool.
   * see {@link CollectorPool}.
   */
  public CollectorPoolFactory(
      final int maxSize, int filledCollectors, int freshCollectors) {
    poolMap = new LinkedHashMap<String, CollectorPool>(maxSize, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(
          Map.Entry<String, CollectorPool> eldest) {
        return size() > maxSize;
      }
    };
    this.freshCollectors = freshCollectors;
    this.filledCollectors = filledCollectors;
    lastFactory = this;
    ExposedCache.getInstance().addRemoteCache(this);
  }

  /**
   * An ugly hack to get singleton-like behaviour with initialization.
   * @return the last factory generated.
   */
  // TODO: Replace this with proper singleton
  public static CollectorPoolFactory getLastFactory() {
    return lastFactory;
  }

  /**
   * If a matching CollectorPool exists, it is returned. If not, a new pool is
   * created. Note that pool creation is costly.
   * @param reader a reader for the full index.
   * @param request specified the behaviour of the wanted CollectorPool.
   * @return a CollectorPool matching the request.
   * @throws java.io.IOException if the reader could not be accessed.
   */
  public synchronized CollectorPool acquire(
      IndexReader reader, FacetRequest request) throws IOException {
    if (readers.add(reader)) {
      reader.addReaderClosedListener(this);
    }
    final String key = request.getGroupKey();
    CollectorPool pool = poolMap.get(key);
    if (pool != null) {
      poolMap.put(key, pool); // Re-insert to support LRU-pool
      return pool;
    }

    if (ExposedSettings.debug) {
      System.out.println("Creating CollectorPool for " + key);
    }
    List<FacetRequestGroup> groups = request.getGroups();
    List<TermProvider> termProviders =
        new ArrayList<TermProvider>(groups.size());
    for (FacetRequestGroup group: groups) {
      TermProvider provider = ExposedCache.getInstance().getProvider(
          reader, group.getGroup());
      if (group.isHierarchical()) {
        provider = new HierarchicalTermProvider(
            provider, group.getDelimiter());
      }
      termProviders.add(provider);
    }

    long mapTime = -System.currentTimeMillis();
    FacetMap facetMap = new FacetMap(reader.maxDoc(), termProviders);
    mapTime += System.currentTimeMillis();
//    System.out.println("Map time: " + mapTime + "ms");
    pool = new CollectorPool(facetMap, filledCollectors, freshCollectors);
    poolMap.put(key, pool);
    return pool;
  }

  public synchronized void clear() {
    poolMap.clear();
  }

  public void purgeAllCaches() {
    if (ExposedSettings.debug) {
      System.out.println("CollectorPoolFactory.purgeAllCaches() called");
    }
    clear();
  }

  public void purge(IndexReader r) {
    if (ExposedSettings.debug) {
      System.out.println("CollectorPoolFactory.purge(" + r + ") called");
    }
    // TODO: Make a selective clear
    clear();
  }

  public String toString() {
    StringWriter sw = new StringWriter();
    sw.append("CollectorPoolFactory(");
    boolean first = true;
    for (Map.Entry<String, CollectorPool> entry: poolMap.entrySet()) {
      if (first) {
        first = false;
      } else {
        sw.append(", ");
      }
      sw.append(entry.getValue().toString());
    }
    sw.append(")");
    return sw.toString();
  }

  @Override
  public void onClose(IndexReader reader) {
    purge(reader);
  }
}
