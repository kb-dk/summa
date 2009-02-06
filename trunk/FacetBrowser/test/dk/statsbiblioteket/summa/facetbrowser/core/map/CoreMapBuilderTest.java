/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.BaseObjects;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Strings;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

public class CoreMapBuilderTest extends TestCase {
    public CoreMapBuilderTest(String name) {
        super(name);
    }

    BaseObjects bo;
    CoreMapBuilder builder;
    @Override
    public void setUp() throws Exception {
        super.setUp();
        bo = new BaseObjects();
        builder = new CoreMapBuilder(
                Configuration.newMemoryBased(), bo.getStructure());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        bo.close();
    }

    public static Test suite() {
        return new TestSuite(CoreMapBuilderTest.class);
    }

    public void testAddition() throws Exception {
        CoreMapBitStuffedTest.testAddition(builder);

    }

    public void testAddition2() throws Exception {
        CoreMapBitStuffedTest.testAddition2(builder);
    }

    public void testMonkey() throws Exception {
        int[] RUNS = {1000, 10000, 100000, 1000000, 10000000};
        CoreMap map = new CoreMapBuilder(
                Configuration.newMemoryBased(), bo.getStructure());
        for (int runs: RUNS) {
            testMonkey(runs, map);
        }
    }

    public void testMonkey(int runs, CoreMap map) throws Exception {
        CoreMapBitStuffedTest.testMonkey(
                runs, bo.getStructure().getFacets().size(), map);
    }

    public void testMonkeyValid() throws Exception {
        testMonkeyValid(10);
    }

    public void testMonkeyValid1000() throws Exception {
        testMonkeyValid(1000);
    }

    public void testMonkeyValid50000() throws Exception {
        testMonkeyValid(50000);
    }
    /*
     * Performs a monkey-test for a CoreMapBuilder and a CoreMapBitStuffed and
     * compares the results.
     */
    public void testMonkeyValid(int runs) throws Exception {
        CoreMapBuilder builderMap = new CoreMapBuilder(
                Configuration.newMemoryBased(), bo.getStructure());
        testMonkey(runs, builderMap);
        CoreMap bitMap = bo.getCoreMap();
        testMonkey(runs, bitMap);

        assertEquals("The highest document ID in the maps should match",
                     builderMap.getDocCount(), bitMap.getDocCount());
        for (int docID = 0 ; docID < builderMap.getDocCount() ; docID++) {
            for (String facet: bo.getStructure().getFacetNames()) {
                int facetID = bo.getStructure().getFacetID(facet);
                //noinspection DuplicateStringLiteralInspection
                assertEquals("The tags for doc " + docID + " and facet '"
                             + facet + "' should match",
                             Arrays.toString(bitMap.get(docID, facetID)),
                             Arrays.toString(builderMap.get(docID, facetID)));
            }
        }
    }
}
