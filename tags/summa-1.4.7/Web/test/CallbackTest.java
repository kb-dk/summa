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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XSLT;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import junit.framework.TestCase;

import java.util.Properties;
import java.io.File;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CallbackTest extends TestCase {
    private static Log log = LogFactory.getLog(CallbackTest.class);

    private static final String XSLTLocationString =
            "data/transformCallback/getLikes.xsl";
    private static final String XMLLocationString =
            "data/transformCallback/callback_input.xml";


    @SuppressWarnings({"deprecation"})
    public void testXMLOperations() throws Exception {


        Properties prop = new Properties();

        prop.put("bundle_global", "globals");
        prop.put("bundle_availability", "availability");
        prop.put("locale", "da");


        System.out.println(XSLT.transform(
                Resolver.getURL(XSLTLocationString),
                Resolver.getUTF8Content(XMLLocationString),
                prop, true));
    }


}
