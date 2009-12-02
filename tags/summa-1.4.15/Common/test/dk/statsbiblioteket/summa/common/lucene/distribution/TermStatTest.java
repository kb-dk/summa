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
package dk.statsbiblioteket.summa.common.lucene.distribution;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class TermStatTest extends TestCase {
//    private static Log log = LogFactory.getLog(TermStatTest.class);

    public TermStatTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (TMP.exists()) {
            Files.delete(TMP);
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(TermStatTest.class);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static File TMP = new File(System.getProperty(
            "java.io.tmpdir"), "termstattemp");
    public void testPersistence() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        TermStat ts = new TermStat(conf);
        ts.create(TMP);
        ts.setDocCount(87);
        ts.setSource("foo");
        ts.store();
        ts.close();

        ts.open(TMP);
        assertEquals("The docCount should match", 87, ts.getDocCount());
        assertEquals("The source should match", "foo", ts.getSource());
    }

/*    public void testReplace() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        TermStat ts = new TermStat(conf);
        ts.create(TMP);
        ts.add(new TermEntry("foo", 87));
        ts.add(new TermEntry("bar", 10));
        assertEquals("The initial count for bar should be correct",
                     10, ts.getTermCount("bar"));

        ts.getTermCounts().remove(ts.getTermCounts().indexOf(
                new TermEntry("bar", 0)));
        ts.add(new TermEntry("bar", 12));
        assertEquals("The updated count for bar should be correct",
                     12, ts.getTermCount("bar"));
    }*/

  /*  public void testProtectedAccess() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        TermStat ts = new TermStat(conf);
        ts.create(TMP);

        //noinspection DuplicateStringLiteralInspection
        Method method = TermStat.class.getDeclaredMethod("getTermCounts");

        method.setAccessible(true);
        Object result = method.invoke(ts);
        //noinspection unchecked
//        SortedPool<TermEntry> pool = (SortedPool<TermEntry>)result;
    }
    */

}
