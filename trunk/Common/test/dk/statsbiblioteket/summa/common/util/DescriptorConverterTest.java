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
        System.out.println("New style:\n"
                           + new DescriptorConverter().convert(old));
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

