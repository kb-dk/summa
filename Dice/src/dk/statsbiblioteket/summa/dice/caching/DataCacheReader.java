/* $Id: DataCacheReader.java,v 1.2 2007/10/04 13:28:17 te Exp $
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

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 20/09/2006
 * Time: 13:58:10
 * Read raw data from a file.
 *
 * WARNING: It is not guaranteed (and generally not true) that
 * this class returns the same number of parts as was written
 * by a {@link DataCacheWriter}. It is guaranteed that the combined
 * data is identical though.
 */
public class DataCacheReader implements CacheReader {

    InputStream in;
    byte[] buffer;

    public void open(String url) throws IOException {
        File file = new File (url);

        if (!file.exists()) {
            throw new FileNotFoundException (url);
        }

        in = new FileInputStream (url);
        buffer = new byte[1024];
    }

    public Object readPart() throws IOException {
        try {
            int len = in.read (buffer);
            if (len == -1) {
                // We're done
                return null;
            }
            if (len < buffer.length) {
                // trim the buffer, to avoid garbage
                byte[] trim = new byte[len];
                System.arraycopy(buffer, 0, trim, 0, len);
                return trim;
            }
            return buffer;
        } catch (EOFException e) {
            return null;
        }
    }

    public void close() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
