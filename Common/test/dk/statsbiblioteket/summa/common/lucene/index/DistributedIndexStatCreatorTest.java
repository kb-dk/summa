/* $Id: DistributedIndexStatCreatorTest.java,v 1.2 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
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
package dk.statsbiblioteket.summa.common.lucene.index;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * DistributedIndexStatCreator Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DistributedIndexStatCreatorTest extends TestCase {

    DistributedIndexStatCreator indexC;
    String indexpart;


    public DistributedIndexStatCreatorTest(String name) {

        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        indexC = new DistributedIndexStatCreator();
        indexpart = "/home/halu/summa_dist";
    }

    public void tearDown() throws Exception {
        super.tearDown();
        indexC = null;
    }

    public static Test suite() {
        return new TestSuite(DistributedIndexStatCreatorTest.class);
    }

    public void testAdd() throws IOException {
       for (File f : new File(indexpart).listFiles()){
           if (f.isDirectory()){
               indexC.add(IndexReader.open(f));
           }
       }
    }


    public void testWriteStats() throws IOException {
        testAdd();
        indexC.writeStats();
    }

}



