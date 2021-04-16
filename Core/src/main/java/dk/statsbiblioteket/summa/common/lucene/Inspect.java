/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * The State and University Library of Denmark
 * CVS:  $Id: Inspect.java,v 1.9 2007/10/04 13:28:20 te Exp $
 */
package dk.statsbiblioteket.summa.common.lucene;

import dk.statsbiblioteket.summa.support.lucene.LuceneUtil;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SuppressWarnings("ALL")
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Class and methods needs Javadoc")
// TODO: Re-implement Inspect for Lucene 4 trunk
public class Inspect {
    private static final String defaultIndex = "/space/512th";

    private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private IndexReader ir;
    private static final long MAXSPEEDTIME = 1000 * 30;
    private List<String> fieldnames;
    private static final String COMMANDS =
        "STATS [field divider], SPEED, HITS, TOPDOCS, QUIT, CACHE field, "
        + "docID or FieldName\n\n"
        + "STATS field divider example:\n"
        + "=> STATS recordID :\n"
        + "will extract all fields with the name 'recordID', then split "
        + "the content of the field at : and count the frequency of the"
        + "first token in the split.\n"
        + "CACHE field example:\n"
        + "=> CACHE sort_title :\n"
        + "will populate a FieldCacheImpl with the content of sort_title "
        + "in the form of plain strings.\n"
        + "=> ";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("No index-location specified. Defaulting to " + defaultIndex);
            new Inspect(defaultIndex);
        } else {
            new Inspect(args[0]);
        }
    }

    public static final String STATS = "STATS";
    public static final String CACHE = "CACHE";
    public Inspect(String indexLocation) throws Exception {
    //    openIndex(indexLocation);
        System.out.println("Simple Lucene Index inspection tool. Commands:");
        System.out.print(COMMANDS + ": ");
        String command;
/*        while (!"QUIT".equals(command = in.readLine())) {
            if (STATS.equals(command)) {
                stats();
            } else if (command.startsWith(STATS)) {
                stats(command.substring(STATS.length() + 1).split(" "));
            } else if (command.startsWith(CACHE)) {
                cache(command.substring(CACHE.length() + 1));
            } else if ("SPEED".equals(command)) {
                speed();
            } else if ("HITS".equals(command)) {
                hits();
            } else if ("TOPDOCS".equals(command)) {
                topdocs();
            } else {
                try {
                    lookup(Integer.parseInt(command));
                } catch (NumberFormatException e) {
                    field(command);
                    */
/*                    System.err.println("\"" + command
                                       + "\" not STATS, SPEED, QUIT, docID " +
                                       "or FieldName:"
                                       + e.getMessage());*/
/*                }
            }
            System.out.print(COMMANDS);
        }                              */
    }

/*    private void cache(String field) throws IOException {
        System.out.println("Populating cache for field '" + field + "'");
        Profiler profiler = new Profiler();
        List<AtomicReader> readers = LuceneUtil.gatherSubReaders(ir);

        long size = 0;
        int defined = 0;
        int nulls = 0;
        for (AtomicReader reader: readers) {
            FieldCache.DocTerms result = FieldCache.DEFAULT.getTerms(reader, field);

            BytesRef ref = new BytesRef();
            for (int i = 0 ; i < result.size() ; i++) {
                ref = result.getTerm(i, ref);
                if (ref != null) {
                    size += ref.length;
                    defined++;
                } else {
                    nulls++;
                }
            }
        }
        String time = profiler.getSpendTime();
        System.out.println("Cached " + defined + " terms for field '" + field + "' of average size "
                           + size * 1.0 / defined + "bytes (" + nulls + " nulls) in " + time);
    }*/
