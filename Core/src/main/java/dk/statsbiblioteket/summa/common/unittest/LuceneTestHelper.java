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
package dk.statsbiblioteket.summa.common.unittest;

import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.ReaderUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    public static void verifyContent(File location, String[] ids) throws IOException {
        List<String> actualIDs = getIDs(location);
        assertEquals("There should be the same number of ids",
                     ids.length, actualIDs.size());
        for (int i = 0 ; i < ids.length; i++) {
            assertEquals("The id '" + ids[i] + "' should be present in the index",
                         ids[i], actualIDs.get(i));
        }
        assertEquals(String.format("The number of checked ids in %s should match", location),
                     ids.length, actualIDs.size());
    }
    /**

     * Verifies that the given ids and only the given ids are present in the
     * index.
     * @param location     the location of a Lucene index.
     * @param ids          the ids to verify existence for.
     * @throws IOException if the index could not be accessed.
     */
    public static void verifyContentNoOrder(File location, String[] ids) throws IOException {
        List<String> actualIDs = getIDs(location);
        Arrays.sort(ids);
        Collections.sort(actualIDs);
        assertEquals("There should be the same number of ids",
                     ids.length, actualIDs.size());
        for (int i = 0 ; i < ids.length; i++) {
            assertEquals("The id '" + ids[i] + "' should be present in the index",
                         ids[i], actualIDs.get(i));
        }
        assertEquals(String.format("The number of checked ids in %s should match", location),
                     ids.length, actualIDs.size());
    }

    public static List<String> getIDs(File location) throws IOException {
        List<String> ids = new ArrayList<String>(100);
        DirectoryReader reader = DirectoryReader.open(new NIOFSDirectory(location));
        List<AtomicReader> readers = new ArrayList<AtomicReader>(10);
        ReaderUtil.gatherSubReaders(readers, reader);
        try {
            for (AtomicReader sub: readers) {
                for (int i = 0 ; i < sub.maxDoc() ; i++) {
                    if (sub.getLiveDocs() == null || sub.getLiveDocs().get(i)) {
                        ids.add(sub.document(i).getValues(IndexUtils.RECORD_FIELD)[0]);
                    }
                }
            }
        } finally {
            reader.close();
        }
        return ids;
    }
}

