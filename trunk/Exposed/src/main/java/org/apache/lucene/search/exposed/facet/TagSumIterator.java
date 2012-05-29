package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.facet.request.SubtagsConstraints;

import java.io.IOException;

/**
 * Iterates tags at a given level and sums counts from sub-tags that satisfies
 * the constraints from the constraints.
 * </p><p>
 * next() must be called at least once before any of the getters will return
 * meaningful information.
 */
public class TagSumIterator {
  final Pool<TagSumIterator> pool;

  final HierarchicalTermProvider provider;
  final int delta;
  final int[] tagCounts;

  SubtagsConstraints constraints;
  int rangeStartPos = 0; // Inclusive
  int rangeEndPos = 0;   // Exclusive
  int level = 0;

  int count = 0;         // At the current level
  int totalCount = 0;    // At all levels
  int tagStartPos = 0;   // Inclusive
  int tagPos = 0;
  int tagEndPos = 0;     // Exclusive
  boolean tagAvailable = false;

  // Ready to use after this
  public TagSumIterator(final HierarchicalTermProvider provider,
                        final SubtagsConstraints constraints,
                        final int[] tagCounts,
                        final int rangeStartPos, final int rangeEndPos,
                        final int level,
                        final int delta) {
    this.provider = provider;
    this.tagCounts = tagCounts;
    this.delta = delta;
    pool = new Pool<TagSumIterator>() {
      @Override
      public TagSumIterator createElement() {
        return new TagSumIterator(this, provider, tagCounts, delta);
      }
    };
    reset(constraints, rangeStartPos, rangeEndPos, level);
  }

  // Remember to call reset after this one
  private TagSumIterator(final Pool<TagSumIterator> pool,
                         final HierarchicalTermProvider provider,
                         final int[] tagCounts, final int delta) {
    this.pool = pool;
    this.provider = provider;
    this.tagCounts = tagCounts;
    this.delta = delta;
  }

  public void reset(SubtagsConstraints constraints,
                    int rangeStartPos, int rangeEndPos, int level) {
    this.constraints = constraints;
    this.rangeStartPos = rangeStartPos;
    this.rangeEndPos = rangeEndPos;
    this.level = level;

    count = 0;
    totalCount = 0;
    tagStartPos = -1;
    tagPos = rangeStartPos;
    tagEndPos = -1;
    tagAvailable = false;
  }

  /*

L P T      C
1 0 A     (1)   (no previous tag)
3 1 A/B/C (2)   (The previous tag "A" matches only the first level of "A/B/C")
3 2 A/B/J (1)   (The previous tag "A/B/C" matches first and second level of "A/B/J")
2 0 D/E   (1)   (no previous match)
3 2 D/E/F (1)   (The previous tag "D/E" matches both the first and second level of "D/E/F")
3 0 G/H/I (1)   (no previous tag)

   */

  /**
   * Skips to the next tag which satisfies the constraints constraints.
   * Behaviour is undefined if next() is called after it has returned false.
   * @return true if a valid tag is found.
   */
  public boolean next() {
    tagAvailable = false;
    tagEndPos = rangeEndPos;
    final int minCount = constraints.getMinCount();
    while (!tagAvailable && tagPos < rangeEndPos) {
//      System.out.println(dumpTag(tagStartPos));
      tagEndPos = rangeEndPos;

      while (tagStartPos == -1 &&
          provider.getLevel(tagPos) < level && tagPos < rangeEndPos) {
        tagPos++;
      }

      // Check for termination
      if ((tagPos > rangeStartPos || provider.getLevel(tagPos) < level) &&
          !provider.matchLevel(tagPos + delta, level)) {
        tagEndPos = tagPos;
        return false;
      }

      tagStartPos = tagPos; // The start of the potential tag
      count = tagCounts[tagStartPos];
      totalCount = count;
      tagPos++; // Advance to next

      // If children, sum them to get totalCount for the tag and advance tagPos
      while (tagPos < rangeEndPos &&
          provider.getLevel(tagPos) > level &&
          provider.getPreviousMatchingLevel(tagPos) >= level) {
//        System.out.println("Subsumming for " + tagPos);
        TagSumIterator subIterator = pool.acquire();
        subIterator.reset(
            constraints.getDeeperLevel(), tagPos, rangeEndPos, level+1);
        int subTotalCount = subIterator.countValid();
        tagPos = subIterator.tagEndPos; // Skip processed
        totalCount += subTotalCount;
      }

      // If totalcount is okay, stop iterating, else start over
      if (tagCounts[tagStartPos] < minCount ||
          totalCount < constraints.getMinTotalCount()) {
        continue;
      }
      tagEndPos = tagPos;
      tagAvailable = true;
    }
    return tagAvailable;
  }

  public String getTagInfo() {
    try {
      return "indirect=" + tagStartPos
          + ", level=" + provider.getLevel(tagStartPos)
          + ", pLevel=" + provider.getPreviousMatchingLevel(tagStartPos)
          + ", tag='" + provider.getOrderedTerm(tagStartPos).utf8ToString() 
          + "'";
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Could not locate the tag for indirect " + tagStartPos, e);
    }
  }

  private int countValid() {
    int total = 0;
    while (next()) {
      total += getTotalCount();
    }
    return total;
  }

  /* Getters */

  public int getCount() {
    return count;
  }
  public int getTotalCount() {
    return totalCount;
  }
}