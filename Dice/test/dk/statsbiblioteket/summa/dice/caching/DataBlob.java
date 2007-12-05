/* $Id: DataBlob.java,v 1.2 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 13/09/2006
 * Time: 11:33:54
 * Cacheable used for testing purposes.
 */
class DataBlob implements Iterable<String> {

    List<String> data = new ArrayList<String>();

    /**
     *
     * @param owner some identification number for the creator of this blob (hint: think threads)
     * @param id a number used to identify this particular blob
     */
    public DataBlob (int owner, int id) {
        for (int i = 0; i < 100; i++) {
            data.add("Owner: " + owner + ", Item " + id + ", part " + i);
        }
    }

    public Iterator<String> iterator () {
        return data.iterator();
    }
}
