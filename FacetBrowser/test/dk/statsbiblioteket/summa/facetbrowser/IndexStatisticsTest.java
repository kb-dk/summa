/* $Id: IndexStatisticsTest.java,v 1.5 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/11 12:56:25 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.facetbrowser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import dk.statsbiblioteket.summa.common.util.FlexiblePair;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;

/**
 * @deprecated until updated to the new IndexDescriptor.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexStatisticsTest extends TestCase {
    // Make statistics for repetition of tags in facets
    // Produce a graph with documentcount at X and unique authors at Y
    public void testUnique() throws Exception {
//        String[] facets = {"author_normalised", "location_normalised", "series_normalised", "llcl", "lnlm", "ldbk", "ludk", "ldds", "lkl", "lfn", "linst", "ldk5", "lma_long", "su_pe", "su_corp", "su_lc", "mesh", "su_dk", "lsu_oai", "llang"};
        String[] facets = {"freetext"};
        IndexReader ir = null; //new IndexSearcher(ClusterCommon.getProperties().getProperty(ClusterCommon.INDEX_LOCATION)).getIndexReader();
//        Term searchTerm = new Term(facet, "");
//        TermEnum terms = ir.terms(searchTerm);
        Vector<HashSet<String>> sets = new Vector<HashSet<String>>(facets.length);

        System.out.print("DocID");
        for(String facet: facets) {
            System.out.print("\t" + facet);
            sets.add(new HashSet<String>(1000));
        }
        System.out.println("");
        for (int i = 0; i < ir.maxDoc() ; i++) {
            int counter = 0;
            for(String facet: facets) {
                HashSet<String> set = sets.get(counter++);
                TermFreqVector tfv = ir.getTermFreqVector(i, facet);
                if (tfv != null) {
                    String[] terms = tfv.getTerms();
                    if (terms != null) {
                        for (String term: terms) {
                            set.add(term);
                        }
                    }
                }
            }
            if (i % (ir.maxDoc() / 100) == 0) {
                System.out.print(i);
                for (HashSet<String> set: sets) {
                    System.out.print("\t" + set.size());
                }
                System.out.println("");
            }
        }
        System.out.print(ir.maxDoc()-1);
        for (HashSet<String> set: sets) {
            System.out.print("\t" + set.size());
        }

        int counter = 2;
        System.out.print("\n\nplot ");
        for(String facet: facets) {
            System.out.println("\"stat.dat\" using 1:" + counter++ +
                               " title '" + facet +
                               (counter < facets.length+2 ? "', \\" : ";"));
        }
    }
             /*
    public void dumpCount() throws Exception {
        int LIMIT = 5;
        int TOTALLIMIT = 5;
        String QUERY = "qzwq";
        Profiler pf = new Profiler();
        pf.setExpectedTotal(LIMIT);

        int hitCount = 0;

        SlimCollector slim = new SlimCollector();
        for (int total = 0 ; total < TOTALLIMIT ; total++) {
            pf.reset();
            for (int i = 0 ; i < LIMIT ; i++) {
                hitCount = SearchEngineImpl.getInstance().getHitCount(QUERY);
                pf.beat();
            }
            System.out.println("Performed " + LIMIT + " hitcounts for " + QUERY +
                               " which gave " + hitCount + " hits. " +
                               "This took a total of " + pf.getSpendTime() +
                               " which is " + (1/pf.getBps(false)) +
                               " seconds/search");

            pf.reset();
            for (int i = 0 ; i < LIMIT ; i++) {
                CountCollector counter = new CountCollector();
                SearchEngineImpl.getInstance().searchWithCollector(QUERY, counter);
                hitCount = counter.getCount();
                pf.beat();
            }
            System.out.println("Performed " + LIMIT + " slimcounts for " +
                               QUERY +
                               " which gave " + hitCount + " hits. " +
                               "This took a total of " + pf.getSpendTime() +
                               " which is " + (1/pf.getBps(false)) +
                               " seconds/search");

            slim.clean();
            pf.reset();
            for (int i = 0 ; i < LIMIT ; i++) {
                SearchEngineImpl.getInstance().searchWithCollector(QUERY, slim);
                hitCount = slim.getDocumentIDs().length;
                pf.beat();
            }
            System.out.println("Performed " + LIMIT + " slimcollect for " +
                               QUERY +
                               " which gave " + hitCount + " hits. " +
                               "This took a total of " + pf.getSpendTime() +
                               " which is " + (1/pf.getBps(false)) +
                               " seconds/search");
        }
    }
               */

    /*
    public void dumpSearchTime() throws Exception {
        int LIMIT = 5;
        int TOTALLIMIT = 5;
        String QUERY = "qzwq";
        IndexConnection index = IndexConnectionFactory.getIndexConnection();
        Profiler pf = new Profiler();
        pf.setExpectedTotal(LIMIT);

        Hits hits;


        for (int total = 0 ; total < TOTALLIMIT ; total++) {

            hits = index.getResults(QUERY); // Warm up

            pf.reset();
            for (int i = 0 ; i < LIMIT ; i++) {
                hits = index.getResults(QUERY);
                pf.beat();
            }
            System.out.println("Performed " + LIMIT + " searches for " + QUERY +
                               " which gave " + hits.length() + " hits. " +
                               "This took a total of " + pf.getSpendTime() +
                               " which is " + (1/pf.getBps(false)) +
                               " seconds/search");

            index.getSlimDocs(QUERY); // Warm up
            pf.reset();
            for (int i = 0 ; i < LIMIT ; i++) {
                index.getSlimDocs(QUERY);
                pf.beat();
            }
            System.out.println("Performed " + LIMIT + " slimdoc-searches for " +
                               QUERY +
                               " which gave " + hits.length() + " hits. " +
                               "This took a total of " + pf.getSpendTime() +
                               " which is " + (1/pf.getBps(false)) +
                               " seconds/search");
        }

    }
      */
    /**
     * Just call testUniqueChars
     */
    /*
    public static void main() throws Exception {
        new IndexStatisticsTest().testUniqueChars();
    } */
    /*
    public void countAuthor() throws Exception {
        countTerms("author_normalised");
    }
    public void countType() throws Exception {
        countTerms("lma_long");
    } */
    /*
    public void countTerms(String field) throws IOException {
        IndexReader ir = new IndexSearcher(ClusterCommon.getProperties().
                getProperty(ClusterCommon.INDEX_LOCATION)).getIndexReader();
        // termcount, doccount_with_this_termcount
        HashMap<Integer, Pair<Integer, String>> map = new HashMap<Integer, Pair<Integer, String>>(2000);
        Profiler pf = new Profiler();
        pf.setExpectedTotal(ir.maxDoc());
        int c = 0;
        for (int i = 0 ; i < ir.maxDoc() ; i++) {
            TermFreqVector terms = ir.getTermFreqVector(i, field);
            pf.beat();
            if (c++ % 49999 == 0) {
                System.out.println(c + "\t" + pf.getTimeLeftAsString(false));
            }
            if (terms != null) {
                int termcount = terms.size();
                Pair<Integer, String> p = map.get(termcount);
                Integer doccount = p == null ? null : p.getKey();
                String[] recordID = ir.document(i).getValues("recordID");
                map.put(termcount, new Pair<Integer, String>(doccount == null ? 1 : doccount + 1,
                                                             recordID == null ? map.get(termcount).getValue() : recordID[0]));
            }
        }
        // Sort by count
        LinkedList<Pair<Integer, Pair<Integer, String>>> sorted =
                new LinkedList<Pair<Integer, Pair<Integer, String>>>();
        for (Map.Entry<Integer, Pair<Integer, String>> entry: map.entrySet()) {
            sorted.add(new Pair<Integer, Pair<Integer, String>>(entry.getKey(), entry.getValue()));
        }
        Collections.sort(sorted);
        StringWriter sw = new StringWriter();
        sw.append(field).append("\n");
        sw.append("Termcount\tDocuments with this\tLast recordID\n");
        for (Pair<Integer, Pair<Integer, String>> entry: sorted) {
            sw.append(String.format("%d\t%d\t%s\n",
                                    entry.getKey(),
                                    entry.getValue().getKey(),
                                    entry.getValue().getValue()));
        }
        System.out.println(sw);
    }
      */

    /**
     * Creates stat-files in /tmp with the most popular tags under the given
     * fields.
     */
