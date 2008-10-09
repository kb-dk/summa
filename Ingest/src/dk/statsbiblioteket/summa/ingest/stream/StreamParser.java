/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.ingest.stream;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configurable;

/**
 * Parses a stream (provided by a Payload) into multiple Records wrapped in
 * Payloads. Normally used by  {@link StreamController}.
 * A StreamParser is reusable and is cleared and initialized by {@link #open}.
 * </p><p>
 * Note: Implemenetations must return false for hasNext if open has not been
 * called.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public interface StreamParser extends Iterator<Payload>, Configurable {
    /**
     * Clears and initializes the parser with the stream from the given Payload.
     * @param streamPayload the Payload with the stream to parse.
     */
    public void open(Payload streamPayload);

    /**
     * If a parsing is underway, it is stopped and any queued Payloads are
     * discarded. The StreamParser can be reused after close.
     */
    public void stop();

    /**
     * Shuts down the parser, stopping running Threads and the like. The state
     * of the parser is undefined after close and it is not guaranteed that it
     * can be reused.
     */
    public void close();
}
