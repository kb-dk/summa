/* $Id$
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
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.ingest.source.RecordGenerator;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.StringWriter;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 *
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FagrefGeneratorTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(FagrefGeneratorTest.class);

    public FagrefGeneratorTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ReleaseTestCommon.setup();
        if (SearchTest.INDEX_ROOT.exists()) {
            Files.delete(SearchTest.INDEX_ROOT);
            SearchTest.INDEX_ROOT.mkdirs();
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
//        ReleaseTestCommon.tearDown();
    }

    public static Test suite() {
        return new TestSuite(FagrefGeneratorTest.class);
    }

    public void testFagrefTemplate() throws Exception {
        RecordGenerator generator = createGenerator();
        System.out.println(generator.next().getRecord().getContentAsUTF8());
    }

    public void testPerformance() throws Exception {
        int RUNS = 10000;
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(100);
        profiler.setExpectedTotal(RUNS);
        RecordGenerator generator = createGenerator();
        for (int i = 0; i < RUNS ; i++) {
            generator.next();
            profiler.beat();
        }
        System.out.println(String.format(
                "Generated %d fagref test records in %s with an average speed " 
                + "of %s records/second",
                RUNS, profiler.getSpendTime(), profiler.getBps()));
    }

    private RecordGenerator createGenerator() {
        Configuration conf = Configuration.newMemoryBased();
        conf.set("firstname", new ArrayList<String>(
                Arrays.asList("Hans", "Jens", "Ole", "Jesper", "Kaj", "Søren",
                              "Benny", "Børge", "M'Bala", "Mikkel", "Thomas")));
        conf.set("surname", new ArrayList<String>(
                Arrays.asList("Jensen", "Hansen", "Sørensen", "Karlsson",
                              "Primbulus", "Sølvtromme", "Kobberhammer",
                              "Guldskovl", "de Trix", "And")));
        conf.set("areaOfExpertise", new ArrayList<String>(
                Arrays.asList("Guld", "Biologi", "Omnilogi", "Østindien",
                              "Vestpakistan", "USA", "Æbler", "Blommer",
                              "Pærer", "Datalogi", "Astrofysik", "Astrologi",
                              "Astronomi", "Tryllebær", "Tegneserier",
                              "Palæontologi", "Drømme", "Kaslafniansk")));
        conf.set(RecordGenerator.CONF_CONTENT_TEMPLATE_LOCATION,
                 "data/generator/fagref_template.xml");
        return new RecordGenerator(conf);
    }

    public void testIngest() throws Exception {
        final int RECORDS = 5000;
        // Quick test on pc286 (desktop 7200 RPM hard disk)
        //   5000: 141/s
        //  10000: 127/s
        //  25000:  88/s
        //  50000:  65/s
        //  75000:  36/s (while programming in the background)
        // 100000:  32/s

        Profiler storageProfiler = new Profiler();
        StorageService storage = OAITest.getStorageService();
        log.info("Finished starting Storage in "
                 + storageProfiler.getSpendTime());
        Configuration conf = Configuration.load(
                "data/generator/generator_configuration.xml");
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
                set(RecordGenerator.CONF_RECORDS, RECORDS);
        FilterService ingestService = new FilterService(conf);
        try {
            ingestService.start();
        } catch (Exception e) {
            throw new RuntimeException("Got exception while ingesting", e);
        }
        while (ingestService.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest of fagref ½ a second");
            Thread.sleep(500);
        }
        ingestService.stop();
        log.debug("Finished ingesting, doing index");

        Profiler indexProfiler = new Profiler() {
            @Override
            public String toString() {
                StringWriter sw = new StringWriter(500);
                sw.append("Beats: ").append(Long.toString(getBeats()));
                sw.append(", time: ").append(getSpendTime());
                sw.append(", average: ").
                        append(Double.toString(getBps(false)));
                sw.append(" beats/second, ETA: ").
                        append(getETAAsString(false));
                return sw.toString();
            }

        };
        indexProfiler.setExpectedTotal(RECORDS);
        Configuration indexConf = Configuration.load(
                "data/search/FacetTest_IndexConfiguration.xml");
        Configuration facetConf =
                indexConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(4).
                getSubConfigurations(IndexControllerImpl.CONF_MANIPULATORS).
                get(1);
        extendFacets(facetConf, "subject", Arrays.asList("lsu_oai"));
        IndexTest.updateIndex(indexConf);
        indexProfiler.setBeats(RECORDS);
        log.info("Finished requesting and indexing " + RECORDS + " Records. "
                 + indexProfiler);

        storage.stop();
    }

    private void extendFacets(Configuration facetConf, String name,
                              List<String> fields) throws IOException {
        List<Configuration> facets =
                facetConf.getSubConfigurations(Structure.CONF_FACETS);
        List<Configuration> newFacets =
                facetConf.createSubConfigurations(
                        Structure.CONF_FACETS, facets.size() + 1);
        for (int i = 0 ; i < facets.size() ; i++) {
            newFacets.get(i).set(
                    FacetStructure.CONF_FACET_NAME,
                    facets.get(i).get(FacetStructure.CONF_FACET_NAME));
            newFacets.get(i).set(
                    FacetStructure.CONF_FACET_FIELDS,
                    facets.get(i).get(FacetStructure.CONF_FACET_FIELDS));
        }
        newFacets.get(newFacets.size() - 1).set(
                FacetStructure.CONF_FACET_NAME, name);
        newFacets.get(newFacets.size() - 1).setStrings(
                FacetStructure.CONF_FACET_FIELDS , fields);
    }
}
