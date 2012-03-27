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
package dk.statsbiblioteket.summa.support.didyoumean;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.support.api.DidYouMeanKeys;
import dk.statsbiblioteket.summa.support.api.DidYouMeanResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DidYouMeanSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(DidYouMeanSearchNodeTest.class);

    public DidYouMeanSearchNodeTest(String name) {
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
        return new TestSuite(DidYouMeanSearchNodeTest.class);
    }

    /*
    This test requires a pre-build DidYouMean-index.
    TODO: Create a test index, then create a DidYoumean-index from it
     */
    public static final String DIDYOUMEAN_INDEX =
        "/home/teg/workspace/summarise-trunk/sites/sb/didyoumean/sb/";
    public static final String SUMMA_INDEX =
        "/home/teg/workspace/testindex/20111021-083932";
    public void testExistingIndex() throws IOException {
        if (!new File(DIDYOUMEAN_INDEX).exists()) {
            fail("There must be a pre-generated DidYouMean index at "
                 + DIDYOUMEAN_INDEX);
        }
        if (!new File(SUMMA_INDEX).exists()) {
            fail("There must be a pre-generated Summa index at " + SUMMA_INDEX);
        }

        Configuration conf = Configuration.newMemoryBased(
            DidYouMeanSearchNode.CONF_DIDYOUMEAN_LOCATION, DIDYOUMEAN_INDEX,
            DidYouMeanSearchNode.CONF_DIDYOMEAN_CLOSE_ON_NON_EXISTING_INDEX,
            false
        );
        PublicDidYouMean didYouMean = new PublicDidYouMean(conf);
        log.debug("Calling managed open for DidYouMean");
        didYouMean.managedOpen(SUMMA_INDEX);
        Request request = new Request(
            DidYouMeanKeys.SEARCH_QUERY, "fo"
        );
        ResponseCollection responses = new ResponseCollection();
        didYouMean.managedSearch(request, responses);
        assertEquals("DidYouMean should return the expected response count",
                     1, responses.size());
        Response response = responses.iterator().next();
        assertTrue("The response should be a DidYouMeanResponse but was "
                   + response.getClass(),
                   response instanceof DidYouMeanResponse);
        DidYouMeanResponse dymResponse = (DidYouMeanResponse)response;
        assertTrue("DidYouMean should contain at least 1 suggestion",
                   dymResponse.getResultTuples().size() > 0);
        didYouMean.close();
    }

    //Need 1 unit-test or unittest will fail
    public void testNothing(){
    	assertTrue(true);
    	
    }
    
    
    private static class PublicDidYouMean extends DidYouMeanSearchNode {
        public PublicDidYouMean(Configuration config) throws IOException {
            super(config);
        }
        @Override
        public void managedSearch(Request request, ResponseCollection responses)
                                                        throws RemoteException {
            super.managedSearch(request, responses);
        }
    }
}
