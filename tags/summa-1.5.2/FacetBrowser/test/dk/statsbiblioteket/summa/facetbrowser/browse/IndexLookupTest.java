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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
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
import dk.statsbiblioteket.util.Profiler;
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

/*  Used to test a specific problem. Code kept as we should make a proper test  
    public void testLTI() throws Exception {
        Configuration structureConf = Configuration.newMemoryBased();
        //noinspection deprecation
        Configuration testFacetConf = structureConf.
                createSubConfigurations(Structure.CONF_FACETS, 1).get(0);
        testFacetConf.set(FacetStructure.CONF_FACET_NAME, "lti");
        testFacetConf.set(FacetStructure.CONF_FACET_LOCALE, "da");
        Structure structure = new Structure(structureConf);

        Configuration tagHandlerConf = Configuration.newMemoryBased();
        tagHandlerConf.set(TagHandler.CONF_USE_MEMORY, false);

        TagHandler tagHandler = new TagHandlerImpl(
                tagHandlerConf, structure, false);
        System.out.println("Opening lti facet data");
        Profiler profiler = new Profiler();
        tagHandler.open(Resolver.getFile("data/lti.dat").getParentFile());
        System.out.println(String.format(
                "Open of %d tags finished in %s",
                tagHandler.getTagCount("lti"), profiler.getSpendTime()));

        //testTagOrderConsistency(tagHandler);


        System.out.println("Dumping index lookup samples");
        int facetID = tagHandler.getFacetID("lti");
        for (String lookup: Arrays.asList("a", "b", "t", "to", "æ", "ø", "å")) {
            System.out.println(lookup + " -direct-> "
                               + tagHandler.getTagName(
                    facetID, 
                    Math.abs(tagHandler.getNearestTagID(facetID, lookup))));
            System.out.println(lookup + " -index-> " + getLookupResult(
                    "lti", lookup, -2, 4, tagHandler));
        }
        tagHandler.close();
    }
  */
    private void testTagOrderConsistency(TagHandler tagHandler) {
        Facet ltiFacet = tagHandler.getFacets().get(0);
        Collator ltiCollator = ltiFacet.getCollator();
        System.out.println("Testing tag-order consistency");
        String last = null;
        for (int i = 0 ; i < ltiFacet.size() ; i++) {
            String current = ltiFacet.get(i);
            if (last != null) {
                assertTrue(String.format(
                        "At position %d, %s should be < %s", i, last, current),
                           ltiCollator.compare(last, current) < 0);
            }
            last = current;
        }
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
        String xml = getLookupXML("test", "b", indexConf, tagHandler);
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
        String actual = getLookupResult(
                "test", term, delta, length, tagHandler);
        if (!actual.equals(expected)) {
            Configuration indexConf = Configuration.newMemoryBased(
                    IndexRequest.CONF_INDEX_DELTA, delta,
                    IndexRequest.CONF_INDEX_LENGTH, length
            );
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

    private String getLookupResult(String field, String term, int delta,
                                   int length, TagHandler tagHandler)
                                                        throws RemoteException {
        Configuration indexConf = Configuration.newMemoryBased(
                IndexRequest.CONF_INDEX_DELTA, delta,
                IndexRequest.CONF_INDEX_LENGTH, length
        );
        return Strings.join(getTerms(field, term, indexConf, tagHandler), ", ");
    }

    private List<String> getTerms(String field, String term,
                                  Configuration indexConf,
                                  TagHandler tagHandler) throws RemoteException{
        String responseXML = getLookupXML(field, term, indexConf, tagHandler);
        Document dom = DOM.stringToDOM(responseXML);
        NodeList nodes = DOM.selectNodeList(dom, "indexresponse/index/term");
        List<String> terms = new ArrayList<String>(nodes.getLength());
        for (int i = 0 ; i < nodes.getLength() ; i++) {
            terms.add(DOM.getElementNodeValue(nodes.item(i)));
        }
        return terms;
    }

    private String getLookupXML(String field, String term,
                                Configuration indexConf,
                                TagHandler tagHandler) throws RemoteException {
        IndexLookup lookup = new IndexLookup(indexConf);
        Request request = new Request();
        request.put(IndexKeys.SEARCH_INDEX_FIELD, field);
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

