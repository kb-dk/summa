/**  Licensed under the Apache License, Version 2.0 (the "License");
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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.common.util.SimplePair;
import dk.statsbiblioteket.summa.common.util.StringExtraction;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultImpl;
import dk.statsbiblioteket.summa.search.*;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.summa.support.harmonise.AdjustingSearchNode;
import dk.statsbiblioteket.summa.support.harmonise.HarmoniseTestHelper;
import dk.statsbiblioteket.summa.support.harmonise.InteractionAdjuster;
import dk.statsbiblioteket.summa.support.solr.SolrSearchNode;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.caching.TimeSensitiveCache;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XMLStepper;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("CallToPrintStackTrace")
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(SummonSearchNodeTest.class);

    public SummonSearchNodeTest(String name) {
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
        return new TestSuite(SummonSearchNodeTest.class);
    }

    protected XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // No resolving of external DTDs
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }

    public void testNestedSearcher() throws Exception {
        Configuration conf = Configuration.load(Resolver.getFile(
                "support/summon/search/nested_summon_searcher.xml").getAbsolutePath());
        SummaSearcher searcher = SummaSearcherFactory.createSearcher(conf);
        System.out.println(searcher.search(new Request(DocumentKeys.SEARCH_QUERY, "foo")));
        searcher.close();
    }
// summon_FETCH-LOGICAL-c2900-af812442b587e68894c61a9785d45a5ead438156d637cf77c5443275a054be473

    public void testSpecificSearch() throws RemoteException {
        //final String QUERY = "roberto constantini";
        final String QUERY = "id:FETCH-LOGICAL-c2900-af812442b587e68894c61a9785d45a5ead438156d637cf77c5443275a054be473";

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        summon.search(new Request(DocumentKeys.SEARCH_QUERY, QUERY), responses);
        System.out.println(responses.toXML());
}

    public void testDebug() throws RemoteException {
        //final String QUERY = "roberto constantini";
        final String QUERY = "hest";

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        summon.search(new Request(DocumentKeys.SEARCH_QUERY, QUERY, DocumentKeys.DEBUG, "true"), responses);
        String xml = responses.toXML();
        assertTrue("The response should contain a DebugResponse\n" + xml, xml.contains("DebugResponse"));
        System.out.println(responses.toXML());
}

    public void testSpecificProblemSearch() throws RemoteException {
        //final String QUERY = "roberto constantini";
        final String QUERY = "foo";

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long stated = getHitCount(summon, DocumentKeys.SEARCH_QUERY, QUERY);
        List<String> returned = getHits(summon, DocumentKeys.SEARCH_QUERY, QUERY);
        long min = Math.min(stated, DocumentKeys.DEFAULT_MAX_RECORDS);
        assertTrue("The number of returned records should be minimum " + min + " but was " + returned.size()
                   + " for " + Strings.join(returned),
                   returned.size() >= min);
        Collections.sort(returned);
//        System.out.println(
//                "Got hitCount=" + stated + " with first " + returned.size() + " IDs " + Strings.join(returned, "\n"));
    }

    public void testMoreLikeThis() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long standard = getHitCount(summon, DocumentKeys.SEARCH_QUERY, "foo");
        assertTrue("A search for 'foo' should give hits", standard > 0);

        long mlt = getHitCount(summon, DocumentKeys.SEARCH_QUERY, "foo", LuceneKeys.SEARCH_MORELIKETHIS_RECORDID, "bar");
        assertEquals("A search with a MoreLikeThis ID should not give hits", 0, mlt);
    }

    // 20140305: The service reportedly returns a maximum of 20 records from IDs
    public void testMultipleDocIDRequest() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        List<String> ids = getAttributes(summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 25
        ), "id", false);
        assertEquals("The must be the correct number of hits to generate a list of test-IDs for lookup",
                     25, ids.size());

        List<String> idsFromLookup = getAttributes(summon, new Request(
                DocumentKeys.SEARCH_IDS, Strings.join(ids)
        ), "id", false);
        assertEquals("There should be the right number of returned documents from explicit ID lookup. " +
                     "Returned IDs were\n" + Strings.join(idsFromLookup),
                     25, idsFromLookup.size());
        log.info("Query: 'foo', IDs: [" + Strings.join(idsFromLookup).replace("summon_", "") + "]");
    }

    // Should really be in sbutil, but I was feeling paranoid and wanted to be sure - te@
    public void testCacheImplementation() throws InterruptedException {
        TimeSensitiveCache<Integer, Integer> cache = new TimeSensitiveCache<>(400, true, 10);
        for (int i = 0 ; i < 100 ; i++) {
            cache.put(i, i);
        }
        assertEquals("After 100 insertions into a cache of size 10, there should be the right amount of entries",
                     10, cache.size());
        Thread.sleep(500);
        cache.put(1234, 5678);
        assertEquals("After sleeping longer than timeout and adding a single entry, the cache size should be correct",
                     1, cache.size());
    }

    public void testDocRequestCaching() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();

        List<String> ids = getAttributes(summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 25
        ), "id", false);
        assertEquals("The must be the correct number of hits to generate a list of test-IDs for lookup",
                     25, ids.size());
        String statsOuter = summon.getRecordCacheStats();
        assertTrue("The record cache should contain 25 entries after plain search: " + statsOuter,
                   statsOuter.contains("inserts=25"));

        List<String> reduced = new ArrayList<>(ids.subList(2, 20));
        {
            List<String> idsFromLookup = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_IDS, Strings.join(reduced)
            ), "id", false);
            assertEquals("There should be the right number of returned documents from reduced ID lookup. " +
                         "Returned IDs were\n" + Strings.join(idsFromLookup),
                         18, idsFromLookup.size());
            String stats = summon.getRecordCacheStats();
            assertTrue("The record cache should report 18 matches after lookup for 18 records: " + stats,
                       stats.contains("matches=18"));
        }
        summon.clearRecordCache();
        {
            List<String> idsFromLookup = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_IDS, Strings.join(reduced)
            ), "id", false);
            assertEquals("There should be the right number of returned documents from reduced ID lookup after cache "
                         + "clear. Returned IDs were\n" + Strings.join(idsFromLookup),
                         18, idsFromLookup.size());
            String stats = summon.getRecordCacheStats();
            assertTrue("The record cache should report 18 inserts after lookup for 18 records post-cache-clear: "
                       + stats,
                       stats.contains("inserts=18"));
            assertTrue("The record cache should report 18 misses after lookup for 18 records post-cache-clear: "
                       + stats,
                       stats.contains("misses=18"));
        }

        {
            List<String> idsFromLookup = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_IDS, Strings.join(ids)
            ), "id", false);
            assertEquals("Mixed cached and non-cached lookups should yield the full amount of responses. " +
                         "Returned IDs were\n" + Strings.join(idsFromLookup),
                         25, idsFromLookup.size());
            String stats = summon.getRecordCacheStats();
            assertTrue("After mixed cached and non-cache lookup, the number of matches should be 18: " + stats,
                       stats.contains("matches=18"));
            assertTrue("After mixed cached and non-cache lookup, the number of misses should be 18+7: " + stats,
                       stats.contains("misses=25"));
        }

        {
            List<String> idsFromLookup = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_IDS, Strings.join(ids)
            ), "id", false);
            assertEquals("Repeating the ID-lookup-request should yield the same number of responses. " +
                         "Returned IDs were\n" + Strings.join(idsFromLookup),
                         25, idsFromLookup.size());
            String stats = summon.getRecordCacheStats();
            assertTrue("After repeated mixed cached and non-cache lookup, the number of matches should be 18+25: "
                       + stats,
                       stats.contains("matches=43"));
        }
    }

    public void disabledtestChangedDocIDRequest() throws RemoteException {
        // Returned as summon_FETCH-LOGICAL-c611-6aa4a4e310c8434ecaf0289c01a569c2b03cf40b9ea8913fd9bc487fb3db1caa1
        final String ID = "summon_FETCH-eric_primary_EJ5633011";

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        List<String> returned = getHits(summon, DocumentKeys.SEARCH_IDS, ID);
        assertEquals("There should be a hit", 1, returned.size());
        System.out.println("Requested: " + ID);
        System.out.println("Received:  " + returned.get(0));
    }

    public void testOtherPrefixDocIDRequest() throws RemoteException {
        // Returned as summon_FETCH-LOGICAL-c611-6aa4a4e310c8434ecaf0289c01a569c2b03cf40b9ea8913fd9bc487fb3db1caa1
        final String ID = "summon_chadwyckhealey_pio_511600010016";

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        List<String> returned = getHits(summon, DocumentKeys.SEARCH_IDS, ID);
        assertEquals("There should be a hit", 1, returned.size());
        System.out.println("Requested: " + ID);
        System.out.println("Received:  " + returned.get(0));
    }

    public void testBulkDocIDRequestSingle() throws RemoteException {
        assertBulkDocIDRequest("Single document", 1, "FETCH-proquest_dll_15622214411");
    }

    public void testBulkIDOverflow() throws Exception {
        SearchNode summon = SummonTestHelper.createPagingSummonSearchNode();
        List<String> ids = getAttributes(summon, new Request(
                DocumentKeys.SEARCH_QUERY, "whale",
                DocumentKeys.SEARCH_MAX_RECORDS, 120
        ), "id", false);
        assertTrue("There should be at least 51 IDs (to trigger paging), but there were only " + ids.size(),
                   ids.size() > 50);

        String[] aIDs = new String[ids.size()];
        ids.toArray(aIDs);
        log.info("testBulkIDOverflow: Got " + ids.size() + " IDs");
        assertBulkDocIDRequest("Paging request", summon, aIDs.length, aIDs);
    }

/*    public void testBulkIDOverflowSpecific120() throws Exception {
        SearchNode summon = SummonTestHelper.createPagingSummonSearchNode();
        String[] ids = new String[]{"summon_FETCH-LOGICAL-c1211-450eaf87622b35ef54c3a658f40e1fba6f4ebe96b4b2588cb7aaa8335b65a0423", "summon_FETCH-LOGICAL-c741-3990b6b90eed6b30a24fe050b2ff55483c45c7e5c0fbd5828bcf1b92cd6e83f83", "summon_FETCH-LOGICAL-g464-24ffd8f898703945098194af6b790a69245b4670d1e177cb5cbc1be6ed0342523", "summon_FETCH-LOGICAL-c751-582a6d14ec191bd4f125e1625b822f81e9d626eda5aa9425e7027851aa70326b3", "summon_FETCH-LOGICAL-p651-c14c8e42de597408a1ce023e65efeb47d26c8a97eead8524e3b401ea572f50093", "summon_FETCH-LOGICAL-c1814-b8b29a06ca1ef477a8334e6445408ec12c1d55439a1fd8489eee904c8532f8ae3", "summon_FETCH-LOGICAL-g462-fe4382da483d56b454803eee3fc576c6a9f487edd94706b99111a7313118b86f3", "summon_FETCH-gale_infotrac_2575123693", "summon_FETCH-LOGICAL-g838-20d5d0c7232f464099b34eb6ddea002717cccd795ba464d2c7cc15952608cb393", "summon_FETCH-LOGICAL-p1126-d7f42da23ec810187e7d4689a60edba0973291d254eed6661a9d1fe905d3737d3", "summon_FETCH-LOGICAL-c991-32daac758b818409e08fae4c79befe571a1e114e82f3734da79334c95ca6e7883", "summon_FETCH-gale_infotrac_1593091683", "summon_FETCH-LOGICAL-p590-21d116636b44448a7cadcc0cb00d140652db426d48ccc77d64661e69e48a37623", "summon_FETCH-LOGICAL-c2049-796a3a123ac6174d23626e87bf7947c34aa6bd1ba29d82ce5301c0c2e7e9aae83", "summon_FETCH-gale_infotrac_4092383873", "summon_FETCH-crossref_primary_10_1016_j_tree_2015_04_0013", "summon_FETCH-crossref_primary_10_1016_j_cub_2012_08_0553", "summon_FETCH-crossref_primary_10_1038_nature_2012_116353", "summon_FETCH-gale_infotrac_4045900293", "summon_FETCH-LOGICAL-c1303-1adea88a3e05cc16c3d1a25173bfa19d4e3726915271fbd1b5e7c3bf65f68103", "summon_FETCH-LOGICAL-g820-1ee99ff4a0ca18fc086954cd8ef3f1b137e33e558883560fab8d6933ec2348363", "summon_FETCH-LOGICAL-c2525-3a79196724c25a7daa44b5d73c9ee5a2b769baf0425335e12ee22caa96fcefe23", "summon_FETCH-gale_incontextgauss_3663463403", "summon_FETCH-proquest_dll_30364492813", "summon_FETCH-LOGICAL-c819-2f65c3ac7d420365716e664cb097d483157548f156dcffaa7d0cee4296f5326e3", "summon_FETCH-LOGICAL-g3731-390d7fed9fbcbeca480850e02dfec1334d64ee416a4935a83e097d734be6fd423", "summon_FETCH-LOGICAL-c1069-515283ed45f6c86bdb4813db1d53598358860f8e069b35761e2476c19aa005c33", "summon_FETCH-LOGICAL-d891-76d72e4e03091f1e1d9cb270d4b001200f5937fb32d354a1e2ea07d99e5a376a3", "summon_FETCH-LOGICAL-c1775-89e29400dc62d28d83344a540e8de1c064a861b8afc96f01d5f53e658a21b20d3", "summon_FETCH-proquest_dll_19459428213", "summon_FETCH-LOGICAL-g1481-a8df3c636755b850e687197c5c37f077ab8d929dba33c661b33174f45569c72b3", "summon_FETCH-LOGICAL-c1125-dc2b967ef6b21c27470e8ebf609a25478837de0a196192299e2bd613c6b8369e3", "summon_FETCH-LOGICAL-g596-fa4586e40c545b9292a106d895f5ccf9d69f1ef80d6fbaafd0809020f8712b5c3", "summon_FETCH-LOGICAL-c1313-d6d449e3469f57aa4ffb236cee45b45d0de8431cdb6e46e78626a46b0902e2883", "summon_FETCH-LOGICAL-c2339-5619fc0409c0ed41bb248f28df170be7c4f627d8fd1f1cfc6f3d972ea897980f3", "summon_FETCH-proquest_dll_23026674313", "summon_FETCH-LOGICAL-c1496-f186f7be592541d812db5613935b1177b61efc4d970224ae12bab606689ca2693", "summon_FETCH-LOGICAL-g523-9210d2a9efd4e29662a1ffec1e12758bc6d3b98e5184f71be89a335a066a26d93", "summon_FETCH-crossref_primary_10_1016_j_annals_2015_05_0053", "summon_FETCH-crossref_primary_10_1126_science_aac89053", "summon_FETCH-LOGICAL-c700-7c506c9c0a52d17700753e64ec28bcdb1ab0970c1d16839b1c8d2e852efb41f23", "summon_FETCH-LOGICAL-c1611-aa2d317a3d325cfa9f60c781c612ea50510bec610f61a90ca1bae986a8f4099e3", "summon_FETCH-crossref_primary_10_1126_science_aaa78213", "summon_FETCH-LOGICAL-g821-96dd61a72fc29226f25d7ab9fa72c8de8a401613dfb685008b51916d6a4783d13", "summon_FETCH-LOGICAL-g597-cb0a2f7dbe774808967d0ce1ad65a15bfb4c37afa9da6d969a0371a24d40a1603", "summon_FETCH-LOGICAL-c968-19e72a9bc550cde29303ee61b1ad10659387e52e907037d8616ba245c0a2c8993", "summon_FETCH-LOGICAL-c1775-2c88aeab9962860bb2ebba82dd84b098e9f32fc160c5f6a2d7062db1fcd8e38a3", "summon_FETCH-crossref_primary_10_1038_climate_2010_323", "summon_FETCH-LOGICAL-p595-bcc21df9812c99d6f4f59229e44395519dc35d23fb5bf1b5f646741bc5ab2bde3", "summon_FETCH-LOGICAL-c666-d1cb544351eaaec4d7bbd017ccbc2f11b1d588986bd8d86883e370d2a7c12d513", "summon_FETCH-LOGICAL-c4126-a8b7ebe506fb919b94daf7c7a81535393ae559bc0f99de6cd2e6d35964166ec3", "summon_FETCH-LOGICAL-p521-7f72fee6ca716b534650c759a372ea5373f73873f33aa1c6b42902b33e5e6add3", "summon_FETCH-LOGICAL-c104b-b938a6e6174816c35b1d3a7580e6503e622cd818015e18f1241559bbdecb77d43", "summon_FETCH-LOGICAL-g3252-60928f7205b1ce653b294bed146174692a799f3e54efee9e944b108416de16783", "summon_FETCH-LOGICAL-c3929-ea3b77f257c351e6c84d2d0e4be2bbfe33c96b4d109c38df92e0dfe1f3ad98683", "summon_FETCH-crossref_primary_10_1242_jeb_1058333", "summon_FETCH-LOGICAL-c975-202a73ac17fbc0b3f08295ae1ceda753da02783089e83a412dd548295422821f3", "summon_FETCH-doaj_primary_oai_doaj_org_article_9d5718b1f5ff462a9c8e019a31f63ae33", "summon_FETCH-projectmuse_primary_bulletin_of_the_center_for_childrens_books_v068_68_2_stevenson07_The_Storm_Whale_by_Benji_Davies_review3", "summon_FETCH-LOGICAL-g751-d3dfb28923c7c2901d8dcf19c8cb43e1b37311107838467d4b69f8a862a8fb263", "summon_FETCH-LOGICAL-c2125-4152387ccd63ead977e1eacad1714df38e896fdcbd0a4a583b68fd402724a2583", "summon_FETCH-LOGICAL-g1580-b57d13b5f64621f01bb67fa85051d7aa3876f185841a2b547d8a722b07b733ea3", "summon_FETCH-LOGICAL-c2189-22d2fb4040692be91446b76e831564db2d2f8beeeca5ae5c93298c066f0f93253", "summon_FETCH-LOGICAL-c1667-8f3dabe219bca71bdf033d2a21133249699a9557167b0905c226f689e12b17203", "summon_FETCH-LOGICAL-c3512-47064d35d339db887da194821c1f24b15cfba249fc49e11a0fe42c7db9e3f5cd3", "summon_FETCH-LOGICAL-c3541-fb76b9b80d262cd0c15c3e7bdb23da57cbf643c3bcfd310a4406706dd3da299d3", "summon_FETCH-LOGICAL-c1778-c84f9bf79b42a457de3436786a5657020714002445e9007f3da8bd6b739bd4e53", "summon_FETCH-LOGICAL-c3934-8c4e775e76dd829322caf0623c0fcd7ea3b9ae7920c38e8faa0f88715d7cde733", "summon_FETCH-LOGICAL-c1314-ebde0b025042887b1ddbea6c52a51a44493217130e73ba38031ea9a0113767ae3", "summon_FETCH-LOGICAL-g593-5a80fdd1b3f5bd4a926d1058be2c90f1e891a691323f080ae193024e225301733", "summon_FETCH-LOGICAL-g3517-64bc3bbdec08bbaeb00444f5dd625cd4ff4fccab97bad45367294f38da2dc5dc3", "summon_FETCH-LOGICAL-c3048-cd234f3dfa937d2c03558c63b19d4ccd1912903bc9192b034d1e29249e5054133", "summon_FETCH-LOGICAL-p1638-ba1a72c510259c682080e4db3b8c68654f5d0e508f7e0dbd3468204893adaf6d3", "summon_FETCH-pubmed_primary_258729583", "summon_FETCH-LOGICAL-c1576-117fcf4fd1b9948fb5159e32c940ca274ae6abdfa7893b25819865b6d87687c53", "summon_FETCH-LOGICAL-g3427-c6d41ce16b8a0c1e5ff17af20eba5b2ab68d59f56c0a1f15c74dddec8a03992d3", "summon_FETCH-LOGICAL-d1493-be05eb5f9ae080872c5e41104ca1737798ca547b2f6b6ee04f398f14240b00173", "summon_FETCH-LOGICAL-p1547-4bb1c72521ea180f5b66a8d947d3e326f8ba36c6da4beca6738f4b46a4deb8b23", "summon_FETCH-projectmuse_primary_theatre_journal_v065_65_3_garner_The_Whale_by_Samuel_D_Hunter_review3", "summon_FETCH-projectmuse_primary_studies_in_american_indian_literatures_v025_25_4_johnston_corpse_whale_by_dg_nanouk_okpik_review3", "summon_FETCH-LOGICAL-c662-2932d988af0f3d5a968b992482f87d9f370d695d6d25caebf2089cc0129935563", "summon_FETCH-proquest_dll_34599570013", "summon_FETCH-LOGICAL-p1111-f659b3898d86db0ecb08503ea890786ad3df2510d5c81f78bf62bd04a224a29e3", "summon_FETCH-LOGICAL-p1352-a1f61379f5b5e15f583fcb785b290ab145b9973af7ee44b26c93057566966bcb3", "summon_FETCH-LOGICAL-c1936-786b2c7c732f1d67de5a6aef0376243d3f4bc9381461b3365d97da72323a18a53", "summon_FETCH-proquest_dll_37046308313", "summon_FETCH-crossref_primary_10_1126_science_aac88493", "summon_FETCH-LOGICAL-p521-46db3a96a242a889582aff24225d0448c19ea5f2c0d0bb01820c5fb1a2f0f7c33", "summon_FETCH-LOGICAL-p975-2b60914620deea77d57c3cb827908bd7cda809d5c905fb20b1da7a4dc49b32c13", "summon_FETCH-crossref_primary_10_1099_ijs_0_0002593", "summon_FETCH-LOGICAL-c1836-c0500b591b95684947af827d62ff812ee7413ba5c13dc3ee2ec7b0269c53b53e3", "summon_FETCH-LOGICAL-c711-7859219805d319e0d745aeb1f96bbd837a5ada90bbdab6ac7104441a2bf79dd73", "summon_FETCH-LOGICAL-c1001-2dbdb2830dfaa72f7b59741eb20d1386caa104b3484349a41fdff30e9d1914fb3", "summon_FETCH-LOGICAL-p594-14d41603c9debadd631f406ba6627246cc0a2094b02345206b30bf1cc49f4ad93", "summon_FETCH-LOGICAL-p1516-32dc664a267f65b15c59a92b3ebdd95f111cc9927ed6b4b67ff64f978938fdf63", "summon_FETCH-LOGICAL-p1355-ac2d9ad19fb1a8e3ac5a30bc5b22b3237d673f691105314a7436a8dabd403e753", "summon_FETCH-LOGICAL-p762-e54683a18286d45c0936806b81fc98a5f2c2e6b84c35baade3c1688705f7065f3", "summon_FETCH-LOGICAL-p1018-6bc60371ac9b2ae797db4b92e338418698e111fd672d702c7b303a4ac7eee9e53", "summon_FETCH-LOGICAL-p838-63005b7e961fd8554c11a1b7f33d56e907c38fc3708f95b0779156dc228c45423", "summon_FETCH-LOGICAL-p1127-6db3d0d4c8a46afde9718941b778de75c3baa104df3fe460108f3425b68141933", "summon_FETCH-LOGICAL-c1622-c299288723ca040fb72e56b2a19851fb699276d46ce7646d923779aeb773a3b53", "summon_FETCH-crossref_primary_10_1016_j_ocecoaman_2015_04_0063", "summon_FETCH-LOGICAL-c1553-22c89cbdfd68f1e8f52090d9bb911f1603d8216766fc65c2dac8b9c8cf85d79c3", "summon_FETCH-LOGICAL-c2305-593619857d315a927fa7ee5bb07fc23bf79224abd72ee29ce9021c324f272c3a3", "summon_FETCH-LOGICAL-c2612-923b3b58f8865c8d52115c4eac6b513902a520847234404476e744dac6a98e093", "summon_FETCH-LOGICAL-p1515-1c73542f14d2e4cabfcb1bf1f332f44cd149b0c57360cf42c60ef387d1d9c6cf3", "summon_FETCH-LOGICAL-c691-b947e51f3821e73557bb004056edd242d1b6fb533d0558aeff47113301f5965d3", "summon_FETCH-LOGICAL-p1509-9c17ae9d661bceb09e75df46b4908c88f3c35f7f29497b585bb8f404eecf55343", "summon_FETCH-LOGICAL-g1543-2ca668e5f467ea875a0a1985cce72ad97a88eb859f78376fe53892d18956c5bf3", "summon_FETCH-LOGICAL-c54c-687b2501e415335fc5143440436f1c8b8f19fa9d0082a7e4c481c6aef5051caa3", "summon_FETCH-LOGICAL-h1434-cb87222a5122d7992a9af613d2c3862684edbbbf5dbe87aee0ffeeb99acbe9763", "summon_FETCH-crossref_primary_10_1353_bcc_2015_02463", "summon_FETCH-LOGICAL-c938-3939643e3fedba37be73358d4145d447c20664cac5b28d32a62470ea41ff09973", "summon_FETCH-LOGICAL-c657-9e678e5cd1d4968d94a0d604a109de725fda53f0783d257bc3eedbccc1a74db33", "summon_FETCH-LOGICAL-p881-7b43cacdc218effdf3a419a3066ea4bc5e5f1708ceeffdba05b11014bf5138713", "summon_FETCH-LOGICAL-c2313-8c5e56ea624c30d3274aa634fe981486e855b6f310bf946a66ce2f83850c3e93", "summon_FETCH-LOGICAL-c791-7378ae69bdbe8fe6aa311508c8058f1a2c65d99469d665b41b853c374a2258d23", "summon_FETCH-LOGICAL-c2316-212f70a5a1949bdc2a88db648b598ff59ceb407fef88e93738332b0b2567c65e3", "summon_FETCH-LOGICAL-c1554-ebcf139ea1ee6ffb56d90036fbe81dd2b15289d3b576073fbeeb3d5f104f0dc23", "summon_FETCH-LOGICAL-p832-c0fc800f5075107899f52ec3a3e30aa3d2ebb1abf6255b7fac633689657b7c793", "summon_FETCH-LOGICAL-p2351-775d308b8d25ec75f26bc2c1f63c592450505635384131a7137e60b4979525e43", "summon_FETCH-LOGICAL-c556-6fc704b9c625ac81d9da9cae883d6777b7632021589b9e3bec42cc61bfbea7983", "summon_FETCH-LOGICAL-p763-189529b59b3f6e0d1307b7a467ead061383b89319f05b1cda604a1d3787880853", "summon_FETCH-LOGICAL-d891-916197de097ba318a181db39e23addf3cd3f9482bae6d87500dd01b6d0418cda3", "summon_FETCH-LOGICAL-c5162-c5afc8e389bed145fce35ccdd814c1820d558acd72f814c4878fccdc57fd06b03", "summon_FETCH-crossref_primary_10_1578_AM_40_3_2014_2253", "summon_FETCH-LOGICAL-c1779-cbf694af525272c03ebf6621e7be7bbaf614b7edaa28a9a173c99deb49f5867e3", "summon_FETCH-LOGICAL-g1559-77ce6abfd1f59c418e9f6f3767c52d27bccd7889808efe55669bd1f2215e6ab43", "summon_FETCH-LOGICAL-p883-160bbfce2444e44830e3e643b4ed40890f25891eac65602c8a787933ac5f5e863", "summon_FETCH-LOGICAL-p2091-19e0d8f221f29eeb994a9a4edb7c8a65f90a43ecde841f739c024ca8aebc800a3", "summon_FETCH-proquest_dll_30364492913", "summon_FETCH-LOGICAL-c946-5440261c0f5af65619f32357bb501333593f5b0580f2c6f81cc89eb8f998d9b43", "summon_FETCH-LOGICAL-g3849-3dc7724b7ffd4b7271368670d660c64b0322ebbbde4bc567f3f65cde7d0d2dbc3", "summon_FETCH-LOGICAL-b5322-d3699e197af32e64186a3bfc177981c88867cda89e52df5763eefea9715f658b3", "summon_FETCH-proquest_dll_30393425813", "summon_FETCH-chadwyckhealey_bscjournals_003595493", "summon_FETCH-LOGICAL-c4720-eeadc31f4d04cfb8a3a824fb083e924419d04b31a8c431e2a8f88d79588e53cc3", "summon_FETCH-LOGICAL-c1426-4722dd7907aa716783b3d2e9e3959205c3c3f937b44d757219519441bb08ee153", "summon_FETCH-LOGICAL-c1823-ab8abb8ebdccab4f49a1ac02b02a51bdf31eca19a6a136688b6841bab42a327b3", "summon_FETCH-LOGICAL-p3299-cdc64768092458e9be89127b101bccad82e2f5c17eb05c2204f872a174ce66643", "summon_FETCH-LOGICAL-p830-dd7c7eca937ef51e80e3ec05ade8fdf22214653237e42a3474b18623ed9dea963", "summon_FETCH-LOGICAL-c1715-b79ca0b2fc9bd4bff84e22c2eab1da98a019910e7f1b2096d16cac33daae3103", "summon_FETCH-LOGICAL-c1868-5054691c1d63f95d3f0307414e3efc4cc20eead344161b850e70249e259b62ad3", "summon_FETCH-LOGICAL-c1211-e08136003e7287a7a64b6eda0bfbc03dbf7257b2d8b53f22a080bb459cc6d8843", "summon_FETCH-LOGICAL-i2173-47513dcda7a4db6fbf381ff3108ddd2c295b9ecc59debe2663347d753b0d018d3", "summon_FETCH-LOGICAL-b2396-a12f27ddad4adaf9b5b886ce66725cf56cad93cf2b4f373b14c57f930ced68bf3", "summon_FETCH-LOGICAL-c942-72f5c8fd826442537b12c42263fda2cd0d9ff66d4e3f22f46d090bf271be8d6d3", "summon_FETCH-LOGICAL-c1796-10758b1f92fd91ee3e8e32a45318ee0f606b650ad0f1836ec4dfcf9f71cf3fb23", "summon_FETCH-LOGICAL-p1703-f7ea0c5035de9eff2792c629b08b28a75a874f4f9257604ee590f540d11b0e243", "summon_FETCH-LOGICAL-p1022-8dc8a25383cc296feb9aff16cdc7cded3d7d9fef89d8a7f079d54ad61fd699113"};
        log.info("testBulkIDOverflow: Got " + ids.length + " IDs");
        assertBulkDocIDRequest("Paging request", summon, ids.length, ids);
    }*/

    public void testBulkIDOverflowRandomize() throws Exception {
        SearchNode summon = SummonTestHelper.createPagingSummonSearchNode();
        List<String> ids = getAttributes(summon, new Request(
                DocumentKeys.SEARCH_QUERY, "children",
                DocumentKeys.SEARCH_MAX_RECORDS, 70
        ), "id", false);
        assertTrue("There should be at least 51 IDs (to trigger paging), but there were only " + ids.size(),
                   ids.size() > 50);

        Collections.shuffle(ids);
        ids = ids.subList(0, 51);
        String[] aIDs = new String[ids.size()];
        ids.toArray(aIDs);

        assertBulkDocIDRequest("Paging request", summon, aIDs.length, aIDs);
    }

    private void assertBulkDocIDRequest(String message, String... summonIDs) throws RemoteException {
        assertBulkDocIDRequest(message, summonIDs.length, summonIDs);
    }
    private void assertBulkDocIDRequest(String message, int expected, String... summonIDs) throws RemoteException {
        assertBulkDocIDRequest(message, SummonTestHelper.createSummonSearchNode(), expected, summonIDs);
    }
    private void assertBulkDocIDRequest(String message, SearchNode searchNode, int expected, String... summonIDs)
            throws RemoteException {
        List<String> ids = Arrays.asList(summonIDs);
        List<String> first = getHits(searchNode, DocumentKeys.SEARCH_IDS, Strings.join(ids));

        Collections.sort(ids);
        Collections.sort(first);

//        System.out.println("Requested: " + Strings.join(ids));
//        System.out.println("Received:  " + Strings.join(first));
        assertEquals(message + ". There should be the expected number of Records for first class ID lookup",
                     expected, first.size());
    }

    public void testBulkDocIDRequestMulti() throws RemoteException {
        final List<String> IDs = Arrays.asList(
                "summon_FETCH-gale_legaltrac_2308302761",
                "summon_FETCH-proquest_dll_11692083811",
                "summon_FETCH-LOGICAL-c811-60dce11a4b98aba9002078a4fc2cfd624c146f82ceca1772fafece2d86f46d861"
//                "summon_FETCH-gale_legaltrac_2308302761","summon_FETCH-proquest_dll_11692083811","summon_FETCH-LOGICAL-c811-60dce11a4b98aba9002078a4fc2cfd624c146f82ceca1772fafece2d86f46d861","summon_FETCH-proquest_dll_11755139711","summon_FETCH-LOGICAL-j890-a40832c4b422ebe6f04bf5436b16305c75aaed4d259dc0573b4b1ef427283b3a1","summon_FETCH-LOGICAL-g810-2f03abff7c1e24a521aa4f2b5c507fe4c0e220e8ca314633ea490c79b66fd6f41","summon_FETCH-proquest_dll_541205601","summon_FETCH-proquest_dll_120207321","summon_FETCH-proquest_dll_9354799911","summon_FETCH-LOGICAL-p521-5b809963a02a8becc6448a736c17e6bcac6ab91cc5c23e6a04a0d68dc23ac84a1","summon_FETCH-LOGICAL-p511-f5702dbcba1e8f738307bb666c84c4c394b0f98a993557a7cce45285ecf6a9cc1","summon_FETCH-proquest_dll_14630363911","summon_FETCH-proquest_dll_30250416711","summon_FETCH-proquest_dll_30226774711","summon_FETCH-proquest_dll_27506997311","summon_FETCH-proquest_dll_8589074411","sb_dbc_artikelbase_8%20491%20335%209","summon_FETCH-proquest_dll_20327444311","summon_FETCH-proquest_dll_19234374111","summon_FETCH-proquest_dll_14679293811","summon_FETCH-proquest_dll_14569276111","summon_FETCH-proquest_dll_19216182211","summon_FETCH-LOGICAL-c619-4a973220953fc3dd40c9dca66e240d4695414ed98336ad12ffabb894b0f9bd571","summon_FETCH-proquest_dll_18538701","summon_FETCH-proquest_dll_9228475611","summon_FETCH-proquest_dll_23577264921","summon_FETCH-proquest_dll_30627273111","summon_FETCH-LOGICAL-p521-566623873b8e44680dd14c2a0f3e399034fc11baf2820ade6047e7178795426f1","summon_FETCH-proquest_dll_21621838511","summon_FETCH-proquest_dll_19859333111","summon_FETCH-LOGICAL-p511-d0687736073e2baed0f757a160de5fd5ec28c5377f1018eb52300496b3415c161","summon_FETCH-LOGICAL-p965-9f0b78a9ef521411ea7397496b17332b6bea6d719d6ae23fb3f9091c1b92e95e1","summon_FETCH-proquest_dll_15899611611","summon_FETCH-gale_primary_2360916871","summon_FETCH-proquest_dll_29938759911","summon_FETCH-proquest_dll_28573609011","summon_FETCH-LOGICAL-g451-8719ffc7785bbdafe328e379dfde083cac7639be92a48ec2e8cb35a60b19f6a91","summon_FETCH-proquest_dll_26919958711","summon_FETCH-proquest_dll_21495980011","summon_FETCH-proquest_dll_29700292111","summon_FETCH-proquest_dll_20606276211","summon_FETCH-LOGICAL-g451-e6fb82a70976b871846177849cf47fbcc678268f10db93a9ae1f25ebb716f1471","summon_FETCH-proquest_dll_17397387011","summon_FETCH-proquest_dll_30072169811","summon_FETCH-gale_primary_3285280891","summon_FETCH-gale_primary_3376139331","summon_FETCH-LOGICAL-c615-eb69c463776454fa11031406b4161ddf55cddd73b9e8fc05deb4ae87cbc009671","summon_FETCH-LOGICAL-c602-939dd1e03c673c1880eb17c2e95632fdbd7fdfc90de8af588585ead941151a041","summon_FETCH-proquest_dll_16279127411","summon_FETCH-LOGICAL-g451-be4ed5a36b5662002dbf06ccf252076e08336ef20746f8f28977d3cdea0d8a0d1","summon_FETCH-proquest_dll_15474120411","summon_FETCH-proquest_dll_16505527411","summon_FETCH-proquest_dll_7702689111","summon_FETCH-proquest_dll_21517494011","summon_FETCH-LOGICAL-p857-6e88c4aca31c2630c440b24ea71b58499b25034e64b7fd93dede58f10126fbd91","summon_FETCH-proquest_dll_98015391","summon_FETCH-proquest_dll_7082516111","summon_FETCH-LOGICAL-c2141-e7dbed816be273ad925acde9c05327cb4d011e6f2429e5aebf94e0f056782da31","summon_FETCH-LOGICAL-p571-937d400983a1479af0babd89504c6bdf0221717404e06574363746dd4569b1151","summon_FETCH-jstor_primary_10_2307_208363691","summon_FETCH-proquest_dll_49732721","summon_FETCH-LOGICAL-j875-58271967e570f37ad32dbec3201e775f25682bc96491f49f1603824a182e9fb11","summon_FETCH-proquest_dll_11570722321","summon_FETCH-LOGICAL-p511-7b18890193bc636a917677961107228ab31568f9a07b916781562cd8b55dca041","summon_FETCH-jstor_primary_10_2307_257829691","summon_FETCH-jstor_primary_10_2307_257840011","summon_FETCH-LOGICAL-c573-dc2ce2c68d6a0e951f4373fbd158b8799d59550906d64131b3691dabbca11ed01","summon_FETCH-LOGICAL-p883-6d3108c4ad91c437a0f667b7e13552a53ce7c17d0e8561a1a4c9ae116cb95a071","summon_FETCH-crossref_primary_10_1300_J159v05n02_211","summon_FETCH-proquest_dll_21792554311","summon_FETCH-proquest_dll_28225259911","summon_FETCH-proquest_dll_18393813111","summon_FETCH-proquest_dll_8540668611","summon_FETCH-proquest_dll_19241936611","summon_FETCH-proquest_dll_15502451611","summon_FETCH-gale_primary_3330653461","summon_FETCH-doaj_primary_oai_doaj_articles_e8f7ac3b0f44c1f186121d206c0efbb21","summon_FETCH-proquest_dll_4403897211","summon_FETCH-proquest_dll_1089657931","summon_FETCH-LOGICAL-g731-aa1d4e9484387442ad972053410bd018ef30952ba39b36864f5685bbbbc78ca51","summon_FETCH-proquest_dll_16472696711","summon_FETCH-LOGICAL-c1051-679616bf2fd1fd6697db594baf9077791ac83509271d212c81ffc3c17d1b66981","summon_FETCH-proquest_dll_11297933311","summon_FETCH-proquest_dll_10404419031","summon_FETCH-proquest_dll_12861170041","summon_FETCH-LOGICAL-p441-6e92efff92fe0be0c93cfb5ec77e406a507a809e18b93408163ad2215cecccbb1","summon_FETCH-LOGICAL-p521-27c6856eb1b1a52fafe05b4a950e92ec7be86b4fb7b6a99c33b48c4fad0b5ede1","summon_FETCH-proquest_dll_20480924511","summon_FETCH-proquest_dll_27628003711","summon_FETCH-proquest_dll_9810371311","summon_FETCH-LOGICAL-g451-14764dc3cce3ef0280e82615b4879ca3b91801d95f627c1d172197692ef8d18c1","summon_FETCH-proquest_dll_18995750811","summon_FETCH-LOGICAL-p596-99896f814c2d29bc2f0a892bac6d7e460a7709eac7bf658da5ab08b7b032ce911","summon_FETCH-proquest_dll_13806557211","summon_FETCH-proquest_dll_13884859611","summon_FETCH-LOGICAL-g451-943b074c83c2ee85586a3f2f4971c76548330adf409162636b82ae5c5fd8c4b81","summon_FETCH-proquest_dll_13149719211","summon_FETCH-proquest_dll_626757911","summon_FETCH-proquest_dll_754250411","summon_FETCH-LOGICAL-c579-5b49813a53ed53f85fc21ab74ef7a2b44eed2cd1c5c9a6dc844ee9fa30487c211","sb_dbc_artikelbase_8%20382%20001%202","summon_FETCH-proquest_dll_858207191","summon_FETCH-proquest_dll_10404467441","summon_FETCH-proquest_dll_347170641","summon_FETCH-proquest_dll_11217587811","summon_FETCH-proquest_dll_11030372411","summon_FETCH-LOGICAL-g1458-246a0accef54ec5dfe8a03ddecb487a670f8fb920da816d1e5652dbcb7248ff61","summon_FETCH-LOGICAL-g741-61c2a3268a4952f3bebbf6026ba3076df1f48fb32ac11be0ad27e820505bb3891","summon_FETCH-LOGICAL-g815-d505db55cd46563c49ce177b91d05dccddae2082237c1ecf5705932bd051fb201","summon_FETCH-proquest_dll_23687907411","summon_FETCH-proquest_dll_23688449911","summon_FETCH-crossref_primary_10_5860_CHOICE_47_40971","summon_FETCH-LOGICAL-p521-b4eadda20a02467df88215e7f7a3b6f556c46b40c4d793d90590262114c8fd2b1","summon_FETCH-LOGICAL-g1102-1c70cfd82ff2a99e03154e5ca660d6e309c70666ef775a12c08f0bd1c931a07e1"
                //"summon_FETCH-gale_legaltrac_2308302761","summon_FETCH-proquest_dll_11692083811","summon_FETCH-LOGICAL-c811-60dce11a4b98aba9002078a4fc2cfd624c146f82ceca1772fafece2d86f46d861","summon_FETCH-proquest_dll_11755139711","summon_FETCH-LOGICAL-j890-a40832c4b422ebe6f04bf5436b16305c75aaed4d259dc0573b4b1ef427283b3a1","summon_FETCH-LOGICAL-g810-2f03abff7c1e24a521aa4f2b5c507fe4c0e220e8ca314633ea490c79b66fd6f41","summon_FETCH-proquest_dll_541205601","summon_FETCH-proquest_dll_120207321","summon_FETCH-proquest_dll_9354799911","summon_FETCH-LOGICAL-p521-5b809963a02a8becc6448a736c17e6bcac6ab91cc5c23e6a04a0d68dc23ac84a1","summon_FETCH-LOGICAL-p511-f5702dbcba1e8f738307bb666c84c4c394b0f98a993557a7cce45285ecf6a9cc1","summon_FETCH-proquest_dll_14630363911","summon_FETCH-proquest_dll_30250416711","summon_FETCH-proquest_dll_30226774711","summon_FETCH-proquest_dll_27506997311","summon_FETCH-proquest_dll_8589074411","sb_dbc_artikelbase_8%20491%20335%209","summon_FETCH-proquest_dll_20327444311","summon_FETCH-proquest_dll_19234374111","summon_FETCH-proquest_dll_14679293811","summon_FETCH-proquest_dll_14569276111","summon_FETCH-proquest_dll_19216182211","summon_FETCH-LOGICAL-c619-4a973220953fc3dd40c9dca66e240d4695414ed98336ad12ffabb894b0f9bd571","summon_FETCH-proquest_dll_18538701","summon_FETCH-proquest_dll_9228475611","summon_FETCH-proquest_dll_23577264921","summon_FETCH-proquest_dll_30627273111","summon_FETCH-LOGICAL-p521-566623873b8e44680dd14c2a0f3e399034fc11baf2820ade6047e7178795426f1","summon_FETCH-proquest_dll_21621838511","summon_FETCH-proquest_dll_19859333111","summon_FETCH-LOGICAL-p511-d0687736073e2baed0f757a160de5fd5ec28c5377f1018eb52300496b3415c161","summon_FETCH-LOGICAL-p965-9f0b78a9ef521411ea7397496b17332b6bea6d719d6ae23fb3f9091c1b92e95e1","summon_FETCH-proquest_dll_15899611611","summon_FETCH-gale_primary_2360916871","summon_FETCH-proquest_dll_29938759911","summon_FETCH-proquest_dll_28573609011","summon_FETCH-LOGICAL-g451-8719ffc7785bbdafe328e379dfde083cac7639be92a48ec2e8cb35a60b19f6a91","summon_FETCH-proquest_dll_26919958711","summon_FETCH-proquest_dll_21495980011","summon_FETCH-proquest_dll_29700292111","summon_FETCH-proquest_dll_20606276211","summon_FETCH-LOGICAL-g451-e6fb82a70976b871846177849cf47fbcc678268f10db93a9ae1f25ebb716f1471","summon_FETCH-proquest_dll_17397387011","summon_FETCH-proquest_dll_30072169811","summon_FETCH-gale_primary_3285280891","summon_FETCH-gale_primary_3376139331","summon_FETCH-LOGICAL-c615-eb69c463776454fa11031406b4161ddf55cddd73b9e8fc05deb4ae87cbc009671","summon_FETCH-LOGICAL-c602-939dd1e03c673c1880eb17c2e95632fdbd7fdfc90de8af588585ead941151a041","summon_FETCH-proquest_dll_16279127411","summon_FETCH-LOGICAL-g451-be4ed5a36b5662002dbf06ccf252076e08336ef20746f8f28977d3cdea0d8a0d1","summon_FETCH-proquest_dll_15474120411","summon_FETCH-proquest_dll_16505527411","summon_FETCH-proquest_dll_7702689111","summon_FETCH-proquest_dll_21517494011","summon_FETCH-LOGICAL-p857-6e88c4aca31c2630c440b24ea71b58499b25034e64b7fd93dede58f10126fbd91","summon_FETCH-proquest_dll_98015391","summon_FETCH-proquest_dll_7082516111","summon_FETCH-LOGICAL-c2141-e7dbed816be273ad925acde9c05327cb4d011e6f2429e5aebf94e0f056782da31","summon_FETCH-LOGICAL-p571-937d400983a1479af0babd89504c6bdf0221717404e06574363746dd4569b1151","summon_FETCH-jstor_primary_10_2307_208363691","summon_FETCH-proquest_dll_49732721","summon_FETCH-LOGICAL-j875-58271967e570f37ad32dbec3201e775f25682bc96491f49f1603824a182e9fb11","summon_FETCH-proquest_dll_11570722321","summon_FETCH-LOGICAL-p511-7b18890193bc636a917677961107228ab31568f9a07b916781562cd8b55dca041","summon_FETCH-jstor_primary_10_2307_257829691","summon_FETCH-jstor_primary_10_2307_257840011","summon_FETCH-LOGICAL-c573-dc2ce2c68d6a0e951f4373fbd158b8799d59550906d64131b3691dabbca11ed01","summon_FETCH-LOGICAL-p883-6d3108c4ad91c437a0f667b7e13552a53ce7c17d0e8561a1a4c9ae116cb95a071","summon_FETCH-crossref_primary_10_1300_J159v05n02_211","summon_FETCH-proquest_dll_21792554311","summon_FETCH-proquest_dll_28225259911","summon_FETCH-proquest_dll_18393813111","summon_FETCH-proquest_dll_8540668611","summon_FETCH-proquest_dll_19241936611","summon_FETCH-proquest_dll_15502451611","summon_FETCH-gale_primary_3330653461","summon_FETCH-doaj_primary_oai_doaj_articles_e8f7ac3b0f44c1f186121d206c0efbb21","summon_FETCH-proquest_dll_4403897211","summon_FETCH-proquest_dll_1089657931","summon_FETCH-LOGICAL-g731-aa1d4e9484387442ad972053410bd018ef30952ba39b36864f5685bbbbc78ca51","summon_FETCH-proquest_dll_16472696711","summon_FETCH-LOGICAL-c1051-679616bf2fd1fd6697db594baf9077791ac83509271d212c81ffc3c17d1b66981","summon_FETCH-proquest_dll_11297933311","summon_FETCH-proquest_dll_10404419031","summon_FETCH-proquest_dll_12861170041","summon_FETCH-LOGICAL-p441-6e92efff92fe0be0c93cfb5ec77e406a507a809e18b93408163ad2215cecccbb1","summon_FETCH-LOGICAL-p521-27c6856eb1b1a52fafe05b4a950e92ec7be86b4fb7b6a99c33b48c4fad0b5ede1","summon_FETCH-proquest_dll_20480924511","summon_FETCH-proquest_dll_27628003711","summon_FETCH-proquest_dll_9810371311","summon_FETCH-LOGICAL-g451-14764dc3cce3ef0280e82615b4879ca3b91801d95f627c1d172197692ef8d18c1","summon_FETCH-proquest_dll_18995750811","summon_FETCH-LOGICAL-p596-99896f814c2d29bc2f0a892bac6d7e460a7709eac7bf658da5ab08b7b032ce911","summon_FETCH-proquest_dll_13806557211","summon_FETCH-proquest_dll_13884859611","summon_FETCH-LOGICAL-g451-943b074c83c2ee85586a3f2f4971c76548330adf409162636b82ae5c5fd8c4b81","summon_FETCH-proquest_dll_13149719211","summon_FETCH-proquest_dll_626757911","summon_FETCH-proquest_dll_754250411","summon_FETCH-LOGICAL-c579-5b49813a53ed53f85fc21ab74ef7a2b44eed2cd1c5c9a6dc844ee9fa30487c211","sb_dbc_artikelbase_8%20382%20001%202","summon_FETCH-proquest_dll_858207191","summon_FETCH-proquest_dll_10404467441","summon_FETCH-proquest_dll_347170641","summon_FETCH-proquest_dll_11217587811","summon_FETCH-proquest_dll_11030372411","summon_FETCH-LOGICAL-g1458-246a0accef54ec5dfe8a03ddecb487a670f8fb920da816d1e5652dbcb7248ff61","summon_FETCH-LOGICAL-g741-61c2a3268a4952f3bebbf6026ba3076df1f48fb32ac11be0ad27e820505bb3891","summon_FETCH-LOGICAL-g815-d505db55cd46563c49ce177b91d05dccddae2082237c1ecf5705932bd051fb201","summon_FETCH-proquest_dll_23687907411","summon_FETCH-proquest_dll_23688449911","summon_FETCH-crossref_primary_10_5860_CHOICE_47_40971","summon_FETCH-LOGICAL-p521-b4eadda20a02467df88215e7f7a3b6f556c46b40c4d793d90590262114c8fd2b1","summon_FETCH-LOGICAL-g1102-1c70cfd82ff2a99e03154e5ca660d6e309c70666ef775a12c08f0bd1c931a07e1"
        ).subList(0, 3);
        final int EXPECTED = IDs.size();

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        List<String> first = getHits(summon, DocumentKeys.SEARCH_IDS, Strings.join(IDs));

        Collections.sort(IDs);
        Collections.sort(first);
        System.out.println("Requested: " + Strings.join(IDs));
        System.out.println("Received:  " + Strings.join(first));
        assertEquals("There should be the expected number of Records for first class ID lookup",
                     EXPECTED, first.size());
    }

    public void testBulkDocIDRequestMultiFallback() throws RemoteException {
        final List<String> IDs = Arrays.asList(
                "summon_FETCH-gale_legaltrac_2308302761",
                "summon_FETCH-proquest_dll_11692083811",
                "summon_FETCH-LOGICAL-c811-60dce11a4b98aba9002078a4fc2cfd624c146f82ceca1772fafece2d86f46d861",
                "summon_proquest_dll_74235477" // Works without fetch, not with. Returns FETCH-proquest_dll_742354771
        );
        final int EXPECTED = IDs.size();

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        List<String> first = getHits(summon, DocumentKeys.SEARCH_IDS, Strings.join(IDs));

        Collections.sort(IDs);
        Collections.sort(first);
        System.out.println("Requested: " + Strings.join(IDs));
        System.out.println("Received:  " + Strings.join(first));
        assertEquals("There should be the expected number of Records for first class ID lookup",
                     EXPECTED, first.size());
    }

    public void testFallbackDocIDRequest() throws RemoteException {
        final List<String> IDs = Arrays.asList(
                "summon_FETCH-eric_primary_EJ5633011",    // Returned as summon_FETCH-LOGICAL-c611-6aa4a4e310c8434ecaf0289c01a569c2b03cf40b9ea8913fd9bc487fb3db1caa1
                "FETCH-proquest_dll_15622214411",         // Works with FETCH, not without
                "summon_chadwyckhealey_pio_608103960009", // Nothing returned, even with FETCH-
                "summon_chadwyckhealey_pio_511600010016", // Works with FETCH, not without
                "summon_proquest_dll_74235477"            // Works without fetch, not with. Returns FETCH-proquest_dll_742354771
        );
        final int EXPECTED = 3;

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        List<String> first = getHits(summon, DocumentKeys.SEARCH_IDS, Strings.join(IDs));

        Collections.sort(IDs);
        Collections.sort(first);
        System.out.println("Requested: " + Strings.join(IDs));
        System.out.println("Received:  " + Strings.join(first));
        assertEquals("There should be the expected number of Records for first class ID lookup",
                     EXPECTED, first.size());
    }

    public void testOriginatingID() throws RemoteException {
        final List<String> IDs = Arrays.asList(
                "summon_chadwyckhealey_pio_511600010016", // Works with FETCH, not without
                "summon_proquest_dll_74235477"            // Works without fetch, not with. Returns FETCH-proquest_dll_742354771
        );
        final List<String> EXPECTED_IDs = Arrays.asList(
                "summon_DETCH-chadwyckhealey_pio_511600010016",
                "summon_FETCH-proquest_dll_742354771"
        );


        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        summon.search(new Request(DocumentKeys.SEARCH_IDS, Strings.join(IDs)), responses);
        List<Pair<String, String>> idPairs = getIDPairs(responses, SummonSearchNode.ORIGINAL_LOOKUP_ID);

        Collections.sort(IDs);
        Collections.sort(idPairs);
        assertEquals("The right number of ID pairs should be returned", EXPECTED_IDs.size(), idPairs.size());

        for (int i = 0 ; i < IDs.size() ; i++) {
            String requested = IDs.get(i);
            String expected = EXPECTED_IDs.get(i);
            String returned = idPairs.get(i).getKey();
            String originating = idPairs.get(i).getValue();
            String concat = String.format(
                    "requested='%s', expected='%s', returned='%s', originating='%s'",
                    requested, expected, returned, originating);
            assertEquals("The originating ID should be as requested. " + concat, requested, originating);
            assertEquals("The returned ID should be as expected. " + concat, requested, originating);
            log.info("IDs: " + concat);
        }
    }

    private List<Pair<String, String>> getIDPairs(ResponseCollection responses, String fieldName) {
        List<Pair<String, String>> idPairs = new ArrayList<>();
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                DocumentResponse docs = (DocumentResponse)response;
                for (DocumentResponse.Record record: docs.getRecords()) {
                    idPairs.add(new Pair<>(record.getId(), record.getFieldValue(fieldName, null)));
                }
            }
        }
        return idPairs;
    }

    public void testHitCountVsDocs() throws RemoteException {
        final String ID = "summon_FETCH-eric_primary_EJ5633011";

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        summon.search(new Request(DocumentKeys.SEARCH_QUERY, "recordID:" + ID), responses);

        List<String> records = getHits(responses);

        long hits = 0;
        for (Response r: responses) {
            if (r instanceof DocumentResponse) {
                hits = ((DocumentResponse)r).getHitCount();
            }
        }
        assertEquals("The reported hitCount should match the number of returned records", hits, records.size());
    }

    public void testIDConsistency() throws RemoteException {
        final String[] QUERIES = {"foo", "horses", "radish", "gnu software", "consistency"};
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        for (String query: QUERIES) {
            List<String> first =
                    getHits(summon, DocumentKeys.SEARCH_QUERY, query, DocumentKeys.SEARCH_MAX_RECORDS, "50");
            List<String> checked = getHits(summon, DocumentKeys.SEARCH_IDS, Strings.join(first));
            Collections.sort(first);
            Collections.sort(checked);
            assertEquals("The IDs returned from the search for " + query + " should match those returned by ID-lookup",
                         Strings.join(first, "\n"), Strings.join(checked, "\n"));
        }
    }

    public void disabledtestTemporaryLookupTest() throws RemoteException {
        final List<String> IDs = Arrays.asList(
                "eric_primary_EJ5633011"
                //"proquest_dll_74235477"
            //    "FETCH-chadwyckhealey_pio_511600010016",
              //  "summon_proquest_dll_74235477"
        );

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();

        List<String> first = getHits(summon, DocumentKeys.SEARCH_QUERY, "recordID:" + IDs.get(0));
        if (first.isEmpty()) {
            System.err.println("No result for IDs ['" + Strings.join(IDs) + "]");
        } else {
            System.out.println("Got " + Strings.join(first));
        }
    }

    public void disabledtestFatalDocIDRequest() throws RemoteException {
        final List<String> IDs = Arrays.asList(
                "summon_FETCH-eric_primary_EJ5633011", // Exists 20140224
                "FETCH-proquest_dll_15622214411",
                "summon_chadwyckhealey_pio_608103960009", // No direct ID lookup
                "FETCH-chadwyckhealey_pio_511600010016",  // No direct ID lookup
                "summon_proquest_dll_74235477",           // No direct ID lookup
                "InvalidID"
        );

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();

        List<String> first = getHits(summon, DocumentKeys.SEARCH_IDS, Strings.join(IDs));
        assertFalse("There should be at least 1 hit for first class ID lookup", first.isEmpty());
        List<String> second = new ArrayList<>(IDs.size());
        { // Old-school singular ID-searches
            for (String id: IDs) {
                id = id.replace("summon_", "");
                if (getHitCount(summon, DocumentKeys.SEARCH_QUERY, "recordID:\"" + id + "\"") > 0) {
                    second.add(id);
                }
            }
        }
        List<String> remove = new ArrayList<>(IDs.size());
        { // Old-school singular ID-searches
            for (String id: IDs) {
                id = id.replace("summon_", "").replace("FETCH-", "");
                if (getHitCount(summon, DocumentKeys.SEARCH_QUERY, "recordID:\"" + id + "\"") > 0) {
                    remove.add(id);
                }
            }
        }
        List<String> add = new ArrayList<>(IDs.size());
        { // Old-school singular ID-searches
            for (String id: IDs) {
                id = id.replace("summon_", "");
                if (!id.startsWith("FETCH-")) {
                    id = "FETCH-" + id;
                }
                if (getHitCount(summon, DocumentKeys.SEARCH_QUERY, "recordID:\"" + id + "\"") > 0) {
                    add.add(id);
                }
            }
        }
        Collections.sort(first);
        Collections.sort(second);
        Collections.sort(remove);
        Collections.sort(add);
        System.out.println(
                "First class:  " + Strings.join(first) + "\n"
                + "Individual:   " + Strings.join(second) + "\n"
                + "Remove FETCH: " + Strings.join(remove) + "\n"
                + "Add FETCH:    " + Strings.join(add) + "\n"
        );
        assertEquals("First class ID-lookup should match singular lookups", second, first);

        summon.close();
    }
    public void testDocIDRequest() throws RemoteException {
        List<String> IDs = Arrays.asList(
                "summon_FETCH-proquest_dll_11531932811",
                "FETCH-proquest_dll_6357072911",
                "summon_FETCH-proquest_dll_15622214411",
                "InvalidID"
        );
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        {
            long standard = getHitCount(summon, DocumentKeys.SEARCH_QUERY, "recordID:" + IDs.get(0));
            assertEquals("A search for '" + IDs.get(0) + "' should give 1 hit", 1, standard);
        }
        {
            long idSpecific = getHitCount(summon, DocumentKeys.SEARCH_IDS, Strings.join(IDs));
            assertEquals("A search with search.document.ids should give all hits", 3, idSpecific);
        }
        {
            long idInvalid = getHitCount(summon, DocumentKeys.SEARCH_IDS, "nonexisting",
                                         "summonresponsebuilder.dumpraw", "true");
            assertEquals("A search with no valid IDs in search.document.ids should give zero hits", 0, idInvalid);
        }
        summon.close();
    }

    public void testPageFault() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        try {
            summon.search(new Request(
                    DocumentKeys.SEARCH_QUERY, "book", DocumentKeys.SEARCH_START_INDEX, 10000), responses);
        } catch (RemoteException e) {
            log.debug("Received RemoteException as expected");
        }
        fail("Search with large page number was expected to fail. Received response:\n" + responses.toXML());
    }

    public void testRecordBaseQueryRewrite() {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        assertNull("Queries for recordBase:summon should be reduced to null (~match all)",
                   summon.convertQuery("recordBase:summon", null));
        assertEquals("recordBase:summon should be removed with implicit AND",
                     "\"foo\"", summon.convertQuery("recordBase:summon foo", null));
        assertEquals("recordBase:summon should be removed with explicit AND",
                     "\"foo\"", summon.convertQuery("recordBase:summon AND foo", null));
        assertEquals("OR with recordBase:summon and another recordBase should match",
                     null, summon.convertQuery("recordBase:summon OR recordBase:blah", null));
        assertEquals("OR with recordBase:summon should leave the rest of the query",
                     "\"foo\"", summon.convertQuery("recordBase:summon OR foo", null));
        assertEquals("recordBase:summon AND recordBase:nonexisting should not match anything",
                     SummonSearchNode.DEFAULT_NONMATCHING_QUERY.replace(":", ":\"")
                     + "\"", summon.convertQuery("recordBase:summon AND recordBase:nonexisting", null));
    }

    /*
    Trying to discover why some phrase searches return more results than non-phrase searches for summon.
    No luck so fas as it seems that neither the ?-wildcard, nor escaping of space works.
     */
