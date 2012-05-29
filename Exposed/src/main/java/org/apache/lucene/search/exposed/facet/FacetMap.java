package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.ExposedUtil;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
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
  // TODO: Consider auto-tuning this value
  private static final int BASE_BITS = 8;
  private static final int EVERY = (int)Math.pow(2, BASE_BITS);

  private final List<TermProvider> providers;
  private final int[] indirectStarts;

  private final int[] refBase;
  private final PackedInts.Mutable doc2ref;
  private final PackedInts.Mutable refs;

  public FacetMap(int docCount, List<TermProvider> providers)
      throws IOException {
    this.providers = providers;
    indirectStarts = new int[providers.size() +1];
    int start = 0;
    long uniqueTime = -System.currentTimeMillis();
    for (int i = 0 ; i < providers.size() ; i++) {
      indirectStarts[i] = start;
      start += providers.get(i).getUniqueTermCount();
    }
    uniqueTime += System.currentTimeMillis();
    indirectStarts[indirectStarts.length-1] = start;

    refBase = new int[(docCount >>> BASE_BITS) + 1];
//    doc2ref = PackedInts.getMutable(docCount+1, PackedInts.bitsRequired(start));
    long tagExtractTime = - System.currentTimeMillis();
    Map.Entry<PackedInts.Mutable, PackedInts.Mutable> pair =
        extractTags(docCount);
    tagExtractTime += System.currentTimeMillis();
    doc2ref = pair.getKey();
    refs = pair.getValue();
//    System.out.println("Unique count: " + uniqueTime
//        + "ms, tag time: " + tagExtractTime + "ms");
  }

  public int getTagCount() {
    return indirectStarts[indirectStarts.length-1];
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
  private Map.Entry<PackedInts.Mutable, PackedInts.Mutable> extractTags(
      int docCount) throws IOException {
    if (ExposedSettings.debug) {
      System.out.println("Creating facet map for " + providers.size()
          + " group" + (providers.size() == 1 ? "" : "s"));
    }
    // We start by counting the references as this spares us a lot of array
    // content re-allocation
    final int[] tagCounts = new int[docCount]; // One counter for each doc
    long maxRefBlockSize = 0; // from refsBase in blocks of EVERY size
    long totalRefs = 0;

    // Fill the tagCounts with the number of tags (references really) for each
    // document.
    countTags(tagCounts);

    { // Update totalRefs and refBase
      int next = 0;
      for (int i = 0 ; i < tagCounts.length ; i++) {
        if (i == next) {
          refBase[i >>> BASE_BITS] = (int)totalRefs;
          next += EVERY;
          maxRefBlockSize = Math.max(maxRefBlockSize,
              totalRefs - (i == 0 ? 0 : refBase[(i-1) >>> BASE_BITS]));

        }
        totalRefs += tagCounts[i];
      }
      maxRefBlockSize = Math.max(maxRefBlockSize,
          totalRefs - (tagCounts.length-1 < EVERY ? 0 :
              refBase[(tagCounts.length-2) >>> BASE_BITS]));
      if (totalRefs > Integer.MAX_VALUE) {
        throw new IllegalStateException(
            "The current implementations does not support more that " +
                "Integer.MAX_VALUE references to tags. The number of " +
                "references was " + totalRefs);
      }
    }

    final PackedInts.Mutable doc2ref =
        ExposedSettings.getMutable(docCount+1, (int)maxRefBlockSize);

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
    return new AbstractMap.SimpleEntry<PackedInts.Mutable, PackedInts.Mutable>(
        doc2ref, refs);
  }

  private void fillRefs(final int[] tagCounts, final long totalRefs,
                        final PackedInts.Mutable refs) throws IOException {
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

          int doc;
          final int base = (int)tuple.docIDBase;
          while ((doc = tuple.docIDs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
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
      System.out.println("Filled map with "
          + ExposedUtil.time("references", totalRefs, fillTime));
    }
  }

  private void initDoc2ref(int[] tagCounts, PackedInts.Mutable doc2ref) {
    long initTime = -System.currentTimeMillis();
    int offset = 0;
    for (int i = 0 ; i < tagCounts.length ; i++) {
      doc2ref.set(i, offset - refBase[i >>> BASE_BITS]);
//      if (tagCounts[i] != 0) {
//          refs.set(offset, tagCounts[i]-1);
      final int oldOffset = offset;
      offset += tagCounts[i];
      tagCounts[i] = oldOffset;

  //    }
    }
    doc2ref.set(
        tagCounts.length, offset - refBase[tagCounts.length >>> BASE_BITS]);
//      doc2ref.set(doc2ref.size()-1, offset);
    initTime += System.currentTimeMillis();
//      System.out.println("Initialized map for " + totalRefs + " references in "
//          + initTime / 1000 + " seconds");
  }

  private void countTags(final int[] tagCounts) throws IOException {
    long tagCountTime = -System.currentTimeMillis();
    long tupleCount = 0;
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
      System.out.println("Counted tag references for "
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
    final int start =
        (int)(refBase[docID >>> BASE_BITS] + doc2ref.get(docID));
    final int end =
        (int)(refBase[(docID+1) >>> BASE_BITS] + doc2ref.get(docID+1));
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
        return providers.get(i).getOrderedTerm(
            termIndirect- indirectStarts[i]);
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
    final int start =
        (int)(refBase[docID >>> BASE_BITS] + doc2ref.get(docID));
    final int end =
        (int)(refBase[(docID+1) >>> BASE_BITS] + doc2ref.get(docID+1));
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
    long bytes = (long)packed.size() * packed.getBitsPerValue() / 8
        + refBase.length * 4;
    if (bytes > 1048576) {
      return bytes / 1048576 + " MB";
    }
    if (bytes > 1024) {
      return bytes / 1024 + " KB";
    }
    return bytes + " bytes";
  }
}
