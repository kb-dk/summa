package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.FlexiblePair;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

    public void testSortQueryRewrite() {
        InteractionAdjuster adjuster = createAdjuster();
        Request request = new Request(
            DocumentKeys.SEARCH_SORTKEY, "sort_year_asc"
        );
        Request rewritten = adjuster.rewrite(request);
        assertEquals("The rewritten sort field should be as expected",
                     "PublicationDate",
                     rewritten.getString(DocumentKeys.SEARCH_SORTKEY));
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
            new HashMap<String, Integer>(), facetIDs, fields, null);
        summaFacetResult.setPrefix("interactionadjustertest");
        //noinspection unchecked
        summaFacetResult.getMap().put("ContentType", Arrays.asList(
            new FlexiblePair<String, Integer>(
                "Audio Sound", 87, FlexiblePair.SortType.PRIMARY_ASCENDING)));
        ResponseCollection responses = new ResponseCollection();
        responses.add(summaFacetResult);

        adjuster.adjust(request, responses);

        assertEquals("The adjusted response should contain rewritten facet "
                     + "query field for material type",
                     "lma_long",
                     ((FacetResultExternal)responses.iterator().next()).
                         getFields().entrySet().iterator().next().
                         getValue()[0]);
        assertEquals("The adjusted response should contain rewritten field for"
                     + " material type",
                     "lma_long",
                     ((FacetResultExternal)responses.iterator().next()).
                         getMap().entrySet().iterator().next().getKey());
        assertEquals("The adjusted response should contain rewritten tag for"
                     + " material type",
                     "audio",
                     ((FacetResultExternal)responses.iterator().next()).
                         getMap().entrySet().iterator().next().getValue().
                         get(0).getKey());
    }

    public void testSortResultRewrite() {
        InteractionAdjuster adjuster = createAdjuster();

        Request request = new Request(
            DocumentKeys.SEARCH_SORTKEY, "sort_year_asc"
        );
        DocumentResponse docResponse = new DocumentResponse(
            "filter", "query", 0, 10, "PublicationDate", false,
            new String[0], 10, 10);
        ResponseCollection responses = new ResponseCollection();
        responses.add(docResponse);
        adjuster.adjust(request, responses);

        assertEquals("The adjusted response should contain rewritten sort",
                     "sort_year_asc, sort_year_desc",
                     ((DocumentResponse)responses.iterator().next()).
                         getSortKey());
    }

    public void testDocumentFieldRewrite() {
        InteractionAdjuster adjuster = createAdjuster();

        {
            Request request = new Request(
                DocumentKeys.SEARCH_SORTKEY, "sort_year_asc",
                InteractionAdjuster.SEARCH_ADJUST_RESPONSE_FIELDS_ENABLED, true
            );
            ResponseCollection responses = createSampleResponseWithFields();
            adjuster.adjust(request, responses);
            assertEquals("The adjusted response should contain rewritten field",
                         "author_normalised",
                         ((DocumentResponse)responses.iterator().next()).
                             getRecords().get(0).getFields().get(0).getName());
        }

        {
            Request request = new Request(
                DocumentKeys.SEARCH_SORTKEY, "sort_year_asc",
                InteractionAdjuster.SEARCH_ADJUST_RESPONSE_FIELDS_ENABLED, false
            );
            ResponseCollection responses = createSampleResponseWithFields();
            adjuster.adjust(request, responses);

            assertEquals("The adjusted response should not rewrite",
                         "Author",
                         ((DocumentResponse)responses.iterator().next()).
                             getRecords().get(0).getFields().get(0).getName());
        }
    }

    private ResponseCollection createSampleResponseWithFields() {
        DocumentResponse docResponse = new DocumentResponse(
            "filter", "query", 0, 10, "PublicationDate", false,
            new String[0], 10, 10);
        DocumentResponse.Record record = new DocumentResponse.Record(
            "foo", "bar", 1.0f, "dummy");
        record.addField(new DocumentResponse.Field("Author", "baz", true));
        docResponse.addRecord(record);
        ResponseCollection responses = new ResponseCollection();
        responses.add(docResponse);
        return responses;
    }

    private InteractionAdjuster createAdjuster() {
        Configuration conf = Configuration.newMemoryBased(
            InteractionAdjuster.CONF_IDENTIFIER, "tester",
/*            InteractionAdjuster.CONF_ADJUST_FACET_FIELDS,
            "author_normalised - Author, lma_long - ContentType, "
            + "llang - Language, lsubject - SubjectTerms",*/
            InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS,
            "recordID - ID, author_normalised - Author, "
            + "lma_long - ContentType, "
            + "llang;lang - Language, "
            + "fa;fb - FieldA;FieldB, "
            + "lsubject - SubjectTerms, "
            + "sort_year_asc;sort_year_desc - PublicationDate"
        );
        conf.set(InteractionAdjuster.CONF_ADJUST_FACET_FIELDS,
                 new ArrayList<String>(Arrays.asList(
            "author_normalised - Author", "lma_long - ContentType",
            "llang;lang - Language", "lsubject - SubjectTerms"))
        );
        List<Configuration> tags;
        try {
            tags = conf.createSubConfigurations(
                InteractionAdjuster.CONF_ADJUST_FACET_TAGS, 3);
        } catch (IOException e) {
            throw new RuntimeException("Configuration creation failed", e);
        }
        tags.get(0).set(TagAdjuster.CONF_FACET_NAME, "llang, lang");
        tags.get(0).set(TagAdjuster.CONF_TAG_MAP,
                        new ArrayList<String>(Arrays.asList(
                        "English - eng",
                        "MyLang - one;two",
                        "Moo;Doo - single",
                        "Space man - always;wanted",
                        "Source A;Source B - Dest A;Dest B",
                        "Boom - boo",
                        "Boom - hoo",
                        "Bim;Bam - bi;ba")));
        tags.get(1).set(TagAdjuster.CONF_FACET_NAME, "lma_long");
        tags.get(1).set(TagAdjuster.CONF_TAG_MAP, "Audio Sound - audio");
        tags.get(2).set(TagAdjuster.CONF_FACET_NAME, "fa");
        tags.get(2).set(TagAdjuster.CONF_TAG_MAP, "ContentA - ca");
        return new InteractionAdjuster(conf);
    }

    public void testQueryFieldRewrite() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "(+Language:\"foo\" +bar)", "llang:\"foo\" bar");
        assertAdjustment(adjuster,
                         "(FieldA:\"ContentA\" OR FieldB:\"ContentA\")",
                         "fa:ca");
    }

    public void testQueryTagRewrite_nonAdjusting() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "(+Language:\"foo\" +bar)", "llang:\"foo\" bar");
        assertAdjustment(adjuster, "English", "English");
        assertAdjustment(adjuster, "eng", "eng");
        assertAdjustment(adjuster, "year:\"2010\"", "year:2010");
        assertAdjustment(adjuster, "ContentType:\"eng\"", "lma_long:eng");
        assertAdjustment(adjuster, "ContentType:\"gryf\"", "lma_long:gryf");
        assertAdjustment(adjuster, "Language:\"eng dan\"", "llang:\"eng dan\"");
    }

    public void testQueryTagRewrite_nonAdjustingPair() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster, "year:\"2010\"", "year:2010");
    }

    public void testQueryTagRewrite_1to1() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster, "Language:\"English\"", "llang:eng");
        assertAdjustment(adjuster, "Language:\"English\"", "llang:\"eng\"");
        assertAdjustment(adjuster, "Language:\"MyLang\"", "llang:\"one\"");
        assertAdjustment(adjuster, "Language:\"MyLang\"", "llang:\"two\"");
    }

    public void testQueryTagAndFieldRewrite() {
        InteractionAdjuster adjuster = createAdjuster();
        // Tag
        assertAdjustment(adjuster, "Language:\"English\"", "llang:eng");
        // Field
        assertAdjustment(adjuster, "Language:\"English\"", "lang:English");
        // Field + Tag
        assertAdjustment(adjuster, "Language:\"English\"", "lang:eng");
    }

    public void testQueryTagRewrite_1ton() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster, "Language:\"MyLang\"", "llang:\"one\"");
        assertAdjustment(adjuster, "Language:\"MyLang\"", "llang:\"two\"");
        assertAdjustment(adjuster, "Language:\"Boom\"", "llang:boo");
        assertAdjustment(adjuster, "Language:\"Boom\"", "llang:hoo");
        assertAdjustment(adjuster, "Language:\"Space man\"", "llang:always");
    }

    public void testQueryTagRewrite_phrase() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "Language:\"empty space\"", "llang:\"empty space\"");
        assertAdjustment(adjuster,
                         "Language:\"Space man\"", "llang:always");
        assertAdjustment(adjuster,
                         "(Language:\"Source A\" OR Language:\"Source B\")",
                         "llang:\"Dest A\"");
    }

    public void testQueryTagRewrite_range() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "Language:[dan TO eng]", "llang:[dan TO eng]");
    }

    public void testQueryTagRewrite_prefix() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "Language:da*", "llang:da*");
    }

    public void testQueryTagRewrite_fuzzy() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "Language:da~2.0", "llang:da~");
    }

    public void testQueryTagRewrite_multiValue() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "(Language:\"foo\" OR Language:\"English\")",
                         "llang:(foo OR eng)");
    }

    public void testQueryTagRewrite_nto1() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "(Language:\"Moo\" OR Language:\"Doo\")",
                         "llang:\"single\"");
    }

    public void testQueryTagRewrite_multiple() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(
            adjuster,
            "(+(Language:\"Moo\" OR Language:\"Doo\") +Language:\"Boom\")",
            "llang:\"single\" llang:boo");
    }

    public void testQueryTagRewrite_nton() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "(Language:\"Bim\" OR Language:\"Bam\")",
                         "llang:\"bi\"");
        assertAdjustment(adjuster,
                         "(Language:\"Bim\" OR Language:\"Bam\")",
                         "llang:\"ba\"");
    }

    private void assertAdjustment(
        InteractionAdjuster adjuster, String expected, String query) {
        Request request = new Request(
            DocumentKeys.SEARCH_FILTER, query,
            DocumentKeys.SEARCH_QUERY, query
        );
        Request rewritten = adjuster.rewrite(request);
        assertEquals("The query should be rewritten correctly",
                     expected,
                     rewritten.get(DocumentKeys.SEARCH_QUERY));
        assertEquals("The filter should be rewritten correctly",
                     expected,
                     rewritten.get(DocumentKeys.SEARCH_FILTER));
    }
}
