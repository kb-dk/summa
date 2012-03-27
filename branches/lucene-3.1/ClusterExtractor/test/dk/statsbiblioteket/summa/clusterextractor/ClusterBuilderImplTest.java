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

import dk.statsbiblioteket.summa.clusterextractor.data.ClusterRepresentative;
import dk.statsbiblioteket.summa.clusterextractor.data.ClusterSet;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * ClusterBuilderImpl Tester.
 * To test the ClusterBuilderImpl, test with different configurations and
 * different indexes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterBuilderImplTest extends TestCase {
    Configuration conf;

    public ClusterBuilderImplTest(String name) {
        super(name);
    }

    /**
     * Load configurations.
     */
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

    /**
     * Build and save {@link ClusterSet} based on the set up configurations.
     * The buildCentroids() method is the search based clustering.
     * @throws java.rmi.RemoteException if ClusterBuilder constructor
     *                                  failed to export object
     * @throws java.io.IOException if not able to load file from given location
     * @throws ClassNotFoundException if class of a serialized object
     *                                cannot be found
     */
    public void testBuildCentroids() throws IOException, ClassNotFoundException {
        //construct cluster builder and build clusters
        ClusterBuilder builder = new ClusterBuilderImpl(conf);
        builder.buildCentroids();

        //find and check result
        checkExistingClusterSet();
    }

    /**
     * Build and save {@link ClusterSet} based on the set up configurations.
     * The buildCentroids2() method is the storage based clustering.
     * @throws java.rmi.RemoteException if ClusterBuilder constructor
     *                                  failed to export object
     * @throws java.io.IOException if not able to load file from given location
     * @throws ClassNotFoundException if class of a serialized object
     *                                cannot be found
     */
    public void testBuildCentroids2() throws IOException, ClassNotFoundException {
        //construct cluster builder and build clusters
        ClusterBuilderImpl builder = new ClusterBuilderImpl(conf);
        builder.buildCentroids2();

        //find and check result
        checkExistingClusterSet();
    }

    public void testExistingClusterSet() throws ClassNotFoundException, IOException {
        //find and check result
        checkExistingClusterSet();
    }
    /**
     * Check cluster set in location specified in configurations.
     * @throws java.io.IOException if not able to load file from given location
     * @throws ClassNotFoundException if class of a serialized object
     *                                cannot be found
     */
    public void checkExistingClusterSet() throws ClassNotFoundException, IOException {
        //find and check result
        String localCentroidSetPath = conf.getString(ClusterBuilder.LOCAL_CLUSTER_SET_PATH_KEY);
        File dir = new File(localCentroidSetPath);
        File result = dir.listFiles()[0];
        FileInputStream fis = new FileInputStream(result);
        ObjectInputStream ois = new ObjectInputStream(fis);

        ClusterSet clusterSet = (ClusterSet) ois.readObject();

        ois.close();

        for (ClusterRepresentative cluster: clusterSet) {
            System.out.println(cluster);
        }
    }

    public static Test suite() {
        return new TestSuite(ClusterBuilderImplTest.class);
    }
}




