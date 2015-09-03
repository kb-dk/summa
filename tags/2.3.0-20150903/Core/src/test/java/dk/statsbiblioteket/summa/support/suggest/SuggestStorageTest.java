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
package dk.statsbiblioteket.summa.support.suggest;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.api.SuggestResponse;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.xml.DOM;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for the {@link SuggestStorage}.
 */
public class SuggestStorageTest extends TestCase {
    private static Log log = LogFactory.getLog(SuggestStorageTest.class);
    SuggestStorage storage;
    File dbLocation;
    int dbCount;

    @Override
    public void setUp() throws Exception {        
        if (storage != null) {
            dbLocation = new File("target/test/", "suggest-" + ++dbCount);
            
            storage.open(dbLocation);
        } else {
            dbCount = 0;            
            storage = new SuggestStorageH2(
                    Configuration.newMemoryBased());
            setUp();
        }
    }

    @Override
    public void tearDown() throws Exception {
        storage.close();
        Files.delete(dbLocation);
    }
    
    
    public void testSpecialFile() throws IOException {
        File location = new File("/home/te/tmp/trier_te/suggest_storage_slow_ps1/suggest");
        //File location = new File("/mnt/bulk/suggest_storage_slow_ps1/suggest");
        //File location = new File("/home/te/tmp/suggest_storage_slow_ps1/suggest");
        final long TIMEOUT = 500000L;

        if (!location.exists()) {
            log.info("The suggest storage files at '" + location +" did not exist. Skipping test");
            return;
        }
        storage = new SuggestStorageH2(Configuration.newMemoryBased());
        storage.open(location);
        testPerformance(storage, 100, TIMEOUT);
        storage.close();
    }

    public void testPerformance(SuggestStorage storage, int runs, Long maxMS) throws IOException {
        final String[] QUERIES =
                ("foo,fo,re,p,s,kompendium aftageligt,d,analysestrategi,bar,hest,ha,the ,ged,os,as,rt,er,zy," +
                 "bom,kalkun,g,h,j,k,l,æ,hanegal,ander,anders,rødhætte").split(",");
        final String[] ADDITIONS =
                ("foo,hanegal,ander,anders,rødhætte,unique " + System.currentTimeMillis() + ",something " ).split(",");
        for (int r = 1 ; r <= runs ; r++) {
            List<Long> mss = measureSuggestPerformance(storage, QUERIES);
            for (long ms: mss) {
                assertTrue("All suggest milliseconds should be <= " + maxMS + " but got " + ms, ms <= maxMS);
            }
            log.info("suggests " + r + "/" + runs + " took " + Strings.join(mss, ", ", 50));

            List<Long> amss = measureAddPerformance(storage, ADDITIONS);
            for (long ms: amss) {
                assertTrue("All add milliseconds should be <= " + maxMS + " but got " + ms, ms <= maxMS);
            }
            log.info("adds " + r + "/" + runs + " took " + Strings.join(mss, ", ", 50));

        }
    }

    private List<Long> measureAddPerformance(SuggestStorage storage, String[] additions) throws IOException {
        List<Long> mss = new ArrayList<>(additions.length);
        for (String addition: additions) {
            long spend = -System.currentTimeMillis();
            storage.addSuggestion(addition, addition.length() + 10);
            spend += System.currentTimeMillis();
            mss.add(spend);
            if (spend > 10000L) {
                log.info("- addition of '" + addition + "' took " + spend + " ms");
            }
        }
        return mss;
    }

    public List<Long> measureSuggestPerformance(SuggestStorage storage, String[] queries) throws IOException {
        List<Long> mss = new ArrayList<>(queries.length);
        for (String query: queries) {
            long spend = -System.currentTimeMillis();
            storage.getSuggestion(query, 5);
            spend += System.currentTimeMillis();
            mss.add(spend);
            if (spend > 10000L) {
                log.info("- query for '" + query + "' took " + spend + " ms");
            }
        }
        return mss;
    }

    
    
    public void testRecentSuggestions() throws Exception {
        storage.addSuggestion("old-1", 1, 1);
        storage.addSuggestion("old-2", 2, 2);
        storage.addSuggestion("old-3", 3, 3);
        storage.addSuggestion("old-4", 4, 4);
        storage.addSuggestion("old-5", 5, 5);

        SuggestResponse resp = storage.getRecentSuggestions(100, 3);
        String xml = resp.toXML();
        log.info(xml);
        // TODO assert

        // We should only see the three items with max queryCount
        assertFalse(xml, xml.contains("old-1"));
        assertFalse(xml, xml.contains("old-2"));
        xml = eat(xml, "old-5");
        xml = eat(xml, "old-4");
        xml = eat(xml, "old-3");

        // Sleep 2s and add some more
        Thread.sleep(2000);
        storage.addSuggestion("new-1", 1, 1);
        storage.addSuggestion("new-2", 2, 2);
        storage.addSuggestion("new-3", 3, 3);
        storage.addSuggestion("new-4", 4, 4);

        // Fetch suggestions added within the last second
        resp = storage.getRecentSuggestions(1, 10);
        xml = resp.toXML();
        log.info(xml);
        log.info(storage.getSuggestion("new", 10).toXML());
        // TODO assert
        // We should not see any old-*, but all of the four new suggestions
        assertFalse(xml, xml.contains("old-"));
        xml = eat(xml, "new-4");
        xml = eat(xml, "new-3");
        xml = eat(xml, "new-2");
        xml = eat(xml, "new-1");
    }