/*
    private void stats(String[] strings) throws IOException {
        int feedback = Math.max(1, ir.maxDoc() / 50);
        if (strings.length != 2) {
            System.err.println("STATS expected 2 arguments: field and divider");
            return;
        }
        String fieldName = strings[0];
        String divider = strings[1];
        HashMap<String, Integer> buckets = new HashMap<String, Integer>(100);
        TermsEnum terms = ir.terms(fieldName).iterator();
        int counter = 0;
        int errors = 0;
        BytesRef ref;
        while ((ref = terms.next()) != null) {
            String[] split = ref.utf8ToString().split(divider, 2);
            if (split.length != 2) {
                errors++;
                if (errors == 1) {
                    System.err.println("Term '" + ref.utf8ToString() + "' in field " + fieldName
                                       + "' did not divide by '" + divider + "'. Skipping all following errors");
                }
            } else {
                if (!buckets.containsKey(split[0])) {
                    buckets.put(split[0], 1);
                } else {
                    buckets.put(split[0], buckets.get(split[0]) + 1);
                }
            }
            if (counter++ % feedback == 0) {
                System.out.print(".");
            }
        }
        System.out.println("\nStats for field '" + fieldName + " with divider '" + divider + "'. DocCount: "
                           + ir.maxDoc() + ". FieldTermCount: " + counter);
        for (Map.Entry<String, Integer> bucket: buckets.entrySet()) {
            System.out.println(bucket.getKey() + ": " + bucket.getValue());
        }
        System.out.flush();
        if (errors > 0) {
            System.err.println(errors + " terms could not be parsed");
        }
        System.out.println("");
    }
  */
    private void hits() throws IOException {
        IndexSearcher is = new IndexSearcher(ir);
        TopScoreDocCollector topDocs = TopScoreDocCollector.create(Integer.MAX_VALUE, false);
        is.search(new MatchAllDocsQuery(), null, topDocs);
        System.gc();
        int feedback = topDocs.getTotalHits() / 100;
        System.out.println("Iterating through " + topDocs.getTotalHits() + " hits, requesting hits.doc for each one");
        Profiler pf = new Profiler();
        pf.setExpectedTotal(topDocs.getTotalHits());
        for (int i = 0 ; i < topDocs.getTotalHits() ; i++) {
            //Document doc = hits.doc(i);
//            Document doc = ir.document(hits.id(i));
/*            for (String field: fieldnames) {
                doc.getValues(field);
            }*/
            if (i % feedback == 0) {
                System.gc();
                System.out.println("Memory usage at " + i + "/" + topDocs.getTotalHits() + ": " + getMem());
            }
            pf.beat();
        }
        System.out.println("Finished " + topDocs.getTotalHits() + " hits in " + pf.getSpendTime());
    }

    private void topdocs() throws IOException {
        IndexSearcher is = new IndexSearcher(ir);
        TopScoreDocCollector topDocsCol = TopScoreDocCollector.create(Integer.MAX_VALUE, false);
        is.search(new MatchAllDocsQuery(), null, topDocsCol);
        TopDocs topDocs = is.search(new MatchAllDocsQuery(), null, topDocsCol.getTotalHits());
        System.gc();
        int feedback = topDocs.totalHits / 100;
        System.out.println("Iterating through " + topDocs.totalHits + " hits, requesting hits.doc for each one");
        Profiler pf = new Profiler();
        pf.setExpectedTotal(topDocs.totalHits);
        for (int i = 0 ; i < topDocs.totalHits ; i++) {
            //Document doc = ir.document(topDocs.scoreDocs[i].doc);
//            Document doc = ir.document(hits.id(i));
/*            for (String field: fieldnames) {
                doc.getValues(field);
            }*/
            if (i % feedback == 0) {
                System.gc();
                System.out.println("Memory usage at " + i + "/" +topDocs.totalHits + ": " + getMem());
            }
            pf.beat();
        }
        System.out.println("Finished " + topDocs.totalHits + " hits in " + pf.getSpendTime());
    }

    private void lookup(int i) throws IOException {
        if (i > ir.maxDoc()) {
            System.err.println("Max doc is " + ir.maxDoc());
            return;
        }
        System.out.println("Dumping document " + i);
        Document doc = ir.document(i);
        for (String field: fieldnames) {
            String[] values = doc.getValues(field);
            if (values == null) {
                BytesRef[] binary = doc.getBinaryValues(field);
                if (binary == null) {
                    IndexableField[] fields = doc.getFields(field);
                    if (fields == null) {
/*                        String indirect = indirect(i, field);
                        System.out.print("No String, binary values or sub-fields for " + field + ". Indirect terms: ");
                        System.out.println("".equals(indirect) ? "none" : indirect);*/
                    } else {
                        System.out.println(
                            "No strings or binary values for " + field + ", but " + fields.length + " sub-fields");
                        for (IndexableField f: fields) {
                            System.out.println(
                                " - ReaderValue: " + f.readerValue() + " IsStored: " + f.fieldType().stored());
                        }
                    }
                } else {
                    System.out.println("Field " + field + " has " + binary.length + " binary values");
                }
            } else {
                System.out.print("Field " + field + ": ");
                for (String term: values) {
                    System.out.print(term);
                    System.out.print(" ");
                }
                System.out.println("");
            }
        }
    }
        /*
    private String indirect(int docID, String fieldName) throws IOException {
        StringWriter sw = new StringWriter(1000);
        TermsEnum terms = ir.terms(fieldName).iterator();
        BytesRef ref;
        DocsEnum docs = null;
        while ((ref = terms.next()) != null) {
            docs = terms.docs(ir.getLiveDocs(), docs);
            while (docs.nextDoc() != DocsEnum.NO_MORE_DOCS) {
                if (docs.docID() == docID) {
                    sw.append(ref.utf8ToString()).append(" ");
                    break;
                }
            }
        }
        return sw.toString();
    }
          */
