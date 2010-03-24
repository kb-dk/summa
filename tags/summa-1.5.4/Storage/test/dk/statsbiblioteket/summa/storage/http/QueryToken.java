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
package dk.statsbiblioteket.summa.storage.http;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.*;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.storage.http.QueryToken
 *
 * @author mke
 * @since Sep 10, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class QueryToken implements Cloneable {
    private String key;
    private String value;

    public QueryToken(String key, String value) {
        reset(key, value);
    }

    public void reset(String key, String value) {
        if (key == null) {
            throw new NullPointerException("'key' for QueryToken is null");
        } else if (value == null) {
            throw new NullPointerException("'value' for QueryToken is null");
        }
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public QueryToken clone() throws CloneNotSupportedException {
        return (QueryToken)super.clone();
    }

    @Override
    public boolean equals(Object other) {
        try {
            QueryToken tok = (QueryToken)other;
            return tok != null &&
                   (this.key.equals(tok.key) && (this.value.equals(tok.value)));
        } catch (ClassCastException e) {
            return false;
        }

    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}

