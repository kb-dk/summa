/* $Id: GZipCacheReader.java,v 1.2 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:17 $
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
package dk.statsbiblioteket.summa.dice.caching;

import dk.statsbiblioteket.summa.dice.util.GZipBlob;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.*;
import java.util.Iterator;

/**
 * To change this template use File | Settings | File Templates.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Needs Javadocs")
public class GZipCacheReader<E> implements CacheReader {

    private Iterator<E> items;

    public void open(String filename) throws IOException {
        items = new GZipBlob<E> (filename).iterator();
    }

    public E readPart() throws IOException {
        if (items.hasNext()) {
            return items.next();
        }
        return null;
    }

    public void close() throws IOException {
        // Nothing necessary
    }
}
