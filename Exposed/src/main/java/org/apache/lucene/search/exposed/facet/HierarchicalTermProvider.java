package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ELog;
import org.apache.lucene.util.packed.GrowingMutable;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Augments the hierarchical tag ordered entries from another TermProvider with
 * level (A=1, A/B=2, A/B/C=3) and previous level match (A, A/B/C, D/E has 0, 1
 * and 0 respectively as there is no previous tag for A, "A" matches level 1 of
 * "A/B/C" and "D/E" has no previous match at any level).
 * </p><p>
 * The augmentation is used when doing hierarchical faceting. The memory
 * overhead is {@code 2 * log2(maxdepht+1) bits}. Examples:<br/>
 * 10 million tags with a maximum depth of 7 takes up 7 MB of heap.<br/>
 * 50 million tags with a maximum depth of 30 takes up 60 MB of heap<br/>
 * @see <a href="http://sbdevel.wordpress.com/2010/10/05/fast-hierarchical-faceting/">Fast, light, n-level hierarchical faceting</a>
 */
// TODO: Consider optimizing the cases where log2(maxlevel+1) <= 4 and <= 8
// by backing with byte[] and [short] instead pf PackedInts.
// TODO: Consider optimizing build with split on byte(s)
// TODO: Make splitting use the display part of the terms
public class HierarchicalTermProvider implements TermProvider {
  private static final ELog log = ELog.getLog(HierarchicalTermProvider.class);

  private final TermProvider source;
  private final PackedInts.Reader levels;
  private final PackedInts.Reader pLevels;

  private enum SPLIT_TYPE {regexp, byteEntry}
  private final SPLIT_TYPE splitType;
  private final String splitRegexp;
  private final int splitByte;
  private final Pattern splitPattern;

  /**
   * Calculates levels and previous level match for all ordered terms in the
   * given provider. The HierarchicalTermProvider is ready for use after
   * construction.
   * Note that the caller is responsible for any escaping mechanism and for
   * ensuring that the splitRegexp works consistently with character escaping.
   * @param source      a plain TermProvider containing hierarchical tags.
   * @param splitRegexp the expression for splitting the tags from source into
   *                    their level-components. If the tags is of the form
   *                    A/B/C, the regexp would be "/".
   * @throws java.io.IOException if the source could not be accessed.
   */
  public HierarchicalTermProvider(TermProvider source, String splitRegexp) throws IOException {
    long buildTime = -System.currentTimeMillis();
    this.source = source;
    splitType = SPLIT_TYPE.regexp;
    splitByte = -1;
    this.splitRegexp = splitRegexp;
    splitPattern = Pattern.compile(splitRegexp);
    Pair<PackedInts.Reader, PackedInts.Reader> lInfo = getLevels();
    levels = lInfo.getVal1();
    pLevels = lInfo.getVal2();
    buildTime += System.currentTimeMillis();
    log.info("Extracted Hierarchical information from source with " + source.getUniqueTermCount() + " unique terms in "
             + buildTime + "ms using regexp '" + splitRegexp + "'");
  }

  /**
   * Calculates levels and previous level match for all ordered terms in the
   * given provider. The HierarchicalTermProvider is ready for use after
   * construction.
   * Note that the caller is responsible for any escaping mechanism and for
   * ensuring that the splitRegexp works consistently with character escaping.
   * @param source    a plain TermProvider containing hierarchical tags.
   * @param splitByte the byte to split on when dividing in path parts.
   *                  If the tags is of the form A/B/C, the regexp would be
   *                  {@code '/'}.
   * @throws java.io.IOException if the source could not be accessed.
   */
  // TODO: Remove the decorate parameter
  public HierarchicalTermProvider(TermProvider source, int splitByte, boolean decorate) throws IOException {
    if (splitByte < 0 || splitByte > 255) {
      throw new IllegalArgumentException("The splitByte must be between 0 and 255, inclusive. It was " + splitByte);
    }
    long buildTime = -System.currentTimeMillis();
    this.source = source;
    splitRegexp = null;
    splitType = SPLIT_TYPE.byteEntry;
    this.splitByte = splitByte;
    splitPattern = null;
    Pair<PackedInts.Reader, PackedInts.Reader> lInfo = decorate ? getLevelsDecorating() : getLevelsFast();
    levels = lInfo.getVal1();
    pLevels = lInfo.getVal2();
    buildTime += System.currentTimeMillis();
    log.info("Extracted Hierarchical information from source with " + source.getUniqueTermCount() + " unique terms in "
             + buildTime + "ms using splitByte " + splitByte);
  }

  /**
   * Helper method for extracting the right tags. See the article that is linked
   * in the class documentation for details.
   * @param indirect an indirect for the underlying TermProvider.
   * @param level the level to match against.
   * @return true if {@code levels.get(indirect) >= level &&
   *                       pLevels.get(indirect) < level}.
   */
  public boolean matchLevel(int indirect, int level) {
    //return levels.get(indirect) >= level && pLevels.get(indirect) < level;
    return levels.get(indirect) >= level && pLevels.get(indirect) >= level-1;
  }

