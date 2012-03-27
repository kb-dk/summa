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
package dk.statsbiblioteket.summa.common.lucene.index;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * DistributedIndexStatCreator Tester.
 * @deprecated {@link dk.statsbiblioteket.summa.common.lucene.index.DistributedIndexStatCreator}
 * is deprecated
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DistributedIndexStatCreatorTest extends TestCase {

    //DistributedIndexStatCreator indexC;
    String indexpart;


    public DistributedIndexStatCreatorTest(String name) {

        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        //indexC = new DistributedIndexStatCreator();
        indexpart = "/home/halu/summa_dist";
    }

    public void tearDown() throws Exception {
        super.tearDown();
        //indexC = null;
    }

    public static Test suite() {
        return new TestSuite();
        //return new TestSuite(DistributedIndexStatCreatorTest.class);
    }

    public void testDummy() {
        assertTrue(true);
    }

/*    public void testAdd() throws IOException {
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
  */
}




