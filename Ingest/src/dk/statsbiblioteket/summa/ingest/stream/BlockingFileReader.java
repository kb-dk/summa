/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.ingest.stream;

import java.io.IOException;

import dk.statsbiblioteket.summa.ingest.StreamFilter;

/**
 * This readers performs a continuous scan of the local file system for any
 * input-files. When it finds a candidate for data it opens it and sends the
 * content onwards in unmodified form. When a file has been emptied, it is
 * closed, moved to a given success-folder and scanning for new files is
 * restarted.
 * </p><p>
 * The moving of files is done with respect to sub-folders, so the file
 * {@code root/sub1/sub2/myfile.xml} is moved to
 * {@code success/sub1/sub2/myfile.xml}.
 */
// TODO: Implement this class
public class BlockingFileReader extends StreamFilter {

    public void setSource(StreamFilter source) {
        // log.warn("Ignoring source");
    }

    public void close(boolean success) {
    }

    public int read() throws IOException {
        return 0;
    }
}
