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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;

import java.util.Collections;
import java.util.Map;

/**
 * Copy of Lucene's {@link org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper} to handle dynamic field
 * name matching. If a field name is not matched to any field analyzers, any prefixAnalyzer that matches the beginning
 * of the field name is returned.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DynamicPerFieldAnalyzerWrapper extends AnalyzerWrapper {
    private final Analyzer defaultAnalyzer;
    private final Map<String, Analyzer> fieldAnalyzers;
    private final Map<String, Analyzer> prefixAnalyzers;

    /**
     * Constructs with default analyzer.
     *
     * @param defaultAnalyzer Any fields not specifically
     * defined to use a different analyzer will use the one provided here.
     */
    public DynamicPerFieldAnalyzerWrapper(Analyzer defaultAnalyzer) {
        this(defaultAnalyzer, null, null);
    }

    /**
     * Constructs with default analyzer and a map of analyzers to use for
     * specific fields.
     *
     * @param defaultAnalyzer Any fields not specifically
     * defined to use a different analyzer will use the one provided here.
     * @param fieldAnalyzers a Map (String field name to the Analyzer) to be
     * used for those fields
     */
    public DynamicPerFieldAnalyzerWrapper(Analyzer defaultAnalyzer, Map<String, Analyzer> fieldAnalyzers) {
        this(defaultAnalyzer, fieldAnalyzers, null);
    }

    public DynamicPerFieldAnalyzerWrapper(
            Analyzer defaultAnalyzer, Map<String, Analyzer> fieldAnalyzers, Map<String, Analyzer> prefixAnalyzers) {
        this.defaultAnalyzer = defaultAnalyzer;
        this.fieldAnalyzers = fieldAnalyzers != null ? fieldAnalyzers : Collections.<String, Analyzer>emptyMap();
        this.prefixAnalyzers = prefixAnalyzers != null ? prefixAnalyzers : Collections.<String, Analyzer>emptyMap();
    }

    @Override
    protected Analyzer getWrappedAnalyzer(String fieldName) {
        if (fieldName == null) {
            return defaultAnalyzer;
        }

        Analyzer analyzer = fieldAnalyzers.get(fieldName);
        if (analyzer != null) {
            return analyzer;
        }

        for (Map.Entry<String, Analyzer> entry: prefixAnalyzers.entrySet()) {
            if (fieldName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return defaultAnalyzer;
    }

    @Override
    protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
        return components;
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public String toString() {
        return "PerFieldAnalyzerWrapper(analyzers=" + fieldAnalyzers + ", prefixAnalyzers=" + prefixAnalyzers
               + ", default=" + defaultAnalyzer + ")";
    }

}
