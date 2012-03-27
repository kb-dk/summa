/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.CollatorFactory;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.Collator;
import java.util.Locale;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetTest extends TestCase {
    private static Log log = LogFactory.getLog(FacetTest.class);
    public FacetTest(String name) {
        super(name);
    }

    private File TMP_FACET = new File(
        new File(System.getProperty("java.io.tmpdir")), "facettst");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (TMP_FACET.exists()) {
            Files.delete(TMP_FACET);
        }
        if (!TMP_FACET.mkdirs()) {
            fail("Unable to create dir " + TMP_FACET);
        }
    }
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (TMP_FACET.exists()) {
            Files.delete(TMP_FACET);
        }
    }

    public static Test suite() {
        return new TestSuite(FacetTest.class);
    }

    public void testPlainFacetUsage() throws Exception {
        FacetStructure structure = new FacetStructure(
            Configuration.newMemoryBased(FacetStructure.CONF_FACET_NAME, "foo"),
            0);
        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        Facet facet = new Facet(structure, false, false);
        facet.open(TMP_FACET);
        for (String tag: new String[]{"a", "c", "j", "b", "7", "å", "ø"}) {
            facet.dirtyAdd(tag);
        }
        log.info("Added " + facet.size() + " tags");
        facet.cleanup();
        assertEquals("The index of c should be correct",
                     (-3)-1, facet.insert("c"));
        assertEquals("The index of ø should be unicode order correct",
                     (-6)-1, facet.insert("ø"));
    }

    public void testLocaleFacetUsage() throws Exception {
        String[] TAGS = new String[]{"a", "c", "j", "b", "7", "å", "ø"};

        FacetStructure structure = new FacetStructure(
            Configuration.newMemoryBased(
                FacetStructure.CONF_FACET_NAME, "foo",
                FacetStructure.CONF_FACET_LOCALE, "da"), 0);
        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        Facet facet = new Facet(structure, false, false);
        facet.open(TMP_FACET);
        for (String tag: TAGS) {
            facet.dirtyAdd(tag);
        }
        facet.cleanup();
        long datSize = new File(TMP_FACET, "foo.dat").length();
        assertTrue("The datFile should contain something", datSize > 0);

        assertEquals("The index of c should be correct",
                     (-3)-1, facet.insert("c"));
        assertEquals("The index of ø should be 'da' locale correct",
                     (-5)-1, facet.insert("ø"));

        for (String tag: TAGS) {
            assertTrue("insert of " + tag + " should return a negative index "
                       + "as it already exists", facet.insert(tag) < 0);
        }
        assertEquals("The datFile should not grow",
                     datSize, new File(TMP_FACET, "foo.dat").length());
    }

    public static final String PROBLEMATIC = "ah\naa\nạ\nị";
    public static final String PROBLEMATIC2 = "ur\nú";

    public void testCleanMultiple() throws UnsupportedEncodingException {
        for (byte b: CachedCollator.COMMON_SUMMA_EXTRACTED.getBytes("utf-8")) {
            if (b > 0xE0) {
                log.info("'" + b + "': " + Integer.toString(b));
            }
        }
        log.info("Problematic 2");
        for (char c: PROBLEMATIC2.toCharArray()) {
            log.info("'" + c + "': " + Integer.toString(c));
        }
        // TODO assert
    }

    public void testSpecialCaseLocale() throws Exception {
        File INPUT = new File(TMP_FACET, "problem.dat");
        Files.saveString(PROBLEMATIC2, INPUT);
        String[] terms = Files.loadString(INPUT).split("\n");
//        String[] terms = Files.loadString(new File("/home/te/tmp/llfo.dat.reducedfail")).split("\n");
        log.info("Got " + terms.length + " terms from " + INPUT);

        FacetStructure structure = new FacetStructure(
            Configuration.newMemoryBased(
                FacetStructure.CONF_FACET_NAME, "foo",
                FacetStructure.CONF_FACET_LOCALE, "da"), 0);
        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        Facet facet = new Facet(structure, false, false);
        facet.open(TMP_FACET);
        long counter = 0;
        log.info("Doing initial fill from " + INPUT);
        for (String tag: terms) {
            facet.dirtyAdd(tag);
            if (counter >> 17 << 17 == counter) {
                log.info(counter + ": " + tag);
            }
            counter++;
        }
        log.info("Cleaning up");

        facet.cleanup();
        long datSize = new File(TMP_FACET, "foo.dat").length();
        assertTrue("The datFile should contain something", datSize > 0);

//        log.info("The content of the data file is\n"
//                           + Files.loadString(new File(TMP_FACET, "foo.dat")));

        log.info("Checking order explicitly");
        Collator collator =
            CollatorFactory.createCollator(new Locale("da"), false);
        String lastTag = null;
        for (String tag: facet) {
            if (lastTag != null) {
                assertTrue("The faulty order was '" + lastTag + "' < '" + tag
                           + "'",
                           collator.compare(lastTag, tag) < 0);
            }
            lastTag = tag;
        }

        counter = 0;
        log.info("Doing re-insert from " + INPUT);
        for (String tag: terms) {
            assertTrue("insert of " + tag + " should return a negative index "
                       + "as it already exists", facet.insert(tag) < 0);
            if (counter >> 17 << 17 == counter) {
                log.info(counter + ": " + tag);
            }
            counter++;
        }
        assertEquals("The datFile should not grow",
                     datSize, new File(TMP_FACET, "foo.dat").length());
    }
}