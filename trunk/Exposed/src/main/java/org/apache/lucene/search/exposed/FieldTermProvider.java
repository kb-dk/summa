package org.apache.lucene.search.exposed;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;
import org.apache.lucene.index.*;
import org.apache.lucene.search.exposed.compare.ComparatorFactory;
import org.apache.lucene.search.exposed.compare.NamedCollatorComparator;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IndexUtil;
import org.apache.lucene.util.packed.IdentityReader;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.Iterator;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Keeps track of a single field from a single AtomicReader.
 * </p><p>
 * The provider is lazy meaning that the ordering is only calculated when
 * needed.
 */
public class FieldTermProvider extends TermProviderImpl {
  private ExposedRequest.Field request;
  private PackedInts.Reader order = null;


  // TODO: Consider letting the cache be a factor of term count
  /**
   * The minimum number of Terms or ExtendedTerms to hold in cache during
   * processing.
   * The allocated amount of memory depends on average term size and whether
   * CollatorKey-based sorting is used. With an average term-length of 20
   * characters, each cache-entry takes approximately 100 bytes without
   * CollatorKey and 300 with CollatorKey.
   * 
   * .
   * 10.000 is thus roughly equivalent to either 1 or 3 MB of RAM depending on
   * Collator presence.
   */
  public static int minSortCacheSize = 20000; // 2-6 MB
  public static double sortCacheFraction = 0.05; // 1M docs = 5-15 MB
  public static int maxSortCacheSize = 300000; // 30-90 MB
  private int sortCacheSize;

  public static int mergeChunkFragmentationFactor = 3; // TODO: Experiment with this
  public static int iteratorCacheSize = 5000; // TODO: Make this relative
  public static int iteratorReadAhead = 100;

  /**
   * If this is true and the given comparator is a
   * {@link org.apache.lucene.search.exposed.compare.NamedCollatorComparator}, sorting is optimized by CollatorKeys.
   * This requires ~1-3 MB extra memory but doubles the chunk-sort speed.
   */
  public static boolean optimizeCollator = true;

  private Terms terms;
  private TermsEnum termsEnum;

  public FieldTermProvider(IndexReader reader, int docIDBase,
                           ExposedRequest.Field request,
                           boolean cacheTables) throws IOException {
    super(checkReader(reader), docIDBase, request.getComparator(),
        "Field " + request.getField(), cacheTables);
    if (!(reader instanceof AtomicReader)) {
      throw new IllegalArgumentException(
          "The IndexReader should be a leaf (no sub readers). It contained "
              + IndexUtil.flatten(reader).size() + " sub readers");
    }
    this.request = request;

    terms = ((AtomicReader)reader).fields().terms(request.getField());
    // TODO: Make an OrdTermsEnum to handle Variable Gap and other non-ord-codec
//    termsEnum = terms == null ? null : terms.iterator(); // It's okay to be empty
    termsEnum = OrdinalTermsEnum.createEnum((AtomicReader)reader, request.getField(), 128);
    sortCacheSize = getSortCacheSize(reader);
  }

  private static IndexReader checkReader(IndexReader reader) {
    if (!(reader instanceof AtomicReader)) {
      throw new IllegalArgumentException(
          "The reader for a FieldTermProvider must not contain sub readers");
    }
    return reader;
  }

    @Override
    public AtomicReader getReader() {
      return (AtomicReader)super.getReader();
    }

    public ExposedRequest.Field getRequest() {
    return request;
  }

  @Override
  public String getField(long ordinal) {
    return request.getField();
  }

