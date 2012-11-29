package org.apache.lucene.search.exposed;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

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
 * Keeps track of a group containing 1 or more providers ensuring term
 * uniqueness and ordering. The providers are normally either
 * {@link FieldTermProvider} or {@link GroupTermProvider}.
 * </p><p>
 * The standard use case calls for caching the GroupTermProvider at segment
 * level as group setups rarely changes.
 */
// TODO: Extend to handle term ordinals > Integer.MAX_VALUE
public class GroupTermProvider extends TermProviderImpl {
  private final List<TermProvider> providers;
  private final ExposedRequest.Group request;
  private final int readerHash;

  private PackedInts.Reader order;

  // Starting points for term ordinals in the providers list
  private final long[] termOrdinalStarts;

  // FIXME: this should be relative to segments, not providers.
  //Current implementation is not valid for multiple fields in the same segment
  private final long[] docIDStarts;

  public GroupTermProvider(int readerHash,
      List<TermProvider> providers, ExposedRequest.Group request,
                                        boolean cacheTables) throws IOException {
    super(null, 0, request.getComparator(), "Group " + request.getName(),
          cacheTables);
    this.readerHash = readerHash;
    this.providers = checkProviders(providers);
    this.request = request;

    long[][] starts = calculateStarts();
    docIDStarts = starts[0];
    termOrdinalStarts = starts[1];
  }

  private List<TermProvider> checkProviders(List<TermProvider> providers) {
    for (TermProvider provider: providers) {
      try {
        provider.getDocIDBase();
      } catch (UnsupportedOperationException e) {
        throw new IllegalArgumentException(
            "The GrouptermProvider currently only supports providers that are "
                + "capable of stating their docIDBase. Received " + provider);
      }
    }
    return providers;
  }

  /**
   * The starts are straight forward for term ordinals as they are just mapped
   * as one long list of ordinals in the source order. We can do there is no
   * authoritative index-level ordinal mapping.
   * </p><p>
   * Document IDs are a bit trickier as they are used at the index-level.
   * We need to handle the case where more than one field from the same segment
   * make up a group. In that case the docIDs should have the same start.
   * </p><p>
   * Important: In order for the calculator to work, the sources must be
   * monotonic increasing
   * @throws IOException if the underlying providers failed.
   * @return the calculated starts. doc, term.
   */
  private long[][] calculateStarts() throws IOException {
        long sanityCheck = 0;
    long[] termOrdinalStarts = new long[providers.size() + 1];
    long[] docIDStarts = new long[providers.size() + 1];

    for (int i = 1 ; i <= providers.size() ; i++) {
      termOrdinalStarts[i] =
          termOrdinalStarts[i-1] + providers.get(i-1).getOrdinalTermCount();
      sanityCheck += providers.get(i-1).getOrdinalTermCount();
    }
    if (sanityCheck > Integer.MAX_VALUE-1) {
      throw new InternalError("There are " + sanityCheck + " terms in total in "
          + "the underlying TermProviders which is more than the current limit "
          + "of Integer.MAX_VALUE");
    }

    long docStart = 0;
    for (int i = 0 ; i < providers.size() ; i++) {
      TermProvider provider = providers.get(i);
      docIDStarts[i] =  provider.getDocIDBase();
      docStart = provider.getDocIDBase() + provider.getMaxDoc();
    }
    docIDStarts[docIDStarts.length-1] = docStart;

/*    long lastHash = -1;
    long docStart = 0;
    for (int i = 0 ; i < providers.size() ; i++) {
      docIDStarts[i] = docStart;
      TermProvider provider = providers.get(i);
      docStart += provider.getRecursiveHash() == lastHash ? 0 :
          provider.getMaxDoc();
      lastHash = provider.getRecursiveHash();
    }
    docIDStarts[docIDStarts.length-1] = docStart;*/
    long[][] result = new long[2][];
    result[0] = docIDStarts;
    result[1] = termOrdinalStarts;
    return result;
  }

  public ExposedRequest.Group getRequest() {
    return request;
  }

  public String getField(long ordinal) throws IOException {
    int providerIndex = getProviderIndex(ordinal);
    return providers.get(providerIndex).
        getField(adjustOrdinal(ordinal, providerIndex));
  }

  private int getProviderIndex(long ordinal) throws IOException {
    for (int i = 1 ; i < termOrdinalStarts.length ; i++) {
      if (ordinal < termOrdinalStarts[i]) {
        return i-1;
      }
    }
    throw new IllegalArgumentException("The term ordinal " + ordinal
        + " is above the maximum " + (getOrdinalTermCount() - 1));
  }

  private long adjustOrdinal(long ordinal, int providerIndex) {
    return providerIndex == 0 ? ordinal :
        ordinal - termOrdinalStarts[providerIndex];
  }

  public synchronized BytesRef getTerm(long ordinal) throws IOException {
    int providerIndex = getProviderIndex(ordinal);
    return providers.get(providerIndex).
        getTerm(adjustOrdinal(ordinal, providerIndex));
  }

  public DocsEnum getDocsEnum(long ordinal, DocsEnum reuse) throws IOException {
    int providerIndex = getProviderIndex(ordinal);
    return providers.get(providerIndex).
        getDocsEnum(adjustOrdinal(ordinal, providerIndex), reuse);
  }

  public String getOrderedField(long indirect) throws IOException {
    return getField(getOrderedOrdinals().get((int)indirect));
  }

  public BytesRef getOrderedTerm(final long indirect) throws IOException {
    return indirect == -1 ? null :
        getTerm(getOrderedOrdinals().get((int)indirect));
  }

