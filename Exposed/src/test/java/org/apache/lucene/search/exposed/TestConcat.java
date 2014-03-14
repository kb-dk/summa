package org.apache.lucene.search.exposed;

import com.ibm.icu.text.Collator;
import junit.framework.TestCase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.exposed.analysis.ConcatICUCollationAnalyzer;
import org.apache.lucene.search.exposed.compare.NamedNaturalComparator;
import org.apache.lucene.search.exposed.facet.CollectorPool;
import org.apache.lucene.search.exposed.facet.CollectorPoolFactory;
import org.apache.lucene.search.exposed.facet.FacetResponse;
import org.apache.lucene.search.exposed.facet.TagCollector;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TestConcat extends TestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    ExposedHelper.deleteIndex();
  }

  public static final String OFFSET_REQUEST =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
      "<facetrequest xmlns=\"http://lucene.apache.org/exposed/facet/request/1.0\" maxtags=\"3\">\n" +
      "  <query>*:*</query>\n" +
      "  <groups>\n" +
      "    <group name=\"id\" order=\"count\" mincount=\"1\">\n" +
      "      <fields>\n" +
      "        <field name=\"id\" />\n" +
      "      </fields>\n" +
      "    </group>\n" +
      "    <group name=\"concat\" order=\"index\" mincount=\"1\" prefix=\"øre\" offset=\"0\">\n" +
      "      <fields>\n" +
      "        <field name=\"concat\" />\n" +
      "      </fields>\n" +
      "    </group>\n" +
      "  </groups>\n" +
      "</facetrequest>";

  public void testIndexLookup() throws Exception {
    buildConcatIndex("ål", "æblegrød", "øre");
    ExposedUtil.addCollator("da", Collator.getInstance(new Locale("da")),
                            CONFIELD);
    System.out.println(getFacetResult(OFFSET_REQUEST, "*:*").toXML());
  }

  private FacetResponse getFacetResult(
      String facetRequest, String query)
      throws IOException, ParseException, XMLStreamException {
    IndexReader reader = ExposedIOFactory.getReader(
        ExposedHelper.INDEX_LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_46, "id", new WhitespaceAnalyzer(Version.LUCENE_46));
    Query q = qp.parse(query);
    searcher.search(q, TopScoreDocCollector.create(10, false));

    CollectorPoolFactory poolFactory = new CollectorPoolFactory(2, 4, 2);
    FacetRequest request = FacetRequest.parseXML(facetRequest);
    CollectorPool collectorPool = poolFactory.acquire(reader, request);

    TagCollector collector;
    String sQuery = request.getQuery();
    collector = collectorPool.acquire(sQuery);
    long countStart = System.currentTimeMillis();
    searcher.search(q, collector);
    long countTime = System.currentTimeMillis() - countStart;
    collector.setCountTime(countTime);
    FacetResponse  response = collector.extractResult(request);
    if (collector.getQuery() != null) { // Cached count
      response.setCountingCached(true);
    }
    long totalTime = System.currentTimeMillis() - countStart;
    response.setTotalTime(totalTime);
    collectorPool.release(sQuery, collector);
    return response;
  }


  public void testConcatOrder() throws IOException {
    String[] EXPECTED = new String[]{"abe", "bil", "æble", "øre", "ål", "år"};
    buildConcatIndex("bil", "abe", "æble", "ål", "øre", "år");
    IndexSearcher searcher = ExposedHelper.getSearcher();
    ExposedUtil.addCollator("da", Collator.getInstance(new Locale("da")), CONFIELD);
    ExposedCache cache = ExposedCache.getInstance();
    TermProvider provider = cache.getProvider(
        searcher.getIndexReader(), "DummyGroup", Arrays.asList(CONFIELD),
        new NamedNaturalComparator());
    assertEquals("The terms should be collator sorted",
                 provider, EXPECTED, provider.getIterator(false));
  }

  private final static String CONFIELD = "concat";
  private final static String IDFIELD = "id";
  private File buildConcatIndex(String... terms) throws IOException {
    Directory dir = FSDirectory.open(ExposedHelper.INDEX_LOCATION);
    Map<String, Analyzer> map = new HashMap<String, Analyzer>();
    map.put(CONFIELD, new ConcatICUCollationAnalyzer(
            Collator.getInstance(new Locale("da"))));
    map.put(IDFIELD, new WhitespaceAnalyzer(Version.LUCENE_46));
    PerFieldAnalyzerWrapper perField = new PerFieldAnalyzerWrapper(
        new WhitespaceAnalyzer(Version.LUCENE_46), map);
    IndexWriter w  = new IndexWriter(
        dir, new IndexWriterConfig(Version.LUCENE_46, perField));

    Document doc = new Document();
    IndexableField plain = new TextField(IDFIELD, "doc0", Field.Store.NO);
    doc.add(plain);
    for (String term: terms) {
      IndexableField bytes = new TextField(CONFIELD, term, Field.Store.NO);
      doc.add(bytes);
    }
    w.addDocument(doc);
    w.close();
    return ExposedHelper.INDEX_LOCATION;
  }

  private void assertEquals(
      String message, TermProvider provider, String[] expected,
      Iterator<ExposedTuple> iterator) throws IOException {
    int index = 0;
    while (iterator.hasNext()) {
      assertEquals(
          message + ". Terms at index " + index + " should match",
          expected[index++],
          provider.getDisplayTerm(iterator.next().ordinal).utf8ToString());
    }
  }
}
