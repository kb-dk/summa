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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.util.ArrayUtil;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This reader performs a recursive scan for a given pattern of files.
 * When it finds a candidate for data it opens it and sends the
 * content onwards in unmodified form, packaged as a stream in a Payload.
 * If close(true) is called, processed files are marked with
 * a given postfix (default: .completed). Any currently opened file is kept open
 * until it has been emptied, but no new files are opened.
 * </p><p>
 * If close(false) is called, no files are marked with the postfix and any open
 * files are closed immediately.
 * </p><p>
 * Meta-info for delivered payloads will contain {@link Payload#ORIGIN} which
 * states the originating file for the stream.
 * </p><p>
 * The files are processed depth-first in unicode-sorted order, unless
 * {@link #CONF_REVERSE_SORT} is true.
 * </p><p>
 * Warning: The FileReader does not check for cyclic folders structures.
 */
// TODO: Make the FileReader handle cyclic folder structures
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FileReader implements ObjectFilter {
    private static Log log = LogFactory.getLog(FileReader.class);

    /**
     * The root folder to scan from.
     * </p><p>
     * This property must be specified.
     */
    public static final String CONF_ROOT_FOLDER =
            "summa.ingest.filereader.rootfolder";
    /**
     * Whether to perform a recursive scan or not (valid values: true, false).
     * </p><p>
     * This property is optional. Default is "true".
     */
    public static final String CONF_RECURSIVE =
            "summa.ingest.filereader.recursive";
    public static final boolean DEFAULT_RECURSIVE = true;

    /**
     * The file pattern to match.
     * </p><p>
     * This property is optional. Default is ".*\.xml".
     */
    public static final String CONF_FILE_PATTERN =
            "summa.ingest.filereader.filepattern";
    private static final String DEFAULT_FILE_PATTERN = ".*\\.xml";
    /**
     * The postfix for the file when it has been fully processed.
     * Setting this to null or the empty String means that no renaming is done.
     * </p><p>
     * This property is optional. Default is ".completed".
     */
    public static final String CONF_COMPLETED_POSTFIX =
            "summa.ingest.filereader.completedpostfix";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String DEFAULT_COMPLETED_POSTFIX = ".completed";

    /**
     * If true, scans are performed in reverse unicode order.
     * </p><p>
     * This property is optional. Default is false.
     */
    public static final String CONF_REVERSE_SORT =
            "summa.ingest.filereader.sort.reverse";
    public static final boolean DEFAULT_REVERSE_SORT = false;

    protected File root;
    private boolean recursive = true;
    private boolean reverse_sort = DEFAULT_REVERSE_SORT;
    private Pattern filePattern;
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private String postfix = DEFAULT_COMPLETED_POSTFIX;

    /**
     * The list of files and folders to process: Files are send onwards directly
     * while folders are expanded when encountered.
     */
    private List<File> todo = new LinkedList<File>();
    /**
     * Keeps track of already encountered files. This guards against endless
     * recursions due to linking.
     */
    protected final List<File> encountered = new ArrayList<File>(100);
    /**
     * Keeps track of all delivered files.
     */
    private final List<Payload> delivered = new ArrayList<Payload>(100);

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
            todo.add(root);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "No root specified for key " + CONF_ROOT_FOLDER);
        }
        recursive = configuration.getBoolean(CONF_RECURSIVE, recursive);
        reverse_sort = configuration.getBoolean(
                CONF_REVERSE_SORT, reverse_sort);
        filePattern = Pattern.compile(configuration.
                getString(CONF_FILE_PATTERN, DEFAULT_FILE_PATTERN));
        postfix = configuration.getString(CONF_COMPLETED_POSTFIX, postfix);
        log.info("FileReader created. Root: '" + root
                 + "', recursive: " + recursive
                 + ", file pattern: '" + filePattern.pattern()
                 + "', completed postfix: '" + postfix + "'");
    }

    public void setSource(Filter source) {
        throw new UnsupportedOperationException(String.format(
                "A %s must be positioned at the start of a filter chain",
                getClass().getName()));
    }

    private FileFilter dataAndFolderFilter = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.canRead()
                   && (pathname.isDirectory()
                       ||  filePattern.matcher(pathname.getName()).matches())
                   && !alreadyHandled(pathname);
        }
    };

    private Comparator<File> fileComparator = new Comparator<File>() {
        public int compare(File o1, File o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    /** If the first File in the to do list is a file or the to do list is
     * empty, do nothing. Else expand the first File (which is logically a
     * folder since it is not a file), add the expansion to the start of the
     * to do list and call updateTodo again.
     * </p><p>
     * This behaviour ensures a lazy depth-first traversal in unicode
     * (optionally reverse unicode) order. It is safe to call this method
     * multiple times.
     * </p><p>
     * When the call exits, the to do list will either be empty or contain a
     * file as the first entry.
     **/
    protected synchronized void updateToDo() {
        if (log.isTraceEnabled()) {
            log.trace("updateTodo() called with first todo-element: "
                      + (todo.size() > 0 ? todo.get(0) : "NA"));
        }
        while (true) {
            File start = null;
            try {
                if (todo.size() == 0 || todo.get(0).isFile()) {
                    return;
                }

                // The first File is a folder
                start = todo.remove(0);
                if (!recursive) { // No expansion, just skip it
                    log.debug("Skipping folder '" + start
                              + "' as recursion is not enabled");
                    continue;
                }

                // Get files from folder
                File files[] = start.listFiles(dataAndFolderFilter);
                if (files.length == 0) {
                    log.trace("No files in '" + start + "'");
                    continue;
                }
                Arrays.sort(files, fileComparator);
                if (reverse_sort) {
                    ArrayUtil.reverse(files);
                }
                //noinspection DuplicateStringLiteralInspection
                log.debug("Queueing " + files.length + " files or folders from "
                          + "'" + start + "'. Queue size before: "
                          + todo.size());
                if (log.isTraceEnabled()) {
                    Logs.log(log, Logs.Level.TRACE, "Queueing Files: ", files);
                }
                todo.addAll(0, Arrays.asList(files));
            } catch (Exception e) {
                log.warn("Could not process '" + start + ". Skipping: "
                         + e.getMessage(), e);
            }
        }
    }

    /**
     * Adds the root to to do list and updates it.
     * @param root the starting point for the to do.
     */
    protected synchronized void updateToDo(File root) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("updateToDo(" + root + ") called");
        todo.add(0, root);
        updateToDo();
    }

    /**
     * Checks whether the file has been encountered before and remembers it
     * for subsequent checks if it hasn't.
     * If the File is a directory is is always considered not handled.
     * @param file a file to check.
     * @return true if the file has been encountered before.
     */
    protected boolean alreadyHandled(File file) {
        if (file.isDirectory()) {
            return false;
        }
        synchronized (encountered) {
            if (encountered.contains(file)) {
                return true;
            }
            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("Adding '" + file + "' to encountered");
            }
            encountered.add(file);
            return false;
        }
    }

    /**
     * FileInputStream that is capable of renaming the file upon close.
     * The Stream is auto-closing, meaning that the file handle is automatically
     * freed when EOF is reached.
     */
    class RenamingFileStream extends FileInputStream {
        private Log log = LogFactory.getLog(RenamingFileStream.class);

        private boolean success = true;
        private File file;
        private boolean closed = false;
        private boolean renamed = false;

        /**
         * Constructs a FileInputStream where the postfix will potentially be
         * used upon close.
         * @param file    the file to open.
         * @param postfix the postfix to add, is setSuccess(true) has been
         *                called and close is called.
         * @throws FileNotFoundException if the file could not be located.
         */
        public RenamingFileStream(File file, String postfix) throws
                                                         FileNotFoundException {
            super(file);
            log.trace("Created reader for '" + file
                      + "' with potential postfix '" + postfix + "'");
            this.file = file;
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
                    file.renameTo(newName);
                    renamed = true;
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

    /**
     * Opens the next file in {@link #todo} and produces a Payload with a
     * stream to the file content.
     * @return a Payload with a stream for the next file or null if no further
     *         files are available.
     */
    public synchronized Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        updateToDo();
        if (todo.size() == 0) {
            log.info("next: No more files available");
            return null;
        }
        return deliverFile(todo.remove(0));
    }

    /**
     * Wrap the current file in a Payload with a RenamingFilestream.
     * When the Payload is closed, the file will be renamed automatically.
     * @param current a file to wrap.
     * @return a Payload with a stream for the file.
     */
    protected Payload deliverFile(File current) {
        log.info("Opening file '" + current + "'");
        try {
            RenamingFileStream in = new RenamingFileStream(current, postfix);
            Payload payload = new Payload(in);
            payload.getData().put(Payload.ORIGIN, current.getPath());
            log.debug("File '" + current + "' opened successfully");
//            System.out.println(delivered + " " + payload);
            synchronized (delivered) {
                delivered.add(payload);
            }
            return payload;
        } catch (FileNotFoundException e) {
            //noinspection DuplicateStringLiteralInspection
            log.error("Could not locate '" + current
                      + "'. Skipping to next file");
            return next();
        }
    }

    /**
     * Graceful shutdown of opened files.
     * @param success if false, all opened files are closed immediately. If
     *                true, processed files are appended with {@link #postfix}
     *                and any currently opened files are kept open until they
     *                are emptied.
     */
    public void close(boolean success) {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Closing");
        //noinspection DuplicateStringLiteralInspection
        closeDelivered(success);
        if (todo.size() > 0) {
            log.debug("When closing, " + todo.size()
                      + " files remained in queue");
            todo.clear();
        }
        // Note: if success, some streams might still be open.
    }

    protected void closeDelivered(boolean success) {
        synchronized (delivered) {
            for (Payload payload: delivered) {
                if (log.isTraceEnabled()) {
                    log.trace("closedelivered(): Calling close(" + success
                              + ") on " + payload);
                }
                if (payload.getStream() == null) {
                    log.warn("Can not close payload " + payload.getId()
                             + ": Payload has no stream");
                    continue;
                }
                if (!(payload.getStream() instanceof RenamingFileStream)) {
                    log.debug("RenamingFilestream not located when closing "
                              + "payload " + payload.getId() + ". Got: "
                              + payload.getStream().getClass().getName());
                } else {
                    RenamingFileStream stream =
                            (RenamingFileStream)payload.getStream();
                    log.debug("Closing stream " + stream.file
                              + " with success: " + success);
                    stream.setSuccess(success);
                }
                Logging.logProcess("FileReader",
                                   "Calling close in closeDelivered",
                                   Logging.LogLevel.TRACE, payload);
                payload.close(); // TODO: Do we want close always?
                if (!success) {  // Or only on failure?
                    // Force close
                    log.debug("Forcing close on payload " + payload);
                    //noinspection DuplicateStringLiteralInspection
                    Logging.logProcess("IndexControllerImpl",
                                       "Calling close with success = false",
                                       Logging.LogLevel.WARN, payload);
                    payload.close();
                }
            }
            delivered.clear();
        }
    }

    /**
     * Pump iterates through all {@link #delivered} payloads and empties the
     * embedded streams. When all streams are emptied, a new payload is created.
     * </p><p>
     * This is a heavy process if there are a lot of files.
     * @return true if pumping should continue, in order to process all data.
     * @throws IOException in case of read errors.
     */
    public synchronized boolean pump() throws IOException {
        for (Payload payload: delivered) {
            if (payload.pump()) {
                return true;
            }
        }
        return hasNext() && next() != null;
    }

    /* Interface implementations */

    public synchronized boolean hasNext() {
        updateToDo();
        return todo.size() > 0;
    }

    public void remove() {
        log.warn("Remove not implemented for " + getClass().getName());
    }

    protected synchronized boolean isTodoEmpty() {
        log.trace("idTodoEmpty(): todo-size: " + todo.size());
        return todo.size() == 0;
    }
}
