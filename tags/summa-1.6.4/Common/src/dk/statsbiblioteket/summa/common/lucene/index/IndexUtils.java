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
package dk.statsbiblioteket.summa.common.lucene.index;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * IndexUtils is a set of static common used methods used for manipulating or
 * building the index.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IndexUtils {

    private static Log log = LogFactory.getLog(IndexUtils.class);
    /**
     * The field-name for the Lucene Field containing the Record ID for the
     * Document. All Document in Summa Lucene Indexes must have one and only
     * one RecordID stored and indexed.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String RECORD_FIELD = "recordID";

    /**
     * The field-name for the Lucene Field containing the Record base for the
     * Document. All Document in Summa Lucene Indexes must have one and only
     * one RecordBase stored and indexed.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String RECORD_BASE = "recordBase";

    /**
     * Ensures that one and only one recordID and one and only one recordBase
     * is assigned to the Lucene Document, taken from record.
     * </p><p>
     * If no Document is present, a warning is logged.
     * @param payload    the Payload with the Record with the ID and the base.
     */
    @QAInfo(level = QAInfo.Level.NORMAL,
            state = QAInfo.State.QA_NEEDED,
            author = "te")
    public static void assignBasicProperties(Payload payload) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("assignBasicProperties(%s)", payload));
        }
        if (payload.getData(Payload.LUCENE_DOCUMENT) == null) {
            String message = "No Document present, so no basic properties can"
                               + " be assigned";
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess("IndexUtils.assignBasicProperties",
                               message, Logging.LogLevel.DEBUG, payload);
            log.warn(String.format(message + ": " + payload));
            return;
        }
        Document document = (Document)payload.getData(Payload.LUCENE_DOCUMENT);

        String id = payload.getId();
        assignSingleField(document, payload, RECORD_FIELD, id);

        if (payload.getRecord() == null) {
            String message = "No Record present, so no base can be assigned";
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess("IndexUtils.assignBasicProperties",
                               message, Logging.LogLevel.DEBUG, payload);
            log.debug(String.format(message + ": " + payload));
            return;
        }
        String base = payload.getRecord().getBase();
        assignSingleField(document, payload, RECORD_BASE, base);
    }

    public static void assignSingleField(Document document, Payload payload,
                                          String field, String term) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Assigning %s:%s to %s",
                                    field, term, payload));
        }
        String[] fields = document.getValues(field);
        if (fields != null && fields.length > 1) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("The " + field + " for " + payload + " was already "
                      + "assigned. Clearing and re-adding");
            document.removeFields(field);
        }
        document.add(new Field(field, term,
                               Field.Store.YES,
                               Field.Index.NOT_ANALYZED));
    }

    /**
     * Extracts the ID from a Document, if it is present. In case of multiple
     * ID's, the first one is returned. The ID is stored in the field
     * RECORD_FIELD.
     * @param document where to extract the ID.
     * @return the ID for the Document if present, else null.
     */
    @QAInfo(level = QAInfo.Level.NORMAL,
            state = QAInfo.State.QA_NEEDED,
            author = "te")
    public String getID(Document document) {
        String[] ids = document.getValues(RECORD_FIELD);
        if (ids != null && ids.length > 0) {
            if (ids.length > 1) {
                Logs.log(log, Logs.Level.WARN, "Multiple RecordIDs defined "
                                               + "in Document '"
                                               + this + "'. Returning first"
                                               + " RecordID out of: ",
                         (Object)ids);
            }
            return ids[0];
        }
        log.trace("getID: No ID found in document");
        return null;
    }
}

