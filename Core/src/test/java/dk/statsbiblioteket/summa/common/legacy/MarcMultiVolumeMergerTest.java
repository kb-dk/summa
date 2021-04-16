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
package dk.statsbiblioteket.summa.common.legacy;

import dk.statsbiblioteket.summa.common.MarcAnnotations;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.util.PayloadMatcher;
import dk.statsbiblioteket.util.xml.DOM;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarcMultiVolumeMergerTest extends TestCase implements ObjectFilter {
    private static Log log = LogFactory.getLog(MarcMultiVolumeMergerTest.class);

    private List<Record> records;

    public MarcMultiVolumeMergerTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    private void makeSampleHorizon() throws IOException {
        Record parent = createRecord("parent_book1.xml");
        List<Record> children = new ArrayList<>(4);
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

        records = new ArrayList<>(1);
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

        records = new ArrayList<>(1);
        records.add(parent);
    }

    private Record createRecord(String filename) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        return new Record(filename, "Dummy", Resolver.getUTF8Content(
                "common/horizon/" + filename).getBytes(StandardCharsets.UTF_8));
    }

    private Record createAlephRecord(String filename) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        return new Record(filename, "aleph", Resolver.getUTF8Content(
                "common/aleph/" + filename).getBytes(StandardCharsets.UTF_8));
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
        makeSampleHorizon();
        MarcMultiVolumeMerger merger = new MarcMultiVolumeMerger(Configuration.newMemoryBased());
        merger.setSource(this);
        String processed = merger.next().getRecord().getContentAsUTF8();
//        log.info("Processed:\n" + processed);
        // FIXME: Convert this to a proper XML-assert as the attribute order depends on Java version
        assertContains(processed, // BIND
                       "<datafield ind2=\"0\" ind1=\"0\" tag=\"248\">" +
                       "<subfield code=\"a\">Kaoskyllingens endeligt</subfield>");
        assertContains(processed, // SECTION
                       "<datafield ind2=\"0\" ind1=\"0\" tag=\"247\">"
                       + "<subfield code=\"a\">Kaoskyllingens rige</subfield>");
        assertContains(processed, // HOVEDPOST
                       "<datafield tag=\"245\" ind1=\"0\" ind2=\"0\">"
                       + "<subfield code=\"a\">Kaoskyllingen</subfield>");
        assertContains(processed, // Correct close tag
                       "</record>");
    }

    public void testDeleteDiscard() throws IOException {
        makeSampleHorizon();
        markAsDeleted("child_book1.xml"); // child1: Den apokalyptiske Kaoskylling
        MarcMultiVolumeMerger merger = new MarcMultiVolumeMerger(Configuration.newMemoryBased());
        merger.setSource(this);
        String processed = merger.next().getRecord().getContentAsUTF8();
        assertFalse("The result should not contain the deleted record\n" + processed,
                    processed.contains("Den apokalyptiske Kaoskylling"));
    }

    @SuppressWarnings("SameParameterValue")
    private void markAsDeleted(String id) {
        for (Record record: records) {
            if (id.equals(record.getId())) {
                record.setDeleted(true);
                return;
            }
            if (record.hasChildren()) {
                for (Record sub : record.getChildren()) {
                    if (id.equals(sub.getId())) {
                        sub.setDeleted(true);
                        return;
                    }
                }
            }
            if (record.hasParents()) {
                for (Record sup : record.getParents()) {
                    if (id.equals(sup.getId())) {
                        sup.setDeleted(true);
                        return;
                    }
                }
            }
        }
        fail("Unable to locate record with ID '" + id + "' intended for delete marker");
    }

    // <datafieldind2="0"ind1="0"tag="248"><subfieldcode="a">Kaoskyllingensendeligt</subfield>
    // <datafieldtag="248"ind2="0"ind1="0"><subfieldcode="a">Kaoskyllingensendeligt</subfield>
    public void testMergeAleph() throws Exception {
        makeSampleAleph();
        MarcMultiVolumeMerger merger = new MarcMultiVolumeMerger(
                Configuration.newMemoryBased());
        merger.setSource(this);
        String processed = merger.next().getRecord().getContentAsUTF8();
        assertNotNull("DOM-parsing should succeed for\n" + countTestCases(),
                      DOM.stringToDOM(processed));
        assertTrue("The result should contain 'Frühe'\n" + processed,
                   processed.contains("Frühe"));
    }

    public void testExtraContent() throws Exception {
        makeSampleAleph();
        List<Record> children = records.get(0).getChildren();
        children.add(new Record("Dummy", "bar", new byte[10]));
        records.get(0).setChildren(children);
        Configuration conf = Configuration.newMemoryBased();
        Configuration ignoreConf = conf.createSubConfiguration(
            MarcMultiVolumeMerger.CONF_MERGE_RECORDS);
        ignoreConf.set(PayloadMatcher.CONF_BASE_REGEX, "aleph");
        MarcMultiVolumeMerger merger = new MarcMultiVolumeMerger(conf);
        merger.setSource(this);
        String processed = merger.next().getRecord().getContentAsUTF8();
        assertNotNull("DOM-parsing should succeed for\n" + countTestCases(),
                      DOM.stringToDOM(processed));
        assertTrue("The result should contain 'Frühe'\n" + processed,
                   processed.contains("Frühe"));
    }

    private void assertContains(String main, String sub) {
        boolean contains = flatten(main).contains(flatten(sub));
        assertTrue("The String '" + sub + "' should exist in the main String\n" + main,
                   contains);
    }

    private String flatten(String str) {
        return str.replace(" ", "").replace("\n", "");
    }

    /* simple ObjectFilter implementation */

    @Override
    public boolean hasNext() {
        return !records.isEmpty();
    }

    @Override
    public void setSource(Filter filter) {
        // Nada
    }

    @Override
    public boolean pump() throws IOException {
        return hasNext() && next() != null && hasNext();
    }

    @Override
    public void close(boolean success) {
        records.clear();
    }

    @Override
    public Payload next() {
        return new Payload(records.remove(0));
    }

    @Override
    public void remove() {
        // Nada
    }
}

