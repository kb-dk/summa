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
package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public interface FacetMap {

  /**
   * @return the number of unique tags in the map.
   */
  int getTagCount();

  /**
   * @param tagCounts a counter of size {@link #getTagCount()};
   * @param docID an docID relative to the full index (aka absolute docID).
   */
  void updateCounter(int[] tagCounts, int docID);

  /**
   * Note: If possible, use the {@link #updateCounter(int[], int)} instead as it is faster.
   * @param collector a collector matching this map.
   * @param docID an docID relative to the full index (aka absolute docID).
   */
  void updateCounter(TagCollector collector, int docID);

  /**
   * @param termIndirect an indirect pointer.
   * @return the corresponding term.
   * @throws IOException if the term could not be located.
   */
  BytesRef getOrderedTerm(int termIndirect) throws IOException;

/**
 * @param termIndirect an indirect pointer.
 * @return the corresponding term, usable for display to end users.
 * @throws IOException if the term could not be located.
 */
  BytesRef getOrderedDisplayTerm(int termIndirect) throws IOException;

  /**
   * @param docID an absolute docID, relative to the full index.
   * @return the terms mapped from the docID.
   * @throws IOException if the terms could not be located.
   */
  BytesRef[] getTermsForDocID(int docID) throws IOException;

  /**
   * @return the starting positions for the individual facets.
   */
  int[] getIndirectStarts();

  /**
   * @return the term providers for this map.
   */
  List<TermProvider> getProviders();

  /**
   * @return short and simple toString, intended for inclusion in facet result.
   */
  String tinyDesignation();
}
