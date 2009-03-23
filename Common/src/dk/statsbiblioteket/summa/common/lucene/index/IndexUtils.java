/* $Id: IndexUtils.java,v 1.4 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:19 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.common.lucene.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;


import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Logs;

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
     * Stores the given ID in the Field RECORD_FIELD in the given Document.
     * If the Field already exists, it is overwritten.
     * @param id       the ID to store in the document.
     * @param document where the ID is stored.
     */
    @QAInfo(level = QAInfo.Level.NORMAL,
            state = QAInfo.State.QA_NEEDED,
            author = "te")
    public static void assignID(String id, Document document) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("assignID(" + id + ", ...) called");
        String[] ids = document.getValues(RECORD_FIELD);
        if (ids != null && ids.length == 1 && ids[0].equals(id)) {
            return;
        }
        if (ids == null || ids.length == 0) {
            log.trace("setId: Adding id '" + id + "' to Document");
        } else {
            if (id.length() == 1) {
                if (ids[0].equals(id)) {
                    return;
                }
                log.debug("Old Document id was '" + ids[0]
                          + "'. Assigning new id '" + id + "'");
            } else {
                Logs.log(log, Logs.Level.WARN,
                         "Document contains multiple RecordIDs. Clearing "
                         + "old ids and assigning id '" + id
                         + "'. Old ids:", (Object)ids);
            }
            document.removeFields(RECORD_FIELD);
        }
        document.add(new Field(RECORD_FIELD, id,
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
