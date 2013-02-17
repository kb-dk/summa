package org.apache.lucene.search.exposed.facet;


import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.GroupTermProvider;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.DoubleIntArrayList;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class FacetMapDualFactory extends FacetMapTripleFactory {

  public static FacetMap createMap(int docCount, List<TermProvider> providers)
      throws IOException {
    if (ExposedSettings.debug) {
      System.out.println("FacetMap: Creating 2 pass map for " + providers.size()
          + " group" + (providers.size() == 1 ? "" : "s") + " with " + docCount
          + " documents)");
    }
    final int[] indirectStarts = new int[providers.size() +1];
    int start = 0;
    long uniqueTime = -System.currentTimeMillis();
    final int[] tagCounts = new int[docCount]; // One counter for each doc
//    System.out.println("******************************");
    for (int i = 0 ; i < providers.size() ; i++) {
//      System.out.println("------------------------------");


      // indirect->ordinal are collected in order to be assigned to the current
      // provider, which uses them for later resolving of Tag Strings.
      // If they are not collected here, the terms will be iterated again upon
      // first request for a faceting result.
      DoubleIntArrayList indirectToOrdinal = new DoubleIntArrayList(100);
      Iterator<ExposedTuple> tuples = providers.get(i).getIterator(true);
      long uniqueCount = 0;
      BytesRef last = null;
      while (tuples.hasNext()) {
        ExposedTuple tuple = tuples.next();
        indirectToOrdinal.add((int) tuple.indirect, (int) tuple.ordinal);
        int docID;
        while ((docID = tuple.docIDs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
          tagCounts[(int) (tuple.docIDBase + docID)]++;
        }
        if (last == null || !last.equals(tuple.term)) {
          uniqueCount++;
          last = tuple.term;
//          System.out.println("FaM got: " + last.utf8ToString());
        }
      }
      if (providers.get(i) instanceof GroupTermProvider) {
        // Not at all OO
        PackedInts.Reader i2o = indirectToOrdinal.getPacked();
        ((GroupTermProvider)providers.get(i)).setOrderedOrdinals(i2o);
        if (ExposedSettings.debug) {
          System.out.println(String.format(
              "FacetMap: Assigning ordered ordinals to %s: %s",
              ((GroupTermProvider)providers.get(i)).getRequest().getFieldNames(),
              i2o));
        }
      } else if (ExposedSettings.debug) {
        System.out.println(String.format(
            "FacetMap: Hoped for GroupTermProvider, but got %s. " +
            "Collected ordered ordinals are discarded",
            providers.get(i).getClass()));
      }

      indirectStarts[i] = start;
//      System.out.println("..............................");
/*      long uc = providers.get(i).getUniqueTermCount();
      if (uc != uniqueCount) {
        throw new IllegalStateException(
            "The expected unique term count should be " + uc + " but was "
            + uniqueCount);
      }*/
      //start += providers.get(i).getUniqueTermCount();
      start += uniqueCount;
    }
    uniqueTime += System.currentTimeMillis();
    indirectStarts[indirectStarts.length-1] = start;
         /*
    { // Sanity check
      int[] verifyCount = new int[docCount];
      countTags(verifyCount);
      for (int i = 0 ; i < tagCounts.length ; i++) {
        if (verifyCount[i] != tagCounts[i]) {
          throw new IllegalStateException(
              "At index " + i + "/" + tagCounts.length
              + ", the expected tag count was " + verifyCount[i]
              + " with actual count " + tagCounts[i]);
        }
      }
    }
           */
//    doc2ref = PackedInts.getMutable(docCount+1, PackedInts.bitsRequired(start));
    long tagExtractTime = - System.currentTimeMillis();
    Map.Entry<PackedInts.Reader, PackedInts.Reader> pair =
        extractTags(providers, indirectStarts, docCount, tagCounts);
    tagExtractTime += System.currentTimeMillis();
    final PackedInts.Reader doc2ref = pair.getKey();
    final PackedInts.Reader refs = pair.getValue();
    if (ExposedSettings.debug) {
      System.out.println(
              "FacetMap: Unique count, tag counts and tag fill (" + docCount
              + " documents, "
              + providers.size() + " providers): "
              + uniqueTime + "ms, tag time: " + tagExtractTime + "ms");
    }
    return new FacetMap(providers, indirectStarts, doc2ref, refs);
  }

}
