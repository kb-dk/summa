/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.plugins;

import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.facetbrowser.FacetManipulator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class SimpleDateFormatTest extends TestCase {
    private static Log log = LogFactory.getLog(SimpleDateFormatTest.class);

    public SimpleDateFormatTest(String name) {
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
        return new TestSuite(SimpleDateFormatTest.class);
    }

    public void testCallback() throws IOException, TransformerException {
        final String[] expecteds = new String[]{
                "2017-11-15T18:00:00Z", // Winter da:+1
                "2017-07-15T19:00:00Z", // Summer da:+2
                "2017-11-16T12:00:00Z",
                "2017-11-17T11:00:00Z",
                "2017-11-18T12:00:00Z",
        };
        String transformed = SaxonXSLT.transform(
                Resolver.getURL("plugins/simpledateformat.xslt"),
                Resolver.getUTF8Content("plugins/simpledateformat.xml"));
        for (String expected: expecteds) {
            assertTrue("The datetime '" + expected + "' should be present in\n" + transformed.replace(">", ">\n"),
                       transformed.contains(expected));
        }
        log.info("Transformed XML:\n" + transformed);
    }

    public void disabledtestTimezoneHack() throws ParseException {
        String[][] TESTS = new String[][]{
                // in, out
                {"2017-11-10T12:00:00Z",     "2017-11-10T12:00:00Z"},
                {"2017-11-10T12:00:00+0200", "2017-11-10T12:00:00Z"},
                {"2017-11-10T12:00:00+0500", "2017-11-10T12:00:00Z"},
                {"2017-11-10T12:00:00-0600", "2017-11-10T12:00:00Z"},
        };
        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        SimpleDateFormat out = new SimpleDateFormat("d. MMMM yyyy 'kl'. HH:mm");
        for (String[] test: TESTS) {
            assertEquals("Input time '" + test[0] + " should be re-written as expected",
                         test[1], out.format(in.parse(test[0])));
        }
    }

}

