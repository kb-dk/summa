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
package dk.statsbiblioteket.summa.ingest.source;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * RecordGenerator Tester.
 *
 * @author <Authors name>
 * @since <pre>12/04/2008</pre>
 * @version 1.0
 */
public class RecordGeneratorTest extends TestCase {
    public RecordGeneratorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(RecordGeneratorTest.class);
    }

    public void testIncrementalNumber() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        String contentTemplate =
                RecordGenerator.CONTENT_INCREMENTAL_NUMBER + "[a]"
                + RecordGenerator.CONTENT_INCREMENTAL_NUMBER + "[b]"
                + "Foo!$Hey!?\n"
                + RecordGenerator.CONTENT_INCREMENTAL_NUMBER + "[b]";
        conf.set(RecordGenerator.CONF_CONTENT_TEMPLATE, contentTemplate);
        RecordGenerator generator = new RecordGenerator(conf);
        assertEquals("Expansion of incrementalNumber should work",
                     "00Foo!$Hey!?\n1", generator.expand(contentTemplate));
    }
}
