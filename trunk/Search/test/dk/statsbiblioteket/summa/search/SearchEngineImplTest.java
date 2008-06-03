/* $Id: SearchEngineImplTest.java,v 1.13 2007/10/11 12:56:24 te Exp $
 * $Revision: 1.13 $
 * $Date: 2007/10/11 12:56:24 $
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
package dk.statsbiblioteket.summa.search;
/**
 * Search Engine Implementation tests
 * @author kfc
 * @since May 30, 2006
 */

import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.XProperties;


@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, kfc")
public class SearchEngineImplTest extends TestCase {
    public SearchEngineImplTest(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        Field instance = SearchEngineImpl.class.getDeclaredField("_instance");
        instance.setAccessible(true);
        instance.set(null, null);

    }

    /** Checks that we only get one instance, and that we get it without trouble
     *
     * @throws Exception
     */
    public void testGetInstance() throws Exception {
        //TODO: Won't work until I actually get a test index in the test dir
        SearchEngine se = SearchEngineImpl.getInstance();
        assertNotNull("Should not be null", se);
        SearchEngine se2 = SearchEngineImpl.getInstance();
        assertSame("Should be the same", se, se2);
    }

    /** Check response time is set and read */
    public void testGetLastResponseTime() throws Exception {
        SearchEngineImpl se = SearchEngineImpl.getInstance();
        assertEquals("Should be 0 before anything has happened", 0, se.getLastResponseTime());
        long l = System.currentTimeMillis();
        se.simpleSearch("test", 1, 0);
        l -= System.currentTimeMillis();
        assertTrue("Should be larger than 0 afterwards", se.getLastResponseTime() > 0);
        assertTrue("Should be less than longer time", se.getLastResponseTime() <= l);
    }


    public void testGetRegistryServer() throws Exception {
        SearchEngineImpl se = SearchEngineImpl.getInstance();
        assertEquals("Should be the test server setting",
                     "localhost", se.getRegistryServer());
    }


    public void testSimpleSearch() throws Exception {
        String query= "Andersen";
        SearchEngine se = SearchEngineImpl.getInstance();
        String result = se.simpleSearch(query, 10,0);

        System.out.println("søgning i: "  + ((SearchEngineImplMBean)se).getLastResponseTime() +  " \n" + result);
        result = se.simpleSearch(query, 10,0);
        System.out.println("søgning i: "  + ((SearchEngineImplMBean)se).getLastResponseTime() +  " \n" + result);
        assertNotNull(result);

    }

    public void testSimpleSearchParallelClusterIndex() throws Exception {
        String query= "cluster:andersen";
        SearchEngine se = SearchEngineImpl.getInstance();
        String result = se.simpleSearch(query, 10,0);

        System.out.println("søgning i: "  + ((SearchEngineImplMBean)se).getLastResponseTime() +  " \n" + result);
        result = se.simpleSearch(query, 10,0);
        System.out.println("søgning i: "  + ((SearchEngineImplMBean)se).getLastResponseTime() +  " \n" + result);
        assertNotNull(result);

    }

    public void testSimilar() throws Exception {
        String query= "neutron irradiations";
        SearchEngine se = SearchEngineImpl.getInstance();
        String result = se.simpleSearch(query, 10,0);
        String needle = "recordID=\"";

        String firstID = result.substring((result.indexOf(needle) + needle.length()), result.indexOf("\"", result.indexOf("recordID=\"") + needle.length()));
        System.out.println(se.getSearchDescriptor());

        System.out.println("søgning i: "  + ((SearchEngineImplMBean)se).getLastResponseTime() +  " \n" + result + "\n" + firstID);
        System.out.println("\n\n\n\n");
        System.out.println(se.getSimilarDocuments(firstID, 0, 10));
    }

    public void testShortRecord() {
        String recordID ="OAI_oai:digital.library.wisc.edu:WI.BelgAmrCol.0723b.bib";
        SearchEngine se = SearchEngineImpl.getInstance();
        System.out.println(se.getQueryLang());
        System.out.println(se.getShortRecord(recordID));
    }

    public void testQueryParse() throws ParseException {
        String[] query = {"-nr:76jswh37 +lma:e_bog test hvad der kommer", "SimpelTest"};
        SearchEngineImpl se = SearchEngineImpl.getInstance();
        for (String s: query){
        Query q = se.summaQueryParser.parse(s);
         System.out.println(q.toString());
        }
    }

    public void testQueryLang() throws Exception {
        SearchEngine se = SearchEngineImpl.getInstance();
        assert se != null;
        System.out.println(se.getQueryLang());
    }

    public void testSearch() throws Exception {
        SearchEngineImpl se = SearchEngineImpl.getInstance();
        Query q = se.summaQueryParser.parse("(ldk5:\"33 12\")");
        System.out.println(q.toString());
        System.out.println(se.simpleSearch("åkjær jeppe author_normalised:\"aakjær jeppe\" su_dk:\"1980 1989\"", 5, 0));
        System.out.print("ok");
    }

    public void testItemCount() throws Exception {

        SearchEngineImpl se = SearchEngineImpl.getInstance();
        assert se.getItemCount("horizon_22") == 1;
        assert se.getItemCount("horizon_1837") == 2;
        assert se.getItemCount("horizon_1846483") == 0;
        assert se.getItemCount("horizon_2822126") == 5;
        assert se.getItemCount("horizon_404502") == 23;

        String[] s = new String[]{"horizon_22","horizon_1837","horizon_1846483","horizon_2822126","horizon_404502","horizon_22","horizon_1837","horizon_1846483","horizon_2822126","horizon_404502"};
        int[] counts = se.getItemCounts(s);
         counts = se.getItemCounts(s);
        for (int k = 0; k<s.length; k++ ){
            assert se.getItemCount(s[k]) == counts[k];
        }
    }




}