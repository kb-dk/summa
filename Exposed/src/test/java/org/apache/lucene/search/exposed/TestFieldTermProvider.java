package org.apache.lucene.search.exposed;

import junit.framework.TestCase;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.exposed.facet.FacetMap;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import com.ibm.icu.text.Collator;

import javax.xml.stream.XMLStreamException;
import java.util.*;

// TODO: Change this to LuceneTestCase but ensure Flex
public class TestFieldTermProvider extends TestCase {
//          new File(System.getProperty("java.io.tmpdir"), "exposed_index");
//          new File("/home/te/projects/lucene/exposed_index");
//      new File("/mnt/bulk/exposed_index");
  public static final int DOCCOUNT = 100;
  private ExposedHelper helper;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // TODO: Figure out how to force Flex the clean way
//    CodecProvider.setDefaultCodec("Standard");
    helper = new ExposedHelper();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    helper.close();
  }

  public void testCreateIndex() throws IOException {
    helper.createIndex(DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
  }

  public void testSegmentcount() throws IOException {
    helper.createIndex(DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    int subCount = reader instanceof AtomicReader ? 1 : ((CompositeReader)reader).getSequentialSubReaders().length;
    assertTrue("The number of segments should be >= 2 but was " + subCount,
        subCount >= 2);
    reader.close();
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
    helper.createIndex( DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    AtomicReader segment =
        ExposedIOFactory.getAtomicReader(ExposedHelper.INDEX_LOCATION);

    ArrayList<String> plainExtraction = new ArrayList<String>(DOCCOUNT);
    TermsEnum terms = segment.fields().terms("a").iterator(null);
    while (terms.next() != null) {
      plainExtraction.add(terms.term().utf8ToString());
    }

    TermProvider segmentProvider = ExposedFactory.createProvider(
        segment, null, Arrays.asList("a"), null, false, "null");
    ArrayList<String> exposedOrdinals = new ArrayList<String>(DOCCOUNT);
    for (int i = 0 ; i < segmentProvider.getOrdinalTermCount() ; i++) {
      exposedOrdinals.add(segmentProvider.getTerm(i).utf8ToString());
    }

    assertEquals("The two lists of terms should be of equal length",
        plainExtraction.size(), exposedOrdinals.size());
    for (int i = 0 ; i < plainExtraction.size() ; i++) {
      assertEquals("The term at index " + i + " should be correct",
          plainExtraction.get(i), exposedOrdinals.get(i));
    }
  }

  public void testSegmentTermSort() throws IOException {
    helper.createIndex( DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    AtomicReader segment =
        ExposedIOFactory.getAtomicReader(ExposedHelper.INDEX_LOCATION);
    testSegmentTermSort(segment);
  }

  private void testSegmentTermSort(AtomicReader segment) throws IOException {
    Collator sorter = Collator.getInstance(new Locale("da"));

    ArrayList<String> plainExtraction = new ArrayList<String>(DOCCOUNT);
    TermsEnum terms = segment.fields().terms("a").iterator(null);
    while (terms.next() != null) {
      plainExtraction.add(terms.term().utf8ToString());
      System.out.println("Unsorted term #" + (plainExtraction.size() - 1) + ": "
          + terms.term().utf8ToString());
    }
    Collections.sort(plainExtraction, sorter);

    ExposedRequest.Field request = new ExposedRequest.Field(
        "a", ExposedComparators.collatorToBytesRef(sorter),
        false, "collator_da");
    FieldTermProvider segmentProvider =
        new FieldTermProvider(segment, 0, request, true);

    ArrayList<String> exposedExtraction = new ArrayList<String>(DOCCOUNT);
    Iterator<ExposedTuple> ei = segmentProvider.getIterator(false);
    int count = 0;
    while (ei.hasNext()) {
      exposedExtraction.add(ei.next().term.utf8ToString());
      System.out.println("Exposed sorted term #" + count++ + ": "
          + exposedExtraction.get(exposedExtraction.size()-1));
    }

    assertEquals("The two lists of terms should be of equal length",
        plainExtraction.size(), exposedExtraction.size());
    for (int i = 0 ; i < plainExtraction.size() ; i++) {
      assertEquals("The term at index " + i + " should be correct",
          plainExtraction.get(i), exposedExtraction.get(i));
    }
  }

  public void testTermEnum() throws IOException {
    ExposedIOFactory.forceFixedCodec = false;

    helper.createIndex( DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
      AtomicReader segment =
          ExposedIOFactory.getAtomicReader(ExposedHelper.INDEX_LOCATION);
    Collator sorter = Collator.getInstance(new Locale("da"));

    ExposedRequest.Field request = new ExposedRequest.Field(
        "a", ExposedComparators.collatorToBytesRef(sorter),
        false, "collator_da");
    FieldTermProvider segmentProvider =
        new FieldTermProvider(segment, 0, request, true);
    ArrayList<ExposedHelper.Pair> exposed =
        new ArrayList<ExposedHelper.Pair>(DOCCOUNT);
    Iterator<ExposedTuple> ei = segmentProvider.getIterator(true);
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
  }

  public void testDocIDMapping() throws IOException {
    helper.createIndex( DOCCOUNT, Arrays.asList("a", "b"), 20, 2);
    AtomicReader segment =
        ExposedIOFactory.getAtomicReader(ExposedHelper.INDEX_LOCATION);
    Collator sorter = Collator.getInstance(new Locale("da"));

    ArrayList<ExposedHelper.Pair> plain =
        new ArrayList<ExposedHelper.Pair>(DOCCOUNT);
    for (int docID = 0 ; docID < segment.maxDoc() ; docID++) {
      plain.add(new ExposedHelper.Pair(
          docID, segment.document(docID).get("a"), sorter));
//      System.out.println("Plain access added " + plain.get(plain.size()-1));
    }
    Collections.sort(plain);

    ExposedRequest.Field request = new ExposedRequest.Field(
        "a", ExposedComparators.collatorToBytesRef(sorter),
        false, "collator_da");
    FieldTermProvider segmentProvider =
        new FieldTermProvider(segment, 0, request, true);

    ArrayList<ExposedHelper.Pair> exposed =
        new ArrayList<ExposedHelper.Pair>(DOCCOUNT);
    Iterator<ExposedTuple> ei = segmentProvider.getIterator(true);

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
      assertEquals("Mapping #" + i + " should be equal but was "
          + plain.get(i) + " vs. " + exposed.get(i),
          plain.get(i), exposed.get(i));
      System.out.println("Sorted docID, term #" + i + ": " + plain.get(i));
    }
  }

  // Just tests for non-crashing
  public void testDocIDMultiMapping() throws IOException {
    helper.createIndex(100, Arrays.asList("a"), 20, 2);
      AtomicReader segment =
          ExposedIOFactory.getAtomicReader(ExposedHelper.INDEX_LOCATION);
    Collator sorter = Collator.getInstance(new Locale("da"));

    ExposedRequest.Field request = new ExposedRequest.Field(
        ExposedHelper.MULTI, ExposedComparators.collatorToBytesRef(sorter),
        false, "collator_da");
    FieldTermProvider segmentProvider =
        new FieldTermProvider(segment, 0, request, true);

    ArrayList<ExposedHelper.Pair> exposed =
        new ArrayList<ExposedHelper.Pair>(DOCCOUNT);
    Iterator<ExposedTuple> ei = segmentProvider.getIterator(true);

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

/*    for (int i = 0 ; i < exposed.size() ; i++) {
      System.out.println("Sorted docID, term #" + i + ": " + exposed.get(i));
    }*/
  }

  public void testMiscountEmpty()
                        throws IOException, XMLStreamException, ParseException {
    String[] FIELDS = new String[]{"recordBase", "lsubject"};
    {
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
    }

    AtomicReader segment =
        ExposedIOFactory.getAtomicReader(ExposedHelper.INDEX_LOCATION);

    for (String field: FIELDS) {
      TermProvider segmentProvider = ExposedFactory.createProvider(
          segment, null, Arrays.asList(field), null, false, "null");
      Iterator<ExposedTuple> it = segmentProvider.getIterator(true);
      while (it.hasNext()) {
        ExposedTuple tuple = it.next();
        assertTrue("The ordinal should be >= 0 for " + tuple,
            tuple.ordinal >= 0);
      }
    }
  }

  public void testMiscountEmptyWithMap()
                        throws IOException, XMLStreamException, ParseException {
    {
      String[] FIELDS = new String[]{"recordBase", "lsubject"};
      int TERMS = 3;
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
      verifyElements("All terms with content", FIELDS, TERMS);
      helper.deleteIndex();
    }

    {
      String[] FIELDS = new String[]{"recordBase", "lsubject"};
      int TERMS = 3;
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
      verifyElements("Empty term", FIELDS, TERMS);
      helper.deleteIndex();
    }
  }

  private void verifyElements(String message, String[] fields, int termCount)
                                                            throws IOException {
    AtomicReader segment =
        ExposedIOFactory.getAtomicReader(ExposedHelper.INDEX_LOCATION);

    List<TermProvider> providers = new ArrayList<TermProvider>(fields.length);
    for (String field: fields) {
      providers.add(ExposedFactory.createProvider(
          segment, null, Arrays.asList(field), null, false, "null"));
    }
    FacetMap map = new FacetMap(1, providers);
    assertEquals(message + ". There should be the correct number of terms for " +
        "the single document in the map",
        termCount, map.getTermsForDocID(0).length);
    BytesRef last = null;
    for (BytesRef term: map.getTermsForDocID(0)) {
      System.out.println("Term: " + term.utf8ToString());
    }
    for (int i = 0 ; i < termCount; i++) {
      if (i != 0) {
        assertFalse(message + ". The term '" + last.utf8ToString()
            + "' at order index # should not be equal to the previous term",
            last.utf8ToString().equals(map.getOrderedTerm(i).utf8ToString()));
      }
      last = map.getOrderedTerm(i);
    }
  }
}
