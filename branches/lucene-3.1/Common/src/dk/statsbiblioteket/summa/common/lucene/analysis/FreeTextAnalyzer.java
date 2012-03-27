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


/**
 * The FreeTextAnalyzer is a standard SummaAnayzer with a 'default'
 * constructor. The analyzer will translitterate, lowercase and tokenize
 * the text pasred through the analyzer.
 *
 * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaAnalyzer
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class FreeTextAnalyzer extends SummaAnalyzer {

    /**
     * Makes a FreeTextAnalyzer, which is identical whit a SummaAnalyzer
     * where no additional rules are specified.
     *
     * @see SummaAnalyzer#SummaAnalyzer(String, boolean, String, boolean, boolean)
     */
    public FreeTextAnalyzer(){
        super("", true, Rules.RELAXED_REPLACE_RULES, true, true);
    }

}




