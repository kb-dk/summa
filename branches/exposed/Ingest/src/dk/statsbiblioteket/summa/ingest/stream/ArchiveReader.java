/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.ingest.stream;

import com.sun.xml.internal.ws.message.EmptyMessageImpl;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.util.ArrayUtil;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import de.schlichtherle.truezip.file.TFile;

/**
 * Traverses sub folders and archives in a given folder depth-first to deliver
 * files as streams. This is a plug-in replacement for FileReader and the
 * recommended way of feeding file content into Summa.
 * </p><p>
 * All options from {@link FileReader} are supported. Note that the renaming of
 * files only take place outside of archives. The archives themselves are never
 * changed.
 * </p><p>
 * Currently the TrueZIP package from http://truezip.java.net/ is used. This
 * package supports multiple parallel streams for ZIP and similar archives.
 * It supports regular ZIP files, ZIP64 (> 4GB ZIPs), tar and .tgz.
 * See http://java.net/projects/truezip for details.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ArchiveReader {//} implements ObjectFilter {
    private static Log log = LogFactory.getLog(ArchiveReader.class);

    protected TFile root;
    private boolean recursive;
    private boolean reverseSort = FileReader.DEFAULT_REVERSE_SORT;
    private Pattern filePattern;
    private String postfix = FileReader.DEFAULT_COMPLETED_POSTFIX;

    public ArchiveReader(Configuration conf) {
        String rootString = conf.getString(FileReader.CONF_ROOT_FOLDER);
        if ("".equals(rootString)) {
            throw new Configurable.ConfigurationException(
                "No root. This must be specified with "
                + FileReader.CONF_ROOT_FOLDER);
        }
        log.trace("Got root-property '" + rootString + "'");
        root = new TFile(rootString).getAbsoluteFile();
        log.debug("Setting root to '" + root + "' from value '"
                  + rootString + "'");
        if (!root.exists()) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("Root '" + root + "' does not exist");
        }

        recursive = conf.getBoolean(
            FileReader.CONF_RECURSIVE, FileReader.DEFAULT_RECURSIVE);
        reverseSort = conf.getBoolean(
            FileReader.CONF_REVERSE_SORT, reverseSort);
        filePattern = Pattern.compile(conf.getString(
            FileReader.CONF_FILE_PATTERN, FileReader.DEFAULT_FILE_PATTERN));
        postfix = conf.getString(FileReader.CONF_COMPLETED_POSTFIX, postfix);
        log.info("ArchiveReader created. Root: '" + root
                 + "', recursive: " + recursive
                 + ", file pattern: '" + filePattern.pattern()
                 + "', completed postfix: '" + postfix + "'");
    }

//    @Override
    public void setSource(Filter source) {
        throw new UnsupportedOperationException(String.format(
                "A %s must be positioned at the start of a filter chain",
                getClass().getName()));
    }

    private static final List<FileProvider> EMPTY =
        new ArrayList<FileProvider>(0);
    private class FileProvider implements Iterator<FileProvider> {
        private final FileProvider parent;
        private final TFile root;
        private final boolean closable;

        private List<FileProvider> subs = null; // null means not expanded yet
        private List<FileProvider> open = new ArrayList<FileProvider>(1);
        private boolean closed = false;

        public FileProvider(FileProvider parent, TFile root, boolean closable) {
            this.root = root;
            this.closable = closable;
            this.parent = parent;
        }

        private List<FileProvider> expand(TFile source) {
            if (source.isFile()) {
                return EMPTY;
            }
            TFile[] files = root.listFiles();
            List<FileProvider> providers =
                new ArrayList<FileProvider>(files.length);
            for (TFile file: files) {
                if (file.isDirectory() || file.isArchive()
                    || filePattern.matcher(file.getName()).matches()) {
                    providers.add(new FileProvider(
                        this, file, closable && !root.isArchive()));
                }
            }
            Collections.sort(providers, new Comparator<FileProvider>() {
                @Override
                public int compare(FileProvider o1, FileProvider o2) {
                    return o1.getRoot().getName().compareTo(
                        o2.getRoot().getName());
                }
            });
            if (reverseSort) {
                Collections.reverse(providers);
            }
            return providers;
        }

        public TFile getRoot() {
            return root;
        }

        public boolean safeToRename() {
            return subs.size() == 0 && closed && closable
                   && (root.isArchive() || root.isFile());
        }

        private void close(boolean success) {
            if (closed) {
                log.warn("File '" + root + "' already closed");
                return;
            }
            closed = true;                         // No renaming of folders
            if (!success) {
                log.debug(String.format(
                    "Closing '%s' with success==false",
                    root.getAbsolutePath()));
                return;
            }
            if (postfix == null || "".equals(postfix)) {
                log.debug(String.format(
                    "Closing '%s' with success==true and no completed postfix",
                    root.getAbsolutePath()));
                return;
            }
            log.debug(String.format(
                "Closing '%s' with success==true and completed postfix '%s'",
                root.getAbsolutePath(), postfix));
            TFile newName = new TFile(root.getPath() + postfix);
            log.trace("Renaming '" + root + "' to '" + newName + "'");
            if (!root.renameTo(newName)) {
                log.warn(String.format(
                    "Unable to rename '%s' to '%s'", root, newName));
            }
            if (!root.setLastModified(System.currentTimeMillis())) {
                log.trace("Unable to set last modification time for '"
                          + root + "'");
            }
        }

        @Override
        public boolean hasNext() {
            if (closed) {
                return false;
            }
            if (subs == null) { // Expand if possible
                subs = expand(root);
            }
            while (subs.size() > 0) {
                if (subs.get(0).getRoot().isFile()) {
                    if (filePattern.matcher(
                        subs.get(0).getRoot().getName()).matches()) {
                        return true;
                    }
                    log.trace("Skipping file '" + subs.get(0).root.getName()
                              + "' as it does not matches the pattern "
                              + filePattern.pattern());
                    subs.remove(0);
                    continue;
                }
                // We have a folder or an archive so we go down
                if (subs.get(0).hasNext()) {
                    return true;
                }
                // No dice, remove it and go to next
                subs.remove(0);
            }
            if (root.isFile()
                   && filePattern.matcher(root.getName()).matches()) {
                return true;
            }
            close(true); // Assuming true seems problematic
            return false;
        }

        @Override
        public FileProvider next() {
            if (hasNext()) {
                throw new IllegalStateException(
                    "hasNext() is false so next() should not be called");
            }
            if (subs.size() > 0) {
                return subs.get(0).next();
            }
            return null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not allowed");
        }
    }
}
