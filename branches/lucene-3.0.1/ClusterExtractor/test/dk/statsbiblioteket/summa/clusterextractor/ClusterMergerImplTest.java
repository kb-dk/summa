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

import dk.statsbiblioteket.summa.clusterextractor.data.Cluster;
import dk.statsbiblioteket.summa.clusterextractor.data.Dendrogram;
import dk.statsbiblioteket.summa.clusterextractor.math.SparseVectorMapImpl;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;

/**
 * ClusterMergerImpl Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterMergerImplTest extends TestCase {
    Configuration conf;

    private static final String X = "x";
    private static final String Y = "y";

    private static final double delta = 0.01;

    public ClusterMergerImplTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        //get test configurations
        String builderConfFileName = "clusterextractor.clusterbuilder.config.xml";
        conf = new Configuration(new FileStorage(builderConfFileName));
        String mergerConfFileName = "clusterextractor.clustermerger.config.xml";
        Configuration mergerConf = new Configuration(new FileStorage(mergerConfFileName));
        conf.importConfiguration(mergerConf);
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMergeCentroidSets() throws Exception {
        //assume clusters build
        //construct cluster merger and call merge method
        ClusterMerger merger = new ClusterMergerImpl(conf);
        merger.mergeCentroidSets();

        //find and check result
        String localDendrogramPath = conf.getString(ClusterMerger.DENDROGRAM_PATH_KEY);
        File dir = new File(localDendrogramPath);
        File[] fileList = dir.listFiles();
        Arrays.sort(fileList);
        File result = fileList[fileList.length-1];
        FileInputStream fis = new FileInputStream(result);
        ObjectInputStream ois = new ObjectInputStream(fis);

        Dendrogram dendrogram = (Dendrogram) ois.readObject();

        ois.close();

        System.out.println("dendrogram = " + dendrogram);
    }

    public void testJoin() throws Exception {
        System.out.println("testJoin: Equality is tested within range delta = " + delta);

        HashMap<String, Number> coords1 = new HashMap<String, Number>();
        coords1.put(X, 1);
        coords1.put(Y, 1);
        Cluster cls1 = new Cluster("name1", new SparseVectorMapImpl(coords1), 50);
        cls1.setSimilarityThreshold(Math.cos(Math.PI / 16));
        HashMap<String, Number> coords2 = new HashMap<String, Number>();
        coords2.put(X, 1);
        coords2.put(Y, .5);
        Cluster cls2 = new Cluster("name2", new SparseVectorMapImpl(coords2), 50);
        cls2.setSimilarityThreshold(Math.cos(Math.PI / 16));

        ClusterMergerImpl merger = new ClusterMergerImpl(conf);
        Cluster joined = merger.join(cls1, cls2, "newName");
        double newSimilarityThreshold = joined.getSimilarityThreshold();
        double expectedThreshold = Math.cos(Math.PI / 8);
        assertEquals("The similarity is somewhere in the vicinity of cos(PI/8)...",
                expectedThreshold, newSimilarityThreshold, delta);
    }

    public static Test suite() {
        return new TestSuite(ClusterMergerImplTest.class);
    }
}




