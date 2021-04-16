package org.apache.solr.exposed;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import org.apache.lucene.search.exposed.ExposedRequest;
import org.apache.lucene.search.exposed.compare.ComparatorFactory;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.search.exposed.facet.CollectorPool;
import org.apache.lucene.search.exposed.facet.CollectorPoolFactory;
import org.apache.lucene.search.exposed.facet.FacetResponse;
import org.apache.lucene.search.exposed.facet.TagCollector;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;
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
import java.io.StringWriter;
import java.util.*;

import static org.apache.solr.exposed.ExposedIndexLookupParams.*;


/**
 * Wrapper for the Exposed-patch for Lucene, providing access to index
 * lookup. An index lookup is a list of terms from a given field,
 * with origo at a provided term prefix plus a provided delta.
 * Example: An index lookup for 'foo' is requested with delta -2 and
 * returns {bar, baz, foo, zoo}.
 */
public class ExposedIndexLookupQueryComponent extends QueryComponent {
  public static final Logger log =
    LoggerFactory.getLogger(SolrResourceLoader.class);

  public static final int DEFAULT_COLLECTOR_POOLS = 6;
  public static final int DEFAULT_COLLECTOR_FILLED = 6;
  public static final int DEFAULT_COLLECTOR_FRESH = 2;

  private CollectorPoolFactory poolFactory;

