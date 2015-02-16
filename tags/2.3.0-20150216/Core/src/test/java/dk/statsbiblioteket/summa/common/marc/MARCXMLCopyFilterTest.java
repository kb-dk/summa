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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.support.enrich.MARCXMLCopyFilter;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.text.ParseException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MARCXMLCopyFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(MARCXMLCopyFilterTest.class);

    public MARCXMLCopyFilterTest(String name) {
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
        ObjectFilter feeder = new PayloadFeederHelper(0, "common/marc/single_marc.xml");
        MARCXMLCopyFilter marcCopy = new MARCXMLCopyFilter(Configuration.newMemoryBased(
                MARCXMLCopyFilter.CONF_OUTPUT, MARCXMLCopyFilter.OUTPUT.directsumma
        ));
        marcCopy.setSource(feeder);
        System.out.println(marcCopy.next().getRecord().getChildren().get(0).getContentAsUTF8());
        marcCopy.close(true);
    }
}
