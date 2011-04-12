/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * FileInputStream that is capable of renaming the file upon close.
 * The Stream is auto-closing, meaning that the file handle is automatically
 * freed when EOF is reached.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RenamingFileStream extends FileInputStream {
    private Log log = LogFactory.getLog(RenamingFileStream.class);

    private boolean success = true;
    private File file;
    private boolean closed = false;
    private boolean renamed = false;
    private String postfix;

    /**
     * Constructs a FileInputStream where the postfix will potentially be
     * used upon close.
     * @param file    the file to open.
     * @param postfix the postfix to add, is setSuccess(true) has been
     *                called and close is called.
     * @throws java.io.FileNotFoundException if the file could not be located.
     */
    public RenamingFileStream(File file, String postfix) throws
                                                         FileNotFoundException {
        super(file);
        log.trace("Created reader for '" + file
                  + "' with potential postfix '" + postfix + "'");
        this.file = file;
        this.postfix = postfix;
    }
    public synchronized void setSuccess(boolean success) {
        this.success = success;
        try {
            close();
        } catch (IOException e) {
            log.warn("setSuccess(" + success + "): Unable to close file '"
                     + file + "'. Attempting rename()");
        }
        rename();
    }
    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        log.trace("Closing stream to file '" + file + "'");
        super.close();
        closed = true;
        rename();
    }

    public File getFile() {
        return file;
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result == -1) {
            close();
        }
        return result;
    }

    @Override
    public int read(byte b[]) throws IOException {
        int result = super.read(b);
        if (result == -1) {
            close();
        }
        return result;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (result == -1) {
            close();
        }
        return result;
    }

    @Override
    public boolean markSupported() {
        // The Stream is auto-closing, so we cannot support marking
        return false;
    }

    private void rename() {
        if (renamed) {
            log.trace("File '" + file + "' already renamed");
            return;
        }
        if (closed && success && postfix != null && !"".equals(postfix)) {
            File newName = new File(file.getPath() + postfix);
            try {
                log.trace("Renaming '" + file + "' to '" + newName + "'");
                renamed = file.renameTo(newName);
                if (!file.setLastModified(System.currentTimeMillis())) {
                    log.trace("Unable to set last modification time for '"
                              + file + "'");
                }
            } catch(Exception e) {
                log.error("Could not rename '" + file
                          + "' to '" + newName + "'", e);
            }
        } else if (log.isTraceEnabled()) {
            log.trace("No renaming of '" + file + "'. closed=" + closed
                      + ", success=" + success + ", postfix='"
                      + postfix + "'");
        }
    }

    @Override
    public String toString() {
        return "RenamingFileStream(" + file + ")";
    }
}