/*    public void testQuoting() throws IOException, TransformerException {
        String[] TESTS = new String[] {
                "dogs myasthenia gravis",
                "dogs myasthenia\\ gravis*",
                "dogs myasthenia*gravis",
                "dogs myasthenia?gravis",
                "dogs \"myasthenia gravis\"",
                "dogs myasthenia\\ gravis"
        };

        log.debug("Creating SummonSearchNode");
        String s = "";
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        for (String query: TESTS) {
            long hits = getHits(summon,
                    DocumentKeys.SEARCH_QUERY, query,
                    SolrSearchNode.SEARCH_PASSTHROUGH_QUERY, "true",
                    DocumentKeys.SEARCH_COLLECT_DOCIDS, "false");
            s += "'" + query + "' gave " + hits + " hits\n";
        }
        summon.close();

        System.out.print(s);
    }
  */


    public void testEmptyFilter() throws IOException, TransformerException {
        String QUERY = "gene and protein evolution";
        String FILTER = "";

        log.debug("Creating SummonSearchNode");
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                DocumentKeys.SEARCH_FILTER, FILTER,
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        List<String> ids = getAttributes(summon, req, "id", false);
        assertEquals("There should be no results", 0, ids.size());
        summon.close();
    }

    public void testNotRecordBase() throws IOException, TransformerException {
        String QUERY = "gene and protein evolution";
        String FILTER = "NOT recordBase:something";

        log.debug("Creating SummonSearchNode");
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                DocumentKeys.SEARCH_FILTER, FILTER,
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        List<String> ids = getAttributes(summon, req, "id", false);
        assertTrue("There should be some results", !ids.isEmpty());
        summon.close();
    }

    /*
    dl reported that some fields were skipped. Investigations showed that the processing of *_xml-fields skipped
    the following field.
     */
    public void testFieldExtraction() throws IOException, TransformerException {
        // Random record that contains PublicationTitle immediately after an *_xml-field
        String QUERY =
                "recordID:summon_FETCH-LOGICAL-g1031-b913763956b78eef2fe5ce46ded8b8ea40871499f685c91b65d45db8b0900b8a1";

        log.debug("Creating SummonSearchNode");
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                SummonResponseBuilder.SEARCH_DUMP_RAW, true,
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        ResponseCollection result = search(req);
        DocumentResponse docs = (DocumentResponse)result.iterator().next();
        assertEquals("There should be a single result", 1, docs.size());
        boolean pubFound = false;
        for (DocumentResponse.Field field: docs.getRecords().get(0)) {
            if ("PublicationTitle".equals(field.getName())) {
                pubFound = true;
                break;
            }
        }
        assertTrue("The document should contain a field with the name 'DocumentTitle'\n" + result.toXML(), pubFound);

        summon.close();
    }

    public void testIDResponse() throws IOException, TransformerException {
        String QUERY = "gene and protein evolution";

        log.debug("Creating SummonSearchNode");
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        List<String> ids = getAttributes(summon, req, "id", false);
        assertTrue("There should be at least 1 result", ids.size() >= 1);

        final Pattern EMBEDDED_ID_PATTERN = Pattern.compile("<field name=\"recordID\">(.+?)</field>", Pattern.DOTALL);
        List<String> embeddedIDs = getPattern(summon, req, EMBEDDED_ID_PATTERN, false);
        ExtraAsserts.assertEquals("The embedded IDs should match the plain IDs", ids, embeddedIDs);
        System.out.println("Received IDs: " + Strings.join(ids, ", "));

        summon.close();
    }

    public void testNegativeFacet() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"darkmans barker\"," +
                "\"search.document.collectdocids\":\"true\","
                + "\"solr.filterisfacet\":\"true\"," +
                "\"solrparam.s.ho\":\"true\","
                + "\"search.document.filter\":\" NOT ContentType:\\\"Newspaper Article\\\"\","
                + "\"search.document.filter.purenegative\":\"true\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        assertFalse("The response should not have Newspaper Article as facet. total response was\n" + responses.toXML(),
                    responses.toXML().contains("<tag name=\"Newspaper Article\""));
        summon.close();
    }

    public void testExplicitFacet() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"peter\","
                + "\"search.document.collectdocids\":\"true\","
                + "\"search.facet.facets\":\"SubjectTerms\"}";
                //+ "\"search.facet.facets\":\"SubjectTerms,lsubject\"}";

        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        assertTrue("The response should contain some facets\n" + responses.toXML(),
                   responses.toXML().contains("facet name"));
        summon.close();
    }

    public void testPagedFacet() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"peter\"," +
                "\"search.document.startindex\":0," +
                "\"search.document.maxrecords\":60," +
                "\"search.document.collectdocids\":true," +
                "\"solr.filterisfacet\":\"true\"," +
                "\"search.document.filter\":\"SubjectTerms:\\\"athletes\\\"\"}";

        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        PagingSearchNode pager = new PagingSearchNode(Configuration.newMemoryBased(
                PagingSearchNode.CONF_SEQUENTIAL, true,
                PagingSearchNode.CONF_GUIPAGESIZE, 20,
                PagingSearchNode.CONF_MAXPAGESIZE, 50
        ), summon);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        pager.search(request, responses);
        assertTrue("The response should contain some facets\n" + responses.toXML(),
                   responses.toXML().contains("facet name"));
        summon.close();
    }

    public void testSpacedEscapedFacetFilter() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"nature\", " +
                "\"search.document.filter\":" +
                "\"ContentType:Magazine\\\\ Article OR ContentType:Journal\\\\ Article\", "  +
                "\"solr.filterisfacet\":\"true\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        log.info("Searching with query='" + request.getString(DocumentKeys.SEARCH_QUERY)
                 + "', filter='" + request.getString(DocumentKeys.SEARCH_FILTER) + "'");
        summon.search(request, responses);
        final long numHits = ((DocumentResponse)responses.iterator().next()).getHitCount();
        assertTrue("The number of hits should exceed 1 but was " + numHits, numHits > 1);
        summon.close();
    }

    public void testSpacedEscapedFacetQuery() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":" +
                "\"nature AND (ContentType:Magazine\\\\ Article OR ContentType:Journal\\\\ Article)\", " +
                "\"solr.filterisfacet\":\"true\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        log.info("Searching with query='" + request.getString(DocumentKeys.SEARCH_QUERY) + "'");
        summon.search(request, responses);
        final long numHits = ((DocumentResponse)responses.iterator().next()).getHitCount();
        assertTrue("The number of hits should exceed 1 but was " + numHits, numHits > 1);
        summon.close();
    }

    public void testSpacedQuotedFacetFilter() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"nature\", " +
                "\"search.document.filter\":" +
                "\"ContentType:\\\"Magazine Article\\\" OR ContentType:\\\"Journal Article\\\"\", " +
                "\"solr.filterisfacet\":\"true\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        log.info("Searching with query='" + request.getString(DocumentKeys.SEARCH_QUERY)
                 + "', filter='" + request.getString(DocumentKeys.SEARCH_FILTER) + "'");
        summon.search(request, responses);
        final long numHits = ((DocumentResponse)responses.iterator().next()).getHitCount();
        assertTrue("The number of hits should exceed 1 but was " + numHits, numHits > 1);
        summon.close();
    }

    public void testFacetSizeSmall() throws RemoteException {
        assertFacetSize(3);
        assertFacetSize(25);
    }

    private void assertFacetSize(int tagCount) throws RemoteException {
        final String JSON = "{\"search.document.query\":\"thinking\",\"search.document.collectdocids\":\"true\"}";
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SummonSearchNode.CONF_SOLR_FACETS, "ContentType (" + tagCount + " ALPHA)");
        SearchNode summon = new SummonSearchNode(conf);

        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        List<String> tags = StringExtraction.getStrings(responses.toXML(), "<tag.+?>");
        assertEquals("The number of returned tags should be " + tagCount + "+1. The returned Tags were\n"
                     + Strings.join(tags, "\n"), tagCount + 1, tags.size());
        summon.close();
    }

    public void testFacetSizeQuery() throws RemoteException {
        int tagCount = 3;
        final String JSON = "{\"search.document.query\":\"thinking\","
                            + "\"search.document.collectdocids\":\"true\","
                            + "\"search.facet.facets\":\"ContentType (" + tagCount + " ALPHA)\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();

        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        List<String> tags = StringExtraction.getStrings(responses.toXML(), "<tag.+?>");
        assertEquals("The number of returned tags should be " + tagCount + "+1. The returned Tags were\n"
                     + Strings.join(tags, "\n"), tagCount + 1, tags.size());
        summon.close();
    }

    public void testRecordBaseFacet() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"darkmans barker\",\"search.document.collectdocids\":\"true\","
                + "\"solr.filterisfacet\":\"true\",\"solrparam.s.ho\":\"true\","
                + "\"search.document.filter\":\" recordBase:summon\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        assertTrue("The response should contain a summon tag from faceting\n"
                   + responses.toXML(), responses.toXML().contains("<tag name=\"summon\" addedobjects=\""));
        summon.close();
    }

    public void testRecordBaseFacetWithOR() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"darkmans barker\",\"search.document.collectdocids\":\"true\","
                + "\"solr.filterisfacet\":\"true\",\"solrparam.s.ho\":\"true\","
                + "\"search.document.filter\":\" recordBase:summon OR recordBase:sb_aleph\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        assertTrue("The response should contain a summon tag from faceting\n"
                   + responses.toXML(), responses.toXML().contains("<tag name=\"summon\" addedobjects=\""));
        summon.close();
    }

    public void testBasicSearch() throws RemoteException {
        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
//        summon.open(""); // Fake open for setting permits
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        //System.out.println(responses.toXML());
        assertTrue("The result should contain at least one record", responses.toXML().contains("<record score"));
        assertTrue("The result should contain at least one tag", responses.toXML().contains("<tag name"));
        log.info("Raw Summon response:\n"
                 + responses.getTransient().get(SummonResponseBuilder.SUMMON_RESPONSE).toString().replace("<", "\n<"));
        log.info("Parsed Summa response:\n" + responses.toXML());
    }

    public void testXMLTree() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SummonResponseBuilder.CONF_XML_FIELD_HANDLING, SummonResponseBuilder.XML_MODE.mixed);
        SummonSearchNode summon = new SummonSearchNode(conf);

        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);

        final Pattern XML_FIELDS = Pattern.compile("(<field name=\"[^\"]*_xml\">.+?</field>)", Pattern.DOTALL);
        List<String> xmlFields = getPattern(summon, request, XML_FIELDS, false);
        log.info("Got XML-fields\n" + Strings.join(xmlFields, "\n"));
    }

    public void testMultiID() throws RemoteException {
        List<String> IDs = Arrays.asList(
                "FETCH-proquest_dll_11531932811",
                "FETCH-proquest_dll_6357072911",
                "FETCH-proquest_dll_15622214411"
        );
        SummonSearchNode searcher = SummonTestHelper.createSummonSearchNode(true);

        for (String id: IDs) {
            assertEquals("The number of hits for ID '" + id + "' should match", 1, getAttributes(searcher, new Request(
                    DocumentKeys.SEARCH_QUERY, "ID:\"" + id + "\"",
                    SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true
            ), "id", false).size());
        }

        String IDS_QUERY = "(ID:\"" + Strings.join(IDs, "\" OR ID:\"") + "\")";

        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, IDS_QUERY,
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true
        );
        List<String> returnedIDs = getAttributes(searcher, req, "id", false);
        if (IDs.size() != returnedIDs.size()) {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(req, responses);
            System.out.println("Returned IDs: " + Strings.join(returnedIDs, ", "));
//            System.out.println(responses.toXML());
        }

        assertEquals(
                "Pre 2013-12-13 Serial Solutions only returned 1 hit for multiple ID searches. That has now changed " +
                "for query '" + IDS_QUERY + "'", 1, returnedIDs.size());
    }

    public void testShortFormat() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SummonResponseBuilder.CONF_SHORT_DATE, true);

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
//        summon.open(""); // Fake open for setting permits
        final Pattern DATEPATTERN = Pattern.compile("<dc:date>(.+?)</dc:date>", Pattern.DOTALL);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_RESULT_FIELDS, "shortformat");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        List<String> dates= getPattern(summon, request, DATEPATTERN, false);
        assertTrue("There should be at least 1 extracted date", !dates.isEmpty());
        for (String date: dates) {
            assertTrue("the returned dates should be of length 4 or less, got '" + date + "'", date.length() <= 4);
        }
