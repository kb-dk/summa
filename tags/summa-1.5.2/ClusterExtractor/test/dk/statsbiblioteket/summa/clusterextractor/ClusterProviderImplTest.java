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
package dk.statsbiblioteket.summa.clusterextractor;

import dk.statsbiblioteket.summa.clusterextractor.math.SparseVector;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * ClusterProviderImpl Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterProviderImplTest extends TestCase {
    Configuration conf;
    Document doc;

    public ClusterProviderImplTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        //get test configurations
        String builderConfFileName = "clusterextractor.clusterbuilder.config.xml";
        conf = new Configuration(new FileStorage(builderConfFileName));
        String mergerConfFileName = "clusterextractor.clustermerger.config.xml";
        Configuration mergerConf = new Configuration(new FileStorage(mergerConfFileName));
        conf.importConfiguration(mergerConf);

        //create test document
        doc = new Document();
        doc.add(new Field("title", "valentino", Field.Store.NO, Field.Index.TOKENIZED));
        doc.add(new Field("lsu_oai", "venskab", Field.Store.NO, Field.Index.TOKENIZED));
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetVector() throws Exception {
        ClusterProviderImpl provider = new ClusterProviderImpl(conf);
        SparseVector vec = provider.getVector(doc);
        System.out.println("vec = " + vec);
    }
    public void testEnrich() throws Exception {
        //Assumes clusters build and merged into dendrogram
        //Construct provider and test enrich
        ClusterProvider provider = new ClusterProviderImpl(conf);

        doc = provider.enrich(doc);
        System.out.println("doc = " + doc);

        //TODO: we need some nice Document examples for testing
        //20071027: Come to think of it, how about building the vocabulary
        //first and then using the vocabulary to generate good test documents
    }

    public static Test suite() {
        return new TestSuite(ClusterProviderImplTest.class);
    }
}




