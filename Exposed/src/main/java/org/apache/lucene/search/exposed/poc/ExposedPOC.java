package org.apache.lucene.search.exposed.poc;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.exposed.ExposedFieldComparatorSource;
import org.apache.lucene.search.exposed.ExposedIOFactory;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.lucene.util.packed.PackedInts;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public class ExposedPOC {
  private static final double MAX_HITS = 20;

  public static void main(String[] args)
      throws IOException, InterruptedException, ParseException {
    if (args.length == 2 && "optimize".equals(args[0])) {
      optimize(new File(args[1]));
      return;
    }
    if (args.length != 6) {
      System.err.println("Need 6 arguments, got " + args.length + "\n");
      usage();
      return;
    }
    String method = args[0];
    File location = new File(args[1]);
    String field = args[2];
    Locale locale =
        "null".equalsIgnoreCase(args[3]) ? null : new Locale(args[3]);
    String defaultField = args[4];
    ExposedSettings.PRIORITY priority =
        ExposedSettings.PRIORITY.fromString(args[5]);
    try {
      shell(method, location, field, locale, defaultField, priority);
    } catch (Exception e) {
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
      usage();
    }
  }

  private static void optimize(File location) throws IOException {
    System.out.println("Optimizing " + location + "...");
    long startTimeOptimize = System.nanoTime();
    IndexWriter writer = ExposedIOFactory.getWriter(location);
    writer.forceMerge(1);
    System.out.println("Optimized index in " + nsToString(
        System.nanoTime() - startTimeOptimize));
    writer.close(true);
  }

  private static void shell(
      String method, File location, String field, Locale locale,
      String defaultField, ExposedSettings.PRIORITY priority)
      throws IOException, InterruptedException, org.apache.lucene.queryparser.classic.ParseException {
    System.out.println(String.format(
        "Testing sorted search for index at '%s' with sort on field %s with " +
            "locale %s, using sort-method %s, priority %s. Heap: %s",
        location, field, locale, method, priority, getHeap()));
    ExposedSettings.priority = priority;

    org.apache.lucene.index.IndexReader reader = ExposedIOFactory.getReader(location);
    System.out.println(String.format(
        "Opened index of size %s from %s. the indes has %d documents and %s" +
            " deletions. Heap: %s",
        readableSize(calculateSize(location)), location,
        reader.maxDoc(),
        reader.hasDeletions() ? "some" : " no",
        getHeap()));

/*    System.out.println(String.format(
        "Creating %s Sort for field %s with locale %s... Heap: %s",
        method, field, locale, getHeap()));
  */
    long startTimeSort = System.nanoTime();
    Sort sort;
    if ("exposed".equals(method) || "expose".equals(method)) {
      ExposedFieldComparatorSource exposedFCS =
          new ExposedFieldComparatorSource(reader, locale);
      sort = new Sort(new SortField(field, exposedFCS));
    } else if ("default".equals(method)) {
      if (locale == null) {
        sort = new Sort(new SortField(field, SortField.Type.STRING));
      } else {
        throw new UnsupportedOperationException(
            "native sort by locale not supported in Lucene 4 trunk");
/*
      sort = locale == null ?
          new Sort(new SortField(field, SortField.STRING)) :
          new Sort(new SortField(field, locale));

 */
      }
    } else {
      throw new IllegalArgumentException(
          "The sort method " + method + " is unsupported");
    }
    long sortTime = System.nanoTime() - startTimeSort;

    System.out.println(String.format(
        "Created %s Sort for field %s in %s. Heap: %s",
        method, field, nsToString(sortTime), getHeap()));

    IndexSearcher searcher = new IndexSearcher(reader);

    System.out.println(String.format(
        "\nFinished initializing %s structures for field %s.\n"
        + "Write standard Lucene queries to experiment with sorting speed.\n"
        + "The StandardAnalyser will be used and the default field is %s.\n"
        + "Finish with 'EXIT'.", method, field, defaultField));
    String query;
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    org.apache.lucene.queryparser.classic.QueryParser qp =
        new org.apache.lucene.queryparser.classic.QueryParser(
            Version.LUCENE_40, defaultField, new WhitespaceAnalyzer(Version.LUCENE_40));

    boolean first = true;
    while (true) {
      if (first) {
        System.out.print("\nQuery (" + method + " sort, first search might take " +
            "a while): ");
        first = false;
      } else {
        System.out.print("Query (" + method + " sort): ");
      }
      query = br.readLine();
      if ("".equals(query)) {
        continue;
      }
      if ("EXIT".equals(query)) {
        break;
      }
      try {
        long startTimeQuery = System.nanoTime();
        Query q = qp.parse(query);
        long queryTime = System.nanoTime() - startTimeQuery;

        long startTimeSearch = System.nanoTime();
//        TopFieldDocs topDocs = searcher.search(
//            q.weight(searcher), null, 20, sort, true);
        TopFieldDocs topDocs = searcher.search(q, 20, sort);
        long searchTime = System.nanoTime() - startTimeSearch;
        System.out.println(String.format(
            "The search for '%s' got %d hits in %s (+ %s for query parsing). "
                + "Showing %d hits.",
            query, topDocs.totalHits,
            nsToString(searchTime),
            nsToString(queryTime),
            (int)Math.min(topDocs.totalHits, MAX_HITS)));
        long startTimeDisplay = System.nanoTime();
        for (int i = 0 ; i < Math.min(topDocs.totalHits, MAX_HITS) ; i++) {
          int docID = topDocs.scoreDocs[i].doc;
          System.out.println(String.format(
              "Hit #%d was doc #%d with %s:%s",
              i, docID, field,
              ((BytesRef)((FieldDoc)topDocs.scoreDocs[i]).fields[0]).
                  utf8ToString()));
        }
        System.out.print("Displaying the search result took "
            + nsToString(
            System.nanoTime() - startTimeDisplay) + ". ");
        System.out.println("Heap: " + getHeap());
      } catch (Exception e) {
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
      }
    }
    System.out.println("\nThank you for playing. Please come back.");
  }

  private static void usage() {
    System.out.println(
        "Usage: ExposedPOC exposed|default <index> <sortField> <locale>" +
            " <defaultField> <optimization>\n"
            + "exposed:         Uses the expose sorter\n"
            + "default:        Uses the default sorter\n"
            + "<index>:        The location of an optimized Lucene index\n"
            + "<sortField>:    The field to use for sorting\n"
            + "<locale>:       The locale to use for sorting. If null is "
                             + "specified, natural term order is used\n"
            + "<defaultField>: The field to search when no explicit field is " +
            "given\n"
            + "<optimization>: Either speed or memory. Only relevant for " +
            "exposed\n"
            + "\n"
            + "Example:\n"
            + "ExposedPOC expose /mnt/bulk/40GB_index author da freetext"
            + "\n"
            + "If the index is is to be optimized, it can be done with\n"
            + "ExposedPOC optimize <index>\n"
            + "\n"
            + "Note: Heap-size is queried after a call to System.gc()\n"
            + "Note: This is a proof of concept. Expect glitches!"
    );
  }

  static String getHeap() throws InterruptedException {
    String b = "Before GC: " + getHeapDirect();
    for (int i = 0 ; i < 1 ; i++) {
      System.gc();
      Thread.sleep(10);
    }
    return b + ", after GC: " + getHeapDirect();
  }

  private static String getHeapDirect() {
    return readableSize(Runtime.getRuntime().totalMemory()
            - Runtime.getRuntime().freeMemory()) + "/"
        + readableSize(Runtime.getRuntime().totalMemory());
  }

  static String readableSize(long size) {
    return size > 2 * 1048576 ?
            size / 1048576 + "MB" :
            size > 2 * 1024 ?
                    size / 1024 + "KB" :
                    size + "bytes";
  }

  static long calculateSize(File file) {
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

  static String measureSortTime(final PackedInts.Reader orderedDocs) {
    Integer[] allDocIDS = new Integer[orderedDocs.size()];
    for (int i = 0 ; i < allDocIDS.length ; i++) {
      allDocIDS[i] = i;
    }
    long startTimeSort = System.nanoTime();
    Arrays.sort(allDocIDS, new Comparator<Integer>() {
      public int compare(Integer o1, Integer o2) {
        return (int)(orderedDocs.get(o1) - orderedDocs.get(o2));
      }
    });
    return nsToString(
            System.nanoTime() - startTimeSort);
  }

  static long footprint(PackedInts.Reader values) {
    return values.getBitsPerValue() * values.size() / 8;
  }

  static String nsToString(long time) {
    return  time > 10L * 1000 * 1000000 ?
            minutes(time) + " min" :
//            time > 2 * 1000000 ?
                    time / 1000000 + "ms";// :
//                    time + "ns";
  }

  private static String minutes(long num) {
    long min = num / 60 / 1000 / 1000000;
    long left = num - (min * 60 * 1000 * 1000000);
    long sec = left / 1000 / 1000000;
    String s = Long.toString(sec);
    while (s.length() < 2) {
      s = "0" + s;
    }
    return min + ":" + s;
  }
  
  
}