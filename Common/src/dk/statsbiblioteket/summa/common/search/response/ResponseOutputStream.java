/* $Id: ResponseOutputStream.java,v 1.2 2007/10/04 13:28:18 te Exp $
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
 * CVS:  $Id: ResponseOutputStream.java,v 1.2 2007/10/04 13:28:18 te Exp $
 */
package dk.statsbiblioteket.summa.common.search.response;

import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

/*
    SummaSocketSearch. Compact, tagged, byte-oriented protocol.

    The general datagram consists of an ID (Java integer) followed by
    ID-specific content. Changes to the different datagram formats are
    forbidden: Create new datagram formats instead.

    Strings are represented as a length (Java integer) and a UTF-8 bytestream.
    Bytes are represented as a length (Java integer) and a bytestream.
    Booleans are represented as Java integers (0 = false, 1 = true).
    PrimitiveComparables are either floats, Strings or integers.
                         They are specified by
                         float:  0x00000001
                         String: 0x00000002
                         int:    0x00000003

    - search
    Requests a search with the given parameters.
    Format: 0x00000001 Query(String) maxResultSize(int) Sortkey(String)
                       ReverseSort(bool) Filter(String) MaxScore(float)


    - abort
    Aborts the previously requested search. If the search has already finished,
    no action is taken.
    Format: 0x00000002


    - search_response
    The response from a search. This states the number of hits, followed by
    the scores, followed by the sortkeys and contents of the hits. The
    transfer of the sortkeys and the content can be aborted by an EOT.
    The response is always terminated by a EOT.

    If an error occurs during transfer, the connection should be closed.

    Format: 0x00000003 NumberOfHits(int) primitiveComparableType
                       sortValue(PrimitiveComparable)*
                       (Content(Bytes) | EOT) *
                       EOT, if the search_response was not aborted by EOT
    * = repeat NumberOfHits times.


    - error
    Send instead of a search_response, if an error occured before the
    search_response could be send.

    Format: 0x00000004 ErrorID(int) ErrorMessage(String)


    - EOT (End Of Transmission)

    Signals that the search_response has finished. The (not 0) marker has
    been chosen as it is an illegal value for String length.

    Format: 0xFFFFFFFF

     */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ResponseOutputStream implements ResponseWriter {
    private static final Log log = LogFactory.getLog(ResponseOutputStream.class);

    public static enum COMMAND {
        search   { int getTag() { return 0x00000001; } },
        abort    { int getTag() { return 0x00000002; } },
        response { int getTag() { return 0x00000003; } },
        error    { int getTag() { return 0x00000004; } },
        eot      { int getTag() { return 0xFFFFFFFF; } };
        abstract int getTag();
        public COMMAND getEnum(int tag) {
            switch (tag) {
                case 1: return search;
                case 2: return abort;
                case 3: return response;
                case 4: return error;
                case 0xFFFFFFFF: return eot;
                default: return null;
            }
        }
    }

    private enum STATE {
        readyForScores,
        readyForContent
    }

    private STATE state;
    private int contentLeft;

    private DataOutputStream out;
    public ResponseOutputStream(OutputStream out) {
        this.out = new DataOutputStream(out);
        state = STATE.readyForScores;
    }

    public void initiateResponse(float[] sortValues) throws IOException {
        initiateResponseHeader(PRIMITIVE_COMPARABLE._float, sortValues.length);
        for (float value: sortValues) {
            put(value);
        }
    }

    public void initiateResponse(String[] sortValues) throws IOException {
        initiateResponseHeader(PRIMITIVE_COMPARABLE._string, sortValues.length);
        for (String value: sortValues) {
            put(value);
        }
    }

    public void error(int errorID, String message) throws IOException {
        if (state != STATE.readyForScores) {
            throw new IllegalStateException("Not ready for error. "
                                            + "The state is " + state);
        }
        put(COMMAND.error);
        put(errorID);
        put(message);
    }

    public void initiateResponse(int[] sortValues) throws IOException {
        initiateResponseHeader(PRIMITIVE_COMPARABLE._int, sortValues.length);
        for (int value: sortValues) {
            put(value);
        }
    }

    private void initiateResponseHeader(PRIMITIVE_COMPARABLE type, int length)
            throws IOException {
        if (state != STATE.readyForScores) {
            throw new IllegalStateException("Not ready for initial response. "
                                            + "The state is " + state);
        }
        put(COMMAND.response);
        put(length);
        put(type);
        contentLeft = length;
        state = STATE.readyForContent; // Jumping the gun a little...
    }

    public void putContent(byte[] content) throws IOException {
        if (state != STATE.readyForContent) {
            throw new IllegalStateException("Not ready for content. "
                                            + "The state is " + state);
        }
        if (contentLeft-- == 0) {
            throw new IllegalStateException("The data from initialResponse "
                                            + "does not allow for any more "
                                            + "content to be transmitted");
        }
        put(content);
    }

    public void endResponse() throws IOException {
        if (state != STATE.readyForContent) {
            throw new IllegalStateException("Not ready for EOF. "
                                            + "The state is " + state);
        }
        put(COMMAND.eot);
        state = STATE.readyForScores;
    }

    private void put(String string) throws IOException {
        try {
            put(string.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            log.fatal("getBytes(\"utf-8\") must be supported on String", e);
            System.exit(-1);
        }
    }
    private void put(float value) throws IOException {
        out.writeFloat(value);
    }
    private void put(int value) throws IOException {
        out.writeInt(value);
    }
    private void put(byte[] bytes) throws IOException {
        out.writeInt(bytes.length);
        out.write(bytes);
    }
    private void put(COMMAND command) throws IOException {
        out.writeInt(command.getTag());
    }
    private void put(PRIMITIVE_COMPARABLE pr) throws IOException {
        out.writeInt(pr.getTag());
    }
}
