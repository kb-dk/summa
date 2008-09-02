/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.MemoryTagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.summa.facetbrowser.IndexBuilder;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import org.apache.lucene.index.IndexReader;

/**
 * CoreMapFactory Tester.
 *
 * @author <Authors name>
 * @since <pre>10/09/2006</pre>
 * @version 1.0
 */
@SuppressWarnings({"deprecation"})
public class CoreMapFactoryTest extends TestCase {
    public CoreMapFactoryTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetCoreMap() throws Exception {
        assertTrue("Standard bitstuffed CoreMap expected for small maxTags",
                   CoreMapFactory.getCoreMap(100000, 20,
                                             (int) StrictMath.pow(2, 5))
                           instanceof CoreMapBitStuffed);
        assertTrue("Long bitstuffed CoreMap expected for large maxTags",
                   CoreMapFactory.getCoreMap(100000, 20,
                                             (int) StrictMath.pow(2, 30))
                           instanceof CoreMapBitStuffedLong);
        assertTrue("Standard bitstuffed CoreMap expected for 31 facets",
                   CoreMapFactory.getCoreMap(100000, 31,
                                             (int) StrictMath.pow(2, 5))
                           instanceof CoreMapBitStuffed);
        assertTrue("Long bitstuffed CoreMap expected for many facets",
                   CoreMapFactory.getCoreMap(100000, 33,
                                             (int) StrictMath.pow(2, 5))
                           instanceof CoreMapBitStuffedLong);
    }

    public void testFillMap() throws Exception {
        // TODO: Change this to use IndexConnector
/*        IndexReader reader = IndexBuilder.getReader();
        String[] facets = new String[]{ IndexBuilder.AUTHOR,
                                        IndexBuilder.TITLE,
                                        IndexBuilder.GENRE,
                                        IndexBuilder.VARIABLE };
        StructureDescription structure = new StructureDescription(facets);
        TagHandler tagHandler = new MemoryTagHandler(reader, structure);
        CoreMap coreMap =
                CoreMapFactory.getCoreMap(reader.maxDoc(),
                                          facets.length,
                                          tagHandler.getMaxTagCount());
        MemoryStorage memStore = new MemoryStorage();
        // Consider adding stopwords to the configuration
        Configuration configuration = new Configuration(memStore);
        CoreMapFactory.fillMap(coreMap, structure, tagHandler, reader,
                               configuration, false);
        assertEquals("The doc count for the core map should equal that of "
                     + "the index", reader.maxDoc(), coreMap.getDocCount());
        assertEquals("The number of authors for document 0 should be 2 " +
                     "(chevalier, og, ségul",
                     3, coreMap.get(0, 0).length);*/
    }

    public static Test suite() {
        return new TestSuite(CoreMapFactoryTest.class);
    }
}
