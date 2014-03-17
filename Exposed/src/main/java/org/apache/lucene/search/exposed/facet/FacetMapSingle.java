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
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Provides a single-field-oriented map from docID to tagID (1 value / document). The mapping is optimized
 * towards low memory footprint and extraction of all tags for each given  document ID.
 * </p><p>
 * Technical notes: This is a much simpler structure than {@link org.apache.lucene.search.exposed.facet.FacetMapMulti},
 * both in term of memory and speed. It requires the provider to have fields with single values, but accepts
 * missing values.
 * </p><p>
 * The distribution of the entries in refs is defined by the index layout and is assumed to be random.
 */
public class FacetMapSingle implements FacetMap {
  // To make this multi-provider and memory-efficient we need to alter the calling code logic significantly
  private final TermProvider provider;
  private final PackedInts.Reader refs;

  private final int[] indirectStarts = new int[]{1, 1};

  /**
   * Important: index 0 in refs indicates no value.
   * @param provider       term provider for the map.
   * @param refs           references from index-wide indirects to term provider entry.
   * @param tagCount       the number of tags in the refs, including the special 0-tag
   */
  public FacetMapSingle(TermProvider provider, PackedInts.Reader refs, int tagCount) {
    this.provider = provider;
    this.refs = refs;
    indirectStarts[1] = tagCount;
  }

  @Override
  public int getTagCount() {
    try {
      return (int)provider.getOrdinalTermCount()+1;
      //return (int)provider.getUniqueTermCount()+1;
    } catch (IOException e) {
      throw new RuntimeException("Unable to determine unique count", e);
    }
  }

  @Override
  public void updateCounter(final int[] tagCounts, final int docID) {
    try {
      final int index = (int)refs.get(docID);
      if (index == 0) { // TODO: Skip this check by incrementing counter 0 while disregarding it later
        return;
      }
      tagCounts[index]++;
    } catch (Exception ex) {
      System.err.println("Exception in updateCounter during evaluation of tagCounts[(int)refs.get(" + docID
                         + ")]++ with refs.size()==" + refs.size() + ", tagCounts.length()==" + tagCounts.length
                         + ", docID==" + docID + " in " + toString());
    }
  }

  @Override
  public void updateCounter(final TagCollector collector, final int docID) {
    try {
      final int index = (int)refs.get(docID);
      if (index == 0) { // TODO: Skip this check by incrementing counter 0 while disregarding it later
        return;
      }
      collector.inc(index);
    } catch (Exception ex) {
      System.err.println("Exception in updateCounter during evaluation of tagCounts[(int)refs.get(" + docID
                         + ")]++ with refs.size()==" + refs.size() + ", docID==" + docID + " in " + toString());
    }
  }

  @Override
  public BytesRef getOrderedTerm(int termIndirect) throws IOException {
    if (termIndirect == 0) {
      return new BytesRef(BytesRef.EMPTY_BYTES);
    }
    try {
      return provider.getOrderedTerm(termIndirect - 1);
    } catch (Exception e) {
      throw new ArrayIndexOutOfBoundsException(
          "The indirect " + termIndirect + " was too high. The maximum indirect supported by the current map is "
          + provider.getUniqueTermCount());
    }
  }

  @Override
  public BytesRef getOrderedDisplayTerm(int termIndirect) throws IOException {
    if (termIndirect == 0) {
      return new BytesRef(BytesRef.EMPTY_BYTES);
    }
    try {
      return provider.getOrderedDisplayTerm(termIndirect-1);
    } catch (Exception e) {
      throw new ArrayIndexOutOfBoundsException(
          "The indirect " + termIndirect + " was too high. The maximum indirect supported by the current map is "
          + provider.getUniqueTermCount());
    }
  }

  @Override
  public BytesRef[] getTermsForDocID(int docID) throws IOException {
    final int indirect = (int)refs.get(docID);
    return indirect == 0 ? new BytesRef[0] : new BytesRef[] { getOrderedTerm(docID) };
  }

  @Override
  public int[] getIndirectStarts() {
    return indirectStarts;
  }

  @Override
  public List<TermProvider> getProviders() {
    return Arrays.asList(provider);
  }

  public String toString() {
    StringWriter sw = new StringWriter();
    sw.append("FacetMapSingle(#docs=").append(Integer.toString(refs.size()-1));
    sw.append(" (").append(packedSize(refs)).append(")");
    sw.append(", provider(").append(provider.toString()).append("))");
    return sw.toString();
  }

  @Override
  public String tinyDesignation() {
    return "FacetMapSingle(refs=" + packedSize(refs) + ", provider=" + provider.getMemUsage()/1024 + "KB)";
  }

  private String packedSize(PackedInts.Reader packed) {
    long bytes = packed.ramBytesUsed();
    if (bytes > 1048576) {
      return bytes / 1048576 + " MB";
    }
    if (bytes > 1024) {
      return bytes / 1024 + " KB";
    }
    return bytes + " bytes";
  }
}
