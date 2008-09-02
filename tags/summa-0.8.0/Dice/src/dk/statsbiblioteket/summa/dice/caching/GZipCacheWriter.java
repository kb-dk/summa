/* $Id: GZipCacheWriter.java,v 1.2 2007/10/04 13:28:17 te Exp $
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

import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;

import dk.statsbiblioteket.summa.dice.util.GZipBlob;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Write java objects to the cache gzipping them on the fly.
 * The stored objects are actually {@link GZipBlob}s and can be read
 * either with a {@link GZipCacheReader} or directly with
 * {@link GZipBlob#GZipBlob(String filename)}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Needs javadocs")
public class GZipCacheWriter<E> implements CacheWriter<E> {

    private ObjectOutputStream out;
    private GZIPOutputStream zipper;
    private FileOutputStream fileStream;

    public void prepare(String url) throws IOException {
        File file = new File (url);
        if (file.exists()) {
            throw new IOException("File already exists: " + url);
        } else {
            // Make sure parent dir exists
            file.getParentFile().mkdirs();
        }

        fileStream = new FileOutputStream (file);
        zipper = new GZIPOutputStream(fileStream);
        out = new ObjectOutputStream (zipper);
    }

    public void writePart(E part) throws IOException {
        out.writeObject (part);
    }

    public void close() throws IOException {
        zipper.finish();
        out.close ();
        zipper.close();
        fileStream.close();
    }
}