    /* Test that suggestions that yeild the same normalized key
     * still have their own hit counts */
    public void testHitCountsOnSameKey() throws Exception {
        storage.addSuggestion("\"foo\"", 4, 2);
        storage.addSuggestion("foo", 10, 1);
        storage.addSuggestion("\"foo\"", 2, 2); // Also test updates

        SuggestResponse resp = storage.getSuggestion("f", 10);
        String xml = resp.toXML();
        log.info(xml);
        // TODO assert

        Document dom = DOM.stringToDOM(xml);
        NodeList nodes = DOM.selectNodeList(dom, "//suggestion");
        assertEquals(2, nodes.getLength());

        int fooHits, fooQuotedHits;
        if ("foo".equals(nodes.item(0).getTextContent())) {
            fooHits = Integer.parseInt(
                    nodes.item(0).getAttributes().getNamedItem("hits")
                            .getTextContent());
            fooQuotedHits = Integer.parseInt(
                    nodes.item(1).getAttributes().getNamedItem("hits")
                            .getTextContent());
        } else {
            fooHits = Integer.parseInt(
                    nodes.item(1).getAttributes().getNamedItem("hits")
                            .getTextContent());
            fooQuotedHits = Integer.parseInt(
                    nodes.item(0).getAttributes().getNamedItem("hits")
                            .getTextContent());
        }

        assertEquals("The 'foo' suggestion should have 10 hits", 10, fooHits);
        assertEquals(
               "The '\"foo\"' suggestion should have 2 hits", 2, fooQuotedHits);
    }

    public void testTwoAdds() throws Exception {
        storage.addSuggestion("food", 4, 2);
        storage.addSuggestion("foo", 10, 1);

        SuggestResponse resp = storage.getSuggestion("f", 10);
        String xml = resp.toXML();
        log.info(xml);
        // TODO assert

        Document dom = DOM.stringToDOM(xml);
        NodeList nodes = DOM.selectNodeList(dom, "//suggestion");
        assertEquals(2, nodes.getLength());

        int fooIndex, foodIndex;
        int fooHits, foodHits, fooQueries, foodQueries;
        if ("foo".equals(nodes.item(0).getTextContent())) {
            fooIndex = 0;
            foodIndex = 1;
        } else {
            fooIndex = 1;
            foodIndex = 0;
        }

        fooHits = Integer.parseInt(
                nodes.item(fooIndex).getAttributes().getNamedItem("hits")
                        .getTextContent());
        fooQueries= Integer.parseInt(
                nodes.item(fooIndex).getAttributes().getNamedItem("queryCount")
                        .getTextContent());
        foodHits = Integer.parseInt(
                nodes.item(foodIndex).getAttributes().getNamedItem("hits")
                        .getTextContent());
        foodQueries = Integer.parseInt(
                nodes.item(foodIndex).getAttributes().getNamedItem("queryCount")
                        .getTextContent());


        assertEquals("The 'foo' suggestion should have 10 hits", 10, fooHits);
        assertEquals("The 'foo' suggestion should have 1 query", 1, fooQueries);
        assertEquals(
                "The 'food' suggestion should have 4 hits", 4, foodHits);
        assertEquals(
                "The 'food' suggestion should have 2 queries", 2, foodQueries);
    }

    public void testDeletes() throws Exception {
        testTwoAdds();
        storage.addSuggestion("foo", 0);
        String xml = storage.getSuggestion("f", 100).toXML();

        Document dom = DOM.stringToDOM(xml);
        NodeList nodes = DOM.selectNodeList(dom, "//suggestion");
        assertEquals(1, nodes.getLength());

        assertEquals("The 'foo' suggestion should have been removed",
                     "food", nodes.item(0).getTextContent());
    }

    /* Suggestions should not include leading/trailing whitespace */
    public void testLeadingAndTrailingWhitespaceDeletes() throws Exception {
        // These additions should all result in the same suggestion
        storage.addSuggestion(" foo bar", 4, 2);
        storage.addSuggestion("foo bar", 10, 1);
        storage.addSuggestion("foo bar ", 27, 68);

        SuggestResponse resp = storage.getSuggestion("f", 10);
        String xml = resp.toXML();
        log.info(xml);
        // TODO assert

        Document dom = DOM.stringToDOM(xml);
        NodeList nodes = DOM.selectNodeList(dom, "//suggestion");
        assertEquals(1, nodes.getLength());
        assertEquals("foo bar", nodes.item(0).getTextContent());
    }

    /**
     * Eats everything out of food, up until, and including, bite.
     * Returns the remainer of food.
     * <p/>
     * The primary purpose of this method is to set for a certain sorted
     * subset of strings within food, in some specific order.
     *
     * @param food
     * @param bite
     * @return
     */
    public String eat(String food, String bite) {
        int i = food.indexOf(bite);
        if (i == -1) {
            fail("'" + bite + "' not found in :\n" + food);
        }
        return food.substring(i + bite.length());
    }
}
