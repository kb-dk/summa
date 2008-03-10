/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.common.lucene;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

/**
 * A class for testing index speed under various circumstances. This is more of
 * a playground for experiments than a real tester. See the benchmark contrib to
 * Lucene for a Real Solution.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexSpeed {
    private static final Log log = LogFactory.getLog(IndexSpeed.class);

    private static int maxdocs = Integer.MAX_VALUE-1;
    private static int ramBuffer = 8;
    private static int flush = 0;
    private static boolean docReuse = false;
    private static int threads = 1;
    private static int termlength = 0;
    private static String indexLocation = null;

    private static int feedbackInterval = 10000;
    private static final String[] SEMI_RANDOMS = new String[]{
            "foo", "bar", "zoo", "kablooie", "Hamster", "Huey", "gooey", "and",
            "the"};
    private IndexWriter writer;

    private static int counter = 0;
    private static Profiler profiler;

    public static void main(String[] args) throws IOException, InterruptedException {
        if (!(args.length >= 1)) {
            System.err.println("Usage: IndexSpeed [-m maxdocs] [-r rambuffer]"
                               + " [-f flushcount] [-d] [-u termlength] "
                               + "indexlocation");
            System.err.println("-m maxdocs\tThe maximum number of documents to "
                               + "create (default: Integer.MAX_VALUE-1)");
            System.err.println("-r rambuffer\tRAM-buffer size in MB (default: "
                               + ramBuffer + ")");
            System.err.println("-f flushcount\tFlush writer every flushcount "
                               + "document (default: 0 (no flushing))");
            System.err.println("-d\tDocument re-use");
            System.err.println("-t\tThreads (Default: " + threads + ")");
            System.err.println("-u termlength\tCreate a unique random term of "
                               + "size termlength (Default: " + termlength
                               + ")");
            System.err.println("indexlocation\tA folder for the index");
            System.exit(-1);
        }

        List<String> arguments = new LinkedList<String>(Arrays.asList(args));
        while (arguments.size() > 0) {
            if ("-m".equals(arguments.get(0))) {
                arguments.remove(0);
                try {
                    maxdocs = Integer.parseInt(arguments.get(0));
                } catch (NumberFormatException e) {
                    System.err.println("Expected maxdocs (an integer)."
                                       + " Got '"
                                       + arguments.get(0) + "'. Exiting");
                    System.exit(-1);
                }
                arguments.remove(0);
            } else if ("-r".equals(arguments.get(0))) {
                arguments.remove(0);
                try {
                    ramBuffer = Integer.parseInt(arguments.get(0));
                } catch (NumberFormatException e) {
                    System.err.println("Expected rambuffer (an integer). Got '"
                                       + arguments.get(0) + "'. Exiting");
                    System.exit(-1);
                }
                arguments.remove(0);
            } else if ("-t".equals(arguments.get(0))) {
                arguments.remove(0);
                try {
                    threads = Integer.parseInt(arguments.get(0));
                } catch (NumberFormatException e) {
                    System.err.println("Expected threads (an integer). Got '"
                                       + arguments.get(0) + "'. Exiting");
                    System.exit(-1);
                }
                arguments.remove(0);
            } else if ("-u".equals(arguments.get(0))) {
                arguments.remove(0);
                try {
                    termlength = Integer.parseInt(arguments.get(0));
                } catch (NumberFormatException e) {
                    System.err.println("Expected termlength (an integer). Got '"
                                       + arguments.get(0) + "'. Exiting");
                    System.exit(-1);
                }
                arguments.remove(0);
            } else if ("-f".equals(arguments.get(0))) {
                arguments.remove(0);
                try {
                    flush = Integer.parseInt(arguments.get(0));
                } catch (NumberFormatException e) {
                    System.err.println("Expected flushcount (an integer). Got '"
                                       + arguments.get(0) + "'. Exiting");
                    System.exit(-1);
                }
                arguments.remove(0);
            } else if ("-d".equals(arguments.get(0))) {
                arguments.remove(0);
                docReuse = true;
            } else if (indexLocation == null) {
                indexLocation = arguments.remove(0);
            } else {
                System.err.println("Unexpected token: '" + arguments.get(0)
                                   + "'. Exiting");
                System.exit(-1);
            }
        }
        new IndexSpeed().speedTest();
    }

    /**
     * Ping the counter after each document add.
     * @return the next unique id, -1 if indexer thread should stop.
     */
    private synchronized int ping() {
        profiler.beat();
        if (counter % feedbackInterval == 0 && counter > 0) {
            System.out.println(counter + "/" + maxdocs + "("
                               + (counter * 100 / maxdocs) + "%) at "
                               + profiler.getBps(true) + " doc/sec ("
                               + profiler.getBps(false) + " total). "
                               + "ETA: " + profiler.getETAAsString(true));
        }
        if (counter++ >= maxdocs) {
            return -1;
        }
        return counter;
    }

    class SpeedThread extends Thread {
        private Random random = new Random();
        private final char[] randChars =
                ("abcdefgh ijklmn opqrstu. vxyz æøå1234 ABCD EFGHIJ "
                 + "KLMNOP QRSTYV WXYZÆØÅ 123456 7890 !?-,").
                        toCharArray();
        private String getRandom() {
            if (termlength == 0) {
                return "";
            }
            StringWriter sw = new StringWriter(termlength);
            for (int i = 0 ; i < termlength ; i++) {
                sw.append(randChars[random.nextInt(randChars.length)]);
            }
            return sw.toString();
        }

        public void run() {
            Field uniqueField =
                    new Field("unique", "d-1",
                              Field.Store.YES, Field.Index.UN_TOKENIZED);
            Field semiRandomField =
                    new Field("semirandom", SEMI_RANDOMS[0],
                              Field.Store.YES, Field.Index.UN_TOKENIZED);
            Field randomField =
                    new Field("random", getRandom(),
                              Field.Store.NO, Field.Index.TOKENIZED);
            Document document = new Document();
            int id;
            while ((id = ping()) != -1) {
                if (!docReuse) {
                    document = new Document();
                    uniqueField = new Field(
                            "unique", "d" + id,
                            Field.Store.YES, Field.Index.UN_TOKENIZED);
                    semiRandomField = new Field(
                            "semirandom",
                            SEMI_RANDOMS[random.nextInt(SEMI_RANDOMS.length)],
                            Field.Store.YES, Field.Index.UN_TOKENIZED);
                    document.add(uniqueField);
                    document.add(semiRandomField);
                    if (termlength > 0) {
                        randomField = new Field("random", getRandom(),
                                                Field.Store.NO,
                                                Field.Index.TOKENIZED);
                        document.add(randomField);
                    }
                } else {
                    document = new Document();
                    uniqueField.setValue(
                            "d" + id);
                    semiRandomField.setValue(
                            SEMI_RANDOMS[random.nextInt(SEMI_RANDOMS.length)]);
                    document.add(uniqueField);
                    document.add(semiRandomField);
                    if (termlength > 0) {
                        randomField.setValue(getRandom());
                        document.add(randomField);
                    }
                }
                try {
                    writer.addDocument(document);
                    if (flush > 0 && id % flush == 0) {
                        writer.flush();
                    }
                } catch (IOException e) {
                    log.error("Exception writing document, aborting", e);
                    break;
                }
            }
        }
    }

    private void speedTest() throws IOException, InterruptedException {
        String meta = maxdocs + " documents with rambuffer "
                      + ramBuffer + " MB, flush " + flush + ", " + threads
                      + " threads"
                      + (termlength > 0 ?
                         ", random field of length " + termlength :
                         "")
                      + " and docReuse " + docReuse
                      + " at '" + indexLocation + "'";
        feedbackInterval = Math.min(Math.max(100, maxdocs / 100),
                                    feedbackInterval);
        log.info("Building " + meta);
        writer = new IndexWriter(new File(indexLocation),
                                 new StandardAnalyzer(), true);
        writer.setRAMBufferSizeMB(ramBuffer);
/*        if (flush) {
            writer.setMaxBufferedDocs(1);
        }*/

        List<SpeedThread> threadList = new ArrayList<SpeedThread>(threads);
        profiler = new Profiler();
        profiler.setExpectedTotal(maxdocs);
        profiler.setBpsSpan(1000);
        for (int i = 0 ; i < threads ; i++) {
            SpeedThread st = new SpeedThread();
            threadList.add(st);
            st.start();
        }
        log.debug("Started " + threads + " threads");

        for (SpeedThread t: threadList) {
            t.join();
        }

        String status = "Indexed " + maxdocs + " documents at "
                        + profiler.getBps() + " doc/sec in "
                        + profiler.getSpendTime() + ".";
        System.out.println(status);
        log.info(status);
        writer.close(true);
        System.out.print("Optimizing... ");
        Profiler optimizeProfiler = new Profiler();
        writer = new IndexWriter(new File(indexLocation),
                                 new StandardAnalyzer(), false);
        System.out.println("Finished optimizing in "
                           + optimizeProfiler.getSpendTime()
                           + ". Total time spend: " + profiler.getSpendTime());
        System.out.println("Index: " + meta);
        writer.close();
    }
}