/*    public void dumpTermFrequency() throws IOException {
        int limit = 500;

        String[] wantedFields = ("ldk5, lma_long, su_pe, su_corp, su_lc, " +
                                 "mesh, location_normalised, " +
                                 "series_normalised, llcc, lnlm, ldbk, ludk, " +
                                 "lddc, lkl, lfn, linst, author_normalised, " +
                                 "sort_title, su_dk, lsu_oai, llang, " +
                                 "cluster").split(", ");

        IndexSearcher is = new IndexSearcher(ClusterCommon.getProperties().
                getProperty(ClusterCommon.INDEX_LOCATION));
        IndexReader ir = is.getIndexReader();

        int facetCount = 1;
        for (String facet : wantedFields) {
            System.out.println("Processing " + facet + " (" + facetCount++ +
                               "/" + wantedFields.length + ")");
            List<FlexiblePair<String, Integer>> tags = getTags(ir, facet);
            StringWriter sw = new StringWriter(10000);
            sw.write("Count\tTag\n");
            Collections.sort(tags);
            int count = 0;
            for (FlexiblePair<String, Integer> termPair: tags) {
                sw.append(String.valueOf(termPair.getValue())).append("\t").append(termPair.getKey()).append("\n");
                if (++count == limit) {
                    sw.append("<Listing aborted after ").append(String.valueOf(limit)).append("/").
                            append(String.valueOf(tags.size())).append(" tags>\n");
                    break;
                }
            }
            File dest = new File("/tmp/stat_" + facet + ".txt");
            System.out.println("Storing " + Math.min(limit, tags.size()) +
                               " tags in " + dest);
            Files.saveString(sw.toString(), dest);
        }
    } */

    /* Tests the speed of various topdoc extraction methods */
