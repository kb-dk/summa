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




