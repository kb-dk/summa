package org.apache.lucene.search.exposed;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.Iterator;

/**
 * Wrapper for TermProvider that provides a flexible cache for terms.
 * Warning: This cache does not work well for docIDs.
 * </p><p>
 * For random access, a plain setup with no read-ahead is suitable.
 * </p><p>
 * For straight iteration in ordinal order, a setup with maximum read-ahead
 * is preferable. This is uncommon.
 * </p><p>
 * For iteration in sorted ordinal order, where the ordinal order is fairly
 * aligned to unicode sorting order, a setup with some read-ahead works well.
 * This is a common case.
 * </p><p>
 * For merging chunks (See the SegmentReader.getSortedTermOrdinals), where
 * the ordinal order inside the chunks if fairly aligned to unicode sorting
 * order, a read-ahead works iff {@link #onlyReadAheadIfSpace} is true as this
 * prevents values from the beginning of other chunks from being flushed when
 * values from the current chunk is filled with read-ahead. This requires that
 * the merger removes processed values from the cache explicitly.
 * </p><p>
 * If the underlying order is used by calling {@link #getOrderedTerm(long)},
 * {@link #getOrderedField(long)} or {@link #getOrderedOrdinals()}, the
 * methods are delegated directly to the backing TermProvider. It is the
 * responsibility of the caller to endure that proper caching of the order
 * is done at the source level.
 */
public class CachedTermProvider extends CachedProvider<BytesRef> implements TermProvider {
  private final TermProvider source;

  /**
   *
   * @param source       the backing term provider.
   * @param cacheSize    the maximum number of elements to hold in cache.
   * @param readAhead    the maximum number of lookups that can be performed after a plain lookup.
   * @throws java.io.IOException if the cache could access the source.
   */
  public CachedTermProvider(TermProvider source, int cacheSize, int readAhead) throws IOException {
    super(cacheSize, readAhead, source.getOrdinalTermCount()-1);
    this.source = source;
  }

  @Override
  protected BytesRef lookup(final long index) throws IOException {
    return source.getTerm(index);
  }

  @Override
  public BytesRef getTerm(final long ordinal) throws IOException {
    return get(ordinal);
  }

  // TODO: Add display terms to caching system
  @Override
  public BytesRef getDisplayTerm(long ordinal) throws IOException {
    return source.getDisplayTerm(ordinal);
  }

  @Override
  public Iterator<ExposedTuple> getIterator(boolean collectDocIDs) throws IOException {
    throw new UnsupportedOperationException("The cache does not support the creation of iterators");
  }

  @Override
  public String getDesignation() {
    return "CachedTermProvider(" + source.getClass().getSimpleName() + "(" + source.getDesignation() + "))";
  }

  @Override
  public void transitiveReleaseCaches(int level, boolean keepRoot) {
    clear();
    source.transitiveReleaseCaches(level, keepRoot);
  }

  /* Straight delegations */

  @Override
  public int getNearestTermIndirect(BytesRef key) throws IOException {
    return source.getNearestTermIndirect(key);
  }

  @Override
  public int getNearestTermIndirect(BytesRef key, int startTermPos, int endTermPos) throws IOException {
    return source.getNearestTermIndirect(key, startTermPos, endTermPos);
  }

  @Override
  public NamedComparator getComparator() {
    return source.getComparator();
  }

  @Override
  public String getField(long ordinal) throws IOException {
    return source.getField(ordinal);
  }

  @Override
  public String getOrderedField(long indirect) throws IOException {
    return source.getOrderedField(indirect);
  }

  @Override
  public BytesRef getOrderedTerm(long indirect) throws IOException {
    return source.getOrderedTerm(indirect);
  }

  @Override
  public BytesRef getOrderedDisplayTerm(long indirect) throws IOException {
    return source.getOrderedDisplayTerm(indirect);
  }

  @Override
  public long getUniqueTermCount() throws IOException {
    return source.getUniqueTermCount();
  }

  @Override
  public long getOrdinalTermCount() throws IOException {
    return source.getOrdinalTermCount();
  }

  @Override
  public long getMaxDoc() {
    return source.getMaxDoc();
  }

  @Override
  public long getMemUsage() {
    return source.getMemUsage() + size() * 50; // 50 chosen by loose but qualified guessing
  }

  @Override
  public IndexReader getReader() {
    return source.getReader();
  }

  @Override
  public DocsEnum getDocsEnum(long ordinal, DocsEnum reuse) throws IOException {
    return source.getDocsEnum(ordinal, reuse);
  }

  @Override
  public PackedInts.Reader getOrderedOrdinals() throws IOException {
    return source.getOrderedOrdinals();
  }

  @Override
  public PackedInts.Reader getOrderedOrdinals(OrderedDecorator decorator) throws IOException {
    return source.getOrderedOrdinals(decorator);
  }

  @Override
  public PackedInts.Reader getDocToSingleIndirect() throws IOException {
    return source.getDocToSingleIndirect();
  }

  @Override
  public int getReaderHash() {
    return source.getReaderHash();
  }

  @Override
  public int getRecursiveHash() {
    return source.getRecursiveHash();
  }

  @Override
  public int getDocIDBase() {
    return source.getDocIDBase();
  }

  @Override
  public void setDocIDBase(int base) {
    source.setDocIDBase(base);
  }

  @Override
  public String toString() {
    return "CachedTermProvider(" + getMemUsage()/1048576 + "MB, source=" + source + ')';
  }
}