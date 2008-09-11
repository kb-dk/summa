/* $Id: ResponseInputStream.java,v 1.2 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:18 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: ResponseInputStream.java,v 1.2 2007/10/04 13:28:18 te Exp $
 */
package dk.statsbiblioteket.summa.common.search.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ResponseInputStream implements ResponseReader {
    private static final Log log = LogFactory.getLog(ResponseInputStream.class);

    private enum STATE {
        readyForStart,
        errorReceived,
        readyForContent
    }
    private STATE state;

    private DataInputStream in;

    private int contentLeft;
    private Response.PRIMITIVE_COMPARABLE sortKey;
    private float[] floatSortValues;
    private String[] stringSortValues;
    private int[] intSortValues;

    private int errorID;
    private String errorMessage;

    private byte[] cachedContent = null;

    public ResponseInputStream(InputStream in) {
        this.in = new DataInputStream(in);
        state = STATE.readyForStart;
    }

    /**
     * Checks to see whether an error or a proper response was received.
     * Calling this method multiple times results in multiple queries (polls
     * from the stream), as long as the result is true.
     * @return true is an error was received.
     * @throws IOException if the state could not be determined.
     */
    public boolean isError() throws IOException {
        if (state == STATE.readyForStart) {
            pollStart();
        }
        if (state == STATE.errorReceived) {
            state = STATE.readyForContent;
            return true;
        }
        return false;
    }

    public int getErrorID() throws IOException {
        if (state != STATE.errorReceived) {
            throw new IllegalStateException("No error has been received");
        }
        return errorID;
    }

    public String getErrorMessage() throws IOException {
        if (state != STATE.errorReceived) {
            throw new IllegalStateException("No error has been received");
        }
        return errorMessage;
    }

    private synchronized void pollStart() throws IOException {
        int c = getInteger();
        ResponseOutputStream.COMMAND command =
                ResponseOutputStream.COMMAND.eot.getEnum(c);
        if (command == null) {
            log.warn("Unknown command in pollStart: " + c);
            throw new IOException("Unknown command: " + c);
        }
        switch (command) {
            case response:
                contentLeft = getInteger();
                int key = getInteger();
                sortKey = Response.PRIMITIVE_COMPARABLE.
                        _float.getEnum(key);
                if (sortKey == null) {
                    String e = "Unknown sortKey: " + key;
                    log.warn(e);
                    throw new IOException(e);
                }
                getSortValues();
                state = STATE.readyForContent;
                break;
            case error:
                state = STATE.errorReceived;
                errorID = getInteger();
                errorMessage = getString();
                break;
            default:
                log.warn("Received unknown command in pollStart: " + command);
                throw new IOException("Unknown command: " + command);
        }
    }

    private void getSortValues() throws IOException {
        switch (sortKey) {
            case _float:
                floatSortValues = new float[contentLeft];
                for (int i = 0 ; i < contentLeft ; i++) {
                    floatSortValues[i] = getFloat();
                }
                break;
            case _string:
                stringSortValues = new String[contentLeft];
                for (int i = 0 ; i < contentLeft ; i++) {
                    stringSortValues[i] = getString();
                }
                break;
            case _int:
                intSortValues = new int[contentLeft];
                for (int i = 0 ; i < contentLeft ; i++) {
                    intSortValues[i] = getInteger();
                }
                break;
            default:
                throw new IOException("SortKey of type " + sortKey
                                      + " not supported");
        }
    }

    public synchronized Response.PRIMITIVE_COMPARABLE getSortKey()
            throws IOException {
        if (state != STATE.readyForContent) {
            throw new IllegalStateException("The state should be "
                                            + STATE.readyForContent
                                            + " when requesting the sortKey."
                                            + "It was " + state);
        }
        return sortKey;
    }

    public float[] getFloatSortValues() throws IOException,
                                               IllegalStateException {
        if (sortKey != Response.PRIMITIVE_COMPARABLE._float) {
            throw new IllegalStateException("The sortKey should be "
                                    + Response.PRIMITIVE_COMPARABLE._float
                                    + " but was " + sortKey);
        }
        return floatSortValues;
    }

    public String[] getStringSortValues() throws IOException,
                                                 IllegalStateException {
        if (sortKey != Response.PRIMITIVE_COMPARABLE._string) {
            throw new IllegalStateException("The sortKey should be "
                                   + Response.PRIMITIVE_COMPARABLE._string
                                   + " but was " + sortKey);
        }
        return stringSortValues;
    }

    public int[] getIntegerSortValues() throws IOException,
                                               IllegalStateException {
        if (sortKey != Response.PRIMITIVE_COMPARABLE._int) {
            throw new IllegalStateException("The sortKey should be "
                                    + Response.PRIMITIVE_COMPARABLE._int
                                    + " but was " + sortKey);
        }
        return intSortValues;
    }

    public boolean hasContent() throws IOException {
        cachedContent = getNextContent();
        return cachedContent != null;
    }

    public byte[] getNextContent() throws IOException {
        if (state != STATE.readyForContent) {
            throw new IllegalStateException("The state should be "
                                            + STATE.readyForContent
                                            + " but was " + state);
        }

        if (cachedContent != null) {
            // Already read, just return it
            byte[] result = cachedContent;
            //noinspection AssignmentToNull
            cachedContent = null;
            return result;
        }

        int token = getInteger();

        if (token == ResponseOutputStream.COMMAND.eot.getTag()) {
            // End of transmission
            state = STATE.readyForStart;
            return null;
        }

        // Real content
        byte[] result = new byte[token];
        in.readFully(result);
        return result;
    }

    /* ******************** Low level ********************* */

    private int getInteger() throws IOException {
        return in.readInt();
    }
    private float getFloat() throws IOException {
        return in.readFloat();
    }
    private String getString() throws IOException {
        byte[] bytes = getBytes();
        return new String(bytes, "utf-8");
    }

    /**
     * 1. The length is read (int).
     * 2. length number of bytes are read and returned.
     * @return the next bytes.
     * @throws IOException if the next bytes could not be received.
     */
    private byte[] getBytes() throws IOException {
        byte[] result = new byte[getInteger()];
        in.readFully(result);
        return result;
    }

}