  /**
   * @param indirect an indirect for the underlying TermProvider.
   * @return the level for the given resolved tag. 'A' -> 1, 'A/B' -> 2 etc.
   */
  public int getLevel(int indirect) {
    return (int)levels.get(indirect);
  }

  /**
   * @param indirect an indirect for the underlying TermProvider.
   * @return the previous match level for the given resolved tag.
   */
  public int getPreviousMatchingLevel(int indirect) {
    return (int)pLevels.get(indirect);
  }

  private Pair<PackedInts.Reader, PackedInts.Reader> getLevels() throws IOException {
    PackedInts.Reader ordered = source.getOrderedOrdinals();
    // FIXME: GrowingMutable should grow up to upper limit for bitsPerValue
    final GrowingMutable levels = new GrowingMutable(0, ordered.size(), 0, 1, true);
    final GrowingMutable pLevels = new GrowingMutable(0, ordered.size(), 0, 1, true);
    String[] previous = new String[0];
    // TODO: Consider speeding up by sorting indirect chunks for seq. access
    // TODO: Consider using StringTokenizer or custom split
    long splitTime = 0;
    for (int index = 0 ; index < ordered.size() ; index++) {
      splitTime -= System.nanoTime();
      final String[] current = splitPattern.split(source.getOrderedTerm(index).utf8ToString());
      splitTime += System.nanoTime();
      int pLevel = 0;
      for (int level = 0 ; level < current.length && level < previous.length ; level++) {
        if (current[level].equals(previous[level])) {
          pLevel = level+1;
        }
      }
      levels.set(index, current.length);
      pLevels.set(index, pLevel);
      previous = current;
    }
    log.debug("Spend " + splitTime / 1000000 + " ms on " + ordered.size() + " splits: "
              + (ordered.size() * 1000000L / splitTime) + " splits/ms");
    return new Pair<PackedInts.Reader, PackedInts.Reader>(reduce(levels), reduce(pLevels));
  }

  private Pair<PackedInts.Reader, PackedInts.Reader> getLevelsFast() throws IOException {
    long initTime = -System.currentTimeMillis();
    PackedInts.Reader ordered = source.getOrderedOrdinals();
    // FIXME: GrowingMutable should grow up to upper limit for bitsPerValue
    final GrowingMutable levels = new GrowingMutable(0, ordered.size(), 0, 1, true);
    final GrowingMutable pLevels = new GrowingMutable(0, ordered.size(), 0, 1, true);
    List<BytesRef> previous = new ArrayList<BytesRef>(10);
    int previousParts = 0;
    // TODO: Consider speeding up by sorting indirect chunks for seq. access
    // TODO: Consider using StringTokenizer or custom split
    final List<BytesRef> current = new ArrayList<BytesRef>(10);
    initTime += System.currentTimeMillis();

    long splitTime = 0;
    for (int indirect = 0 ; indirect < ordered.size() ; indirect++) {
      splitTime -= System.nanoTime();
      // TODO: Make the splitting byte definable
      int parts = fastSplit(current, source.getOrderedTerm(indirect), '/');
//        final String[] current = splitPattern.split(
//            source.getOrderedTerm(indirect).utf8ToString());
      splitTime += System.nanoTime();
      int pLevel = 0;
      for (int level = 0 ; level < parts && level < previousParts ; level++) {
        if (current.get(level).equals(previous.get(level))) {
          pLevel = level+1;
        }
      }
      levels.set(indirect, parts);
      pLevels.set(indirect, pLevel);
      previous = current;
      previousParts = parts;
    }
    long reduceTime = -System.currentTimeMillis();
    Pair<PackedInts.Reader, PackedInts.Reader> reduced =
      new Pair<PackedInts.Reader, PackedInts.Reader>(reduce(levels), reduce(pLevels));
    reduceTime += System.currentTimeMillis();
    log.debug("getLevelsFast(): Init time: " + initTime + "ms. " + "Splits " + splitTime / 1000000 + " ms on "
              + ordered.size() + " splits (" + (ordered.size() * 1000000L / splitTime)
              + " splits/ms). levels grow: " + levels.getGrowTime() / 1000000
              + "ms. pLevels grow: " + pLevels.getGrowTime() / 1000000
              + "ms. Mutable size optimization: " + reduceTime + " ms");
    return reduced;
  }

