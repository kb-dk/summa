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
package org.apache.lucene.analysis;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Reader;

/**
 * Needs to be in the analysis package due to protected access to {@link #getPositionIncrementGap(String)}..
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FieldSeparatingAnalyzer extends Analyzer {
    private Analyzer underlyingAnalyzer;

    public FieldSeparatingAnalyzer(Analyzer underlyingAnalyzer) {
        this.underlyingAnalyzer = underlyingAnalyzer;
    }

    @Override
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return underlyingAnalyzer.createComponents(fieldName, reader);
    }

    @Override
    public Reader initReader(String field, Reader reader) {
        return underlyingAnalyzer.initReader(field, reader);
    }

    @Override
    public int getPositionIncrementGap(String fieldName) {
        return 100;
    }

    @Override
    public int getOffsetGap(String field) {
        return underlyingAnalyzer.getOffsetGap(field);
    }

    @Override
    public void close() {
        underlyingAnalyzer.close();
    }

    @Override
    public String toString() {
        return "FieldSeparatingAnalyzer";
    }
}
