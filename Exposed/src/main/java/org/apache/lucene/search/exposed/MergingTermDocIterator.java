package org.apache.lucene.search.exposed;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;
import org.apache.lucene.search.exposed.compare.ComparatorFactory;
import org.apache.lucene.search.exposed.compare.NamedCollatorComparator;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Multi source Iterator that merges the ExposedTuples according to the given
 * comparator. Designed to work tightly with {@link GroupTermProvider}.
 * </p><p>
 * It is recommended to use {@link CachedTermProvider} with look-ahead on the
 * sources or otherwise ensure that ordered iteration is fast if no docIDs are
 * to be returned. If docIDs are to be returned, caching at the source level
 * will not help.
 * </p><p>
 * Important: The ExposedTuple is reused between calls to {@link #next()} unless
 * {@link #setReuseTuple(boolean)} is called with false.
 * </p><p>
 * The merger works by keeping a list of the first elements from all sources
 * and seeding a priority queue with indexes 0...#sources-1. When the PQ is
 * popped, the elements corresponding to the indexes in the PQ are compared.
 * When an element is returned, it is delivered to the iterator. The next time
 * next() is called on the iterator, the index for the previous tuple is
 * re-inserted into the PQ, unless the source for that index is empty.
 * This ensures full reuse of the tuples and thus low impact on GC.
 */
class MergingTermDocIterator implements Iterator<ExposedTuple> {
  private final List<Iterator<ExposedTuple>> iterators;
  private final boolean collectDocIDs;
  private final GroupTermProvider groupProvider;

  private final ExposedTuple[] backingTuples;
  private final ExposedPriorityQueue pq;

  private final Collator collator;
  private final RawCollationKey[] backingKeys; // Only used with Collator

  // TODO: Make reuseTuple = true work
  private boolean reuseTuple = false;
  private long indirect = 0;

  /*
  If pending is true, the currentIndex points to the iterator and backingTuple
  that is to be drained.
   */
  private boolean pending = false; // true == Terms or docIDs are ready in tuple
  /**
   * If pending is true, this is ready for delivery.
   */
  private final ExposedTuple tuple = new ExposedTuple("", new BytesRef("UniqueStringNotInIndex¤#%##¤¤&¤/"),
          -1, -1, null, -1);
  /**
   * If pending is true, this is the index for the source responsible for
   * delivering the tuple.
   */
  private int currentIndex = -1;

  /**
   * If this is true and the given comparator is a
   * {@link org.apache.lucene.search.exposed.compare.NamedCollatorComparator}, sorting is optimized by CollatorKeys.
   * This requires ~1-3 MB extra memory but doubles the chunk-sort speed.
   */
  public static boolean optimizeCollator = true;

  public MergingTermDocIterator(
      GroupTermProvider groupProvider, List<TermProvider> sources,
      Comparator<BytesRef>  comparator, boolean collectDocIDs)
      throws IOException {
    this.groupProvider = groupProvider;
    this.collectDocIDs = collectDocIDs;

    iterators = new ArrayList<Iterator<ExposedTuple>>(sources.size());
    backingTuples = new ExposedTuple[sources.size()];

    collator = optimizeCollator &&
        comparator instanceof NamedCollatorComparator ? ((NamedCollatorComparator)comparator).getCollator() : null;
    backingKeys = new RawCollationKey[sources.size()];

    ComparatorFactory.OrdinalComparator wrappedComparator =
        collator == null ?
        ComparatorFactory.wrapBacking(backingTuples, comparator)
        : ComparatorFactory.wrapBacking(backingKeys);
    pq = new ExposedPriorityQueue(wrappedComparator, sources.size());
    int index = 0;
    for (TermProvider source: sources) {
      // TODO: Divide cacheLimit and avoid cache on collectDocIDs(?)
      Iterator<ExposedTuple> iterator = source.getIterator(collectDocIDs);
      iterators.add(iterator);
      ExposedTuple tuple = getNextTuple(index);
      if (tuple != null) {
        backingTuples[index] = tuple;
        if (collator != null) {
          RawCollationKey key = new RawCollationKey();
          backingKeys[index] = collator.getRawCollationKey(tuple.term.utf8ToString(), key);
        }
        pq.add(index);
      }
      index++;
    }
  }

  // Ensure that we can deliver an id
  @Override
  public boolean hasNext() {
    while (true) {
      if (pending) {
//        System.out.println("hasnext ***" + tuple.term.utf8ToString());
        return true;
      }
      if (pq.size() == 0) {
        return false;
      }
      try {
        seekToNextTuple();
      } catch (IOException e) {
        throw new RuntimeException(
            "IOException while seeking to next docID", e);
      }
    }
  }

  @Override
  public ExposedTuple next() {
//    System.out.println("next " + tuple.term.utf8ToString());
    if (!hasNext()) {
      throw new IllegalStateException("The iterator is depleted");
    }
    ExposedTuple delivery = reuseTuple ? tuple : new ExposedTuple(tuple);
    pending = false;
//    System.out.println("Merging delivery: " + delivery);
    return delivery;
  }

  // Pops a tuple from the iterator indicated by the index and modifies term
  // ordinal and docIDBase from segment to index level.
  // If index == -1 or hasNext() == false,  null is returned.
  private ExposedTuple getNextTuple(int index) throws IOException {
    if (index == -1 || !iterators.get(index).hasNext()) {
      return null;
    }
    // TODO: Avoid the creation of a new tuple by assigning to existing tuple
    ExposedTuple nextTuple = new ExposedTuple(iterators.get(index).next());
    long s = nextTuple.docIDBase;
    nextTuple.docIDBase = (int) groupProvider.segmentToIndexDocID(index, nextTuple.docIDBase);
    nextTuple.ordinal = groupProvider.segmentToIndexTermOrdinal(index, nextTuple.ordinal);
    nextTuple.indirect = -1; // Just for good measure: It will be updated later
    return nextTuple;
  }

  private void seekToNextTuple() throws IOException {

/*
    if (collectDocIDs && currentIndex != -1) {

      // Start by checking if the previous source has more tuples with the
      // same ordinal (e.g. if it has more docIDs)
      ExposedTuple candidate = backingTuples[currentIndex];
      if (candidate != null && candidate.ordinal == tuple.ordinal) {
        // Field, term, ordinal and indirect are the same
        tuple.docID = candidate.docID;
        pending = true;
        backingTuples[currentIndex] = getNextTuple(currentIndex);
        return;
      }
    }
  */
    // DocID-optimization must take place before reinserting into pq

    // Take the next tuple
    while (!pending && pq.size() > 0) {
      final ExposedTuple newTuple = pop();
      if (newTuple == null) {
        break;
      }

      if (tuple.term.equals(newTuple.term)) {
        if (!collectDocIDs) {
          continue; // Skip duplicates when no docID
        }
        tuple.set(newTuple.field, tuple.term, tuple.ordinal, tuple.indirect,
            newTuple.docIDs, newTuple.docIDBase);
      } else {
        tuple.set(newTuple.field, newTuple.term, newTuple.ordinal, indirect++,
            newTuple.docIDs, newTuple.docIDBase);
      }
      pending = true;
    }
  }

  @SuppressWarnings("ObjectToString")
  private ExposedTuple pop() throws IOException {
    currentIndex = pq.pop();
    if (currentIndex == -1) {
      return null;
    }
    final ExposedTuple foundTuple = backingTuples[currentIndex];

    final ExposedTuple newTuple = getNextTuple(currentIndex);
    if (newTuple != null) {
      backingTuples[currentIndex] = newTuple;
      if (collator != null) {
        RawCollationKey key = new RawCollationKey();
        try {
          backingKeys[currentIndex] = collator.getRawCollationKey(newTuple.term.utf8ToString(), key);
        } catch (StringIndexOutOfBoundsException e) {
            throw (StringIndexOutOfBoundsException) new StringIndexOutOfBoundsException(
                "StringIndexOutOfBoundsException calling collator.getRawCollationKey("
                + newTuple.term.utf8ToString() + ") with collator " + collator.toString()).initCause(e);
        }
      }
      pq.add(currentIndex);
    }
    return foundTuple;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Not a valid operation");
  }

  public void setReuseTuple(boolean reuseTuple) {
    this.reuseTuple = reuseTuple;
  }
}