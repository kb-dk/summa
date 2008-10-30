/* $Id: Telegram.java,v 1.2 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:20 $
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
package dk.statsbiblioteket.summa.common.fast;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;

/*
* The State and University Library of Denmark
* CVS:  $Id: Telegram.java,v 1.2 2007/10/04 13:28:20 te Exp $
*/
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Possibly deprecated (or rather: never used)")
public interface Telegram {
    /**
     * Creates a streaming representation of the telegram, meant for use by
     * {@link #fromStream}.
     * @param stream adds a representation of the telegram to the given stream.
     * @throws IOException in case of communication problems.
     */
    public void toStream(InputStream stream) throws IOException;

    /**
     * Receives a stream and fills the telegram with data from it.
     * @param stream contains a streamed telegram, normally created by
     *               {@link#toStream}.
     * @throws IOException in case of communication problems.
     */
    public void fromStream(OutputStream stream) throws IOException;

    /**
     * A telegram is fully formed if every property can be read without
     * waiting for more data to be added. This is especially relevant for
     * remote telegrams.
     * @return true if the telegram is fully formed.
     */
    public boolean isFullyFormed();
}



