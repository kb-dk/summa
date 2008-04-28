/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.control.api;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;

/**
 * Generic object to represent the status of a ClientManager service or client.
 * @see Service
 * @see dk.statsbiblioteket.summa.control.client.Client
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Status implements Serializable {
    public static enum CODE {
        /**
         * The object does not exist, but can be instantiated. This state
         * is typically returned by some service proxying the object.
         * For example a ClientManager Client managing a service returns this
         * state if the service is deployed, but not running.  
         */
        not_instantiated,
        /**
         * This object has been constructed and is ready for start.
         */
        constructed,
        /**
         * The object is initialising its state.
         */
        startingUp,
        /**
         * The object is ready for action.
         */
        idle,
        /**
         * The object state is running - further requests to the object might
         * be delayed.
         */
        running,
        /**
         * The object has experienced a failure and is recovering.
         */
        recovering,
        /**
         * The object has been intentionally terminated.
         */
        stopping,
        /**
         * The object has been intentionally terminated.
         */
        stopped,
        /**
         * The structure of the object has been compromised, so that it
         * can no longer function. Termination is required.
         */
        crashed
    }

    private CODE code;
    private String message;

    /**
     * Create a new Status object.
     * @param code Status code for status object
     * @param message Message with explanatory text
     */
    public Status(CODE code, String message) {
        this.code = code;
        this.message = message;
    }

    public String toString() {
        return "<"+code+":"+message+">";
    }

    public CODE getCode () {
        return code;
    }
}
