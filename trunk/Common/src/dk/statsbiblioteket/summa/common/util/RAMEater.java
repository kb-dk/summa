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
package dk.statsbiblioteket.summa.common.util;

import java.util.ArrayList;
import java.util.List;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Allocates RAM and fills it with garbage until an OutOfMemoryError occurs.
 * Normally used as a stand-alone program for clearing the disc-cache.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class RAMEater {

    /**
     * Allocated memory in chunks of 1 MB and fills it with garbage. This goes
     * on until an OutOfMemoryError is thrown, at which point a clean exit is
     * performed.
     * @param args all arguments are ignored.
     */
    public static void main(String[] args) {
        int BLOCK_SIZE = 1024*1024;
        List<byte[]> bytes = new ArrayList<byte[]>(1000);
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                byte[] newBytes = new byte[BLOCK_SIZE];
                for (int i = 0 ; i < BLOCK_SIZE ;i++) {
                    // We don't care about the content, but we put in something
                    // to prevent any fancy sparse memory allocations.
                    newBytes[i] = (byte)i;
                }
                bytes.add(newBytes);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("Allocated "
                               + (long)bytes.size() * BLOCK_SIZE / 1024 / 1024
                               + "MB before OutOfMemoryError was thrown");
        }
    }
}
