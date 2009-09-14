package dk.statsbiblioteket.summa.support.suggest;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.api.SuggestResponse;
import dk.statsbiblioteket.util.Files;

import java.io.File;

/**
 * 
 */
public class SuggestStorageTest extends TestCase {

    SuggestStorage storage;
    File dbLocation;
    int dbCount;

    public void setUp() throws Exception {
        if (storage != null) {
            dbLocation = new File(
                    System.getProperty("java.io.tmpdir"),
                    "suggest-" + ++dbCount);
            storage.open(dbLocation);
        } else {
            dbCount = 0;            
            storage = new SuggestStorageH2(
                    Configuration.newMemoryBased());
            setUp();
        }
    }

    public void tearDown() throws Exception {
        Files.delete(dbLocation);
    }

    public void testRecentSuggestions() throws Exception {
        storage.addSuggestion("old-1", 1, 1);
        storage.addSuggestion("old-2", 2, 2);
        storage.addSuggestion("old-3", 3, 3);
        storage.addSuggestion("old-4", 4, 4);
        storage.addSuggestion("old-5", 5, 5);

        SuggestResponse resp = storage.getRecentSuggestions(100, 3);
        String xml = resp.toXML();
        System.out.println(xml);

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
        System.out.println(xml);
        System.out.println(storage.getSuggestion("new", 10).toXML());

        // We should not see any old-*, but all of the four new suggestions
        assertFalse(xml, xml.contains("old-"));
        xml = eat(xml, "new-4");
        xml = eat(xml, "new-3");
        xml = eat(xml, "new-2");
        xml = eat(xml, "new-1");
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
