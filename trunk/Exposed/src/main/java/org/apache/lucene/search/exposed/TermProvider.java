package org.apache.lucene.search.exposed;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.Iterator;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Order-oriented variation on {@link org.apache.lucene.index.Terms}.
 * </p><p>
 * Access is primarily through ordinals. This can be done directly with
 * {@link #getTerm} and {@link #getField} or indirectly through
 * {@link #getOrderedTerm} and {@link #getOrderedField}. Using the ordered
 * methods is equivalent to {@code getTerm(getOrder().get(orderPosition))}.
 */
public interface TermProvider {

    /**
     * A decorating version of {@link #getOrderedOrdinals()}. For each ordinal
     * an {@link ExposedTuple} is passed to the decorator.
     * @param decorator called for each tuple.
     * @return ordinals ordered by Comparator..
     * @throws IOException if the ordinals could not be retrieved.
     */
    PackedInts.Reader getOrderedOrdinals(OrderedDecorator decorator) throws IOException;

    /**
     * A decorator for extra Term-oriented processing during ordinal generation by
     * {@link #getOrderedOrdinals(org.apache.lucene.search.exposed.TermProvider.OrderedDecorator)}.
     */
    public static interface OrderedDecorator {
      /**
       * Called once and once only for every term in order.
       * @param term     the current term. This should not be modified.
       * @param indirect the index in the order-array.
       */
      public void decorate(BytesRef term, long indirect);
    }

  /**
   * Performs a search for the given term, returning the indirect for the
   * term or the nearest indirect if not present.
   * </p><p>
   * Note: While a binary search is used, this is still somewhat expensive as
   * the terms themselves needs to be retrieved.
   * @param key          the term to search for.
   * @return the indirect for the best matching term.
   * @throws java.io.IOException if the terms could not be accessed.
   */
  public int getNearestTermIndirect(BytesRef key) throws IOException;

  /**
   * Performs a search for the given term, returning the indirect for the
   * term or the nearest indirect if not present.
   * </p><p>
   * Note: While a binary search is used, this is still somewhat expensive as
   * the terms themselves needs to be retrieved.
   * @param key          the term to search for.
   * @param startTermPos where to search from (inclusive).
   * @param endTermPos   where to search to (exclusive).
   * @return the indirect for the best matching term.
   * @throws java.io.IOException if the terms could not be accessed.
   */
  public int getNearestTermIndirect(
      BytesRef key, int startTermPos, int endTermPos) throws IOException;

  /**
   * @return a comparator used for sorting. If this is null, natural BytesRef
   * order is to be used.
   */
  NamedComparator getComparator();

  /**
   * @return a short name or description of this provider.
   */
  String getDesignation(); // Debugging and feedback

  String getField(long ordinal) throws IOException;

  BytesRef getTerm(long ordinal) throws IOException;

  String getOrderedField(long indirect) throws IOException;

  // Note: indirect might be -1 which should return null
  BytesRef getOrderedTerm(long indirect) throws IOException;

  long getUniqueTermCount() throws IOException;

  long getOrdinalTermCount() throws IOException;

  long getMaxDoc();
  
  IndexReader getReader();

  /**
   * @return the base to add to every returned docID in order to get the 
   * index-wide docID.
   */
  int getDocIDBase();
  
  void setDocIDBase(int base);

  int getReaderHash();

  /**
   * If the provider is a leaf, the hash-value is that of the IndexReader.
   * If the provider is a node, the value is the sum of all contained
   * term-providers hash-values.
   * </p><p>
   * This is useful for mapping segment-level docIDs to index-level then a
   * group consists of more than one segment.
   * @return a hash-value useful for determining reader-equivalence.
   */
  int getRecursiveHash();

  /**
   * The ordinals sorted by a previously provided Comparator (typically a
   * Collator).
   * @return ordinals ordered by Comparator..
   * @throws IOException if the ordinals could not be retrieved.
   */
  PackedInts.Reader getOrderedOrdinals() throws IOException;

  /**
   * Mapping from docID to the ordered ordinals returned by
   * {@link #getOrderedOrdinals()}. Usable for sorting by field value as
   * comparisons of documents can be done with {@code
   PackedInts.Reader doc2indirect = getDocToSingleIndirect();
   ...
   if (doc2indirect.get(docID1) < doc2indirect.get(docID2)) {
    System.out.println("Doc #" + docID1 + " comes before doc # " + docID2);
   }}. If the term for a document is needed it can be retrieved with
   * {@code getOrderedTerm(doc2indirect.get(docID))}.
   * </p><p>
   * Documents without a corresponding term has the indirect value {@code -1}.
   * @return a map from document IDs to the indirect values for the terms for
   *         the documents.
   * @throws IOException if the index could not be accessed,
   */
  // TODO: Handle missing value (-1? Max+1? Defined by "empty first"?)
  PackedInts.Reader  getDocToSingleIndirect() throws IOException;

  /**
   * @param collectDocIDs if true, the document IDs are returned as part of the
   *        tuples. Note that this normally increases the processing time
   *        significantly.
   * @return an iterator over tuples in the order defined for the TermProvider.
   * @throws IOException if the iterator could not be constructed.
   */
  Iterator<ExposedTuple> getIterator(boolean collectDocIDs) throws IOException;

  DocsEnum getDocsEnum(long ordinal, DocsEnum reuse) throws IOException;

  /**
   * Release all cached values if the provider satisfies the given constraints.
   * @param level    the level in a provider-tree. This starts at 0 and should
   *                 be incremented by 1 if the provider delegates cache release
   *                 to sub-providers.
   * @param keepRoot if true, caches at the root level (level == 0) should not
   *                 be released.
   */
  void transitiveReleaseCaches(int level, boolean keepRoot);
}
