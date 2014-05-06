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
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.ComponentCallable;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponent;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryReducerTest extends TestCase {
    public QueryReducerTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testDefaultFullMatch() throws IOException {
        QueryReducer reducer = getDefaultReducer();
        assertQuery("The query 'foo:bar' should be reduced to none", reducer, "MySearcher",
                    "", "foo:bar");
    }

    public void testNamedFullMatch() throws IOException {
        QueryReducer reducer = getNamedReducer("MySearcher");
        assertQuery("The query 'foo:bar' should be reduced to none", reducer, "MySearcher",
                    "", "foo:bar");
    }

    public void testNamedFullMatchNoNameMatch() throws IOException {
        QueryReducer reducer = getNamedReducer("MySearcher");
        assertQuery("The query 'foo:bar' should not be reduced as the componentID does not match", reducer,
                    "AnotherSearcher", "foo:bar", "foo:bar");
    }

    public void testDefaultField() throws IOException {
        QueryReducer reducer = getDefaultReducer();
        assertQuery("The query 'myfield:whatever' should be reduced to none", reducer, "MySearcher",
                    "", "myfield:whatever");
    }

    public void testBooleanDefaultField() throws IOException {
        assertDefault("", "cheese:ham AND myfield:whatever");
    }

    public void testBooleanNotDefaultField() throws IOException {
        assertDefault("cheese:ham", "cheese:ham NOT myfield:whatever");
        assertDefault("cheese:ham", "cheese:ham AND NOT myfield:whatever");
    }

    public void testNotDefaultField() throws IOException {
        final String QUERY = "NOT myfield:whatever";
        QueryReducer reducer = getDefaultReducer();
        assertQuery("The query '" + QUERY + "' should be reduced to none", reducer, "MySearcher",
                    "", QUERY);
    }

    public void testEmptyIn() throws IOException {
        QueryReducer reducer = getDefaultReducer();
        assertQuery("The empty query should be removed", reducer, "MySearcher",
                    null, "");
    }

    public void testEmptyOut() throws IOException {
        QueryReducer reducer = getDefaultReducer();
        assertQuery("The query 'myfield:whatever' should be reduced to none", reducer, "MySearcher",
                    "", "myfield:whatever");
    }

    public void testDefaultTextMatch() throws IOException {
        QueryReducer reducer = getDefaultReducer();
        assertQuery("The query 'whatever:myterm' should be reduced to none", reducer, "MySearcher",
                    "", "whatever:myterm");
    }

    public void testDefaultNoMatch() throws IOException {
        QueryReducer reducer = getDefaultReducer();
        assertQuery("The query 'kaos:king' should not be reduced", reducer, "MySearcher",
                    "kaos:king", "kaos:king");
    }

    public void testExplicitAnd() throws IOException {
        assertDefault("", "foo:bar AND whatever");
    }

    public void testImplicitAnd() throws IOException {
        assertDefault("", "foo:bar whatever");
    }

    public void testOr() throws IOException {
        assertDefault("whatever", "foo:bar OR whatever");
    }

    public void testRange() throws IOException {
        assertDefault("", "myfield:[a TO B]");
    }

    public void testPhrase() throws IOException {
        assertDefault("", "myfield:\"a b\"");
    }

    public void testPhrase2() throws IOException {
        assertDefault("", "whatever:\"myterm\"");
    }

    public void testPrefixField() throws IOException {
        // Should never match on the field
        assertDefault("", "myfield:a*");
    }

    public void testPrefixPair() throws IOException {
        // Might match foo:barista
        assertDefault("foo:bar*", "foo:bar*");
    }

    public void testPrefixText() throws IOException {
        // Might match myterminal
        assertDefault("myterm*", "myterm*");
    }

    public void testFuzzyField() throws IOException {
        // Fields are not fuzzy
        assertDefault("", "myfield:a~");
    }

    public void testFuzzyPair() throws IOException {
        // Fuzzy might match something other than bar
        assertDefault("foo:bar~2", "foo:bar~");
    }

    public void testFuzzyText() throws IOException {
        assertDefault("myterm~2", "myterm~");
    }

    public void testSubWeight() throws IOException {
        assertDefault("moo^7.1898894",
                      "(moo^7.1898894 OR foo:bar^7.1898894)");
    }

    public void testSubORQuery() throws IOException {
        // QueryRewriter removes '+'
        assertDefault("(hello my:world phrase:\"mongo pongo\") (something)",
                      "+(hello my:world phrase:\"mongo pongo\") +(something OR foo:bar)");
    }

    public void testSubANDQuery() throws IOException {
        assertDefault("",
                      "+(hello my:world phrase:\"mongo pongo\") +(something AND foo:bar)");
    }

    private void assertDefault(String expected, String query) throws IOException {
        QueryReducer reducer = getDefaultReducer();
        assertQuery("The query '" + query + "' should be reduced to '" + expected + "'", reducer, "MySearcher",
                    expected, query);
    }

    private QueryReducer getDefaultReducer() throws IOException {
        return getNamedReducer(""); // "" == match all componentIDs
    }

    private QueryReducer getNamedReducer(String componentID) throws IOException {
        Configuration reducerConf = Configuration.newMemoryBased(QueryRewriter.CONF_QUOTE_TERMS, false);
        Configuration defaultConf = reducerConf.createSubConfiguration(QueryReducer.CONF_TARGETS);
        defaultConf.set(QueryReducer.ReducerTarget.CONF_COMPONENT_ID, componentID);
        defaultConf.set(QueryReducer.ReducerTarget.CONF_MATCH_NONES,
                        new ArrayList<>(Arrays.asList("foo:bar", ":myterm", "myfield:")));
        return new QueryReducer(reducerConf);
    }

    private void assertQuery(String message, QueryReducer reducer, String componentID, String expected, String query) {
        ComponentCallable componentCallable = createCC(componentID);
        componentCallable.getParams().set(CommonParams.Q, query);
        List<ComponentCallable> ccs = Arrays.asList(componentCallable);
        reducer.adjustRequests(new SolrQuery(), ccs);
        assertEquals(message, expected, componentCallable.getParams().get(CommonParams.Q));

        // TODO: Also check filter(s)
    }

    private ComponentCallable createCC(final String componentID) {
        return new ComponentCallable(new HubComponent() {
            @Override
            public String getID() {
                return componentID;
            }

            @Override
            public List<String> getBases() {
                throw new UnsupportedOperationException("Not implemented for test");
            }

            @Override
            public MODE getMode() {
                throw new UnsupportedOperationException("Not implemented for test");
            }

            @Override
            public boolean limitOK(Limit limit) {
                return true;
            }

            @Override
            public QueryResponse search(Limit limit, SolrParams params) throws Exception {
                throw new UnsupportedOperationException("Not implemented for test");
            }
        }, null, new ModifiableSolrParams());
    }
}
