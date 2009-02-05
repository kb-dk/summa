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

public class CoreMapBuilderTest extends TestCase {
    public CoreMapBuilderTest(String name) {
        super(name);
    }

    BaseObjects bo;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        bo = new BaseObjects();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        bo.close();
    }

    public static Test suite() {
        return new TestSuite(CoreMapBuilderTest.class);
    }

    public void testMonkey() throws Exception {
        int[] RUNS = {1000, 10000, 100000, 1000000};
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
}
