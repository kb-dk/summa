package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.ExposedUtil;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.ELog;
import org.apache.lucene.util.packed.MonotonicReaderFactory;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Battle-tested construction of FacetMapMulti.
 */
public class FacetMapTripleFactory {
  private static final ELog log = ELog.getLog(FacetMapTripleFactory.class);

  // This is highly inefficient as it performs 3 iterations of terms
  // 1. Generate sorted and de-duplicated ordinals list
  // 2. Count references from documents to tags
  // 3. Update map with references from documents to tags
  public static FacetMapMulti createMap(int docCount, List<TermProvider> providers) throws IOException {
    log.info("Creating map for " + providers.size() + " group" + (providers.size() == 1 ? "" : "s") + " with "
             + docCount + " documents)");

    final int[] indirectStarts = new int[providers.size() +1];
    int start = 0;
    long uniqueTime = -System.currentTimeMillis();
    for (int i = 0 ; i < providers.size() ; i++) {
      indirectStarts[i] = start;
      start += providers.get(i).getUniqueTermCount();
    }
    uniqueTime += System.currentTimeMillis();
    indirectStarts[indirectStarts.length-1] = start;

//    doc2ref = PackedInts.getMutable(docCount+1, PackedInts.bitsRequired(start));
    long tagExtractTime = - System.currentTimeMillis();
    Map.Entry<PackedInts.Reader, PackedInts.Reader> pair = extractTags(providers, indirectStarts, docCount);
    tagExtractTime += System.currentTimeMillis();
    final PackedInts.Reader doc2ref = pair.getKey();
    final PackedInts.Reader refs = pair.getValue();
    log.info("Unique count (" + providers.size() + " providers): "
             + uniqueTime + "ms, tag time: " + tagExtractTime + "ms");
    return new FacetMapMulti(providers, indirectStarts, doc2ref, refs);
  }

  private static Map.Entry<PackedInts.Reader, PackedInts.Reader> extractTags(
      List<TermProvider> providers, int[] indirectStarts, int docCount) throws IOException {
    // We start by counting the references as this spares us a lot of array
    // content re-allocation
    final int[] tagCounts = new int[docCount]; // One counter for each doc
    // Fill the tagCounts with the number of tags (references really) for each
    // document.
    countTags(providers, tagCounts);
    return extractTags(providers, indirectStarts, docCount, tagCounts);
  }

