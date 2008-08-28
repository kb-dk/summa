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
package dk.statsbiblioteket.summa.facetbrowser.connection;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * IndexConnectionImplSumma Tester.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te",
        comment="The IndexConnection has been deprecated")
public class IndexConnectionImplSummaTest extends TestCase {
    public IndexConnectionImplSummaTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetResults() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetTopResults() throws Exception {
        IndexConnection connection = new IndexConnectionImplSumma();
        Document doc = connection.getDoc(1);
        assertNotNull("There should be at least 2 documents in the index", doc);
        TopDocs topDocs = connection.getTopResults("Peter Gabriel", 10);
        assertTrue("Should get at least 10 hits for POeter Gabriel",
                   topDocs.totalHits >= 10);
    }

    public void dumpTags() throws Exception {
        IndexConnection connection = new IndexConnectionImplSumma();
        IndexReader ir = connection.getIndexReader();
        for (int i = 0 ; i < ir.maxDoc() ; i++) {
            if (i % 1000 == 0) {
                System.out.println(i + "/" + ir.maxDoc());
            }
            TermFreqVector tfv = ir.getTermFreqVector(i, "cluster");
            if (tfv != null) {
                System.out.print(tfv.getField());
                for (String term: tfv.getTerms()) {
                    System.out.print(" " + term);
                }
                System.out.println("");
            }
        }
        System.out.println("Finished");
    }

/*    public void testInstance() throws Exception {
        SearchEngineImpl.getInstance();
        System.out.println("Calling second");
        SearchEngineImpl.getInstance();
    }
  */
    public void testGetDoc() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetTermFreqVector() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(IndexConnectionImplSummaTest.class);
    }
}
