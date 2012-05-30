/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.apache.lucene.search.exposed;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import com.ibm.icu.text.Collator;
import java.util.*;

/**
 * Custom sorter that uses the Exposed framework. Trade-offs are slow first-time
 * use, fast subsequent use and low memory footprint.
 */
// TODO: Add adjustable sortNullFirst
public class ExposedFieldComparatorSource extends FieldComparatorSource {
  private final IndexReader reader;
  private final Comparator<BytesRef> comparator;
  private final String comparatorID;
  private final boolean sortNullFirst;

  // TODO: Figure out how to avoid the need for re-creation upon re-open

  /**
   * Creates a field comparator source with the given comparator. It is
   * recommended to use this constructor instead of constructing with a locale
   * an to provide a fast comparator, such as the ICU Collator.
   * </p><p>
   * The class {@link ExposedComparators} provides easy wrapping of Collators
   * and other comparators.
   * @param reader        the reader to sort for.
   * @param comparator    a custom comparator.
   * @param comparatorID  an id for the comparator.
   * @param sortNullFirst if true, documents without content in the sort field
   *                      are sorted first.
   */
  public ExposedFieldComparatorSource(
      IndexReader reader, Comparator<BytesRef> comparator, String comparatorID,
      boolean sortNullFirst) {
    this.reader = reader;
    this.comparatorID = comparatorID;
    this.comparator = comparator;
    this.sortNullFirst = sortNullFirst;
  }

  /**
   * Creates a field comparator source with a Collator generated from the given
   * locale using {@link ExposedComparators}. By default this means Java's
   * build-in Collator, which is rather slow.
   * @param reader the reader to sort for.
   * @param locale the locale to use for constructing the Collator.
   */
  public ExposedFieldComparatorSource(IndexReader reader, Locale locale) {
    this(reader,
        ExposedComparators.localeToBytesRef(locale),
        locale == null ? ExposedRequest.LUCENE_ORDER : locale.toString(),
        false);
  }

  /**
   * @param fieldname the field to sort on. If a group of fields is wanted,
   *        field names must be separated by {@code ;}.
   *        Example: {@code foo;bar;zoo}. Note that grouping treats the terms
   *        from the separate fields at the same level and that there should
   *        only be a single term i total for the given fields for a single
   *        document. Grouping is not normally used at this level.
   * @param numHits  the number of hits to return.
   * @param sortPos  ?
   * @param reversed reversed sort order.
   * @return a comparator based on the given parameters.
   * @throws IOException if the 
   */
  @Override
  public FieldComparator newComparator(
      String fieldname, int numHits, int sortPos, boolean reversed)
                                                            throws IOException {
    List<String> fieldNames = Arrays.asList(fieldname.split(":"));
    return new ExposedFieldComparator(
        reader, fieldname, fieldNames, numHits, sortPos, reversed);
  }

  private class ExposedFieldComparator extends FieldComparator {
    private final TermProvider provider;

    private final int undefinedTerm = -1;
    private final PackedInts.Reader docOrder;

    private final String groupName;
    private final List<String> fieldNames;
    private final int numHits;
    private final int sortPos;
    private final boolean reversed;
    private final int factor; // Reverse-indicator
    private int docBase = 0;  // Added to all incoming docIDs

    private int[] order; // docOrder
    private int bottom;  // docOrder

    public ExposedFieldComparator(
        IndexReader reader, String groupName, List<String> fieldNames,
        int numHits, int sortPos, boolean reversed) throws IOException {
      this.groupName = groupName;
      this.fieldNames = fieldNames;
      this.provider = ExposedCache.getInstance().getProvider(
          reader, groupName, fieldNames, comparator, comparatorID);

      this.numHits = numHits;
      this.sortPos = sortPos;
      // TODO: Whys is it wrong to reverse here when it is a parameter?
      this.reversed = false; //reversed; // Reverse is handled at this level
      factor = 1; //reversed ? -1 : 1;
      docOrder = provider.getDocToSingleIndirect();

      order = new int[numHits];
    }

    @Override
    public int compare(int slot1, int slot2) {
      if (!sortNullFirst) {
        int slot1order = order[slot1];
        int slot2order = order[slot2];
        if (slot1order == undefinedTerm) {
          return slot2order == undefinedTerm ? 0 : -1;
        } else if (slot2order == undefinedTerm) {
          return 1; // TODO: Verify factor (reverse) behaviour
        }
        return factor * (slot1order - slot2order);
        //return slot1order - slot2order;
      }
      // No check for null as null-values are always assigned -1
      return factor * (order[slot1] - order[slot2]);
 //     return order[slot1] - order[slot2];
    }

    @Override
    public void setBottom(int slot) {
      bottom = order[slot];
    }

    @Override
    public int compareBottom(final int doc) throws IOException {
      if (!sortNullFirst) {
        try {
          final long bottomOrder = bottom;
          final long docOrderR = docOrder.get(doc+docBase);
          if (bottomOrder == undefinedTerm) {
            return docOrderR == undefinedTerm ? 0 : -1;
          } else if (docOrderR == undefinedTerm) {
            return 1;
          }
          return (int)(factor * (bottomOrder - docOrderR));
          //return (int)(bottomOrder - docOrderR);
        } catch (ArrayIndexOutOfBoundsException e) {
          throw new ArrayIndexOutOfBoundsException(
              "Exception requesting at index " + (doc+docBase)
                    + " with docOrder.size==" + docOrder.size() + ", doc==" + doc
                  + ", docBase==" + docBase + ", reader maxDoc==" + maxDoc);
        }
      }
      // No check for null as null-values are always assigned -1
      return (int)(factor * (bottom - docOrder.get(doc+docBase)));
//      return (int)(bottom - docOrder.get(doc+docBase));
    }

    @Override
    public void copy(int slot, int doc) throws IOException {
      // TODO: Remove this
//      System.out.println("Copy called: order[" + slot + "] = "
//          + doc + "+" + docBase + " = " + (doc + docBase));
//      System.out.println("docID " + (doc+docBase) + " has term " + provider.getOrderedTerm(doc+docBase).utf8ToString());
      order[slot] = (int)docOrder.get(doc+docBase);
    }


    long maxDoc;
    @Override
    public FieldComparator setNextReader(
      AtomicReaderContext context) throws IOException {
      this.docBase = context.docBase;
      maxDoc = context.reader().maxDoc();
      return this;
    }

    private final BytesRef EMPTY = new BytesRef("");
    @Override
    public Comparable<?> value(int slot) {
      try { // A bit cryptic as we need to handle the case of no sort term
        final long resolvedDocOrder = order[slot];
        // TODO: Remove this
/*        System.out.println("Resolving docID " + slot + " with docOrder entry "
            + resolvedDocOrder + " to term "
            + (resolvedDocOrder == undefinedTerm ? "null" :reader.getTermText(
            (int)termOrder.get((int)resolvedDocOrder))));
  */
        return resolvedDocOrder == undefinedTerm ? EMPTY :
            provider.getOrderedTerm(resolvedDocOrder);
      } catch (IOException e) {
        throw new RuntimeException(
            "IOException while extracting term String", e);
      }
    }

    // TODO: Implement compareDoctoValue
    @Override
    public int compareDocToValue(int i, Object o) throws IOException {
      throw new UnsupportedOperationException("Not supported yet");
    }
  }


}
