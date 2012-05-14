package org.apache.lucene.search.exposed;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.Iterator;

/**
 * Single source and single field Iterator that provides terms and potentially
 * document IDs.
 * </p><p>
 * It is recommended to use a {@link CachedTermProvider} as source or otherwise
  * ensure that ordered iteration is fast if no docIDs are to be returned..
 * If docIDs are to be returned, caching at the source level will not help.
 * </p><p>
 * Important: The ExposedTuple is reused between calls to {@link #next()} unless
 * {@link #setReuseTuple(boolean)} is called with false.
 */
public class TermDocIterator implements Iterator<ExposedTuple> {
  private final TermProvider source;
  private final PackedInts.Reader order;
  private final boolean collectDocIDs;

  private int position = 0;
  private boolean reuseTuple = true;

  private DocsEnum docsEnum = null; // Reusing

  private boolean pending = false; // true == Terms or docIDs are ready in tuple
  private final ExposedTuple tuple;

  public TermDocIterator(
      TermProvider source, boolean collectDocIDs) throws IOException {
    this.source = source;
    this.collectDocIDs = collectDocIDs;
    order = source.getOrderedOrdinals();
    String field = source.getField(0);

    tuple = new ExposedTuple(field, null, 0, 0, null, 0);
  }

  // Ensure that we can deliver an id
  public boolean hasNext() {
    while (true) {
      if (pending) {
        return true;
      }
      if (position >= order.size()) {
        if (source instanceof CachedTermProvider && ExposedSettings.debug) {
          System.out.println("TermDocsIterator depleted: "
              + ((CachedTermProvider)source).getStats());
        }
        return false;
      }
      try {
        seekToNextTerm();
      } catch (IOException e) {
        throw new RuntimeException(
            "IOException while seeking to next docID", e);
      }
    }
  }

  public ExposedTuple next() {
    if (!hasNext()) {
      throw new IllegalStateException("The iterator is depleted");
    }
    pending = false;
    return reuseTuple ? tuple : new ExposedTuple(tuple);
  }

  private void seekToNextTerm() throws IOException {
    while (true) {
      pending = false;
      if (position >= order.size()) {
        return;
      }
      tuple.indirect = position;
      tuple.ordinal = order.get(position);
      try {
        tuple.term = source.getTerm(tuple.ordinal);
      } catch (IOException e) {
        throw new RuntimeException(
            "Unable to resolve the term at order="
                + order + ", ordinal=" + order.get(position), e);
      }
      position++;

      if (!collectDocIDs) {
        pending = true;
        return;
      }

      // TODO: Speed up by reusing (this currently breaks TestExposedCache.testIndirectIndex)
      docsEnum = source.getDocsEnum(tuple.ordinal, null);
      if (docsEnum == null) {
        continue; // TODO: Is this possible?
      }
      tuple.docIDs = docsEnum;
      pending = true;
      return;
    }
  }

  public void remove() {
    throw new UnsupportedOperationException("Not a valid operation");
  }

  public void setReuseTuple(boolean reuseTuple) {
    this.reuseTuple = reuseTuple;
  }
}