/*    public void testTopDocPerformance() throws Exception {
        int WARM = 2;
        int RUNS = 5;
        int[] MAXS = new int[] {10, 100, 1000, 10000};

        String indexLocation = ClusterCommon.getProperties().
                getProperty(ClusterCommon.INDEX_LOCATION);
        IndexSearcher is = new IndexSearcher(indexLocation);
        SearchDescriptor d = new SearchDescriptor(indexLocation);
        d.loadDescription(indexLocation);
        // TODO: Implement this
        SummaQueryParser p = new SummaQueryParser(new String[]{"freetext"},
                                                  new SimpleAnalyzer(), d);
        Query query = p.parse("freetext:bog");
        assertTrue("The number of hits for the search should be > 0",
                   is.search(query).length() > 0);
        System.out.println("The number of hits for '" + query + "' was "
                           + is.search(query).length());

        SlimCollector slim = new SlimCollector(is.search(query).length());
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(RUNS);

        for (int i = 0 ; i < WARM ; i++) {
            is.search(query, null, 1000);
        }

        for (int max: MAXS) {
            System.gc();
            profiler.reset();
            for (int i = 0 ; i < RUNS ; i++) {
                is.search(query, null, max);
                profiler.beat();
            }
            System.out.println(RUNS + " plain runs with " + max + " maxhits at "
                               + profiler.getBps() + " runs/second");
*/
/*            System.gc();
            profiler.reset();
            for (int i = 0 ; i < RUNS ; i++) {
                slim.clean();
                is.search(query, slim);
                profiler.beat();
            }
            System.out.println(RUNS + " slimc runs with " + max + " maxhits at "
                               + profiler.getBps() + " runs/second");
  */
