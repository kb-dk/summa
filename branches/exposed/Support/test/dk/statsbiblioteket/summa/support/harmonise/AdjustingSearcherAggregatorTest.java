package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.summa.search.SummaSearcherAggregator;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.support.summon.search.SummonSearchNode;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
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
    public void testAggregator() throws IOException {
        Configuration conf = Configuration.newMemoryBased(
            ResponseMerger.CONF_ORDER, "summon, sb",
            ResponseMerger.CONF_MODE,
            ResponseMerger.MERGE_MODE.concatenate.toString());
//            ResponseMerger.CONF_MODE, "score");
        List<Configuration> searcherConfs = conf.createSubConfigurations(
            SummaSearcherAggregator.CONF_SEARCHERS, 2);
        // SB
        Configuration sbConf = searcherConfs.get(0);
        sbConf.set(SummaSearcherAggregator.CONF_SEARCHER_DESIGNATION, "sb");
        sbConf.set(InteractionAdjuster.CONF_IDENTIFIER, "sb");
        sbConf.set(AdjustingSearcherAggregator.CONF_SEARCH_ADJUSTING, true);
        sbConf.set(AdjustingSearchClient.CONF_RPC_TARGET,
                   "//localhost:55000/sb-searcher");
        sbConf.set(InteractionAdjuster.CONF_ADJUST_SCORE_MULTIPLY, 5.0);
        
        // Summon
        Configuration summonConf = searcherConfs.get(1);
        summonConf.set(SummaSearcherAggregator.CONF_SEARCHER_DESIGNATION,
                       "summon");
        summonConf.set(InteractionAdjuster.CONF_IDENTIFIER, "summon");
        summonConf.set(AdjustingSearcherAggregator.CONF_SEARCH_ADJUSTING, true);
        summonConf.set(AdjustingSearchClient.CONF_RPC_TARGET,
                   "//localhost:55400/summon-searcher");
        summonConf.set(InteractionAdjuster.CONF_ADJUST_SCORE_ADD, -0.5);

        log.debug("Creating adjusting aggregator");
        AdjustingSearcherAggregator aggregator =
            new AdjustingSearcherAggregator(conf);

        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        log.debug("Searching");
        ResponseCollection responses = aggregator.search(request);
        log.debug("Finished searching");
//        System.out.println(responses.toXML());

        System.out.println("Records");
        BufferedReader lines =
            new BufferedReader(new StringReader(responses.toXML()));
        String line;
        while ((line = lines.readLine()) != null) {
            if (line.contains("record score")) {
                System.out.println(line);
            }
        }
        aggregator.close();
    }

}
