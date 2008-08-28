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
/*
 * The State and University Library of Denmark
 * CVS:  $Id$
 */
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import org.apache.log4j.Logger;

/**
 * @deprecated in favor of DiskStringPool.
 */
public class DiskTagHandlerFacet {
    private static Logger log = Logger.getLogger(DiskTagHandlerFacet.class);

    private static final int VERSION = 1;

    private long[] tags;
    private RandomAccessFile disk;
    private String facet;
    private File folder;
    protected boolean indexLoadPerformed = false;

    public DiskTagHandlerFacet(File folder, String facet) throws IOException {
        this.folder = folder;
        this.facet = facet;
        disk = new RandomAccessFile(getFile(folder, facet), "r");
        getPointers();
    }

    protected static File getFile(File folder, String facet) {
        return new File(folder, "facet_" + facet + ".dat");
    }

    protected String getIndexName(String facet) {
        return "facet_" + facet + ".index";
    }

    private void getPointers() throws IOException {
        File indexFile = new File(folder, getIndexName(facet));
        if (indexFile.exists() &&
            getFile(folder, facet).lastModified() <= indexFile.lastModified()) {
            try {
                loadIndex();
                return;
            } catch (Exception e) {
                String error = "Error loading index for facet " + facet +
                               ": " + e.getMessage() +
                               ". Switching to full load.";
                log.error(error, e);
                throw new IOException(error);
            }
        }

        log.info("Unable to get updated index for " + facet +
                 ". Loading tag names and creating new index...");
        loadPointers();
        try {
            storeIndex();
        } catch (IOException e) {
            log.warn("Exception storing index", e);
        }
    }

    // TODO: Make a proper check for index up-to-dateness
    private void loadIndex() throws IOException {
        log.debug("Loading index for " + facet +
                  " from " + getIndexName(facet) + " in " + folder);
        File indexFile = new File(folder, getIndexName(facet));
        if (!indexFile.exists()) {
            String error = "The index file " + indexFile + " does not exist";
            log.error(error);
            throw new IOException(error);
        }
        ObjectInputStream in =
                        ClusterCommon.objectLoader(folder, getIndexName(facet));
        log.trace("InputStream created, reading version");
        int version = in.readInt();
        if (version != VERSION) {
            String error = "The index version was " + version +
                                  ". The expected version was " + VERSION;
            log.warn(error);
            throw new IOException(error);
        }
        log.trace("The version " + version + " was correct");
        int length = in.readInt();
        log.trace("The index should contain " + length + " elements");
        tags = new long[length];
        for (int i = 0 ; i < length ; i++) {
            tags[i] = in.readLong();
        }
        in.close();
        indexLoadPerformed = true;
        log.trace("Finished loading index for " + facet);
    }

    private void storeIndex() throws IOException {
        log.debug("Storing index for " + tags.length + " tags for " + facet);
        ObjectOutputStream out =
                ClusterCommon.objectPrinter(folder, getIndexName(facet));
        out.writeInt(VERSION);
        out.writeInt(tags.length);
        for (long tag: tags) {
           out.writeLong(tag);
        }
        out.close();
        log.trace("Finished storing index for " + facet);
    }

    private void loadPointers() throws IOException {
        log.debug("Loading pointers for " + facet);
        ArrayList<Long> temp = new ArrayList<Long>(50000);

        disk.readLine(); // Header

        long lastPos = disk.getFilePointer();
        String facetLine = disk.readLine();
        int counter = 0;
        while (facetLine != null) {
            temp.add(lastPos);
            lastPos = disk.getFilePointer();
            facetLine = disk.readLine();
            if (counter++ % 100000 == 0 && counter-1 != 0) {
                log.trace("Read tag #" + (counter-1) + " from " + facet);
            }
        }

        tags = new long[temp.size()];
        for (int i = 0 ; i < tags.length ; i++) {
            tags[i] = temp.get(i);
        }
        log.trace("Finished loading pointers for " + facet);
    }

    public String getTagName(int tagID) {
        try {
            disk.seek(tags[tagID]);
            String tagLine = disk.readLine();
            String[] tokens = tagLine.split("\t", 2);
            if (tokens.length == 2) {
                return tokens[1];
            } else {
                log.error("Unexpected content in line \"" + tagLine +
                          "\" for tag id " + tagID + " in facet " + facet);
                return null;
            }
        } catch (IOException e) {
            log.error("Could not get tag name for tag id " + tagID +
                      " from facet " + facet, e);
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Invalid ID #" + tagID + " for facet " + facet +
                      " (out of bounds)", e);
            return null;
        }
    }

    public void close() throws IOException {
        disk.close();
    }

    public int size() {
        return tags.length; 
    }
}
