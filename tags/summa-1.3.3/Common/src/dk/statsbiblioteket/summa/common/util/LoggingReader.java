/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;

import java.io.Reader;
import java.io.IOException;
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
