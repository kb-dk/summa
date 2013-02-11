package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.search.SummaSearcherAggregator;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.Timer;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AdjustingSearcherAggregatorTest extends TestCase {
    private static Log log =
        LogFactory.getLog(AdjustingSearcherAggregatorTest.class);

    public AdjustingSearcherAggregatorTest(String name) {
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
        return new TestSuite(AdjustingSearcherAggregatorTest.class);
    }

    // Very bogus as it requires already running searchers at specific
    // addresses on the local machine
    // TODO: Create a unit test that works independently of running searchers
    public void testAggregator() throws IOException, SubConfigurationsNotSupportedException {
        AdjustingSearcherAggregator aggregator = getAggregator();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        ResponseCollection responses = aggregator.search(request);
        log.debug("Finished searching");
//        System.out.println(responses.toXML());
        assertTrue("The combined result should contain at least one record",
                   responses.toXML().contains("<record score"));
        assertTrue("The combined result should contain at least one tag",
                   responses.toXML().contains("<tag name"));

        String SECURITY = "security crisis";
        request.put(DocumentKeys.SEARCH_QUERY, SECURITY);
        log.debug("Searching for " + SECURITY);
        responses = aggregator.search(request);
        log.debug("Finished searching");
//        System.out.println(responses.toXML());
        assertTrue("The combined result should contain at least one record",
                   responses.toXML().contains("<record score"));
        assertTrue("The combined result should contain at least one tag",
                   responses.toXML().contains("<tag name"));

        String LOCALHOST = "localhost 2010";
        request.put(DocumentKeys.SEARCH_QUERY, LOCALHOST);
        log.debug("Searching for " + LOCALHOST);
        responses = aggregator.search(request);
        log.debug("Finished searching");
//        System.out.println(responses.toXML());
        assertTrue("The combined result should contain at least one record",
                   responses.toXML().contains("<record score"));
        assertTrue("The combined result should contain at least one tag",
                   responses.toXML().contains("<tag name"));

/*        System.out.println("Records");
        BufferedReader lines =
            new BufferedReader(new StringReader(responses.toXML()));
        String line;
        while ((line = lines.readLine()) != null) {
            if (line.contains("record score")) {
                System.out.println(line);
            }
        }*/
        aggregator.close();
    }

    public void testAggregatorDivider() throws IOException, SubConfigurationsNotSupportedException {
        AdjustingSearcherAggregator aggregator = getAggregator();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo - bar");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        log.debug("Searching");
        ResponseCollection responses = aggregator.search(request);
        log.debug("Finished searching");
//        System.out.println(responses.toXML());
        assertTrue("The combined result should contain at least one record\n" + responses.toXML(),
                   responses.toXML().contains("<record score"));

        aggregator.close();
    }

    public void testTimingAggregation() throws IOException,
                                        SubConfigurationsNotSupportedException {
        AdjustingSearcherAggregator aggregator = getAggregator();
        Request request = new Request(
            DocumentKeys.SEARCH_QUERY, "foo",
            DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        ResponseCollection responses = aggregator.search(request);
        assertUniqueTiming("Aggregated search", responses);
        //System.out.println(timings);
    }

    public void testTimingSingleAggregation() throws IOException, SubConfigurationsNotSupportedException {
        AdjustingSearcherAggregator aggregator = getSingleAggregator();
        Request request = new Request(
            DocumentKeys.SEARCH_QUERY, "foo",
            DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        ResponseCollection responses = aggregator.search(request);
//        System.out.println(responses.getTiming());
        assertUniqueTiming("Aggregated search", responses);
        //System.out.println(timings);
    }

    public void testSingleSearchTiming() throws IOException {
        Configuration conf = Configuration.newMemoryBased(
            AdjustingSearchClient.CONF_RPC_TARGET, "//localhost:57000/sb-searcher",
            InteractionAdjuster.CONF_IDENTIFIER, "sb");
        AdjustingSearchClient asc = new AdjustingSearchClient(conf);
        Request request = new Request(
            DocumentKeys.SEARCH_QUERY, "foo",
            DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        ResponseCollection responses = asc.search(request);
        assertUniqueTiming("Single adjusting search", responses);
    }

    private void assertUniqueTiming(
        String message, ResponseCollection responses) {
        String duplicate = checkDuplicates(responses);
        if (duplicate == null) {
            return;
        }
        fail(message + ". The timing '" + duplicate + "' should not be duplicated in "
             + responses.getTiming().replaceAll("[|]", "\n"));
    }

    /**
     * @param timer a timer to check for duplicates.
     * @return null if not duplicates, else one instance of the duplicates.
     */
    private String checkDuplicates(Timer timer) {
        String[] timings = timer.getTiming().split("[|]");
        Arrays.sort(timings);
        String last = null;
        for (String timing: timings) {
            if (last != null && last.equals(timing)) {
                return timing;
            }
            last = timing;
        }
        return null;
    }

    private AdjustingSearcherAggregator getAggregator() throws IOException {
        Configuration conf = Configuration.newMemoryBased(
            ResponseMerger.CONF_ORDER, "summon, sb",
            ResponseMerger.CONF_MODE,
            ResponseMerger.MODE.interleave.toString());
//            ResponseMerger.CONF_MODE, "score");
        List<Configuration> searcherConfs = conf.createSubConfigurations(
            SummaSearcherAggregator.CONF_SEARCHERS, 2);
        // SB
        Configuration sbConf = searcherConfs.get(0);
        sbConf.set(SummaSearcherAggregator.CONF_SEARCHER_DESIGNATION, "sb");
        sbConf.set(InteractionAdjuster.CONF_IDENTIFIER, "sb");
        sbConf.set(AdjustingSearcherAggregator.CONF_SEARCH_ADJUSTING, true);
        sbConf.set(AdjustingSearchClient.CONF_RPC_TARGET, "//localhost:57000/sb-searcher");
        sbConf.set(InteractionAdjuster.CONF_ADJUST_SCORE_MULTIPLY, 5.0);

        // Summon
        Configuration summonConf = searcherConfs.get(1);
        summonConf.set(SummaSearcherAggregator.CONF_SEARCHER_DESIGNATION, "summon");
        summonConf.set(InteractionAdjuster.CONF_IDENTIFIER, "summon");
        summonConf.set(AdjustingSearcherAggregator.CONF_SEARCH_ADJUSTING, true);
        summonConf.set(AdjustingSearchClient.CONF_RPC_TARGET, "//localhost:57400/summon-searcher");
        summonConf.set(InteractionAdjuster.CONF_ADJUST_SCORE_ADD, -0.5);
        summonConf.set(InteractionAdjuster.CONF_ADJUST_FACET_FIELDS,
                       "author_normalised - Author, lma_long - ContentType, llang - Language, lsubject - SubjectTerms");
        Configuration llang = summonConf.createSubConfigurations(
            InteractionAdjuster.CONF_ADJUST_FACET_TAGS, 1).get(0);
        llang.set(TagAdjuster.CONF_FACET_NAME, "llang");
        llang.set(TagAdjuster.CONF_TAG_MAP, "Spanish - spa");
        // Unmapped: Genre, IsScholary, TemporalSubjectTerms

        log.debug("Creating adjusting aggregator");
        return new AdjustingSearcherAggregator(conf);
    }

    private AdjustingSearcherAggregator getSingleAggregator() throws IOException {
        Configuration conf = Configuration.newMemoryBased(
            ResponseMerger.CONF_ORDER, "sb",
            ResponseMerger.CONF_MODE,
            ResponseMerger.MODE.interleave.toString());
//            ResponseMerger.CONF_MODE, "score");
        List<Configuration> searcherConfs = conf.createSubConfigurations(
            SummaSearcherAggregator.CONF_SEARCHERS, 1);
        // SB
        Configuration sbConf = searcherConfs.get(0);
        sbConf.set(SummaSearcherAggregator.CONF_SEARCHER_DESIGNATION, "sb");
        sbConf.set(InteractionAdjuster.CONF_IDENTIFIER, "sb");
        sbConf.set(AdjustingSearcherAggregator.CONF_SEARCH_ADJUSTING, true);
        sbConf.set(AdjustingSearchClient.CONF_RPC_TARGET, "//localhost:57000/sb-searcher");
        sbConf.set(InteractionAdjuster.CONF_ADJUST_SCORE_MULTIPLY, 5.0);

        log.debug("Creating adjusting aggregator");
        return new AdjustingSearcherAggregator(conf);
    }
}
