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
 * Trivial implementation of generic triple.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class Triple<T, S, R> {
    protected T value1;
    protected S value2;
    protected R value3;

    public Triple(T value1, S value2, R value3) {
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
    }

    @Override
    public int hashCode() {
        return value1.hashCode() + value2.hashCode() + value3.hashCode();
    }

    public T getValue1() {
        return value1;
    }

    public S getValue2() {
        return value2;
    }

    public R getValue3() {
        return value3;
    }

    public String toString() {
        //noinspection ObjectToString
        return "Triple(" + value1 + ", " + value2 + ", " + value3 + ")";
    }

}