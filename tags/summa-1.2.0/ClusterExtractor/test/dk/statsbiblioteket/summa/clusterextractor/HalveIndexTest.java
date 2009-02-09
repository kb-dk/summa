/* $Id: HalveIndexTest.java,v 1.1 2007/12/03 08:51:19 bam Exp $
 * $Revision: 1.1 $
 * $Date: 2007/12/03 08:51:19 $
 * $Author: bam $
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
package dk.statsbiblioteket.summa.clusterextractor;

import junit.framework.TestCase;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * HalveIndexTest has a method to halve an index for test purposes.
 * This is not a JUnit test.
 */
public class HalveIndexTest extends TestCase {
    protected static final Log log = LogFactory.getLog(HalveIndexTest.class);

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHalveIndex() throws IOException {
        halveIndex("/tmp/indextmp/");
    }

    /**
     * Delete half of the index in the given directory.
     * I.e. to create a small test index, copy the full index to 'dir';
     * run this method with 'dir' as parameter.
     * @param dir name of index directory
     * @throws java.io.IOException on index reader errors
     */
    public void halveIndex(String dir) throws IOException {
        long starttime = System.currentTimeMillis();
        IndexReader ir = IndexReader.open(dir);
        log.info("halveIndex start; dir = " + dir +
                "ir.maxDoc() = " + ir.maxDoc());
        for (int i=ir.maxDoc()-1; i>=0; i--) {
            if (i%2!=0) {
                ir.deleteDocument(i);
            }
            if (i%100000==0) {
                log.trace("i = " + i);
            }
        }
        //flush deletes
        ir.close();
        log.trace("deletes flushed");

        //get a writer to optimize index - note: expensive!
        IndexWriter iw = new IndexWriter(dir, new SimpleAnalyzer(), false);
        iw.optimize();
        iw.close();
        log.trace("index optimised");

        ir = IndexReader.open(dir);
        long endtime = System.currentTimeMillis();
        double minutes = (endtime - starttime) / 1000.0 / 60.0;
        log.info("halveIndex end; ir.maxDoc() = " + ir.maxDoc() +
                "; time: "+minutes+" minutes.");
    }

}



