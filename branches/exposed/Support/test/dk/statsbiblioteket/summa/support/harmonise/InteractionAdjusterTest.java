package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;

/**
 * InteractionAdjuster Tester.
 *
 * @author <Authors name>
 * @since <pre>06/20/2011</pre>
 * @version 1.0
 */
public class InteractionAdjusterTest extends TestCase {
    public InteractionAdjusterTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(InteractionAdjusterTest.class);
    }

    public void testFacetFieldQueryRewrite() {
        InteractionAdjuster adjuster = createAdjuster();
        Request request = new Request(
            FacetKeys.SEARCH_FACET_FACETS, "llang, lma_long"
        );
        Request rewritten = adjuster.rewrite(request);
        assertEquals("The rewritten facet fields should be as expected",
                     "Language, ContentType",
                     Strings.join(rewritten.getStrings(
                         FacetKeys.SEARCH_FACET_FACETS), ", "));
    }

    // ContentType -> lma_long
    public void testFacetFieldResultRewrite() {
        InteractionAdjuster adjuster = createAdjuster();

        Request request = new Request(
            FacetKeys.SEARCH_FACET_FACETS, "lma_long"
        );

        HashMap<String, Integer> facetIDs = new HashMap<String, Integer>();
        facetIDs.put("ContentType", 0);
        HashMap<String, String[]> fields = new HashMap<String, String[]>();
        fields.put("ContentType", new String[]{"ContentType"});
        FacetResultExternal summaFacetResult = new FacetResultExternal(
            new HashMap<String, Integer>(), facetIDs, fields);
        ResponseCollection responses = new ResponseCollection();
        responses.add(summaFacetResult);

        adjuster.adjust(request, responses);

        assertEquals("The adjusted response should contain rewritten field for"
                     + " material type",
                     "lma_long",
                     ((FacetResultExternal)responses.iterator().next()).
                         getFields().entrySet().iterator().next().
                         getValue()[0]);
    }

    private InteractionAdjuster createAdjuster() {
        Configuration conf = Configuration.newMemoryBased(
            InteractionAdjuster.CONF_IDENTIFIER, "tester",
            InteractionAdjuster.CONF_ADJUST_FACET_FIELDS,
            "author_normalised - Author, lma_long - ContentType, "
            + "llang - Language, lsubject - SubjectTerms",
            InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS,
            "recordID - ID, author_normalised - Author, "
            + "lma_long - ContentType, llang - Language, "
            + "lsubject - SubjectTerms"
        );
        return new InteractionAdjuster(conf);
    }

    public void testQueryFieldRewrite() {
        InteractionAdjuster adjuster = createAdjuster();
        Request request = new Request(
            DocumentKeys.SEARCH_QUERY, "llang:\"foo\" bar"
        );
        Request rewritten = adjuster.rewrite(request);
        assertEquals("The rewritten query should have a changed field",
                     "Language:\"foo\" bar",
                     rewritten.get(DocumentKeys.SEARCH_QUERY));
    }
}
