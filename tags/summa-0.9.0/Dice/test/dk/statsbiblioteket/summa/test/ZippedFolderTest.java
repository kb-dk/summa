/* $Id: ZippedFolderTest.java,v 1.2 2007/10/04 13:28:17 te Exp $
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
import dk.statsbiblioteket.summa.dice.util.ZippedFolder;
import dk.statsbiblioteket.summa.dice.util.FileAlreadyExistException;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.util.zip.CheckedInputStream;
import java.util.zip.Adler32;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 19/09/2006
 * Time: 16:09:36
 * To change this template use File | Settings | File Templates.
 */
public class ZippedFolderTest extends TestCase {

    private long adler32Checksum (String filename) throws IOException {
        // Compute Adler-32 checksum
        CheckedInputStream cis = new CheckedInputStream(
                new FileInputStream(filename), new Adler32());
        byte[] tempBuf = new byte[128];
        while (cis.read(tempBuf) >= 0) {
        }
        return cis.getChecksum().getValue();
    }

    private long recursiveChecksum (String filename, long offset) throws IOException {
        File file = new File (filename);

        if (file.isFile()) {
            return adler32Checksum(filename);
        }

        for (String child : file.list()) {
            offset += recursiveChecksum(filename + File.separator + child, offset);
        }

        return offset;
    }

    public void testZippedFolderIdemPotency () throws Exception {
        // TODO: Write proper unit test for ZippedFolder

        String srcFolder = "/home/mikkel/tmp/shepherd";
        String zipFile = "/home/mikkel/tmp/tmp.zip";
        String outputFolder = "/home/mikkel/tmp/tmpzip";

        System.out.println ("Zipping " + srcFolder + " to " + zipFile);
        new ZippedFolder (srcFolder, zipFile, false);

        // Test overwrite exception when creating zip file
        System.out.println ("Testing overwrite failure on zipping");
        try {
            new ZippedFolder (srcFolder, zipFile, false);
            throw new Exception ("FileAlreadyExistException should be thrown when trying to overwrite existing files");
        } catch (FileAlreadyExistException e) {
            System.out.println("Caught expected FileAlreadyExistException, all good.");
        }

        // Test overwrite exception when extracting files
        System.out.println ("Testing overwrite failure on unzip");
        try {
            ZippedFolder.unzip (zipFile, new File(srcFolder).getParent(), false);
            throw new Exception ("FileAlreadyExistException should be thrown when trying to overwrite existing files");
        } catch (FileAlreadyExistException e) {
            System.out.println("Caught expected FileAlreadyExistException, all good.");
        }

        System.out.println ("Unzipping " + zipFile + " to " + outputFolder);
        ZippedFolder.unzip (zipFile, outputFolder, false);

        // Do recursive checksums of srcFolder and outputFolder
        System.out.println ("Checksumming output");
        assertTrue("Recursive checksums of input and output dirs must be equal",
                   recursiveChecksum(srcFolder, 0) == recursiveChecksum(outputFolder, 0));

        System.out.println ("Deleting temporary files");
        new File (zipFile).delete();
    }

}



