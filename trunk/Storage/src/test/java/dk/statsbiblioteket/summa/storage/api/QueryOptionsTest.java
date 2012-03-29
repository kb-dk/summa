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
package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

/**
 * Test cases for {@link QueryOptions}
 *
 * @author Mikkel Kamstrup <mailto:mke@statsbiblioteket.dk>
 * @since Jan 8, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class QueryOptionsTest extends TestCase {

    public void testAllowsRecord() {
        Record standard = new Record("1", "b", new byte[0]);
        Record deleted = new Record("1", "b", new byte[0]);
        deleted.setDeleted(true);
        Record nonIndexable = new Record("1", "b", new byte[0]);
        nonIndexable.setIndexable(false);

        QueryOptions opt = new QueryOptions();
        assertTrue(opt.allowsRecord(standard));
        assertTrue(opt.allowsRecord(deleted));
        assertTrue(opt.allowsRecord(nonIndexable));

        opt = new QueryOptions(true, null, 0, 0);
        assertFalse(opt.allowsRecord(standard));
        assertTrue(opt.allowsRecord(deleted));
        assertFalse(opt.allowsRecord(nonIndexable));

        opt = new QueryOptions(false, null, 0, 0);
        assertTrue(opt.allowsRecord(standard));
        assertFalse(opt.allowsRecord(deleted));
        assertTrue(opt.allowsRecord(nonIndexable));

        opt = new QueryOptions(null, true, 0, 0);
        assertTrue(opt.allowsRecord(standard));
        assertTrue(opt.allowsRecord(deleted));
        assertFalse(opt.allowsRecord(nonIndexable));

        opt = new QueryOptions(null, false, 0, 0);
        assertFalse(opt.allowsRecord(standard));
        assertFalse(opt.allowsRecord(deleted));
        assertTrue(opt.allowsRecord(nonIndexable));
    }

    public void testMeta() {
        QueryOptions opt = new QueryOptions();

        assertFalse(opt.hasMeta());
        assertNull(opt.meta("foo"));

        opt.meta("foo", "bar");
        assertEquals("bar", opt.meta("foo"));
        assertTrue(opt.hasMeta());
    }

    public void testGetNewRecord() {
        QueryOptions opt = new QueryOptions(null, null, 0, 0, null,
                new QueryOptions.ATTRIBUTES[] { QueryOptions.ATTRIBUTES.RECORDBASE,
                                                QueryOptions.ATTRIBUTES.RECORDMETA,
                                                QueryOptions.ATTRIBUTES.RECORDID}
                );

        Record r = opt.getNewRecord(new Record("test", "test-base", true, false,
                                    null, 0, 0, null, null, null, false));
        assertNotNull(r);

        opt = new QueryOptions(null, null, 0, 0, null,
                new QueryOptions.ATTRIBUTES[] { QueryOptions.ATTRIBUTES.RECORDBASE,
                                                QueryOptions.ATTRIBUTES.RECORDMETA}
                );
        try {
            r = opt.getNewRecord(r);
            fail("This should fail with illegal arguement exception");
        } catch(Exception e) {
            // okay
        }

        opt = new QueryOptions(null, null, 0, 0, null,
                new QueryOptions.ATTRIBUTES[] { QueryOptions.ATTRIBUTES.RECORDID,
                                                QueryOptions.ATTRIBUTES.RECORDMETA}
                );
        try {
            r = opt.getNewRecord(r);
            fail("This should fail with illegal arguement exception");
        } catch(Exception e) {
            // okay
        }

    }

}
