/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.ByteArrayInputStream;

/**
 * Creates a Stream-Payload by wrapping the content if Records in received
 * Payloads
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "te")
public class RecordToStreamFilter extends ObjectFilterImpl {

    /**
     * If true, a warning is logged when a Payload with no Stream is
     * encountered. If false, nothing happens.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_WARN_ON_NO_CONTENT =
            "common.recordtostream.warnonnocontent";
    public static final boolean DEFAULT_WARN_ON_NO_CONTENT = true;

    private boolean warnOnNoContent = DEFAULT_WARN_ON_NO_CONTENT;

    public RecordToStreamFilter(Configuration conf) {
        super(conf);
        warnOnNoContent = conf.getBoolean(
                CONF_WARN_ON_NO_CONTENT, warnOnNoContent);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getRecord() == null
            || payload.getRecord().getContent(false) == null) {
            if (warnOnNoContent) {
                //noinspection DuplicateStringLiteralInspection
                Logging.logProcess("RecordtoStreamFilter",
                                   "No Content in Record",
                                   Logging.LogLevel.WARN, payload);
            }
            return true;
        }
        ByteArrayInputStream out = new ByteArrayInputStream(
                payload.getRecord().getContent(true));
        payload.setID(payload.getId()); // We discard the Record with this
        payload.setStream(out);
        //noinspection DuplicateStringLiteralInspection
        Logging.logProcess("RecordtoStreamFilter",
                           "Mapped content to stream",
                           Logging.LogLevel.TRACE, payload);
        return true;
    }

}