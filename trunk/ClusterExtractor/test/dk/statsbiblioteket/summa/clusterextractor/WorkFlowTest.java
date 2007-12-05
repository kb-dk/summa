/* $Id: WorkFlowTest.java,v 1.2 2007/12/04 10:26:43 bam Exp $
 * $Revision: 1.2 $
 * $Date: 2007/12/04 10:26:43 $
 * $Author: bam $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
 *   – run the ClusterBuilder.buildVocabulary method
 *   – move the created vocabulary data structure to a central location
 *   (defined in the properties).
 * • Central: Call the ClusterMerger.mergeVocabularies method.
 * • For each index machine:
 *   – copy the new full vocabulary to this machine
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

        //register merger with builders and providers
        builder1.registerMerger(merger);
        builder2.registerMerger(merger);
        provider1.registerMerger(merger);
        provider2.registerMerger(merger);
        
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
        builder1.buildVocabulary();
        builder2.buildVocabulary();
        //TODO: move vocab
        
        merger.mergeVocabularies();
        //TODO: move vocab

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
