/* $Id: SparseVectorMapImplTest.java,v 1.5 2007/12/04 10:26:43 bam Exp $
 * $Revision: 1.5 $
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
package dk.statsbiblioteket.summa.clusterextractor.math;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * SparseVectorMapImpl Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class SparseVectorMapImplTest extends TestCase {
    private static final String X = "x";
    private static final String Y = "y";

    private static final double delta = Math.pow(10, -15);

    public SparseVectorMapImplTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNorm() {
        HashMap<String, Number> coords1 = new HashMap<String, Number>();
        coords1.put(X, 1);
        SparseVectorMapImpl vec1 = new SparseVectorMapImpl(coords1);
        HashMap<String, Number> coords2 = new HashMap<String, Number>();
        SparseVectorMapImpl vec2 = new SparseVectorMapImpl(coords2);
        assertEquals("The norm of vec1 should be equal to distance from vec1" +
                "to the zero vector (vec2)", vec1.norm(),
                vec1.dist(vec2));
    }

    public void testSimilarity() {
        System.out.println("Equality is tested within range delta = " + delta);

        HashMap<String, Number> coords1 = new HashMap<String, Number>();
        coords1.put(X, 1);
        SparseVectorMapImpl vec1 = new SparseVectorMapImpl(coords1);
        HashMap<String, Number> coords2 = new HashMap<String, Number>();
        coords2.put(Y, 1);
        SparseVectorMapImpl vec2 = new SparseVectorMapImpl(coords2);
        assertEquals("The similarity is the cosine of the angle, "
                + ",which is pi/2, that is cos(pi/2) = zero",
                (double) 0, vec1.similarity(vec2), delta);
        assertEquals("The similarity is the cosine of the angle, "
                + "which is pi/2, that is cos(pi/2) = zero, "
                + "also if we call the method on the other vector",
                (double) 0, vec2.similarity(vec1), delta);

        HashMap<String, Number> coords3 = new HashMap<String, Number>();
        coords3.put(X, 1);
        coords3.put(Y, 1);
        SparseVectorMapImpl vec3 = new SparseVectorMapImpl(coords3);
        assertEquals("The similarity is the cosine of the angle, "
                + ",which is pi/4, that is cos(pi/4)",
                Math.cos(Math.PI/4), vec1.similarity(vec3), delta);
        assertEquals("The similarity is the cosine of the angle, "
                + ",which is pi/4, that is cos(pi/4)",
                Math.cos(Math.PI/4), vec2.similarity(vec3), delta);

    }

    public void testGetEntries() throws Exception {
        Map<String, Number> entries = new HashMap<String, Number>();
        entries.put("x", 1);
        entries.put("y", 1);
        entries.put("z", 2);
        entries.put("w", 0.456789);
        SparseVectorMapImpl vec = new SparseVectorMapImpl(entries);
        assertEquals(entries, vec.getCoordinates());
        //should a copy be returned instead?
    }

    public void testNorm2() {
        Map<String, Number> entries = new HashMap<String, Number>();
        entries.put("x", 3);
        entries.put("y", 4);
        SparseVectorMapImpl vec = new SparseVectorMapImpl(entries);
        assertEquals((double) 5, vec.norm());
    }

    public void testAngle() {
        Map<String, Number> entries1 = new HashMap<String, Number>();
        entries1.put("x", 3);
        entries1.put("y", 3);
        SparseVectorMapImpl vec1 = new SparseVectorMapImpl(entries1);
        Map<String, Number> entries2 = new HashMap<String, Number>();
        entries2.put("x", 3);
        entries2.put("y", 0);
        SparseVectorMapImpl vec2 = new SparseVectorMapImpl(entries2);
        assertEquals(Math.PI/4, vec1.angle(vec2));
        assertEquals(vec1.angle(vec2), vec2.angle(vec1));
        Map<String, Number> entries3 = new HashMap<String, Number>();
        entries3.put("x", 3);// y=0
        SparseVectorMapImpl vec3 = new SparseVectorMapImpl(entries3);
        assertEquals(Math.PI/4, vec1.angle(vec3));
        assertEquals(vec1.angle(vec3), vec3.angle(vec1));
        //TODO: SparseVectorMapImpl equals method and performance test
    }

    public void testDist() {
        Map<String, Number> entries1 = new HashMap<String, Number>();
        entries1.put("y", 3);
        SparseVectorMapImpl vec1 = new SparseVectorMapImpl(entries1);
        Map<String, Number> entries2 = new HashMap<String, Number>();
        entries2.put("y", 14);
        SparseVectorMapImpl vec2 = new SparseVectorMapImpl(entries2);
        assertEquals((double) 11, vec1.dist(vec2));
        assertEquals((double) 11, vec2.dist(vec1));

        Map<String, Number> entries3 = new HashMap<String, Number>();//0-vector
        SparseVectorMapImpl vec3 = new SparseVectorMapImpl(entries3);
        assertEquals((double) 3, vec1.dist(vec3));
        assertEquals((double) 3, vec3.dist(vec1));

        Map<String, Number> entries4 = new HashMap<String, Number>();
        entries4.put("z", 4);
        SparseVectorMapImpl vec4 = new SparseVectorMapImpl(entries4);
        assertEquals((double) 5, vec1.dist(vec4));
        assertEquals((double) 5, vec4.dist(vec1));
    }

    public void testNonZeroEntries() {
        Map<String, Number> entries = new HashMap<String, Number>();
        entries.put("x", 0);
        entries.put("y", 1);
        entries.put("z", 2);
        entries.put("w", 0.456789);
        SparseVectorMapImpl vec = new SparseVectorMapImpl(entries);
        assertEquals(3, vec.nonZeroEntries());
    }

    public void testGetValue() throws Exception {
        Map<String, Number> entries = new HashMap<String, Number>();
        entries.put("x", 1);
        entries.put("y", 1);
        entries.put("z", 2);
        entries.put("w", 0.456789);
        SparseVectorMapImpl vec = new SparseVectorMapImpl(entries);
        assertEquals(entries.get("y").doubleValue(), vec.getValue("y"));
    }
    public static Test suite() {
        return new TestSuite(SparseVectorMapImplTest.class);
    }
}