//        System.out.println("Got dates:\n" + Strings.join(dates, ", "));
    }

    public void testIDSearch() throws IOException, TransformerException {
        List<String> sampleIDs = getSampleIDs();
        assertFalse("There should be at least 1 sample ID", sampleIDs.isEmpty());
        String ID = sampleIDs.get(0);
//        System.out.println("Got IDs " + Strings.join(sampleIDs, ", "));
//        String ID = "summon_FETCH-gale_primary_2105957371";

        /*
        From Summon-provided test site
        From Summon-provided test site
        queryString="
        s.rec.qs.max=
        &amp;s.mr=
        &amp;s.ho=t
        &amp;s.rec.db.max=
        &amp;s.ps=10
        &amp;s.q=ID%3A%22FETCH-LOGICAL-a8990-b5a26f60b0093e6474a5a91213bf9fccef1af6e41cbc4c3456d008ae2e43f7e61%22
        &amp;s.pn=1
         */

        String query = "recordID:\"" + ID + "\"";
        //String query = "ID:\"" + ID + "\"";
        log.info("Creating SummonSearchNode and performing search for " + query);
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, query,
                DocumentKeys.SEARCH_MAX_RECORDS, 10,
//                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "ps", "10",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        List<String> ids = getAttributes(summon, req, "id", false);
        assertTrue("There should be at least 1 result", ids.size() >= 1);
    }

    private List<String> getSampleIDs() throws IOException, TransformerException {
        String QUERY = "gene and protein evolution";

        log.debug("Creating SummonSearchNode");
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        try {
            Request req = new Request(
                    DocumentKeys.SEARCH_QUERY, QUERY,
                    DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
            List<String> ids = getAttributes(summon, req, "id", false);
            assertTrue("getSampleIDs(): There should be at least 1 result", ids.size() >= 1);

            final Pattern EMBEDDED_ID_PATTERN =
                    Pattern.compile("<field name=\"recordID\">(.+?)</field>", Pattern.DOTALL);
            List<String> embeddedIDs = getPattern(summon, req, EMBEDDED_ID_PATTERN, false);
            ExtraAsserts.assertEquals("getSampleIDs(): The embedded IDs should match the plain IDs", ids, embeddedIDs);
            return embeddedIDs;
        } finally {
            summon.close();
        }
    }

    public void testTruncation() throws IOException, TransformerException {
        String PLAIN = "Author:andersen";
        String ESCAPED = "Author:andersen\\ christian";
        String TRUNCATED = "Author:andersen*";
        String ESCAPED_TRUNCATED = "Author:andersen\\ c*";
        String ESCAPED_TRUNCATED2 = "lfo:andersen\\ h\\ c*";

        List<String> QUERIES = Arrays.asList(PLAIN, ESCAPED, TRUNCATED, ESCAPED_TRUNCATED, ESCAPED_TRUNCATED2);

        log.debug("Creating SummonSearchNode");
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SolrSearchNode.CONF_SOLR_READ_TIMEOUT, 20*1000);
        SearchNode summon = new SummonSearchNode(conf);
        for (String query: QUERIES) {
            Request req = new Request(
                    DocumentKeys.SEARCH_QUERY, query,
                    DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
            long searchTime = -System.currentTimeMillis();
            List<String> ids = getAttributes(summon, req, "id", false);
            searchTime += System.currentTimeMillis();
            assertFalse("There should be at least 1 result for " + query, ids.isEmpty());
            log.info("Got " + ids.size() + " from query '" + query + "' in " + searchTime + " ms");
        }
    }

    /*
      * At the end of june and the start of july 2013, 5-15% of the summon-requests in production exceeded the
      * connection timeout of 2000 ms. This unit test was made to verify this and is left (although disabled)
      * if the problem should arise again.
     */
    public void disabledtestTimeout() throws Exception {
        final int CONNECT_TIMEOUT = 20000;
        final int READ_TIMEOUT = 20000;
        final int RUNS = 20;
        final int DELAY_MS = 5000;
        final int VARIANCE_MS = 2234;

/*        List<String> QUERIES = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(
                "/home/te/tmp/sumfresh/common/performancetest/testQueries.txt")), "utf-8"));
        String line;
        while ((line = in.readLine()) != null) {
            if (!line.isEmpty()) {
                QUERIES.add(line);
            }
        }
        log.info("Loaded " + QUERIES.size() + " queries");
  */
        List<String> QUERIES = Arrays.asList("foo", "heat", "heat beads", "heat pans", "dolphin calls",
                                             "fresh water supply", "fresh water", "fresh water irrigation");

        log.debug("Creating SummonSearchNode");
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SolrSearchNode.CONF_SOLR_CONNECTION_TIMEOUT, CONNECT_TIMEOUT);
        conf.set(SolrSearchNode.CONF_SOLR_READ_TIMEOUT, READ_TIMEOUT);
        SummonSearchNode summon = new SummonSearchNode(conf);

        long maxConnectTime = -1;
        int success = 0;
        Random random = new Random();
        for (int run = 0 ; run < RUNS ; run++) {
            String query = QUERIES.get(random.nextInt(QUERIES.size()));
            Request req = new Request(
                    DocumentKeys.SEARCH_QUERY, query,
                    DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
            long searchTime = -System.currentTimeMillis();
            try {
                List<String> ids = getAttributes(summon, req, "id", false);
                searchTime += System.currentTimeMillis();
                log.info(String.format("Test %d/%d. Got %d hits with connect time %dms for '%s' in %dms",
                                       run+1, RUNS, ids.size(), summon.getLastConnectTime(), query, searchTime));
                success++;
            } catch (Exception e) {
                searchTime += System.currentTimeMillis();
                if (e instanceof IllegalArgumentException) {
                    log.warn(String.format("Test %d/%d. Unable to get a result from '%s' in %d ms with connect " +
                                           "time %d due to illegal argument (probably a faulty query)",
                                           run+1, RUNS, query, searchTime, summon.getLastConnectTime()));
                } else if (e.getMessage().contains("java.net.SocketTimeoutException: connect timed out")) {
                    log.warn(String.format("Test %d/%d. Unable to get a result from '%s' in %d ms with connect " +
                                           "time %d due to connect timeout",
                                           run+1, RUNS, query, searchTime, summon.getLastConnectTime()));
                } else {
                    log.error(String.format("Test %d/%d. Unable to get a result from '%s' in %d ms with connect " +
                                            "time %d due to unexpected exception",
                                            run+1, RUNS, query, searchTime, summon.getLastConnectTime()), e);
                }
            }

            maxConnectTime = Math.max(maxConnectTime, summon.getLastConnectTime());
            if (run != RUNS-1) {
                int delay = DELAY_MS - VARIANCE_MS/2 + random.nextInt(VARIANCE_MS);
                synchronized (this) {
                    this.wait(delay);
                }
            }
        }
        log.info(String.format("Successfully performed %d/%d queries with max connect time %dms",
                               success, RUNS, maxConnectTime));
        summon.close();
    }

    public void testGetField() throws IOException, TransformerException {
        String ID = "summon_FETCH-gale_primary_2105957371";

        log.debug("Creating SummonSearchNode");
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        String fieldName = "shortformat";
        String field = getField(summon, ID, fieldName);
        assertTrue("The field '" + fieldName + "' from ID '" + ID + "' should have content",
                   field != null && !"".equals(field));
//        System.out.println("'" + field + "'");
    }

    /* This is equivalent to SearchWS#getField */
    private String getField(SearchNode searcher, String id, String fieldName) throws IOException, TransformerException {
        String retXML;

        Request req = new Request();
        req.put(DocumentKeys.SEARCH_QUERY, "ID:\"" + id + "\"");
        req.put(DocumentKeys.SEARCH_MAX_RECORDS, 1);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);

        ResponseCollection res = new ResponseCollection();
        searcher.search(req, res);
//        System.out.println(res.toXML());
        Document dom = DOM.stringToDOM(res.toXML());
        Node subDom = DOM.selectNode(
                dom, "/responsecollection/response/documentresult/record/field[@name='" + fieldName + "']");
        retXML = DOM.domToString(subDom);
        return retXML;
    }

    public void testNonExistingFacet() throws RemoteException {
        final Request request = new Request(
                "search.document.query", "foo",
                "search.document.filter", "Language:abcde32542f",
                "search.document.collectdocids", "true",
                "solr.filterisfacet", "true"
        );
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        for (Response response : responses) {
            if (response instanceof FacetResultExternal) {
                FacetResultExternal facets = (FacetResultExternal)response;
                for (Map.Entry<String, List<FacetResultImpl.Tag<String>>> entry: facets.getMap().entrySet()) {
                    assertEquals("The number of tags for facet '" + entry.getKey()
                                 + "' should be 0 as there should be no hits. First tag was '"
                                 + (entry.getValue().isEmpty() ? "N/A" : entry.getValue().get(0).getKey()) + "',",
                                 0, entry.getValue().size());
                }
            }
        }
    }

    // TODO: Test search for term with colon, quoted and unquoted
    public void testColonSearch() throws RemoteException {
        final String OK = "FETCH-proquest_dll_14482952011";
        final String PROBLEM = "FETCH-doaj_primary_oai:doaj-articles:932b6445ce452a2b2a544189863c472e1";
        performSearch("ID:\"" + OK + "\"");
        performSearch("ID:\"" + PROBLEM + "\"");
    }

    public void testColonNameSearch() throws RemoteException {
        performSearch("Gillis\\ P\\:son\\ Wetter");
    }

    public void testColonNameWeightedSearch() throws RemoteException {
        performSearch("P\\:son^1.2");
    }

    public void testColonFieldShortNameSearch() throws RemoteException {
        performSearch("AuthorCombined:(Gillis Wetter)");
    }

    public void testColonFieldLongNameSearch() throws RemoteException {
        performSearch("AuthorCombined:(Gillis P\\:son Wetter)");
    }

    public void testColonPhrasedNameSearch() throws RemoteException {
        performSearch("AuthorCombined:\"Gillis P:son Wetter\"");
    }

    private void performSearch(String query) throws RemoteException {
        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, query);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
