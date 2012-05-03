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

import java.io.Serializable;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A standard pair that compares on key.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Pair <T extends Comparable<T> , S>
        implements Comparable<Pair<T, S>>, Serializable {
    private static final long serialVersionUID = 340404402L;
    protected T key;
    protected S value;

    public Pair(T key, S value) {
        this.key = key;
        this.value = value;
    }
    public int compareTo(Pair<T, S> o) {
        if (!o.getClass().equals(getClass())) {
            return 0;
        }
        if (!o.getKey().getClass().equals(key.getClass())) {
            return 0;
        }
        return key.compareTo(o.getKey());
    }
    public void setKey(T key) {
        this.key = key;
    }
    public T getKey() {
        return key;
    }
    public void setValue(S value) {
        this.value = value;
    }
    public S getValue() {
        return value;
    }
    public String toString() {
        //noinspection ObjectToString
        return value + "(" + key + ")";
    }
}




