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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.support.harmonise.AdjustingSearchNode;
import dk.statsbiblioteket.summa.support.harmonise.InteractionAdjuster;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.rmi.RemoteException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(SummonSearchNodeTest.class);

    public SummonSearchNodeTest(String name) {
        super(name);
    }

    private static final File SECRET =
        new File(System.getProperty("user.home") + "/summon-credentials.dat");
    private String id;
    private String key;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!SECRET.exists()) {
            throw new IllegalStateException(
                "The file '" + SECRET.getAbsolutePath() + "' must exist and "
                + "contain two lines, the first being access ID, the second"
                + "being access key for the Summon API");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(
            new FileInputStream(SECRET), "utf-8"));
        id = br.readLine();
        key = br.readLine();
        br.close();
        log.debug("Loaded credentials from " + SECRET);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SummonSearchNodeTest.class);
    }

    public void testBasicSearch() throws RemoteException {
        Configuration conf = Configuration.newMemoryBased(
            SummonSearchNode.CONF_SUMMON_ACCESSID, id,
            SummonSearchNode.CONF_SUMMON_ACCESSKEY, key
            //SummonSearchNode.CONF_SUMMON_FACETS, ""
        );

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
        System.out.println(responses.toXML());
        assertTrue("The result should contain at least one record",
                   responses.toXML().contains("<record score"));
        assertTrue("The result should contain at least one tag",
                   responses.toXML().contains("<tag name"));

    }

    public void testAdjustingSearcher() throws IOException {
        Configuration conf = Configuration.newMemoryBased(
            InteractionAdjuster.CONF_IDENTIFIER, "summon",
            InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "recordID - ID");
        Configuration inner = conf.createSubConfiguration(
            AdjustingSearchNode.CONF_INNER_SEARCHNODE);
        inner.set(SearchNodeFactory.CONF_NODE_CLASS,
                  SummonSearchNode.class.getCanonicalName());
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSID, id);
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSKEY, key);

        log.debug("Creating adjusting SummonSearchNode");
        AdjustingSearchNode adjusting = new AdjustingSearchNode(conf);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        adjusting.search(request, responses);
        log.debug("Finished searching");
        System.out.println(responses.toXML());
    }
}
