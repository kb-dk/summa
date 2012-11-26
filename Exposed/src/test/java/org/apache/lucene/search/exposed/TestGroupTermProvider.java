package org.apache.lucene.search.exposed;

import com.ibm.icu.text.Collator;
import junit.framework.TestCase;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.exposed.compare.NamedCollatorComparator;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.search.exposed.compare.NamedNaturalComparator;
import org.apache.lucene.search.exposed.facet.FacetMap;
import org.apache.lucene.util.BytesRef;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.*;

// TODO: Change this to LuceneTestCase but ensure Flex
public class TestGroupTermProvider extends TestCase {
  public static final int DOCCOUNT = 10;
  private ExposedHelper helper;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    helper = new ExposedHelper();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    helper.close();
  }

  public void testIndexGeneration() throws Exception {
    helper.createIndex( DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    long termCount = 0;
    TermsEnum terms = MultiFields.getFields(reader).
        terms(ExposedHelper.ID).iterator(null);
    while (terms.next() != null) {
      assertEquals("The ID-term #" + termCount + " should be correct",
          ExposedHelper.ID_FORMAT.format(termCount),
          terms.term().utf8ToString());
      termCount++;
    }

    assertEquals("There should be the right number of terms",
            DOCCOUNT, termCount);
    reader.close();
  }

  public void testOrdinalAccess() throws IOException {
    String[] fieldNames = new String[]{"a"};
    helper.createIndex( DOCCOUNT, Arrays.asList(fieldNames), 20, 2);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);

    ArrayList<String> plainExtraction = new ArrayList<String>(DOCCOUNT);
    for (String fieldName: fieldNames) {
      // FIXME: The ordinals are not comparable to the exposed ones!
      TermsEnum terms =
          MultiFields.getFields(reader).terms(fieldName).iterator(null);
      while (terms.next() != null) {
        plainExtraction.add(terms.term().utf8ToString());
      }
    }

    TermProvider groupProvider = ExposedFactory.createProvider(
        reader, "TestGroup", Arrays.asList(fieldNames),
        new NamedNaturalComparator());

    ArrayList<String> exposedOrdinals = new ArrayList<String>(DOCCOUNT);
    for (int i = 0 ; i < groupProvider.getOrdinalTermCount() ; i++) {
      exposedOrdinals.add(groupProvider.getTerm(i).utf8ToString());
    }

    assertEquals("The two lists of terms should be of equal length",
        plainExtraction.size(), exposedOrdinals.size());
    // We sort as the order of the terms is not significant here
    Collections.sort(plainExtraction);
    Collections.sort(exposedOrdinals);
    for (int i = 0 ; i < plainExtraction.size() ; i++) {
      assertEquals("The term at index " + i + " should be correct",
          plainExtraction.get(i), exposedOrdinals.get(i));
    }
    reader.close();

  }
  public void testTermSortAllDefined() throws IOException {
    helper.createIndex( DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    IndexReader reader =
      ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    testTermSort(reader, Arrays.asList("a"));
    reader.close();
  }

  public void testBuildPerformance() throws IOException {
    final String FIELD = "a";
    final int DOCS = 1000;
    ExposedSettings.debug = true;

    helper.createIndex(DOCCOUNT, Arrays.asList(FIELD), DOCS, 2);
    IndexReader reader = ExposedIOFactory.getReader(
        ExposedHelper.INDEX_LOCATION);

    Collator sorter = Collator.getInstance(new Locale("da"));
    long extractTime = -System.currentTimeMillis();
    TermProvider groupProvider = ExposedFactory.createProvider(
        reader, "a-group", Arrays.asList(FIELD),
        new NamedCollatorComparator(sorter));
    extractTime += System.currentTimeMillis();

    System.out.println("Extracted and sorted " + 20 + " terms in "
                       + extractTime/1000 + " seconds");


    reader.close();
  }

  public void testTermSortAllScarce() throws IOException {
    helper.createIndex( DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    testTermSort(reader, Arrays.asList("even"));
    reader.close();
  }

  private void testTermSort(IndexReader index, List<String> fieldNames)
                                                            throws IOException {
    Collator sorter = Collator.getInstance(new Locale("da"));

    ArrayList<String> plainExtraction = new ArrayList<String>(DOCCOUNT);
    // TODO: Make this handle multiple field names
    TermsEnum terms =
        MultiFields.getFields(index).terms(fieldNames.get(0)).iterator(null);
    while (terms.next() != null) {
      String next = terms.term().utf8ToString();
      plainExtraction.add(next);
//      System.out.println("Default order term #"
//          + (plainExtraction.size() - 1) + ": " + next);
    }
    Collections.sort(plainExtraction, sorter);

    TermProvider groupProvider = ExposedFactory.createProvider(
        index, "a-group", fieldNames,
        new NamedCollatorComparator(sorter));
    
    ArrayList<String> exposedExtraction = new ArrayList<String>(DOCCOUNT);
    Iterator<ExposedTuple> ei = groupProvider.getIterator(false);
    int count = 0;
    while (ei.hasNext()) {
      String next = ei.next().term.utf8ToString();
      exposedExtraction.add(next);
//      System.out.println("Exposed sorted term #" + count++ + ": " + next);
    }

    assertEquals("The two lists of terms should be of equal length",
        plainExtraction.size(), exposedExtraction.size());
    for (int i = 0 ; i < plainExtraction.size() ; i++) {
      assertEquals("The term at index " + i + " should be correct",
          plainExtraction.get(i), exposedExtraction.get(i));
    }
  }

  public void testDocIDMapping() throws IOException {
    helper.createIndex( DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    Collator sorter = Collator.getInstance(new Locale("da"));

    ArrayList<ExposedHelper.Pair> plain =
        new ArrayList<ExposedHelper.Pair>(DOCCOUNT);
    for (int docID = 0 ; docID < reader.maxDoc() ; docID++) {
      plain.add(new ExposedHelper.Pair(
          docID, reader.document(docID).get("a"), sorter));
//      System.out.println("Plain access added " + plain.get(plain.size()-1));
    }
    Collections.sort(plain);

    TermProvider index = ExposedFactory.createProvider(
      reader, "docIDGroup", Arrays.asList("a"),
      new NamedCollatorComparator(sorter));

    ArrayList<ExposedHelper.Pair> exposed =
        new ArrayList<ExposedHelper.Pair>(DOCCOUNT);
    Iterator<ExposedTuple> ei = index.getIterator(true);

    while (ei.hasNext()) {
      final ExposedTuple tuple = ei.next();
      if (tuple.docIDs != null) {
        int doc;
        // TODO: Test if bulk reading (which includes freqs) is faster
        while ((doc = tuple.docIDs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
          exposed.add(new ExposedHelper.Pair(
              doc + tuple.docIDBase, tuple.term.utf8ToString(), sorter));
        }
      }
    }
    Collections.sort(exposed);

    assertEquals("The two docID->term maps should be of equal length",
        plain.size(), exposed.size());
    for (int i = 0 ; i < plain.size() ; i++) {
//      System.out.println("Sorted docID, term #" + i + ". Plain=" + plain.get(i)
//          + ", Exposed=" + exposed.get(i));
      assertEquals("Mapping #" + i + " should be equal",
          plain.get(i), exposed.get(i));
    }
    reader.close();
  }

  public void testMultiField() throws IOException {
    final int DOCCOUNT = 5;
    ExposedHelper helper = new ExposedHelper();
    File location = helper.buildMultiFieldIndex(DOCCOUNT);

    IndexReader reader = ExposedIOFactory.getReader(location);
    Collator sorter = Collator.getInstance(new Locale("da"));

    TermProvider index =
        ExposedFactory.createProvider(
            reader, "multiGroup", Arrays.asList("a", "b"),
            new NamedCollatorComparator(sorter));

    ArrayList<ExposedHelper.Pair> exposed =
        new ArrayList<ExposedHelper.Pair>(DOCCOUNT);
    Iterator<ExposedTuple> ei = index.getIterator(true);

    long maxID = 0;
    while (ei.hasNext()) {
      final ExposedTuple tuple = ei.next();
      if (tuple.docIDs != null) {
        int doc;
        // TODO: Test if bulk reading (which includes freqs) is faster
        while ((doc = tuple.docIDs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
          exposed.add(new ExposedHelper.Pair(
              doc + tuple.docIDBase, tuple.term.utf8ToString(), sorter));
          maxID = Math.max(maxID, doc + tuple.docIDBase);
        }
      }
    }
    Collections.sort(exposed);

    for (int i = 0 ; i < exposed.size() && i < 10 ; i++) {
      System.out.println("Sorted docID, term #" + i + ". Exposed="
          + exposed.get(i));
    }
    reader.close();
    assertEquals("The maximum docID in tuples should be equal to the maximum " +
        "from the reader", reader.maxDoc() - 1, maxID);
  }

  public void testTermCount() throws IOException {
    helper.createIndex(100, Arrays.asList("a"), 20, 2);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    NamedComparator comp = new NamedNaturalComparator();
    ExposedRequest.Field fieldRequest = new ExposedRequest.Field(
        ExposedHelper.MULTI, comp);
    ExposedRequest.Group groupRequest = new ExposedRequest.Group(
      "TestGroup", Arrays.asList(fieldRequest), comp);
    TermProvider provider = ExposedCache.getInstance().getProvider(
        reader, groupRequest);
    
    assertEquals("The number of unique terms for multi should match",
        25, provider.getUniqueTermCount());
  }

  public void testVerifyCorrectCountWithMap()
                        throws IOException, XMLStreamException, ParseException {
    String[] FIELDS = new String[]{"recordBase", "lsubject"};
    IndexWriter w = ExposedHelper.getWriter();
    ExposedHelper.addDocument(w,
        ExposedHelper.ID + ":1",
        ExposedHelper.ALL + ":" + ExposedHelper.ALL,
        "recordBase:foo",
        "lsubject:bar",
        "lsubject:zoo"
    );
    w.commit();
    w.close();
    verifyGroupElements("Empty term present", FIELDS, 3);
    verifyMapElements("All terms with content", FIELDS, 3);
  }

  public void testMiscountEmptyWithMap()
                        throws IOException, XMLStreamException, ParseException {
    String[] FIELDS = new String[]{"recordBase", "lsubject"};
    IndexWriter w = ExposedHelper.getWriter();
    ExposedHelper.addDocument(w,
        ExposedHelper.ID + ":1",
        ExposedHelper.ALL + ":" + ExposedHelper.ALL,
        "recordBase:foo",
        "lsubject:bar",
        "lsubject:"
    );
    w.commit();
    w.close();
    verifyGroupElements("Empty term present", FIELDS, 3);
    verifyMapElements("Empty term present", FIELDS, 3);
  }

  public void testMiscountEmptyGroupProvider()
                        throws IOException, XMLStreamException, ParseException {
    String[] FIELDS = new String[]{"lsubject"};
    IndexWriter w = ExposedHelper.getWriter();
    ExposedHelper.addDocument(w,
        ExposedHelper.ID + ":1",
        ExposedHelper.ALL + ":" + ExposedHelper.ALL,
        "lsubject:"
    );
    w.commit();
    w.close();
    verifyGroupElements("Single group with empty term", FIELDS, 1);
  }

  private void verifyGroupElements(
      String message, String[] fields, int termCount) throws IOException {
    IndexReader reader = ExposedIOFactory.getReader(
        ExposedHelper.INDEX_LOCATION);
    List<TermProvider> providers = createGroupProviders(
        message, fields, reader);
    long uniques = 0;
    for (TermProvider provider: providers) {
      uniques += provider.getUniqueTermCount();
    }
    assertEquals(message + ". The unique count should be as expected",
        termCount, uniques);
    reader.close();
  }

  private void verifyMapElements(String message, String[] fields, int termCount)
                                                            throws IOException {
    IndexReader reader = ExposedIOFactory.getReader(
        ExposedHelper.INDEX_LOCATION);

    List<TermProvider> providers = createGroupProviders(message, fields, reader);
    FacetMap map = new FacetMap(1, providers);
    assertEquals(message + ". There should be the correct number of terms for " +
        "the single document in the map",
        termCount, map.getTermsForDocID(0).length);
    assertEquals(message + ". There should be the correct number of indirects",
        termCount, map.getIndirectStarts()[map.getIndirectStarts().length-1]);
    BytesRef last = null;
    for (int i = 0 ; i < 3 ; i++) {
      if (i != 0) {
        assertFalse(message + ". The term '" + last.utf8ToString() + "' at " +
            "order index #" + i + " should not be equal to the previous term",
            last.utf8ToString().equals(map.getOrderedTerm(i).utf8ToString()));
      }
      last = map.getOrderedTerm(i);
    }
    reader.close();
  }

  private List<TermProvider> createGroupProviders(
      String message, String[] fields, IndexReader reader) throws IOException {
    List<TermProvider> providers = new ArrayList<TermProvider>(fields.length);
    NamedComparator comp = new NamedNaturalComparator();
    for (String field: fields) {
      List<ExposedRequest.Field> fieldRequests =
          new ArrayList<ExposedRequest.Field>(fields.length);
      fieldRequests.add(new ExposedRequest.Field(field, comp));
      ExposedRequest.Group groupRequest = new ExposedRequest.Group(
        "TestGroup", fieldRequests, comp);
      TermProvider provider = ExposedCache.getInstance().getProvider(
          reader, groupRequest);
      assertTrue(message + ". The provider for " + field + " should be a " +
          "GroupTermProvider but was a " + provider.getClass().getSimpleName(),
          provider instanceof GroupTermProvider);
      providers.add(provider);
    }
    return providers;
  }
}