//        System.out.println(responses.toXML());
        assertTrue("The result should contain at least one record for query '" + query + "'",
                   responses.toXML().contains("<record score"));
    }

    public void testFacetOrder() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        //SummonSearchNode.CONF_SOLR_FACETS, ""

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
//        summon.open(""); // Fake open for setting permits
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        List<String> facets = getFacetNames(responses);
        List<String> expected = new ArrayList<>(Arrays.asList(SummonSearchNode.DEFAULT_SUMMON_FACETS.split(" ?, ?")));
        expected.add("recordBase"); // We always add this when we're doing faceting
        for (int i = expected.size()-1 ; i >= 0 ; i--) {
            if (!facets.contains(expected.get(i))) {
                expected.remove(i);
            }
        }
        assertEquals("The order of the facets should be correct",
                     Strings.join(expected, ", "), Strings.join(facets, ", "));

//        System.out.println(responses.toXML());
//        System.out.println(Strings.join(facets, ", "));
    }

    public void testSpecificFacets() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        conf.set(SummonSearchNode.CONF_SOLR_FACETS, "SubjectTerms");

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        List<String> facets = getFacetNames(responses);
        assertEquals("The number of facets should be correct", 2, facets.size()); // 2 because of recordBase
        assertEquals("The returned facet should be correct",
                     "SubjectTerms, recordBase", Strings.join(facets, ", "));
    }

    public void testFacetSortingCount() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        conf.set(SummonSearchNode.CONF_SOLR_FACETS, "SubjectTerms");
        assertFacetOrder(conf, false);
    }

    // Summon does not support index ordering of facets so we must cheat by over-requesting and post-processing
    public void testFacetSortingAlpha() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        conf.set(SummonSearchNode.CONF_SOLR_FACETS, "SubjectTerms(ALPHA)");
        assertFacetOrder(conf, true);
    }

    private void assertFacetOrder(Configuration summonConf, boolean alpha) throws RemoteException {
        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(summonConf);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        List<String> tags = getTags(responses, "SubjectTerms");
        List<String> tagsAlpha = new ArrayList<>(tags);
        Collections.sort(tagsAlpha);
        if (alpha) {
            ExtraAsserts.assertEquals(
                    "The order should be alphanumeric\nExp: "
                    + Strings.join(tagsAlpha, " ") + "\nAct: " + Strings.join(tags, " "),
                    tagsAlpha, tags);
        } else {
            boolean misMatch = false;
            for (int i = 0 ; i < tags.size() ; i++) {
                if (!tags.get(i).equals(tagsAlpha.get(i))) {
                    misMatch = true;
                    break;
                }
            }
            if (!misMatch) {
                fail("The order should not be alphanumeric but it was");
            }
        }
        log.debug("Received facets with alpha=" + alpha + ": " + Strings.join(tags, ", "));
    }

    private List<String> getTags(ResponseCollection responses, String facet) {
        List<String> result = new ArrayList<>();
        for (Response response: responses) {
            if (response instanceof FacetResultExternal) {
                FacetResultExternal facetResult = (FacetResultExternal)response;
                List<FacetResultImpl.Tag<String>> tags = facetResult.getMap().get(facet);
                for (FacetResultImpl.Tag<String> tag: tags) {
                    result.add(tag.getKey() + "(" + tag.getCount() + ")");
                }
                return result;
            }
        }
        fail("Unable to locate a FacetResponse in the ResponseCollection");
        return null;
    }

    public void testNegativeFacets() throws RemoteException {
        final String QUERY = "foo fighters NOT limits NOT (boo OR bam)";
        final String FACET = "SubjectTerms:\"united states\"";
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        assertHits("There should be at least one hit for positive faceting",
                   summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, FACET);
        assertHits("There should be at least one hit for parenthesized positive faceting", summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, "(" + FACET + ")");
        assertHits("There should be at least one hit for filter with pure negative faceting", summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, "true",
                   DocumentKeys.SEARCH_FILTER, "NOT " + FACET);
        summon.close();
    }

    public void testFilterFacets() throws RemoteException {
        final String QUERY = "foo fighters";
        final String FACET = "SubjectTerms:\"united states\"";
        final String FACET_NEG = "-SubjectTerms:\"united states\"";
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        assertHits("There should be at least one hit for standard positive faceting",
                   summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, FACET);
        assertHits("There should be at least one hit for facet filter positive faceting",
                   summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, FACET,
                   SummonSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        assertHits("There should be at least one hit for standard negative faceting",
                   summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, FACET_NEG,
                   DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, "true");
        assertHits("There should be at least one hit for facet filter negative faceting",
                   summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, FACET_NEG,
                   SummonSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        summon.close();
    }

    // summon used to support pure negative filters (in 2011) but apparently does not with the 2.0.0-API.
    // If they change their stance on the issue, we want to switch back to using pure negative filters, as it
    // does not affect ranking.
    public void testNegativeFacetsSupport() throws RemoteException {
        final String QUERY = "foo fighters NOT limits NOT (boo OR bam)";
        final String FACET = "SubjectTerms:\"united states\"";
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SummonSearchNode.CONF_SUPPORTS_PURE_NEGATIVE_FILTERS, true);
        SummonSearchNode summon = new SummonSearchNode(conf);
        assertEquals("There should be zero hits for filter with assumed pure negative faceting support", 0,
                     getHitCount(summon, DocumentKeys.SEARCH_QUERY, QUERY, DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE,
                                 "true", DocumentKeys.SEARCH_FILTER,
                                 "NOT "
                                 + FACET));
        summon.close();
    }

    public void testQueryWithNegativeFacets() throws RemoteException {
        final String QUERY = "foo";
        final String FACET = "SubjectTerms:\"analysis\"";
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        assertHits("There should be at least one hit for positive faceting", summon, DocumentKeys.SEARCH_QUERY,
                   QUERY, DocumentKeys.SEARCH_FILTER, FACET);
        assertHits("There should be at least one hit for query with negative facet", summon,
                   DocumentKeys.SEARCH_QUERY, QUERY, DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, Boolean.TRUE.toString(), DocumentKeys.SEARCH_FILTER,
                   "NOT " + FACET);
        summon.close();
    }

    public void testSortedSearch() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "sort_year_asc - PublicationDate");

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
//        summon.open(""); // Fake open for setting permits
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "sb");
        request.put(DocumentKeys.SEARCH_SORTKEY, "PublicationDate");
