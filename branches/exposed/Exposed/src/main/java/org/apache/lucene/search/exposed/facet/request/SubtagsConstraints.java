package org.apache.lucene.search.exposed.facet.request;

public interface SubtagsConstraints {
  int getMaxTags();
  int getMinCount();
  int getMinTotalCount();
  SUBTAGS_ORDER getSubtagsOrder();

  /**
   *
   * @return a SubtagsConstraints for the next level or the current definer if there
   *         are no defined sub level.
   */
  SubtagsConstraints getDeeperLevel();

  enum SUBTAGS_ORDER {count, base;
    public static SUBTAGS_ORDER fromString(String order) {
      if (count.toString().equals(order)) {
        return count;
      }
      if (base.toString().equals(order)) {
        return base;
      }
      throw new IllegalArgumentException("The order was '" + order
          + "' where only " + SUBTAGS_ORDER.count
          + " and " + SUBTAGS_ORDER.base + " is allowed");
    }
  }
}