/*            System.gc();
            profiler.reset();
            DiscardingCollector disc = new DiscardingCollector();
            for (int i = 0 ; i < RUNS ; i++) {
                is.search(query, disc);
                profiler.beat();
            }
            System.out.println(RUNS + " disc  runs with " + max + " maxhits at "
                               + profiler.getBps() + " runs/second");

            System.gc();
            profiler.reset();
            TopCollector topc = new TopCollector(max);
            for (int i = 0 ; i < RUNS ; i++) {
                topc.reset();
                is.search(query, topc);
                profiler.beat();
            }
            System.out.println(RUNS + " topc  runs with " + max + " maxhits at "
                               + profiler.getBps() + " runs/second");

            System.gc();
            profiler.reset();
            BinaryCollector bin = new BinaryCollector(max);
            for (int i = 0 ; i < RUNS ; i++) {
                bin.reset();
                is.search(query, bin);
                profiler.beat();
            }
            System.out.println(RUNS + " binc  runs with " + max + " maxhits at "
                               + profiler.getBps() + " runs/second");

            System.out.println("");
        }
  */
//    }

    public List<FlexiblePair<String, Integer>> getTags(IndexReader ir,
                                                       String facet) throws
                                                                   IOException {
        List<FlexiblePair<String, Integer>> tags =
                new ArrayList<FlexiblePair<String, Integer>>(10000);
        Term searchTerm = new Term(facet, "");
        TermEnum terms = ir.terms(searchTerm);
        while (terms.next()) {
            Term term = terms.term();
            if (!term.field().equals(facet)) {
                break;
            }
            tags.add(new FlexiblePair<String, Integer>(term.text(),
                                   ir.docFreq(term),
                                   FlexiblePair.SortType.SECONDARY_DESCENDING));
        }
        Collections.sort(tags);
        return tags;
    }
      /*
    public void testNullThingie() throws Exception {
        IndexSearcher is = new IndexSearcher(ClusterCommon.getProperties().
                getProperty(ClusterCommon.INDEX_LOCATION));
        IndexReader ir = is.getIndexReader();
        String facet = "lma_long";

        Term searchTerm = new Term(facet, "");

        TermEnum terms = ir.terms(searchTerm);
        assertNotNull("We should find something for lma_long",
                      terms.term());

        TermEnum terms2 = ir.terms();
        assertNull("Default without next yields null",
                      terms2.term());
        terms2.next();
        assertNotNull("We should find something for default terms after next()",
                      terms2.term());

    }   */
  /*
    public void matchLang() throws Exception {
        String LANG = "llang";
        IndexSearcher is = new IndexSearcher(ClusterCommon.getProperties().
                getProperty(ClusterCommon.INDEX_LOCATION));
        IndexReader ir = is.getIndexReader();

        System.out.println("Count\tTag\tISO639-2");
        List<FlexiblePair<String, Integer>> langs = getTags(ir, LANG);
        for (FlexiblePair<String, Integer> langPair: langs) {
            String englishName = "";
            for (String[] iso639_2Code: ClusterCommon.ISO639_2) {
                if (langPair.getKey().equals(iso639_2Code[0])) {
                    englishName = iso639_2Code[2];
                    break;
                }
            }
            System.out.println(langPair.getValue() + "\t" + langPair.getKey() +
                               "\t" + englishName);
        }
    }
    */
    /**
     * Dumps a list of all the unique characters in the index, sorted by
     * popularity.
     */
