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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class DiscardRelativesFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(
            DiscardRelativesFilterTest.class);

    public DiscardRelativesFilterTest(String name) {
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
        return new TestSuite(DiscardRelativesFilterTest.class);
    }

    private List<Payload> getSampleData() {
        List<Payload> payloads = new ArrayList<Payload>(10);

        Record parentRecord = new Record("Parent", "dummy", new byte[0]);
        Record middleRecord = new Record("Middle", "dummy", new byte[0]);
        Record childRecord =  new Record("Child",  "dummy", new byte[0]);
        parentRecord.setChildren(Arrays.asList(middleRecord));
        middleRecord.setChildren(Arrays.asList(childRecord));
        middleRecord.setParents(Arrays.asList(parentRecord));
        childRecord.setParents(Arrays.asList(middleRecord));
        payloads.add(new Payload(parentRecord));
        payloads.add(new Payload(middleRecord));
        payloads.add(new Payload(childRecord));

        payloads.add(new Payload(new Record(
                "NoRelatives", "dummy", new byte[0])));
        payloads.add(new Payload(new Record(
                "StillNoRelatives", "dummy", new byte[0])));
        return payloads;
    }

    private List<Payload> suck(ObjectFilter filter) {
        PayloadFeederHelper feeder = new PayloadFeederHelper(getSampleData());
        filter.setSource(feeder);
        List<Payload> result = new ArrayList<Payload>(10);
        while (filter.hasNext()) {
            result.add(filter.next());
        }
        return result;
    }

    public void testDiscardHasParents() throws Exception {
        DiscardRelativesFilter discarder =
                new DiscardRelativesFilter(Configuration.newMemoryBased(
                        DiscardRelativesFilter.CONF_DISCARD_HASPARENT, true));
        List<Payload> passed = suck(discarder);
        assertEquals("Only Payloads with no parent should pass",
                     3, passed.size());
    }

    public void testDiscardHasChildren() throws Exception {
        DiscardRelativesFilter discarder =
                new DiscardRelativesFilter(Configuration.newMemoryBased(
                        DiscardRelativesFilter.CONF_DISCARD_HASCHILDREN, true));
        List<Payload> passed = suck(discarder);
        assertEquals("Only Payloads with no children should pass",
                     3, passed.size());
    }

    public void testDiscardHasParentOrHasChildren() throws Exception {
        DiscardRelativesFilter discarder =
                new DiscardRelativesFilter(Configuration.newMemoryBased(
                        DiscardRelativesFilter.CONF_DISCARD_HASPARENT, true,
                        DiscardRelativesFilter.CONF_DISCARD_HASCHILDREN, true));
        List<Payload> passed = suck(discarder);
        assertEquals("Only Payloads with no relatives should pass",
                     2, passed.size());
    }

    public void testDiscardNone() throws Exception {
        DiscardRelativesFilter discarder =
                new DiscardRelativesFilter(Configuration.newMemoryBased());
        List<Payload> passed = suck(discarder);
        assertEquals("All Payloads should pass",
                     5, passed.size());
    }

    public void testToXML() throws Exception {
        List<Payload> payloads = getSampleData();

        log.info("Performing standard Payload.toString");
        for (Payload payload: payloads) {
            log.debug(payload);
        }

        log.info("Performing verbose Payload.toString");
        for (Payload payload: payloads) {
            log.debug(payload.getRecord().toString(true));
        }

        log.info("Performing Record XML dump");
        for (Payload payload: payloads) {
            log.debug(RecordUtil.toXML(payload.getRecord()));
        }
    }
}
