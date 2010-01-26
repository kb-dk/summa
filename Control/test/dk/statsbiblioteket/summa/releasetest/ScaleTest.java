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
package dk.statsbiblioteket.summa.releasetest;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
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

    public void testBuild1() throws Exception {
        testBuild(1);
    }
    public void testBuild20() throws Exception {
        testBuild(20);
    }
    public void testBuild1000() throws Exception {
        testBuild(1000);
    }
    public void testBuild10000() throws Exception {
        testBuild(10000);
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
        String xsltLocation =
                Resolver.getURL("data/scale/xslt/fagref_index.xsl").getFile();
        filters.get(1).set(XMLTransformer.CONF_XSLT, xsltLocation);

        // OldToNew
        String legacyLocation =
                Resolver.getURL("LegacyToSummaDocumentXML.xslt").getFile();
        filters.get(2).set(XMLTransformer.CONF_XSLT, legacyLocation);

        // Document creator
        String descriptorLocation = Resolver.getURL(
                "data/scale/scale_index_descriptor.xml").getFile();
        log.debug("Descriptor location: " + descriptorLocation);
        filters.get(3).getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, descriptorLocation);

        // Index - general
        filters.get(4).set(IndexControllerImpl.CONF_INDEX_ROOT_LOCATION,
                           ReleaseTestCommon.PERSISTENT_FOLDER);

        // Index - Lucene
        filters.get(4).getSubConfigurations(
                IndexControllerImpl.CONF_MANIPULATORS).get(0).
                getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).set(
                IndexDescriptor.CONF_ABSOLUTE_LOCATION, descriptorLocation);

        return conf;
    }
}

