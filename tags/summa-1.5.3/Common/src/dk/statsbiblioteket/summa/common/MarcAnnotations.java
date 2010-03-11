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
package dk.statsbiblioteket.summa.common;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Marc-specific annotations used for information-exchange between the ingest
 * and the index-steps for Marc-records.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MarcAnnotations {
    private static Log log = LogFactory.getLog(MarcAnnotations.class);

    /**
     * The key for the multi volume type for Marc-Records.
     */
    public static final String META_MULTI_VOLUME_TYPE = "MarcMultiVolumeType";

    /**
     * The different types of Marc multi volumes.
     */
    public static enum MultiVolumeType {
        HOVEDPOST, SEKTION, BIND, NOTMULTI;

        public static MultiVolumeType fromString(String str) {
            if ("hovedpost".equals(str)) {
                return HOVEDPOST;
            }
            if ("sektion".equals(str)) {
                return SEKTION;
            }
            if ("bind".equals(str)) {
                return BIND;
            }
            if (!"notmulti".equals(str)) {
                log.warn("Unable to recognize '" + str
                         + "'. Returning NOTMULTI");
            }
            return NOTMULTI;
        }

        public String toString() {
            switch (this) {
                case HOVEDPOST: return "hovedpost";
                case SEKTION: return "sektion";
                case BIND: return "bind";
                case NOTMULTI: return "notmulti";
                default: {
                    log.warn("Unknown enum-value " + super.toString());
                    return "unknown";
                }
            }
        }
    }

}