//        request.put(DocumentKeys.SEARCH_REVERSE, true);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        List<String> sortValues = getAttributes(summon, request, "sortValue", true);
        String lastValue = null;
        for (String sortValue: sortValues) {
            assertTrue("The sort values should be in unicode order but was " + Strings.join(sortValues, ", "),
                       lastValue == null || lastValue.compareTo(sortValue) <= 0);
//            System.out.println(lastValue + " vs " + sortValue + ": " + (lastValue == null ? 0 : lastValue.compareTo(sortValue)));
            lastValue = sortValue;
        }
        log.debug("Test passed with sort values\n" + Strings.join(sortValues, "\n"));
    }

    public void testSortedDate() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "sort_year_asc - PublicationDate");

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
//        summon.open(""); // Fake open for setting permits
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "dolphin whale");
        request.put(DocumentKeys.SEARCH_SORTKEY, "PublicationDate");
//        request.put(DocumentKeys.SEARCH_REVERSE, true);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        List<String> fields = getField(summon, request, "PublicationDate_xml_iso");
        log.debug("Finished searching");
        String lastValue = null;
        for (String sortValue: fields) {
            assertTrue("The field values should be in unicode order but was " + Strings.join(fields, ", "),
                       lastValue == null || lastValue.compareTo(sortValue) <= 0);
//            System.out.println(lastValue + " vs " + sortValue + ": " + (lastValue == null ? 0 : lastValue.compareTo(sortValue)));
            lastValue = sortValue;
        }
        log.debug("Test passed with field values\n" + Strings.join(fields, "\n"));
    }

    public void testFacetedSearchNoFaceting() throws Exception {
        assertSomeHits(new Request(
                DocumentKeys.SEARCH_QUERY, "first"
        ));
    }

    public void testFacetedSearchNoHits() throws Exception {
        Request request = new Request(
                DocumentKeys.SEARCH_FILTER, "recordBase:nothere",
                DocumentKeys.SEARCH_QUERY, "first"
        );
        ResponseCollection responses = search(request);
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be no hits. Response was\n"
                     + responses.toXML(), 0, ((DocumentResponse) responses.iterator().next()).getHitCount());
    }

    public void testFacetedSearchSomeHits() throws Exception {
        assertSomeHits(new Request(DocumentKeys.SEARCH_FILTER, "recordBase:summon", DocumentKeys.SEARCH_QUERY, "first"));
    }

    public void testDashSomeHits() throws Exception {
        assertSomeHits(new Request(DocumentKeys.SEARCH_QUERY, "merrian - webster"));
    }

    public void testDashWeightSomeHits() throws Exception {
        assertSomeHits(new Request(DocumentKeys.SEARCH_QUERY, "merrian \\-^12.2 webster"));
    }

    public void testAmpersandWeightSomeHits() throws Exception {
        assertSomeHits(new Request(DocumentKeys.SEARCH_QUERY, "merrian &^12.2 webster"));
    }

    public void testDashWeightQuotedSomeHits() throws Exception {
        assertSomeHits(new Request(
                DocumentKeys.SEARCH_QUERY, "merrian \"-\"^12.2 webster"
        ));
    }

    private void assertSomeHits(Request request) throws RemoteException {
        ResponseCollection responses = search(request);
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertTrue("There should be some hits. Response was\n" + responses.toXML(),
                   ((DocumentResponse) responses.iterator().next()).getHitCount() > 0);
    }

    private ResponseCollection search(Request request) throws RemoteException {
        SearchNode searcher = SummonTestHelper.createSummonSearchNode();
        try {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(request, responses);
            return responses;
        } finally {
            searcher.close();
        }
    }

    public void testSortedSearchRelevance() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "sort_year_asc - PublicationDate");

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
//        summon.open(""); // Fake open for setting permits
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_SORTKEY, DocumentKeys.SORT_ON_SCORE);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        List<String> ids = getAttributes(summon, request, "id", false);
        assertTrue("There should be some hits", !ids.isEmpty());
    }

    public void testPaging() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
