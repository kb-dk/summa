package org.apache.lucene.search.exposed;

import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import com.ibm.icu.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Wraps different types of comparators.
 */
public class ExposedComparators {
  
  /**
   * Simple atomic specialization of Comparator<Integer>. Normally used to
   * compare terms by treating the arguments as ordinals and performing lookup
   * in an underlying reader.
   */
  public interface OrdinalComparator {
    int compare(int value1, int value2);
  }

  public static Comparator<BytesRef> localeToBytesRef(final Locale locale) {
    return locale == null ? new NaturalComparator()
        : collatorToBytesRef(getCollator(locale));
  }

  /**
   * Creates a Collator that ignores punctuation and whitespace, mimicking the
   * Sun/Oracle default Collator.
   * @param locale defines the compare-rules.
   * @return a Collator ready for use.
   */
  public static Collator getCollator(Locale locale) {
    Collator collator = Collator.getInstance(locale);
    if (collator instanceof RuleBasedCollator) {
      // Treat spaces as normal characters
      ((RuleBasedCollator)collator).setAlternateHandlingShifted(false);
    }
    return collator;
  }

  public static Comparator<BytesRef> collatorToBytesRef(
      final Collator collator) {
    return collator == null ? new NaturalComparator()
        : new BytesRefWrappedCollator(collator);
  }
  public static final class BytesRefWrappedCollator implements
                                                          Comparator<BytesRef> {
    private final Collator collator;
    public BytesRefWrappedCollator(Collator collator) {
      this.collator = collator;
    }
    public int compare(BytesRef o1, BytesRef o2) {
      return collator.compare(o1.utf8ToString(), o2.utf8ToString());
    }
    public Collator getCollator() {
      return collator;
    }
  }
  public static final class NaturalComparator implements Comparator<BytesRef> {
    public int compare(BytesRef o1, BytesRef o2) {
      return o1.compareTo(o2); // TODO: Consider null-case
    }
  }

  /**
   * @param provider   the provider of BytesRefs.
   * @param comparator compares two BytesRef against each other.
   * @return a comparator that compares ordinals resolvable by provider by
   *         looking up the BytesRefs values and feeding them to the comparator.
   */
  public static OrdinalComparator wrap(TermProvider provider,
                                   Comparator<BytesRef> comparator) {
    return new BytesRefWrapper(provider, comparator);
  }

  /**
   * @param provider   the provider of BytesRefs.
   * @param map        indirect map from logical index to term.
   * @param comparator compares two BytesRef against each other.
   * @return a comparator that compares ordinals resolvable by provider by
   *         looking up the BytesRefs values and feeding them to the comparator.
   *         The BytesRefs are requested by calling getTerm with map[value].
   */
  public static OrdinalComparator wrapIndirect(
      TermProvider provider, int[] map, Comparator<BytesRef> comparator) {
    return new IndirectBytesRefWrapper(provider, map, comparator);
  }

  /**
   * @param provider   the provider of BytesRefs.
   * @param map        indirect map from logical index to ordinal.
   * @return a comparator that compares ordinals resolvable by provider by
   *         looking up the CollatorKeys and comparing them.
   *         The keys are requested by calling get with map[value].
   */
  public static OrdinalComparator wrapIndirect(
      CachedCollatorKeyProvider provider, int[] map) {
    return new IndirectCollatorKeyWrapper(provider, map);
  }

  /**
   * Creates a comparator that relies on a list of ExposedTuples to get the
   * BytesRef for comparison.
   * @param backingTuples the tuples with the terms to use for comparisons.
   * @param comparator compares two BytesRef against each other.
   * @return a comparator that compares indexes relative to the backingTuples.
   *   The BytesRefs are requested by calling {@code backingTuples[index].term}.
   */
  public static OrdinalComparator wrapBacking(
      ExposedTuple[] backingTuples, Comparator<BytesRef> comparator) {
    return new BackingTupleWrapper(backingTuples, comparator);
  }

  public static OrdinalComparator wrapBacking(RawCollationKey[] backingKeys) {
    return new BackingCollatorWrapper(backingKeys);
  }

  /**
   * Compares the given ordinals directly.
   */
  public static final class DirectComparator implements OrdinalComparator {
    public final int compare(final int value1, final int value2) {
      return value1-value2;
    }
  }

