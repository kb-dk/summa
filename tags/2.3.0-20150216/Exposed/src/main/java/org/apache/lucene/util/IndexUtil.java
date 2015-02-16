package org.apache.lucene.util;


import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for accessing the inner parts of the index.
 */
public class IndexUtil {

  /**
   * Flattens the given reader down to AtomicReaders.
   * A simple wrapper for {@link org.apache.lucene.index.IndexReader#leaves()}.
   * @param reader the IndexReader to flatten.
   * @return a list of AtomicReaders
   */
  public static List<? extends IndexReader> flatten(IndexReader reader) {
    List<AtomicReader> readers = new ArrayList<AtomicReader>();
    for (AtomicReaderContext context: reader.leaves()) {
        readers.add(context.reader());
    }
    return readers;
  }
}
