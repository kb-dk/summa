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
package dk.statsbiblioteket.summa.common.unittest;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import junit.framework.TestCase;

/**
 * te forgot to document this class.
 */
public class LuceneUtils extends TestCase {
    /**
     * Verifies that the given ids are present in the given order in the index.
     * @param location     the location of a Lucene index.
     * @param ids          the ids to verify existence for.
     * @throws IOException if the index could not be accessed.
     */
    public static void verifyContent(File location,
                                      String[] ids) throws IOException {
        IndexReader reader = IndexReader.open(location);
        try {
            int expectedCount = 0;
            for (int i = 0 ; i < reader.maxDoc() ; i++) {
                if (!reader.isDeleted(i)) {
                    assertEquals("The id '" + ids[expectedCount]
                                 + "' should be present in the "
                                 + "index at position " + i,
                                 ids[expectedCount],
                                 reader.document(i).getValues(
                                         IndexUtils.RECORD_FIELD)[0]);
                    expectedCount++;
                }
            }
            assertEquals("The number of checked ids should match",
                         expectedCount, ids.length);
        } finally {
            reader.close();
        }
    }

}
