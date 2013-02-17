package org.apache.lucene.search.exposed.facet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.exposed.ExposedCache;
import org.apache.lucene.search.exposed.ExposedHelper;
import org.apache.lucene.search.exposed.ExposedIOFactory;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.search.exposed.compare.NamedNaturalComparator;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.util.BytesRef;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Arrays;

// TODO: Change this to LuceneTestCase but ensure Flex
public class TestHierarchicalFacets extends TestCase {
  private ExposedHelper helper;
  private ExposedCache cache;

  public TestHierarchicalFacets(String name) {
    super(name);
//    CodecProvider.setDefaultCodec("Standard");
    TestHierarchicalTermProvider.deleteIndex();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cache = ExposedCache.getInstance();
    helper = new ExposedHelper();
  }

  @Override

  public void tearDown() throws Exception {
    super.tearDown();
    cache.purgeAllCaches();
    helper.close();
  }


  public static Test suite() {
    return new TestSuite(TestHierarchicalFacets.class);
  }

  public void testBasicIndexBuild() throws IOException {
    TestHierarchicalTermProvider.createIndex(1000, 3, 4);
  }

  public static final String HIERARCHICAL_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
          "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\">\n" +
          "  <query>even:true</query>\n" +
          "  <groups>\n" +
          "    <group name=\"hierarchical\" order=\"count\" hierarchical=\"true\">\n" +
          "      <fields>\n" +
          "        <field name=\"deep\" />\n" +
          "      </fields>\n" +
          "      <subtags suborder=\"base\" maxtags=\"5\"/>\n" +
          "    </group>\n" +
          "  </groups>\n" +
          "</facetrequest>";

  // http://sbdevel.wordpress.com/2010/10/05/fast-hierarchical-faceting/
  public int buildSampleIndex() throws IOException {
    IndexWriter w = ExposedHelper.getWriter();
    ExposedHelper.addDocument(w,
        ExposedHelper.ID + ":1",
        ExposedHelper.ALL + ":" + ExposedHelper.ALL,
        "even:true",
        "deep:A/B/C",
        "deep:D/E/F");
    ExposedHelper.addDocument(w,
        ExposedHelper.ID + ":2",
        ExposedHelper.ALL + ":" + ExposedHelper.ALL,
        "even:false",
        "deep:A/B/C",
        "deep:A/B/J");
    ExposedHelper.addDocument(w,
        ExposedHelper.ID + ":3",
        ExposedHelper.ALL + ":" + ExposedHelper.ALL,
        "even:true",
        "deep:A",
        "deep:D/E",
        "deep:G/H/I");
    w.close(true);
    return 3;
  }

  public void testReflection() throws XMLStreamException {
    FacetRequest request = FacetRequest.parseXML(HIERARCHICAL_REQUEST);
    System.out.println(request.toXML());
  }

  @SuppressWarnings({"NullableProblems"})
  public void testMapOld() throws IOException {
    buildSampleIndex();

    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    TermProvider basic = ExposedCache.getInstance().getProvider(
        reader, "myGroup", Arrays.asList("deep"), new NamedNaturalComparator());
    FacetMap map = FacetMapFactory.createMap(reader.maxDoc(), Arrays.asList(basic));
    for (int i = 0 ; i < reader.maxDoc() ; i++) {
      System.out.print("Doc " + i + ":");
      BytesRef[] terms = map.getTermsForDocID(i);
      for (BytesRef term: terms) {
        System.out.print(" " + term.utf8ToString());
      }
      System.out.println("");
    }
  }

  @SuppressWarnings({"NullableProblems"})
  public void testMap() throws IOException {
    final int DOCS = 4;
    TestHierarchicalTermProvider.createIndex(DOCS, 5, 4);

    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    TermProvider basic = ExposedCache.getInstance().getProvider(
        reader, "myGroup", Arrays.asList("deep"), new NamedNaturalComparator());
    FacetMap map = FacetMapFactory.createMap(reader.maxDoc(), Arrays.asList(basic));
    for (int i = 0 ; i < reader.maxDoc() ; i++) {
      System.out.print("Doc " + i + ":");
      BytesRef[] terms = map.getTermsForDocID(i);
      for (BytesRef term: terms) {
        System.out.print(" " + term.utf8ToString());
      }
      System.out.println("");
    }
  }

}