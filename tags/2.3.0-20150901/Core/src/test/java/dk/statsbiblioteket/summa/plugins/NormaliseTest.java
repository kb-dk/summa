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
import dk.statsbiblioteket.util.xml.XSLT;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.xml.transform.TransformerException;
import java.io.IOException;

public class NormaliseTest extends TestCase {
    public NormaliseTest(String name) {
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
        return new TestSuite(NormaliseTest.class);
    }

    public void testCallback() throws IOException, TransformerException {
        String normalised = XSLT.transform(
                Resolver.getURL("plugins/normalise.xslt"), Resolver.getUTF8Content("plugins/normalise.xml"));
        assertTrue("The Normaliser callback should remove paranthesis\n" + normalised,
                   normalised.contains("Some data"));
    }

}

