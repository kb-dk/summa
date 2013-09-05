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

import com.ibm.icu.impl.duration.impl.Utils;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ManyToManyMapper;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrRequestAdjusterTest extends TestCase {

    private SolrRequestAdjuster adjuster = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ManyToManyMapper fieldMap = new ManyToManyMapper(Arrays.asList("a - b", "c - d,e", "f - g,h", "i,j - k,l"));
        adjuster = new SolrRequestAdjuster(Configuration.newMemoryBased(), fieldMap, null);
    }

    public void testBasicMapping() {
        SolrParams mapped = map(createParams(
                CommonParams.FIELD, "a"
        ));
        SolrParams expected = createParams(
                CommonParams.FIELD, "b"
        );
        assertEquals("The field should be mapped correctly", expected, mapped);
    }

    private SolrParams map(ModifiableSolrParams params) {
        return adjuster.adjust(params);
    }

    private ModifiableSolrParams createParams(String... vars) {
        if (vars.length %2 != 0) {
            throw new IllegalArgumentException("There must be an even number of arguments. There were " + vars.length);
        }
        ModifiableSolrParams params = new ModifiableSolrParams();
        for (int i = 0 ; i < vars.length ; i+=2) {
            params.set(vars[i], vars[i+1]);
        }
        return params;
    }

    public void assertEquals(String message, SolrParams expected, SolrParams actual) {
        List<String> expectedKeys = getSortedKeys(expected);
        List<String> actualKeys = getSortedKeys(actual);
        if (expectedKeys.size() != actualKeys.size()) {
            //List<String> missing =
        }
    }

    private List<String> getSortedKeys(SolrParams params) {
        List<String> result = new ArrayList<String>();
        Iterator<String> paramKeys = params.getParameterNamesIterator();
        while (paramKeys.hasNext()) {
            result.add(paramKeys.next());
        }
        Collections.sort(result);
        return result;
    }
}