  private Pair<PackedInts.Reader, PackedInts.Reader> getLevelsDecorating() throws IOException {
    long decorateTime = -System.currentTimeMillis();
    final GrowingMutable levels = new GrowingMutable(0, 1, 0, 1, true);
    final GrowingMutable pLevels = new GrowingMutable(0, 1, 0, 1, true);
    final long[] timings = new long[2]; // split, assign
    PackedInts.Reader ordered = source.getOrderedOrdinals(new OrderedDecorator() {
        List<BytesRef> previous = new ArrayList<BytesRef>(10);
        int previousParts = 0;
        final List<BytesRef> current = new ArrayList<BytesRef>(10);

        @Override
        public void decorate(BytesRef term, long indirect) {
          timings[0] -= System.nanoTime();
          // TODO: Make the splitting byte definable
          int parts = fastSplit(current, term, '/');
          timings[0] += System.nanoTime();
          int pLevel = 0;
          for (int level = 0 ; level < parts && level < previousParts ; level++) {
            if (current.get(level).equals(previous.get(level))) {
              pLevel = level+1;
            }
          }
          timings[1] -= System.nanoTime();
          levels.set((int)indirect, parts);
          pLevels.set((int)indirect, pLevel);
          timings[1] += System.nanoTime();
          previous = current;
          previousParts = parts;
        }
      });
    decorateTime += System.currentTimeMillis();

    long reduceTime = -System.currentTimeMillis();
    Pair<PackedInts.Reader, PackedInts.Reader> reduced =
      new Pair<PackedInts.Reader, PackedInts.Reader>(reduce(levels), reduce(pLevels));
    reduceTime += System.currentTimeMillis();
    log.debug("getLevelsDecorating(): Total time: " + decorateTime / 1000000 + ", splits " + timings[0] / 1000000
              + " ms on " + ordered.size() + " splits (" + (ordered.size() * 1000000L / timings[0]) + " splits/ms)"
              + ", assignments " + timings[1] / 1000000 + " ms on " + ordered.size() + " level-assignments ("
              + (ordered.size() * 1000000L / timings[1]) + " splits/ms). Mutable size optimization: " + reduceTime
              + " ms");
    return reduced;
  }

  /**
   * Fast byte-based split with low object allocations. Requires the
   * splitting element to be a single byte. Works like String.split(...).
   * @param reuse     re-usable buffer to lower object allocation.
   * @param input     The term to split.
   * @param splitByte the byte to split on.
   * @return the number of valid elements n the reuse list.
   */
  private int fastSplit(final List<BytesRef> reuse, BytesRef input, int splitByte) {
    if (reuse == null) {
      throw new IllegalArgumentException("A valid list of BytesRefs must be provided. The given list was null");
    }
    int elementCount = 0;
    int start = input.offset;
    for (int i = input.offset ; i < input.offset + input.length ; i++) {
      if (input.bytes[i] == splitByte) {
        assign(reuse, ++elementCount, input, start, i-start);
      }
    }
    if (start != input.offset + input.length) {
      assign(reuse, ++elementCount, input, start, input.offset + input.length - start);
    }
    return elementCount;
  }

  private void assign(final List<BytesRef> reuse, int elementCount, BytesRef input, int offset, int length) {
    if (reuse.size() < elementCount) {
      reuse.add(new BytesRef(input.bytes, offset, length));
    } else {
      BytesRef element = reuse.get(elementCount-1);
      element.bytes = input.bytes;
      element.offset = offset;
      element.length = length;
    }
  }

  private PackedInts.Reader reduce(GrowingMutable grower) {
    PackedInts.Mutable reduced = PackedInts.getMutable(grower.size(), grower.getBitsPerValue(), PackedInts.COMPACT);
    for (int i = 0 ; i < grower.size() ; i++) {
      reduced.set(i, grower.get(i));
    }
    return reduced;
  }

  private final class Pair<S, T> {
    private final S val1;
    private final T val2;
    private Pair(S val1, T val2) {
      this.val1 = val1;
      this.val2 = val2;
    }
    public S getVal1() {
      return val1;
    }
    public T getVal2() {
      return val2;
    }
  }

  public String toString() {
      return "HierarchicalTermProvider("  + getMemUsage()/1048576 + "MB, source=" + source + ")";
  }

  /* Plain delegations */

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
  public String getDesignation() {
    return source.getDesignation();
  }

  @Override
  public String getField(long ordinal) throws IOException {
    return source.getField(ordinal);
  }

  @Override
  public BytesRef getTerm(long ordinal) throws IOException {
    return source.getTerm(ordinal);
  }

  @Override
  public BytesRef getDisplayTerm(long ordinal) throws IOException {
    return source.getDisplayTerm(ordinal);
  }

  @Override
  public BytesRef getOrderedDisplayTerm(long indirect) throws IOException {
    return source.getOrderedDisplayTerm(indirect);
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
  public IndexReader getReader() {
    return source.getReader();
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
  public Iterator<ExposedTuple> getIterator(
      boolean collectDocIDs) throws IOException {
    return source.getIterator(collectDocIDs);
  }

  @Override
  public DocsEnum getDocsEnum(long ordinal, DocsEnum reuse) throws IOException {
    return source.getDocsEnum(ordinal, reuse);
  }

  @Override
  public void transitiveReleaseCaches(int level, boolean keepRoot) {
    source.transitiveReleaseCaches(level, keepRoot);
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
  public long getMemUsage() {
    return source.getMemUsage()
           + (levels == null ? 0 : levels.ramBytesUsed())
           + (pLevels == null ? 0 : pLevels.ramBytesUsed());
  }
}