/*    public void stats() throws IOException {
        System.out.println("Mem: " + getMem());
        System.out.println("\nFields:");

        FieldsEnum fields = ir.fields().iterator();
        String field;
        long totalTermCount = 0;
        while ((field = fields.next()) != null) {
            long termCount = 0;
            if (ir.flatten() == null) {
                termCount = ir.terms(field).getUniqueTermCount();
            } else {
                for (IndexReader sub: ir.flatten()) {
                    termCount += sub.terms(field).getUniqueTermCount();
                }
            }
            System.out.println("Field " + field + ": " + termCount + " terms");
            totalTermCount += termCount;
        }
        System.out.println("Total: " + ir.maxDoc()+ " documents, "
                           + totalTermCount + " terms");
    }
  */
    public static String getMem() {
        return (Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()) / 1048576 + "/" +
               Runtime.getRuntime().totalMemory() / 1048576 + "MB used";

    }

    public static void dumpMemory() throws Exception {
        System.out.println("Hello world!");
        System.out.println(getMem());
    }
    /*
    public void openIndex(String indexLocation) throws IOException {
        System.out.println("Opening index at " + indexLocation);
        ir = DirectoryReader.open(new NIOFSDirectory(new File(indexLocation), new SimpleFSLockFactory()));
        Collection fields = ir.getFieldNames(IndexReader.FieldOption.ALL);
        fieldnames = new ArrayList<String>(fields.size());
        for (Object field: fields) {
            fieldnames.add((String)field);
        }
        Collections.sort(fieldnames);
    }
      */
    // TODO: Re-implement speed
/*    public void speed() throws Exception {
        long endTime = System.currentTimeMillis() + MAXSPEEDTIME;
        System.out.println("Finding fields in documents");
        Collection fields = ir.getFieldNames(IndexReader.FieldOption.INDEXED);
        List<String> fieldnames = new ArrayList<String>(fields.size());
        for (Object field: fields) {
            System.out.println(field);
            fieldnames.add((String)field);
        }
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(10000);
        System.out.println("Index size: " + ir.maxDoc() + " documents");
        int statspan = ir.maxDoc() / 100;
        for (int i = 0 ; i < ir.maxDoc() ; i++) {
            Document document = ir.document(i);
            for (String field: fieldnames) {
                document.get(field);
                profiler.beat();
            }
            if (i % statspan == 0 || System.currentTimeMillis() > endTime) {
                System.out.println(profiler.getBps(false) + " requests/sec Mem: " + getMem() + " doc " + i + "/"
                                   + ir.maxDoc());
            }
            if (System.currentTimeMillis() > endTime) {
                System.out.println("Stopping speed measurement due as it exceeded the allocated time");
                return;
            }
        }
    }

    public void field(String fieldName) throws IOException {
        int MAXDOCS = 10;
        int counter = 0;

        TermsEnum terms = ir.terms(fieldName).iterator();
        while (terms.next() != null) {
            System.out.println(fieldName + ": " + terms.term().utf8ToString());
            field(fieldName, terms.term().utf8ToString());
            if (counter++ >= MAXDOCS) {
                System.out.println("Maximum display count reached");
                break;
            }
        }
    }

    public void field(String fieldName, String termName) throws IOException {
        int MAXDOCS = 5;

        TermsEnum terms = ir.terms(fieldName).iterator();
        BytesRef ref = new BytesRef(termName);
        if (!terms.seekExact(ref, false)) {
            System.out.println(" - No TermDocs for field " + fieldName);
            return;
        }

        System.out.println(" - Dumping the first " + MAXDOCS + " docs for " + fieldName + ": " + termName);
        int counter = 0;
        DocsEnum docs = null;
        docs = terms.docs(ir.getLiveDocs(), docs);
        do {
            System.out.println("  - Document " + docs.read());
            counter++;
        } while (counter < MAXDOCS && docs.nextDoc() != DocsEnum.NO_MORE_DOCS);
    }
    */
}
