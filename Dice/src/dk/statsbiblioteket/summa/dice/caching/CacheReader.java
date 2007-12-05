/* $Id: CacheReader.java,v 1.2 2007/10/04 13:28:17 te Exp $
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
import java.io.FileNotFoundException;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 8/09/2006
 * Time: 13:22:09
 * To change this template use File | Settings | File Templates.
 */
public interface CacheReader<E> {

    /**
     * Prepare reader to read <code>url</code>. If the URL doesn't point to
     * a valid data file, throw a {@link FileNotFoundException}.
     * @param url
     * @throws FileNotFoundException Must be thrown if <code>url</code> isn't found
     */
    public void open (String url) throws IOException;

    /**
     * Read next part of the url specified in the {@link #open} method.
     * @return the read object or null id there are no more objects to read
     * @throws IOException
     */
    public E readPart () throws IOException;

    /**
     * Close reader.
     * @throws IOException
     */
    public void close()  throws IOException;
}
