package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.packed.GrowingMutable;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Augments the hierarchical tag ordered entries from another TermProvider with
 * level (A=1, A/B=2, A/B/C=3) and previous level match (A, A/B/C, D/E has 0, 1
 * and 0 respectively as there is no previous tag for A, "A" matches level 1 of
 * "A/B/C" and "D/E" has no previous match at any level).
 * </p><p>
 * The augmentation is used when doing hierarchical faceting. The memory
 * overhead is {@code 2 * log2(maxdepht+1) bits}. Examples:
 * 10 million tags with a maximum depth of 7 takes up 7 MB of heap.
 * 50 million tags with a maximum depth of 30 takes up 60 MB of heap
 * @see <a href="http://sbdevel.wordpress.com/2010/10/05/fast-hierarchical-faceting/">Fast, light, n-level hierarchical faceting</a>.
 */
// TODO: Consider optimizing the cases where log2(maxlevel+1) <= 4 and <= 8
// by backing with byte[] and [short] instead pf PackedInts.
// TODO: Consider optimizing build with split on byte(s)
public class HierarchicalTermProvider implements TermProvider {
  private final TermProvider source;
  private final PackedInts.Reader levels;
  private final PackedInts.Reader pLevels;
  private final String splitRegexp;
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
  public HierarchicalTermProvider(TermProvider source, String splitRegexp)
                                                            throws IOException {
    long buildTime = -System.currentTimeMillis();
    this.source = source;
    this.splitRegexp = splitRegexp;
    splitPattern = Pattern.compile(splitRegexp);
    Pair<PackedInts.Reader, PackedInts.Reader> lInfo = getLevels();
    levels = lInfo.getVal1();
    pLevels = lInfo.getVal2();
    buildTime += System.currentTimeMillis();
    if (ExposedSettings.debug) {
      System.out.println("Extracted Hierarchical information from source with "
          + source.getUniqueTermCount() + " unique terms in "
          + buildTime + "ms");
    }
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

  private Pair<PackedInts.Reader, PackedInts.Reader> getLevels() throws
                                                                   IOException {
    PackedInts.Reader ordered = source.getOrderedOrdinals();
    // FIXME: GrowingMutable should grow up to upper limit for bitsPerValue
    final GrowingMutable levels = new GrowingMutable(
        0, ordered.size(), 0, 1, true);
    final GrowingMutable pLevels = new GrowingMutable(
        0, ordered.size(), 0, 1, true);
    String[] previous = new String[0];
    // TODO: Consider speeding up by sorting indirect chunks for seq. access
    // TODO: Consider using StringTokenizer or custom split
    long splitTime = 0;
    for (int index = 0 ; index < ordered.size() ; index++) {
      splitTime -= System.nanoTime();
      final String[] current = splitPattern.split(
          source.getOrderedTerm(index).utf8ToString());
      splitTime += System.nanoTime();
      int pLevel = 0;
      for (int level = 0 ;
           level < current.length && level < previous.length ;
           level++) {
        if (current[level].equals(previous[level])) {
          pLevel = level+1;
        }
      }
      levels.set(index, current.length);
      pLevels.set(index, pLevel);
      previous = current;
    }
/*    System.out.println("Spend " + splitTime / 1000000 + " ms on "
        + ordered.size() + " splits: " + splitTime / ordered.size()
        + " ns/split");*/
    return new Pair<PackedInts.Reader, PackedInts.Reader>(
        reduce(levels), reduce(pLevels));
  }

  private PackedInts.Reader reduce(GrowingMutable grower) {
    PackedInts.Mutable reduced = PackedInts.getMutable(
        grower.size(), grower.getBitsPerValue());
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
      return "HierarchicalTermProvider(source=" + source + ")";
  }

  /* Plain delegations */

  public int getNearestTermIndirect(BytesRef key) throws IOException {
    return source.getNearestTermIndirect(key);
  }

  public int getNearestTermIndirect(
      BytesRef key, int startTermPos, int endTermPos) throws IOException {
    return source.getNearestTermIndirect(key, startTermPos, endTermPos);
  }

  public NamedComparator getComparator() {
    return source.getComparator();
  }

  public String getDesignation() {
    return source.getDesignation();
  }

  public String getField(long ordinal) throws IOException {
    return source.getField(ordinal);
  }

  public BytesRef getTerm(long ordinal) throws IOException {
    return source.getTerm(ordinal);
  }

  public String getOrderedField(long indirect) throws IOException {
    return source.getOrderedField(indirect);
  }

  public BytesRef getOrderedTerm(long indirect) throws IOException {
    return source.getOrderedTerm(indirect);
  }

  public long getUniqueTermCount() throws IOException {
    return source.getUniqueTermCount();
  }

  public long getOrdinalTermCount() throws IOException {
    return source.getOrdinalTermCount();
  }

  public long getMaxDoc() {
    return source.getMaxDoc();
  }

  public IndexReader getReader() {
    return source.getReader();
  }

  public int getReaderHash() {
    return source.getReaderHash();
  }

  public int getRecursiveHash() {
    return source.getRecursiveHash();
  }

  public PackedInts.Reader getOrderedOrdinals() throws IOException {
    return source.getOrderedOrdinals();
  }

  public PackedInts.Reader getDocToSingleIndirect() throws IOException {
    return source.getDocToSingleIndirect();
  }

  public Iterator<ExposedTuple> getIterator(
      boolean collectDocIDs) throws IOException {
    return source.getIterator(collectDocIDs);
  }

  public DocsEnum getDocsEnum(long ordinal, DocsEnum reuse) throws IOException {
    return source.getDocsEnum(ordinal, reuse);
  }

  public void transitiveReleaseCaches(int level, boolean keepRoot) {
    source.transitiveReleaseCaches(level, keepRoot);
  }

  public int getDocIDBase() {
    return source.getDocIDBase();
  }

  public void setDocIDBase(int base) {
    source.setDocIDBase(base);
  }
}
