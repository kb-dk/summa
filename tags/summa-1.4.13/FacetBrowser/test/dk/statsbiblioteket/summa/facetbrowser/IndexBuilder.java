/* $Id: IndexBuilder.java,v 1.11 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.11 $
 * $Date: 2007/10/05 10:20:22 $
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

import java.io.IOException;
import java.io.File;
import java.util.Random;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.common.index.IndexCommon;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Builds a Lucene test index.
 * Checks for index existence and version before build.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", "JavaDoc"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexBuilder {
    public static final File INDEX_ROOT =
            new File(System.getProperty("java.io.tmpdir"), "index_root");
    public static final File DESCRIPTOR =
            new File(INDEX_ROOT, "TestIndexDescriptor.xml");
    public static final File DATE_LOCATION =
            new File(INDEX_ROOT, "20080904-143735");
    public static final String INDEX_LOCATION =
            new File(DATE_LOCATION, "lucene").getAbsolutePath();
    public static final int REPLICATIONCOUNT = 500;
    public static final File TIMESTAMP_FILE =
            new File(DATE_LOCATION, IndexCommon.VERSION_FILE);

    public static final String AUTHOR = "author";
    public static final String AUTHOR_NORMALISED = "author_normalised";
    public static final String ID =     "UniqueID";
    public static final String TITLE =  "title";
    public static final String GENRE =  "genre";
    public static final String STATUS = "status";
    public static final String STATIC = "static";
    public static final String FREETEXT =  "freetext";
    public static final String VARIABLE = "variable";

    public static final String STATIC_CONTENT = "qzwq";

    /**
     * If VERSION is changed, a new test index will be build upon call
     * to checkIndex(), if the old index does not correspond to VERSION.
     */
    private static final String VERSION = "1.8";
    // 1.3: Added TermFrequencyVectors
    // 1.4: Added non-stored STATUS
    // 1.5: Added feedback
    // 1.6: Added variable field
    // 1.7: Added static field
    // Changed to Summa format

    public static void main(String[] args) throws IOException {
        checkIndex();
    }

    /**
     * Check if the test index exists and is up to date. If not, a new index
     * is created. Call this method as part of JUnit setUp().
     */
    public static void checkIndex() throws IOException {
        if (!versionFile().exists()){
            buildIndex();
        }
    }