  /**
   * Uses the given values as indexes in a backing int[], comparing the values
   * from the int[] directly. If the values are the same, natural integer order
   * for the values are used.
   */
  public static final class IndirectComparator implements OrdinalComparator {
    private final int[] values;

    public IndirectComparator(int[] values) {
      this.values = values;
    }

    public final int compare(final int value1, final int value2) {
      final int diff = values[value1]-values[value2];
      return diff == 0 ? value2 - value1 : diff;
    }
  }

  /**
   * Like {@link IndirectComparator} but in reverse order.
   */
  public static final class ReverseIndirectComparator
                                                  implements OrdinalComparator {
    private final int[] values;

    public ReverseIndirectComparator(int[] values) {
      this.values = values;
    }

    public final int compare(final int value1, final int value2) {
      final int diff = values[value2]-values[value1];
      return diff == 0 ? value2 - value1 : diff;
    }
  }

  private static class BytesRefWrapper implements OrdinalComparator {
    private final TermProvider provider;
    private final Comparator<BytesRef> comparator;

    public BytesRefWrapper(
        TermProvider provider, Comparator<BytesRef> comparator) {
      this.comparator = comparator;
      this.provider = provider;
    }

    public final int compare(final int value1, final int value2) {
      try {
        return comparator == null ?
            provider.getTerm(value1).compareTo(provider.getTerm(value2)) :
            comparator.compare(
                provider.getTerm(value1), provider.getTerm(value2));
      } catch (IOException e) {
        throw new RuntimeException(
            "IOException encountered while comparing terms for the ordinals "
                + value1 + " and " + value2, e);
      }
    }
  }

  private static class IndirectBytesRefWrapper implements OrdinalComparator {
    private final TermProvider provider;
    private final int[] map;
    private final Comparator<BytesRef> comparator;

    public IndirectBytesRefWrapper(
        TermProvider provider, int[] map, Comparator<BytesRef> comparator) {
      this.provider = provider;
      this.map = map;
      this.comparator = comparator;
    }

    public final int compare(final int value1, final int value2) {
      try {
        return comparator.compare(
            provider.getTerm(map[value1]), provider.getTerm(map[value2]));
      } catch (IOException e) {
        throw new RuntimeException(
            "IOException encountered while comparing terms for the ordinals "
                + value1 + " and " + value2, e);
      }
    }
  }

  private static class IndirectCollatorKeyWrapper implements OrdinalComparator {
    private final CachedCollatorKeyProvider provider;
    private final int[] map;

    public IndirectCollatorKeyWrapper(
        CachedCollatorKeyProvider provider, int[] map) {
      this.provider = provider;
      this.map = map;
    }

    // TODO: Consider null as value
    public final int compare(final int value1, final int value2) {
      try {
        return provider.get(map[value1]).compareTo(provider.get(map[value2]));
      } catch (IOException e) {
        throw new RuntimeException(
            "IOException encountered while comparing keys for the ordinals "
                + value1 + " and " + value2, e);
      }
    }
  }

  private static class BackingTupleWrapper implements OrdinalComparator {
    public ExposedTuple[] backingTerms; // Updated from the outside
    private final Comparator<BytesRef> comparator;

    private BackingTupleWrapper(
        ExposedTuple[] backingTerms, Comparator<BytesRef> comparator) {
      this.backingTerms = backingTerms;
      this.comparator = comparator;
    }

    public int compare(int v1, int v2) {
      try {
      return comparator == null ?
          backingTerms[v1].term.compareTo(backingTerms[v2].term) :
          comparator.compare(backingTerms[v1].term, backingTerms[v2].term);
      } catch (NullPointerException e) {
        // TODO: We should not fail silently here, but how do we notify?
        return 0;
      }
    }
  }

  private static class BackingCollatorWrapper implements OrdinalComparator {
    public RawCollationKey[] backingKeys; // Updated from the outside

    public BackingCollatorWrapper(RawCollationKey[] backingKeys) {
      this.backingKeys = backingKeys;
    }

    public int compare(int v1, int v2) {
      try {
        return backingKeys[v1].compareTo(backingKeys[v2]);
      } catch (NullPointerException e) {
        return 0;
      }
    }

  }

}
