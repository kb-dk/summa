/* $Id: SplitterTest.java,v 1.2 2007/10/04 13:28:17 te Exp $
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
package dk.statsbiblioteket.summa.test;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.dice.util.Splitter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.zip.CheckedInputStream;
import java.util.zip.Adler32;
import java.util.Random;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 19/09/2006
 * Time: 13:30:10
 * To change this template use File | Settings | File Templates.
 */
public class SplitterTest extends TestCase {

    private long adler32Checksum (String filename) throws IOException {
        // Compute Adler-32 checksum
        CheckedInputStream cis = new CheckedInputStream(
                new FileInputStream(filename), new Adler32());
        byte[] tempBuf = new byte[128];
        while (cis.read(tempBuf) >= 0) {
        }
        return cis.getChecksum().getValue();
    }

    public void testFileSplitIdemPotency () throws Exception {
        String inFile = "/home/mikkel/tmp/test_split_in";
        String outFile = "/home/mikkel/tmp/test_split_out";

        System.out.println ("Splitting and collecting, " + inFile + " to " + outFile);
        Splitter splitter = new Splitter(inFile, 99);
        Splitter.collect (splitter.iterator(), outFile);

        System.out.println ("Checksumming");
        assertTrue("Checksums of original and collected file should match",
                                            adler32Checksum(inFile) == adler32Checksum(outFile));

        System.out.println ("Deleting output file");
        new File (outFile).delete();
    }


    public void testBufferSplitIdemPotency () throws Exception {
        Random rand = new Random (System.currentTimeMillis());
        byte[] bytes = new byte[64];
        rand.nextBytes(bytes);

        Splitter splitter = new Splitter(bytes, 5);
        byte[] result = Splitter.collect(splitter.iterator());

        System.out.println ("Original: " + new String (bytes, "UTF-8"));
        System.out.println ("Result:   " + new String(result, "UTF-8"));

        assertTrue("Splitting and collecting a byte array should be an idem potent operation", Arrays.equals(bytes, result));
    }

}



