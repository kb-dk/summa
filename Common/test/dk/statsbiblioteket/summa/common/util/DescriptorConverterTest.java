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
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * DescriptorConverter Tester.
 *
 * @author <Authors name>
 * @since <pre>11/26/2008</pre>
 * @version 1.0
 */
public class DescriptorConverterTest extends TestCase {
    private static Log log = LogFactory.getLog(DescriptorConverterTest.class);
    private static final String OLD_DESCRIPTOR =
           "dk/statsbiblioteket/summa/common/index/IndexDescriptorOldStyle.xml";

    public DescriptorConverterTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testConvert() throws Exception {
        String old = Resolver.getUTF8Content(OLD_DESCRIPTOR);
        log.info("New style:\n" + new DescriptorConverter().convert(old));
    }

    public void testConvertNoComment() throws Exception {
        String old = Resolver.getUTF8Content(OLD_DESCRIPTOR);
        DescriptorConverter converter = new DescriptorConverter();
        converter.setDumpAnalyzer(false);
        converter.setDumpResolver(false);
        log.info("New style:\n" + converter.
                convert(old));
    }

    public void testConvertClean() throws Exception {
        String old = Resolver.getUTF8Content(OLD_DESCRIPTOR);
        DescriptorConverter converter = new DescriptorConverter();
        converter.setDumpAnalyzer(false);
        converter.setDumpResolver(false);
        converter.setDumpHasSuggest(false);
        converter.setResetBoosts(true);
        System.out.println(converter.convert(old));
    }

    public static Test suite() {
        return new TestSuite(DescriptorConverterTest.class);
    }
}
