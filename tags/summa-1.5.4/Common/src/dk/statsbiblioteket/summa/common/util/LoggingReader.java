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

import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;

/**
 * Upon construction, the reader empties the given source, logs the content as
 * trace and stores the full content of the Reader internally. It then acts as
 * a standard reader for the content.
 * </p><p>
 * This behaviour is meant for debugging.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LoggingReader extends Reader {
    private Reader source;

    public LoggingReader(Log log, String message, Reader source) {
        System.out.println("***");
        if (!log.isTraceEnabled()) {
            this.source = source;
            return;
        }
        System.out.println("+++");
        try {
            String content = Strings.flush(source);
            System.out.println("----");
            this.source = new StringReader(content);
            log.trace(message + content);
        } catch (IOException e) {
            throw new RuntimeException("Unable to flush Reader " + source);
        }
    }

    /* Direct delegation to source */

    @Override
    public int read(CharBuffer target) throws IOException {
        return source.read(target);
    }

    @Override
    public int read() throws IOException {
        return source.read();
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        return source.read(cbuf);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return source.read(cbuf, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return source.skip(n);
    }

    @Override
    public boolean ready() throws IOException {
        return source.ready();
    }

    @Override
    public boolean markSupported() {
        return source.markSupported();
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        source.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        source.reset();
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}

