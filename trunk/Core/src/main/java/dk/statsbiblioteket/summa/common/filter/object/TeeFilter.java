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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Creates multiple copies of incoming Records, one for each base/idprefix combination.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class TeeFilter extends MultiFilterBase {
    private static Log log = LogFactory.getLog(TeeFilter.class);

    /**
     * The recordBases for the outgoing Records.
     * For each incoming Record, there will be #tee.bases * #tee.idPrefixes outgoing records.
     * </p><p>
     * Half mandatory: Either tee.bases or tee.idprefixes must be defined.
     */
    public static final String CONF_NEW_BASES = "tee.newbases";

    /**
     * The idPrefixes for the outgoing Records.
     * For each incoming Record, there will be #tee.bases * #tee.idPrefixes outgoing records.
     * </p><p>
     * Half mandatory: Either tee.bases or tee.idprefixes must be defined.
     */
    public static final String CONF_ID_PREFIXES = "tee.idprefixes";

    /**
     * If true, the content of Records is referenced, meaning that a change to one will reflect in the others.
     * If false, content is copied, making the produced Records fully independent. Copying the content can
     * be costly in terms of performance and/or memory, but is a safe process.
     */
    public static final String CONF_REFERENCE_CONTENT = "tee.referencecontent";
    public static final boolean DEFAULT_REFERENCE_CONTENT = true;

    public final List<String> recordBases;
    public final List<String> idPrefixes;
    public final boolean referenceContent;

    /**
     * Constructs a new replace filter, with the specified configuration.
     *
     * @param conf The configuration to construct this filter.
     */
    public TeeFilter(Configuration conf) {
        super(conf);
        feedback = false;
        recordBases = getStrings(conf, CONF_NEW_BASES);
        idPrefixes = getStrings(conf, CONF_ID_PREFIXES);
        referenceContent = conf.getBoolean(CONF_REFERENCE_CONTENT, DEFAULT_REFERENCE_CONTENT);
        if (recordBases.isEmpty() && idPrefixes.isEmpty()) {
            throw new ConfigurationException(
                    "Either " + CONF_NEW_BASES + " or " + CONF_ID_PREFIXES + " must be specified and have content");
        }
        log.info("Created " + this);
    }

    private List<String> getStrings(Configuration conf, String key) {
        if (conf.containsKey(key)) {
            return conf.getStrings(key);
        }
        return Collections.emptyList();
    }

    @Override
    protected void process(Payload payload) throws PayloadException {
        for (String base: recordBases.isEmpty() ? Arrays.asList(payload.getRecord().getBase()) : recordBases) {
            for (String prefix: idPrefixes.isEmpty() ? Arrays.asList("") : idPrefixes) {
                Record newRecord = RecordUtil.clone(payload.getRecord(), referenceContent);
                newRecord.setBase(base);
                newRecord.setId(prefix + newRecord.getId());
                deliver(new Payload(newRecord));
            }
        }
    }

    @Override
    public String toString() {
        return "TeeFilter(bases=[" + Strings.join(recordBases) + "], idPrefixes=[" + Strings.join(idPrefixes) + "])";
    }
}