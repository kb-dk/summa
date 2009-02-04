/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.facetbrowser.lucene;

import java.io.IOException;
import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.facetbrowser.BaseObjects;
import dk.statsbiblioteket.summa.facetbrowser.IndexBuilder;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetCore;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import org.apache.lucene.document.Document;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class LuceneFacetBuilderTest extends TestCase {
    private static Log log = LogFactory.getLog(LuceneFacetBuilderTest.class);

    public LuceneFacetBuilderTest(String name) {
        super(name);
    }

    BaseObjects bo;
    @Override
    public void setUp() throws Exception {
        super.setUp();
        bo = new BaseObjects();
        IndexBuilder.checkIndex();
        if (facetFolder.exists()) {
            Files.delete(facetFolder);
        }
    }

    File facetFolder = new File(IndexBuilder.DATE_LOCATION,
                          FacetCore.FACET_FOLDER);

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        bo.close();
        if (builder != null) {
            builder.close();
            //noinspection AssignmentToNull
            builder = null;
        }
        if (facetFolder.exists()) {
//            Files.delete(facetFolder);
        }
    }

    public void testBasicConstructionAndOpen() throws Exception {
        getBuilder();
    }

    private LuceneFacetBuilder builder;
    private LuceneFacetBuilder getBuilder() throws IOException {
        if (builder == null) {
            builder = new LuceneFacetBuilder(
                    Configuration.newMemoryBased(), bo.getStructure(),
                    bo.getCoreMap(), bo.getTagHandler());
            builder.open(IndexBuilder.DATE_LOCATION);
        }
        return builder;
    }

    public void testMultipleAdds10() throws Exception {
        testMultipleAdds(10);
    }

    public void testMultipleAdds1000() throws Exception {
        testMultipleAdds(1000);
    }

    public void testMultipleAdds1000000() throws Exception {
        testMultipleAdds(1000000);
    }

    private void testMultipleAdds(int runs) throws Exception {
        int feedback = Math.max(1, Math.min(1000, runs / 100));
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(runs);
        profiler.setBpsSpan(5000);

        LuceneFacetBuilder builder = getBuilder();

        for (int i = 0 ; i < runs ; i++) {
            builder.update(makePayload("foo" + i, i));
            profiler.beat();
            if (i % feedback == 0) {
                log.debug("Added " + (i+1) + "/" + runs + " Payloads at "
                          + profiler.getBps(true) + " Payloads/second. ETA: "
                          + profiler.getETAAsString(true));
            }
        }
        log.debug("Finished adding " + runs + " Payloads in "
                  + profiler.getSpendTime() + " at a total speed of "
                  + profiler.getBps(false) + " Payloads/second");
        long startTime = System.currentTimeMillis();
        builder.store();
        log.debug("Finished closing in "
                  + (System.currentTimeMillis() - startTime) + " ms");
        builder.close();

    }

    public void testAddPayload() throws Exception {
        LuceneFacetBuilder builder = getBuilder();
        int titleID = builder.getStructure().getFacets().
                get(IndexBuilder.TITLE).getFacetID();

        log.debug("Got builder, checking for emptiness");
        assertEquals("The coreMap should contain nothing to start with",
                     0, builder.getCoreMap().getDocCount());
        log.debug("Updating with payload");
        builder.update(makePayload("foo", 0));
        log.debug("Updated with payload, checking for change");
        assertEquals("The coreMap should contain tagIDs for the added document",
                     1, builder.getCoreMap().get(0, titleID).length);
        builder.store();
        builder.close();
    }

    public void testRemovePayload() throws Exception {
        LuceneFacetBuilder builder = getBuilder();
        int titleID = builder.getStructure().getFacets().
                get(IndexBuilder.TITLE).getFacetID();

        builder.update(makePayload("foo", 0));
        builder.update(makePayload("bar", 1));
        log.debug("Updated with 2 payloads, checking for change");
        assertEquals("The added coreMap should contain documents",
                     2, builder.getCoreMap().getDocCount());
        Payload remover = makePayload("foo", 0);
        remover.getData().remove(LuceneIndexUtils.META_ADD_DOCID);
        remover.getData().put(LuceneIndexUtils.META_DELETE_DOCID, 0);
        assertEquals("The coreMap should contain less tagIDs due to deletion",
                     1, builder.getCoreMap().get(0, titleID).length);
    }

    private Payload makePayload(String recordID, int docID) {
        Document doc = IndexBuilder.createDocument(recordID);
        Record record = new Record(recordID, "bar", new byte[0]);
        Payload payload = new Payload(record);
        payload.getData().put(Payload.LUCENE_DOCUMENT, doc);
        payload.getData().put(LuceneIndexUtils.META_ADD_DOCID, docID);
        return payload;
    }

    public void testBuild() throws Exception {
        LuceneFacetBuilder builder = getBuilder();
        builder.build(true);
        log.debug("Finished building, map status is " + builder.getCoreMap());
        assertEquals("The number of documents should be as expected",
                   IndexBuilder.getDocumentCount(),
                   builder.getCoreMap().getDocCount());
        log.debug("Builder stats: " + builder.getCoreMap());
    }

    public static Test suite() {
        return new TestSuite(LuceneFacetBuilderTest.class);
    }
}