  // TODO: Consider merging with the facet component, but keep separate pools
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
  }

  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    if (rb.req.getParams().getBool(ELOOKUP, false)
        && rb.req.getParams().get("q", null) != null) {
      // && rb.req.getParams().getBool(ELOOKUP_QUERY, false)

      // TODO: Check if the query is already cached
      // TODO: Eliminate ELOOKUP_QUERY and use standard query instead?
      rb.setNeedDocSet(true);
      //rb.doFacets = true;
    }
  }

  @Override
  public void process(ResponseBuilder rb) throws IOException {
    if (poolFactory == null) {
      throw new IllegalStateException(
        "CollectorPoolFactory not initialized. Init must be called before process");
    }

    SolrQueryRequest req = rb.req;
    SolrQueryResponse rsp = rb.rsp;
    SolrParams params = req.getParams();
    if (!"true".equals(params.get(ELOOKUP))) {
      return; // Not an exposed facet request
    }

    FacetRequest eReq = createRequest(params);
    if (eReq == null) {
      return; // Not a usable request for exposed index lookup
    }

    CollectorPool collectorPool;
    try {
      collectorPool = poolFactory.acquire(req.getSearcher().getIndexReader(), eReq);
    } catch (IOException e) {
      throw new RuntimeException(
        "Unable to acquire a CollectorPool for " + eReq, e);
    }
    TagCollector tagCollector = null;
    FacetResponse lookupResponse;
    try {
      tagCollector = collectorPool.acquire(eReq.getBuildKey());

      try {
        if (tagCollector.getQuery() == null) { // Not cached so fill it
          tagCollector.collect(rb.getResults().docSet.iterator());
        }
        lookupResponse = tagCollector.extractResult(eReq);
      } catch (IOException e) {
        throw new RuntimeException("Unable to extract response from TagCollector", e);
      }
    } finally {      //***group(name=text_, order=index, locale=null, fields(text), hierarchical=false, delimiter=null)
      // TODO: Check that index lookup works as intended with multiple requests for the same key
      collectorPool.release(eReq.getBuildKey(), tagCollector);
    }
    exposedToSolr(lookupResponse, rsp, req);
  }

  // See http://wiki.apache.org/solr/SimpleFacetParameters#Examples
  private void exposedToSolr(
    FacetResponse fr, SolrQueryResponse rsp, SolrQueryRequest req) {
    NamedList<Object> res = new SimpleOrderedMap<Object>();
    NamedList<Object> fields = new SimpleOrderedMap<Object>(); // Skip groups

    for (FacetResponse.Group group: fr.getGroups()) {
      NamedList<Object> terms = new SimpleOrderedMap<Object>();
      List<LookupTag> searchable = new ArrayList<LookupTag>();
      for (FacetResponse.Tag tag: group.getTags().getTags()) {
        terms.add(tag.getTerm(), tag.getCount());
        searchable.add(new LookupTag(tag.getTerm(), tag.getCount()));
      }
      NamedList<Object> field = new SimpleOrderedMap<Object>();
      field.add("terms", terms);
      field.add("origo", getOrigo(searchable, req));
      fields.add(group.getFieldsStr(), field);
    }
    // Add origo
    res.add("fields", fields);
    rsp.add(ELOOKUP, res);
  }

  private FacetRequest createRequest(SolrParams params) {
/*    String query = params.get("q");
    if (query == null) {
      throw new IllegalArgumentException(
        // TODO: Consider removing this requirements and check
        // for liveDocs is the query is null
        "The parameter 'q' must be specified");
    }*/
    String[] fieldNames = params.getParams(ELOOKUP_FIELD);
    if (fieldNames == null) {
      throw new IllegalArgumentException("At least one field must be specified with " + ELOOKUP_FIELD);
    }

    Locale locale = null;
    String sort = params.get(ELOOKUP_SORT, ELOOKUP_DEFAULT_SORT);
    if ("".equals(sort)) {
      sort = ELOOKUP_DEFAULT_SORT;
    }
    if (!ELOOKUP_SORT_BYINDEX.equals(sort)
        && !ELOOKUP_SORT_BYLOCALE.equals(sort)) {
      throw new IllegalArgumentException(
        "Invalid " + ELOOKUP_SORT + " value: '" + sort + "'. Valid values are "
        + ELOOKUP_SORT_BYINDEX + " and " + ELOOKUP_SORT_BYLOCALE);
    }
    NamedComparator.ORDER order = NamedComparator.ORDER.fromString(sort);
    if (order == NamedComparator.ORDER.locale) {
      String localeStr = params.get(ELOOKUP_SORT_LOCALE_VALUE, null);
      if (localeStr == null) {
        throw new IllegalArgumentException(
          ELOOKUP_SORT + "=" + ELOOKUP_SORT_BYLOCALE + " specified without corresponding " + ELOOKUP_SORT_LOCALE_VALUE);
      }
      locale = new Locale(localeStr);
    }
    NamedComparator comparator = ComparatorFactory.create(locale);

    List<ExposedRequest.Field> fields = new ArrayList<ExposedRequest.Field>();
    StringWriter groupID = new StringWriter(100);
    boolean subsequent = false;
    for (String fieldName: fieldNames) {
      fields.add(new ExposedRequest.Field(fieldName, comparator));
      if (subsequent) {
        groupID.append("_");
      }
      subsequent = true;
      groupID.append(fieldName);
    }
    List<FacetRequestGroup> groups = new ArrayList<FacetRequestGroup>(1);
    ExposedRequest.Group eGroup = new ExposedRequest.Group(
      groupID.toString(), fields, comparator);
    NamedComparator.ORDER facetOrder = ELOOKUP_SORT_BYINDEX.equals(sort) ?
                                         NamedComparator.ORDER.index :
                                         NamedComparator.ORDER.locale;
    String term = params.get(ELOOKUP_TERM, "");
    if (!params.getBool(ELOOKUP_CASE_SENSITIVE, ELOOKUP_DEFAULT_CASE_SENSITIVE)) {
        term = term.toLowerCase(Locale.ENGLISH);
    }
    FacetRequestGroup facetGroup = new FacetRequestGroup(
      eGroup, facetOrder, false, locale == null ? null : locale.toString(),
      params.getInt(ELOOKUP_DELTA, ELOOKUP_DEFAULT_DELTA),
      params.getInt(ELOOKUP_LENGTH, ELOOKUP_DEFAULT_LENGTH),
      params.getInt(ELOOKUP_MINCOUNT, ELOOKUP_DEFAULT_MINCOUNT),
      term);
    groups.add(facetGroup);
    FacetRequest facetRequest =
      new FacetRequest(params.get("q", "*:*"), groups);
    if (locale != null) {
      facetRequest.setOrder(NamedComparator.ORDER.locale);
      facetRequest.setLocale(locale.toString());
    } else {
      facetRequest.setOrder(NamedComparator.ORDER.index);
    }
//    System.out.println(facetRequest.toXML());
    return facetRequest;
  }

  private class LookupTag implements Comparable<LookupTag> {
    protected String term;
    protected int count;

    private LookupTag(String term, int count) {
      this.term = term;
      this.count = count;
    }

    @Override
    public int compareTo(LookupTag other) {
      return term.compareTo(other.getTerm());
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof LookupTag && compareTo((LookupTag)obj) == 0;
    }

    public String getTerm() {
      return term;
    }

    public int getCount() {
      return count;
    }

    public String toString() {
      //noinspection ObjectToString
      return term + "(" + count + ")";
    }
  }

  private Object getOrigo(List<LookupTag> tags, SolrQueryRequest req) {
    String term = req.getParams().get(ELOOKUP_TERM, "");
    String sort = req.getParams().get(ELOOKUP_SORT, ELOOKUP_DEFAULT_SORT);
    Boolean sensitive = req.getParams().getBool(ELOOKUP_CASE_SENSITIVE, ELOOKUP_DEFAULT_CASE_SENSITIVE);
    Comparator<LookupTag> sorter;
    Collator collator = ELOOKUP_SORT_BYINDEX.equals(sort) ? null : createCollator(new Locale(sort));
    sorter = sensitive ? new Sensitive(collator) : new Insensitive(collator);
    int origo = Collections.binarySearch(tags, new LookupTag(term, 0), sorter);
    return origo < 0 ? (origo + 1) * -1 : origo;
  }

  public static Collator createCollator(Locale locale) {
      Collator collator = Collator.getInstance(locale);
      if (collator instanceof RuleBasedCollator) {
          // true ignores spaces and punctuation but at SB space is just
          // as significant as letters (and comes before them)
          ((RuleBasedCollator)collator).setAlternateHandlingShifted(false);
      } else {
          log.warn("Expected the ICU Collator to be a " + RuleBasedCollator.class.getSimpleName()
                   + " but got " + collator.getClass());
      }
      return collator;
  }

  private class Sensitive implements Comparator<LookupTag> {
    private Collator collator = null;
    public Sensitive(Collator collator) {
        this.collator = collator;
    }
    @Override
    public int compare(LookupTag o1, LookupTag o2) {
        return collator == null ?
               o1.getTerm().compareTo(o2.getTerm()) :
               collator.compare(o1.getTerm(), o2.getTerm());
    }
  }

  private class Insensitive implements Comparator<LookupTag> {
    private Collator collator = null;
    public Insensitive(Collator collator) {
        this.collator = collator;
    }
    @Override
    public int compare(LookupTag o1, LookupTag o2) {
      String t1 = o1.getTerm().toLowerCase(Locale.ENGLISH);
      String t2 = o2.getTerm().toLowerCase(Locale.ENGLISH);
      return collator == null ? t1.compareTo(t2) : collator.compare(t1, t2);
    }
  }
}
