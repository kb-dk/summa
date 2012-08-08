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

import org.apache.lucene.analysis.CharStream;
import org.apache.lucene.analysis.charfilter.CharFilter;
import org.apache.lucene.analysis.util.CharacterUtils;
import org.apache.lucene.util.Version;

import java.io.IOException;

/**
 * Solr's {@link org.apache.lucene.analysis.core.LowerCaseFilter} converted to a
 * {@link org.apache.lucene.analysis.charfilter.CharFilter} so that it can be applied before
 * {@link org.apache.lucene.analysis.charfilter.MappingCharFilter}s and similar.
 */
public class LowerCaseCharFilter extends CharFilter {
    private final CharacterUtils charUtils;

    protected LowerCaseCharFilter(Version matchVersion, CharStream in) {
        super(in);
        charUtils = CharacterUtils.getInstance(matchVersion);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = super.read(cbuf, off, len);
        for (int i = off ; i < read ; ) {
            // This assumes that the number of chars is always equal for upper- and lower-case codepoints.
            // The Solr code makes this assumption, but it would be nice to verify if it is true.
            i += Character.toChars(
                    Character.toLowerCase(
                        charUtils.codePointAt(cbuf, i)), cbuf, i);
        }
        return read;
    }
}
