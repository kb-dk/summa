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

import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import org.apache.lucene.util.BytesRef;

import java.util.Locale;

/**
 * Collator-backed NamedComparator with CollatorKey-generation.
 */
public class NamedCollatorComparator implements NamedComparator {
  private boolean isReverse = DEFAULT_REVERSE;
  private boolean isNullFirst = DEFAULT_NULL_FIRST;
  private int orderFactor = isReverse ? -1 : 1;
  private final Collator collator;
  private final String id;

  public NamedCollatorComparator(Collator collator) {
    this.collator = collator;
    id = "Collator(" + collator.toString() + ")";
  }

  public NamedCollatorComparator(Collator collator, String id) {
    this.collator = collator;
    this.id = id;
  }

  /**
   * Creates a Collator that ignores punctuation and whitespace, mimicking the
   * Sun/Oracle default Collator.
   * @param locale the locale for the Collator.
   */
  public NamedCollatorComparator(Locale locale) {
    collator = Collator.getInstance(locale);
    if (collator instanceof RuleBasedCollator) {
      // Treat spaces as normal characters
      ((RuleBasedCollator)collator).setAlternateHandlingShifted(false);
    }
    id = "Collator(Locale(" + locale.toString() + "))";
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
    return orderFactor * collator.compare(o1.utf8ToString(), o2.utf8ToString());
  }

  public Collator getCollator() {
    return collator;
  }

  /**
   * Note: The CollationKey does not handle isNullFirst and isReverse.
   * @param source must not be null.
   */
  public CollationKey getCollationKey(String source) {
    return collator.getCollationKey(source);
  }

  /**
   * Note: The CollationKey does not handle isNullFirst and isReverse.
   * @param source must not be null.
   */
  public CollationKey getCollationKey(BytesRef source) {
    return collator.getCollationKey(source.utf8ToString());
  }

  @Override
  public String getID() {
    return id;
  }

  @Override
  public ORDER getOrder() {
    return ORDER.locale;
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
