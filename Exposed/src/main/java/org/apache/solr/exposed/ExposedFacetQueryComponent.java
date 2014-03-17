package org.apache.solr.exposed;

import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.search.exposed.facet.*;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.component.QueryComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.solr.exposed.ExposedFacetParams.*;

/**
 * Wrapper for the Exposed-patch for Lucene, providing fast, low-memory,
 * expensive-startup hierarchical faceting. Arguments are mimicked from
 * http://wiki.apache.org/solr/SimpleFacetParameters
 * </p><p>
 * efacet.query: Lucene query parser syntax query used for faceting.
 *               Cannot be overridden for specific fields.
 */
public class ExposedFacetQueryComponent extends QueryComponent {
  public static final Logger log = LoggerFactory.getLogger(SolrResourceLoader.class);

  public static final int DEFAULT_COLLECTOR_POOLS = 12;
  public static final int DEFAULT_COLLECTOR_FILLED = 2;
  public static final int DEFAULT_COLLECTOR_FRESH = 2;

  private CollectorPoolFactory poolFactory;

  @Override
  public void init(NamedList args) {
    int cPools = DEFAULT_COLLECTOR_POOLS;
    int cFresh = DEFAULT_COLLECTOR_FRESH;
    int cFilled= DEFAULT_COLLECTOR_FILLED;
    if (args.get("pools") != null) {
      NamedList pools =(NamedList)args.get("pools");
      for (int i = 0 ; i < pools.size() ; i++) {
        if ("pools".equals(pools.getName(i))) {
          cPools = Integer.parseInt((String)pools.getVal(i));
        } else
        if ("fresh".equals(pools.getName(i))) {
          cFresh = Integer.parseInt((String)pools.getVal(i));
        } else
        if ("filled".equals(pools.getName(i))) {
          cFilled = Integer.parseInt((String)pools.getVal(i));
        }
      }
    }
    poolFactory = new CollectorPoolFactory(cPools, cFilled, cFresh);
    setupCollectorPolicy(args);
  }

  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    if (rb.req.getParams().getBool(EFACET, false)) {
    // TODO: Check if the query is already cached
      rb.setNeedDocSet( true );
      //rb.doFacets = true;
    }
  }

  @Override
  public void process(ResponseBuilder rb) throws IOException {
    if (poolFactory == null) {
      throw new IllegalStateException("CollectorPoolFactory not initialized. Init must be called before process");
    }

    SolrQueryRequest req = rb.req;
    SolrQueryResponse rsp = rb.rsp;
    SolrParams params = req.getParams();
    if (!"true".equals(params.get(EFACET))) {
      return; // Not an exposed facet request
    }

    setupCollectorPolicy(params);
    setupFacetMapPolicy(params);

    FacetRequest eReq = createRequest(params);
    if (eReq == null) {
      return; // Not a usable request for exposed facets
    }

    CollectorPool collectorPool;
    try {
        collectorPool = poolFactory.acquire(req.getSearcher().getIndexReader(), eReq);
    } catch (IOException e) {
        throw new RuntimeException("Unable to acquire a CollectorPool for " + eReq, e);
    }
    TagCollector tagCollector = collectorPool.acquire(eReq.getBuildKey());

/*    if (tagCollector.getQuery() == null) { // Not cached
      try {
        QParser qp = QParser.getParser(eReq.getQuery(), null, req);
        System.out.println(
            "Query parsed '" + eReq.getQuery() + "' to " + qp.parse());
        req.getSearcher().search(qp.parse(), tagCollector);
      } catch (ParseException e) {
        throw new IllegalArgumentException(
            "Unable to parse '" + eReq.getQuery() + "'", e);
      }
    }*/
    FacetResponse facetResponse;
    try {
        if (tagCollector.getQuery() == null) { // Not cached
          tagCollector.collect(rb.getResults().docSet.getBits());
        }
        facetResponse = tagCollector.extractResult(eReq);
    } catch (IOException e) {
        throw new RuntimeException("Unable to extract response from TagCollector", e);
    }
    collectorPool.release(eReq.getBuildKey(), tagCollector);
    exposedToSolr(facetResponse, rsp);
  }

  // See http://wiki.apache.org/solr/SimpleFacetParameters#Examples
  private void exposedToSolr(FacetResponse fr, SolrQueryResponse rsp) {
    NamedList<Object> res = new SimpleOrderedMap<Object>();
    NamedList<Object> fields = new SimpleOrderedMap<Object>(); // Skip groups
    NamedList<Object> fieldsMeta = new SimpleOrderedMap<Object>(); // Skip groups

    for (FacetResponse.Group group: fr.getGroups()) {
      if (group.isHierarchical()) {
        exposedHierarchicalGroupToSolr(group, fields);
      } else {
        NamedList<Object> field = new SimpleOrderedMap<Object>();
        for (FacetResponse.Tag tag: group.getTags().getTags()) {
          field.add(tag.getTerm(), tag.getCount());
        }
        fields.add(group.getFieldsStr(), field);

        NamedList<Object> meta = new SimpleOrderedMap<Object>();
        FacetResponse.TagCollection tags = group.getTags();

        meta.add("info", fr.getProcessingInfo());
        meta.add("potentialtags", tags.getPotentialTags());
        meta.add("totaltags", tags.getTotalTags());
        meta.add("count", tags.getCount());
        fieldsMeta.add(group.getFieldsStr(), meta);
      }
    }
    res.add(EFACET + "_fields", fields);
    res.add(EFACET + "_fields_meta", fieldsMeta);
    rsp.add(EFACET + "_counts", res);
  }

  private void exposedHierarchicalGroupToSolr(FacetResponse.Group group, NamedList<Object> fields) {
    NamedList<Object> content = new SimpleOrderedMap<Object>();
    content.add("field", group.getFieldsStr());
    FacetResponse.TagCollection tags = group.getTags();
    content.add("paths", expandHierarchical(tags, group.getRequest().getDelimiter(), 0));
    fields.add(group.getFieldsStr(), content);
  }

  private NamedList<Object> expandHierarchical(FacetResponse.TagCollection tags, String delimiter, int level) {
    NamedList<Object> content = new SimpleOrderedMap<Object>();
    content.add("recursivecount", tags.getTotalCount());
    content.add("potentialtags", tags.getPotentialTags());
    content.add("totaltags", tags.getTotalTags());
    content.add("count", tags.getCount());
    content.add("level", level++);
    NamedList<Object> cTags = new SimpleOrderedMap<Object>();
    for (FacetResponse.Tag tag: tags.getTags()) {
      NamedList<Object> cTag = new SimpleOrderedMap<Object>();
      cTag.add("count", tag.getCount());
//      cTag.add("path", tag.getTerm()); // TODO: Calculate full path
      if (tag.getSubTags() != null) {
        cTag.add("sub", expandHierarchical(tag.getSubTags(), delimiter, level));
      }
      String[] tokens = tag.getTerm().split(delimiter);
      cTags.add(tokens[tokens.length-1], cTag);
    }
    content.add("sub", cTags);
    return content;
  }

  private FacetRequest createRequest(SolrParams params) {
    String query = params.get("q");
    if (query == null) {
      throw new IllegalArgumentException("The parameter 'q' must be specified");
    }

    String[] fieldNames = params.getParams(EFACET_FIELD);
    if (fieldNames == null) {
      throw new IllegalArgumentException("At least one field must be specified with " + EFACET_FIELD);
    }

    FacetRequest eReq = new FacetRequest(query);
    // Default values
    eReq.setMaxTags(params.getInt(EFACET_LIMIT, eReq.getMaxTags()));
    eReq.setOffset(params.getInt(EFACET_OFFSET, eReq.getOffset()));
    eReq.setMinCount(params.getInt(EFACET_MINCOUNT, eReq.getMinCount()));
    String sort = params.get(EFACET_SORT, EFACET_SORT_COUNT); // Count is def.
    Boolean reverse = params.getBool(EFACET_REVERSE, false);
    if (EFACET_SORT_INDEX.equals(sort)) {
      eReq.setOrder(NamedComparator.ORDER.index);
    } else if (EFACET_SORT_LOCALE.equals(sort)) {
      eReq.setOrder(NamedComparator.ORDER.locale);
      String locale =params.get(EFACET_SORT_LOCALE_VALUE);
      if (locale != null) {
        eReq.setLocale(locale);
      }
    } else { // EFACET_SORT_COUNT.equals(sort)
        eReq.setOrder(NamedComparator.ORDER.count); // default
    }
    eReq.setHierarchical(params.getBool(EFACET_HIERARCHICAL, false));
    eReq.setDelimiter(params.get(EFACET_HIERARCHICAL_DELIMITER, EFACET_HIERARCHICAL_DELIMITER_DEFAULT));
    eReq.setLevels(params.getInt(EFACET_HIERARCHICAL_LEVELS, Integer.MAX_VALUE));
    eReq.setReverse(reverse);

    // TODO: Add hierarchical
    // TODO: Consider adding grouping

    // Specific fields
    for (String fieldName: fieldNames) {
      // TODO: Handle field-specific settings
      eReq.createGroup(fieldName);
    }
//    System.out.println(eReq.toXML());
    return eReq;
  }

  private void setupCollectorPolicy(SolrParams params) {
    setupCollectorPolicy(params.getBool(EFACET_SPARSE, ExposedSettings.useSparseCollector),
                         params.getBool(EFACET_SPARSE_FORCE, ExposedSettings.forceSparseCollector),
                         params.getDouble(EFACET_SPARSE_FACTOR, TagCollectorSparse.DEFAULT_SPARSE_FACTOR));
  }
  private void setupCollectorPolicy(NamedList args) {
    setupCollectorPolicy(getBoolean(args, EFACET_SPARSE, ExposedSettings.useSparseCollector),
                         getBoolean(args, EFACET_SPARSE_FORCE, ExposedSettings.forceSparseCollector),
                         getDouble(args, EFACET_SPARSE_FACTOR, TagCollectorSparse.DEFAULT_SPARSE_FACTOR));
  }
  private void setupFacetMapPolicy(SolrParams params) {
    setupFacetMapPolicy(params.getBool(EFACET_MAP_SINGLE, FacetMapFactory.attemptSingle),
                        params.getBool(EFACET_MAP_SINGLE_FORCE, FacetMapFactory.forceSingle));
  }
  private void setupFacetMapPolicy(NamedList args) {
    setupFacetMapPolicy(getBoolean(args, EFACET_MAP_SINGLE, FacetMapFactory.attemptSingle),
                        getBoolean(args, EFACET_MAP_SINGLE_FORCE, FacetMapFactory.forceSingle));

  }

  private Boolean getBoolean(NamedList args, String key, boolean defaultValue) {
    Object value;
    return (value = args.get(key)) == null ? defaultValue : (Boolean)value;
  }
  private Double getDouble(NamedList args, String key, double defaultValue) {
    Object value;
    return (value = args.get(key)) == null ? defaultValue : (Double)value;
  }


  private void setupCollectorPolicy(boolean useSparse, boolean forceSparse, double sparseFactor) {
    if (ExposedSettings.useSparseCollector != useSparse || ExposedSettings.forceSparseCollector != forceSparse ||
        Math.abs(TagCollectorSparse.DEFAULT_SPARSE_FACTOR - sparseFactor) > 0.0001) {
      log.info(String.format("Changing sparse tag counter policy to use=%b, force=%b, factor=%f",
                             useSparse, forceSparse, sparseFactor));
      poolFactory.clear();
      ExposedSettings.useSparseCollector = useSparse;
      ExposedSettings.forceSparseCollector = forceSparse;
      TagCollectorSparse.DEFAULT_SPARSE_FACTOR = sparseFactor;
    }
  }

  private void setupFacetMapPolicy(boolean useSingle, boolean forceSingle) {
    if (FacetMapFactory.attemptSingle != useSingle || FacetMapFactory.forceSingle != forceSingle) {
      log.info(String.format("Changing FacetMap single policy to use=%b, force=%b", useSingle, forceSingle));
      poolFactory.clear();
      FacetMapFactory.attemptSingle = useSingle;
      FacetMapFactory.forceSingle = forceSingle;
    }
  }

  // key("efacet.offset", "title") -> "efacet.title.offset"
  private String key(String majorKey, String fieldName) {
    return majorKey.substring(0, EFACET.length()) + "." + fieldName
           + majorKey.substring(EFACET.length(), majorKey.length());
  }
}