  private static final int ITERATE_LIMIT = 10;
  private long lastOrdinalRequest = -1;
  @Override
  public synchronized BytesRef getTerm(final long ordinal) throws IOException {
    if (termsEnum == null) {
      throw new IOException("No terms for field " + request.getField()
          + " in segment " + getReader()
          + ". Requested ordinal was " + ordinal);
    }
    // TODO: Upstream this simple sequential access optimization
    if (lastOrdinalRequest != -1 && lastOrdinalRequest <= ordinal &&
        ordinal <= lastOrdinalRequest + ITERATE_LIMIT) {
      BytesRef term = termsEnum.term();
      while (lastOrdinalRequest != ordinal) {
        term = termsEnum.next();
        if (term == null) {
          throw new IOException("Unable to locate term for ordinal " + ordinal
                                + ". Last ordinalRequest was " + lastOrdinalRequest);
        }
        lastOrdinalRequest++;
      }
      lastOrdinalRequest = ordinal;
      return copy(term);
    }

    termsEnum.seekExact(ordinal);
    /*if ( TermsEnum.SeekStatus.FOUND != termsEnum.seek(ordinal)) {
      throw new IOException("Unable to locate term for ordinal " + ordinal);
    }*/
    BytesRef result = termsEnum.term();
    lastOrdinalRequest = ordinal;
    return copy(result);
  }
  private BytesRef copy(BytesRef br) {
      // The clone produces a new Bytesref with shared underlying bytes!
      byte[] bytes = new byte[br.length];
      System.arraycopy(br.bytes, br.offset, bytes, 0, br.length);
      return new BytesRef(bytes, 0, br.length);
  }

  @Override
  public DocsEnum getDocsEnum(long ordinal, DocsEnum reuse) throws IOException {
    if (termsEnum == null) {
      throw new IOException("No terms or docIDs for field " + request.getField()
          + " in segment " + getReader()
          + ". Requested ordinal was " + ordinal);
    }
    if (ordinal != lastOrdinalRequest) {
      termsEnum.seekExact(ordinal);
       //&& TermsEnum.SeekStatus.FOUND != termsEnum.seek(ordinal)) {
//      throw new IOException("Unable to locate term for ordinal " + ordinal);
    }

    lastOrdinalRequest = ordinal;
    //
    return termsEnum.docs(getReader().getLiveDocs(), reuse);
  }

  @Override
  public String getOrderedField(long indirect) throws IOException {
    return request.getField();
  }

  @Override
  public BytesRef getOrderedTerm(final long indirect) throws IOException {
    return indirect == -1 ? null :
        getTerm(getOrderedOrdinals().get((int)indirect));
  }

  @Override
  public long getUniqueTermCount() throws IOException {
    long termCount = terms == null ? 0 :
        termsEnum instanceof OrdinalTermsEnum ?
            ((OrdinalTermsEnum) termsEnum).getTermCount() :
            terms.size();
    // TODO: terms.size() does not handle deleted documents and might return -1
    if (termCount == -1) {
      throw new UnsupportedOperationException(
        "The Terms.size() returned -1 and explicit counting is not "
        + "implemented yet");
    }
    return termCount;
  }

  @Override
  public long getOrdinalTermCount() throws IOException {
    return getUniqueTermCount(); // FieldTermProviders only contains uniques
  }

  @Override
  public long getMaxDoc() {
    return getReader().maxDoc();
  }

  @Override
  public int getReaderHash() {
    return getReader().hashCode();
  }

  @Override
  public int getRecursiveHash() {
    return getReader().hashCode();
  }

  @Override
  public PackedInts.Reader getOrderedOrdinals() throws IOException {
    if (termsEnum == null) {
      return ExposedSettings.getMutable(0, 1);
    }

    if (order != null) {
      return order;
    }
    PackedInts.Reader newOrder;
    if (NamedComparator.ORDER.index == request.getComparator().getOrder()
        || NamedComparator.ORDER.count == request.getComparator().getOrder()) {
      newOrder = new IdentityReader((int)getUniqueTermCount());
    } else {
      newOrder = sortOrdinals();
    }

    if (cacheTables) {
      order = newOrder;
    }
    return newOrder;
  }

