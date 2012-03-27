/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermEntry;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermStat;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatQueryRewriterTest extends TestCase {
    public TermStatQueryRewriterTest(String name) {
        super(name);
    }

    private static final File TMP = new File(
        System.getProperty("java.io.tmpdir"), "termstatqueryrewriter");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (TMP.exists()) {
            Files.delete(TMP);
        }
        assertTrue("The folder " + TMP + " should be created", TMP.mkdirs());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Files.delete(TMP);
    }

    public static Test suite() {
        return new TestSuite(TermStatQueryRewriterTest.class);
    }

    static final String[] COLUMNS = new String[]{"term", "df"};
    public void testRewrite() throws IOException {
        TermStatQueryRewriter rewriter = setupTestTermStats();

        double idealNE = Math.pow(Math.log((10+20)/(double)(3+3+1)) + 1.0, 2);
        double targetNEa = Math.pow(Math.log(10/(double)(3+1)) + 1.0, 2);
        float boostNEa = (float) (idealNE / targetNEa);
        double targetNEb = Math.pow(Math.log(20/(double)(3+1)) + 1.0, 2);
        float boostNEb = (float) (idealNE / targetNEb);

/*        System.out.println(String.format(
            "test idealIDFSqr=%f, targetIDFSqr=%f, boostFactor=%f",
            idealNE, targetNEa, boostNEa));
  */

        double idealA = Math.pow(Math.log((10+20)/(double)(5+2+1)) + 1.0, 2);
        double targetAa = Math.pow(Math.log(10/(double)(5+1)) + 1.0, 2);
        float boostAa = (float) (idealA / targetAa);
        double targetAb = Math.pow(Math.log(20/(double)(2+1)) + 1.0, 2);
        float boostAb = (float) (idealA / targetAb);

        String[][] TESTS = new String[][]{ // query, expectedA, expectedB
            {"foo", "foo^" + boostNEa, "foo^" + boostNEb},
            {"a", "a^" + boostAa, "a^" + boostAb}
        };
        Request request = new Request();
        assertRewriteResults(rewriter, TESTS, request);
    }

    public void testNumber() throws IOException {
        TermStatQueryRewriter rewriter = setupTestTermStats();
        String[] TESTS = new String[]{ // query, expectedA, expectedB
            "birds of prey",
            "localhost 2010 hello",
            "localhost OR hello",
            "localhost -hello"
        };
        for (String test: TESTS) {
            Request request = new Request();
            request.put(DocumentKeys.SEARCH_QUERY, test);
            Map<String, String> rewrittenMap = rewriter.rewrite(request);

            String actualA = rewrittenMap.get("a");
            System.out.println("'" + test + "'' -> '" + actualA + "'");
            assertEquals(
                "There should be the same number of tokens in input '" + test
                + "' and result '" + actualA + "'",
                test.split(" ").length, actualA.split(" ").length);
        }
    }

    public void testRewriteBoost() throws IOException {
        TermStatQueryRewriter rewriter = setupTestTermStats();

        double weightA = 0.9;
        double weightB = 0.8;
        float queryBoost = 1.3f;

        double idealB = Math.pow(Math.log(
            (10+20)/(1.0*weightA+10*weightB+1)) + 1.0, 2);
        double targetBa = Math.pow(Math.log(10/(double)(1+1)) + 1.0, 2);
        float boostBa = (float) (idealB / targetBa * queryBoost);
        double targetBb = Math.pow(Math.log(20/(double)(10+1)) + 1.0, 2);
        float boostBb = (float) (idealB / targetBb * queryBoost);

/*        System.out.println(String.format(
            "test idealIDFSqr=%f, targetIDFSqr=%f, boostFactor=%f",
            idealB, targetBa, boostBa));
  */

        String[][] TESTS2 = new String[][]{ // query, expectedA, expectedB
            {"b^" + queryBoost, "a^" + boostBa, "b^" + boostBb}
        };

        Request request = new Request();
        request.put("a." + TermStatQueryRewriter.Target.CONF_WEIGHT, weightA);
        request.put("b." + TermStatQueryRewriter.Target.CONF_WEIGHT, weightB);
        assertRewriteResults(rewriter, TESTS2, request);
    }

    private void assertRewriteResults(
        TermStatQueryRewriter rewriter, String[][] TESTS, Request request) {
        for (String[] test: TESTS) {
            String sourceQuery = test[0];
            String expectedA = test[1];
            String expectedB = test[2];
            request.put(DocumentKeys.SEARCH_QUERY, sourceQuery);
            Map<String, String> rewrittenMap = rewriter.rewrite(request);
            String actualA = rewrittenMap.get("a");
            String actualB = rewrittenMap.get("b");
            assertFEquals("The rewrite of '" + sourceQuery + " should be "
                          + "correct for target a", expectedA, actualA);
            assertFEquals("The rewrite of '" + sourceQuery + " should be "
                          + "correct for target b", expectedB, actualB);
        }
    }

    private void assertFEquals(
        String message, String expectedA, String actualA) {
        if (!expectedA.contains("^")) {
            assertEquals(message, expectedA, actualA);
        }
        double GAP = 0.0001;
        float expected = Float.parseFloat(expectedA.split("\\^")[1]);
        float actual = Float.parseFloat(actualA.split("\\^")[1]);
        if (Math.abs(expected-actual) < GAP) {
            return;
        }
        fail(message + "\nExpected: " + expectedA + "\nActual   : " + actualA);
    }

    private TermStatQueryRewriter setupTestTermStats() throws IOException {
        createA(COLUMNS);
        createB(COLUMNS);
        Configuration conf = Configuration.newMemoryBased();
        List<Configuration> subConfs = conf.createSubConfigurations(
            TermStatQueryRewriter.CONF_TARGETS, 2);
        Configuration confA = subConfs.get(0);
        Configuration confB = subConfs.get(1);

        confA.set(TermStatQueryRewriter.Target.CONF_ID, "a");
        confA.set(TermStatQueryRewriter.Target.CONF_FALLBACK_DF, 3);
        confA.set(TermStatQueryRewriter.Target.CONF_TERMSTAT_LOCATION,
                  new File(TMP, "a"));
        confB.set(TermStatQueryRewriter.Target.CONF_ID, "b");
        confB.set(TermStatQueryRewriter.Target.CONF_FALLBACK_DF, 3);
        confB.set(TermStatQueryRewriter.Target.CONF_TERMSTAT_LOCATION,
                  new File(TMP, "b"));

        TermStatQueryRewriter rewriter = new TermStatQueryRewriter(conf);
        return rewriter;
    }

    private void createA(String[] COLUMNS) throws IOException {
        TermStat tsA = new TermStat(Configuration.newMemoryBased());
        tsA.create(new File(TMP, "a"), null, COLUMNS);
        tsA.setDocCount(10);
        tsA.setSource("TestA");
        long[] stat = new long[1];

        stat[0] = 5;
        tsA.add(new TermEntry("a", stat, COLUMNS));
        stat[0] = 1;
        tsA.add(new TermEntry("b", stat, COLUMNS));
        stat[0] = 3;
        tsA.add(new TermEntry("c", stat, COLUMNS));
        tsA.store();
        tsA.close();
    }

    private void createB(String[] COLUMNS) throws IOException {
        TermStat tsB = new TermStat(Configuration.newMemoryBased());
        tsB.create(new File(TMP, "b"), null, COLUMNS);
        tsB.setDocCount(20);
        tsB.setSource("TestB");
        long[] stat = new long[1];

        stat[0] = 2;
        tsB.add(new TermEntry("a", stat, COLUMNS));
        stat[0] = 10;
        tsB.add(new TermEntry("b", stat, COLUMNS));
        stat[0] = 3;
        tsB.add(new TermEntry("c", stat, COLUMNS));
        stat[0] = 4;
        tsB.add(new TermEntry("d", stat, COLUMNS));
        tsB.store();
        tsB.close();
    }

    public void testNullRewrite() {
        String[] MATCHING = new String[]{
            "foo", "foo^2", "foo^2.3", "bar:zoo", "bar:zoo^4", "bar:zoo^4.5"
        };
        TermStatQueryRewriter rewriter =
            new TermStatQueryRewriter(Configuration.newMemoryBased());
        Request request = new Request();
        for (String test: MATCHING) {
            request.put(DocumentKeys.SEARCH_QUERY, test);
            rewriter.rewrite(request);
            String rewritten = request.getString(DocumentKeys.SEARCH_QUERY);
            System.out.println(test + " -> " + rewritten);
        }
    }

    public void testDumpSplit() {
        String[] MATCHING = new String[]{
            "foo", "foo^2", "foo^2.3", "bar:zoo", "bar:zoo^4", "bar:zoo^4.5"
        };

    }
}
