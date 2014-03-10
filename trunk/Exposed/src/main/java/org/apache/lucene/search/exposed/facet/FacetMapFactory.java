package org.apache.lucene.search.exposed.facet;


import org.apache.lucene.search.exposed.TermProvider;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class FacetMapFactory {

  public enum IMPL {stable, pass2, pass1long, pass1packed}

  // stable is well-tested, pass2 is deprecated and pass1long is experimental
  public static IMPL defaultImpl = IMPL.pass2;

  public static FacetMapMulti createMap(int docCount, List<TermProvider> providers)
      throws IOException {
    return createMap(docCount, providers, defaultImpl);
  }

  public static FacetMapMulti createMap(
      int docCount, List<TermProvider> providers, IMPL impl)
      throws IOException {
    switch (impl) {
      case stable: return FacetMapTripleFactory.createMap(docCount, providers);
      case pass2: return FacetMapDualFactory.createMap(docCount, providers);
      case pass1long:
        return FacetMapSingleLongFactory.createMap(docCount, providers);
      case pass1packed:
        return FacetMapSinglePackedFactory.createMap(docCount, providers);
      default: throw new UnsupportedOperationException(
          "The implementation '" + impl + "' is unknown");
    }
  }

}
