package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.exposed.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.DoubleIntArrayList;
import org.apache.lucene.util.packed.MonotonicReaderFactory;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Provides a map from docID to tagIDs (0 or more / document), where tagID
 * refers to a tag in one out of multiple TermProviders. The mapping is optimized
 * towards low memory footprint and extraction of all tags for each given
 * document ID.
 * </p><p>
 * Technical notes: This is essentially a two-dimensional array of integers.
 * Dimension 1 is document ID, dimension 2 is references to indirects.
 * However, plain Java two-dimensional arrays take up a lot of memory so we handle
 * this by letting the array doc2ref contain one pointer for each docID into a
 * second array refs, which contains the references. We know the number of refs
 * to extract for a given docID by looking at the starting point for docID+1.
 * </p><p>
 * The distribution of the entries in refs is defined by the index layout and
 * is assumed to be random.
 * TODO: Consider sorting each sub-part of refs and packing it
 * The distribution of entries in doc2ref is monotonically increasing. We
 * exploit this by having a third array refBase which contains the starting
 * points in refs for every 256th entry in doc2ref. This allows us to use less
 * bits in doc2ref to represent the pointers into refs. The code for the
 * starting point in refs for a given docID is thus
 * {@code refBase[docID >>> 8] + doc2ref[docID]}.
 */
public class FacetMap {

  protected enum IMPL {stable, pass2, pass1}
  // stable is well-tested, pass2 is deprecated and pass1 is experimental
  public static IMPL defaultImpl = IMPL.stable;

  private final List<TermProvider> providers;
  private final int[] indirectStarts;

  private final PackedInts.Reader doc2ref;
  private final PackedInts.Reader refs;

  public static FacetMap createMap(int docCount, List<TermProvider> providers)
      throws IOException {
    return createMap(docCount, providers, defaultImpl);
  }

  public static FacetMap createMap(
      int docCount, List<TermProvider> providers, IMPL impl)
      throws IOException {
    switch (impl) {
      case stable: return new FacetMap(docCount, providers);
      case pass2: return new FacetMap(docCount, providers, true);
      case pass1: return new FacetMap(docCount, providers, "dummy");
      default: throw new UnsupportedOperationException(
          "The implementation '" + impl + "' is unknown");
    }
  }

  // This is highly inefficient as it performs 3 iterations of terms
  // 1. Generate sorted and de-duplicated ordinals list
  // 2. Count references from documents to tags
  // 3. Update map with references from documents to tags
  // By using the decorator and an auto-expanding map, this can be done in a
  // single run.
  public FacetMap(int docCount, List<TermProvider> providers)
      throws IOException {
    this.providers = providers;

    if (ExposedSettings.debug) {
      System.out.println("FacetMap: Creating map for " + providers.size()
          + " group" + (providers.size() == 1 ? "" : "s") + " with " + docCount
          + " documents)");
    }

    indirectStarts = new int[providers.size() +1];
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
    Map.Entry<PackedInts.Reader, PackedInts.Reader> pair =
        extractTags(docCount);
    tagExtractTime += System.currentTimeMillis();
    doc2ref = pair.getKey();
    refs = pair.getValue();
    if (ExposedSettings.debug) {
      System.out.println(
              "FacetMap: Unique count (" + providers.size() + " providers): "
              + uniqueTime + "ms, tag time: " + tagExtractTime + "ms");
    }
  }

  // Experimental 2 pass
  public FacetMap(int docCount, List<TermProvider> providers, boolean disabled)
      throws IOException {
    this.providers = providers;
    if (ExposedSettings.debug) {
      System.out.println("FacetMap: Creating 2 pass map for " + providers.size()
          + " group" + (providers.size() == 1 ? "" : "s") + " with " + docCount
          + " documents)");
    }
    indirectStarts = new int[providers.size() +1];
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
        ((GroupTermProvider)providers.get(i)).setOrderedOrdinals(
            indirectToOrdinal.getPacked());
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
        extractTags(docCount, tagCounts);
    tagExtractTime += System.currentTimeMillis();
    doc2ref = pair.getKey();
    refs = pair.getValue();
    if (ExposedSettings.debug) {
      System.out.println(
              "FacetMap: Unique count, tag counts and tag fill (" + docCount
              + " documents, "
              + providers.size() + " providers): "
              + uniqueTime + "ms, tag time: " + tagExtractTime + "ms");
    }
  }

