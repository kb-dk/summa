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
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.clusterextractor.math.SparseVector;
import dk.statsbiblioteket.summa.clusterextractor.math.IncrementalCentroid;
import dk.statsbiblioteket.util.Profiler;

/**
 * Class for testing the memory needed, if we want a vector-map.
 * A vector map is a map from record id or doc id to sparse vectors.
 */
public class TestVectorMapMemory  extends TestCase {
    protected static final Log log = LogFactory.getLog(TestVectorMapMemory.class);
    Configuration conf;

    protected void setUp() throws Exception {
        super.setUp();
        //get test configurations
        String builderConfFileName = "clusterextractor.clusterbuilder.config.xml";
        conf = new Configuration(new FileStorage(builderConfFileName));
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testVectorMapMemory() throws IOException {
        IndexReader ir = IndexReader.open(
                conf.getString(ClusterBuilder.LOCAL_INDEX_PATH_KEY));

        int maxDoc = ir.maxDoc();
        if (log.isTraceEnabled()) {
            log.trace("maxDoc = " + maxDoc);
        }
        Profiler feedback = new Profiler();
        feedback.setExpectedTotal(maxDoc);
        Runtime runtime = Runtime.getRuntime();
        ClusterBuilderImpl builder = new ClusterBuilderImpl(conf);

        Map<Integer, SparseVector> vectorMap =
                new HashMap<Integer, SparseVector>(1000000);
        for (int index=0; index<maxDoc; index++) {
            feedback.beat();

            vectorMap.put(index, builder.getVec(ir, index));

            log.trace(index + "/" + maxDoc + "; Mem: " +
                    (runtime.totalMemory()-runtime.freeMemory()) + "; ETA: "
                    + feedback.getETAAsString(false));
        }
        log.trace("End; Mem: " + (runtime.totalMemory()-runtime.freeMemory()));
    }

    public static Test suite() {
        return new TestSuite(ClusterBuilderImplTest.class);
    }
}




