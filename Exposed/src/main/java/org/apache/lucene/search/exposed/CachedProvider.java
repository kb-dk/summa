package org.apache.lucene.search.exposed;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class CachedProvider<T> {
  private final Map<Long, T> cache;
  private int cacheSizeNum;
  private int readAheadNum;
  private final long finalIndex; // Do not read past this

  private boolean onlyReadAheadIfSpace = false;
  private boolean stopReadAheadOnExistingValue = true;

  // Stats
  private long requests = 0;
  private long lookups = 0;
  private long readAheadRequests = 0;
  private long misses = 0;
  private long lookupTime = 0;

  protected CachedProvider(int size, int readAhead, long finalIndex) {
    this.cacheSizeNum = size;
    this.readAheadNum = readAhead;
    this.finalIndex = finalIndex;
    cache = new LinkedHashMap<Long, T>(size, 1.2f, false) {
      @Override
      protected boolean removeEldestEntry(
              Map.Entry<Long, T> eldest) {
        return size() > cacheSizeNum;
      }
    };
  }

  protected abstract T lookup(final long index) throws IOException;

  /**
   * @return short form human readable description for the wrapped provider.
   */
  protected abstract String getDesignation();

  public T get(final long index) throws IOException {
    requests++;
    T entry = cache.get(index);
    if (entry != null) {
      return entry;
    }
    misses++;

    long startLookup = System.nanoTime();
    entry = lookup(index);
    lookupTime += System.nanoTime() - startLookup;
    lookups++;
    cache.put(index, entry);
    readAhead(index+1, readAheadNum);
    return entry;

  }

  private void readAhead(final long startIndex, final int readAhead)
                                                            throws IOException {
    long index = startIndex;
    int readsLeft = readAhead;
    while (true) {
      if (readsLeft == 0 || index > finalIndex||
          (onlyReadAheadIfSpace && cache.size() >= cacheSizeNum)) {
        break;
      }
      readAheadRequests++;
      T entry = cache.get(index);
      if (entry == null) {
        long startLookup = System.nanoTime();
        entry = lookup(index);
        lookupTime += System.nanoTime() - startLookup;
        lookups++;
        if (entry == null) {
          break;
        }
/*        if (index > 19999) {
          System.out.println("Putting " + index);
        }*/
        cache.put(index, entry);
      } else if (stopReadAheadOnExistingValue) {
        break;
      }
      index++;
      readsLeft--;
    }
  }

  /**
   * Removes the cache entry for the given index from cache is present.
   * </p><p>
   * If the user of the cache knows that the String for a given ordinal should
   * not be used again, calling this method helps the cache to perform better.
   * @param index the ordinal for the elementto remove.
   * @return the old element if it was present in the cache.
   */
  public T release(final long index) {
    return cache.remove(index);
  }

  /**
   * Clears the cache.
   */
  public void clear() {
    cache.clear();
    requests = 0;
    lookups = 0;
    readAheadRequests = 0;
    misses = 0;
    lookupTime = 0;
  }

  public int getCacheSize() {
    return cacheSizeNum;
  }

  /**
   * Setting the cache size clears the cache.
   * @param cacheSize the new size of the cache, measured in elements.
   */
  public void setCacheSize(int cacheSize) {
    this.cacheSizeNum = cacheSize;
    cache.clear();
  }

  public int getReadAhead() {
    return readAheadNum;
  }

  public void setReadAhead(int readAhead) {
    this.readAheadNum = readAhead;
  }

  public boolean isOnlyReadAheadIfSpace() {
    return onlyReadAheadIfSpace;
  }

  public void setOnlyReadAheadIfSpace(boolean onlyReadAheadIfSpace) {
    this.onlyReadAheadIfSpace = onlyReadAheadIfSpace;
  }

  public boolean isStopReadAheadOnExistingValue() {
    return stopReadAheadOnExistingValue;
  }

  public void setStopReadAheadOnExistingValue(
                                         boolean stopReadAheadOnExistingValue) {
    this.stopReadAheadOnExistingValue = stopReadAheadOnExistingValue;
  }

  public String getStats() {
    return getDesignation() + " cacheSize=" + cacheSizeNum
        + ", misses=" + misses + "/" + requests
        + ", lookups=" + lookups + " (" + lookupTime / 1000000 + " ms ~= "
        + (lookupTime == 0 ? "N/A" : lookups * 1000000 / lookupTime)
        + " lookups/ms), readAheads=" + readAheadRequests;
  }
}