//        summon.open(""); // Fake open for setting permits
        List<String> ids0 = getAttributes(
                summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 20,
                DocumentKeys.SEARCH_START_INDEX, 0),
                "id", false);
        List<String> ids1 = getAttributes(
                summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 20,
                DocumentKeys.SEARCH_START_INDEX, 20),
                "id", false);
        List<String> ids2 = getAttributes(
                summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 20,
                DocumentKeys.SEARCH_START_INDEX, 40),
                "id", false);

        assertNotEquals("The hits should differ from page 0 and 1",
                        Strings.join(ids0, ", "), Strings.join(ids1, ", "));
        assertNotEquals("The hits should differ from page 1 and 2",
                        Strings.join(ids1, ", "), Strings.join(ids2, ", "));
    }

    public void testPaging20() throws RemoteException {
        testPaging(20);
    }

    private void testPaging(int pageSize) throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
//        summon.open(""); // Fake open for setting permits
        List<String> ids0 = getAttributes(
                summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, pageSize,
                DocumentKeys.SEARCH_START_INDEX, 0),
                "id", false);
        List<String> ids1 = getAttributes(
                summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, pageSize,
                DocumentKeys.SEARCH_START_INDEX, pageSize),
                "id", false);
        List<String> ids2 = getAttributes(
                summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, pageSize,
                DocumentKeys.SEARCH_START_INDEX, 2*pageSize),
                "id", false);

        assertNotEquals("The hits should differ from page 0 and 1",
                        Strings.join(ids0, ", "), Strings.join(ids1, ", "));
        assertNotEquals("The hits should differ from page 1 and 2",
                        Strings.join(ids1, ", "), Strings.join(ids2, ", "));

        List<String> ids12 = getAttributes(
                summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 2*pageSize,
                DocumentKeys.SEARCH_START_INDEX, 0),
                "id", false);

        List<String> idsConcatenated = new ArrayList<>(3*pageSize);
        idsConcatenated.addAll(ids0);
        idsConcatenated.addAll(ids1);

        assertEquals("The hits for 1 page of " + 2 * pageSize + " and 2 pages of " + pageSize + " should match",
                     Strings.join(ids12, "\n"), Strings.join(idsConcatenated, "\n"));
    }

    public void testPingFromSummaSearcher() throws IOException {
        Configuration conf = Configuration.newMemoryBased();
        SimplePair<String, String> credentials = SummonTestHelper.getCredentials();
        conf.set(SearchNodeFactory.CONF_NODE_CLASS, SummonSearchNode.class);
        conf.set(SummonSearchNode.CONF_SUMMON_ACCESSID, credentials.getKey());
        conf.set(SummonSearchNode.CONF_SUMMON_ACCESSKEY, credentials.getValue());

        SummaSearcher searcher = new SummaSearcherImpl(conf);
        ResponseCollection responses = searcher.search(new Request());
        assertTrue("The response collection should be empty", responses.isEmpty());
        assertTrue("The timing should contain summon.pingtime", responses.getTiming().contains("summon.pingtime"));
    }

    private void assertNotEquals(String message, String expected, String actual) {
        assertFalse(message + ".\nExpected: " + expected + "\nActual:   " + actual,
                    expected.equals(actual));
    }

    private List<String> getAttributes(
            SearchNode searcher, Request request, String attributeName, boolean explicitMerge) throws RemoteException {
        final Pattern IDPATTERN = Pattern.compile("<record.*?" + attributeName + "=\"(.+?)\".*?>", Pattern.DOTALL);
        return getPattern(searcher, request, IDPATTERN, explicitMerge);
/*        ResponseCollection responses = new ResponseCollection();
        searcher.search(request, responses);
        responses.iterator().next().merge(responses.iterator().next());
        String[] lines = responses.toXML().split("\n");
        List<String> result = new ArrayList<String>();
        for (String line: lines) {
            Matcher matcher = IDPATTERN.matcher(line);
            if (matcher.matches()) {
                result.add(matcher.group(1));
            }
        }
        return result;*/
    }

    private List<String> getField(SearchNode searcher, Request request, String fieldName) throws RemoteException {
        final Pattern IDPATTERN = Pattern.compile(
                "<field name=\"" + fieldName + "\">(.+?)</field>", Pattern.DOTALL);
        return getPattern(searcher, request, IDPATTERN, false);
    }

    private List<String> getFacetNames(ResponseCollection responses) {
        Pattern FACET = Pattern.compile(".*<facet name=\"(.+?)\">");
        List<String> result = new ArrayList<>();
        String[] lines = responses.toXML().split("\n");
        for (String line : lines) {
            Matcher matcher = FACET.matcher(line);
            if (matcher.matches()) {
                result.add(matcher.group(1));
            }
        }
        return result;
    }

    private List<String> getPattern(
            SearchNode searcher, Request request, Pattern pattern, boolean explicitMerge) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        searcher.search(request, responses);
        if (explicitMerge) {
            responses.iterator().next().merge(responses.iterator().next());
        }
        String xml = responses.toXML();
        Matcher matcher = pattern.matcher(xml);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(Strings.join(matcher.group(1).split("\n"), ", "));
        }
        return result;
    }

    public void testRecommendations() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "PublicationTitle:jama");
//        request.put(DocumentKeys.SEARCH_QUERY, "+(PublicationTitle:jama OR PublicationTitle:jama) +(ContentType:Article OR ContentType:\"Book Chapter\" OR ContentType:\"Book Review\" OR ContentType:\"Journal Article\" OR ContentType:\"Magazine Article\" OR ContentType:Newsletter OR ContentType:\"Newspaper Article\" OR ContentType:\"Publication Article\" OR ContentType:\"Trade Publication Article\")");
      /*
 \+\(PublicationTitle:jama\ OR\ PublicationTitle:jama\)\ \+\(ContentType:Article\ OR\ ContentType:\"Book\ Chapter\"\ OR\ ContentType:\"Book\ Review\"\ OR\ ContentType:\"Journal\ Article\"\ OR\ ContentType:\"Magazine\ Article\"\ OR\ ContentType:Newsletter\ OR\ ContentType:\"Newspaper\ Article\"\ OR\ ContentType:\"Publication\ Article\"\ OR\ ContentType:\"Trade\ Publication\ Article\"\)
      */
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        summon.search(request, responses);
//        System.out.println(responses.toXML());
        assertTrue("The result should contain at least one recommendation",
                   responses.toXML().contains("<recommendation "));

/*        responses.clear();
        request.put(DocumentKeys.SEARCH_QUERY, "noobddd");
        summon.search(request, responses);
        System.out.println(responses.toXML());
  */
    }

    public void testReportedTiming() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        summon.search(request, responses);
        Integer rawTime = getTiming(responses, "summon.rawcall");
        assertNotNull("There should be raw time", rawTime);
        Integer reportedTime = getTiming(responses, "summon.reportedtime");
        assertNotNull("There should be reported time", reportedTime);
        assertTrue("The reported time (" + reportedTime + ") should be lower than the raw time (" + rawTime + ")",
                   reportedTime <= rawTime);
        log.debug("Timing raw=" + rawTime + ", reported=" + reportedTime);
    }

    private Integer getTiming(ResponseCollection responses, String key) {
        String[] timings = responses.getTiming().split("[|]");
        for (String timing: timings) {
            String[] tokens = timing.split(":");
            if (tokens.length == 2 && key.equals(tokens[0])) {
                return Integer.parseInt(tokens[1]);
            }
        }
        return null;
    }

    public void testFilterVsQuery() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long qHitCount = getHitCount(summon,
                                     DocumentKeys.SEARCH_QUERY, "PublicationTitle:jama",
                                     SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, "true");
        long fHitCount = getHitCount(summon,
                                     DocumentKeys.SEARCH_FILTER, "PublicationTitle:jama",
                                     SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, "true");

        assertTrue("The filter hit count " + fHitCount + " should differ from query hit count " + qHitCount
                   + " by less than 100",
                   Math.abs(fHitCount - qHitCount) < 100);
    }

    public void testFilterVsQuery2() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long qHitCount = getHitCount(
                summon,
                DocumentKeys.SEARCH_QUERY, "PublicationTitle:jama",
                DocumentKeys.SEARCH_FILTER, "old");
        long fHitCount = getHitCount(
                summon,
                DocumentKeys.SEARCH_FILTER, "PublicationTitle:jama",
                DocumentKeys.SEARCH_QUERY, "old");

        assertTrue("The filter(old) hit count " + fHitCount + " should differ less than 100 from query(old) hit count "
                   + qHitCount + " as summon API 2.0.0 does field expansion on filters",
                   Math.abs(fHitCount - qHitCount) < 100);
        // This was only true in the pre-API 2.0.0. Apparently the new API does expands default fields for filters
