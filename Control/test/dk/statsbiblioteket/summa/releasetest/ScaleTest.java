/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.releasetest;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.ingest.source.RecordGenerator;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.control.service.FilterService;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.List;
import java.io.File;

/**
 * Updates an index with pseudo-random Records, in order to test scalability in
 * index building and searching. No Storage is used in this process.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ScaleTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(ScaleTest.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
//        ReleaseTestCommon.setup();
/*        if (SearchTest.INDEX_ROOT.exists()) {
            Files.delete(SearchTest.INDEX_ROOT);
            SearchTest.INDEX_ROOT.mkdirs();
        }*/
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
//        ReleaseTestCommon.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ScaleTest.class);
    }

    public void testBuild1000() throws Exception {
        testBuild(100000);
    }
    public void testBuild(int records) throws Exception {
        Profiler profiler = new Profiler();
        Configuration conf = getConfiguration();
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
                set(RecordGenerator.CONF_RECORDS, records);

        FilterService indexService = new FilterService(conf);
        indexService.start();
        IndexTest.waitForService(indexService, Integer.MAX_VALUE);
        indexService.stop(); // Just to make sure
        File indexLocation = ((IndexControllerImpl)indexService.
                getFilterControl().getPumps().get(0).getFilters().get(4)).
                getIndexLocation();
        log.debug("Finished indexing " + records + " to " + indexLocation 
                  + " in " + profiler.getSpendTime());
    }

    private Configuration getConfiguration() throws Exception {
        Configuration conf =
                Configuration.load("data/scale/scale_configuration.xml");
        List<Configuration> filters =
                conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS);

        // Generator
        filters.get(0).set(RecordGenerator.CONF_CONTENT_TEMPLATE_LOCATION,
                           Resolver.getFile("data/scale/fagref_template.xml").
                                   getAbsolutePath());

        // Fagref-XSLT
        String xsltLocation = Resolver.getURL("data/fagref/fagref_index.xsl").
                getFile();
        filters.get(1).set(XMLTransformer.CONF_XSLT, xsltLocation);

        // OldToNew

        // Document creator
        String descriptorLocation = Resolver.getURL(
                "data/scale/scale_index_descriptor.xml").getFile();
        log.debug("Descriptor location: " + descriptorLocation);
        filters.get(3).getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, descriptorLocation);

        // Index - general
        filters.get(4).set(IndexControllerImpl.CONF_INDEX_ROOT_LOCATION,
                           ReleaseTestCommon.PERSISTENT_FOLDER);

        // Index - Lucene
        filters.get(4).getSubConfigurations(
                IndexControllerImpl.CONF_MANIPULATORS).get(0).
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).set(
                IndexDescriptor.CONF_ABSOLUTE_LOCATION, descriptorLocation);

        return conf;
    }
}
