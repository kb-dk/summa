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
package dk.statsbiblioteket.summa.common.lucene;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.index.lucene.StreamingDocumentCreator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;

/**
 * Tests whether indexing and searching on dynamic (prefix-based) fields works.
 */
public class DynamicFieldTest extends TestCase {
    public static final File DESCRIPTOR = Resolver.getFile("common/lucene/DynamicDescriptor.xml");

    public DynamicFieldTest(String name) {
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
        return new TestSuite(DynamicFieldTest.class);
    }

    private ObjectFilter getIndexChain(ObjectFilter feeder) {
        ObjectFilter xmlToDoc = new StreamingDocumentCreator(Configuration.newMemoryBased(

        ));
        xmlToDoc.setSource(feeder);
        return xmlToDoc;
    }
}

