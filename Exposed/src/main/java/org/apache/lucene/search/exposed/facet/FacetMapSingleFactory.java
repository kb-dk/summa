package org.apache.lucene.search.exposed.facet;


import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.GroupTermProvider;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.packed.IdentityReader;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.Iterator;

/**
 * Highly specialized factory for single field, single value, popularity ordered facet.
 */
public class FacetMapSingleFactory extends FacetMapTripleFactory {

  // Returns null if the source is not single valued
  public static FacetMapSingle createMap(int docCount, TermProvider provider) throws IOException {
    if (ExposedSettings.debug) {
      System.out.println(
          "FacetMapSingleFactory: Creating 1 pass 1 field 1 value/doc map with " + docCount + " documents)");
    }

    PackedInts.Mutable refs = PackedInts.getMutable(
        docCount, PackedInts.bitsRequired(provider.getOrdinalTermCount()+1), PackedInts.COMPACT);

    long fillTime = -System.currentTimeMillis();

    Iterator<ExposedTuple> tuples = provider.getIterator(true);
    while (tuples.hasNext()) {
      ExposedTuple tuple = tuples.next();
      if (tuple.indirect != tuple.ordinal) {
        throw new IllegalArgumentException(
            "The factory requires popularity ordering (indirect == ordinal) but found indirect=" + tuple.indirect
            + " and ordinal=" + tuple.ordinal + " for term " + tuple.term.utf8ToString());
      }
      int docID;
      while ((docID = tuple.docIDs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
        if (refs.get(docID) != 0) {
          if (ExposedSettings.debug) {
            System.out.println(
                "FacetMapSingleFactory: The docID " + docID + " was already assigned when assigning term '"
                + tuple.term.utf8ToString() + "'. Returning null instead of map");
          }
          return null;
        }
        refs.set(docID, tuple.indirect+1);
      }
    }

    if (provider instanceof GroupTermProvider) {
      ((GroupTermProvider)provider).setOrderedOrdinals(new IdentityReader((int) provider.getOrdinalTermCount()));
      if (ExposedSettings.debug) {
        System.out.println(String.format(
            "FacetMapSingleFactory: Assigning identity ordered ordinals to %s",
            ((GroupTermProvider)provider).getRequest().getFieldNames()));
      }
    } else if (ExposedSettings.debug) {
      System.out.println(String.format(
          "FacetMapSingleFactory: Hoped for GroupTermProvider, but got %s. Collected ordered ordinals are discarded",
          provider.getClass()));
    }
    fillTime += System.currentTimeMillis();

    if (ExposedSettings.debug) {
      System.out.println(
          "FacetMapSingleFactory: Tag fill (" + docCount + " documents): " + fillTime + "ms");
    }
    return new FacetMapSingle(provider, refs);
  }
}
