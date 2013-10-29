package org.apache.lucene.search.exposed;

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

import org.apache.lucene.index.*;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wrapper for a TermsEnum that guarantees that the methods
 * {@link TermsEnum#ord()} and {@link TermsEnum#seekExact(long)} are
 * implemented.
 * </p><p>
 * Important: the {@link #ord} is only consistent when the last position
 * changing method used was {@link #seekExact(long)} or {@link #next()}.
 * </p><p>
 * The wrapper works by keeping a TermState for every X ordinal and thus allows
 * for a flexible space/speed trade off.
 */
// TODO: Measure memory impact as it seems horribly wasteful to keep term+state
// TODO: Make an option for only storing BytesRef
// TODO: Use ByteBlockPool or similar to hold BytesRefs (see BytesRefHash)
// TODO: Merge with Solr's NumberedTermsEnum
public class OrdinalTermsEnum extends TermsEnum {
  public final TermsEnum inner;
  public final int divider;
  public final List<TermState> marks;
  public final List<BytesRef> terms;
  public final long termCount;

  private boolean ordOK = true;
  private long ordinal = 0;

  /**
   * A hack that creates a TermsEnum and tries to access the ordinal for the
   * first term. If an exception is thrown, the TermsEnum is wrapped as an
   * OrdinalTermsEnum. If no exception is thrown, the TermsEnum is reset and
   * returned directly.
   * @param reader  the reader to request the TermsEnum from.
   * @param field   the field to request the TermsEnum for.
   * @param divider keep a TermState for every X terms if an OrdinalTermsEnum
   *                is created.
   * @return a plain TermsEnum if ordinal access is provided for the given field
   *         by the given reader, else an OrdinalTermsEnum. If no TermsEnum can
   *         be requested at all, null is returned.
   * @throws java.io.IOException if the index could not be accessed.
   */
  public static TermsEnum createEnum(
      AtomicReader reader, String field, int divider) throws IOException {
    Terms terms = reader.fields().terms(field);
    if (terms == null) {
      return null;
    }
    TermsEnum inner = terms.iterator(null);
    if (inner.next() == null) {
      return null; // No terms
    }
    try {
      inner.ord();
    } catch (UnsupportedOperationException e) {
      // No ordinal seeking, so we make our own
      return new OrdinalTermsEnum(inner, divider);
    }
    return inner;
  }

  /**
   * @param inner   the TermsEnum to use for all calls except ord() and
   *                seek(long). The TermsEnum must be positioned at the first
   *                term, which basically means that next() must have been
   *                called once.
   * @param divider keep a TermState for every X terms.
   * @throws java.io.IOException if it was not possible to extract TermStates.
   */
  public OrdinalTermsEnum(TermsEnum inner, int divider) throws IOException {
    this.inner = inner;
    this.divider = divider;
    marks = new ArrayList<TermState>();
    terms = new ArrayList<BytesRef>();
    long count = 0;
    while (true) {
      //System.out.println(count + ": " + inner.term().utf8ToString());
      if (count % divider == 0) {
        marks.add(inner.termState());
        terms.add(copy(inner.term()));
      }
      count++;
      if (inner.next() == null) {
        break;
      }
    }
    inner.seekExact(terms.get(0), marks.get(0));
    termCount = count;
  }
  private BytesRef copy(BytesRef bytesRef) {
      byte[] bytes = new byte[bytesRef.length];
      System.arraycopy(bytesRef.bytes, bytesRef.offset, bytes, 0, bytesRef.length);
      return new BytesRef(bytes);
  }

  /**
   * @return the number of unique terms in this enum.
   */
  public long getTermCount() {
    return termCount;
  }

  @Override
  public long ord() throws IOException {
    if (!ordOK) {
      throw new IllegalStateException(
          "ord() can only be called when the last position-changing call was to " +
              "seek(long) or next()");
    }
    return ordinal;
  }

  @Override
  public void seekExact(long ord) throws IOException {
    ordOK = true;
    if (ordinal > ord || ord > ordinal + divider) { // We're outside the block
      int pos = (int)(ord / divider);
      inner.seekExact(terms.get(pos), marks.get(pos));
//      System.out.println("*** " + br.utf8ToString() + " " + ord);
      ordinal = ord / divider * divider;
    }

    while (ordinal < ord) {
      if (inner.next() == null) {
        throw new IOException(
            "Ordinal " + ord + " exceeded the term iterators capacity");
      }
      ordinal++;
    }
  }

  @Override
  public BytesRef next() throws IOException {
    BytesRef next = inner.next();
    if (next != null) {
      ordinal++;
    }
    return next;
  }

  /* Direct delegations */

  @Override
  public AttributeSource attributes() {
    return inner.attributes();
  }

  public boolean seekExact(BytesRef text, boolean useCache) throws IOException {
    ordOK = false;
    return inner.seekExact(text);
  }

  public SeekStatus seekCeil(BytesRef text, boolean useCache) throws IOException {
    ordOK = false;
    return inner.seekCeil(text);
  }

  @Override
  public SeekStatus seekCeil(BytesRef bytesRef) throws IOException {
      return seekCeil(bytesRef, true);
  }

    @Override
  public void seekExact(BytesRef term, TermState state) throws IOException {
    ordOK = false;
    inner.seekExact(term, state);
  }

  @Override
  public BytesRef term() throws IOException {
    return inner.term();
  }

  @Override
  public int docFreq() throws IOException {
    return inner.docFreq();
  }

  @Override
  public long totalTermFreq() throws IOException {
    return inner.totalTermFreq();
  }

  @Override
  public DocsEnum docs(Bits liveDocs, DocsEnum reuse, int flags) throws IOException {
    return inner.docs(liveDocs, reuse, flags);
  }

  @Override
  public DocsAndPositionsEnum docsAndPositions(
      Bits skipDocs, DocsAndPositionsEnum reuse, int flags)
      throws IOException {
    return inner.docsAndPositions(skipDocs, reuse, flags);
  }

  @Override
  public TermState termState() throws IOException {
    return inner.termState();
  }

  @Override
  public Comparator<BytesRef> getComparator() {
    return inner.getComparator();
  }
}
