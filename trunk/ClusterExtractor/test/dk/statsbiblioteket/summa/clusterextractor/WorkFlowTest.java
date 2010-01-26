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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import junit.framework.TestCase;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Top level work flow test.
 *
 * The work flow for extracting clusters looks something like this:
 * • For each index machine:
 *   – run the ClusterBuilder.buildCentroids method
 *   – copy the new local centroid data structure to a central location.
 * • Central: Call the ClusterMerger.mergeCentroidSets method.
 * • For each index machine:
 *   – copy the new full centroid datastructure to this machine
 *   – call the local ClusterProvider update method
 */
public class WorkFlowTest extends TestCase {
    Configuration confBuilder1;
    Configuration confBuilder2;

    ClusterBuilder builder1;
    ClusterBuilder builder2;
    ClusterMerger merger;
    ClusterProvider provider1;
    ClusterProvider provider2;

    Document doc;

    /**
     * Set up two cluster builders, one merger and to providers and create test doc.
     */
    protected void setUp() throws Exception {
        super.setUp();
        //Set up two cluster builders, one merger and to providers.
        String confFileName = "clusterextractor.clusterbuilder.config.xml";
        confBuilder1 = new Configuration(new FileStorage(confFileName));
        builder1 = new ClusterBuilderImpl(confBuilder1);

        confFileName = "clusterextractor.clusterbuilder2.config.xml";
        confBuilder2 = new Configuration(new FileStorage(confFileName));
        builder2 = new ClusterBuilderImpl(confBuilder2);

        confFileName = "clusterextractor.clustermerger.config.xml";
        Configuration conf = new Configuration(new FileStorage(confFileName));
        merger = new ClusterMergerImpl(conf);

        confFileName = "clusterextractor.clusterprovider.config.xml";
        conf = new Configuration(new FileStorage(confFileName));
        conf.importConfiguration(confBuilder1);
        provider1 = new ClusterProviderImpl(conf);

        confFileName = "clusterextractor.clusterprovider2.config.xml";
        conf = new Configuration(new FileStorage(confFileName));
        conf.importConfiguration(confBuilder2);
        provider2 = new ClusterProviderImpl(conf);

        //register merger with builders and providers???

        //create test document
        doc = new Document();
        doc.add(new Field("title", "valentino", Field.Store.NO, Field.Index.TOKENIZED));
        doc.add(new Field("lsu_oai", "venskab", Field.Store.NO, Field.Index.TOKENIZED));
    }

    /**
     * WorkFlowTest tearDown not used.
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testWorkFlow() {
        builder1.buildCentroids();
        builder2.buildCentroids();
        //TODO: move
        merger.mergeCentroidSets();
        //TODO: move 

        provider1.reload();
        Document doc1 = provider1.enrich(doc);

        provider2.reload();
        Document doc2 = provider2.enrich(doc);

        assertEquals("Enriching a document using different providers should "
                + "yield the same result.", doc1, doc2);
    }
}




