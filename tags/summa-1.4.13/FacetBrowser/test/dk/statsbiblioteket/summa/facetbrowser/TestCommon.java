/* $Id: TestCommon.java,v 1.6 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.facetbrowser;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

@SuppressWarnings({"DuplicateStringLiteralInspection", "deprecation"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TestCommon extends TestCase {
    private static final Logger log = Logger.getLogger(TestCommon.class);

/*    public static String oneliner(IndexConnection connection, int docID) {
        Document doc = connection.getDoc(docID);
        TermFreqVector freetext =
                connection.getTermFreqVector(docID, ClusterCommon.FREETEXT);
        return ClusterCommon.oneliner(doc, docID, freetext);
    }
  */
    public void testAssign() throws Exception {
        int a = 10;
        assertEquals("a should be assigned to the start value", 10, a);
        int b = a = 5;
        assertEquals("a should be assigned to the new value", 5, a);
        assertEquals("b should be assigned to a's new value", 5, b);
    }

    public void testNewString() throws Exception {
        assertEquals("New with empty should give empty", "",
                     new String(new byte[0]));
        try {
            new String((byte[])null);
            fail("New String with null should throw a NullPointerException");
        } catch (NullPointerException e) {
            // Expected, so do nothing
        }
    }
    /*
    public void testGetNamesFromProperties() throws Exception {
        ClusterCommon.getProperties().put("Test", "a,b, c");
        String[] simple = ClusterCommon.getNamesFromProperties("Test");
        assertEquals("The number of names should be 3", 3, simple.length);
        assertEquals("The second name should be b", "b", simple[1]);
        assertEquals("The third name should be c", "c", simple[2]);

        int[] simpleValues = ClusterCommon.getValuesFromProperties("Test", 87);
        for (int value: simpleValues) {
            assertEquals("All simple values should be 87", 87, value);
        }

        ClusterCommon.getProperties().put("Test2", "a, r (45), c, d");
        String[] paren = ClusterCommon.getNamesFromProperties("Test2");
        assertEquals("The number of names should be 4", 4, paren.length);
        assertEquals("The second name should be r", "r", paren[1]);
        assertEquals("The fourth name should be d", "d", paren[3]);

        int[] parenValues = ClusterCommon.getValuesFromProperties("Test2", 87);
        int counter = 0;
        for (int value: parenValues) {
            if (counter++ == 1) {
                assertEquals("The value for r should be 45", 45, value);
            } else {
                assertEquals("All paren values, except number 2 should be 87",
                             87, value);
            }
        }

        ClusterCommon.getProperties().put("TestS", "a,b, c,  d123 , e");
        String[] spaces = ClusterCommon.getNamesFromProperties("TestS");
        assertEquals("The number of spacenames should be 5", 5, spaces.length);
        assertEquals("The second sname should be b", "b", spaces[1]);
        assertEquals("The third sname should be c", "c", spaces[2]);
        assertEquals("The fourth sname should be d123", "d123", spaces[3]);
        assertEquals("The fifth sname should be e", "e", spaces[4]);


    }
      */
    /*
    public static String oneliner(IndexConnection connection, String recordID) throws IOException {
        String searchTerm = ClusterCommon.colonEscape(recordID);
        Hits hits =
                 connection.getResults(ClusterCommon.RECORDID + ":\"" +
                                       searchTerm + "\"");
        if (hits == null || hits.length() == 0) {
            log.error("Could not find a document with recordID " + recordID);
            return "No document for " + searchTerm;
        }
        if (hits.length() > 1) {
            log.error("More than one document with recordID" + recordID);
        }
        return oneliner(connection, hits.id(0));
    }

    public void testOneliner() throws Exception {
        IndexConnection connection = IndexConnectionFactory.getIndexConnection();
        String oneliner = oneliner(connection, 0);
        assertNotNull("Oneliner should give something", oneliner);
        assertEquals("The number of lines should be 1",
                     1, oneliner.split("\n").length);
    }
    public void dumpOneliners() throws Exception {
        IndexConnection connection = IndexConnectionFactory.getIndexConnection();
        for (int i = 0 ; i < 10 ; i++) {
            System.out.println(oneliner(connection, i));
        }
    }

    public void testEnum() throws Exception {
        Element.SortOrder orderEnum = Element.SortOrder.valueOf("SCORE");
        assertEquals("Enum should be extracted correctly", Element.SortOrder.SCORE, orderEnum);
        try {
            //noinspection UnusedAssignment
            orderEnum = Element.SortOrder.valueOf("Nonexisting");
            fail("Requesting an invalid enum should throw an exception");
        } catch (IllegalArgumentException ex) {
            // Do nothing, as this is expected
        }
    }
      */
    public void measureBitTweaking() {
        int arraySize = 1000000;
        int iterations = 500;
        int[] array = new int[arraySize];

        Profiler pf = new Profiler();
        pf.setExpectedTotal(arraySize+iterations);
        for (int i = 0 ; i < iterations ; i++) {
            for (int index = 0 ; index < arraySize ; index++) {
                array[index] = index;
            }
        }
        System.out.println("Plain assignment: " + pf.getSpendTime());

        pf.reset();
        for (int i = 0 ; i < iterations ; i++) {
            for (int index = 0 ; index < arraySize ; index++) {
                array[index] = index | i << 27;
            }
        }
        System.out.println("Tweak assignment: " + pf.getSpendTime());

        pf.reset();
        int dummy = 0;
        pf.setExpectedTotal(arraySize+iterations);
        for (int i = 0 ; i < iterations ; i++) {
            for (int index = 0 ; index < arraySize ; index++) {
                dummy = array[index];
            }
        }
        if (dummy == 123454) {
            System.out.println("Dummy");
        }
        System.out.println("Plain access: " + pf.getSpendTime());
        pf.reset();

        dummy = 0;
        int dummy2 = 0;
        int mask = Integer.MAX_VALUE << 5 >> 5;
        pf.setExpectedTotal(arraySize+iterations);
        for (int i = 0 ; i < iterations ; i++) {
            for (int index = 0 ; index < arraySize ; index++) {
                dummy = array[index] & mask;
                dummy2 = array[index] >> 27;
            }
        }
        if (dummy == 123454) {
            System.out.println("Dummy " + dummy2);
        }
        System.out.println("Tweak access: " + pf.getSpendTime());

    }

    public void speedArrays() {
        int arraySize = 500000;
        int[][] arrays = new int[10][];
        int runs = 10;

        Profiler pf = new Profiler();
        pf.setExpectedTotal(runs);
        for (int r = 0 ; r < runs ; r++) {
            for (int i = 0 ; i < arrays.length ; i++) {
                arrays[i] = new int[arraySize];
            }
            pf.beat();
        }
        System.out.println("New CPS: " + pf.getBps(false));

        pf.reset();
        for (int r = 0 ; r < runs ; r++) {
            for (int i = 0 ; i < arrays.length ; i++) {
                Arrays.fill(arrays[i], 0);
            }
            pf.beat();
        }
        System.out.println("Clear CPS: " + pf.getBps(false));

        pf.reset();
        for (int r = 0 ; r < runs ; r++) {
            for (int i = 0 ; i < arrays.length ; i++) {
                arrays[i] = new int[arraySize];
            }
            pf.beat();
        }
        System.out.println("New CPS 2: " + pf.getBps(false));
    }

    public static void deleteDir(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files!= null) {
                for (File file: files) {
                    if (file.isDirectory()) {
                        deleteDir(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}



