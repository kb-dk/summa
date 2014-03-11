package org.apache.lucene.search.exposed.facet;


import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.GroupTermProvider;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.ELog;
import org.apache.lucene.util.packed.IdentityReader;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.Iterator;

/**
 * Highly specialized factory for single field, single value, popularity ordered facet.
 */
public class FacetMapSingleFactory extends FacetMapTripleFactory {
  private static final ELog log = ELog.getLog(FacetMapSingleFactory.class);

  public static FacetMapSingle createMap(int docCount, TermProvider provider, boolean forceSingle) throws IOException {
    log.info("Creating 1 pass 1 field 1 value/doc map with " + docCount + " documents and forceSingle=" + forceSingle);
    boolean hasWarnedOnMulti = false;

    PackedInts.Mutable refs = PackedInts.getMutable(
        docCount, PackedInts.bitsRequired(provider.getOrdinalTermCount()+1), PackedInts.COMPACT);

    long fillTime = -System.currentTimeMillis();

    Iterator<ExposedTuple> tuples = provider.getIterator(true);
    long highest = Long.MIN_VALUE;
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
          if (forceSingle) {
            if (!hasWarnedOnMulti) {
              log.warn("The docID " + docID + " was already assigned when assigning term '" + tuple.term.utf8ToString()
                       + "'. Extra values are ignored. This warning will not be repeated for this map");
              hasWarnedOnMulti = true;
            }
            continue;
          }
          log.warn("The docID " + docID + " was already assigned when assigning term '" + tuple.term.utf8ToString()
                   + "'. Returning null instead of map");
          return null;
        }
        refs.set(docID, tuple.indirect+1);
        if (highest < tuple.indirect+1) {
          highest = tuple.indirect+1;
        }
      }
    }
    if (provider instanceof GroupTermProvider) {
      ((GroupTermProvider)provider).setOrderedOrdinals(new IdentityReader((int) provider.getOrdinalTermCount()));
      log.debug("Assigning identity ordered ordinals to " + ((GroupTermProvider)provider).getRequest().getFieldNames());
    } else {
      log.debug(String.format("Hoped for GroupTermProvider, but got %s. Collected ordered ordinals are discarded",
                              provider.getClass()));
    }
    fillTime += System.currentTimeMillis();
    log.info("Finished construction with tag fill (" + docCount + " documents) in " + fillTime/1000.0 + " seconds");
    return new FacetMapSingle(provider, refs, (int) (provider.getOrdinalTermCount()+1));
  }
}
