/* $Id: DataCacheWriter.java,v 1.2 2007/10/04 13:28:17 te Exp $
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

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A {@link CacheWriter} writing raw data (byte arrays) to files.
 *
 * WARNING: It is not guaranteed (and generally not true) that
 * reading the written file with a {@link DataCacheReader} will
 * result in the same number of parts. It is guaranteed that the
 * combined data is identical however.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Needs Javadoc")
public class DataCacheWriter implements CacheWriter {

    OutputStream out;

    public void prepare(String url) throws IOException {
        File file = new File (url);
        if (file.exists()) {
            throw new IOException("File already exists: " + url);
        } else {
            // Make sure parent dir exists
            file.getParentFile().mkdirs();
        }

        out = new FileOutputStream (file);
    }

    /**
     * It is assumed that <code>part</code> is a byte array.
     */
    public void writePart(Object part) throws IOException {
        out.write ((byte[])part);
    }

    public void close() throws IOException {
        out.flush();
        out.close();
    }
}
