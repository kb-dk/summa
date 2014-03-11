package org.apache.lucene.search.exposed;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.util.BytesRef;

/**
 * A representation of Field, Term, ordinal, indirect and potentially docID.
 */
public class ExposedTuple {
  public String field;
  /**
   * The term itself. Note that this is re-used by the iterator, so users of
   * the iterator must finish processing the current term before calling next.
   */
  public BytesRef term;
  /**
   * The ordinal for the term, used for direct lookup.
   */
  public long ordinal;
  /**
   * The indirect for the term, used for lookup with
   * {@link TermProvider#getOrderedTerm(long)}.
   */
  public long indirect;
  /**
   * An iterator for docIDs. null if docIDs are not requested or no docIDs
   * exists for the term.
   * If docIDs are requested and if there are multiple docIDs for the term,
   * multiple ExposedTuples with the same term and the same ordinal might be
   * used.
   */
  // TODO: Consider if deletedDocuments should be provided here
  public DocsEnum docIDs;

  /**
   * The base to add to all docIDs.
   */
  public long docIDBase;

  /*
  * The order of the term, relative to the other Terms delivered, when using
  * an Iterator from {@link #getExposedTuples}. Starts at 0, increments by at
  * most 1 for each subsequent ExposedTuple from the iterator.
  */
  //public long order;

  public static long instances = 0;

  public ExposedTuple(String field, BytesRef term, long ordinal, long indirect, DocsEnum docIDs, long docIDBase) {
    this.field = field;
    this.term = term;
    this.ordinal = ordinal;
    this.indirect = indirect;
    this.docIDs = docIDs;
    this.docIDBase = docIDBase;
    instances++;
  }

  public ExposedTuple(String field, BytesRef term, long ordinal, long indirect) {
    this.field = field;
    this.term = term;
    this.ordinal = ordinal;
    this.indirect = indirect;
    docIDs = null;
    docIDBase = -1;
    instances++;
  }

  public ExposedTuple(ExposedTuple other) {
    field = other.field;
    term = other.term;
    ordinal = other.ordinal;
    indirect = other.indirect;
    docIDs = other.docIDs;
    docIDBase = other.docIDBase;
    instances++;
  }

  public String toString() {
    return "ExposedTuple(" + field + ":"
        + (term == null ? "null" : term.utf8ToString()) + ", ord=" + ordinal + ", indirect=" + indirect
        + ", docIDs " + (docIDs == null ? "not " : "") + "present)";
  }

  // Convenience
  public void set(String field, BytesRef term, long ordinal, long indirect, DocsEnum docIDs, long docIDBase) {
    this.field = field;
    this.term = term;
    this.ordinal = ordinal;
    this.indirect = indirect;
    this.docIDs = docIDs;
    this.docIDBase = docIDBase;
  }
}
