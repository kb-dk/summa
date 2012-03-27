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
package dk.statsbiblioteket.summa.common.pool;

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.LineReader;

/**
 * Handles value-specific conversions. To be used together with the pool
 * framework.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface ValueConverter<E extends Comparable> {
    /**
     * Converts the given value to an array of bytes, suitable for storage.
     * The conversion should mirror {@link #bytesToValue}.
     * @param value the value to converto to bytes.
     * @return bytes based on values.
     */
    abstract byte[] valueToBytes(E value);

    /**
     * Converts the given bytes to a value. The conversion should mirror
     * {@link #valueToBytes}.
     * @param buffer the bytes to convert.
     * @param length the number of bytes to convert.
     * @return a value constructed from the given bytes.
     */
    abstract E bytesToValue(byte[] buffer, int length);
}




