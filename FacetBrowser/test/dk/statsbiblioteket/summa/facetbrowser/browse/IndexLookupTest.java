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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.CollatorFactory;
import dk.statsbiblioteket.summa.facetbrowser.BaseObjects;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexResponse;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.Facet;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.xml.DOM;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.Collator;
import java.util.*;

public class IndexLookupTest extends TestCase {
    public IndexLookupTest(String name) {
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
        return new TestSuite(IndexLookupTest.class);
    }

    // A tag handler with a single empty facet "test" with locale "da"
    public static TagHandler createEmptyTagHandler() throws IOException {
        Configuration structureConf = Configuration.newMemoryBased();
        //noinspection deprecation
        Configuration testFacetConf = structureConf.
                createSubConfigurations(Structure.CONF_FACETS, 1).get(0);
        testFacetConf.set(FacetStructure.CONF_FACET_NAME, "test");
        testFacetConf.set(FacetStructure.CONF_FACET_LOCALE, "da");
        Structure structure = new Structure(structureConf);

        Configuration tagHandlerConf = Configuration.newMemoryBased();
        tagHandlerConf.set(TagHandler.CONF_USE_MEMORY, true);

        TagHandler tagHandler = new TagHandlerImpl(
                tagHandlerConf, structure, false);
        tagHandler.open(File.createTempFile("tagHandler", ".tmp"));
        return tagHandler;
    }

    public void testBasic() throws Exception {
        TagHandler tagHandler = fillSimpleTagHandler();

        assertEquals("b", -1, 2, tagHandler, "a, b");
        assertEquals("a", -100, 200, tagHandler, "a, b, d, do, æ, ø, å");
        assertEquals("c", -1, 5, tagHandler, "b, d, do, æ, ø");
        assertEquals("ø", -1, 3, tagHandler, "æ, ø, å");
        assertEquals("", -1, 3, tagHandler, "a, b, d");
    }

    private TagHandler fillSimpleTagHandler() throws IOException {
        TagHandler tagHandler = makeHandler(Arrays.asList(
                "b", "å", "ø", "æ", "a", "d", "do"));
        // a b d do æ ø å
        assertTrue("There should be some tags for the facet test",
                   tagHandler.getTagCount("test") > 0);
        return tagHandler;
    }

    private TagHandler makeHandler(List<String> tags) throws IOException {
        TagHandler tagHandler = createEmptyTagHandler();
        int authorID = tagHandler.getFacetID("test");
        for (String tag: tags) {
            tagHandler.insertTag(authorID, tag);
        }
        return tagHandler;
    }

    public void testCollator() throws Exception {
        List<String> EXPECTED = Arrays.asList("ko", "koste", "kost er bedst");
        List<String> input = Arrays.asList("koste", "kost er bedst", "ko");

        Collator daCollator = Collator.getInstance(new Locale("da"));
        testCompare("Spaces is sorted after everything else by the standard "
                    + "collator",
                    daCollator, EXPECTED, input);

    /*
        Comparator<String> insensitive =
                new IndexResponse.InSensitiveComparator(daCollator);
        testCompare("Spaces hould be sorted before anything else by "
                     + "insensitive comparator",
                     insensitive, EXPECTED, input);*/
    }

    public void testCachedCollator() throws Exception {
        List<String> EXPECTED = Arrays.asList("ko", "koste", "kost er bedst");
        List<String> input = Arrays.asList("koste", "kost er bedst", "ko");

        Collator cachedCollator = new CachedCollator(new Locale("da"), "");
        testCompare("Spaces is sorted after everything else by cached "
                    + "collator",
                    cachedCollator, EXPECTED, input);
    }

    public void testModifiedCollator() throws Exception {
        List<String> EXPECTED = Arrays.asList("ko", "kost er bedst", "koste");
        List<String> input = Arrays.asList("koste", "kost er bedst", "ko");

        Collator daCollator = CollatorFactory.createCollator(new Locale("da"));
        testCompare("Spaces hould be sorted before anything else by custom "
                    + "collator", daCollator, EXPECTED, input);
    }

    public void testCompare(String message, Comparator comparator,
                            List<String> expected, List<String> actual)
                                                              throws Exception {
        List<String> sorted = new ArrayList<String>(actual);
        //noinspection unchecked
        Collections.sort(sorted, comparator);
        assertEquals(message,
                     Strings.join(expected, ", "), Strings.join(sorted, ", "));
    }

