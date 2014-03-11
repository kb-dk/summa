package org.apache.lucene.search.exposed;

import com.ibm.icu.text.Collator;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ExposedHelper {
  static final char[] CHARS = // Used for random content
      ("abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ1234567890      ").toCharArray();
  public static final File INDEX_LOCATION = new File("tmp/testfieldtermprovider");

  // Fields in the test index
  public static final String ID = "id";     // doc #0 = "0", doc #1 = "1" etc
  public static final String EVEN = "even"; // "true" or "false"
  public static final String ALL = "all"; // all == "all"
  public static final String EVEN_NULL = "evennull"; // odd = random content
  public static final String MULTI = "facet"; // 0-5 of values A to Z

  public static final DecimalFormat ID_FORMAT = new DecimalFormat("00000000");

  public ExposedHelper() {
    //deleteIndex();
    INDEX_LOCATION.mkdirs();
  }

  public void close() {
    deleteIndex();
  }
  public static void deleteIndex() {
    if (INDEX_LOCATION.exists()) {
      for (File file: INDEX_LOCATION.listFiles()) {
        file.delete();
      }
      INDEX_LOCATION.delete();
    }
  }

  // MB
  public static long getMem() {
    System.gc();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    System.gc();
    return (Runtime.getRuntime().totalMemory()
        - Runtime.getRuntime().freeMemory()) / 1048576;
  }

  public static String getTime(long ms) {
    if (ms < 2999) {
      return ms + " ms";
    }
    long seconds = Math.round(ms / 1000.0);
    long minutes = seconds / 60;
    return String.format("%d:%02d minutes", minutes, seconds - (minutes * 60));
  }

  public static class Pair implements Comparable<Pair> {
    public long docID;
    public String term;
    public Collator comparator;

    public Pair(long docID, String term, Collator comparator) {
      this.docID = docID;
      this.term = term;
      this.comparator = comparator;
    }

    @Override
    public int compareTo(Pair o) {
      return comparator.compare(term, o.term);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof Pair)) {
        return false;
      }
      Pair other = (Pair)obj;
      return term.equals(other.term) && docID == other.docID;
    }

    @Override
    public String toString() {
      return "Pair(" + docID + ", " + term + ")";
    }
  }

  public static IndexWriter getWriter() throws IOException {
    return getWriter(INDEX_LOCATION);
  }

  public static IndexWriter getWriter(File location) throws IOException {
    return ExposedIOFactory.getWriter(location);
  }

  public static IndexReader getReader() throws IOException {
    return ExposedIOFactory.getReader(INDEX_LOCATION);
  }

  public static IndexSearcher getSearcher() throws IOException {
    return new IndexSearcher(getReader());
  }

  public static void addDocument(
      IndexWriter writer, String... values) throws IOException {
    Document doc = new Document();
    for (String value: values) {
      String[] tokens = value.split(":", 2);
      if (tokens.length == 1) {
        doc.add(new Field(tokens[0], "",
            Field.Store.NO, Field.Index.NOT_ANALYZED));
      } else if (tokens.length == 2) {
        doc.add(new Field(tokens[0], tokens[1],
            Field.Store.NO, Field.Index.NOT_ANALYZED));
      } else {
          throw new IllegalArgumentException("Could not split '" + value + "'");
      }
    }
    writer.addDocument(doc);
  }

  public static void addAnalyzedDocument(
      IndexWriter writer, String... values) throws IOException {
    Document doc = new Document();
    for (String value: values) {
      String[] tokens = value.split(":");
      if (tokens.length != 2) {
        throw new IllegalArgumentException("Could not split '" + value + "'");
      }
      doc.add(new Field(tokens[0], tokens[1],
          Field.Store.NO, Field.Index.ANALYZED));
    }
    writer.addDocument(doc);
  }

  public void createIndex(
      int docCount, List<String> fields, int fieldContentLength,
      int minSegments) throws IOException {
    createIndex(docCount, fields, 1, fieldContentLength, 0, 6, minSegments);
  }

  public void optimize() throws IOException {
    optimize(INDEX_LOCATION);
  }
  public void optimize(File location) throws IOException {
    IndexWriter writer = ExposedIOFactory.getWriter(location);
    writer.forceMerge(1, true);
    writer.close(true);
  }

  public void createIndex(int docCount, List<String> fields,  int fieldFactor,
                          int fieldContentLength, int minFacets, int maxFacets, int minSegments) throws IOException {
    createIndex(INDEX_LOCATION, docCount, fields, fieldFactor, fieldContentLength, minFacets, maxFacets, minSegments);
  }
  public void createIndex(File location, int docCount, List<String> fields,  int fieldFactor,
                          int fieldContentLength, int minFacets, int maxFacets, int minSegments) throws IOException {
    long startTime = System.nanoTime();
    Random random = new Random(87);
    int every = Math.max(1, docCount / 100);

    for (int i = 0 ; i < 100 ; i++) {
      System.out.print("-");
    }
    System.out.println("");

    IndexWriter writer = getWriter(location);
    for (int docID = 0 ; docID < docCount ; docID++) {
      Document doc = new Document();
      for (String field: fields) {
        for (int f = 0 ; f < fieldFactor ; f++) {
          doc.add(new StringField(
              field,
              getRandomString(random, CHARS, 1, fieldContentLength) + docID + field,
              Field.Store.YES));
        }
      }
      doc.add(new StringField(ID, ID_FORMAT.format(docID), Field.Store.YES));
      doc.add(new StringField(EVEN, docID % 2 == 0 ? "true" : "false", Field.Store.YES));
      int facets = random.nextInt(maxFacets-minFacets+1) + minFacets;
      for (int i = 0 ; i < facets ; i++) {
        doc.add(new StringField(MULTI, Character.toString((char)(random.nextInt(25) + 'A')), Field.Store.NO));
      }
      if (docID % 2 == 1) {
        doc.add(new StringField(
            EVEN_NULL, getRandomString(random, CHARS, 1, fieldContentLength) + docID, Field.Store.YES));
      }
      doc.add(new StringField(ALL, ALL, Field.Store.YES));
      writer.addDocument(doc);
      if (docID == docCount / minSegments) {
        writer.commit(); // Ensure minSegments
      }
      if (docID % every == 0) {
        System.out.print(".");
      }
    }
    System.out.print("Closing");
//    writer.optimize();
    writer.commit();
    writer.close();
    System.out.println("");
    System.out.println(String.format(
        "Created %d document index with %d fields with average " +
            "term length %d and total size %s in %sms at %s",
        docCount, fields.size() + 2, fieldContentLength / 2,
        readableSize(calculateSize(location)),
        (System.nanoTime() - startTime) / 1000000, location.getAbsolutePath()));
  }

  /*
  Constructs a simple index with fields a, b and c. Meant for testing groups
  with multiple fields.
   */
  public File buildMultiFieldIndex(int docs) throws IOException {
    IndexWriter w = getWriter();
    for (int docID = 0 ; docID < docs ; docID++) {
      addDocument(w, 
          ID + ":1",
          ALL + ":" + ALL,
          "a:a" + docID,
          "b:b" + docID / 2,
          "c:c" + docID % 2);
    }
    w.close(true);
    return INDEX_LOCATION;
  }

  public File buildSpecificIndex(String[][] documents)
      throws IOException {
    IndexWriter w = getWriter();
    int docID = 0;
    for (String[] document: documents) {
      List<String> fields = new ArrayList<String>(document.length+1);
      fields.add(ID + ":" + docID);
      fields.add(ALL + ":" + ALL);
      fields.addAll(Arrays.asList(document));
      String[] f = new String[fields.size()];
      fields.toArray(f);
      addDocument(w, f);
    }
    w.close(true);
    return INDEX_LOCATION;
  }

  /*
  Re-creates the test case from https://issues.apache.org/jira/browse/SOLR-475?focusedCommentId=12650071&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_12650071
   */
  public void createFacetIndex(final int docCount) throws IOException {
    long startTime = System.nanoTime();
    File location = INDEX_LOCATION;
    Random random = new Random(87);

    IndexWriter writer = getWriter();
    writer.getConfig().setRAMBufferSizeMB(32.0);
    final int feedback = docCount / 100;
    for (int docID = 0 ; docID < docCount ; docID++) {
      Document doc = new Document();
      addTagsToDocument(doc, random, 10, 100);
      addTagsToDocument(doc, random, 100, 10);
      addTagsToDocument(doc, random, 1000, 5);
      addTagsToDocument(doc, random, 10000, 5);
      addTagsToDocument(doc, random, 100000, 5);
      addTagsToDocument(doc, random, 100000, 10);
      addTagsToDocument(doc, random, 1000000, 5);  // Extra
      addTagsToDocument(doc, random, 10000000, 1); // Extra
      doc.add(new Field(ALL, ALL, Field.Store.YES, Field.Index.NOT_ANALYZED));
      for (int hits: new int[]{1000, 10000, 100000, 1000000, 10000000}) {
        if (docCount > hits && docID % (docCount / hits) == 0) {
          doc.add(new Field("hits" + hits, "true",
              Field.Store.NO, Field.Index.NOT_ANALYZED));
        }
      }
      writer.addDocument(doc);
      if (docID % feedback == 0) {
        System.out.print(".");
      }
    }
//    writer.optimize();
    writer.close();
    System.out.println(String.format(
        "\nCreated %d document index with total size %s in %sms at %s",
        docCount, readableSize(calculateSize(location)),
        (System.nanoTime() - startTime) / 1000000, location.getAbsolutePath()));
  }

  private void addTagsToDocument(
      Document doc, Random random, int unique, int occurrences) {
    final int num = random.nextInt(occurrences+1);
    for (int i = 0 ; i < num ; i++) {
      Field field = new Field("f" + unique + "_" + occurrences + "_t", 
          "t" + random.nextInt(unique+1),
          Field.Store.NO, Field.Index.NOT_ANALYZED);
      doc.add(field);
    }
  }

  private static StringBuffer buffer = new StringBuffer(100);
  static synchronized String getRandomString(
      Random random, char[] chars, int minLength, int maxLength) {
    int length = minLength == maxLength ? minLength :
        random.nextInt(maxLength-minLength+1) + minLength;
    buffer.setLength(0);
    for (int i = 0 ; i < length ; i++) {
      buffer.append(chars[random.nextInt(chars.length)]);
    }
    return buffer.toString();
  }

  private String readableSize(long size) {
    return size > 2 * 1048576 ?
        size / 1048576 + "MB" :
        size > 2 * 1024 ?
            size / 1024 + "KB" :
            size + "bytes";
  }

  private long calculateSize(File file) {
    long size = 0;
    if (file.isDirectory()) {
      for (File sub: file.listFiles()) {
        size += calculateSize(sub);
      }
    } else {
      size += file.length();
    }
    return size;
  }

}
