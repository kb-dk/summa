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
package dk.statsbiblioteket.summa.common.lucene;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Simple reduction of an existing index by deletion of documents.
 * </p><p>
 * The reducer is called with the wanted fragment size and the index is reduced
 * by deleting documents is a uniform manner. Example: If the fraction is 0.5,
 * the first document is deleted, the second retained, the third deleted and
 * so forth. This ensures that the reduced index has a layout that corresponds
 * to the full index.
 * </p><p>
 * If the fraction 1.0 is given, an optimize is called on the unmodified index.
 * This can be used to convert indexes from older formats to the format used by
 * the Lucene-version in the classpath.
 * </p><p>
 * Note: This process is destructive, so make a copy of the index before
 * stating the reduction.
 */
public class IndexReducer {

    /**
     * Reduce the given index to the given fraction.
     * @param args The first argument is the location of an index, the second
     *             argument is the fraction that the index should be reduced to.
     * @throws FileNotFoundException if the index could not be located.
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: IndexReducer indexLocation [fraction]");
            return;
        }
        File location = new File(args[0]);
        double fraction = 1;
        if (args.length == 1) {
            System.out.println("No fraction given. Defaulting to 1.0");
        } else {
            try {
                fraction = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("The fraction-argument '" + args[1] + "' was not a number", e);
            }
        }
        reduce(location, fraction);
    }

    public static void reduce(File location, double fraction) throws IOException {
        if (!location.exists()) {
            //noinspection DuplicateStringLiteralInspection
            throw new FileNotFoundException(
                "The stated index location '" + location.getAbsoluteFile() + "' does not exist");
        }
        if (!(fraction > 0.0 && fraction <= 1.0)) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("The fraction must be > 0 and <= 1.0. it was '" + fraction + "'");
        }

        long starttime = System.currentTimeMillis();
        if (fraction != 1.0) {
            performReduce(location, fraction);
        }

        System.out.println("Opening index writer...");
        IndexWriterConfig writerConfig = new IndexWriterConfig(
            Version.LUCENE_40, new SimpleAnalyzer(Version.LUCENE_40));
        // FIXME: Set max field length to unlimited
//        writerConfig.setMaxFieldLength(
//                                   IndexWriterConfig.UNLIMITED_FIELD_LENGTH);
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        IndexWriter iw = new IndexWriter(new NIOFSDirectory(location), writerConfig);
        System.out.println("Optimizing index...");
        iw.forceMerge(1);
        iw.close();
        System.out.println("Index optimized");

        long endtime = System.currentTimeMillis();
        double minutes = (endtime - starttime) / 1000.0 / 60.0;
        System.out.println(
            "Index reduction of '" + location + "' to fraction " + fraction + " took " + minutes + " minutes.");
    }

    private static void performReduce(File location, final double fraction) throws IOException {
        final DirectoryReader ir = DirectoryReader.open(new NIOFSDirectory(location));
        IndexWriter writer = new IndexWriter(
            new NIOFSDirectory(location),
            new IndexWriterConfig(Version.LUCENE_40, new WhitespaceAnalyzer(Version.LUCENE_40)));
        final int docCount = ir.maxDoc();
        System.out.println("Reducing '" + location + "' to " + fraction + " size (" + ir.maxDoc() + " => "
                           + (int)(ir.maxDoc() * fraction) + " documents...)");
        writer.deleteDocuments(new FilteredQuery(new MatchAllDocsQuery(), new Filter() {
            final int deleteInterval = (int) (1.0 * ir.maxDoc() / (ir.maxDoc() * (1.0 - fraction)));
            int pos = 0;

            @Override
            public DocIdSet getDocIdSet(final AtomicReaderContext context, Bits acceptDocs) throws IOException {
                DocIdSet docs = new DocIdSet() {
                    @Override
                    public DocIdSetIterator iterator() throws IOException {
                        final int startPos = pos;
                        return new DocIdSetIterator() {
                            private int pos = startPos;
                            private final int maxDoc = context.reader().maxDoc();

                            @Override
                            public int docID() {
                                return 0;  //To change body of implemented methods use File | Settings | File Templates.
                            }

                            @Override
                            public int nextDoc() throws IOException {
                                // TODO: Implement skipping
                                if (pos < maxDoc) {
                                    return pos++;
                                }
                                return DocIdSetIterator.NO_MORE_DOCS;
                            }

                            @Override
                            public int advance(int target) throws IOException {
                                throw new UnsupportedOperationException("Makes no sense for reduction");
                            }
                        };
                    }
                };
                pos += context.reader().maxDoc();
                return docs;
            }
        }));
        System.out.println("Documents marked as deleted. Closing writer...");
        writer.close();
        ir.close();
        System.out.println("Writer closed");
    }
}




