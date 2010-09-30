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
package dk.statsbiblioteket.summa.clusterextractor.math;

import dk.statsbiblioteket.summa.clusterextractor.data.ClusterRepresentative;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;
import java.util.Map;

/**
 * IncrementalCentroid Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class IncrementalCentroidTest extends TestCase {
    public IncrementalCentroidTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void testSmallScale() {
        IncrementalCentroid incCenStd = new IncrementalCentroid("dessert");
        Map<String, Number> entries = new HashMap<String, Number>(10);
        entries.put("æble", 2);
        entries.put("pære", 1);
        entries.put("frugtsalat", 1);
        SparseVector vec = new SparseVectorMapImpl(entries);
        incCenStd.addPoint(vec);

        entries = new HashMap<String, Number>(10);
        entries.put("æble", 1);
        entries.put("pære", 1);
        entries.put("bla", 1);
        entries.put("banan", 2);
        vec = new SparseVectorMapImpl(entries);
        incCenStd.addPoint(vec);

        entries = new HashMap<String, Number>(10);
        entries.put("æble", 2);
        entries.put("frugtsalat", 1);
        entries.put("noget", 1);
        vec = new SparseVectorMapImpl(entries);
        incCenStd.addPoint(vec);

        ClusterRepresentative cls = incCenStd.getCluster();
        System.out.println("cls = " + cls);

        System.out.println("cls.getCentroid().getCoordinates() = "
                + cls.getCentroid().getCoordinates());
    }

    public static Test suite() {
        return new TestSuite(IncrementalCentroidTest.class);
    }
}




