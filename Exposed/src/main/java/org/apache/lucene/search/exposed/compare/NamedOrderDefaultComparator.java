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

/**
 * Hack: This class is a placeholder for popularity ordering. When building the structures, the BytesRef.compare-order
 * is used and when requesting, the tag count should be used (handled outside of the comparator).
 */
public class NamedOrderDefaultComparator implements NamedComparator {
  private boolean isReverse = DEFAULT_REVERSE;
  private boolean isNullFirst = DEFAULT_NULL_FIRST;

  @Override
  public String getID() {
    return "Ordered_placeholder";
  }

  @Override
  public ORDER getOrder() {
    return ORDER.count;
  }

  @Override
  public boolean isReverse() {
    return isReverse;
  }

  @Override
  public void setReverse(boolean reverse) {
    isReverse = reverse;
  }

  @Override
  public boolean isNullFirst() {
    return isNullFirst;
  }

  @Override
  public void setNullFirst(boolean nullFirst) {
    isNullFirst = nullFirst;
  }

  @SuppressWarnings("ComparatorMethodParameterNotUsed")
  @Override
  public int compare(BytesRef o1, BytesRef o2) {
      return o1.compareTo(o2);
/*    throw new UnsupportedOperationException(
      "The " + getClass().getSimpleName()
      + " comparator cannot be used directly");*/
  }
}