    public void testSpaceSort() throws Exception {
        TagHandler tagHandler = makeHandler(Arrays.asList(
                "koste", "kost er bedst", "ko"));
        String EXPECTED = "ko, kost er bedst, koste";

        Facet facet = tagHandler.getFacets().get(0);
        assertEquals("The facet should contain the tags in sorted order",
                     EXPECTED, Strings.join(facet, ", "));

        assertEquals("", -1, 100, tagHandler, EXPECTED);
    }

    public void testSpaceSort2() throws Exception {
        TagHandler tagHandler = makeHandler(Arrays.asList(
                "ki", "kan", "kaos", "kost er bedst", "kost", "koste",
                "ko so", "ko sø", "ko så"));

        assertEquals("", -1, 100, tagHandler,
                     "kan, kaos, ki, ko so, ko sø, ko så, kost, kost er bedst, "
                     + "koste");
    }

    public void testDOM() throws Exception {
        TagHandler tagHandler = fillSimpleTagHandler();
        Configuration indexConf = Configuration.newMemoryBased(
                IndexRequest.CONF_INDEX_DELTA, -5,
                IndexRequest.CONF_INDEX_LENGTH, 10
        );
        String xml = getLookupXML("b", indexConf, tagHandler);
        System.out.println("Got XML:\n" + xml);
        Document domIndex = DOM.stringToDOM(xml);
        NodeList nl = DOM.selectNodeList(domIndex, "//term");

        String index = "";
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            index += "'" + (DOM.selectString(n, "./text()")) + "'";
            if (i < nl.getLength() - 1) {
                // only add the , if this isn't the very last element
                index += ",";
            }
        }

        System.out.println(index);
    }

    private void assertEquals(String term, int delta, int length,
                              TagHandler tagHandler, String expected)
                                                        throws RemoteException {
        Configuration indexConf = Configuration.newMemoryBased(
                IndexRequest.CONF_INDEX_DELTA, delta,
                IndexRequest.CONF_INDEX_LENGTH, length
        );
        String actual = Strings.join(
                getTerms(term, indexConf, tagHandler), ", ");
        if (!actual.equals(expected)) {
            IndexRequest requestFactory = new IndexRequest(indexConf);
            Request request = new Request();
            request.put(IndexKeys.SEARCH_INDEX_FIELD, "test");
            request.put(IndexKeys.SEARCH_INDEX_TERM, term);
            IndexRequest indexRequest = requestFactory.createRequest(request);
            String tags = Strings.join(Arrays.asList(
                    tagHandler.getFacets().get(0).toArray()), ", ");
            fail(String.format(
                    "Expected [%s], got [%s] for term %s with delta %d and "
                    + "length %d in facet with tags [%s]",
                    expected, actual, term, indexRequest.getDelta(),
                    indexRequest.getLength(), tags));
        }
    }

    private List<String> getTerms(String term, Configuration indexConf,
                                  TagHandler tagHandler) throws RemoteException{
        String responseXML = getLookupXML(term, indexConf, tagHandler);
        Document dom = DOM.stringToDOM(responseXML);
        NodeList nodes = DOM.selectNodeList(dom, "indexresponse/index/term");
        List<String> terms = new ArrayList<String>(nodes.getLength());
        for (int i = 0 ; i < nodes.getLength() ; i++) {
            terms.add(DOM.getElementNodeValue(nodes.item(i)));
        }
        return terms;
    }

    private String getLookupXML(String term, Configuration indexConf,
                                TagHandler tagHandler) throws RemoteException {
        IndexLookup lookup = new IndexLookup(indexConf);
        Request request = new Request();
        request.put(IndexKeys.SEARCH_INDEX_FIELD, "test");
        request.put(IndexKeys.SEARCH_INDEX_TERM, term);
        ResponseCollection responses = new ResponseCollection();
        lookup.lookup(request, responses, tagHandler);
        assertEquals("The number of collected responses should be correct for"
                     + " term '" + term + "'",
                     1, responses.size());
        IndexResponse indexResponse = (IndexResponse)responses.toArray()[0];
        return indexResponse.toXML();
    }
}
