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
 * CVS:  $Id: BuildPerformance.java,v 1.2 2007/10/04 13:28:20 te Exp $
 */
package dk.statsbiblioteket.summa.common.lucene;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

/**
 * Builds a Lucene index and provides running feedback on index speed.
 * Used for testing raw build-performance.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BuildPerformance {
    static Random random = new Random();
    static long uniqueCounter = 0;

    /**
     * Builds an index with the given number of documents, the given number
     * of fields and terms with random length up to maximum term size.
     * @param args number of documents, number of fields, maximum term size.
     * @throws IOException if error occured.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: BuildPerformance documentCount "
                               + "fieldCount maxTermLength [noIndex]");
            System.exit(-1);
        }
        int documents = Integer.parseInt(args[0]);
        int fieldCount = Integer.parseInt(args[1]);
        String[] fields = new String[fieldCount];
        System.out.print("Fields:");
        for (int i = 0 ; i < fieldCount ; i++) {
            fields[i] = "field" + i;
            System.out.print(" " + fields[i]);
        }
        System.out.println("");
        int maxTermLength = Integer.parseInt(args[2]);
        boolean noindex = false;
        if (args.length > 3) {
            noindex = Boolean.parseBoolean(args[3]);
        }

        File indexLocation = new File("testindex");
        if (indexLocation.exists()) {
            deleteDir(indexLocation);
        }
        System.out.println("Building index in '" + indexLocation + "' with "
                           + documents + " documents with " + fieldCount
                           + " fields with max term length " + maxTermLength);
        IndexWriterConfig writerConfig =
               new IndexWriterConfig(Version.LUCENE_30, 
                                     new StandardAnalyzer(Version.LUCENE_30));
        writerConfig.setMaxFieldLength(
                                    IndexWriterConfig.UNLIMITED_FIELD_LENGTH);
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        writerConfig.setRAMBufferSizeMB(1000);
        IndexWriter writer = new IndexWriter(new NIOFSDirectory(indexLocation),
                                             writerConfig);
        int feedback = Math.max(1, documents / 100);
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(documents);
        profiler.setBpsSpan(1000);
        for (int i = 0 ; i < documents ; i++) {
            Document document = createDocument(fields, maxTermLength);
            if (!noindex) {
                writer.addDocument(document);
            }
            profiler.beat();
            if (i % feedback == 0) {
                System.out.println("Adding document " + (i+1) + "/" + documents
                                   + " at " + Math.round(profiler.getBps(true))
                                   + " docs/sec. ETA: "
                                   + profiler.getETAAsString(true));
            }
        }
        writer.close();
        System.out.println("Finished writing " + documents + " documents in "
                           + profiler.getSpendTime()
                           + " at " + Math.round(profiler.getBps())
                           + " documents/second");
        //deleteDir(indexLocation);
    }

    private static Document createDocument(String[] fields, int maxTermLength) {
        Document document = new Document();
        for (String field: fields) {
            randomWords(maxTermLength);
            document.add(new Field(field, randomWords(maxTermLength),
                              Field.Store.YES, Field.Index.NOT_ANALYZED,
                              Field.TermVector.NO));
        }
        return document;
    }

    private static String randomWords(int maxTermLength) {
        int size = random.nextInt(maxTermLength)+1;
        StringBuilder sb = new StringBuilder(maxTermLength + 20);
        while (sb.length() < size) {
            randomWord(sb, 10);
            sb.append(" ");
        }
        return sb.toString().substring(0, size);
    }

    private static String randomWord(int length) {
        StringBuilder sb = new StringBuilder(length);
        randomWord(sb, length);
        return sb.toString();
    }
    private static void randomWord(StringBuilder sb, int length) {
//        for (int i = 0 ; i < length ; i++) {
//            sb.append((char)(65 + random.nextInt(26)));
            sb.append(Long.toString(uniqueCounter++));
//        }
    }

    private static Document createDocument(int fields, int maxTermLength) {
        return null;
    }

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




