/* $Id: ResponseWriter.java,v 1.2 2007/10/04 13:28:18 te Exp $
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

/*
* The State and University Library of Denmark
* CVS:  $Id: ResponseWriter.java,v 1.2 2007/10/04 13:28:18 te Exp $
*
* The natural order of method calls:
* 1. Call initiateResponse or error
* 2. Call putContent a maximum of sortValues.length times (0 times is allowed)
* 3. Call endResponse
* 4. If the implementation supports it, go to 1
*
 * It is recommended practise that the implementation either allows for random
 * order or throws IllegalStateExceptions if the methods are called out of
 * order.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface ResponseWriter extends Response {
    /**
     * The sortValues given in the response initiation also define the size
     * of the search result.
     * @param sortValues   the values that was used for sorting the result.
     * @throws IOException if the initiation values could not be processed.
     */
    public void initiateResponse(float[] sortValues) throws IOException;

    /**
     * The sortValues given in the response initiation also define the size
     * of the search result.
     * @param sortValues   the values that was used for sorting the result.
     * @throws IOException if the initiation values could not be processed.
     */
    public void initiateResponse(String[] sortValues) throws IOException;

    /**
     * State an error instead of a proper search response. This must be done
     * as the first action.
     * @param errorID      an implementation specific error ID.
     * @param message      a human readable error message.
     * @throws IOException if the error could not be processed.
     */
    public void error(int errorID, String message) throws IOException;

    /**
     * The sortValues given in the response initiation also define the size
     * of the search result.
     * @param sortValues   the values that was used for sorting the result.
     * @throws IOException if the initiation values could not be processed.
     */
    public void initiateResponse(int[] sortValues) throws IOException;

    /**
     * Add content to the response. This must be done in the order of the
     * sortValues given in initiateResponse. It is not legal to add more
     * contents than sortValues. It is legal to add fewer.
     * @param content      the content for a search hit.
     * @throws IOException if the content could not be processed.
     */
    public void putContent(byte[] content) throws IOException;

    /**
     * Signal that the response has finished. This must be done after
     * initiateResponse.
     * @throws IOException if the end response could not be processed.
     */
    public void endResponse() throws IOException;
}



