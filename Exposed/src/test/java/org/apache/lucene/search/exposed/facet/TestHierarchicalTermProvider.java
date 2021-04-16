package org.apache.lucene.search.exposed.facet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.search.exposed.ExposedCache;
import org.apache.lucene.search.exposed.ExposedHelper;
import org.apache.lucene.search.exposed.ExposedIOFactory;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.search.exposed.compare.NamedNaturalComparator;
import org.apache.lucene.util.IndexUtil;
import org.apache.lucene.util.packed.PackedInts;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

// TODO: Change this to LuceneTestCase but ensure Flex
public class TestHierarchicalTermProvider extends TestCase {
  public static final String HIERARCHICAL = "deep";

  @Override
  public void setUp() throws Exception {
    super.setUp();
//    CodecProvider.setDefaultCodec("Standard");
    deleteIndex();
  }
  public static void deleteIndex() {
    if (ExposedHelper.INDEX_LOCATION.exists()) {
      for (File file: ExposedHelper.INDEX_LOCATION.listFiles()) {
        file.delete();
      }
      ExposedHelper.INDEX_LOCATION.delete();
    }
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public static Test suite() {
    return new TestSuite(TestHierarchicalTermProvider.class);
  }

  public void testBasicTermBuildDump() throws IOException {
    createIndex(1000, 3, 4);
    IndexReader reader = ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    dumpField(reader, HIERARCHICAL, 10);
  }

  public void testSplit() {
    final String TAG = "A/B/C";
    final int EXPECTED_PARTS = 3;
    final String REGEXP = "/";
    Pattern pattern = Pattern.compile(REGEXP);
    assertEquals("The tag '" + TAG + "' should be split correctly by '" + REGEXP + "'",
                 EXPECTED_PARTS, pattern.split(TAG).length);
  }

  public void testAugmentationDump() throws IOException {
    final String REGEXP = "/";
    final int SIZE = 100;
    final int MAX_DUMP = 10;

    createIndex(SIZE, 3, 4);
    IndexReader reader =
        ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
    TermProvider basic = ExposedCache.getInstance().getProvider(
            reader, "myGroup", Arrays.asList(HIERARCHICAL), new NamedNaturalComparator());
    HierarchicalTermProvider augmented = new HierarchicalTermProvider(basic, REGEXP);
    PackedInts.Reader aOrder = augmented.getOrderedOrdinals();
    for (int i = 0 ; i < aOrder.size() && i < MAX_DUMP ; i++) {
      System.out.println("Tag #" + i
          + ": L=" + augmented.getLevel(i)
          + ", P=" + augmented.getPreviousMatchingLevel(i)
          + " " + augmented.getOrderedTerm(i).utf8ToString());
    }
  }

    public void testAugmentationBuildTime() throws IOException {
      final int RUNS = 6;
      final String REGEXP = "/";
      final int SPLIT_BYTE = '/';
      final int[] SIZES = new int[]{100, 10000, 100000};
      for (int size: SIZES) {
        deleteIndex();
        long refs = createIndex(size, 5, 5);
        IndexReader reader = ExposedIOFactory.getReader(ExposedHelper.INDEX_LOCATION);
        for (int i = 0 ; i < RUNS ; i++) {
          //ExposedSettings.debug = i >= RUNS-3;
          ExposedCache.getInstance().purgeAllCaches();
          long basicTime = -System.currentTimeMillis();
          TermProvider basic = ExposedCache.getInstance().getProvider(
              reader, "myGroup", Arrays.asList(HIERARCHICAL), new NamedNaturalComparator());
          assertNotNull("Basic should provide ordered ordinals", basic.getOrderedOrdinals());
          basicTime += System.currentTimeMillis();
          long buildTime = -System.currentTimeMillis();
  //        HierarchicalTermProvider augmented =
  //            new HierarchicalTermProvider(basic, REGEXP);

          // TODO: sortOrdinals(decorator) in GroupTermProvider is called here. The underlying provider were supposed to have already done that!?
          HierarchicalTermProvider augmented =
              i % 3 == 0 ?
              new HierarchicalTermProvider(basic, REGEXP) :
              new HierarchicalTermProvider(basic, SPLIT_BYTE, i % 3 == 1);
          buildTime += System.currentTimeMillis();
          System.out.println(
            (i % 3 == 1 ? "Decorating" : "Standard  ") + " tp build with " + (i % 3 == 0 ? "regexp" : "byte  ")
            + " split with a total of " + augmented.getOrderedOrdinals().size() + " tags " +
            "referred " + refs + " times: " + basicTime + " ms, augmented extra time: " + buildTime + " ms ("
            + (basicTime == 0 ? "N/A" : (buildTime / basicTime)) + " times standard)");
        }
      }
      deleteIndex();
    }

  private void dumpField(IndexReader reader, String field, int tags) throws IOException {
    System.out.println("Dumping a maximum of " + tags + " from field " + field);
    int count = 0;
    List<? extends IndexReader> readers = IndexUtil.flatten(reader);
    DocsEnum docsEnum = null;
    for (IndexReader inner: readers) {
      AtomicReader r = (AtomicReader)inner;
      Terms mTerms = r.terms(field);
      if (mTerms == null) {
        continue;
      }
      assertNotNull("There should be terms for field " + field, mTerms);
      TermsEnum terms = mTerms.iterator(null);
      while (terms.next() != null) {
        System.out.print(terms.term().utf8ToString() + ":");
        docsEnum = terms.docs(r.getLiveDocs(), docsEnum, 0);
        while (docsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS) {
          System.out.print(" " + docsEnum.docID());
        }
        System.out.println("");
        if (++count == tags) {
          return;
        }
      }
    }
  }

  public static final String TAGS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static long createIndex(int docCount, int maxTagsPerLevel, int maxLevel) throws IOException {
    long startTime = System.nanoTime();
    long references = 0;
    File location = ExposedHelper.INDEX_LOCATION;
    Random random = new Random(87);


    IndexWriter writer = ExposedIOFactory.getWriter(location);

    int every = docCount > 100 ? docCount / 100 : 1;

    int next = every;
    for (int docID = 0 ; docID < docCount ; docID++) {
      if (docID == next) {
        System.out.print(".");
        next += every;
      }
      Document doc = new Document();

      int levels = random.nextInt(maxLevel+1);
      references += addHierarchicalTags(doc, docID, random, levels, "", maxTagsPerLevel, false);

      doc.add(new Field(ExposedHelper.ID, ExposedHelper.ID_FORMAT.format(docID),
          Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field(ExposedHelper.EVEN, docID % 2 == 0 ? "true" : "false",
          Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field(ExposedHelper.ALL, ExposedHelper.ALL,
          Field.Store.YES, Field.Index.NOT_ANALYZED));
      writer.addDocument(doc);
    }
    writer.close();
    System.out.println("");
    System.out.println(String.format(
            Locale.ROOT, "Created %d document index with %d tag references, maxTagsPerlevel=%d, "
                         + "maxLevel=%d in %sms at %s",
            docCount, references, maxTagsPerLevel, maxLevel,
        (System.nanoTime() - startTime) / 1000000, location.getAbsolutePath()));
    return references;
  }

  private static long addHierarchicalTags(
      Document doc, int docID, Random random, int levelsLeft, String prefix, int maxTagsPerLevel, boolean addDocID) {
    long references = 0;
    if (levelsLeft == 0) {
      return references;
    }
    int tags = random.nextInt(maxTagsPerLevel+1);
    if (tags == 0) {
      tags = 1;
      levelsLeft = 1;
    }
    String docIDAdder = addDocID ? "_" + docID : "";
    for (int i = 0 ; i < tags ; i++) {
      String tag = levelsLeft == 1 ?
          TAGS.charAt(random.nextInt(TAGS.length())) + docIDAdder :
          "" + TAGS.charAt(random.nextInt(TAGS.length()));
      if (levelsLeft == 1 || random.nextInt(10) == 0) {
        doc.add(new Field(HIERARCHICAL, prefix + tag, Field.Store.NO, Field.Index.NOT_ANALYZED));
        references++;
      }
      references += addHierarchicalTags(
              doc, docID, random, levelsLeft-1, prefix + tag + "/", maxTagsPerLevel, addDocID);
    }
    return references;
  }

}
