/* $Id$
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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Thrown in case of problems with a specific Payload.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PayloadException extends Exception {
    private Payload payload = null;

    public PayloadException() {
        super();
    }

    public PayloadException(String message) {
        super(message);
    }

    public PayloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public PayloadException(Throwable cause) {
        super(cause);
    }

    public PayloadException(Payload payload) {
        super();
        this.payload = payload;
    }

    public PayloadException(String message, Payload payload) {
        super(message);
        this.payload = payload;
    }

    public PayloadException(String message, Throwable cause, Payload payload) {
        super(message, cause);
        this.payload = payload;
    }

    public PayloadException(Throwable cause, Payload payload) {
        super(cause);
        this.payload = payload;
    }

    public Payload getPayload() {
        return payload;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (payload == null ? "" : payload.toString());
    }
}
