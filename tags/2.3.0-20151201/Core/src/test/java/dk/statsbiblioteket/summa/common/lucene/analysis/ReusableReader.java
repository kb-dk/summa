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
package dk.statsbiblioteket.summa.common.lucene.analysis;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.io.Reader;

/**
 * Helper class for testing reuse of Analyzers.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReusableReader extends Reader {
    private CharSequence source = null;
    private int pos = 0;

    public ReusableReader(CharSequence source) {
        this.source = source;
    }

    @Override
    public synchronized int read(char[] cbuf, int off, int len) throws IOException {
        if (rest() == 0) {
            return -1;
        }
        int read = 0;
        for (int i = 0 ; i < len && rest() > 0 ; i++) {
            cbuf[off + i] = source.charAt(pos++);
            read++;
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        pos = 0;
    }

    @Override
    public void reset() throws IOException {
        pos = 0;
    }

    @Override
    public long skip(long n) throws IOException {
        if (rest() >= n) {
            pos += n;
            return n;
        }
        int r = rest();
        pos = source.length();
        return r;
    }

    public synchronized void set(CharSequence source) {
        this.source = source;
        pos = 0;
    }

    private int rest() {
        return source == null ? 0 : source.length() - pos;
    }
}
