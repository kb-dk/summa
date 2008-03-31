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
package dk.statsbiblioteket.summa.ingest.stream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.stream.StreamFilter;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This reader performs a recursive scan for a given pattern of files.
 * When it finds a candidate for data it opens it and sends the
 * content onwards in unmodified form. When a file has been emptied, the next
 * file is processed. If close(true) is called, processed files are marked with
 * a given postfix (default: .completed). Any currently opened file is kept open
 * until it has been emptied, but no new files are opened.
 * </p><p>
 * if close(false) is called, no files are marked with the postfix and any open
 * files are closed immediately.
 * </p><p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FileReader extends StreamFilter {
    private static Log log = LogFactory.getLog(FileReader.class);

    /**
     * The root folder to scan from.
     * </p><p>
     * This property must be specified.
     */
    public static final String CONF_ROOT_FOLDER =
            "summa.ingest.filereader.root_folder";
    /**
     * Whether to perform a recursive scan or not (valid values: true, false).
     * </p><p>
     * This property is optional. Default is "true".
     */
    public static final String CONF_RECURSIVE =
            "summa.ingest.filereader.recursive";
    /**
     * The file pattern to match.
     * </p><p>
     * This property is optional. Default is ".*\.xml".
     */
    public static final String CONF_FILE_PATTERN =
            "summa.ingest.filereader.file_pattern";
    private static final String DEFAULT_FILE_PATTERN = ".*\\.xml";
    /**
     * The postfix for the file when it has been fully processed.
     * </p><p>
     * This property is optional. Default is ".completed".
     */
    public static final String CONF_COMPLETED_POSTFIX =
            "summa.ingest.filereader.completed_postfix";

    private File root;
    private boolean recursive = true;
    private Pattern filePattern;
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private String postfix = ".completed";

    private boolean started = false;
    private List<File> todo;
    private File current;
    private List<File> done;
    private boolean closedWithSuccess = false;

    private InputStream inputStream;

    /**
     * Sets up the properties for the FileReader. Scanning for files are
     * postponed until the first read() or pump() is called.
     * @param configuration the setup for the FileReader. See the CONF-constants
     *                      for available properties.
     */
    public FileReader(Configuration configuration) {
        log.trace("creating FileReader");
        try {
            String rootString = configuration.getString(CONF_ROOT_FOLDER);
            if ("".equals(rootString)) {
                log.debug("Setting root to current folder");
                root = new File(".").getAbsoluteFile();
            } else {
                log.trace("Got root-property '" + rootString + "'");
                root = new File(rootString).getAbsoluteFile();
                log.debug("Setting root to '" + root + "' from value '"
                          + rootString + "'");
                if (!root.exists()) {
                    //noinspection DuplicateStringLiteralInspection
                    log.warn("Root '" + root + "' does not exist");
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("No root specified for key "
                                               + CONF_ROOT_FOLDER);
        }
        recursive = configuration.getBoolean(CONF_RECURSIVE, recursive);
        filePattern = Pattern.compile(configuration.
                getString(CONF_FILE_PATTERN, DEFAULT_FILE_PATTERN));
        postfix = configuration.getString(CONF_COMPLETED_POSTFIX, postfix);
        log.info("FileReader created. Root: '" + root
                 + "', recursive: " + recursive
                 + ", file pattern: '" + filePattern.pattern()
                 + "', completed postfix: '" + postfix + "'");
    }

    public void setSource(Filter source) {
        throw new UnsupportedOperationException("A FileReader must be "
                                                + "positioned at the start of "
                                                + "a filter chain");
    }

    /* Recursive file adder */
    private void fillToDo(File start) {
        try {
            if (start.isDirectory()) {
                log.debug("fillTodo: Listing files in '" + start + "'");
                File files[] = start.listFiles();
                for (File file: files) {
                    fillToDo(file);
                }
            } else {
                if (filePattern.matcher(start.getName()).matches()) {
                    log.debug("fillToDo: Adding '" + start + "' to todo");
                    todo.add(start);
                } else {
                    log.trace("fillToDo: Skipping '" + start + "'");
                }
            }
        } catch (Exception e) {
            log.warn("fillToDo: Could not process '" + start + ". Skipping");
        }
    }

    /* One-time setup */
    private void checkInit() {
        if (started) {
            return;
        }
        log.trace("checkInit: Filling todo");
        todo = new ArrayList<File>(100);
        fillToDo(root);
        done = new ArrayList<File>(Math.max(1, todo.size()));
        started = true;
        log.info("Located " + todo.size() + " files matching pattern '"
                 + filePattern.pattern() + "'");
        openNext();
    }

    /**
     * Opens the next file in {@link #todo}. If a stream is currently open, it
     * is closed before the next file is opened. Opening a file removes it from
     * the todo. Closing a file adds it to {@link #done}. Opened files are
     * stored in {@link #current}.
     */
    private void openNext() {
        closePrevious();
        while (current == null) {
            if (todo.size() == 0) {
                log.info("openNext: No more files available");
                return;
            }
            current = todo.remove(0);
            log.info("Opening file '" + current + "'");
            try {
                InputStream in = new FileInputStream(current);
                // BufferedInputStream does not support streams > 2GB?
                inputStream = new MetaInfo(current.toString(),
                                           current.length()).appendHeader(in);
                log.debug("File '" + current + "' opened successfully");
            } catch (FileNotFoundException e) {
                //noinspection DuplicateStringLiteralInspection
                log.error("Could not locate '" + current
                          + "'. Skipping to next file");
                //noinspection AssignmentToNull
                current = null;
            }
        }
    }

    /**
     * If any file is currently open, close it and add it to done.
     */
    private void closePrevious() {
        log.trace("closePrevious called");
        if (inputStream != null) {
            log.debug("Closing previous InputStream '" + current + "'");
            try {
                inputStream.close();
                if (current != null) { // Only add to done when no error
                    done.add(current);
                    if (closedWithSuccess) {
                        markAsProcessed(current);
                    }
                    //noinspection AssignmentToNull
                    current = null;
                }
            } catch (IOException e) {
                log.error("openNext: Exception closing InputStream '"
                          + current + "'. Ignoring and continuing", e);
            }
        }
    }


    /**
     * Graceful shutdown of opened filed.
     * @param success if false, all opened files are closed immediately. If
     *                true, processed files are appended with {@link #postfix}
     *                and any currently opened files are kept open until they
     *                are emptied.
     */
    public void close(boolean success) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("close(" + success + ") called");
        if (!success) {
            closePrevious();
        }
        if (todo.size() > 0) {
            log.debug("close: Discarding " + todo + " files from todo");
        }
        todo.clear();
        if (success && !"".equals(postfix)) {
            log.debug("Adding postfix '" + postfix + " to processed files");
            for (File file: done) {
                markAsProcessed(file);
            }
        }
        closedWithSuccess = success;
        // Note: if success, then current might still be open.
    }

    private void markAsProcessed(File file) {
        File newName = new File(file.getPath() + postfix);
        try {
            log.trace("Rename '" + file + "' to '" + newName + "'");
            file.renameTo(newName);
        } catch (Exception e) {
            log.warn("Could not rename '" + file + "' to '" + newName
                     + "'");
        }
    }

    /**
     * Warning: This implementation is not guaranteed to return > 0 if there is
     * available content.
     * @return a minimum amount of bytes available.
     * @throws IOException if a read exception occured.
     */
    // FIXME: Make the method more useful by guaranteeing > 0 in case of content
    public int available() throws IOException {
        checkInit();
        return current == null ? 0 : inputStream.available();
    }

    public boolean pump() throws IOException {
        checkInit();
        return super.pump();
    }

    public int read() throws IOException {
        checkInit();
        if (current == null) {
            log.trace("current == null: EOF reached");
            return EOF;
        }
        int value;
        while ((value = inputStream.read()) == EOF) {
            openNext();
            if (current == null) {
                return EOF;
            }
        }
        return value;
    }
}
