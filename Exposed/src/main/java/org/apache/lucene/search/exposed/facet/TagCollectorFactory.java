package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.util.ELog;

public class TagCollectorFactory {
  private static final ELog log = ELog.getLog(TagCollectorFactory.class);

  public static TagCollector getCollector(FacetMap map) {
    if (ExposedSettings.useSparseCollector) {
      if (ExposedSettings.forceSparseCollector || TagCollectorSparse.isRecommended(map)) {
        log.debug("Constructing sparse collector");
        return new TagCollectorSparse(map);
      }
    }
    log.debug("Constructing multi collector");
    return new TagCollectorMulti(map);
  }
}