//        assertTrue("The filter(old) hit count " + fHitCount + " should differ from query(old) hit count " + qHitCount
//                   + " by more than 100 as filter query apparently does not query parse with default fields",
//                   Math.abs(fHitCount - qHitCount) > 100);
    }

    public void testDismaxAnd() throws RemoteException {
        String QUERY1 = "public health policy";
        String QUERY2 = "alternative medicine";
        //String QUERY = "work and life balance";
        //String QUERY = "Small business and Ontario";
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        List<String> titlesLower = getField(summon, new Request(
                DocumentKeys.SEARCH_QUERY, QUERY1 + " and " + QUERY2,
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true,
                SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false
        ), "Title");
        String lower = Strings.join(titlesLower, "\n").replace("&lt;h&gt;", "").replace("&lt;/h&gt;", "");
        List<String> titlesUpper = getField(summon, new Request(
                DocumentKeys.SEARCH_QUERY, QUERY1 + " AND " + QUERY2,
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true,
                SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false
        ), "Title");
        String upper = Strings.join(titlesUpper, "\n").replace("&lt;h&gt;", "").replace("&lt;/h&gt;", "");

        summon.close();
        if (lower.equals(upper)) {
            fail("Using 'and' and 'AND' should not yield the same result\n" + lower);
        } else {
            System.out.println("Using 'and' and 'AND' gave different results:\nand: " +
                               lower.replace("\n", ", ") + "\nAND: " + upper.replace("\n", ", "));
        }
    }

    public void testDismaxWithQuoting() throws RemoteException {
        String QUERY = "public health policy and alternative medicine";
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();

        List<String> titlesRaw = getField(summon, new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true,
                SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false
        ), "Title");
        String raw = Strings.join(titlesRaw, "\n").replace("&lt;h&gt;", "").replace("&lt;/h&gt;", "");
        List<String> titlesQuoted = getField(summon, new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, false, // Adds quotes around individual terms
                SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false
        ), "Title");
        String quoted = Strings.join(titlesQuoted, "\n").replace("&lt;h&gt;", "").replace("&lt;/h&gt;", "");
        List<String> titlesNonDismaxed = getField(summon, new Request(
                DocumentKeys.SEARCH_QUERY, "(" + QUERY + ")",
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, false, // Adds quotes around individual terms
                SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false
        ), "Title");
        String nonDismaxed = Strings.join(titlesNonDismaxed, "\n").replace("&lt;h&gt;", "").replace("&lt;/h&gt;", "");

        summon.close();

        System.out.println("raw " + (raw.equals(quoted) ? "=" : "!") + "= quoted");
        System.out.println("raw " + (raw.equals(nonDismaxed) ? "=" : "!") + "= parenthesized");
        System.out.println("quoted " + (quoted.equals(nonDismaxed) ? "=" : "!") + "= parenthesized");
        System.out.println("raw =           " + raw.replace("\n", ", "));
        System.out.println("quoted =        " + quoted.replace("\n", ", "));
        System.out.println("parenthesized = " + nonDismaxed.replace("\n", ", "));

        assertEquals("The result from the raw (and thus dismaxed) query should match the result from "
                     + "the quoted terms query",
                     raw, quoted);
    }

    public void testFilterVsQuery3() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long qCombinedHitCount = getHitCount(
                summon,
                DocumentKeys.SEARCH_QUERY,
                "PublicationTitle:jama AND Language:English");
        long qHitCount = getHitCount(
                summon,
                DocumentKeys.SEARCH_QUERY, "PublicationTitle:jama",
                DocumentKeys.SEARCH_FILTER, "(Language:English)");
        long fHitCount = getHitCount(
                summon,
                DocumentKeys.SEARCH_FILTER, "PublicationTitle:jama",
                DocumentKeys.SEARCH_QUERY, "Language:English");

        assertTrue("The filter(old) hit count " + fHitCount + " should differ"
                   + " from query(old) hit count " + qHitCount
                   + " by less than 100. Combined hit count for query is "
                   + qCombinedHitCount,
                   Math.abs(fHitCount - qHitCount) < 100);
    }

    public void testCustomParams() throws RemoteException {
        final String QUERY = "reactive arthritis yersinia lassen";

        Configuration confInside = SummonTestHelper.getDefaultSummonConfiguration();
        Configuration confOutside = SummonTestHelper.getDefaultSummonConfiguration();
        confOutside.set(SummonSearchNode.CONF_SOLR_PARAM_PREFIX + "s.ho",
                        new ArrayList<>(Arrays.asList("false"))
        );

        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, QUERY);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);

        SummonSearchNode summonInside = new SummonSearchNode(confInside);
        ResponseCollection responsesInside = new ResponseCollection();
        summonInside.search(request, responsesInside);

        SummonSearchNode summonOutside = new SummonSearchNode(confOutside);
        ResponseCollection responsesOutside = new ResponseCollection();
        summonOutside.search(request, responsesOutside);

        request.put(SummonSearchNode.CONF_SOLR_PARAM_PREFIX + "s.ho",
                    new ArrayList<>(Arrays.asList("false")));
        ResponseCollection responsesSearchTweak = new ResponseCollection();
        summonInside.search(request, responsesSearchTweak);

        int countInside = countResults(responsesInside);
        int countOutside = countResults(responsesOutside);
        assertTrue("The number of results for a search for '" + QUERY + "' within holdings (" + confInside
                   + ") should be less that outside holdings (" + confOutside + ")",
                   countInside < countOutside);
        log.info(String.format("The search for '%s' gave %d hits within holdings and %d hits in total",
                               QUERY, countInside, countOutside));

        int countSearchTweak = countResults(responsesSearchTweak);
        assertEquals(
                "Query time specification of 's.ho=false' should give the same "
                + "result as configuration time specification of the same",
                countOutside, countSearchTweak);
    }

    public void testConvertRangeQueries() throws RemoteException {
        final String QUERY = "foo bar:[10 TO 20] OR baz:[87 TO goa]";
        Map<String, List<String>> params = new HashMap<>();
        String stripped = new SummonSearchNode(getSummonConfiguration()).convertQuery(QUERY, params);
        assertNotNull("RangeFilter should be defined", params.get("s.rf"));
        List<String> ranges = params.get("s.rf");
        assertEquals("The right number of ranges should be extracted", 2, ranges.size());
        assertEquals("Range #1 should be correct", "bar,10:20", ranges.get(0));
        assertEquals("Range #2 should be correct", "baz,87:goa", ranges.get(1));
        assertEquals("The resulting query should be stripped of ranges", "\"foo\"", stripped);
    }

    public void testConvertRangeQueriesEmpty() throws RemoteException {
        final String QUERY = "bar:[10 TO 20]";
        Map<String, List<String>> params = new HashMap<>();
        String stripped = new SummonSearchNode(getSummonConfiguration()).convertQuery(QUERY, params);
        assertNotNull("RangeFilter should be defined", params.get("s.rf"));
        List<String> ranges = params.get("s.rf");
        assertEquals("The right number of ranges should be extracted", 1, ranges.size());
        assertEquals("Range #1 should be correct", "bar,10:20", ranges.get(0));
        assertNull("The resulting query should be null", stripped);
    }

    private Configuration getSummonConfiguration() {
        return Configuration.newMemoryBased(
                SummonSearchNode.CONF_SUMMON_ACCESSID, "foo",
                SummonSearchNode.CONF_SUMMON_ACCESSKEY, "bar");
    }

    public void testFaultyQuoteRemoval() throws RemoteException {
        final String QUERY = "bar:\"foo:zoo\"";
        Map<String, List<String>> params = new HashMap<>();
        String stripped = new SummonSearchNode(getSummonConfiguration()).convertQuery(QUERY, params);
        assertNull("RangeFilter should not be defined", params.get("s.rf"));
        assertEquals("The resulting query should unchanged", QUERY, stripped);
    }

    // This fails, but as we are really testing Summon here, there is not much
    // we can do about it
    @SuppressWarnings({"UnusedDeclaration"})
    public void disabledtestCounts() throws RemoteException {
        //      final String QUERY = "reactive arthritis yersinia lassen";
        final String QUERY = "author:(Helweg Larsen) abuse";

        Request request = new Request();
        request.addJSON(
                "{search.document.query:\"" + QUERY + "\", summonparam.s.ps:\"15\", summonparam.s.ho:\"false\"}");
//        String r1 = request.toString(true);

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        summon.search(request, responses);
        int count15 = countResults(responses);

        request.clear();
        request.addJSON(
                "{search.document.query:\"" + QUERY + "\", summonparam.s.ps:\"30\", summonparam.s.ho:\"false\"}");
        //      String r2 = request.toString(true);
        responses.clear();
        summon.search(request, responses);
        int count20 = countResults(responses);
/*
        request.clear();
        request.put(DocumentKeys.SEARCH_QUERY, QUERY);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        request.put(SummonSearchNode.CONF_SOLR_PARAM_PREFIX + "s.ho",
                    new ArrayList<String>(Arrays.asList("false")));
        request.put(DocumentKeys.SEARCH_MAX_RECORDS, 15);
        String rOld15 = request.toString(true);
        responses.clear();
        summon.search(request, responses);
        int countOld15 = countResults(responses);

        request.clear();
        request.put(DocumentKeys.SEARCH_QUERY, QUERY);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        request.put(SummonSearchNode.CONF_SOLR_PARAM_PREFIX + "s.ho",
                    new ArrayList<String>(Arrays.asList("false")));
        request.put(DocumentKeys.SEARCH_MAX_RECORDS, 20);
        String rOld20 = request.toString(true);
        responses.clear();
        summon.search(request, responses);
        int countOld20 = countResults(responses);
  */
//        System.out.println("Request 15:  " + r1 + ": " + count15);
//        System.out.println("Request 20:  " + r2 + ": " + count20);
//        System.out.println("Request O15: " + rOld15 + ": " + countOld15);
//        System.out.println("Request O20: " + rOld20 + ": " + countOld20);
        assertEquals("The number of hits should not be affected by page size", count15, count20);
    }

    // Author can be returned in the field Author_xml (primary) and Author (secondary). If both fields are present,
    // Author should be ignored.
    public void testAuthorExtraction() throws IOException {

    }

    private int countResults(ResponseCollection responses) {
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                return (int)((DocumentResponse)response).getHitCount();
            }
        }
        throw new IllegalArgumentException(
                "No documentResponse in ResponseCollection");
    }

    public void testAdjustingSearcher() throws IOException {
        SimplePair<String, String> credentials = SummonTestHelper.getCredentials();
        Configuration conf = Configuration.newMemoryBased(
                InteractionAdjuster.CONF_IDENTIFIER, "summon",
                InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "recordID - ID");
        Configuration inner = conf.createSubConfiguration(AdjustingSearchNode.CONF_INNER_SEARCHNODE);
        inner.set(SearchNodeFactory.CONF_NODE_CLASS, SummonSearchNode.class.getCanonicalName());
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSID, credentials.getKey());
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSKEY, credentials.getValue());

        log.debug("Creating adjusting SummonSearchNode");
        AdjustingSearchNode adjusting = new AdjustingSearchNode(conf);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request(
                //request.put(DocumentKeys.SEARCH_QUERY, "foo");
                DocumentKeys.SEARCH_QUERY, "recursion in string theory",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        adjusting.search(request, responses);
        log.debug("Finished searching");
        // TODO: Add proper test
//        System.out.println(responses.toXML());
    }

    public void testExplicitMust() throws IOException {
        final String QUERY = "miller genre as social action";
        ResponseCollection explicitMustResponses = new ResponseCollection();
        {
            Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
            conf.set(QueryRewriter.CONF_TERSE, false);
            SearchNode summon  = new SummonSearchNode(conf);
            Request request = new Request(
                    DocumentKeys.SEARCH_QUERY, QUERY
            );
            summon.search(request, explicitMustResponses);
            summon.close();
        }

        ResponseCollection implicitMustResponses = new ResponseCollection();
        {
            Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
            conf.set(QueryRewriter.CONF_TERSE, true);
            SearchNode summon  = new SummonSearchNode(conf);
            Request request = new Request(
                    DocumentKeys.SEARCH_QUERY, QUERY
            );
            summon.search(request, implicitMustResponses);
            summon.close();
        }
        HarmoniseTestHelper.compareHits(QUERY, false, explicitMustResponses, implicitMustResponses);
    }

    /*
    Tests if explicit weight-adjustment of terms influences the scores significantly.
     */
    public void testExplicitWeightScoring() throws RemoteException {
        assertScores("dolphin whales", "dolphin whales^1.000001", 10.0f);
    }

    /*
    Tests if explicit weight-adjustment of terms influences the order of documents.
     */
    public void testExplicitWeightOrder() throws RemoteException {
        assertOrder("dolphin whales", "dolphin whales^1.0");
    }

    public void testExplicitWeightOrderSingleTerm() throws RemoteException {
        assertOrder("whales", "whales^1.0");
    }

    public void testExplicitWeightOrderFoo() throws RemoteException {
        assertOrder("foo", "foo^1.0"); // By some funny coincidence, foo works when whales doesn't
    }

    private void assertOrder(String query1, String query2) throws RemoteException {
        SearchNode summon  = SummonTestHelper.createSummonSearchNode();
        try {
            List<String> ids1 = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_QUERY, query1,
                    SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true
            ), "id", false);
            List<String> ids2 = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_QUERY, query2,
                    SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true
            ), "id", false);
            ExtraAsserts.assertPermutations("Query '" + query1 + "' and '" + query2 + "'", ids1, ids2);