  @Override
  public PackedInts.Reader getOrderedOrdinals(OrderedDecorator decorator) throws IOException {
    PackedInts.Reader ordered = getOrderedOrdinals();
    for (int indirect = 0 ; indirect < ordered.size() ; indirect++) {
      decorator.decorate(getOrderedTerm(indirect), indirect);
    }
    return ordered;
  }

  @Override
  public Iterator<ExposedTuple> getIterator(
      boolean collectDocIDs) throws IOException {
    PackedInts.Reader order = getOrderedOrdinals();
    if (collectDocIDs) {
      return new TermDocIterator(this, true);
    }
    this.order = order;
    CachedTermProvider cache = new CachedTermProvider(
        this, iteratorCacheSize, iteratorReadAhead);
    return new TermDocIterator(cache, false);
  }

  private PackedInts.Reader sortOrdinals() throws IOException {
    int termCount = (int)getOrdinalTermCount();
    int[] ordered = new int[termCount];
    for (int i = 0 ; i < termCount ; i++) {
      ordered[i] = i; // Initial order = Lucene
    }

    long startTime = System.nanoTime();
    sort(ordered);
    long sortTime = (System.nanoTime() - startTime);


    // TODO: Remove this
    if (ExposedSettings.debug) {
      System.out.println("Chunk total sort for field " + getField(0)
          + ": " + ExposedUtil.time(
          "terms", ordered.length, sortTime / 1000000));
    }
/*    System.out.println(String.format(
            "Sorted %d Terms in %s out of which %s (%s%%) was lookups and " +
                    "%s (%s%%) was collation key creation. " +
                   "The cache (%d terms) got %d requests with %d (%s%%) misses",
            termCount, nsToString(sortTime),
            nsToString(lookupTime),
            lookupTime * 100 / sortTime,
            nsToString(collatorKeyCreation),
            collatorKeyCreation * 100 / sortTime,
            sortCacheSize, cacheRequests,
            cacheMisses, cacheMisses * 100 / cacheRequests));
  */
    PackedInts.Mutable packed =
        ExposedSettings.getMutable(termCount, termCount);
    for (int i = 0 ; i < termCount ; i++) {
      packed.set(i, ordered[i]); // Save space by offsetting min to 0
    }
    return packed;
  }

  /*
   * Starts by dividing the ordered array in logical chunks, then sorts each
   * chunk separately and finishes by merging the chunks.
   */
  private void sort(final int[] ordinals) throws IOException {
    int chunkSize = Math.max(sortCacheSize, ordinals.length / sortCacheSize);
    int chunkCount = (int) Math.ceil(ordinals.length * 1.0 / chunkSize);

    // We do not thread the sort as the caching of Strings is more important
    // than processing power.

    // We sort in chunks so the cache is 100% effective
    if (optimizeCollator
        && request.getComparator() instanceof NamedCollatorComparator) {
//      System.out.println("Using CollatorKey optimized chunk sort");
      optimizedChunkSort(ordinals, chunkSize);
    } else {
      chunkSort(ordinals, chunkSize);
    }

    if (chunkSize >= ordinals.length) {
      return; // Only one chunk. No need for merging
    }

    // We have sorted chunks. Commence the merging!

    chunkMerge(ordinals, chunkSize, chunkCount);
  }

