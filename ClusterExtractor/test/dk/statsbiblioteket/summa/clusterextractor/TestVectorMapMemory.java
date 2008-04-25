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
