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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class MUXFilterTest extends TestCase implements ObjectFilter {
    private static Log log = LogFactory.getLog(MUXFilterTest.class);

    public MUXFilterTest(String name) {
        super(name);
    }

    private static final int PAYLOADS = 5;
    @Override
    public void setUp() throws Exception {
        super.setUp();
        recordsLeft = PAYLOADS;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }


    public static Test suite() {
        return new TestSuite(MUXFilterTest.class);
    }

    /**
     * Sets up a muxer with slow processing (DelayFilter) and feeds it an EOF,
     * checking that all Payloads are processed.
     * https://gforge.statsbiblioteket.dk/tracker/?func=detail&atid=109&aid=1553&group_id=8}
     * @throws Exception if things go bonkers.
     */
    public void testEOF() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        List<Configuration> delays =
                conf.createSubConfigurations(MUXFilter.CONF_FILTERS, 1);
        delays.get(0).set(MUXFilterFeeder.CONF_FILTER_CLASS,
                          DelayFilter.class.getCanonicalName());
        delays.get(0).set(DelayFilter.CONF_FIXED_DELAY_PREREQUEST,
                          200 * 1000000);
        delays.get(0).set(MUXFilter.CONF_INSTANCES, 2);
        log.debug("Creating MUXFilter");
        MUXFilter muxer = new MUXFilter(conf);
        muxer.setSource(this);
        int counter = 0;
        log.debug("Emptying MUXFilter");
        while (muxer.hasNext()) {
            muxer.next();
            counter++;
        }
        assertEquals("The number of processed Payloads should be correct",
                     PAYLOADS, counter);
    }

    /* ObjectFilter */
    private int recordsLeft = 0;
    public boolean hasNext() {
        return recordsLeft > 0;
    }
    public void setSource(Filter filter) {
        // Nada
    }
    public boolean pump() throws IOException {
        next();
        return true;
    }
    public void close(boolean success) {
        // Nada
    }
    public Payload next() {
        if (!hasNext()) {
            return null;
        }
        log.debug("Delivering Dummy_" + recordsLeft);
        return new Payload(new Record(
                "Dummy_" + recordsLeft--, "foo", new byte[0]));
    }
    public void remove() {
        recordsLeft--;
    }
}
