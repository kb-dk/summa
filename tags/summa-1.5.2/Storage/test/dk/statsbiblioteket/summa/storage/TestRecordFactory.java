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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;

/**
 * A test-helper tool for creating dummy records
 */
public class TestRecordFactory {

    public static long recordId = 0;
    public static String recordContent = "Dummy content";
    public static String recordBase = "dummyBase";

    /**
     * Create a new dummy record.
     * <p/>
     * The public static variable {@link #recordId} is guaranteed to contain
     * the id of the last produced record.
     *
     * @return A newly created record. After this method call {@link #recordId}
     *         will contain the id of the returned record
     */
    public static Record next () {
        recordId++;
        return new Record (""+ recordId, recordBase,
                           recordContent.getBytes());

    }
}

