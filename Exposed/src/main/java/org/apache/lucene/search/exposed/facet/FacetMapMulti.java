package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

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
 */
public class FacetMapMulti implements FacetMap {

  private final List<TermProvider> providers;
  private final int[] indirectStarts;

  private final PackedInts.Reader doc2ref;
  private final PackedInts.Reader refs;

  protected FacetMapMulti(List<TermProvider> providers, int[] indirectStarts,
                  PackedInts.Reader doc2ref, PackedInts.Reader refs) {
    this.providers = providers;
    this.indirectStarts = indirectStarts;
    this.doc2ref = doc2ref;
    this.refs = refs;
  }

  @Override
  public int getTagCount() {
    return indirectStarts[indirectStarts.length-1];
  }

  /**
   * Takes an array where each entry corresponds to a tagID in this facet map
   * and increments the counts for the tagIDs associated with the given docID.
   * @param tagCounts a structure for counting occurences of tagIDs.
   * @param docID an absolute document ID from which to extract tagIDs.
   */
  // TODO: Check if static helps speed in this inner loop method
  @Override
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

  @Override
  public BytesRef getOrderedTerm(final int termIndirect) throws IOException {
    for (int i = 0 ; i < providers.size() ; i++) {
      if (termIndirect < indirectStarts[i+1]) {
        return providers.get(i).getOrderedTerm(termIndirect- indirectStarts[i]);
      }
    }
    throw new ArrayIndexOutOfBoundsException(
        "The indirect " + termIndirect + " was too high. The maximum indirect supported by the current map is "
        + indirectStarts[indirectStarts.length-1]);
  }

  @Override
  public BytesRef getOrderedDisplayTerm(final int termIndirect) throws IOException {
    for (int i = 0 ; i < providers.size() ; i++) {
      if (termIndirect < indirectStarts[i+1]) {
        return providers.get(i).getOrderedDisplayTerm(termIndirect- indirectStarts[i]);
      }
    }
    throw new ArrayIndexOutOfBoundsException(
        "The indirect " + termIndirect + " was too high. The maximum indirect supported by the current map is "
        + indirectStarts[indirectStarts.length-1]);
  }

  /**
   * Generates an array of terms for the given docID. This method is normally
   * used for debugging and other inspection purposed.
   * @param docID the docID from which to request terms.
   * @return the terms for a given docID.
   * @throws java.io.IOException if the terms could not be accessed.
   */
  @Override
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
    sw.append("FacetMapMulti(#docs=").append(Integer.toString(doc2ref.size()-1));
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

  @Override
  public int[] getIndirectStarts() {
    return indirectStarts;
  }

  @Override
  public List<TermProvider> getProviders() {
    return providers;
  }

  public PackedInts.Reader getDoc2ref() {
    return doc2ref;
  }

  public PackedInts.Reader getRefs() {
    return refs;
  }
}