  /**
   * Note that this method calculates ordered ordinals to determine the unique
   * terms.
   * @return The number of unique terms.
   * @throws IOException if the underlyind providers failed.
   */
  public long getUniqueTermCount() throws IOException {
    return getOrderedOrdinals().size();
  }

  public long getOrdinalTermCount() throws IOException {
    return termOrdinalStarts[termOrdinalStarts.length-1];
  }

  public long getMaxDoc() {
    return docIDStarts[docIDStarts.length-1];
  }

  @Override
  public IndexReader getReader() {
    throw new UnsupportedOperationException(
        "Cannot request a reader from a collection of readers");
  }

  public int getReaderHash() {
    return readerHash;
  }

  public int getRecursiveHash() {
    int hash = 0;
    for (TermProvider provider: providers) {
      hash += provider.hashCode();
    }
    return hash;
  }

  public PackedInts.Reader getOrderedOrdinals() throws IOException {
    return getOrderedOrdinals(null);
  }

    @Override
  public synchronized PackedInts.Reader getOrderedOrdinals(
        OrderedDecorator decorator) throws IOException {
      if (order != null) {
        if (decorator != null) {
          for (int indirect = 0 ; indirect < order.size() ; indirect++) {
            decorator.decorate(getOrderedTerm(indirect), indirect);
          }
        }
        return order;
      }
      PackedInts.Reader newOrder = sortOrdinals(decorator);
      if (cacheTables) {
        order = newOrder;
      }
      return newOrder;
    }

    private PackedInts.Reader sortOrdinals(final OrderedDecorator decorator)
            throws IOException {
//    System.out.println("FacetGroup sorting ordinals from " + providers.size()
//        + " providers");
    int maxTermCount = (int)termOrdinalStarts[termOrdinalStarts.length-1];
    long iteratorConstruction = System.currentTimeMillis();
    PackedInts.Mutable collector = ExposedSettings.getMutable(
        maxTermCount, maxTermCount);
        //new GrowingMutable(0, maxTermCount, 0, maxTermCount);
    long iteratorTime = -System.currentTimeMillis();
    Iterator<ExposedTuple> iterator = getIterator(false);
    iteratorTime += System.currentTimeMillis();
//    System.out.println("Group " + getDesignation() + " iterator constructed in "
//        + iteratorTime + "ms");

    iteratorConstruction = System.currentTimeMillis() - iteratorConstruction;
    if (ExposedSettings.debug) {
      System.out.println("Group total iterator construction: "
          + ExposedUtil.time("ordinals", maxTermCount, iteratorConstruction));
    }

    int uniqueTermCount = 0;
    long extractionTime = -System.currentTimeMillis();
    long lastIndirect = -1;
    while (iterator.hasNext()) {
      ExposedTuple tuple = iterator.next();
      if (decorator != null && tuple.indirect != lastIndirect) {
        decorator.decorate(tuple.term, tuple.indirect);
      }
//      System.out.println("sortOrdinals " + tuple + " term = " + tuple.term.utf8ToString() + " lookup term " + getTerm(tuple.ordinal).utf8ToString());
      collector.set((int)tuple.indirect, tuple.ordinal);
      uniqueTermCount++;
//      collector.set(uniqueTermCount++, tuple.indirect);
    }
//    System.out.println("Sorted merged term ordinals to " + collector);

    long reducetime = -System.currentTimeMillis();
    PackedInts.Mutable result = ExposedSettings.getMutable(
        uniqueTermCount, maxTermCount);
    for (int i = 0 ; i < uniqueTermCount ; i++) {
      result.set(i, collector.get(i));
    }
    reducetime += System.currentTimeMillis();

    extractionTime += System.currentTimeMillis();
    if (ExposedSettings.debug) {
      System.out.println("Group ordinal iterator depletion from "
          + providers.size() + " providers: "
          + ExposedUtil.time("ordinals", result.size(), extractionTime)
          + " (Memory optimize time: " + reducetime + " ms)");
    }
    return result;
  }


  @Override
  public Iterator<ExposedTuple> getIterator(boolean collectDocIDs)
                                                            throws IOException {
    return new MergingTermDocIterator(
        this, providers, request.getComparator(), collectDocIDs);
  }

  public long segmentToIndexDocID(int providerIndex, long segmentDocID) {
    return docIDStarts[providerIndex] + segmentDocID;
  }

  public long segmentToIndexTermOrdinal(
      int providerIndex, long segmentTermOrdinal) {
    return termOrdinalStarts[providerIndex] + segmentTermOrdinal;
  }

  @Override
  public int getDocIDBase() {
    throw new UnsupportedOperationException(
        "No docIDBase can be inferred from GroupTermProvider");
  }

  public String toString() {
    if (order == null) {
      return "GroupTermProvider(" + request.getName() + ", #subProviders="
          + providers.size() + ", no order cached, " + super.toString() + ")";
    }
    return "GroupTermProvider(" + request.getName() + ", #subProviders="
        + providers.size() + ", order.length=" + order.size() + " mem="
        + packedSize(order) + ", " + super.toString() + ")";
  }

  @Override
  public void transitiveReleaseCaches(int level, boolean keepRoot) {
    if (!keepRoot || level > 0) {
      order = null;
    }
    level++;
    super.transitiveReleaseCaches(level, keepRoot);
    for (TermProvider provider: providers) {
      provider.transitiveReleaseCaches(level, keepRoot);
    }
  }
}
