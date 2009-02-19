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
package dk.statsbiblioteket.summa.common;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

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
