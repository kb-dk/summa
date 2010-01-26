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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * Adds the given key and value to the meta data for the Payloads.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class DummyFilter extends ObjectFilterImpl {
//    private static Log log = LogFactory.getLog(DummyFilter.class);

    public static final String CONF_KEY =   "key";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String CONF_VALUE = "value";

    private String key;
    private String value;

    public DummyFilter(Configuration conf) {
        super(conf);
        key =   conf.getString(CONF_KEY);
        value = conf.getString(CONF_VALUE);
    }

    @Override
    protected boolean processPayload(Payload payload) {
        payload.getData().put(key, value);
        return true;
    }
}