  /**
   * Iterates all terms and counts the number of references from each document
   * to any tag.
   *
   * @param providers the sources for terms for tags.
   * @param tagCounts #tag-references, one entry/document.
   * @throws IOException if the tags could not be iterated.
   */
  private static void countTags(List<TermProvider> providers, final int[] tagCounts) throws IOException {
    long tagCountTime = -System.currentTimeMillis();
    long tupleCount = 0;
    long referenceCount = 0;
    long tupleTime = 0;
    for (TermProvider provider: providers) {
      final Iterator<ExposedTuple> tuples = provider.getIterator(true);
      while (tuples.hasNext()) {
        tupleTime -= System.nanoTime();
        final ExposedTuple tuple = tuples.next();
        tupleTime += System.nanoTime();
        tupleCount++;
        if (tuple.docIDs == null) {
          continue;
        }
        int doc;
        // TODO: Test if bulk reading (which includes freqs) is faster
        while ((doc = tuple.docIDs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
          tagCounts[(int)(doc + tuple.docIDBase)]++;
          referenceCount++;
        }
          /*
        int read;
        DocsEnum.BulkReadResult bulk = tuple.docIDs.getBulkResult();
        final IntsRef intsRef = bulk.docs;
        final int base = (int)tuple.docIDBase;
        while ((read = tuple.docIDs.read()) > 0) {
          final int to = read + intsRef.offset;
          for (int i = intsRef.offset ; i < to ; i++) {
            tagCounts[base + intsRef.ints[i]]++;
          }
        }
        */
      }
    }

    tagCountTime += System.currentTimeMillis();
    log.info("Counted " + referenceCount + " tag references for "
             + ExposedUtil.time("documents", tagCounts.length, tagCountTime)
             + ". Retrieved " + ExposedUtil.time("tuples", tupleCount, tupleTime / 1000000));
  }

  /*
  In order to efficiently populate the ref-structure, we perform a three-pass
  run.
  Pass 1: The number of references is counted for each document.
  Pass 2: The doc2ref and refs-arrays are initialized so that doc2ref points
  to the final offsets in refs and so that the first entry in each refs-chunk
  designate the offset in the chunk for the next reference to store.
  Pass 3: The refs-array is filled by iterating the ExposedTuples for each
  TermProvider, adjusting the docID accordingly and storing the tagID in the
  refs-array at the position given by doc2ref plus the offset from pass 2.
  If the offset was larger than 0, it it decreased.
   */
  static Map.Entry<PackedInts.Reader, PackedInts.Reader> extractTags(
      List<TermProvider> providers, int[] indirectStarts, int docCount, final int[] tagCounts) throws IOException {
    long totalRefs = 0;

    {
      for (int tagCount : tagCounts) {
        totalRefs += tagCount;
      }
      if (totalRefs > Integer.MAX_VALUE) {
        throw new IllegalStateException(
            "The current implementations does not support more that Integer.MAX_VALUE references to tags. " +
            "The number of references was " + totalRefs);
      }
    }

    final PackedInts.Mutable doc2ref = ExposedSettings.getMutable(docCount+1, totalRefs);

    // With the tag counts and the refBase in place, it is possible to fill the
    // doc2ref with the correct pointers into the (still non-existing) refs.
    initDoc2ref(tagCounts, doc2ref);
/*    System.out.print("doc2ref:");
    for (int i = 0 ; i < doc2ref.size() ; i++) {
      System.out.print(" " + doc2ref.get(i));
    }
    System.out.println("");
  */
    // As we know the number of references we can create the refs-array.
    final PackedInts.Mutable refs = ExposedSettings.getMutable((int)totalRefs, indirectStarts[indirectStarts.length-1]);

    // We could save a lot of memory by discarding the tagCounts at this point
    // and use doc2ref to keep track of pointers. However, this adds some time
    // to the overall processing (about 1/3), so we choose speed over ram

    // We are now ready to fill in the actual tagIDs. There will be a lot of
    // random writes to the refs-array as we're essentially inverting index
    // and value from the TermDocs.
    fillRefs(providers, indirectStarts, tagCounts, totalRefs, refs);

    // Finally we reduce the doc2ref representation by defining the content of
    // refBase and creating a new doc2ref
    // Find max
/*    {
      long reduceTime = -System.currentTimeMillis();
      long max = 0;
      for (int i = 0 ; i < doc2ref.size() ; i += EVERY) {
        final long count = doc2ref.get(
            Math.min(i + EVERY, doc2ref.size()-1)) - doc2ref.get(i);
        max = Math.max(max, count);
      }

      // Allocate new doc2ref
      PackedInts.Mutable reduced =
          ExposedSettings.getMutable(doc2ref.size(), max);

      // Adjust bases and doc2refs
      for (int i = 0 ; i < doc2ref.size() ; i += EVERY) {
        final long base = doc2ref.get(i);
        if (refBase[i >>> BASE_BITS] != (int)base) {
          System.out.println("Invalid! " + refBase[i >>> BASE_BITS] + " vs " + base);
        }
        refBase[i >>> BASE_BITS] = (int)base;

        final int to = Math.min(doc2ref.size(), i + EVERY);
        for (int docID = i ; docID < to ; docID++) {
          reduced.set(docID, doc2ref.get(docID) - base);
        }
      }
      reduceTime += System.currentTimeMillis();
      System.out.println("Reduced doc2ref with " + doc2ref.size()
          + " entries and " + doc2ref.getBitsPerValue() + " bits/value from "
          + packedSize(doc2ref) + " to " +
          + reduced.getBitsPerValue() + " bits/value = " + packedSize(reduced)
          + " plus " + refBase.length*4/1024 + " KB for refBase in "
          + reduceTime / 1000 + " seconds");
      doc2ref = reduced;
    }*/
    return new AbstractMap.SimpleEntry<PackedInts.Reader, PackedInts.Reader>(
        MonotonicReaderFactory.reduce(doc2ref), refs);
  }

  private static void initDoc2ref(int[] tagCounts, PackedInts.Mutable doc2ref) {
//    long initTime = -System.currentTimeMillis();
    int offset = 0;
    for (int i = 0 ; i < tagCounts.length ; i++) {
      doc2ref.set(i, offset);
//      if (tagCounts[i] != 0) {
//          refs.set(offset, tagCounts[i]-1);
      final int oldOffset = offset;
      offset += tagCounts[i];
      tagCounts[i] = oldOffset;

      //    }
    }
    doc2ref.set(tagCounts.length, offset);
//      doc2ref.set(doc2ref.size()-1, offset);
//    initTime += System.currentTimeMillis();
    // < 100 ms for 10M doc2refs so we do not print performance data
/*    if (ExposedSettings.debug) {
      System.out.println("initDoc2Ref with " + doc2ref.size() + " doc2refs "
          +"finished in " + initTime + " ms");
    }*/
  }


  private static void fillRefs(List<TermProvider> providers, int[] indirectStarts, final int[] tagCounts,
                               final long totalRefs, final PackedInts.Mutable refs) throws IOException {
    long nextDocTime = 0;
    long nextDocCount = 0;

    long fillTime = -System.currentTimeMillis();
    for (int providerNum = 0 ; providerNum < providers.size() ; providerNum++) {
      final TermProvider provider = providers.get(providerNum);
      final long termOffset = indirectStarts[providerNum];
      final Iterator<ExposedTuple> tuples = provider.getIterator(true);
      while (tuples.hasNext()) {
        final ExposedTuple tuple = tuples.next();
        final long indirect = tuple.indirect + termOffset;
        if (tuple.docIDs == null) {
          continue;  // It happens sometimes with non-expunged deletions
        }
       /*
        int read;
        final DocsEnum.BulkReadResult bulk = tuple.docIDs.getBulkResult();
        final IntsRef docs = bulk.docs;
        final int base = (int)tuple.docIDBase;
        while ((read = tuple.docIDs.read()) > 0) {
          final int to = read + docs.offset;
          for (int i = docs.offset ; i < to ; i++) {
//            final int docID = ;
  //          final int refsOrigo = (int)doc2ref.get(docID);
//          final int chunkOffset = (int)refs.get(refsOrigo);
//            final int chunkOffset = --tagCounts[docID];
            final int refsPos = tagCounts[docs.ints[i] + base]++;
            try {
              refs.set(refsPos, indirect);
            } catch (ArrayIndexOutOfBoundsException e) {
              throw new RuntimeException(
                  "Array index out of bounds. refs.size=" + refs.size()
                      + ", refs.bitsPerValue=" + refs.getBitsPerValue()
                      + ", refsPos="
                      + refsPos
                      + ", tuple.indirect+termOffset="
                      + tuple.indirect + "+" + termOffset + "="
                      + (tuple.indirect+termOffset), e);
            }
          }
        }
         */

        nextDocTime -= System.nanoTime();
        int doc;
        final int base = (int)tuple.docIDBase;
        while ((doc = tuple.docIDs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
          nextDocCount++;
          //            final int docID = ;
          //          final int refsOrigo = (int)doc2ref.get(docID);
          //          final int chunkOffset = (int)refs.get(refsOrigo);
          //            final int chunkOffset = --tagCounts[docID];
          final int refsPos = tagCounts[doc + base]++;
          try {
            refs.set(refsPos, indirect);
          } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException(
                "Array index out of bounds. refs.size=" + refs.size() + ", refs.bitsPerValue=" + refs.getBitsPerValue()
                + ", refsPos=" + refsPos + ", tuple.indirect+termOffset=" + tuple.indirect + "+"
                + termOffset + "=" + (tuple.indirect+termOffset), e);
          }
        }
        nextDocTime += System.nanoTime();

       /*
        int doc;
        // TODO: Test if bulk reading (which includes freqs) is faster
        while ((doc = tuple.docIDs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
          final int refsOrigo = (int)doc2ref.get((int)(doc + tuple.docIDBase));
//          final int chunkOffset = (int)refs.get(refsOrigo);
          final int chunkOffset = --tagCounts[((int) (doc + tuple.docIDBase))];
          try {
            refs.set(refsOrigo + chunkOffset, tuple.indirect + termOffset);
          } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException(
                "Array index out of bounds. refs.size=" + refs.size()
                    + ", refs.bitsPerValue=" + refs.getBitsPerValue()
                    + ", refsOrigo+chunkOffset="
                    + refsOrigo + "+" + chunkOffset
                    + "=" + (refsOrigo+chunkOffset)
                    + ", tuple.indirect+termOffset="
                    + tuple.indirect + "+" + termOffset + "="
                    + (tuple.indirect+termOffset), e);
          }
//          if (chunkOffset != 0) {
//            refs.set(refsOrigo, chunkOffset-1);
//          }
        }
         */
      }
    }
    fillTime += System.currentTimeMillis();
    log.info("Filled map with " + ExposedUtil.time("references", totalRefs, fillTime) + " out of which was " +
             ExposedUtil.time("nextDocs", nextDocCount, nextDocTime / 1000000));
  }
}
