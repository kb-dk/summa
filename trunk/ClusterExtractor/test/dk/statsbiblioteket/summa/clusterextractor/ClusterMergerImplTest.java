/* $Id: ClusterMergerImplTest.java,v 1.3 2007/12/04 10:26:43 bam Exp $
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

import dk.statsbiblioteket.summa.clusterextractor.data.Dendrogram;
import dk.statsbiblioteket.summa.clusterextractor.math.CentroidVector;
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
        CentroidVector vec1 = new CentroidVector("name1", 50, coords1);
        vec1.setSimilarityThreshold(Math.cos(Math.PI / 16));
        HashMap<String, Number> coords2 = new HashMap<String, Number>();
        coords2.put(X, 1);
        coords2.put(Y, .5);
        CentroidVector vec2 = new CentroidVector("name2", 50, coords2);
        vec2.setSimilarityThreshold(Math.cos(Math.PI / 16));

        ClusterMergerImpl merger = new ClusterMergerImpl(conf);
        CentroidVector joined = merger.join(vec1, vec2, "newName");
        double newSimilarityThreshold = joined.getSimilarityThreshold();
        double expectedThreshold = Math.cos(Math.PI / 8);
        assertEquals("The similarity is somewhere in the vicinity of cos(PI/8)...",
                expectedThreshold, newSimilarityThreshold, delta);
    }

    public static Test suite() {
        return new TestSuite(ClusterMergerImplTest.class);
    }
}
