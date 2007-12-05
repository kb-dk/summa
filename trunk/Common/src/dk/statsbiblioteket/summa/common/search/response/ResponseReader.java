/* $Id: ResponseReader.java,v 1.2 2007/10/04 13:28:18 te Exp $
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
package dk.statsbiblioteket.summa.common.search.response;

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The natural order of method calls:
 * 1. Check {@link #isError} and take corresponding actions, if it is true
 *    (See {@link #getErrorID} and {@link #getErrorMessage}.
 * 2. Check {@link #getSortKey} and call {link #getFloatSortValues},
 *    {@link #getStringSortValues} or {@link #getIntegerSortValues}, depending
 *    on the result.
 * 3. While {@link #hasContent} is true, call {@link #getNextContent}.
 *    Note that this might return fewer than sortValues.length results, if the
 *    search was aborted.
 * 4. If the implementation permits it, go to 1.
 *
 * It is recommended practise that the implementation either allows for random
 * order or throws IllegalStateExceptions if the methods are called out of
 * order.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface ResponseReader extends Response {
    /**
     * Checks whether the response is a standard response or an error.
     * @return true if the response is an error.
     * @throws IOException if the state of the response could not be retrieved.
     */
    public boolean isError() throws IOException;

    /**
     * Returns the type for the sortValues.
     * @return the type for the sortValues.
     * @throws IOException if the type could not be retrieved.
     */
    public Response.PRIMITIVE_COMPARABLE getSortKey()
            throws IOException;

    /**
     * The error ID is implementation specific and should be used by the
     * machine to decide which action to take.
     * @return an implementation specific error ID.
     * @throws IOException if the error ID could not be retrieved.
     */
    public int getErrorID() throws IOException;

    /**
     * The error message should describe what went wrong.
     * @return a human readable error message.
     * @throws IOException if the error message could not be retrieved.
     */
    public String getErrorMessage() throws IOException;

    /**
     * Return the sort values for the content. This method should be called if
     * {@link #getSortKey} returned the
     * {@link Response.PRIMITIVE_COMPARABLE} enum _float.
     * @return the float-values that was used for sorting.
     * @throws IOException if the sortValues could not be retrieved.
     * @throws IllegalStateException if {@link #getSortKey} did not specify
     *         _float as the sort key.
     */
    public float[] getFloatSortValues() throws IOException,
                                               IllegalStateException;

    /**
     * Return the sort values for the content. This method should be called if
     * {@link #getSortKey} returned the
     * {@link Response.PRIMITIVE_COMPARABLE} enum _string.
     * @return the String-values that was used for sorting.
     * @throws IOException if the sortValues could not be retrieved.
     * @throws IllegalStateException if {@link #getSortKey} did not specify
     *         _string as the sort key.
     */
    public String[] getStringSortValues() throws IOException,
                                                IllegalStateException;

    /**
     * Return the sort values for the content. This method should be called if
     * {@link #getSortKey} returned the
     * {@link Response.PRIMITIVE_COMPARABLE} enum _integer.
     * @return the integer-values that was used for sorting.
     * @throws IOException if the sortValues could not be retrieved.
     * @throws IllegalStateException if {@link #getSortKey} did not specify
     *         _integer as the sort key.
     */
    public int[] getIntegerSortValues() throws IOException,
                                               IllegalStateException;

    /**
     * Checks to see if there is more content.
     * @return true if there is more content.
     * @throws IOException if it could not be determined if there is more
     *                     content.
     */
    public boolean hasContent() throws IOException;

    /**
     * Get the next content block.
     * @return a content block.
     * @throws IOException if the content block could not be retrieved.
     */
    public byte[] getNextContent() throws IOException;

}
