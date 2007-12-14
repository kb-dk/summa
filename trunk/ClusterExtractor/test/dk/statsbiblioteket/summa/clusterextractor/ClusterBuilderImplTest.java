/* $Id: ClusterBuilderImplTest.java,v 1.3 2007/12/04 10:26:43 bam Exp $
 * $Revision: 1.3 $
 * $Date: 2007/12/04 10:26:43 $
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

import dk.statsbiblioteket.summa.clusterextractor.data.Cluster;
import dk.statsbiblioteket.summa.clusterextractor.data.ClusterSet;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Set;

/**
 * ClusterBuilderImpl Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterBuilderImplTest extends TestCase {
    Configuration conf;

    public ClusterBuilderImplTest(String name) {
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

    public void testBuildCentroids() throws Exception {
        //construct cluster builder and build clusters
        ClusterBuilder builder = new ClusterBuilderImpl(conf);
        builder.buildCentroids();

        //find and check result
        String localCentroidSetPath = conf.getString(ClusterMerger.CLUSTER_SETS_PATH_KEY);
        File dir = new File(localCentroidSetPath);
        File result = dir.listFiles()[0];
        FileInputStream fis = new FileInputStream(result);
        ObjectInputStream ois = new ObjectInputStream(fis);

        ClusterSet clusterSet = (ClusterSet) ois.readObject();

        ois.close();

        for (Cluster cluster: clusterSet) {
            System.out.println(cluster);
        }
    }

    public void testBuildCentroidsFromStorage() throws Exception {
        //construct cluster builder and build clusters
        ClusterBuilderImpl builder = new ClusterBuilderImpl(conf);
        Set<String> candidateTerms = builder.getCandidateTerms();
        ClusterSet clusters = builder.buildCentroidsFromStorage(candidateTerms);
        System.out.println("clusters.size() = " + clusters.size());
    }

    public void testPattern() {
        
    }

    public static Test suite() {
        return new TestSuite(ClusterBuilderImplTest.class);
    }
}
