/* $Id: ZippedFolder.java,v 1.2 2007/10/04 13:28:17 te Exp $
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
package dk.statsbiblioteket.summa.dice.util;

import java.io.*;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Utility class to help zipping entire folders and store the zip
 * file on disk. Primarily intended for zipping lucene indexes,
 * but the usage is not restricted to this.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class ZippedFolder implements Iterable, Serializable {

    private String srcFolder;
    private String outputFilename;
    private ZipOutputStream zip;
    private boolean overwrite;

    /**
     * Recursicvely zip a folder and write the zip file to a given location.
     * @param path folder to zip
     * @param outputFilename name of the outputfile
     * @param overwrite whether or not to overwrite existing file
     * @throws IOException
     */
    public ZippedFolder (String path, String outputFilename, boolean overwrite) throws IOException {
        this.overwrite = overwrite;
        File file =  new File (outputFilename);
        if (!overwrite) {
            if (file.exists()) {
                throw new FileAlreadyExistException(outputFilename);
            }
        }

        // Ensure parent dir exists
        file.getParentFile().mkdirs();

        this.srcFolder = path;
        this.outputFilename = outputFilename;
        validate();

        FileOutputStream fileWriter = new FileOutputStream(outputFilename);
        zip = new ZipOutputStream(fileWriter);

        // Write zip file
        addFolderToZip("", srcFolder);

        // Clean up
        zip.flush();
        zip.finish();
        zip.close();
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Name of zipped file
     */
    public String getFilename () {
        return outputFilename;
    }

    /**
     * Iterator returning raw data. You can recombine them with
     * {@link Splitter#collect}.
     */
    public Iterator iterator () {
        try {
            return new Splitter(outputFilename, 2048).iterator();
        } catch (IOException e) {
            throw new RuntimeException("Error creating Splitter", e);
        }
    }

    /**
     * Unzip a zip file to a target directory.
     * @param zipFilename path to the zip file to extract
     * @param outputDir directory to place output in
     * @param overwrite overwrite files
     * @throws FileAlreadyExistException if trying to overwrite a file and overwrite==False
     * @throws IOException
     */
    public static void unzip (String zipFilename, String outputDir, boolean overwrite) throws IOException {
        new File(outputDir).mkdirs();

        BufferedOutputStream dest = null;
        FileInputStream fis = new FileInputStream(zipFilename);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
        ZipEntry entry;
        while((entry = zis.getNextEntry()) != null) {
            int count;
            byte data[] = new byte[2048];
            String newFile = outputDir + File.separator + entry.getName();

            if (!overwrite) {
                if (new File (newFile).exists()) {
                    throw new FileAlreadyExistException();
                }
            }

            // Create parent dir
            new File (newFile).getParentFile().mkdirs();

            // Write data
            FileOutputStream fos = new FileOutputStream(newFile);
            dest = new BufferedOutputStream(fos, data.length);
            while ((count = zis.read(data, 0, data.length)) != -1) {
                dest.write(data, 0, count);
            }
            dest.flush();
            dest.close();
        }
        zis.close();

    }

    private void addToZip (String path, String srcFile) throws IOException {
        File folder = new File(srcFile);

        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile);
        } else {
            byte[] buf = new byte[2048];
            int len;

            zip.putNextEntry(new ZipEntry(path + File.separator + folder.getName()));

            FileInputStream in = new FileInputStream(srcFile);
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }

        }
    }

    private void addFolderToZip (String path, String srcFolder) throws IOException {
        File folder = new File(srcFolder);

        for (String filename : folder.list()) {
            addToZip(path+ File.separator + folder.getName(), srcFolder+ File.separator +filename);
        }

    }

    private void validate () throws IOException {
        File folder = new File (srcFolder);
        File outputFile = new File (outputFilename);

        if (!folder.isDirectory()) {
            throw new IOException ("Target path " + srcFolder + " is not a directory");
        }

        if (outputFile.exists() && !overwrite) {
            throw new FileAlreadyExistException(outputFilename);
        }
    }
}