//    public void testUniqueChars() throws Exception {
//        testUniqueChars(ClusterCommon.getProperties().
//                getProperty(ClusterCommon.INDEX_LOCATION));
//    }

    public void testUniqueCharsInRealIndex() throws Exception {
        testUniqueChars("/space/full_index");
    }

    public void testUniqueCharsInPartRealIndex() throws Exception {
        testUniqueChars("/home/te/projects/summaIndex/split/64th");
    }

    public void testUniqueChars(String indexLocation) throws Exception {
        int limit = Integer.MAX_VALUE;
        int maxIDs = 5;
        int linelength = 50;
        IndexReader ir = new IndexSearcher(indexLocation).getIndexReader();
        Map<Character, Pair<Integer, String>> chars =
                new HashMap<Character, Pair<Integer, String>>(10000);
        int fieldcount = 1;
        Collection fieldNames = ir.getFieldNames(IndexReader.FieldOption.ALL);
        int total = fieldNames.size();
        for (Object object: fieldNames) {
            String field = (String)object;
            System.out.println("Field: " + fieldcount++ + "/" + total + " " +
                               field + " " +
                               "(unique chars so far: " + chars.size() + ")");
            Term searchTerm = new Term(field, "");
            TermEnum terms = ir.terms(searchTerm);
            while (terms.next()) {
                if (!field.equals(terms.term().field())) {
                    break;
                }
                String term = terms.term().text();
                if (term != null) {
                    for (char c: term.toCharArray()) {
                        Pair<Integer, String> p = chars.get(c);
                        if (p == null) {
                            List<String> docIDs =
                                    getRecordsWithTerm(ir, field, term, maxIDs);
                            p = new Pair<Integer, String>(1,
                                                      listToString(docIDs,
                                                                   maxIDs) +
                                                      "\t" + term);
                            chars.put(c, p);
                        } else {
                            p.setKey(p.getKey() + 1);
                            //p.setValue(term);
                        }
                    }
                }
            }
            if (chars.size() > limit) {
                break;
            }
        }
        StringWriter sw = new StringWriter();

        sw.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
                  + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
        sw.append("<html xml:lang=\"da\" lang=\"da\">\n");
        sw.append("<head>\n");
        sw.append("<title>Unique char statistics for " + indexLocation
                  + "</title>\n");
        sw.append("<meta http-equiv=\"Content-Type\" "
                  + "content=\"text/html; charset=utf-8\" />\n");
        sw.append("<link rel=\"stylesheet\" type=\"text/css\" "
                  + "href=\"chars.css\" />\n");
        sw.append("</head>\n");
        sw.append("<body>\n");
        sw.append("<h1>Unique char statistics for " + indexLocation
                  + "</h1>\n");

        sw.append("<p>Found ").append(String.valueOf(chars.size()));
        sw.append(" unique chars:</p>\n");
        sw.append("<p>");


        // Sort by popularity
        // Sorted is popularity, (char, sample)
/*        LinkedList<ReversePair<Integer, Pair<Character, String>>> sorted =
                new LinkedList<ReversePair<Integer, Pair<Character, String>>>();
        for (Map.Entry<Character, Pair<Integer, String>> entry:
                chars.entrySet()) {
            Character character = entry.getKey();
            int count = entry.getValue().getKey();
            String sample = entry.getValue().getValue();
            sorted.add(new ReversePair<Integer, Pair<Character, String>>(
                    count, new Pair<Character, String>(character, sample)));
        }
        Collections.sort(sorted);

        int c = 0;
        for (ReversePair<Integer, Pair<Character, String>> entry: sorted) {
            sw.append(entry.getValue().getKey());
            if (c++ == linelength) {
                c = 0;
                sw.append(" ");
            }
        }
        sw.append("</p>\n");

        sw.append("<h2>Order by popularity</h2>\n");
  */
//        sw.append("Char\tUnicode\tCount\tSample\n");
   /*     for (ReversePair<Integer, Pair<Character, String>> entry: sorted) {
                        sw.append(String.format("%s\t%s\t%d\t%s\n",
                                    entry.getValue().getKey(),
                                    Integer.toString(entry.getValue().getKey(), 16),
                                    entry.getKey(),
                                    entry.getValue().getValue()));

            sw.append("<div class=\"c\">");
            sw.append(entry.getValue().getKey()).append(" ");
            sw.append(Integer.toString(entry.getKey()));
            sw.append(" ");
            sw.append(expand(entry.getValue().getKey()));
            sw.append("</div>\n");
        }
        sw.append("</body>\n</html>");
                                    */
        storeString(sw.toString(), "/tmp/unique.html");
        System.out.println("Finished. Result stored in /tmp/unique.txt");
    }

    private String expand(int number) {
        String result = Integer.toString(number, 16);
        while (result.length() < 4) {
            result = "0" + result;
        }
        return "0x" + result;
    }

    public void doreteFind() throws Exception {
        StringWriter sw = new StringWriter(100);
        sw.append((char)0x0060);
        sw.append((char)0x00B4);
        sw.append((char)0x005E);
        sw.append((char)0x007E);
        sw.append((char)0x00AF);
        sw.append((char)0x02D8);
        sw.append((char)0x02C7);
        sw.append((char)0x02DD);
        sw.append((char)0x00B8);
        sw.append((char)0x02DB);
        findRecordsWithChars(sw.toString());
    }

    public void findCombining() throws Exception {
        StringWriter sw = new StringWriter(100);
        for (int c = 0x300 ; c <= 0x36F ; c++) {
            sw.append((char)c);
        }
        findRecordsWithChars(sw.toString());
    }

    public void findRecordsWithChars(String searchChars) throws Exception {
        int limit = Integer.MAX_VALUE;
        int maxRecords = Integer.MAX_VALUE;
        IndexReader ir = null;//new IndexSearcher(ClusterCommon.getProperties().
                //getProperty(ClusterCommon.INDEX_LOCATION)).getIndexReader();
        Map<String, Set<Character>> foundRecords =
                new HashMap<String, Set<Character>>(10000);
        int fieldcount = 1;
        Collection fieldNames = ir.getFieldNames(IndexReader.FieldOption.ALL);
        int total = fieldNames.size();
        for (Object object: fieldNames) {
            String field = (String)object;
            System.out.println("Field: " + fieldcount++ + "/" + total + " " +
                               field + " " +
                               "(unique recordIDs so far: " +
                               foundRecords.size() + ")");
            Term searchTerm = new Term(field, "");
            TermEnum terms = ir.terms(searchTerm);
            while (terms.next()) {
                if (!field.equals(terms.term().field())) {
                    break;
                }
                String term = terms.term().text();
                if (term != null) {
                    for (char c: term.toCharArray()) {
                        boolean found = false;
                        for (char s: searchChars.toCharArray()) {
                            if (c == s) {
                                List<String> records =
                                        getRecordsWithTerm(ir, field, term,
                                                           maxRecords);
                                for (String record : records) {
                                    Set<Character> storedChars =
                                            foundRecords.get(record);
                                    if (storedChars == null) {
                                        Set<Character> newCharSet =
                                                new HashSet<Character>(1);
                                        foundRecords.put(record, newCharSet);

                                    } else {
                                        storedChars.add(c);
                                    }
                                }
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                        if (foundRecords.size() > limit) {
                            break;
                        }
                    }
                }
                if (foundRecords.size() > limit) {
                    break;
                }
            }
            if (foundRecords.size() > limit) {
                break;
            }
        }
        StringWriter sw = new StringWriter();
        sw.append("Found ").append(String.valueOf(foundRecords.size()))
                .append(" records containing one or more of the characters ")
                .append(searchChars).append(":\n");

        // Sort and write
/*        List<Pair<String, Set<Character>>> sorted = new
            LinkedList<Pair<String, Set<Character>>>();
        for (Map.Entry<String, Set<Character>> entry: foundRecords.entrySet()) {
            sorted.add(new Pair<String,
                    Set<Character>>(entry.getKey(), entry.getValue()));

        }
        Collections.sort(sorted);
        for (Pair<String, Set<Character>> recordID: sorted) {
            sw.append(recordID.getKey()).append("\t");
            for (Character c: recordID.getValue()) {
                sw.append(c);
            }
            sw.append("\thttp://statsbiblioteket.dk/summa/");
            sw.append("showrecord.jsp?record_id=").append(recordID.getKey());
            sw.append("\n");
        }

        storeString(sw.toString(), "/tmp/searchresult.txt");                  */
        System.out.println("Finished. Result stored in /tmp/searchresult.txt");
    }

    public void storeString(String content, String file) throws
                                                         FileNotFoundException {
        FileOutputStream out = new FileOutputStream(file);
        PrintWriter p = new PrintWriter(out);
        p.println(content);
        p.close();
    }

    /**
     * Search for terms with characters that seems to be mangled by conversion
     * (or rather: lack thereof) to utf-8 from iso-8859-1.
     */
    public void termsWithMangledChars() throws Exception {
        System.out.println("Running through the terms, looking for wrangled " +
                           "letters. Please have patience.");
        Character[] wranglesigns = new Character[] {}; // TODO: Find the right 2 chars again (lost due to CVS char-thingies) 
        int maxResults = 10000;
        int maxRecords = Integer.MAX_VALUE;
        IndexReader ir = null;//new IndexSearcher(ClusterCommon.getProperties().
                //getProperty(ClusterCommon.INDEX_LOCATION)).getIndexReader();
        Collection fieldNames = ir.getFieldNames(IndexReader.FieldOption.ALL);
        int hits = 0;
        StringWriter sw = new StringWriter(20000);
        for (Object object: fieldNames) {
            String field = (String)object;
            Term searchTerm = new Term(field, "");
            TermEnum terms = ir.terms(searchTerm);
            while (terms.next()) {
                if (!field.equals(terms.term().field())) {
                    break;
                }
                String term = terms.term().text();
                if (term != null) {
                    char[] chars = term.toCharArray();
                    for (int i = 0 ; i < chars.length ; i++ ) {
                        char c = chars[i];
                        for (Character wranglesign: wranglesigns) {
                            /* Is the character on the list of
                               usual suspects? */
                            if (c == wranglesign) {
                                /* It must not be the last char and the
                                   following char should have a code above
                                   126 and below 255. */
                                if (i != chars.length-1 && chars[i+1] > 126 &&
                                    chars[i+1] < 255) {
                                    if (hits < maxResults) {
                                        List<String> docIDs =
                                              getRecordsWithTerm(ir, field, term, maxRecords);
                                        sw.append(term).append("\n").append(unwrangle(term)).append("\nrecordIDs: ")
                                                .append(listToString(docIDs, 10)).append(" (Total: ")
                                                .append(String.valueOf(docIDs.size())).append(")\n\n");
                                    }
                                    hits++;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        String destination = "/tmp/wrangle.txt";
        String total = "Found a total of " + hits +
                       " terms, suspected of iso-8859-1 <> utf-8 wrangling.";
        System.out.println(total + "\nThe result is stored in " + destination);
        storeString(total + "\nPrinting the fishy strings with " +
                    "suggestions for corrections:\n\n" + sw.toString(),
                    destination);

    }

    public String listToString(List list, int limit) {
        int counter = 0;
        StringWriter sw = new StringWriter(10000);
        for (Object o : list) {
            if (counter++ > 0) {
                sw.append("\t");
            }
            sw.append(o.toString());
            if (counter == limit) {
                break;
            }
        }
        return sw.toString();
    }

    public List<String> getRecordsWithTerm(IndexReader ir,
                                           String field,
                                           String term,
                                           int limit) {
        LinkedList<String> result = new LinkedList<String>();
        try {
            TermDocs docs = ir.termDocs(new Term(field, term));
            int counter = 0;
            while(docs.next()) {
                String[] recordID =
                        ir.document(docs.doc()).getValues("recordID");
                if (recordID != null && recordID.length > 0) {
                    for (String id: recordID) {
                        result.add(id);
                    }
                }
                if (++counter == limit) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Exception searching for " + field + ":" + term);
            e.printStackTrace();
        }
        return result;
    }

    public void dumpWrangledChars() throws Exception {
/*        String input = "æ ø å ö ó Æ Ø Å Ö Ó";
        byte[] utf8 = input.getBytes("UTF-8");
        String wrangledUtf8 = new String(utf8, "iso-8859-1");
        System.out.println(input + " => " + wrangledUtf8);
  */
        byte[] all = new byte[128];
        for (int i = 128 ; i < 256 ; i++) {
            all[i-128] = (byte)i;
        }
        byte[] plainutf8 = new String(all, "iso-8859-1").getBytes("UTF-8");
        String wrangledUtf8 = new String(plainutf8, "iso-8859-1");
        String unwrangled =
                new String(wrangledUtf8.getBytes("iso-8859-1"), "utf-8");

        System.out.println(new String(all, "iso-8859-1"));
        System.out.println(wrangledUtf8);
        System.out.println(unwrangled);
    }

    public String unwrangle(String in) {
        try {
            return new String(in.getBytes("iso-8859-1"), "utf-8");
        } catch (Exception ex) {
            // Ugly, but this is just a quick hack
            return "Error in unwrangler";
        }
    }

    /**
     * Just playing around.
     */
    public void coolName() {
        // This should give "Toke" with lightning above the o
        System.out.println(Character.toString('T') +
                           Character.toString((char)0x035B) + "ke");
    }
}



