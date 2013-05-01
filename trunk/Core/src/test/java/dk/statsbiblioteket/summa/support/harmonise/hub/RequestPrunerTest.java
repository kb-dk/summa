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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RequestPrunerTest extends TestCase {
    private static Log log = LogFactory.getLog(RequestPrunerTest.class);

    public RequestPrunerTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testBlank() throws Exception {
        SolrParams out = getPruned(Configuration.newMemoryBased());
        assertFalse("There should be no parameters after pruning", out.getParameterNamesIterator().hasNext());
    }

    public void testSingle() throws Exception {
        ArrayList<String> whitelist = new ArrayList<String>(Arrays.asList("foo=.+"));
        List<String> pruned = getPrunedPairs(Configuration.newMemoryBased(
                RequestPruner.CONF_WHITELIST, whitelist
        ));
        assertPruned(pruned, "foo=bar");
    }

    public void testValueMatchInList() throws Exception {
        ArrayList<String> whitelist = new ArrayList<String>(Arrays.asList("zoo=.+o"));
        List<String> pruned = getPrunedPairs(Configuration.newMemoryBased(
                RequestPruner.CONF_WHITELIST, whitelist
        ));
        assertPruned(pruned, "zoo=moo");
    }

    public void testTwoMatchers() throws Exception {
        ArrayList<String> whitelist = new ArrayList<String>(Arrays.asList("foo=.+", "zoo=.+"));
        List<String> pruned = getPrunedPairs(Configuration.newMemoryBased(
                RequestPruner.CONF_WHITELIST, whitelist
        ));
        assertPruned(pruned, "foo=bar", "zoo=baz", "zoo=moo");
    }

    public void testTwoMatchersOneBlacklist() throws Exception {
        ArrayList<String> whitelist = new ArrayList<String>(Arrays.asList("foo=.+", "zoo=.+"));
        ArrayList<String> blacklist = new ArrayList<String>(Arrays.asList("zoo=b.+"));
        List<String> pruned = getPrunedPairs(Configuration.newMemoryBased(
                RequestPruner.CONF_WHITELIST, whitelist,
                RequestPruner.CONF_BLACKLIST, blacklist
        ));
        assertPruned(pruned, "foo=bar", "zoo=moo");
    }

    private void assertPruned(List<String> pruned, String... expected) {
        final String result = Strings.join(pruned);
        assertEquals("There should be the right number of pruned parameters in [" + result + "]",
                     expected.length, pruned.size());
        for (String e: expected) {
            assertTrue("The result [" + result + "] should contain the pair " + e, pruned.contains(e));
        }
    }

    private List<String> getPrunedPairs(Configuration conf) {
        List<String> pairs = new ArrayList<String>();
        SolrParams pruned = getPruned(conf);
        Iterator<String> keys = pruned.getParameterNamesIterator();
        while (keys.hasNext()) {
            String key = keys.next();
            for (String value: pruned.getParams(key)) {
                pairs.add(key + "=" + value);
            }
        }
        return pairs;
    }

    private SolrParams getPruned(Configuration conf) {
        RequestPruner pruner = new RequestPruner(conf);
        pruner.addComponent(new DummyLeaf(Configuration.newMemoryBased()));
        return pruner.adjustRequest(getInput());
    }

    private ModifiableSolrParams getInput() {
        ModifiableSolrParams input = new ModifiableSolrParams();
        input.set("foo", "bar");
        input.add("zoo", "baz");
        input.add("zoo", "moo");
        return input;
    }


}
