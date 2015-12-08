package org.apache.lucene.search.exposed;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;

import java.io.IOException;

public class CachedCollatorKeyProvider extends CachedProvider<RawCollationKey> {
  private final TermProvider source;
  private final Collator collator;

  /**
   * @param source       the backing term provider.
   * @param collator     the Collator used for key generation.
   * @param cacheSize    the maximum number of elements to hold in cache.
   * @param readAhead    the maximum number of lookups that can be performed
   *                     after a plain lookup.
   * @throws java.io.IOException if the cache could access the source.
   */
  public CachedCollatorKeyProvider(
      TermProvider source, Collator collator, int cacheSize, int readAhead) throws IOException {
    super(cacheSize, readAhead, source.getOrdinalTermCount()-1);
    this.source = source;
    this.collator = collator;
  }

  @Override
  protected RawCollationKey lookup(final long index) throws IOException {
    RawCollationKey key = new RawCollationKey();
    return collator.getRawCollationKey(source.getTerm(index).utf8ToString(), key);
  }

  @Override
  protected String getDesignation() {
    return "CachedCollatorKeyProvider(" + source.getClass().getSimpleName() + "(" + source.getDesignation() + "))";
  }
}