//    public static IndexConnectionImplLocal getConnection() {
//        return new IndexConnectionImplLocal(IndexBuilder.INDEX_LOCATION);
//    }

    public static IndexReader getReader() throws IOException {
        checkIndex();
        Configuration conf = new Configuration(new MemoryStorage());
        conf.set(IndexConnector.INDEXROOT + "TYPE",
                 IndexConnector.INDEXTYPE.singleIndex);
        conf.set(IndexConnector.INDEXROOT + "LINKS",
                 INDEX_LOCATION);
        return new IndexConnector(conf).getReader();

    //    return getConnection().getIndexReader();
    }




    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String[][] comics = {
            {"Dværgenes Konge",      "Chevalier og Ségul", "fantasy hardcover", "hjemme"},
            {"Rejsen til Saturn",    "Deleurant",          "dansk softcover",   "hjemme"},
            {"Thorfin",              "Deleurant",          "dansk softcover",   "hjemme"},
            {"Troldkvindens datter", "Serge Le Tendre",    "fantasy",           "hjemme"},
            {"Zanfyst fra Troy",     "Christophe Arleston","fantasy",           "hjemme"},
            {"Appleseed",            "Masamune Shirow",    "sci-fi manga",      "hjemme"},
            {"Ranma ½",              "Rumiko Takahashi",   "manga",             "ikkehjemme"},
            {"Dragonball",           "Akira Toriyama",     "manga",             "hjemme"},
            {"Hypernauten",          "Peter Snejbjerg",    "dansk softcover",   "hjemme"}
    };


    /**
     * buildIndex needs to be called once, before testing anything. This is
     * handled automatically by checkIndex().
     * Changes to the test-index naturally requires a re-run of buildIndex.
     * All this collides with the standard way of doing Unit-tests, but it
     * takes too long to build the index from scratch every time a test is run.
     * @throws IOException
     */
    private static void buildIndex() throws IOException {
        Random random = new Random();
        // PerFieldAnalyzerWrapper
        System.out.println("Building test index with " + REPLICATIONCOUNT
                           + " replications");

        // Delete the old
        deleteDir(new File(INDEX_LOCATION));

        IndexWriter writer = new IndexWriter(new File(INDEX_LOCATION),
                                             new StandardAnalyzer(),
                                             true);
        writer.setUseCompoundFile(false);
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(REPLICATIONCOUNT);
        for (int i = 0 ; i < REPLICATIONCOUNT ; i++) {
            profiler.beat();
            //noinspection ConstantConditions,PointlessBooleanExpression
            if (REPLICATIONCOUNT > 100 && i % (REPLICATIONCOUNT / 100) == 0) {
                System.out.println(i / (REPLICATIONCOUNT / 100) + "% - ETA: "
                                   + profiler.getETAAsString(false));
            }
            int j = 0;
            for (String[] comic : comics) {
                String id = String.format("%d-%d", i, j);
                Document doc = createDocument(comic, id);
                writer.addDocument(doc);
                j++;
            }
        }
        writer.optimize();
        writer.close();
        versionFile().createNewFile();

        Files.saveString(Long.toString(System.currentTimeMillis()),
                         TIMESTAMP_FILE);

        Files.saveString(
                Resolver.getUTF8Content("data/TestIndexDescriptor.xml"),
                DESCRIPTOR);

        System.out.println("Finished creating test-index in "
                           + profiler.getSpendTime() + " (" + 
                           + profiler.getBps(true) + ". It can be found in"
                           + INDEX_LOCATION);
    }

    public static Document createDocument() {
        return createDocument("random" + random.nextInt());
    }
    public static Document createDocument(String id) {
        return createDocument(comics[random.nextInt(comics.length)], id);
    }

    private static Random random = new Random();
    private static Document createDocument(String[] comic, String id) {
        Document doc = new Document();
        doc.add(new Field(TITLE, comic[0],
                          Field.Store.YES, Field.Index.TOKENIZED,
                          Field.TermVector.WITH_POSITIONS_OFFSETS));
        doc.add(new Field(ID, id,
                          Field.Store.YES, Field.Index.UN_TOKENIZED,
                          Field.TermVector.WITH_POSITIONS_OFFSETS));
        doc.add(new Field(AUTHOR, comic[1],
                          Field.Store.YES, Field.Index.TOKENIZED,
                          Field.TermVector.WITH_POSITIONS_OFFSETS));
        doc.add(new Field(STATIC, STATIC_CONTENT,
                          Field.Store.YES, Field.Index.UN_TOKENIZED,
                          Field.TermVector.YES));
        doc.add(new Field(AUTHOR_NORMALISED, comic[1],
                          Field.Store.YES, Field.Index.UN_TOKENIZED,
                          Field.TermVector.YES));
        doc.add(new Field(GENRE, comic[2],
                          Field.Store.YES, Field.Index.TOKENIZED,
                          Field.TermVector.WITH_POSITIONS_OFFSETS));
        doc.add(new Field(STATUS, comic[3],
                          Field.Store.YES, //or NO?
                          Field.Index.TOKENIZED,
                          Field.TermVector.NO));
        doc.add(new Field(VARIABLE, "Variable_" +
                                    random.nextInt(REPLICATIONCOUNT/2),
                          Field.Store.YES,
                          Field.Index.TOKENIZED,
                          Field.TermVector.NO));
        doc.add(new Field(FREETEXT, comic[0] + " " + comic[1] + " "+
                                    comic[2],
                          Field.Store.YES, //or NO?
                          Field.Index.TOKENIZED,
                          Field.TermVector.WITH_POSITIONS_OFFSETS));
        return doc;
    }

    public static int getDocumentCount() {
        return REPLICATIONCOUNT * comics.length;
    }

    public static File versionFile() {
        return new File(String.format("%s/%s", INDEX_LOCATION, VERSION));
    }

    // http://javaalmanac.com/egs/java.io/DeleteDir.html
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
}



