/* $Id: ClusterProviderImplTest.java,v 1.2 2007/12/04 10:26:43 bam Exp $
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



