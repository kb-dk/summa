/* $Id: SBToMARCTest.java,v 1.3 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.3 $
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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * SBToMARC Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal")
public class SBToMARCTest extends TestCase {
    public SBToMARCTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIdentity() throws Exception {
        SBToMARC filter = new SBToMARC();
        String[] input = { "Foo bar <hello>  < tag>",
                           "<Foo bar <hello>  < tag>",
                           "F<oo bar <hello>  < tag>",
                           "F<oo bar <hel\nlo>  < tag>",
                           "Foo bar <hello>  < tag"};

        for (String i: input) {
            Reader in = new StringReader(i);
            Writer out = new StringWriter(i.length());
            filter.applyFilter(in, out);
            assertEquals("The input should be returned unmodified", i,
                        out.toString());
        }
    }

    public void testReplace() throws Exception {
        SBToMARC filter = new SBToMARC();
        String[] input = { "Foo bar <hello>  < tag>",
                "<datafield tag=\"014\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"z\">",
                " <datafield tag=\"014\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"h\">",
                " <datafield tag=\"015\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"z\">",
                " <datafield tag=\"014\" ind1=\"0\" ind2=\"0\">\n" +
                "        <Flam code=\"z\">" +
                "        <subfield code=\"h\">",
                " <datafield tag=\"015\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"z\">" +
                "        <subfield code=\"z\">",
                " <datafield tag=\"017\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"a\">" +
                "        <subfield code=\"z\">",
                " <datafield tag=\"015\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"z\">" +
                "        <subfield code=\"z\">" +
                " <datafield tag=\"017\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"z\">" +
                "        <subfield code=\"z\">"};
        String[] expected = { "Foo bar <hello>  < tag>",
                "<datafield tag=\"014\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"a\">",
                " <datafield tag=\"014\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"h\">",
                " <datafield tag=\"015\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"a\">",
                " <datafield tag=\"014\" ind1=\"0\" ind2=\"0\">\n" +
                "        <Flam code=\"z\">" +
                "        <subfield code=\"h\">",
                " <datafield tag=\"015\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"a\">" +
                "        <subfield code=\"a\">",
                " <datafield tag=\"017\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"a\">" +
                "        <subfield code=\"z\">",
                " <datafield tag=\"015\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"a\">" +
                "        <subfield code=\"a\">" +
                " <datafield tag=\"017\" ind1=\"0\" ind2=\"0\">\n" +
                "        <subfield code=\"z\">" +
                "        <subfield code=\"z\">"};

        for (int i1 = 0; i1 < input.length; i1++) {
            String i = input[i1];
            String e = expected[i1];
            Reader in = new StringReader(i);
            Writer out = new StringWriter(i.length());
            filter.applyFilter(in, out);
            assertEquals("The input should be converted correctly", e,
                         out.toString());
        }
    }

    public static Test suite() {
        return new TestSuite(SBToMARCTest.class);
    }
}



