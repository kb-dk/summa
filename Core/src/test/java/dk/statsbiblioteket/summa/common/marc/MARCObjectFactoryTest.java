/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.common.marc;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.exposed.compare.NamedCollatorComparator;

import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MARCObjectFactoryTest extends TestCase {
    private static Log log = LogFactory.getLog(MARCObjectFactoryTest.class);

    public MARCObjectFactoryTest(String name) {
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

    public void testSingleMarc() throws IOException, XMLStreamException, ParseException {
        FileInputStream xml = new FileInputStream(Resolver.getFile("common/marc/single_marc.xml"));
        List<MARCObject> marcs = MARCObjectFactory.generate(xml);
        assertEquals("The result should be a single MARCObject", 1, marcs.size());
        MARCObject marc = marcs.get(0);
        assertNotNull("There should be a leader", marc.getLeader());
        assertEquals("The number of fields should match", 10, marc.getDataFields().size());
        assertEquals("The content of 240a should be as expected",
                     "Melange", marc.getFirstDataField("240").getFirstSubField("a").getContent());
        System.out.println(marc.toXML());
    }

    public void test2Marc() throws IOException, XMLStreamException, ParseException {
        FileInputStream xml = new FileInputStream(Resolver.getFile("common/marc/marc_collection.xml"));
        List<MARCObject> marcs = MARCObjectFactory.generate(xml);
        assertEquals("The result should be a single MARCObject", 2, marcs.size());
    }
}
