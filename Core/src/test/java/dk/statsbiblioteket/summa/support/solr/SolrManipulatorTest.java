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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrManipulatorTest extends TestCase {
    private static Log log = LogFactory.getLog(SolrManipulatorTest.class);


    private final String solrWarPath="../SBSolr/target/summa-sbsolr-1.8.0-20120502-trunk-SNAPSHOT.war";
   	public final String solrHome= "src/test/tomcat/solr"; //data-dir (index) will be created here.
   	public final String context="/solr";
   	public final int port = 8983;

    //private EmbeddedJettyWithSolrServer server=  new EmbeddedJettyWithSolrServer(solrHome,solrWarPath,context,port);


    public SolrManipulatorTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("basedir", ".");

    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SolrManipulatorTest.class);
    }


}
