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
package dk.statsbiblioteket.summa.common.solr.analysis;

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.util.CharacterUtils;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;

/**
 * Solr's {@link org.apache.lucene.analysis.core.LowerCaseFilter} converted to a
 * {@link org.apache.lucene.analysis.CharFilter} so that it can be applied before
 * {@link org.apache.lucene.analysis.charfilter.MappingCharFilter}s and similar.
 */
public class LowerCaseCharFilter extends CharFilter {
    private final CharacterUtils charUtils;

    protected LowerCaseCharFilter(Version matchVersion, Reader in) {
        super(in);
        charUtils = CharacterUtils.getInstance(matchVersion);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = directRead(cbuf, off, len);
        if (read == -1) {
            return -1;
        }
        String sBuf = new String(cbuf, off, read);
        for (int i = off ; i < off+read ; ) {
            // This assumes that the number of chars is always equal for upper- and lower-case codepoints.
            // The Solr code makes this assumption, but it would be nice to verify if it is true.
            int oldI = i;
            i += Character.toChars(
                    Character.toLowerCase(
                        charUtils.codePointAt(sBuf, i-off)), cbuf, i);
            if (oldI == i) {
                throw new IllegalStateException(
                    "Converting char at position " + i + " (" + cbuf[i] + ") did not cause progress to the position");
            }
        }
        return read;
    }

    private int directRead(char[] cbuf, int off, int len) throws IOException {
        int read = 0;
        int c;
        while (read < len && (c = input.read()) != -1) {
            cbuf[off + read] = (char)c;
            read++;
        }
        return read == 0 ? -1 : read;
    }

    @Override
    protected int correct(int i) {
        return i; // TODO: Check what the correct behaviour is here
    }

    @Override
    public void close() throws IOException {
        input.close();
        super.close();
    }
}
