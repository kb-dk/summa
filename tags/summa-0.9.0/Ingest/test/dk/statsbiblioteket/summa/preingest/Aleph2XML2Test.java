/* $Id: Aleph2XML2Test.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:23 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.preingest;

import java.io.File;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Aleph2XML2 Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal")
public class Aleph2XML2Test extends TestCase {
    File nondeleted = new File("Ingest/test/dk/statsbiblioteket/summa/preingest"
                               + "/nondeleted_aleph.data").getAbsoluteFile();
    File nondeleted_converted = new File(nondeleted.toString() + ".xml");

    File deleted = new File("Ingest/test/dk/statsbiblioteket/summa/preingest"
                               + "/deleted_aleph.data").getAbsoluteFile();
    File deleted_converted = new File(deleted.toString() + ".xml");

    File deleted2 = new File("Ingest/test/dk/statsbiblioteket/summa/preingest"
                               + "/deleted_aleph2.data").getAbsoluteFile();
    File deleted2_converted = new File(deleted2.toString() + ".xml");

    public Aleph2XML2Test(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (nondeleted_converted.exists()) {
            nondeleted_converted.delete();
        }
        File nondeleted_done = new File(nondeleted.toString() + ".done");
        if (nondeleted_done.exists()) {
            nondeleted_done.renameTo(nondeleted);
        }

        if (deleted_converted.exists()) {
            deleted_converted.delete();
        }
        File deleted_done = new File(deleted.toString() + ".done");
        if (deleted_done.exists()) {
            deleted_done.renameTo(deleted);
        }

        if (deleted2_converted.exists()) {
            deleted2_converted.delete();
        }
        File deleted2_done = new File(deleted2.toString() + ".done");
        if (deleted2_done.exists()) {
            deleted2_done.renameTo(deleted2);
        }
    }

    public void testNonDeleted() throws Exception {
        assertTrue("The nondeleted aleph record must exist at " + nondeleted,
                   nondeleted.exists());
        Aleph2XML2 filter = new Aleph2XML2();
        filter.applyFilter(nondeleted, Extension.xml, "utf-8");
        assertTrue("The destination file must exist",
                   nondeleted_converted.exists());
    }
    public void testDeleted() throws Exception {
        testDeleted(deleted, deleted_converted);
        testDeleted(deleted2, deleted2_converted);
    }

    Pattern storeID =
            Pattern.compile("(?s).*<datafield tag=\"994\" ind1=\"0\" " +
                           "ind2=\"0\">\n<subfield code=\"z\">.+</subfield>.*");
    Pattern invalidID =
            Pattern.compile("(?s).*<datafield tag=\"994\" ind1=\"0\" " +
                   "ind2=\"0\">\n<subfield code=\"z\">-000000001</subfield>.*");
    private void testDeleted(File in, File out) throws Exception {
        assertTrue("The aleph record marked as deleted must exist at " + in,
                   in.exists());
        Aleph2XML2 filter = new Aleph2XML2();
        filter.applyFilter(in, Extension.xml, "utf-8");
        assertTrue("The destination file must exist",
                   out.exists());
        String content = Files.loadString(out);
        assertTrue("There should be a 994.z with the ID for the record at "
                   + in,
                   storeID.matcher(content).matches());
        assertFalse("The ID -000000001 should not be generated for " + in,
                   invalidID.matcher(content).matches());
    }

    public void dumpResults() throws Exception {
        Aleph2XML2 filter = new Aleph2XML2();
        for (File[] in: new File[][] {{nondeleted, nondeleted_converted},
                                    {deleted, deleted_converted},
                                    {deleted2, deleted2_converted}}) {
            filter.applyFilter(in[0], Extension.xml, "utf-8");
            assertTrue("The destination file must exist", in[1].exists());
            String content = Files.loadString(in[1]);
            System.out.println(content);
        }

    }

    public static Test suite() {
        return new TestSuite(Aleph2XML2Test.class);
    }
}



