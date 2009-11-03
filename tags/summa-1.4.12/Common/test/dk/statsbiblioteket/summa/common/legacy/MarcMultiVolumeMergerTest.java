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
package dk.statsbiblioteket.summa.common.legacy;

import dk.statsbiblioteket.summa.common.MarcAnnotations;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.util.xml.DOM;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarcMultiVolumeMergerTest extends TestCase implements
                                                                  ObjectFilter {
    private static Log log = LogFactory.getLog(MarcMultiVolumeMergerTest.class);

    private List<Record> records;

    public MarcMultiVolumeMergerTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        makeSampleHorizon();
    }

    private void makeSampleHorizon() throws IOException {
        Record parent = createRecord("parent_book1.xml");
        List<Record> children = new ArrayList<Record>(4);
        Record child1 = createRecord("child_book1.xml");
        child1.getMeta().put(MarcAnnotations.META_MULTI_VOLUME_TYPE,
                             MarcAnnotations.MultiVolumeType.BIND.toString());
        children.add(child1);
        Record child2 = createRecord("child_book2.xml");
        child2.getMeta().put(MarcAnnotations.META_MULTI_VOLUME_TYPE,
                             MarcAnnotations.MultiVolumeType.BIND.toString());
        children.add(child2);
        Record child4 = createRecord("child_book4.xml");
        child4.getMeta().put(MarcAnnotations.META_MULTI_VOLUME_TYPE,
                            MarcAnnotations.MultiVolumeType.SEKTION.toString());
        Record subChild1 = createRecord("subchild_book1.xml");
        subChild1.getMeta().put(MarcAnnotations.META_MULTI_VOLUME_TYPE,
                               MarcAnnotations.MultiVolumeType.BIND.toString());
        child4.setChildren(Arrays.asList(subChild1));
        children.add(child4);
        parent.setChildren(children);

        records = new ArrayList<Record>(1);
        records.add(parent);
    }

    private void makeSampleAleph() throws IOException {
        Record parent = createAlephRecord("aleph_parent.xml");

        Record middle = createAlephRecord("aleph_middle.xml");
        middle.getMeta().put(MarcAnnotations.META_MULTI_VOLUME_TYPE,
                             MarcAnnotations.MultiVolumeType.BIND.toString());

        Record child = createAlephRecord("aleph_child.xml");
        child.getMeta().put(MarcAnnotations.META_MULTI_VOLUME_TYPE,
                            MarcAnnotations.MultiVolumeType.BIND.toString());

        middle.setChildren(Arrays.asList(child));
        parent.setChildren(Arrays.asList(middle));

        records = new ArrayList<Record>(1);
        records.add(parent);
    }

    private Record createRecord(String filename) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        return new Record(filename, "Dummy", Resolver.getUTF8Content(
                "data/horizon/" + filename).getBytes("utf-8"));
    }

    private Record createAlephRecord(String filename) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        return new Record(filename, "Dummy", Resolver.getUTF8Content(
                "data/aleph/" + filename).getBytes("utf-8"));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        records.clear();
    }

    public static Test suite() {
        return new TestSuite(MarcMultiVolumeMergerTest.class);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void testMerge() throws Exception {
        MarcMultiVolumeMerger merger =
                new MarcMultiVolumeMerger(Configuration.newMemoryBased());
        merger.setSource(this);
        String processed = merger.next().getRecord().getContentAsUTF8();
        log.info("Processed:\n" + processed);
        assertContains(processed, // BIND
              "  <datafield tag=\"248\" ind1=\"0\" ind2=\"0\">\n"
              + "    <subfield code=\"a\">Kaoskyllingens endeligt</subfield>");
        assertContains(processed, // SECTION
              "  <datafield tag=\"247\" ind1=\"0\" ind2=\"0\">\n"
              + "    <subfield code=\"a\">Kaoskyllingens rige</subfield>");
        assertContains(processed, // HOVEDPOST
              "  <datafield tag=\"245\" ind1=\"0\" ind2=\"0\">\n"
              + "    <subfield code=\"a\">Kaoskyllingen</subfield>");
        assertContains(processed, // Correct close tag
              "</record>");
    }

    public void testMergeAleph() throws Exception {
        makeSampleAleph();
        MarcMultiVolumeMerger merger = new MarcMultiVolumeMerger(
                Configuration.newMemoryBased());
        merger.setSource(this);
        String processed = merger.next().getRecord().getContentAsUTF8();
        assertNotNull("DOM-parsing should succeed for\n" + countTestCases(),
                      DOM.stringToDOM(processed));
        log.info("Processed:\n" + processed);
        // TODO: Check here
    }

    private void assertContains(String main, String sub) {
        boolean contains = flatten(main).contains(flatten(sub));
        assertTrue("The String '" + sub + "' should exist in the main String",
                   contains);
    }

    private String flatten(String str) {
        return str.replace(" ", "").replace("\n", "");
    }

    /* simple ObjectFilter implementation */

    public boolean hasNext() {
        return records.size() > 0;
    }

    public void setSource(Filter filter) {
        // Nada
    }

    public boolean pump() throws IOException {
        return hasNext() && next() != null && hasNext();
    }

    public void close(boolean success) {
        records.clear();
    }

    public Payload next() {
        return new Payload(records.remove(0));
    }

    public void remove() {
        // Nada
    }
}
