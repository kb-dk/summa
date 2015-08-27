/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.lucene.search.exposed.compare;

import org.apache.lucene.util.BytesRef;

import java.util.Comparator;

/**
 * BytesRef comparator with a logical ID.
 * {@link #isNullFirst()} takes precedence over {@link #isReverse()}.
 */
public interface NamedComparator extends Comparator<BytesRef> {
  public static final boolean DEFAULT_NULL_FIRST = false;
  public static final boolean DEFAULT_REVERSE = false;

  /**
   * The basic ways of sorting terms.
   */
  enum ORDER {index, locale, count, custom;
    public static ORDER fromString(String order) {
      if (count.toString().equals(order)) {
        return count;
      }
      if (index.toString().equals(order)) {
        return index;
      }
      if (locale.toString().equals(order)) {
        return locale;
      }
      if (custom.toString().equals(order)) {
        return custom;
      }
      throw new IllegalArgumentException(
        "The order was '" + order + "' where only " + count+ ", " + index
        + ", " + locale + " and " + custom + " is allowed");
    }
  }

  /**
   * The ID is a unique identifier for the given Comparator.
   * Separate Comparators with equal behaviour should be avoided.
   * @return the ID of this comparator. Never null or the empty String.
   */
  public String getID();


  public ORDER getOrder();

  /**
   * @return true if null-BytesRefs are sorted first.
   */
  public boolean isNullFirst();

  /**
   * @param nullFirst true if null-BytesRefs should be sorted first.
   */
  public void setNullFirst(boolean nullFirst);

  /**
   * @return true if the order is logically reversed.
   */
  public boolean isReverse();

  /**
   * @param reverse whether or not the logical order should be reversed.
   */
  public void setReverse(boolean reverse);

}