  private void chunkMerge(
      int[] ordinals, int chunkSize, int chunkCount) throws IOException {
    // Merging up to cache-size chunks requires an efficient way to determine
    // the chunk with the lowest value. As locality is not that important with
    // all comparables in cache, we use a heap.
    // The heap contains an index (int) for all active chunks in the term order
    // array. When an index is popped, it is incremented and re-inserted unless
    // it is a block start index in which case it is just discarded.

    if (ExposedSettings.debug) {
      System.out.println("Beginning merge sort of " + ordinals.length
          + " ordinals in " + chunkCount + " chunks");
    }
    long mergeTime = System.currentTimeMillis();
    CachedProvider cache;
    ComparatorFactory.OrdinalComparator indirectComparator;
    if (optimizeCollator
        && request.getComparator() instanceof NamedCollatorComparator) {
      Collator collator =
        ((NamedCollatorComparator)request.getComparator()).getCollator();
      // TODO: Hanele isReverse & isNullFirst
      CachedCollatorKeyProvider keyCache = new CachedCollatorKeyProvider(
          this, collator, sortCacheSize, chunkSize-1);
      keyCache.setReadAhead(Math.max(100, sortCacheSize / chunkCount /
          mergeChunkFragmentationFactor));
      keyCache.setOnlyReadAheadIfSpace(true);
      keyCache.setStopReadAheadOnExistingValue(true);
      indirectComparator = ComparatorFactory.wrapIndirect(keyCache, ordinals);
      cache = keyCache;
    } else {
      CachedTermProvider termCache = new CachedTermProvider(
          this, sortCacheSize, chunkSize-1);
      // Configure the cache so that read ahead is lower as the access pattern
      // to the ordinals are not guaranteed linear
      termCache.setReadAhead(Math.max(100, sortCacheSize / chunkCount /
          mergeChunkFragmentationFactor));
      termCache.setOnlyReadAheadIfSpace(true);
      termCache.setStopReadAheadOnExistingValue(true);
      for (int i = 0 ; i < ordinals.length ; i += chunkSize) {
        termCache.getTerm(i); // Warm cache
      }
      indirectComparator = ComparatorFactory.wrapIndirect(
        termCache, ordinals, request.getComparator());
      cache = termCache;
    }

    ExposedPriorityQueue pq = new ExposedPriorityQueue(
        indirectComparator, chunkCount);
    for (int i = 0 ; i < ordinals.length ; i += chunkSize) {
      pq.add(i);
    }

    int[] sorted = new int[ordinals.length];
    for (int i = 0 ; i < sorted.length ; i++) {
      Integer next = pq.pop();
      if (next == -1) {
        throw new IllegalStateException(
            "Popping the heap should never return -1");
      }
      sorted[i] = ordinals[next];
      cache.release(sorted[i]); // Important for cache read ahead efficiency
      if (++next % chunkSize != 0 && next != ordinals.length) {
        pq.add(next);
      }
    }
    mergeTime = System.currentTimeMillis() - mergeTime;
    if (ExposedSettings.debug) {
      System.out.println(
          "Merged " + ExposedUtil.time("chunks", chunkCount, mergeTime)
              + " aka " + ExposedUtil.time("terms", sorted.length, mergeTime)
              + ": " + cache.getStats());
    }
    System.arraycopy(sorted, 0, ordinals, 0, sorted.length);
    if (ExposedSettings.debug) {
      System.out.println("Cache stats for chunkMerge for " + getDesignation()
          + ": " + cache.getStats());
/*    System.out.println(String.format(
        "Heap merged %d sorted chunks of size %d (cache: %d, total terms: %s)" +
            " in %s with %d cache misses (%d combined for both sort passes)",
        chunkCount, chunkSize, sortCacheSize, ordinals.length,
        nsToString(System.nanoTime() - startTimeHeap),
        cacheMisses - oldHeapCacheMisses, cacheMisses - oldCacheMisses));*/
    }
  }

  private void chunkSort(int[] ordinals, int chunkSize) throws IOException {
    CachedTermProvider cache = new CachedTermProvider(
        this, sortCacheSize, chunkSize-1);
    // Sort the chunks individually
    //long startTimeMerge = System.nanoTime();
    ComparatorFactory.OrdinalComparator comparator =
        ComparatorFactory.wrap(cache, request.getComparator());
    for (int i = 0 ; i < ordinals.length ; i += chunkSize) {
      long chunkTime = System.currentTimeMillis();
      cache.getTerm(i); // Tim-sort starts at 1 so we init the read-ahead at 0
      ExposedTimSort.sort(
          ordinals, i, Math.min(i + chunkSize, ordinals.length), comparator);
      chunkTime = System.currentTimeMillis() - chunkTime;
      int percent = (Math.min(i + chunkSize, ordinals.length)-1) *100
          / ordinals.length;
      if (ExposedSettings.debug) {
        System.out.println("Chunk sorted " + percent + "% " + i + "-"
            + (Math.min(i + chunkSize, ordinals.length)-1) + ": "
            + ExposedUtil.time("ordinals", Math.min(i + chunkSize,
            ordinals.length) - i, chunkTime) + ": "+ cache.getStats());
      }
      cache.clear();
    }
  }

