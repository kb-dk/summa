package org.apache.lucene.search.exposed;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;
import junit.framework.TestCase;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.exposed.compare.ComparatorFactory;
import org.apache.lucene.search.exposed.compare.NamedNaturalComparator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IndexUtil;
import org.apache.lucene.util.Version;
import org.apache.lucene.util.packed.PackedInts;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

// TODO: The whole sorting comparison-thingie is deprecated as search-time
// sorting is removed from Lucene trunk in favor of indexing collatorkeys
// TODO: Change this to LuceneTestCase but ensure Flex
public class TestExposedCache  extends TestCase {
  public static final int DOCCOUNT = 10;
  private ExposedHelper helper;
  private ExposedCache cache;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cache = ExposedCache.getInstance();
//    CodecProvider.setDefaultCodec("Standard");
    helper = new ExposedHelper();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    cache.purgeAllCaches();
    //helper.close();
  }

  public void testPlainSortDump() throws Exception {
    helper.createIndex(DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_43, ExposedHelper.EVEN,
        new WhitespaceAnalyzer(Version.LUCENE_43));
        //new MockAnalyzer(new Random(), MockTokenizer.WHITESPACE, false));
    Query q = qp.parse("true");
    Sort aSort = new Sort(new SortField("a", SortField.Type.STRING));

    TopFieldDocs docs = searcher.search(q, null, 100, aSort);
    dumpDocs(reader, docs, "a");
    reader.close();
  }

  public void testIndirectIndex() throws IOException {
    helper.createIndex(DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);

    TermProvider provider = ExposedCache.getInstance().getProvider(
        reader, "foo", Arrays.asList("a"), ComparatorFactory.create("da"));

    Iterator<ExposedTuple> iterator = provider.getIterator(false);
    while (iterator.hasNext()) {
      ExposedTuple tuple = iterator.next();
      assertEquals("The provided term should match ordinal lookup term",
          tuple.term.utf8ToString(),
          provider.getTerm(tuple.ordinal).utf8ToString());
      assertEquals("The provided term should match indirect lookup term",
          tuple.term.utf8ToString(),
          provider.getOrderedTerm(tuple.indirect).utf8ToString());
    }

    for (int docID = 0 ; docID < reader.maxDoc() ; docID++) {
      String exposed = provider.getOrderedTerm(
          provider.getDocToSingleIndirect().get(docID)).utf8ToString();
      String direct = reader.document(docID).get("a");
      assertEquals("Doc #" + docID + " should have the correct a-term",
          direct, exposed);
    }
  }

  public void testIndirectSegment() throws IOException {
    helper.createIndex(DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    IndexReader reader = getFirstAtomic(ExposedIOFactory.getReader(
        ExposedHelper.INDEX_LOCATION));

    ExposedRequest.Field fRequest = new ExposedRequest.Field(
        "a", ComparatorFactory.create("da"));
    TermProvider provider = ExposedCache.getInstance().getProvider(
        reader, 0, fRequest, true, true);

    for (int docID = 0 ; docID < reader.maxDoc() ; docID++) {
      long indirect = provider.getDocToSingleIndirect().get(docID);
      String exposed = indirect == -1 ? null :
          provider.getOrderedTerm(indirect).utf8ToString();
      String direct = reader.document(docID).get("a");
      assertEquals("Doc #" + docID + " should have the correct a-term",
          direct, exposed);
    }
  }

  private AtomicReader getFirstAtomic(IndexReader reader) {
      return (AtomicReader)IndexUtil.flatten(reader).get(0);
  }

  public void testDocCount() throws IOException {
    int docs = 12000;
    helper.createIndex(docs, Arrays.asList("a", "b"), 20, 2);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    ExposedCache.getInstance().purgeAllCaches();

    TermProvider provider = ExposedCache.getInstance().getProvider(
        reader, "foo", Arrays.asList("a"), ComparatorFactory.create("da"));
    assertEquals("The number of documents should be correct",
        reader.maxDoc(), provider.getMaxDoc());
  }

    // TODO: Implement test for re-open
    /*
  public void testReopen() throws IOException, ParseException {
    final int RUNS = 5;
    final int UPDATE_SIZE = 5000;
    List<Long> exposedSortTimes = new ArrayList<Long>(RUNS);

    helper.createIndex(UPDATE_SIZE, Arrays.asList("a", "b"), 20, 2);

    DirectoryReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    QueryParser qp = new QueryParser(
        Version.LUCENE_43, ExposedHelper.ALL, new MockAnalyzer(
        new Random(), MockTokenizer.WHITESPACE, false));
    Query query = qp.parse(ExposedHelper.ALL);
//    Sort plainSort = new Sort(new SortField("a", new Locale("da")));
    Sort plainSort = new Sort(new SortField("a", SortField.Type.STRING));

    exposedSortTimes.add(testReopen(reader, query, plainSort));
    for (int i = 1 ; i < RUNS ; i++) {
      System.out.println("\nModifying index and re-opening reader "
          + (i+1) + "/" + RUNS);
      helper.createIndex(UPDATE_SIZE, Arrays.asList("a", "b"), 20, 1);
      IndexReader oldReader = reader;
      reader = reader.reopen();
      assertFalse("The index should change", oldReader == reader);
      oldReader.close();
      exposedSortTimes.add(testReopen(reader, query, plainSort));
    }
    reader.close();
    long total = 0;
    for (long t: exposedSortTimes) {
      total += t;
    }
    System.out.println(
        "\n" + RUNS + " exposed sortings with doc#-delta " + UPDATE_SIZE
        + " between each took " + total
        + " ms split in " + exposedSortTimes);
  }
      */
  private long testReopen(
      IndexReader reader, Query query, Sort plainSort) throws IOException {
    final int docCount = reader.maxDoc();
    final int MAX_HITS = 100;
    Sort exposedSort = new Sort(new SortField(
        "a", new ExposedFieldComparatorSource(
      reader, new NamedNaturalComparator())));
        //new ExposedFieldComparatorSource(reader, new Locale("da"))));

    IndexSearcher searcher = new IndexSearcher(reader);
    long plainTime = System.currentTimeMillis();
    TopFieldDocs plainDocs = searcher.search(query, MAX_HITS, plainSort);
    plainTime = System.currentTimeMillis() - plainTime;

    long exposedTime = System.currentTimeMillis();
    // TODO: Check for fillfields
    TopFieldDocs exposedDocs = searcher.search(query, MAX_HITS, exposedSort);
    exposedTime = System.currentTimeMillis() - exposedTime;
    System.out.println(
        "Reopen sort test extracted "
            + Math.min(MAX_HITS, docCount) + "/" + docCount + " hits in "
            + plainTime + " ms for standard Lucene collator sort and "
            + exposedTime + " ms for exposed sort");
//    dumpDocs(reader, plainDocs, sortField);
//    dumpDocs(reader, exposedDocs, sortField);
    assertEquals("The two search results should be equal",
        plainDocs, exposedDocs);
    return exposedTime;
  }

  @Test
  public void testExposedSortWithNullValues() throws Exception {
    testExposedSort(ExposedHelper.EVEN_NULL, DOCCOUNT, false);
  }

  public void testExposedSortWithRandom() throws Exception {
    testExposedSort("a", DOCCOUNT, false);
  }

  public void testExposedSortWithRandomReverse() throws Exception {
    testExposedSort("a", DOCCOUNT, false);
    helper.close();
    cache.purgeAllCaches();
    helper = new ExposedHelper();
    testExposedSort("a", DOCCOUNT, false, true);
  }

  public void testSpeedExposedSortWithRandom() throws Exception {
//    testExposedSort("a", 150000, true); Fails at GrowingMutable.get from compareBottom with java.lang.ArrayIndexOutOfBoundsException: 22674
    testExposedSort("a", 20000, true);
  }

  private void testExposedSort(String sortField, int docCount, boolean feedback)
      throws IOException, ParseException {
    testExposedSort(sortField, docCount, feedback, false);
  }

  /*
  TODO: Create a new test that actually uses Collator-based sorting
   */
  private void testExposedSort(
      String sortField, int docCount, boolean feedback, boolean reverse)
                                            throws IOException, ParseException {
    helper.createIndex(docCount, Arrays.asList("a", "b"), 20, 2);
//    testExposedSort(ExposedHelper.INDEX_LOCATION, new Locale("da"),
    testExposedSort(ExposedHelper.INDEX_LOCATION, null,
        sortField, docCount, feedback, reverse);
  }

  public void testExternallyGeneratedIndex() throws Exception {
    File external = new File("/home/te/projects/lucene4index");
    if (!external.exists()) {
      return;
    }
    testExposedSort(external, new Locale("da"), "sort_title", 10, true, false);
  }

  public void testExposedNaturalOrder() throws IOException, ParseException {
    helper.createIndex(DOCCOUNT, Arrays.asList("a"), 20, 2);
    testExposedSort(ExposedHelper.INDEX_LOCATION, null, "a", 10, true, false);
  }

  // locale can be null
  private void testExposedSort(File index, Locale locale, String sortField,
                               int docCount, boolean feedback, boolean reversed)
      throws IOException, ParseException {
    if (locale != null) {
      throw new UnsupportedOperationException(
          "Locale-based sort not supported at this time since it was removed " +
              "from Lucene 4 trunk");
    }
    final int MAX_HITS = 50;
    int retries = feedback ? 5 : 1;

    IndexReader reader = ExposedIOFactory.getReader(index);
    IndexSearcher searcher = new IndexSearcher(reader);
    QueryParser qp = new QueryParser(
        Version.LUCENE_43, ExposedHelper.ALL,
        new WhitespaceAnalyzer(Version.LUCENE_43));
        //new MockAnalyzer(new Random(), MockTokenizer.WHITESPACE, false));
    Query q = qp.parse(ExposedHelper.ALL);
    Sort aPlainSort = new Sort(
        new SortField(sortField, SortField.Type.STRING, reversed));
  //      new Sort(new SortField(sortField, locale, reversed));
    Sort aExposedSort = new Sort(new SortField(
        sortField, new ExposedFieldComparatorSource(reader, locale), reversed));

    for (int run = 0 ; run < retries ; run++) {
      long plainTime = System.currentTimeMillis();
      TopFieldDocs plainDocs = searcher.search(q, MAX_HITS, aPlainSort);
      plainTime = System.currentTimeMillis() - plainTime;

      long exposedTime = System.currentTimeMillis();
      TopFieldDocs exposedDocs = searcher.search(q, MAX_HITS, aExposedSort);
      exposedTime = System.currentTimeMillis() - exposedTime;
      System.out.println(
          "Sorting on field " + sortField + " with "
              + Math.min(MAX_HITS, docCount) + "/" + docCount + " hits took "
              + plainTime + " ms for standard Lucene collator sort and "
              + exposedTime + " ms for exposed sort");
//    dumpDocs(reader, plainDocs, sortField);
//    dumpDocs(reader, exposedDocs, sortField);
      assertEquals("The two search results should be equal",
          plainDocs, exposedDocs);
    }
    reader.close();
  }

  public void testRawSort() throws IOException {
//    final int[] SIZES = new int[]{10, 1000, 20000, 100000};
    final int[] SIZES = new int[]{20000};
    final int RUNS = 3;
    Random random = new Random(87);
    long createTime = System.currentTimeMillis();
    for (int size: SIZES) {
      List<BytesRef> terms = new ArrayList<BytesRef>(size);
      for (int termPos = 0 ; termPos < size ; termPos++) {
        String term = ExposedHelper.getRandomString(
            random, ExposedHelper.CHARS, 1, 20) + termPos;
        terms.add(new BytesRef(term));
      }
      System.out.println("Created " + size + " random BytesRefs in "
          + (System.currentTimeMillis() - createTime) + "ms");

      Comparator<BytesRef> comparator = ComparatorFactory.create("da");

      for (int run = 0 ; run < RUNS ; run++) {
        Collections.shuffle(terms);

        long sortTime = System.currentTimeMillis();
        Collections.sort(terms, comparator);
        sortTime = System.currentTimeMillis() - sortTime;
        System.out.println("Dumb sorted "
            + ExposedUtil.time("bytesrefs", terms.size(), sortTime));

        long keyTime = System.currentTimeMillis();
        Collator plainCollator = Collator.getInstance(new Locale("da"));
        List<RawCollationKey> keys = new ArrayList<RawCollationKey>(size);
        for (BytesRef term: terms) {
          RawCollationKey key = new RawCollationKey();
          keys.add(plainCollator.getRawCollationKey(term.utf8ToString(), key));
        }
        keyTime = System.currentTimeMillis() - keyTime;
        System.out.println("Created "
            + ExposedUtil.time("CollatorKeys", size, keyTime));
        Collections.shuffle(keys);

        long keySortTime = System.currentTimeMillis();
        Collections.sort(keys);
        keySortTime = System.currentTimeMillis() - keySortTime;
        System.out.println("Collator sorted "
            + ExposedUtil.time("CollatorKeys", size, keySortTime));
      }
      System.out.println("");
    }
  }

  public void testPlainOrdinalAccess() throws IOException {
    final int[] SIZES = new int[]{10, 1000, 50000};
    final int RETRIES = 6;
    for (int size: SIZES) {
      System.out.println("\nMeasuring for " + size + " documents");
      helper.close();
      helper = new ExposedHelper();
      helper.createIndex(size, Arrays.asList("a", "b"), 20, 2);
      AtomicReader reader = getFirstAtomic(
          ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION));
      TermsEnum terms = reader.fields().terms("a").iterator(null);
      long num = reader.fields().terms("a").size();

      Random random = new Random();
      for (int i = 0 ; i < RETRIES ; i++) {
        boolean doRandom = i % 2 == 0;
        long firstHalf = System.currentTimeMillis();
        for (int ordinal = 0 ; ordinal < num / 2 ; ordinal++) {
          try {
          terms.seekExact(doRandom ? random.nextInt(ordinal+1) : ordinal);
          } catch (UnsupportedOperationException e) {
            System.out.println("The current codec for field 'a' does not " +
                "support ordinal-based access. Skipping test");
            return;
          }
          BytesRef term = terms.term();
          assertNotNull(term); // To avoid optimization of terms.term()
        }
        firstHalf = System.currentTimeMillis() - firstHalf;

        long secondHalf = System.currentTimeMillis();
        for (long ordinal = num / 2 ; ordinal < num ; ordinal++) {
          terms.seekExact(
              doRandom ? random.nextInt((int) (ordinal+1)) : ordinal);
          BytesRef term = terms.term();
          assertNotNull(term); // To avoid optimization of terms.term()
        }
        secondHalf = System.currentTimeMillis() - secondHalf;

        System.out.println("Seeked " + num + " ordinals "
            + (doRandom ? "randomly" : "sequent.") + ". " +
            "First half: " + firstHalf + " ms, second half: "
            + secondHalf + " ms"
            + (firstHalf + secondHalf == 0 ? "" : ". "
            + num / (firstHalf + secondHalf) + " seeks/ms"));
      }
      reader.close();
    }
  }
  
  public void testExposedOrdinalAccess() throws IOException {
    final int[] SIZES = new int[]{10, 1000, 50000};
    final int RETRIES = 6;
    for (int size: SIZES) {
      System.out.println("\nMeasuring for " + size + " documents by exposed");
      helper.close();
      helper = new ExposedHelper();
      helper.createIndex(size, Arrays.asList("a", "b"), 20, 2);
      IndexReader reader = getFirstAtomic(ExposedIOFactory.getReader(
          ExposedHelper.INDEX_LOCATION));
      ExposedRequest.Field request =
          new ExposedRequest.Field("a", new NamedNaturalComparator());
      TermProvider provider = ExposedCache.getInstance().getProvider(
          reader, 0, request, true, true);
      long num = provider.getMaxDoc();

      Random random = new Random();
      for (int i = 0 ; i < RETRIES ; i++) {
        boolean doRandom = i % 2 == 0;
        long firstHalf = System.currentTimeMillis();
        for (int ordinal = 0 ; ordinal < num / 2 ; ordinal++) {
          BytesRef term = provider.getTerm(
              doRandom ? random.nextInt(ordinal+1) : ordinal);
          assertNotNull(term); // To avoid optimization of terms.term()
        }
        firstHalf = System.currentTimeMillis() - firstHalf;

        long secondHalf = System.currentTimeMillis();
        for (long ordinal = num / 2 ; ordinal < num ; ordinal++) {
          BytesRef term = provider.getTerm(
              doRandom ? random.nextInt((int) (ordinal+1)) : ordinal);
          assertNotNull(term); // To avoid optimization of terms.term()
        }
        secondHalf = System.currentTimeMillis() - secondHalf;

        System.out.println("Requested " + (num-1) + " ordinals "
            + (doRandom ? "randomly" : "sequent.") + ". " +
            "First half: " + firstHalf + " ms, second half: "
            + secondHalf + " ms"
            + (firstHalf + secondHalf == 0 ? "" : ". "
            + num / (firstHalf + secondHalf) + " seeks/ms"));
      }
      reader.close();
    }
  }
         /*

Measuring for 50000 documents
Created 50000 document index with 4 fields with average term length 10 and total size 7MB in 2409ms
org.apache.lucene.index.codecs.standard.StandardTermsDictReader$FieldReader@38a36b53
org.apache.lucene.index.codecs.standard.StandardTermsDictReader$FieldReader@d694eca
Chunk sorted 0-19999: 20000 ordinals in 1061 ms: ~= 18 ordinals/ms: CachedTermProvider cacheSize=20000, misses=1/255151, lookups=20000 (66 ms ~= 300 lookups/ms), readAheads=19999
Chunk sorted 20000-25000: 5001 ordinals in 179 ms: ~= 27 ordinals/ms: CachedTermProvider cacheSize=20000, misses=1/54131, lookups=5001 (2 ms ~= 1722 lookups/ms), readAheads=5000
Chunk merged 2 chunks in 234 ms: ~= 0 chunks/ms aka 25001 terms in 234 ms: ~= 106 terms/ms: CachedTermProvider cacheSize=20000, misses=15/49938, lookups=25804 (32 ms ~= 789 lookups/ms), readAheads=25797
Chunk total sort for field a: 25001 terms in 1478 ms: ~= 16 terms/ms
Chunk sorted 0-19999: 20000 ordinals in 523 ms: ~= 38 ordinals/ms: CachedTermProvider cacheSize=20000, misses=1/257147, lookups=20000 (12 ms ~= 1566 lookups/ms), readAheads=19999
Chunk sorted 20000-24998: 4999 ordinals in 112 ms: ~= 44 ordinals/ms: CachedTermProvider cacheSize=20000, misses=1/53847, lookups=4999 (2 ms ~= 1752 lookups/ms), readAheads=4998
Chunk merged 2 chunks in 116 ms: ~= 0 chunks/ms aka 24999 terms in 116 ms: ~= 215 terms/ms: CachedTermProvider cacheSize=20000, misses=21/49996, lookups=25217 (15 ms ~= 1674 lookups/ms), readAheads=25210
Chunk total sort for field a: 24999 terms in 753 ms: ~= 33 terms/ms
FacetGroup total iterator construction: 50000 ordinals in 2246 ms: ~= 22 ordinals/ms
TermDocsIterator depleted: CachedTermProvider cacheSize=5000, misses=411/25001, lookups=26406 (33 ms ~= 798 lookups/ms), readAheads=26181
TermDocsIterator depleted: CachedTermProvider cacheSize=5000, misses=390/24999, lookups=26092 (32 ms ~= 799 lookups/ms), readAheads=25868
FacetGroup ordinal iterator depletion from 2 providers: 50001 ordinals in 426 ms: ~= 117 ordinals/ms
Got 50001 ordered ordinals in 2673ms: ~18 terms/ms

          */
  public void testTiming() throws IOException {
//    final int[] SIZES = new int[]{10, 1000, 20000, 50000};
    final int[] SIZES = new int[]{20000};
    for (int size: SIZES) {
      System.out.println("\nMeasuring for " + size + " documents");
      helper.close();
      helper = new ExposedHelper();
      helper.createIndex(size, Arrays.asList("a", "b"), 20, 2);
      IndexReader reader =
          ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
      ExposedCache.getInstance().purgeAllCaches();

      TermProvider provider = ExposedCache.getInstance().getProvider(
          reader, "foo", Arrays.asList("a"), ComparatorFactory.create("da"));
      assertEquals("The number of ordinal accessible terms should match",
          size, provider.getOrdinalTermCount());

      long sortTime = System.currentTimeMillis();
      PackedInts.Reader orderedOrdinals = provider.getOrderedOrdinals();
      sortTime = System.currentTimeMillis() - sortTime;
      System.out.println("Got " + orderedOrdinals.size()
          + " ordered ordinals in " + sortTime + "ms"
          + (sortTime == 0 ? "" : ": ~" + (orderedOrdinals.size() / sortTime)
          + " terms/ms"));
      reader.close();
/*      Iterator<ExposedTuple> iterator = provider.getIterator(false);
      while (iterator.hasNext()) {
        ExposedTuple tuple = iterator.next();
        assertEquals("The provided term should match ordinal lookup term",
            tuple.term.utf8ToString(),
            provider.getTerm(tuple.ordinal).utf8ToString());
        assertEquals("The provided term should match indirect lookup term",
            tuple.term.utf8ToString(),
            provider.getOrderedTerm(tuple.indirect).utf8ToString());
      }

      for (int docID = 0 ; docID < reader.maxDoc() ; docID++) {
        String exposed = provider.getOrderedTerm(
          provider.getDocToSingleIndirect().get(docID)).utf8ToString();
      String direct = reader.document(docID).get("a");
      assertEquals("Doc #" + docID + " should have the correct a-term",
          direct, exposed);
    }*/
    }
  }

  public static void assertEquals(String message, TopFieldDocs expected, 
                                  TopFieldDocs actual) {
    assertEquals(message + ". Expected length " + expected.scoreDocs.length
        + " got " + actual.scoreDocs.length,
        expected.scoreDocs.length, actual.scoreDocs.length);
    for (int i = 0 ; i < actual.scoreDocs.length ; i++) {
      ScoreDoc e = expected.scoreDocs[i];
      ScoreDoc a = actual.scoreDocs[i];
      String ef = ((FieldDoc)e).fields[0] == null ? ""
          : ((BytesRef)((FieldDoc)e).fields[0]).utf8ToString();
      String af = ((FieldDoc)a).fields[0] == null ? "" // TODO: Is "" == null?
          : ((BytesRef)((FieldDoc)a).fields[0]).utf8ToString();
      if (e.doc != a.doc) {
        System.out.println("The 10 first expected hits:");
        dumpResult(expected.scoreDocs);
        System.out.println("The 10 first actual hits:");
        dumpResult(actual.scoreDocs);
      }
      assertEquals(String.format(
          "%s. The docID for hit#%d/%d should be correct. " +
              "Expected %d with term '%s', got %d with term '%s'",
          message, i+1, expected.scoreDocs.length, e.doc, ef, a.doc, af),
          e.doc, a.doc);

      assertEquals(message + ". The sort value for hit#" + (i+1)
          + "/" + expected.scoreDocs.length + " should be "
          + ef + " but was " + af,
          ef, af);
    }
  }

  // Max 10
  private static void dumpResult(ScoreDoc[] scoreDocs) {
    for (int i = 0 ; i < scoreDocs.length && i < 10 ; i++) {
      ScoreDoc e = scoreDocs[i];
      String es = ((BytesRef)((FieldDoc)scoreDocs[i]).fields[0]).utf8ToString();
      System.out.println("Hit #" + i + ", doc=" + e.doc + ", term='" + es + "");
    }
  }

  private void dumpDocs(IndexReader reader, TopFieldDocs docs, String field)
                                                            throws IOException {
    final int MAX = 20;
    int num = Math.min(docs.scoreDocs.length, MAX);
    System.out.println("Dumping " + num +"/" + docs.scoreDocs.length + " hits");
    int count = 0;
    for (ScoreDoc doc: docs.scoreDocs) {
      if (count++ > num) {
        break;
      }
      String sortTerm = ((FieldDoc)doc).fields[0] == null ? "null"
          : ((BytesRef)((FieldDoc)doc).fields[0]).utf8ToString();
      System.out.println(
          doc.doc + " ID=" + reader.document(doc.doc).get(ExposedHelper.ID)
              + ", field " + field + "=" + reader.document(doc.doc).get(field) 
              + ", sort term=" + sortTerm);
    }
  }

  
}
