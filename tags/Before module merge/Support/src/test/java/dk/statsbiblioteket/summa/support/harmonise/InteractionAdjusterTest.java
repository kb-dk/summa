package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ConvenientMap;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultImpl;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult.Reliability;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                     Strings.join(rewritten.getStrings(FacetKeys.SEARCH_FACET_FACETS), ", "));
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
        Structure structure = new Structure("ContentType", 10);
        FacetResultExternal summaFacetResult = new FacetResultExternal(
            new HashMap<String, Integer>(), facetIDs, fields, structure);
        summaFacetResult.setPrefix("interactionadjustertest");
        //noinspection unchecked
        summaFacetResult.getMap().put("ContentType", Arrays.asList(
            new FacetResultImpl.Tag<String>("Audio Sound", 87,Reliability.PRECISE)));
                
        ResponseCollection responses = new ResponseCollection();
        responses.add(summaFacetResult);

        adjuster.adjust(request, responses);

        assertFacetResultRewrite(responses, "lma_long", "audio");
    }

    public void testFacetFieldMultiTarget() {
        InteractionAdjuster adjuster = createAdjuster();

        Request request = new Request(
            FacetKeys.SEARCH_FACET_FACETS, "lma_long"
        );

        HashMap<String, Integer> facetIDs = new HashMap<String, Integer>();
        facetIDs.put("ContentType", 0);
        HashMap<String, String[]> fields = new HashMap<String, String[]>();
        fields.put("ContentType", new String[]{"ContentType"});
        Structure structure = new Structure("ContentType", 10);
        FacetResultExternal summaFacetResult = new FacetResultExternal(
            new HashMap<String, Integer>(), facetIDs, fields, structure);
        summaFacetResult.setPrefix("interactionadjustertest");
        //noinspection unchecked
      
        summaFacetResult.getMap().put("ContentType", Arrays.asList(
                new FacetResultImpl.Tag<String>("Newspaper Article", 2,Reliability.PRECISE),
                new FacetResultImpl.Tag<String>("Journal Article", 5,Reliability.PRECISE)        		
        		));

        
        ResponseCollection responses = new ResponseCollection();
        responses.add(summaFacetResult);

        adjuster.adjust(request, responses);
        assertTagCount(responses, "lma_long", "avisart", 2);
        assertTagCount(responses, "lma_long", "tssart", 5);
        assertTagCount(responses, "lma_long", "artikel", 7);
   }

    private void assertTagCount(ResponseCollection responses, String facet,
                                String tag, int count) {
        FacetResultExternal fr =
            (FacetResultExternal)responses.iterator().next();
        List<FacetResultImpl.Tag<String>> tags = fr.getMap().get(facet);
        for (FacetResultImpl.Tag<String> currentTag: tags) {
            if (tag.equals(currentTag.getKey())) {
                assertEquals("The count for tag '" + tag + "' should match",
                             (int) Integer.valueOf(count), currentTag.getCount());
                return;
            }
        }
        fail("The tag '" + tag + "' with count " + count + " could not be "
             + "located in facet '" + facet + "'");
    }

    private void assertFacetResultRewrite(
        ResponseCollection responses, String field, String term) {
        assertEquals("The adjusted response should contain rewritten facet query field for material type",
                     field,
                     ((FacetResultExternal)responses.iterator().next()).
                         getFields().entrySet().iterator().next().getValue()[0]);
        assertEquals("The adjusted response should contain rewritten field for material type",
                     field,
                     ((FacetResultExternal)responses.iterator().next()).
                         getMap().entrySet().iterator().next().getKey());
        assertEquals("The adjusted response should contain rewritten tag for material type",
                     term,
                     ((FacetResultExternal)responses.iterator().next()).
                         getMap().entrySet().iterator().next().getValue().get(0).getKey());
    }

    public void testSortResultRewrite() {
        InteractionAdjuster adjuster = createAdjuster();

        Request request = new Request(
            DocumentKeys.SEARCH_SORTKEY, "sort_year_asc"
        );
        DocumentResponse docResponse = new DocumentResponse(
            "filter", "query", 0, 10, "PublicationDate", false, new String[0], 10, 10);
        ResponseCollection responses = new ResponseCollection();
        responses.add(docResponse);
        adjuster.adjust(request, responses);

        assertEquals("The adjusted response should contain rewritten sort",
                     "sort_year_asc, sort_year_desc",
                     ((DocumentResponse)responses.iterator().next()).getSortKey());
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

    private static final Serializable TEST_ADJUSTER_ID = "tester";
    private InteractionAdjuster createAdjuster() {
        Configuration conf = createAdjusterConfiguration();
        return new InteractionAdjuster(conf);
    }

    private Configuration createAdjusterConfiguration() {
        Configuration conf = Configuration.newMemoryBased(
            InteractionAdjuster.CONF_IDENTIFIER, TEST_ADJUSTER_ID,
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
                 new String[]{
                     "author_normalised - Author",
                     "lma_long - ContentType",
                     "llang;lang - Language",
                     "lsubject - SubjectTerms"
                 });

        conf.set(InteractionAdjuster.CONF_ADJUST_UNSUPPORTED_FIELDS,
        	    new String[]{
     		        "ma",
     		        "unsupported_field"
                });
        conf.set(InteractionAdjuster.CONF_ADJUST_UNSUPPORTED_QUERY,"year:999999");


        List<Configuration> tags;
        try {
            tags = conf.createSubConfigurations(
                InteractionAdjuster.CONF_ADJUST_FACET_TAGS, 3);
        } catch (IOException e) {
            throw new RuntimeException("Configuration creation failed", e);
        }
        tags.get(0).set(TagAdjuster.CONF_FACET_NAME, "llang, lang");
        tags.get(0).set(TagAdjuster.CONF_TAG_MAP,
                        new String[]{
                        "English - eng",
                        "MyLang - one;two",
                        "Moo;Doo - single",
                        "Space man - always;wanted",
                        "Source A;Source B - Dest A;Dest B",
                        "Boom - boo",
                        "Boom - hoo",
                        "Bim;Bam - bi;ba"
                        });

        tags.get(1).set(TagAdjuster.CONF_FACET_NAME, "lma_long");
        tags.get(1).set(TagAdjuster.CONF_TAG_MAP,
                        new String[]{ // Alternative syntax
                            "Audio Sound - audio",
                            "Article - artikel",
                            "Book Chapter - artikel;artikelibog",
                            "Journal Article - artikel;tssart",
                            "Book Review - artikel",
                            "Magazine Article - artikel;magart",
                            "Newsletter - artikel",
                            "Newspaper Article - artikel;avisart",
                            "Publication Article - artikel",
                            "Trade Publication Article - artikel",
                        });

        tags.get(2).set(TagAdjuster.CONF_FACET_NAME, "fa");
        tags.get(2).set(TagAdjuster.CONF_TAG_MAP, "ContentA - ca");
        return conf;
    }

    public void testQueryFieldRewrite() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster, "Language:\"foo\" \"bar\"", "llang:\"foo\" bar");
        assertAdjustment(adjuster, "FieldB:\"ContentA\" OR FieldA:\"ContentA\"", "fa:ca");
    }

    
    public void testQueryUnsupportedField() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster, "\"foo\" year:\"999999\"", "foo ma:bar");
    }

    
    
    public void testQueryFieldMultiple() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "ContentType:\"Newspaper Article\" OR "
                         + "ContentType:\"Magazine Article\" OR "
                         + "ContentType:\"Newsletter\" OR "
                         + "ContentType:\"Book Review\" OR "
                         + "ContentType:\"Trade Publication Article\" OR "
                         + "ContentType:\"Publication Article\" OR "
                         + "ContentType:\"Journal Article\" OR "
                         + "ContentType:\"Book Chapter\" OR "
                         + "ContentType:\"Article\"",
                         "lma_long:\"artikel\"");
        assertAdjustment(adjuster,
                         "-ContentType:\"Book Chapter\"",
                         "-lma_long:\"artikelibog\"");
    }


    public void testQueryTagRewrite_nonAdjusting() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster, "Language:\"foo\" \"bar\"", "llang:\"foo\" bar");
        assertAdjustment(adjuster, "\"English\"", "English");
        assertAdjustment(adjuster, "\"eng\"", "eng");
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
                         "Language:\"Source B\" OR Language:\"Source A\"",
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
                         "Language:\"foo\" OR Language:\"English\"",
                         "llang:(foo OR eng)");
    }

    public void testQueryDividerRewrite_multiValue() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster, "\"foo\" \"-\" \"bar\"", "foo - bar");
    }

    public void testQueryDividerPhraseRewrite_multiValue() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster, "\"foo - bar\"", "\"foo - bar\"");
    }

    public void testQueryTagRewrite_nto1() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "Language:\"Doo\" OR Language:\"Moo\"",
                         "llang:\"single\"");
    }

    public void testQueryTagRewrite_multiple() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(
            adjuster,
            "(Language:\"Doo\" OR Language:\"Moo\") Language:\"Boom\"",
            "llang:\"single\" llang:boo");
    }

    public void testQueryTagRewrite_nton() {
        InteractionAdjuster adjuster = createAdjuster();
        assertAdjustment(adjuster,
                         "Language:\"Bim\" OR Language:\"Bam\"",
                         "llang:\"bi\"");
        assertAdjustment(adjuster,
                         "Language:\"Bim\" OR Language:\"Bam\"",
                         "llang:\"ba\"");
    }

    public void testMultiply() {
        testAdjustment("Untouched record", "foo bar", new ConvenientMap(
            ), 1.0f);
        testAdjustment("Multiply modified record", "foo bar", new ConvenientMap(
                InteractionAdjuster.SEARCH_ADJUST_SCORE_MULTIPLY, 2.0f
            ), 2.0f);
        testAdjustment("Multiply id-based modified record", "foo bar", new ConvenientMap(
                TEST_ADJUSTER_ID + "." + InteractionAdjuster.SEARCH_ADJUST_SCORE_MULTIPLY, 2.0f
            ), 2.0f);
        testAdjustment("Multiply id-based stacked modified record", "foo bar", new ConvenientMap(
                InteractionAdjuster.SEARCH_ADJUST_SCORE_MULTIPLY, 2.0f,
                TEST_ADJUSTER_ID + "." + InteractionAdjuster.SEARCH_ADJUST_SCORE_MULTIPLY, 2.0f
            ), 4.0f);
        testAdjustment("Multiply id-based simple modified record", "foo bar", new ConvenientMap(
                TEST_ADJUSTER_ID + "." + InteractionAdjuster.SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY, 3.0f
            ), 3.0f);
        testAdjustment("Multiply id-based simple stacked modified record", "foo bar", new ConvenientMap(
                InteractionAdjuster.SEARCH_ADJUST_SCORE_MULTIPLY, 2.0f, // Overridden below
                InteractionAdjuster.SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY, 1.0f, // Overrides above
                TEST_ADJUSTER_ID + "." + InteractionAdjuster.SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY, 3.0f
            ), 3.0f);
        testAdjustment("Multiply modified non-simple record", "foo -bar", new ConvenientMap(
                InteractionAdjuster.SEARCH_ADJUST_SCORE_MULTIPLY, 2.0f,
                InteractionAdjuster.SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY, 3.0f
            ), 2.0f);
        testAdjustment("Multiply modified simple record", "foo bar", new ConvenientMap(
                InteractionAdjuster.SEARCH_ADJUST_SCORE_MULTIPLY, 2.0f,
                InteractionAdjuster.SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY, 3.0f
            ), 3.0f);
    }

    public void testSimpleDetection() {
        testAdjustment("Indirect non-simple record trigger false", "foo", "-bar", true,
                       new ConvenientMap(
                           InteractionAdjuster.CONF_PURE_NEGATIVE_FILTER_TRIGGERS_NOT_SIMPLE, false),
                       new ConvenientMap(
                           DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, true,
                           InteractionAdjuster.SEARCH_ADJUST_SCORE_MULTIPLY, 2.0f,
                           InteractionAdjuster.SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY, 3.0f
                       ), 3.0f);
        testAdjustment("Indirect non-simple record trigger true", "foo", "-bar", true,
                       new ConvenientMap(
                           InteractionAdjuster.CONF_PURE_NEGATIVE_FILTER_TRIGGERS_NOT_SIMPLE, true),
                       new ConvenientMap(
                           DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, true,
                           InteractionAdjuster.SEARCH_ADJUST_SCORE_MULTIPLY, 2.0f,
                           InteractionAdjuster.SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY, 3.0f
                       ), 2.0f);
    }

    public void testAdjustment(String message, String query, ConvenientMap setup, float expected) {
        testAdjustment(message, query, null, false, new ConvenientMap(), setup, expected);
    }
    public void testAdjustment(
        String message, String query, String filter, boolean isPureNegative, ConvenientMap forcedSetup,
        ConvenientMap setup, float expected) {
        {
            Request request = new Request();
            for (Map.Entry<String, Serializable> entry: setup.entrySet()) {
                request.put(entry.getKey(), entry.getValue());
            }
            request.put(DocumentKeys.SEARCH_QUERY, query);
            if (filter != null) {
                request.put(DocumentKeys.SEARCH_FILTER, filter);
                request.put(DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, isPureNegative);
            }
            Configuration conf = createAdjusterConfiguration();
            for (Map.Entry<String, Serializable> entry: forcedSetup.entrySet()) {
                conf.set(entry.getKey(), entry.getValue());
            }
            InteractionAdjuster adjuster = new InteractionAdjuster(conf);
            ResponseCollection responses = createSampleResponseWithFields();
            adjuster.adjust(request, responses);
            assertEquals(message + " with query '" + query + " should have the expected weight for search-based setup",
                         expected, ((DocumentResponse)responses.iterator().next()).getRecords().get(0).getScore());
        }

        {
            Configuration conf = createAdjusterConfiguration();
            for (Map.Entry<String, Serializable> entry: forcedSetup.entrySet()) {
                conf.set(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Serializable> entry: setup.entrySet()) {
                conf.set(entry.getKey(), entry.getValue());
            }
            InteractionAdjuster adjuster = new InteractionAdjuster(conf);
            Request request = new Request(DocumentKeys.SEARCH_QUERY, query);
            if (filter != null) {
                request.put(DocumentKeys.SEARCH_FILTER, filter);
                request.put(DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, isPureNegative);
            }
            ResponseCollection responses = createSampleResponseWithFields();
            adjuster.adjust(request, responses);
            assertEquals(message + " with query '" + query + " should have the expected weight for conf-based setup",
                         expected, ((DocumentResponse)responses.iterator().next()).getRecords().get(0).getScore());

        }
    }

    /**
     * Tests whether the adjuster correctly rewrites a query going from the
     * outer caller to the searcher requiring special syntax or fields.
     * @param adjuster responsible for performing query adjustment.
     * @param expected the expected result
     * @param query will be rewritten using the adjuster and the result compared
     *        to expected.
     */
    private void assertAdjustment(InteractionAdjuster adjuster, String expected, String query) {
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
