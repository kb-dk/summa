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

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Tracks memory usage of Strings.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Check for 64-bit (but be aware of 32bit-pointer-mode)
public class StringTracker extends ResourceTrackerImpl<String> {

    /**
     * Basic overhead for a single String.
     * @see <a href="http://www.javamex.com/tutorials/memory/string_memory_usage.shtml">
     * String Memory Usage</a>
     */
    public static final int SINGLE_ENTRY_OVERHEAD = 38; // 32 bit

    public StringTracker(long maxCountLimit, long memLimit) {
        super(maxCountLimit, memLimit);
    }

    public StringTracker(long minCountLimit, long maxCountLimit, long memLimit) {
        super(minCountLimit, maxCountLimit, memLimit);
    }

    @Override
    public long calculateBytes(String element) {
        return element == null ? 0
               : element.length() * 2 + SINGLE_ENTRY_OVERHEAD;
    }
}

