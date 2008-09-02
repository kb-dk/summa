/* $Id: FlexiblePairTest.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.facetbrowser.util;

import java.util.LinkedList;
import java.util.Random;
import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * FlexiblePair Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FlexiblePairTest extends TestCase {
    private Log log = LogFactory.getLog(FlexiblePairTest.class);

    public FlexiblePairTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPerformance() throws Exception {
        int size = 100000;
        Random random = new Random();
        Profiler pf = new Profiler();
        LinkedList<FlexiblePair<String, Integer>> alphaResult =
                new LinkedList<FlexiblePair<String, Integer>>();
        for (int i = 0 ; i < size ; i++) {
            alphaResult.add(new FlexiblePair<String, Integer>(
                    Integer.toString(random.nextInt(10000000)),
                    random.nextInt(10000000),
                    FlexiblePair.SortType.PRIMARY_ASCENDING));
        }
        log.info("Finished adding " + size + " pairs " +
                           "in " + pf.getSpendTime());
        pf.reset();
        Collections.sort(alphaResult);
        log.info("Sorted in " + pf.getSpendTime());
    }

    public static Test suite() {
        return new TestSuite(FlexiblePairTest.class);
    }
}