/*            assertEquals("The number of hits for '" + query1 + "' and '" + query2 + "' should be equal",
                         ids1.size(), ids2.size());
            assertEquals("The document order for '" + query1 + "' and '" + query2 + "' should be equal",
                         Strings.join(ids1, ", "), Strings.join(ids2, ", "));
                         */
        } finally {
            summon.close();
        }
    }

    public void testDisMaxDisabling() throws RemoteException {
        final String QUERY= "asian philosophy";
        SearchNode summon  = SummonTestHelper.createSummonSearchNode();
        try {
            long plainCount =
                    getHitCount(summon, DocumentKeys.SEARCH_QUERY, QUERY, SummonSearchNode.SEARCH_DISMAX_SABOTAGE, "false");
            long sabotagedCount =
                    getHitCount(summon, DocumentKeys.SEARCH_QUERY, QUERY, SummonSearchNode.SEARCH_DISMAX_SABOTAGE, "true");
            assertEquals("The number of hits for a DisMax-enabled and DisMax-sabotages query should match",
                         plainCount, sabotagedCount);

            List<String> plain = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_QUERY, QUERY, SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false), "id", false);
            List<String> sabotaged = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_QUERY, QUERY, SummonSearchNode.SEARCH_DISMAX_SABOTAGE, true), "id", false);
            assertFalse("The ids returned by DisMax-enabled and DisMax-sabotaged query should differ",
                        Strings.join(plain, ", ").equals(Strings.join(sabotaged, ", ")));
        } finally {
            summon.close();
        }
    }

    public void testDismaxDisablingExperiment() throws RemoteException {
        assertOrder("foo bar", "(foo bar)");
    }


    /*
    Tests if quoting of terms influences the scores significantly.
     */
    public void testQuotingScoring() throws RemoteException {
        assertScores("dolphin whales", "\"dolphin\" \"whales\"", 10.0f);
    }

    private void assertScores(String query1, String query2, float maxDifference) throws RemoteException {
        SearchNode summon  = SummonTestHelper.createSummonSearchNode();

        ResponseCollection raw = new ResponseCollection();
        summon.search(new Request(
                DocumentKeys.SEARCH_QUERY, query1,
                SolrSearchNode.SEARCH_PASSTHROUGH_QUERY, true
        ), raw);

        ResponseCollection weighted = new ResponseCollection();
        summon.search(new Request(DocumentKeys.SEARCH_QUERY, query2), weighted);

        summon.close();

        List<Double> rawScores = getScores(raw);
        List<Double> weightedScores = getScores(weighted);

        assertEquals("The number of hits for '" + query1 + "' and '" + query2 + "' should be equal",
                     rawScores.size(), weightedScores.size());
        for (int i = 0 ; i < rawScores.size() ; i++) {
            assertTrue(String.format(
                    "The scores at position %d were %s and %s. Max difference allowed is %s. "
                    + "All scores for '%s' and '%s':\n%s\n%s",
                    i, rawScores.get(i), weightedScores.get(i), maxDifference,
                    query1, query2, Strings.join(rawScores, ", "), Strings.join(weightedScores, ", ")),
                       Math.abs(rawScores.get(i) - weightedScores.get(i)) <= maxDifference);
        }
    }

    private List<Double> getScores(ResponseCollection responses) {
        DocumentResponse docs = (DocumentResponse)responses.iterator().next();
        List<Double> result = new ArrayList<>(docs.size());
        for (DocumentResponse.Record record: docs.getRecords()) {
            result.add((double)record.getScore());
        }
        return result;
    }


    public void testAuthorsAsXML() throws RemoteException, XMLStreamException {
        final String query = "PQID:821707502";

        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SummonResponseBuilder.CONF_XML_FIELD_HANDLING, SummonResponseBuilder.XML_MODE.full);
        SummonSearchNode summon = new SummonSearchNode(conf);

        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, query);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);

        final Pattern AUTHOR_XML_FIELDS = Pattern.compile("(<field name=\"Author_xml\">.+?</field>)", Pattern.DOTALL);
        List<String> authorXMLFields = getPattern(summon, request, AUTHOR_XML_FIELDS, false);
        String content = Strings.join(authorXMLFields, "\n");
        assertEquals("There should be the right number of 'Author_xml'-fields\n" + content,
                     1, authorXMLFields.size());
        final AtomicInteger count = new AtomicInteger(0);
        XMLStepper.iterateTags(
                xmlFactory.createXMLStreamReader(new StringReader(authorXMLFields.get(0))),
                new XMLStepper.Callback() {
                    @Override
                    public boolean elementStart(
                            XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                        count.incrementAndGet();
                        return false;
                    }
                });
        // field is the first start-element
        assertEquals("There should be the right number of author contributor elements", 1+5, count.get());

        log.info("Got XML-fields\n" + content);
    }


    // Author_xml contains the authoritative order for the authors so it should override the non-XML-version
    public void testAuthor_xmlExtraction() throws RemoteException {
        String fieldName = "Author";
        String query = "PQID:821707502";

/*        {
            Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
            conf.set(SummonResponseBuilder.CONF_XML_OVERRIDES_NONXML, false);
            SearchNode summon = new SummonSearchNode(conf);
            ResponseCollection responses = new ResponseCollection();
            summon.search(new Request(DocumentKeys.SEARCH_QUERY, query), responses);

            DocumentResponse docs = (DocumentResponse)responses.iterator().next();
            for (DocumentResponse.Record record: docs.getRecords()) {
                System.out.println("\n" + record.getId());
                String author = "";
                String author_xml = "";
                for (DocumentResponse.Field field: record.getFields()) {
                    if ("Author".equals(field.getName())) {
                        author = field.getContent().replace("\n", ", ").replace("<h>", "").replace("</h>", "");
                        System.out.println("Author:     " + author);
                    } else if ("Author_xml".equals(field.getName())) {
                        author_xml = field.getContent().replace("\n", ", ");
                        System.out.println("Author_xml: " + author_xml);
                    } else if ("PQID".equals(field.getName())) {
                        System.out.println("PQID: " + field.getContent());
                    }
                }
                if (author.length() != author_xml.length()) {
                    fail("We finally found a difference between Author and Author_xml besides name ordering");
                }
            }
            summon.close();
        }
  */

        { // Old behaviour
            String expected = "Koetse, Willem\n"
                              + "Krebs, Christopher P\n"
                              + "Lindquist, Christine\n"
                              + "Lattimore, Pamela K\n"
                              + "Cowell, Alex J";
            Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
            conf.set(SummonResponseBuilder.CONF_XML_OVERRIDES_NONXML, false);
            SearchNode summonNonXML = new SummonSearchNode(conf);
            assertFieldContent("XML no override", summonNonXML, query, fieldName, expected, false);
            summonNonXML.close();
        }

        { // New behaviour
            String expected = "Lattimore, Pamela K\n"
                              + "Krebs, Christopher P\n"
                              + "Koetse, Willem\n"
                              + "Lindquist, Christine\n"
                              + "Cowell, Alex J";
            SearchNode summonXML = SummonTestHelper.createSummonSearchNode();
            assertFieldContent("XML override", summonXML, query, fieldName, expected, false);
            summonXML.close();
        }

    }

    public void testAuthor_xmlExtraction_shortformat() throws RemoteException {
        String query = "ID:FETCH-LOGICAL-c937-88b9adcf681a925445118c26ea8da2ed792f182b51857048dbb48b11a133ea321";
        { // sanity-check the Author-field
            String expected = "Ferlay, Jacques\n"
                              + "Shin, Hai-Rim\n"
                              + "Bray, Freddie\n"
                              + "Forman, David\n"
                              + "Mathers, Colin\n"
                              + "Parkin, Donald Maxwell";
            SearchNode summon = SummonTestHelper.createSummonSearchNode();
            assertFieldContent("author direct", summon, query, "Author", expected, false);
            summon.close();
        }

        { // shortformat should match Author
            String expected =
                    "  <shortrecord>\n" +
                    "    <rdf:RDF xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                    "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
                    "      <rdf:Description>\n" +
                    "        <dc:title>Estimates of worldwide burden of cancer in 2008: GLOBOCAN 2008</dc:title>\n" +
                    "        <dc:creator>Ferlay, Jacques</dc:creator>\n" +
                    "        <dc:type xml:lang=\"da\">Journal Article</dc:type>\n" +
                    "        <dc:type xml:lang=\"en\">Journal Article</dc:type>\n" +
                    "        <dc:date>2010</dc:date>\n" +
                    "        <dc:format></dc:format>\n" +
                    "        <dc:identifier>ctx_ver=Z39.88-2004&amp;ctx_enc=info%3Aofi%2Fenc%3AUTF-8&amp;rfr_id=info:sid/summon.serialssolutions.com&amp;rft_val_fmt=info:ofi/fmt:kev:mtx:journal&amp;rft.genre=article&amp;rft.atitle=Estimates+of+worldwide+burden+of+cancer+in+2008%3A+GLOBOCAN+2008&amp;rft.jtitle=International+journal+of+cancer.+Journal+international+du+cancer&amp;rft.au=Ferlay%2C+Jacques&amp;rft.au=Shin%2C+Hai-Rim&amp;rft.au=Bray%2C+Freddie&amp;rft.au=Forman%2C+David&amp;rft.date=2010-12-15&amp;rft.eissn=1097-0215&amp;rft.volume=127&amp;rft.issue=12&amp;rft.spage=2893&amp;rft_id=info:pmid/21351269&amp;rft.externalDocID=21351269</dc:identifier>\n" +
                    "      </rdf:Description>\n" +
                    "    </rdf:RDF>\n" +
                    "  </shortrecord>\n";
            SearchNode summon = SummonTestHelper.createSummonSearchNode();
            assertFieldContent("shortformat", summon, query, "shortformat", expected, false);
            summon.close();
        }
    }

    public void testAuthor_xmlExtraction_shortformat2() throws RemoteException {
        String query = "ID:FETCH-LOGICAL-c1590-71216b8d44129eb55dba9244d0a7ad32261d9b5e7a00e7987e3aa5b33750b0dc1";
        { // sanity-check the Author-field
            String expected = "Fallah, Mahdi\n"
                              + "Kharazmi, Elham";
            SearchNode summon = SummonTestHelper.createSummonSearchNode();
            assertFieldContent("author direct", summon, query, "Author", expected, false);
            summon.close();
        }

        { // shortformat should match Author
            String expected =
                    "  <shortrecord>\n"
                    + "    <rdf:RDF xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
                    + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                    + "      <rdf:Description>\n"
                    + "        <dc:title>Substantial under-estimation in cancer incidence estimates for developing "
                    + "countries due to under-ascertainment in elderly cancer cases</dc:title>\n"
                    + "        <dc:creator>Fallah, Mahdi</dc:creator>\n"
                    + "        <dc:creator>Kharazmi, Elham</dc:creator>\n"
                    + "        <dc:type xml:lang=\"da\">Journal Article</dc:type>\n"
                    + "        <dc:type xml:lang=\"en\">Journal Article</dc:type>\n"
                    + "        <dc:date>2008</dc:date>\n"
                    + "        <dc:format></dc:format>\n"
                    + "      </rdf:Description>\n"
                    + "    </rdf:RDF>\n"
                    + "  </shortrecord>\n";
            SearchNode summon = SummonTestHelper.createSummonSearchNode();
            assertFieldContent("shortformat", summon, query, "shortformat", expected, false);
            summon.close();
        }
    }

    public void testScoreAssignment() throws RemoteException {
        String QUERY =
                "The effect of multimedia on perceived equivocality and perceived usefulness of information systems";
        String BAD =
                "<record score=\"0.0\" "
                + "id=\"summon_FETCH-LOGICAL-j865-7bb06e292771fe19b17b4f676a0939e693be812b38d8502735ffb8ab6e46b4d21\" "
                + "source=\"Summon\">";
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                DocumentKeys.SEARCH_MAX_RECORDS, 10,
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false);

        ResponseCollection responses = new ResponseCollection();
        summon.search(req, responses);
        assertFalse("There should be a score != 0.0 for all records in\n" + responses.iterator().next().toXML(),
                    responses.iterator().next().toXML().contains(BAD));
        summon.close();

    }

    private void assertFieldContent(String message, SearchNode searchNode, String query, String fieldName,
                                    String expected, boolean sort) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        searchNode.search(new Request(DocumentKeys.SEARCH_QUERY, query), responses);
        DocumentResponse docs = (DocumentResponse)responses.iterator().next();
        assertEquals(message + ". There should only be a single hit", 1, docs.getHitCount());
        boolean found = false;
        for (DocumentResponse.Record record: docs.getRecords()) {
            for (DocumentResponse.Field field: record) {
                if (fieldName.equals(field.getName())) {
                    String content = field.getContent();
                    if (sort) {
                        String[] tokens = content.split("\n");
                        Arrays.sort(tokens);
                        content = Strings.join(tokens, "\n");

                    }
                    assertEquals(message + ".The field '" + fieldName + "' should have the right content",
                                 expected, content);
                    found = true;
                }
            }
        }
        if (!found) {
            fail("Unable to locate the field '" + fieldName + "'");
        }
    }

    public void testIDAdjustment() throws IOException {
        SimplePair<String, String> credentials = SummonTestHelper.getCredentials();
        Configuration conf = Configuration.newMemoryBased(
                InteractionAdjuster.CONF_IDENTIFIER, "summon",
                InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "recordID - ID");
        Configuration inner = conf.createSubConfiguration(
                AdjustingSearchNode.CONF_INNER_SEARCHNODE);
        inner.set(SearchNodeFactory.CONF_NODE_CLASS, SummonSearchNode.class.getCanonicalName());
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSID, credentials.getKey());
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSKEY, credentials.getValue());

        log.debug("Creating adjusting SummonSearchNode");
        AdjustingSearchNode adjusting = new AdjustingSearchNode(conf);
        Request request = new Request();
        //request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_QUERY, "recursion in string theory");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        List<String> ids = getAttributes(adjusting, request, "id", false);
        assertTrue("There should be at least one ID", !ids.isEmpty());

        request.clear();
        request.put(DocumentKeys.SEARCH_QUERY, IndexUtils.RECORD_FIELD + ":\"" + ids.get(0) + "\"");
        List<String> researchIDs = getAttributes(adjusting, request, "id", false);
        assertTrue("There should be at least one hit for a search for ID '"
                   + ids.get(0) + "'", !researchIDs.isEmpty());
    }

    // TODO: "foo:bar zoo"


    // It seems that "Book / eBook" is special and will be translated to s.fvgf (Book OR eBook) by summon
    // This is important as it means that we cannot use filter ContentType:"Book / eBook" to get the same
    // hits as a proper facet query
    public void testFacetTermWithDivider() throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);

        long filterCount = getHitCount(
                summon,
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_FILTER, "ContentType:\"Book / eBook\"",
                SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        long queryCount = getHitCount(
                summon,
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_FILTER, "ContentType:Book OR ContentType:eBook");

        assertTrue("There should be at least 1 hit for either query or filter request",
                   queryCount > 0 || filterCount > 0);
        assertEquals("The number of hits for filter and query based restrictions should be the same",
                     filterCount, queryCount);
        summon.close();
    }

    public void testFacetFieldValidity() throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        String[][] FACET_QUERIES = new String[][]{
                //{"Ferlay", "Author", "Ferlay\\, Jacques"}, // We need a sample from the Author facet
                {"foo", "Language", "German"},
                {"foo", "IsScholarly", "true"},
                {"foo", "IsFullText", "true"},
                {"foo", "ContentType", "Book / eBook"},
                {"foo", "SubjectTerms", "biology"}
        };
        for (String[] facetQuery: FACET_QUERIES) {
            String q = facetQuery[0];
            String ff = facetQuery[1] + ":\"" + facetQuery[2] + "\"";
            log.debug(String.format("Searching for query '%s' with facet filter '%s'", q, ff));
            long queryCount = getHitCount(
                    summon,
                    DocumentKeys.SEARCH_QUERY, q,
                    DocumentKeys.SEARCH_FILTER, ff,
                    SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
            assertTrue(String.format("There should be at least 1 hit for query '%s' with facet filter '%s'", q, ff),
                       queryCount > 0);
        }
    }

    public void testFilterEffect() throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        String[][] FACET_QUERIES = new String[][]{
                //{"Ferlay", "Author", "Ferlay\\, Jacques"}, // We need a sample from the Author facet
                {"foo", "Language", "German"},
                {"foo", "IsScholarly", "true"},
                //  {"foo", "IsFullText", "true"},
                {"foo", "ContentType", "Book / eBook"},
                {"foo", "SubjectTerms", "biology"}
        };
        for (String[] facetQuery: FACET_QUERIES) {
            String q = facetQuery[0];
            String ff = facetQuery[1] + ":\"" + facetQuery[2] + "\"";

            long queryCount = getHitCount(
                    summon,
                    DocumentKeys.SEARCH_QUERY, q,
                    SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
            log.debug(String.format("Searching for query '%s' with no facet filter gave %d hits'", q, queryCount));
            assertTrue(String.format("There should be at least 1 hit for query '%s' with no facet filter", q),
                       queryCount > 0);

            long filteredCount = getHitCount(
                    summon,
                    DocumentKeys.SEARCH_QUERY, q,
                    DocumentKeys.SEARCH_FILTER, ff,
                    SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
            log.debug(String.format("Searching for query '%s' with facet filter '%s' gave %d hits",
                                    q, ff, filteredCount));

            assertTrue(String.format("There should not be the same number of hits with and without filtering for " +
                                     "query '%s' with facet filter '%s but there were %d hits'", q, ff, queryCount),
                       queryCount != filteredCount);
        }
    }

    public void testEmptyQuerySkip() throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        long hits = getHitCount(
                summon,
                DocumentKeys.SEARCH_QUERY, "",
                SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        summon.close();
        assertEquals("There should be zero hits", 0, hits);
    }

    public void testIsScholarly() throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        String q = "foo";
        long queryCount = getHitCount(
                summon,
                DocumentKeys.SEARCH_QUERY, "foo",
                SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        log.debug(String.format("Searching for query '%s' with no special params gave %d hits'", q, queryCount));
        assertTrue(String.format("There should be at least 1 hit for query '%s' with no special params", q),
                   queryCount > 0);

        long filteredCount = getHitCount(
                summon,
                DocumentKeys.SEARCH_QUERY, q,
                "summonparam.s.cmd", "addFacetValueFilters(IsScholarly,true)",
                SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        log.debug(String.format("Searching for query '%s' with special param " +
                                "summonparam.s.cmd=addFacetValueFilters(IsScholarly,true) gave %d hits",
                                q, filteredCount));

        assertTrue(String.format("There should not be the same number of hits for special param " +
                                 "summonparam.s.cmd=addFacetValueFilters(IsScholarly,true) with and without " +
                                 "filtering for query '%s' but there were %d hits'", q, queryCount),
                   queryCount != filteredCount);
    }

    public void testQueryNoFilter() throws Exception {
        assertResponse(new Request(DocumentKeys.SEARCH_QUERY, "foo"), true);
    }

    public void testFilterNoQuery() throws Exception {
        assertResponse(new Request(DocumentKeys.SEARCH_FILTER, "foo"), true);
    }

    public void testFilterAndQuery() throws Exception {
        assertResponse(new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_FILTER, "bar"
        ), true);
    }

    public void testNoFilterNoQuery() throws Exception {
        assertResponse(new Request("foo", "bar"), false);
    }

    private void assertResponse(Request request, boolean responseExpected) throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        ResponseCollection responses = new ResponseCollection();
        summon.search(request, responses);
        if (responseExpected) {
            assertTrue("There should be a response for " + request, responses.iterator().hasNext());
        } else {
            assertFalse("There should not be a response for " + request, responses.iterator().hasNext());
        }
    }


    protected List<String> getHits(SearchNode searcher, String... arguments) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(arguments), responses);
        return getHits(responses);
    }

    private List<String> getHits(ResponseCollection responses) {
        List<String> ids = new ArrayList<>();
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                DocumentResponse docs = (DocumentResponse)response;
                for (DocumentResponse.Record record: docs.getRecords()) {
                    ids.add(record.getId());
                }
            }
        }
        return ids;
    }

    protected long getHitCount(SearchNode searcher, String... arguments) throws RemoteException {
        String HITS_PATTERN = "(?s).*hitCount=\"([0-9]*)\".*";
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(arguments), responses);
        if (!Pattern.compile(HITS_PATTERN).matcher(responses.toXML()).matches()) {
            return 0;
        }
        String hitsS = responses.toXML().replaceAll(HITS_PATTERN, "$1");
        return "".equals(hitsS) ? 0L : Long.parseLong(hitsS);
    }

    protected void assertHits(String message, SearchNode searcher, String... queries) throws RemoteException {
        long hits = getHitCount(searcher, queries);
        assertTrue(message + ". Hits == " + hits, hits > 0);
    }

    protected void assertHits(String message, String query, int expectedHits) throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long hits = getHitCount(summon, DocumentKeys.SEARCH_QUERY, query);
        assertEquals(message + ". Query='" + query + "'", expectedHits, hits);
    }
}
