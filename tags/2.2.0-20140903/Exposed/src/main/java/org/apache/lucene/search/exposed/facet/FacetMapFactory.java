package org.apache.lucene.search.exposed.facet;


import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.util.ELog;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class FacetMapFactory {
  private static final ELog log = ELog.getLog(FacetMapFactory.class);

  public enum IMPL {stable, pass2, pass1long, pass1packed}

  // stable is well-tested, pass2 is deprecated and pass1long is experimental
  public static IMPL defaultImpl = IMPL.pass2;
  /**
   * If true, 1 field, 1 segment, natural order is assumed to also be single valued and an optimized map is used.
   * If it turns out to be multi-valued, fallback to standard mapping will happen. The time spend on the failed
   * build of the optimized is wasted.
   */
  public static boolean attemptSingle = true;
  /**
   * If true, 1 field, 1 segment, natural order facets are treated as single values, even if they have multiple values.
   * The extra values will be discarded, with no order as to which of the conflicting values are kept.
   */
  public static boolean forceSingle = false;

  public static FacetMap createMap(int docCount, List<TermProvider> providers) throws IOException {
    if (providers.size() == 1) {
      TermProvider provider = providers.get(0);
      if (attemptSingle && (provider.getComparator().getOrder() == NamedComparator.ORDER.count ||
                            provider.getComparator().getOrder() == NamedComparator.ORDER.index)) {
        try {
          if (provider.getReader() instanceof AtomicReader) {
            log.info("Got 1 field 1 segment, natural order request. Using optimised FacetMapSingle");
            FacetMap map = FacetMapSingleFactory.createMap(docCount, provider, forceSingle);
            if (map != null) {
              return map;
            }
          }
        } catch (UnsupportedOperationException e) {
          log.warn("Unable to create optimized map as the index is not single segment. Defaulting to generic map");
        }
      }
    }

    return createMap(docCount, providers, defaultImpl);
  }

  public static FacetMap createMap(int docCount, List<TermProvider> providers, IMPL impl) throws IOException {
    log.info("Activating FacetMap factory " + impl + " for " + docCount + " documents with "
             + providers.size() + " providers");

    switch (impl) {
      case stable:      return FacetMapTripleFactory.createMap(docCount, providers);
      case pass2:       return FacetMapDualFactory.createMap(docCount, providers);
      case pass1long:   return FacetMapSingleLongFactory.createMap(docCount, providers);
      case pass1packed: return FacetMapSinglePackedFactory.createMap(docCount, providers);
      default: throw new UnsupportedOperationException("The implementation '" + impl + "' is unknown");
    }
  }

}
