/* $Id: SimpleResponse.java,v 1.2 2007/10/04 13:28:18 te Exp $
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
 * CVS:  $Id: SimpleResponse.java,v 1.2 2007/10/04 13:28:18 te Exp $
 */
package dk.statsbiblioteket.summa.common.search.response;

import java.io.IOException;
import java.util.ArrayList;

import dk.statsbiblioteket.summa.common.search.response.ResponseReader;
import dk.statsbiblioteket.summa.common.search.response.ResponseWriter;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Simple implementation of the Response reader and writer.
 * It is not guaranteed that the methods fail, if they do no make sense
 * (e.g. requesting contents, when an error was given or requesting float
 * sort values, when String sort values was given).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SimpleResponse implements ResponseReader, ResponseWriter {
    private boolean error = false;
    private int errorID;
    private String errorMessage;

    private PRIMITIVE_COMPARABLE sortKey;
    private float[]  floatSortValues;
    private String[] stringSortValues;
    private int[]    intSortValues;

    private ArrayList<byte[]> contents;
    private int nextcontentOut = 0;

    /* ****************************** Reader ************************** */

    public boolean isError() throws IOException {
        return error;
    }

    public int getErrorID() throws IOException {
        if (!error) {
            throw new IllegalStateException("No error stated");
        }
        return errorID;
    }

    public String getErrorMessage() throws IOException {
        if (!error) {
            throw new IllegalStateException("No error stated");
        }
        return errorMessage;
    }

    public synchronized PRIMITIVE_COMPARABLE getSortKey() throws IOException {
        if (error) {
            throw new IllegalStateException("This response in an" +
                                            " error-response");
        }
        return sortKey;
    }

    public synchronized float[] getFloatSortValues() throws IOException,
                                               IllegalStateException {
        checkSortValues(PRIMITIVE_COMPARABLE._float, floatSortValues);
        return floatSortValues;
    }

    public synchronized String[] getStringSortValues() throws IOException,
                                                         IllegalStateException {
        checkSortValues(PRIMITIVE_COMPARABLE._string, stringSortValues);
        return stringSortValues;
    }

    public synchronized int[] getIntegerSortValues() throws IOException,
                                                         IllegalStateException {
        checkSortValues(PRIMITIVE_COMPARABLE._int, intSortValues);
        return intSortValues;
    }

    public synchronized boolean hasContent() throws IOException {
        if (error) {
            throw new IllegalStateException("This response in an" +
                                            " error-response");
        }
        return contents != null && nextcontentOut < contents.size();
    }

    public synchronized byte[] getNextContent() throws IOException {
        if (error) {
            throw new IllegalStateException("This response in an" +
                                            " error-response");
        }
        if (contents == null) {
            throw new IllegalStateException("No content defined");
        }
        if (nextcontentOut >= contents.size()) {
            throw new IllegalStateException("Not enough content received");
        }
        return contents.get(nextcontentOut++);
    }


    private void checkSortValues(PRIMITIVE_COMPARABLE wantedType, Object ref) {
        if (error) {
            throw new IllegalStateException("This response in an" +
                                            " error-response");
        }
        if (ref == null) {
            throw new IllegalStateException("No sortValues assigned!");
        }
        if (sortKey != wantedType) {
            throw new IllegalStateException("The sort values are "
                                            + sortKey + ", not "
                                            + wantedType);
        }
    }
    /* ****************************** Writer ************************** */

    public synchronized void initiateResponse(float[] sortValues)
            throws IOException {
        floatSortValues = sortValues;
        sortKey = PRIMITIVE_COMPARABLE._float;
        //noinspection AssignmentToNull
        contents = null;
        error = false;
        nextcontentOut = 0;
    }

    public synchronized void initiateResponse(int[] sortValues)
            throws IOException {
        intSortValues = sortValues;
        sortKey = PRIMITIVE_COMPARABLE._int;
        //noinspection AssignmentToNull
        contents = null;
        error = false;
        nextcontentOut = 0;
    }

    public synchronized void initiateResponse(String[] sortValues)
            throws IOException {
        stringSortValues = sortValues;
        sortKey = PRIMITIVE_COMPARABLE._string;
        //noinspection AssignmentToNull
        contents = null;
        error = false;
        nextcontentOut = 0;
    }

    public synchronized  void error(int errorID, String message)
            throws IOException {
        error = true;
        this.errorID = errorID;
        errorMessage = message;
    }

    public synchronized void putContent(byte[] content) throws IOException {
        if (contents == null) {
            contents = new ArrayList<byte[]>(100);
        }
        contents.add(content);
    }

    public void endResponse() throws IOException {
        // Does nothing, as there is no signalling here
    }
}
