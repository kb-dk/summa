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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerImpl;
import dk.statsbiblioteket.summa.facetbrowser.BaseObjects;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexResponse;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.Strings;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.rmi.RemoteException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

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
        TagHandler tagHandler = createEmptyTagHandler();
        int authorID = tagHandler.getFacetID("test");
        tagHandler.insertTag(authorID, "b");
        tagHandler.insertTag(authorID, "å");
        tagHandler.insertTag(authorID, "ø");
        tagHandler.insertTag(authorID, "æ");
        tagHandler.insertTag(authorID, "a");
        tagHandler.insertTag(authorID, "d");
        tagHandler.insertTag(authorID, "do");
        // a b d do æ ø å
        assertTrue("There should be some tags for the facet test",
                   tagHandler.getTagCount("test") > 0);

        assertEquals("b", -1, 2, tagHandler, "a, b");
        assertEquals("a", -100, 200, tagHandler, "a, b, d, do, æ, ø, å");
        assertEquals("c", -1, 5, tagHandler, "b, d, do, æ, ø");
        assertEquals("ø", -1, 3, tagHandler, "æ, ø, å");
    }

    private void assertEquals(String term, int delta, int length,
                              TagHandler tagHandler, String expected)
                                                        throws RemoteException {
        Configuration indexConf = Configuration.newMemoryBased(
                IndexRequest.CONF_INDEX_DELTA, delta,
                IndexRequest.CONF_INDEX_LENGTH, length
        );
        String actual =
                Strings.join(getTerms(term, indexConf, tagHandler), ", ");
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
                    + "length %d in [%s]",
                    expected, actual, term, indexRequest.getDelta(),
                    indexRequest.getLength(), tags));
        }
    }

    private List<String> getTerms(String term, Configuration indexConf,
                                  TagHandler tagHandler) throws RemoteException{
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
        Document dom = DOM.stringToDOM(indexResponse.toXML());
        NodeList nodes = DOM.selectNodeList(dom, "indexresponse/index/term");
        List<String> terms = new ArrayList<String>(nodes.getLength());
        for (int i = 0 ; i < nodes.getLength() ; i++) {
            terms.add(DOM.getElementNodeValue(nodes.item(i)));
        }
        return terms;
    }
}