  // Experimental single pass
  public FacetMap(int docCount, List<TermProvider> providers, String hack)
//  public FacetMap(int docCount, List<TermProvider> providers, String disabled)
      throws IOException {
    final long startTime = System.currentTimeMillis();
    this.providers = providers;
    if (ExposedSettings.debug) {
      System.out.println(
          "FacetMap: Creating single pass map for " + providers.size()
          + " group" + (providers.size() == 1 ? "" : "s") + " with " + docCount
          + " documents)");
    }
    indirectStarts = new int[providers.size() +1];
    int start = 0;
    long uniqueTime = -System.currentTimeMillis();

    // pairs collects unordered docID -> indirect
    DoubleIntArrayList pairs = new DoubleIntArrayList(docCount); // docIDs, indirect
//    System.out.println("******************************");
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
      long uniqueCount = 0;
      BytesRef last = null;
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
          uniqueCount++;
          last = tuple.term;
//          System.out.println("FaM got: " + last.utf8ToString());
        }
      }

      start += indirectToOrdinal.size();
      if (providers.get(i) instanceof GroupTermProvider) {
        // Not at all OO
        ((GroupTermProvider)providers.get(i)).setOrderedOrdinals(
            indirectToOrdinal.getPacked());
      }
  /*
      { // Sanity test
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
      }
    */
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
          + "Sorting d2i and extracting structures. Temporary map: " + pairs);
    }
    long sortTime = -System.currentTimeMillis();
    // TODO: Sorting takes way too much time. Re-introduce tagCount
    pairs.sortByPrimaries();
    sortTime += System.currentTimeMillis();
    if (ExposedSettings.debug) {
      System.out.println(
          "FacetMap: Sorted d2i (" + pairs.size() + " entries) in "
          + sortTime/1000 + " seconds");
    }
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
    long tagExtractTime = - System.currentTimeMillis();
    Map.Entry<PackedInts.Reader, PackedInts.Reader> pair =
        extractTags(docCount, pairs);
    tagExtractTime += System.currentTimeMillis();
    doc2ref = pair.getKey();
    refs = pair.getValue();
    if (ExposedSettings.debug) {
      System.out.println(
              "FacetMap: Unique count, tag counts and tag fill (" + docCount
              + " documents, " + providers.size() + " providers): "
              + uniqueTime + "ms, tag time: " + tagExtractTime + "ms");
    }
  }

  public int getTagCount() {
    return indirectStarts[indirectStarts.length-1];
  }

  private Map.Entry<PackedInts.Reader, PackedInts.Reader> extractTags(
      int docCount) throws IOException {
    // We start by counting the references as this spares us a lot of array
    // content re-allocation
    final int[] tagCounts = new int[docCount]; // One counter for each doc
    // Fill the tagCounts with the number of tags (references really) for each
    // document.
    countTags(tagCounts);
    return extractTags(docCount, tagCounts);
  }

  /**
   * @param docCount the number of documents, including deletions.
   * @param doc2in docID to indirect map for the full facet map.
   *               Sorted by docID, but may skip docIDs.
   * @return doc2ref and refs.
   */
  private Map.Entry<PackedInts.Reader, PackedInts.Reader> extractTags(
      int docCount, DoubleIntArrayList doc2in) {

    // refs are indirects
    // maxRefBlockSize-1 as we count from 0
    final PackedInts.Mutable doc2ref =
        ExposedSettings.getMutable(docCount+1, doc2in.size());
    final PackedInts.Mutable refs = doc2in.getSecondariesPacked();

    // Fill doc2ref and refs from doc2in
    int doc2inIndex = 0;
    int refIndex = 0;
    for (int docID = 0 ; docID < docCount ; docID++) {
      doc2ref.set(docID, refIndex);
      if (doc2inIndex == doc2in.size() ||
          doc2in.getPrimary(doc2inIndex) != docID) { // No entry
        continue;
      }
      while (doc2inIndex < doc2in.size()
             && doc2in.getPrimary(doc2inIndex) == docID) {
        doc2inIndex++;
        refIndex++;
      }
    }
    doc2ref.set(docCount, refIndex); // Terminator

    if (ExposedSettings.debug) {
      System.out.println(String.format(
          "extractTags: temp doc2in(%d refs, %dKB) -> "
          + "doc2ref(%d docs, %dKB (non-optimized: %dKB)) "
          + "+ refs=(%d ins, %dKB)",
          doc2in.size(), doc2in.capacity()*8/1024,
          doc2ref.size(), doc2ref.ramBytesUsed() / 1024,
          1L * docCount * PackedInts.bitsRequired(refs.size()) / 8 / 1024,
          refs.size(), refs.ramBytesUsed()/1024));
    }

    return new AbstractMap.SimpleEntry<PackedInts.Reader, PackedInts.Reader>(
        MonotonicReaderFactory.reduce(doc2ref), refs);
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
  private Map.Entry<PackedInts.Reader, PackedInts.Reader> extractTags(
      int docCount, final int[] tagCounts) throws IOException {
    long totalRefs = 0;

    {
      for (int tagCount : tagCounts) {
        totalRefs += tagCount;
      }
      if (totalRefs > Integer.MAX_VALUE) {
        throw new IllegalStateException(
            "The current implementations does not support more that " +
                "Integer.MAX_VALUE references to tags. The number of " +
                "references was " + totalRefs);
      }
    }

    final PackedInts.Mutable doc2ref =
        ExposedSettings.getMutable(docCount+1, totalRefs);

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
    final PackedInts.Mutable refs = ExposedSettings.getMutable(
        (int)totalRefs, getTagCount());

    // We could save a lot of memory by discarding the tagCounts at this point
    // and use doc2ref to keep track of pointers. However, this adds some time
    // to the overall processing (about 1/3), so we choose speed over ram

    // We are now ready to fill in the actual tagIDs. There will be a lot of
    // random writes to the refs-array as we're essentially inverting index
    // and value from the TermDocs.
    fillRefs(tagCounts, totalRefs, refs);

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

  private void fillRefs(final int[] tagCounts, final long totalRefs,
                        final PackedInts.Mutable refs) throws IOException {
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
                  "Array index out of bounds. refs.size=" + refs.size()
                      + ", refs.bitsPerValue=" + refs.getBitsPerValue()
                      + ", refsPos="
                      + refsPos
                      + ", tuple.indirect+termOffset="
                      + tuple.indirect + "+" + termOffset + "="
                      + (tuple.indirect+termOffset), e);
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
    if (ExposedSettings.debug) {
      System.out.println("FacetMap: Filled map with "
          + ExposedUtil.time("references", totalRefs, fillTime)
          + " out of which was " +
          ExposedUtil.time("nextDocs", nextDocCount, nextDocTime / 1000000));
    }
  }

  private void initDoc2ref(int[] tagCounts, PackedInts.Mutable doc2ref) {
    long initTime = -System.currentTimeMillis();
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
    initTime += System.currentTimeMillis();
    // < 100 ms for 10M doc2refs so we do not print performance data
/*    if (ExposedSettings.debug) {
      System.out.println("initDoc2Ref with " + doc2ref.size() + " doc2refs "
          +"finished in " + initTime + " ms");
    }*/
  }

  /**
   * Iterates all terms and counts the number of references from each document
   * to any tag.
   * @param tagCounts    #tag-references, one entry/document.
   * @throws IOException if the tags could not be iterated.
   */
  private void countTags(final int[] tagCounts) throws IOException {
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
    if (ExposedSettings.debug) {
      System.out.println(
          "FacetMap: Counted " + referenceCount + " tag references for "
          + ExposedUtil.time("documents",  tagCounts.length, tagCountTime)
          + ". Retrieved "
          + ExposedUtil.time("tuples", tupleCount, tupleTime / 1000000));
    }
  }

  /**
   * Takes an array where each entry corresponds to a tagID in this facet map
   * and increments the counts for the tagIDs associated with the given docID.
   * @param tagCounts a structure for counting occurences of tagIDs.
   * @param docID an absolute document ID from which to extract tagIDs.
   */
  // TODO: Check if static helps speed in this inner loop method
  public final void updateCounter(final int[] tagCounts, final int docID) {
    final int start = (int)doc2ref.get(docID);
    final int end = (int)doc2ref.get(docID+1);
    for (int refI = start ; refI < end ; refI++) {
      try {
        tagCounts[(int)refs.get(refI)]++;
      } catch (Exception ex) {
        System.err.println("Exception in updateCounter during evaluation of " +
            "tagCounts[(int)refs.get(" + refI + ")]++ with refs.size()=="
            + refs.size() + ", tagCounts.length()==" + tagCounts.length
            + ", docID==" + docID + ", start==" + start + ", end==" + end
            + " in " + toString());
      }
    }
  }

  public int[] getIndirectStarts() {
    return indirectStarts;
  }

  public List<TermProvider> getProviders() {
    return providers;
  }

  public BytesRef getOrderedTerm(final int termIndirect) throws IOException {
    for (int i = 0 ; i < providers.size() ; i++) {
      if (termIndirect < indirectStarts[i+1]) {
        return providers.get(i).getOrderedTerm(termIndirect- indirectStarts[i]);
      }
    }
    throw new ArrayIndexOutOfBoundsException(
        "The indirect " + termIndirect + " was too high. The maximum indirect "
        + "supported by the current map is "
        + indirectStarts[indirectStarts.length-1]);
  }

  /**
   * Generates an array of terms for the given docID. This method is normally
   * used for debugging and other inspection purposed.
   * @param docID the docID from which to request terms.
   * @return the terms for a given docID.
   * @throws java.io.IOException if the terms could not be accessed.
   */
  public BytesRef[] getTermsForDocID(int docID) throws IOException {
    final int start = (int)doc2ref.get(docID);
    final int end = (int) doc2ref.get(docID+1);
//    System.out.println("Doc " + docID + ", " + start + " -> " + end);
    BytesRef[] result = new BytesRef[end - start];
    for (int refI = start ; refI < end ; refI++) {
      result[refI - start] = getOrderedTerm((int)refs.get(refI));
    }
    return result;
  }

  public String toString() {
    StringWriter sw = new StringWriter();
    sw.append("FacetMap(#docs=").append(Integer.toString(doc2ref.size()-1));
    sw.append(" (").append(packedSize(doc2ref)).append(")");
    sw.append(", #refs=").append(Integer.toString(refs.size()));
    sw.append(" (").append(packedSize(refs)).append(")");
    sw.append(", providers(");
    for (int i = 0 ; i < providers.size() ; i++) {
      if (i != 0) {
        sw.append(", ");
      }
      sw.append(providers.get(i).toString());
    }
    sw.append("))");
    return sw.toString();
  }

  private String packedSize(PackedInts.Reader packed) {
    long bytes = packed.ramBytesUsed();
    if (bytes > 1048576) {
      return bytes / 1048576 + " MB";
    }
    if (bytes > 1024) {
      return bytes / 1024 + " KB";
    }
    return bytes + " bytes";
  }
}
