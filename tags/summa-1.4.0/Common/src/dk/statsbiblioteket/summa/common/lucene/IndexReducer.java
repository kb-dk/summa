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
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.SimpleAnalyzer;

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
 * This can be used to convert indexes from older formats to format used by
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
                throw new IllegalArgumentException("The fraction-argument '"
                                                   + args[1]
                                                   + "' was not a number", e);
            }
        }
        reduce(location, fraction);
    }

    public static void reduce(File location, double fraction)
            throws IOException {
        if (!location.exists()) {
            //noinspection DuplicateStringLiteralInspection
            throw new FileNotFoundException("The stated index location '"
                                            + location.getAbsoluteFile()
                                            + "' does not exist");
        }
        if (!(fraction > 0.0 && fraction <= 1.0)) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("The fraction must be > 0 and "
                                               + "<= 1.0. it was '"
                                               + fraction + "'");
        }

        long starttime = System.currentTimeMillis();
        if (fraction != 1.0) {
            IndexReader ir = IndexReader.open(location);
            int docCount = ir.maxDoc();
            System.out.println("Reducing '" + location + "' to " + fraction
                               + " size (" + ir.maxDoc() + " => "
                               + (int)(ir.maxDoc() * fraction) + " documents)");
            int feedback = Math.max(1, docCount / 100);
            int lastReduction = -1;
            double fractionCounter = 0.0;
            for (int i = docCount-1; i >= 0; i--) {
                if ((int)fractionCounter != lastReduction) {
                    lastReduction = (int)fractionCounter;
                } else {
                    ir.deleteDocument(i);
                }
                fractionCounter += fraction;
                if (i % feedback == 0) {
                    System.out.println("Reduction progress: "
                                       + (docCount-1-i) + "/" + docCount);
                }
            }
            System.out.println("Closing reader...");
            ir.close();
            System.out.println("Reader closed");
        }

        System.out.println("Opening index writer...");
        IndexWriter iw = new IndexWriter(location, new SimpleAnalyzer(), false);
        System.out.println("Optimizing index...");
        iw.optimize();
        iw.close();
        System.out.println("Index optimized");

        long endtime = System.currentTimeMillis();
        double minutes = (endtime - starttime) / 1000.0 / 60.0;
        System.out.println("Index reduction of '" + location + "' to fraction "
                           + fraction + " took " + minutes + " minutes.");
    }
}



