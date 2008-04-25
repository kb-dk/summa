/* $Id: IncrementalCentroidTest.java,v 1.2 2007/10/04 09:48:13 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 09:48:13 $
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
