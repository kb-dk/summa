package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.util.ELog;

public class TagCollectorFactory {
  private static final ELog log = ELog.getLog(TagCollectorFactory.class);

  public static TagCollector getCollector(FacetMap map) {
    if (ExposedSettings.useSparseCollector) {
      if (ExposedSettings.forceSparseCollector || TagCollectorSparse.isRecommended(map)) {
        if (ExposedSettings.useCompactCollectors) {
          log.debug("Constructing sparse compact collector");
          return new TagCollectorSparsePacked(map);
        } else {
          log.debug("Constructing sparse collector");
          return new TagCollectorSparse(map);
        }
      }
    }
    log.debug("Constructing multi collector");
    return new TagCollectorMulti(map);
  }
}
