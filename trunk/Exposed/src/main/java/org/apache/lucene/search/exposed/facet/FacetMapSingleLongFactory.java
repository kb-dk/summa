package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.GroupTermProvider;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.DoubleIntArrayList;
import org.apache.lucene.util.packed.MonotonicReaderFactory;
import org.apache.lucene.util.packed.PackedIntWrapper;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Single pass FacetMap builder that uses long[]s to hold all termID-docID
 * pairs. While this is markedly faster than {@link FacetMapDualFactory}, the
 * memory overhead is significant, especially when the number of references is
 * high (1.5-3x when testing with the corpus at Statsbiblioteket).
 */
public class FacetMapSingleLongFactory {

  public static FacetMap createMap(int docCount, List<TermProvider> providers)
      throws IOException {
    final long startTime = System.currentTimeMillis();
    if (ExposedSettings.debug) {
      System.out.println(
          "FacetMap: Creating long single pass map for "
          + providers.size() + " group" + (providers.size() == 1 ? "" : "s")
          + " with " + docCount + " documents)");
    }
    final int[] indirectStarts = new int[providers.size() +1];
    int start = 0;
    long uniqueTime = -System.currentTimeMillis();

    // pairs collects unordered docID -> indirect
    DoubleIntArrayList pairs = new DoubleIntArrayList(docCount); // docIDs, indirect
//    System.out.println("******************************");
    long totalUniqueTerms = 0;
    for (int i = 0 ; i < providers.size() ; i++) {
      indirectStarts[i] = start;
      final long termOffset = start;
//      System.out.println("------------------------------");
      Iterator<ExposedTuple> tuples = providers.get(i).getIterator(true);

      // indirect->ordinal are collected in order to be assigned to the current
      // provider, which uses them for later resolving of Tag Strings.
      // If they are not collected here, the terms will be iterated again upon
      // first request for a faceting result.
      DoubleIntArrayList indirectToOrdinal = new DoubleIntArrayList(100);
      BytesRef last = null;
      int localUniqueTerms = 0;
      long localTime = -System.currentTimeMillis();
      int pairsStartSize = pairs.size();
      while (tuples.hasNext()) {
        ExposedTuple tuple = tuples.next();
        indirectToOrdinal.add((int) tuple.indirect, (int) tuple.ordinal);
        int docID;
        while ((docID = tuple.docIDs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
//          System.out.println("*** " + tuple + " docID " + docID);
          pairs.add((int) (tuple.docIDBase + docID),
                    (int) (tuple.indirect+termOffset));
        }
        if (last == null || !last.equals(tuple.term)) {
          last = tuple.term;
          localUniqueTerms++;
//          System.out.println("FaM got: " + last.utf8ToString());
        }
      }
      totalUniqueTerms += localUniqueTerms;
      localTime += System.currentTimeMillis();

      start += indirectToOrdinal.size();
      if (providers.get(i) instanceof GroupTermProvider) {
        // Not at all OO
        PackedInts.Reader i2o = indirectToOrdinal.getPacked();
        ((GroupTermProvider)providers.get(i)).setOrderedOrdinals(i2o);
        if (ExposedSettings.debug) {
          System.out.println(String.format(
              "FacetMap: Assigning indirects for %d unique terms, " +
              "%d references, extracted in %d ms, to %s: %s",
              localUniqueTerms, pairs.size()-pairsStartSize, localTime,
              ((GroupTermProvider)providers.get(i)).getRequest().getFieldNames(),
              i2o));
        }
      } else if (ExposedSettings.debug) {
        System.out.println(String.format(
            "FacetMap: Hoped for GroupTermProvider, but got %s. " +
            "Collected ordered ordinals are discarded",
            providers.get(i).getClass()));
      }
/*      { // Sanity test
        PackedInts.Mutable i2o = indirectToOrdinal.getPacked();
        PackedInts.Reader authoritative = providers.get(i).getOrderedOrdinals();
        if (i2o.size() != authoritative.size()) {
          throw new IllegalStateException(
              "Expected indirect to ordinal map to have size "
              + authoritative.size() + " but got " + i2o.size());
        }
        for (int j = 0 ; j < i2o.size() ; j++) {
          if (i2o.get(j) != authoritative.get(j)) {
            throw new IllegalStateException(
                "Expected indirect to ordinal map entry " + j
                + " to have ordinal value " + authoritative.get(j)
                + " but it had " + i2o.get(j));
          }
        }
      }*/
//      System.out.println("..............................");
/*      long uc = providers.get(i).getUniqueTermCount();
      if (uc != uniqueCount) {
        throw new IllegalStateException(
            "The expected unique term count should be " + uc + " but was "
            + uniqueCount);
      }*/
      //start += providers.get(i).getUniqueTermCount();
    }
    uniqueTime += System.currentTimeMillis();
    indirectStarts[indirectStarts.length-1] = start;
    if (ExposedSettings.debug) {
      System.out.println(
          "FacetMap: Full index iteration in "
          + (System.currentTimeMillis() - startTime) + "ms. "
          + "Commencing extraction of structures. Temporary map: "
          + pairs);
    }
    long tagExtractTime = - System.currentTimeMillis();
    Map.Entry<PackedInts.Reader, PackedInts.Reader> pair =
        extractReferences(pairs, docCount, totalUniqueTerms);
    tagExtractTime += System.currentTimeMillis();
    final PackedInts.Reader doc2ref = pair.getKey();
    final PackedInts.Reader refs = pair.getValue();
    if (ExposedSettings.debug) {
      System.out.println(
              "FacetMap: Unique count, tag counts and tag fill (" + docCount
              + " documents, " + providers.size() + " providers): "
              + uniqueTime + "ms, tag time: " + tagExtractTime + "ms");
    }
    return new FacetMap(providers, indirectStarts, doc2ref, refs);
  }

  /**
   * This operation temporarily allocates a {@code int[docCount]} and runs in
   * <tt>O(n)</tt>, where n is {@code pairs.size()}. In reality, two passes is
   * done on <tt>pairs</tt> where the first one is sequential and the second one
   * performs scattered writes in the destination structure.
   * @param pairs    the docID->indirect mao.
   * @param docCount the number of documents.
   * @param uniqueTerms the number of unique terms (and thus indirects).
   */
  private static Map.Entry<PackedInts.Reader, PackedInts.Reader>
  extractReferences(DoubleIntArrayList pairs, int docCount, long uniqueTerms) {
    long startTime = System.currentTimeMillis();

    // Count tags and convert the tagCounts to starting positions
    final int[] starts = new int[docCount+1];
    for (int i = 0 ; i < pairs.size() ; i++) {
      starts[pairs.getPrimary(i)]++;
    }
    int index = 0 ;
    for (int i = 0 ; i < starts.length ; i++) {
      final int delta = starts[i];
      starts[i] = index;
      index += delta;
    }
    starts[starts.length-1] = index;

    // The starting positions are used in the final structure, so we absorb them
    // right away. Important: We guarantee value copying (by reducing or
    // explicit copy) as they are changed while constructing the ref-structure.
    PackedInts.Reader direct = new PackedIntWrapper(starts);
    PackedInts.Reader doc2ref = MonotonicReaderFactory.reduce(direct);
    if (direct == doc2ref) {
      int[] startsCopy = new int[starts.length];
      System.arraycopy(starts, 0, startsCopy, 0, starts.length);
      doc2ref = new PackedIntWrapper(startsCopy);
    }

    long countTime = System.currentTimeMillis() - startTime;

    final PackedInts.Mutable refs = PackedInts.getMutable(
        pairs.size(), PackedInts.bitsRequired(uniqueTerms), 0);

    // Iterate pairs and put the pairs at the proper starting positions
    for (int pairIndex = 0 ; pairIndex < pairs.size() ; pairIndex++) {
/*      if ((pairIndex & 0xFFFF) == 0) {
        System.out.print("|");
      }*/
      int docID = pairs.getPrimary(pairIndex);
      int destination = starts[docID]++;
      refs.set(destination, pairs.getSecondary(pairIndex));
    }
//    System.out.println("\n");

    if (ExposedSettings.debug) {
      System.out.println(String.format(
          "FacetMap: Extracted doc2in and refs %s from %d pairs for %d docIDs "
          + "in %d seconds (%d of these seconds used for counting docID "
          + "frequencies and creating doc2in structure)",
          refs, pairs.size(), docCount,
          (System.currentTimeMillis()-startTime)/1000, countTime/1000));
    }
    return new AbstractMap.SimpleEntry<PackedInts.Reader, PackedInts.Reader>(
        doc2ref, refs);
  }
}
