/* $Id: TikaTest.java 1674 2009-08-15 01:07:00Z toke-sb $
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
package dk.statsbiblioteket.summa.support.arc;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.filter.object.DumpFilter;
import dk.statsbiblioteket.summa.common.filter.object.MUXFilter;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.List;
import java.io.File;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TikaTest extends TestCase {
    private static Log log = LogFactory.getLog(TikaTest.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (TEST_ROOT.exists()) {
            Files.delete(TEST_ROOT);
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(TikaTest.class);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final File TEST_ROOT = new File(new File(System.getProperty(
            "java.io.tmpdir")), "tikatest");

    public void testDumpTikaResult() throws Exception {
        Configuration conf = Configuration.load("data/TikaTestChain.xml");
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
                set(FileReader.CONF_ROOT_FOLDER, 
                    Resolver.getFile("data/arc").toString());
        FilterControl control = new FilterControl(conf);
        control.start();
        control.waitForFinish(100000);
    }

    public void testTikaDocumentCreator() throws Exception {
        Profiler profiler = new Profiler();
        Configuration conf = Configuration.load("data/TikaDocumentChain.xml");
        List<Configuration> filterConfs =
                conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS);
        filterConfs.get(0).set(FileReader.CONF_ROOT_FOLDER,
//                               "/home/te/tmp/arc");
        // TODO: Change back
                               Resolver.getFile("data/arc").toString());

        List<Configuration> innerConfs = filterConfs.get(1).
                getSubConfigurations(MUXFilter.CONF_FILTERS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS);
        String descriptorLocation = "data/tika/TikaTest_IndexDescriptor.xml";
        for (int filterPos: new int[]{1, 2}) {
            assertTrue("An inner descriptor location should be present for "
                       + filterPos, innerConfs.get(filterPos).
                    getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                    valueExists(IndexDescriptor.CONF_ABSOLUTE_LOCATION));
            innerConfs.get(filterPos).
                    getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).set(
                    IndexDescriptor.CONF_ABSOLUTE_LOCATION, descriptorLocation);
        }

        assertTrue("A descriptor location should be present for the indexer",
                   filterConfs.get(2).getSubConfiguration(
                           IndexDescriptor.CONF_DESCRIPTOR).valueExists(
                           IndexDescriptor.CONF_ABSOLUTE_LOCATION));
        filterConfs.get(2).
                getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).set(
                IndexDescriptor.CONF_ABSOLUTE_LOCATION, descriptorLocation);


/*        filterConfs.get(5).set(
                DumpFilter.CONF_OUTPUTFOLDER, TEST_ROOT.getAbsolutePath()
                                              + "/d2");*/
        assertTrue("An absolute path for the index should exist",
                   filterConfs.get(2).valueExists(
                           IndexControllerImpl.CONF_INDEX_ROOT_LOCATION));
        filterConfs.get(2).set(IndexControllerImpl.CONF_INDEX_ROOT_LOCATION,
                               TEST_ROOT.getAbsolutePath());

        FilterControl control = new FilterControl(conf);
        control.start();
        control.waitForFinish(100000);
        System.out.println("Total time used: " + profiler.getSpendTime());
    }
}
