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
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te, mke")
public class LuceneTestHelper extends TestCase {
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
            int encountered = 0;
            for (int i = 0 ; i < reader.maxDoc() && i < ids.length; i++) {
                if (!reader.isDeleted(i)) {
                    assertEquals("The id '" + ids[encountered]
                                 + "' should be present in the "
                                 + "index at position " + i,
                                 ids[encountered],
                                 reader.document(i).getValues(
                                         IndexUtils.RECORD_FIELD)[0]);
                    encountered++;
                }
            }
            assertEquals(String.format(
                    "The number of checked ids in %s should match", location),
                         ids.length, encountered);
        } finally {
            reader.close();
        }
    }

}



