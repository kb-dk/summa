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
package dk.statsbiblioteket.summa.common.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.net.URL;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class RecordUtilTest extends TestCase {
    private static Log log = LogFactory.getLog(RecordUtilTest.class);

    public RecordUtilTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(RecordUtilTest.class);
    }

    private static final URL schema = Resolver.getURL(
            "dk/statsbiblioteket/summa/common/Record.xsd");
    public void testSingleRecord() throws Exception {
        Record record = new Record(
                "foo&<", "/>baze",
                "Hello <![CDATA[&<]]> world".getBytes("utf-8"));
        String xml = RecordUtil.toXML(record);
        assertNotNull("The schema Record.xsd should be available",
                      schema.getFile());
        ExtraAsserts.assertValidates("single-level Record should validate",
                                     schema, ParseUtil.XML_HEADER + "\n" + xml);
        assertEquals("fromXML(toXML(simpleRecord)) should work",
                     record, RecordUtil.fromXML(xml));
        log.info("Generated XML:\n" + xml);
    }

    public void testRecordWithRelatives() throws Exception {
        Record record = new Record(
                "middleman", "bar",
                "<marc_xml_or_other_xml ...>Malcolm in the middle".getBytes("utf-8"));
        Record parent = new Record(
                "parent", "bar",
                "<marc_xml_or_other_xml ...>I am a parent Record".getBytes("utf-8"));
        Record child1 = new Record(
                "child_1", "bar",
                "<marc_xml_or_other_xml ...>I am a child".getBytes("utf-8"));
        Record child2 = new Record(
                "child_2", "bar",
                "<marc_xml_or_other_xml ...>I am another child".getBytes("utf-8"));
        record.setParents(Arrays.asList(parent));
        record.setChildren(Arrays.asList(child1, child2));

        String xml = RecordUtil.toXML(record);
        ExtraAsserts.assertValidates("single-level Record should validate",
                                     schema, ParseUtil.XML_HEADER + "\n" + xml);
        assertEquals("fromXML(toXML(simpleRecord)) should work",
                     record, RecordUtil.fromXML(xml));
        log.info("Generated XML:\n" + xml);
    }
}
