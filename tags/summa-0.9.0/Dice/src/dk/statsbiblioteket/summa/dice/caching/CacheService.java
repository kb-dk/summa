/* $Id: CacheService.java,v 1.2 2007/10/04 13:28:17 te Exp $
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

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 8/09/2006
 * Time: 11:23:30
 *
 * Generic implementation of a {@link Cache}. For additional functionality
 * see {@link RemoteCacheService} and {@link BlockingCacheService}.
 *
 * This implementation is not thread safe. If you need thread safety look
 * at {@link BlockingCacheService}.
 */
public class CacheService<E> implements Cache<E> {

    private final String dataPrefix = "item";

    private Class writerClass;
    private Class readerClass;
    private String dataURL;
    private long lastID;
    private long lastHandle;
    private Map<Long,CacheReader<E>> readers;
    private Map<Long,CacheWriter<E>> writers;

    /**
     * Create a new {@link Cache} using the specied reader and writer
     * for IO operations.
     * @param writer class of a {@link CacheWriter}
     * @param reader class of a {@link CacheReader}
     * @param dataURL url or path passed to the reader and writer when storing retrieving items
     */
    public CacheService (Class writer, Class reader, String dataURL) {
        this.dataURL = dataURL;
        lastID = -1;
        lastHandle = -1;
        readers = new HashMap<Long,CacheReader<E>> ();
        writers = new HashMap<Long,CacheWriter<E>>();
        writerClass = writer;
        readerClass = reader;
    }


    public long startPut () throws IOException {
        CacheWriter writer = null;
        long id = getNextID();
        try {
            writer = (CacheWriter) writerClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate writer", e);
        }
        writer.prepare (getDataURL(id));
        writers.put (new Long(id), writer);

        return id;
    }

    public void putPart (E part, long id) throws IOException {
        CacheWriter<E> writer = writers.get(new Long(id));
        writer.writePart(part);
    }

    public void endPut (long id) throws IOException {
        CacheWriter writer = writers.get(id);
        writer.close ();
        writers.remove(new Long(id));
    }

    public long lookup (long id) throws IOException {
        CacheReader reader = null;
        try {
            reader = (CacheReader) readerClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate reader" ,e);
        }

        try {
            reader.open (getDataURL(id));
        } catch (FileNotFoundException e) {
            return Cache.NO_SUCH_ITEM;
        }

        long handle = getNextHandle();
        readers.put(new Long(handle), reader);
        return handle;
    }

    public E readPart (long handle) throws IOException {
        CacheReader<E> reader = readers.get (new Long(handle));
        return reader.readPart();
    }

    public void close (long handle) throws IOException {
        CacheReader reader = readers.get (new Long(handle));
        reader.close();
        readers.remove(handle);
    }

    public String getDataURL (long id) {
        return dataURL + File.separator + dataPrefix + "." + id;
    }

    private synchronized long getNextID () {
        lastID++; // For thread safety we need to increment before checking for existence

        // Make sure the corresponding data file doesn't already exist
        while (new File (getDataURL(lastID)).exists()) {
            lastID++;
        }

        return lastID;
    }

    private synchronized long getNextHandle () {
        lastHandle++;
        return lastHandle;
    }
}



