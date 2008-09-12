/* $Id: CacheClientRunner.java,v 1.2 2007/10/04 13:28:21 te Exp $
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
import java.util.Random;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 13/09/2006
 * Time: 11:36:41
 * Simple {@link Runnable} writing a specified number of {@link DataBlob}s
 * to a {@link Cache}. Used for testing purposes.
 */
public class CacheClientRunner implements Runnable {

    CacheClient<String> cache;
    List<DataBlob> blobs;

    public CacheClientRunner (int runnerId, Cache<String> cache, int numBlobs) {
        this.cache = new GenericCacheClient<String>(cache);
        this.blobs = new ArrayList<DataBlob>();
        for (int i = 0; i < numBlobs; i ++) {
            blobs.add (new DataBlob(runnerId, i));
        }
    }

    public void run () {
        Random random = new Random(System.currentTimeMillis());
        for (DataBlob blob : blobs) {
            try {
                cache.put (blob);
                try {
                    Thread.sleep(random.nextInt(100));
                } catch (InterruptedException e) {
                    // Ignore
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit (1);
            }
        }
        System.out.println ("CacheClientRunner " + Thread.currentThread().getName() + " complete.");
    }
}




