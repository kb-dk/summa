package org.apache.lucene.search.exposed.compare;

import com.ibm.icu.text.RawCollationKey;
import org.apache.lucene.search.exposed.CachedCollatorKeyProvider;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Comparator;
import java.util.Locale;

/**
 * Helper class for constructing NamedComparators.
 */
public class ComparatorFactory {

  /**
   * Simple atomic specialization of Comparator<Integer>. Normally used to
   * compare terms by treating the arguments as ordinals and performing lookup
   * in an underlying reader.
   */
  public interface OrdinalComparator {
    int compare(int value1, int value2);
  }

  /**
   * Creates a comparator backed by natural order or a Collator, depending on
   * the given locale.
   * @param locale if null of empty, natural order is used. If defined, a
   *               Collator is created based on {@code new Locale(locale)}.
   * @return either {@link NamedNaturalComparator} or
   * {@link NamedCollatorComparator}.
   */
  public static NamedComparator create(String locale) {
    return create(locale == null || "".equals(locale) ?
                  null :
                  new Locale(locale));
  }

  /**
   * Creates a comparator backed by natural order or a Collator, depending on
   * the given locale.
   * @param locale if null, natural order is used.
   *               If defined, a Collator is created based on the locale.
   * @return either {@link NamedNaturalComparator} or
   * {@link NamedCollatorComparator}.
   */
  public static NamedComparator create(Locale locale) {
    return locale == null ?
           new NamedNaturalComparator() :
           new NamedCollatorComparator(locale);
  }

  /**
   * @param provider   the provider of BytesRefs.
   * @param comparator compares two BytesRef against each other.
   * @return a comparator that compares ordinals resolvable by provider by
   *         looking up the BytesRefs values and feeding them to the comparator.
   */
  public static OrdinalComparator wrap(
    TermProvider provider, Comparator<BytesRef> comparator) {
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
   * Uses the given values as indexes in a backing int[], comparing the values
   * from the int[] directly. If the values are the same, natural integer order
   * for the values are used.
   */
  public static final class IndirectComparator implements OrdinalComparator {
    private final int[] values;

    public IndirectComparator(int[] values) {
      this.values = values;
    }

    @Override
    public final int compare(final int value1, final int value2) {
      final int diff = values[value1]-values[value2];
      return diff == 0 ? value2 - value1 : diff;
    }
  }

  /**
   * Uses the given values as indexes in a backing int[], comparing the values
   * from the int[] directly. If the values are the same, natural integer order
   * for the values are used.
   * </p></p>
   * Reverse version of {@link IndirectComparator}.
   */
  public static final class IndirectComparatorReverse implements OrdinalComparator {
    private final int[] values;

    public IndirectComparatorReverse(int[] values) {
      this.values = values;
    }

    @Override
    public final int compare(final int value1, final int value2) {
      final int diff = values[value2]-values[value1];
      return diff == 0 ? value1 - value2 : diff;
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

    @Override
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

    @Override
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
    @Override
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

    @Override
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

    @Override
    public int compare(int v1, int v2) {
      try {
        return backingKeys[v1].compareTo(backingKeys[v2]);
      } catch (NullPointerException e) {
        return 0;
      }
    }

  }

}
