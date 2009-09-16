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
package dk.statsbiblioteket.summa.support;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;

import java.util.Arrays;
import java.io.FileInputStream;

public class TikaDocumentCreatorTest extends TestCase {
    public TikaDocumentCreatorTest(String name) {
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
        return new TestSuite(TikaDocumentCreatorTest.class);
    }

    public void testStrictXHTML() throws Exception {
        String descriptorLocation = "data/tika/TikaTest_IndexDescriptor.xml";

        Payload strict = new Payload(new FileInputStream(Resolver.getFile(
                "data/tika/strict1.xhtml")));
        Payload lax = new Payload(new FileInputStream(Resolver.getFile(
                "data/tika/lax1.xhtml")));
        PayloadFeederHelper feeder =
                new PayloadFeederHelper(Arrays.asList(strict, lax));
        Configuration conf = Configuration.newMemoryBased();
        Configuration idConf = conf.createSubConfiguration(
                IndexDescriptor.CONF_DESCRIPTOR);
        idConf.set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, descriptorLocation);

        TikaDocumentCreator tika = new TikaDocumentCreator(conf);
        tika.setSource(feeder);
        System.out.println("**** Dumping strict");
        tika.next();
        System.out.println("**** Dumping lax");
        tika.next();
    }
}