  // Ordinals _must_ be monotonously increasing
  private void optimizedChunkSort(
      int[] ordinals, int chunkSize) throws IOException {
    Collator collator =
      ((NamedCollatorComparator)request.getComparator()).getCollator();
    CachedTermProvider cache = new CachedTermProvider(
        this, sortCacheSize, chunkSize-1);
    CollatorPair[] keys = new CollatorPair[chunkSize];
    for (int start = 0 ; start < ordinals.length ; start += chunkSize) {
      long chunkTime = System.currentTimeMillis();

      int end = Math.min(start + chunkSize, ordinals.length); // Exclusive
      // Fill
      for (int index = start ; index < end ; index++) {
        RawCollationKey key = new RawCollationKey();
        keys[index - start] = new CollatorPair(
            ordinals[index], collator.getRawCollationKey(
                cache.getTerm(ordinals[index]).utf8ToString(), key));
      }
      // Sort
      ExposedTimSort.sort(
          ordinals, start, end, new TimComparator(start, keys));
/*      // Store
      for (int index = start ; index < end ; index++) {
        ordinals[index] = (int) keys[index - start].ordinal;
      }*/

      chunkTime = System.currentTimeMillis() - chunkTime;
      int percent = (int) ((long)end * 100 / ordinals.length);
      if (ExposedSettings.debug) {
        System.out.println("Chunk sorted " + percent + "% "
            + start + "-" + (end-1) + ": "
            + ExposedUtil.time("ordinals", end - start, chunkTime)
            + ": "+ cache.getStats());
      }
      cache.clear();
    }
  }
  private static final class TimComparator
                               implements ComparatorFactory.OrdinalComparator {
    private int start; // Inclusive
    private CollatorPair[] keys;

    public TimComparator(int start, CollatorPair[] keys) {
      this.start = start;
      this.keys = keys;
    }

    @Override
    public int compare(int value1, int value2) {
      return keys[value1 - start].compareTo(keys[value2 - start]);
    }
  }


  private static final class CollatorPair implements Comparable<CollatorPair>{
    private long ordinal;
    private RawCollationKey key;

    private CollatorPair(long ordinal, RawCollationKey key) {
      this.ordinal = ordinal;
      this.key = key;
    }
    @Override
    public int compareTo(CollatorPair o) {
      return key.compareTo(o.key);
    }
    @SuppressWarnings({"UnusedDeclaration"})
    public long getOrdinal() {
      return ordinal;
    }
  }

  /**
   * Calculate the sort cache size as a fraction of the total number of
   * documents in the reader,
   * @param reader the reader to cache.
   * @return a cache size that fits in scale to the reader.
   */
  private int getSortCacheSize(IndexReader reader) {
    final int fraction = (int) (reader.maxDoc() * sortCacheFraction);
    return Math.min(maxSortCacheSize, Math.max(minSortCacheSize, fraction));
  }

  public String toString() {
    if (order == null) {
      return "FieldTermProvider(" + request.getField() + ", no order cached, "
          + super.toString() + ")";
    }
    return "FieldTermProvider(" + request.getField() 
        + ", order.length=" + order.size()
        + " mem=" + packedSize(order) + ", " + super.toString() + ")";
  }

  @Override
  public void transitiveReleaseCaches(int level, boolean keepRoot) {
    if (!keepRoot || level > 0) {
      order = null;
    }
    super.transitiveReleaseCaches(level, keepRoot);
  }
}