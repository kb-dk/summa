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
 * Simple NamedComparator that uses the natural order of the BytesRefs.
 */
public class NamedNaturalComparator implements NamedComparator {
  private boolean isReverse = DEFAULT_REVERSE;
  private boolean isNullFirst = DEFAULT_NULL_FIRST;
  private int orderFactor = isReverse ? -1 : 1;

  @Override
  public String getID() {
    return "Natural";
  }

  @Override
  public ORDER getOrder() {
    return ORDER.index;
  }

  @Override
  public int compare(BytesRef o1, BytesRef o2) {
    if (o1 == null && o2 == null) {
      return 0;
    }
    if (o1 == null) {
      return isNullFirst ? -1 : 1;
    }
    if (o2 == null) {
      return isNullFirst ? 1 : -1;
    }
    return orderFactor * o1.compareTo(o2);
  }

  @Override
  public boolean isReverse() {
    return isReverse;
  }

  @Override
  public void setReverse(boolean reverse) {
    isReverse = reverse;
    orderFactor = isReverse ? -1 : 1;
  }

  @Override
  public boolean isNullFirst() {
    return isNullFirst;
  }

  @Override
  public void setNullFirst(boolean nullFirst) {
    isNullFirst = nullFirst;
  }
}
