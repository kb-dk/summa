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

import dk.statsbiblioteket.util.reader.ReplaceReader;
import dk.statsbiblioteket.util.reader.CircularCharBuffer;

import java.io.FilterReader;
import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;

/**
 *
 */
public class LowerCasingReader extends ReplaceReader {

    /**
     * Creates a new filtered reader.
     *
     * @param in a Reader object providing the underlying stream.
     *
     * @throws NullPointerException if <code>in</code> is <code>null</code>
     */
    protected LowerCasingReader(Reader in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int codePoint = in.read();

        if (codePoint == -1) {
            return -1;
        }

        return Character.toLowerCase(codePoint);
    }

    @Override
    public int read(char[] buf, int offset, int count) throws IOException {
        int numRead = in.read(buf, offset, count);

        for (int i = offset+numRead; i >= offset; i--) {
            buf[i] = Character.toLowerCase(buf[i]);
        }
        
        return numRead;
    }

    public int read(CircularCharBuffer cbuf, int length) throws IOException {
        int codePoint;
        int count = 0;

        while ((codePoint = in.read()) != -1) {
            cbuf.put((char)Character.toLowerCase(codePoint));
            count++;
        }

        if (count == 0) {
            return -1;
        }

        return count;
    }

    public String transform(String s) {
        return s.toLowerCase();
    }

    public char[] transformToChars(char c) {
        return new char[]{Character.toLowerCase(c)};
    }

    public char[] transformToChars(char[] chars) {
        char[] result = new char[chars.length];

        for (int i = 0; i < chars.length; i++) {
            result[i] = Character.toLowerCase(chars[i]);
        }

        return result;
    }

    public char[] transformToCharsAllowInplace(char[] chars) {        
        for (int i = 0; i < chars.length; i++) {
            chars[i] = Character.toLowerCase(chars[i]);
        }

        return chars;
    }

    /**
     * Prepare the reader for lower casing the character stream from another
     * reader.
     *
     * @param in the new reader to read character data from
     * @return always returns {@code this}
     */
    public LowerCasingReader setSource(Reader in) {
        this.in = in;
        return this;
    }

    @Override
    @SuppressWarnings({"CloneDoesntCallSuperClone",
            "CloneDoesntDeclareCloneNotSupportedException"})
    public LowerCasingReader clone() {
        return new LowerCasingReader(new StringReader(""));
    }
}

