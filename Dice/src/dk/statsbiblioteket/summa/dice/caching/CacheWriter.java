/* $Id: CacheWriter.java,v 1.2 2007/10/04 13:28:17 te Exp $
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

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 8/09/2006
 * Time: 11:06:15
 * IO layer for a
 */
public interface CacheWriter<E> {

    /**
     * Prepare the writer to write data parts to the given url.
     * @throws IOException
     */
    public void prepare (String url) throws IOException;

    /**
     * Write the given data part to the url defined in {@link #prepare}
     * @throws IOException
     */
    public void writePart (E part) throws IOException;

    /**
     * Close and flush the data file.
     * @throws IOException
     */
    public void close () throws IOException;

}



