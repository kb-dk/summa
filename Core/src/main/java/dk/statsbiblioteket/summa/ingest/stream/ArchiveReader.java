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

import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.fs.FsSyncException;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

import de.schlichtherle.truezip.file.TFile;

/**
 * Traverses sub folders and archives in a given folder depth-first to deliver
 * files as streams. This is a plug-in replacement for FileReader and the
 * recommended way of feeding file content into Summa.
 * </p><p>
 * Note that the renaming of files only take place outside of archives.
 * The archives themselves are never changed.
 * </p><p>
 * Currently the TrueZIP package from http://truezip.java.net/ is used. This
 * package supports multiple parallel streams for ZIP and similar archives.
 * It supports regular ZIP files, ZIP64 (> 4GB ZIPs), tar and .tgz.
 * See http://java.net/projects/truezip for details.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class ArchiveReader extends FileSystemReader {
    private static Log log = LogFactory.getLog(ArchiveReader.class);

    protected FileProvider provider;

    public ArchiveReader(Configuration conf) {
        boolean recursive = conf.getBoolean(CONF_RECURSIVE, DEFAULT_RECURSIVE);
        boolean reverseSort = conf.getBoolean(CONF_REVERSE_SORT, DEFAULT_REVERSE_SORT);
        Pattern filePattern = Pattern.compile(conf.getString(CONF_FILE_PATTERN, DEFAULT_FILE_PATTERN));
        String postfix = conf.getString(CONF_COMPLETED_POSTFIX, DEFAULT_COMPLETED_POSTFIX);
        String realPostfix = "".equals(postfix) ? null : postfix;

        String rootString = conf.getString(CONF_ROOT_FOLDER);
        if ("".equals(rootString)) {
            throw new Configurable.ConfigurationException(String.format(
                "No root. This must be specified with %s",
                CONF_ROOT_FOLDER));
        }
        log.trace("Got root-property '" + rootString + "'");
        TFile root = new TFile(rootString).getAbsoluteFile();
        log.debug(String.format("Setting root to '%s' from value '%s'", root, rootString));

        if (!root.exists()) {
            //noinspection DuplicateStringLiteralInspection
            log.warn(String.format("Root '%s'' does not exist. No files will be read", root));
            provider = new EmptyProvider();
        } else if (root.isFile()) {
            log.debug(String.format("Root '%s' is a single regular file", root));
            provider = new SingleFile(null, root, false, realPostfix);
        } else {
            log.debug(String.format("Root '%s' is a folder or an archive", root));
            provider = new FileContainer(null, root, false, realPostfix, filePattern, reverseSort);
        }

        log.info("ArchiveReader created. Root: '" + root + "', recursive: " + recursive
                 + ", file pattern: '" + filePattern.pattern() + "', completed postfix: '" + postfix + "'");
    }

    @Override
    public void setSource(Filter source) {
        throw new UnsupportedOperationException(String.format(
                "A %s must be positioned at the start of a filter chain",
                getClass().getName()));
    }

    @Override
    public boolean hasNext() {
        log.trace("hasNext() called");
        return provider.hasNext();
    }

    @Override
    public boolean pump() throws IOException {
        log.trace("pump() called");
        if (hasNext()) {
            Payload next = next();
            //noinspection StatementWithEmptyBody
            while (next.pump()); // Empty the Payload
        }
        return hasNext();
    }

    @Override
    public void close(boolean success) {
        log.debug("close(" + success + ") called");
        provider.close(success);
    }

    @Override
    public Payload next() {
        log.trace("next() called");
        return provider.next();
    }

    private static final List<FileProvider> EMPTY = new ArrayList<FileProvider>(0);

    private static abstract class FileProvider
        implements Iterator<Payload> {
        /**
         * Optional parent (folder or archive).
         */
        protected final FileProvider parent;
        /**
         * The main entry representing the provider. If the entry is a file,
         * this will be the only entry.
         */
        protected final TFile root;
        /**
         * If true, the provider is an inner element of an archive. The direct
         * consequence is that no renaming will take place on close.
         */
        protected final boolean inArchive;
        
        protected final String postfix;
        
        private boolean isRenamed = false;
        private boolean allOK = true;
        private static int warnCount = 0; // Not the cleanest design as it works badly with multiple ArchiveReaders

        private FileProvider(FileProvider parent, TFile root, boolean inArchive, 
                             String postfix) {
            this.parent = parent;
            this.root = root;
            this.inArchive = inArchive;
            this.postfix = postfix;
        }
        
        public synchronized void cleanup() {
            umount(root);
            if (parent != null) {
                parent.cleanup();
            }
        }

        private void umount(TFile file) {
            if (root != null) {
                try {
                    TVFS.umount(root);
                } catch (FsSyncException e) {
                    if (warnCount++ < 10) {
                        log.warn("Unable to umount file. This warning will be showed a max of 10 times. File: " + root,
                                 e);
                    }
                }
            }
        }

        /**
         * @return true iff {@link #isSafeToRemove()} is true and the provider
         * is a file or an archive, not embedded in another archive.
         */
        public boolean isSafeToRename() {
            return isSafeToRemove() && !"".equals(postfix) && !inArchive
                   && (root.isArchive() || root.isFile());
        }

        /**
         * @return true iff all files has been delivered as streams and all
         * streams has been closed.
         */
        protected abstract boolean isSafeToRemove();

        /**
         * Rename the file or archive if {@link #isSafeToRename()} is true and
         * the file or archive has not been renamed previously.
         * This method is safe to call multiple times.
         */
        public synchronized void rename() {
            log.trace("rename() called");
            if (isRenamed || !allOK || !isSafeToRename()) {
                return;
            }
            if (postfix == null || "".equals(postfix)) {
                log.debug("No renaming of '" + root + "' as postfix is empty");
                return;
            }
            log.debug(String.format(
                "%s: Renaming '%s' with completed postfix '%s'",
                this.getClass().getSimpleName(), root.getAbsolutePath(),
                postfix));
            // We use File instead of TFile for rename to avoid folder creation
            File newFile = new File(root.getPath() + postfix);
            if (newFile.exists()) {
                log.warn("A file with the name '" + newFile + "' already "
                         + "exists. The file will be deleted");
                if (!newFile.delete()) {
                    log.warn("Deletion of '" + newFile + "' was unsuccessful");
                }
            }
            try {
                if (root == root.getTopLevelArchive()) {
                    TVFS.umount(root);
                }
            } catch (FsSyncException e) {
                log.warn("Explicit umount of TFile("+ root.getAbsolutePath() + ") failed. This "
                         + "might leave non-renamed files", e);
            }
            if (!new File(root.getPath()).renameTo(newFile)) {
                log.warn(String.format("Unable to rename '%s' to '%s'", root, newFile));
            } else {
                isRenamed = true;
                if (root.exists()) {
                    log.warn(this.getClass().getSimpleName() +
                             ": Although the renaming of '" + root + "' to '" + newFile + "' was reported successfully,"
                             + " a file with the old name is still present");
                }
            }
            //No longer touch files to update lastmodificationtime
        }

        /**
         * Premature close, allowing delivered Payloads to finish processing.
         * @param success if the close war benign.
         */
        public void close(boolean success) {
            allOK = false;
        }
    }

    private static class EmptyProvider extends FileProvider {

        private EmptyProvider() {
            super(null, new TFile("dummy"), false, "");
        }

        @Override
        protected boolean isSafeToRemove() {
            return true;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Payload next() {
            throw new IllegalAccessError("next() must never be called on the empty provider");
        }

        @Override
        public void remove() {
            // Do nothing
        }

        @Override
        public void close(boolean success) {
            // Do nothing
        }
    }

    private static class SingleFile extends FileProvider {
        private boolean open = false;
        private boolean finished = false;
        private boolean removed = false;

        public SingleFile(FileProvider parent, TFile root, boolean inArchive,
                          String postfix) {
            super(parent, check(root), inArchive, postfix);
        }

        private static TFile check(TFile root) {
            if (!root.isFile()) {
                throw new IllegalArgumentException(String.format(
                    "The TFile '%s' was not a regular file", root));
            }
            return root;
        }

        @Override
        public synchronized boolean hasNext() {
            return !open && !finished && !removed;
        }

        @Override
        public synchronized Payload next() {
            if (!hasNext()) {
                throw new IllegalStateException(
                    "hasNext() is false so no next() is allowed");
            }
            open = true;
            TFileInputStream stream;
            try {
                stream = new TFileInputStream(root);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(
                    "Unable to open stream for '" + root + "'", e);
            }
            // TODO: test implications of disabling auto close
            InputStream closingStream = new CloseCallbackStream(stream, false) {
                @Override
                public void callback() {
                    log.trace("Closing stream from '" + root + "'");
                    open = false;
                    finished = true;
                    cleanup();
                }
            };
            Payload payload = new Payload(closingStream, root.getPath());
            payload.getData().put(Payload.ORIGIN, root.getPath());
            if (log.isTraceEnabled()) {
                log.trace("Created " + payload);
            }
            return payload;
        }

        @Override
        public synchronized void remove() {
            if (open) {
                throw new IllegalStateException("The file '" + root + " is currently open");
            }
            log.debug("Skipping '" + root + "'");
            removed = true;
            cleanup();
        }

        @Override
        protected boolean isSafeToRemove() {
            return finished || removed;
        }

        @Override
        public synchronized void close(boolean success) {
            log.trace("close(" + success + ") called");
            if (hasNext()) {
                remove();
            }
            try {
                TVFS.umount(root);
            } catch (FsSyncException e) {
                log.warn("Unable to umount " + root, e);
            }
            super.close(success);
        }

        @Override
        public String toString() {
            return "SingleFile(" + root + ")";
        }
    }

    /**
     * A folder or an archive.
     * </p><p>
     * As renaming of archives should only take place when all containing files
     * has been processed, a hierarchy of FileProviders with open streams is
     * held in memory. The hierarchy is auto collapsed as streams are closed.
     */
    private static class FileContainer extends FileProvider {
        private final Pattern filePattern;
        private final boolean reverseSort;

        /**
         * If the root is a folder or an archive, it can have 0 or more sub
         * providers, representing the content.
         * </p><p>
         * subs is lazily expanded where null means not expanded yet.
         */
        private List<FileProvider> subs = null; // null means not expanded yet
        /**
         * A list of providers with open streams. Providers from subs are placed
         * here when they have no more unopened files and removed when the files
         * are closed.
         */
        private List<FileProvider> open = new ArrayList<FileProvider>(1);

        /**
         * Constructs a lazily expanded provider.
         * @param parent    optional parent.
         * @param root      the entry in the file system/archive hierarchy.
         * @param inArchive if true, the provider resides in an archive and
         *                  should never have its name changes as part of close.
         * @param postfix   optional postfix for completed files.
         * @param filePattern the pattern that files must match to be streamed.
         * @param reverseSort if true, entries are expanded in reverse order.
         */
        public FileContainer(FileProvider parent, TFile root, boolean inArchive, String postfix, Pattern filePattern,
                             boolean reverseSort){
            super(parent, checkContainer(root), inArchive, postfix);
            this.filePattern = filePattern;
            this.reverseSort = reverseSort;
        }

        private static TFile checkContainer(TFile root) {
            if (!root.isDirectory() && !root.isArchive()) {
                throw new IllegalArgumentException("The provided file '" + root + "' must not be a regular file");
            }
            return root;
        }

        int umountWarnCount = 0;
        private List<FileProvider> expand(TFile source) {
            if (source.isFile()) {
                return EMPTY;
            }
//            System.out.println("Listing files for " + source);
            TFile[] files = source.listFiles();
            List<FileProvider> providers = new ArrayList<FileProvider>(files.length);
            for (TFile file: files) {
                try {
                    if (postfix != null && file.getName().endsWith(postfix)) {
                        log.trace("Skipping '" + file.getName() + "' as it is " + "marked with the completed postfix");
// TODO: This test results in auto-mounting. Maybe we can do isDirectory and pattern-match first?
                    } else if (file.isDirectory() || file.isArchive()) {
//                        System.out.println(" Adding folder " + file);

                        providers.add(new FileContainer(this, file, inArchive || root.isArchive(), postfix,
                                                        filePattern, reverseSort));
                    } else if (filePattern.matcher(file.getName()).matches()) {
//                        System.out.println(" Adding file  " + file);
                        providers.add(new SingleFile(this, file, inArchive || root.isArchive(), postfix));
                    } else {
                        log.debug("Skipping '" + file.getName() + "' as it does not match the pattern '"
                                  + filePattern.pattern() + "'");
                    }
                    try {
                        TVFS.umount(file);
                    } catch (FsSyncException e) {
                        if (umountWarnCount++ < 10) {
                            log.warn("Unable to unmount " + file + ". This error will be displayed maximum 10 times", e);
                        }
                    }
                } catch (NullPointerException e) {
                    if (file.getName().contains(":")) {
                        log.warn("Got NPE while accessing a name with colon. TrueZIP 7.0-pr2 does not support this and "
                                 + "although later versions claims to do so, this is likely the cause for the NPE. "
                                 + "The offending name is '" + file.getAbsolutePath() + "'");
                        continue;
                    }
                    NullPointerException e2 = new NullPointerException(
                            "NPE during access to '" + file + "' from '" + source + "'");
                    e2.initCause(e);
                    throw e2;
                }
            }
            Collections.sort(providers, new Comparator<FileProvider>() {
                @Override
                public int compare(FileProvider o1, FileProvider o2) {
                    return o1.root.getName().compareTo(o2.root.getName());
                }
            });
            if (reverseSort) {
                Collections.reverse(providers);
            }
            return providers;
        }

        @Override
        public boolean isSafeToRemove() {
            return subs != null && subs.size() == 0 && open.size() == 0;
        }

        @Override
        public synchronized boolean hasNext() {
            if (subs == null) { // Expand if possible
                subs = expand(root);
            }
            while (subs.size() > 0) {
                FileProvider currentProvider = subs.get(0);
                if (subs.get(0).hasNext()) {
                    return true;
                }
                // A remove might have been triggered
                if (subs.size() == 0) {
                    break;
                }
                if (subs.get(0) != currentProvider) {
                    // Cleanup has been triggered, start over
                    continue;
                }
                // Current does not have next but has open files
                FileProvider sub = subs.remove(0);
                open.add(sub);
            }
            cleanup();
            return false;
        }

        /**
         * Call this whenever there is a chance that the provider has been
         * depleted. Clean will call parent.clean() if the current provider
         * is fully depleted.
         */
        @Override
        public synchronized void cleanup() {
            log.trace("FileContainer: cleanUp() called");
            if (subs == null) {
                return; // Not expanded yet
            }
            cleanup(subs);
            cleanup(open);
            if (log.isTraceEnabled()) {
                log.trace("Filecontainer: cleanUp() left " + subs.size() + " pending subs and "
                          + open.size() + " open subs");
            }
            if (!isSafeToRemove()) {
                return;
            }
            rename();
            super.cleanup();
        }
        private void cleanup(List<FileProvider> p) {
            // Only remove until something unfinished is reached
            while (p.size() > 0 && p.get(0).isSafeToRemove()) {
//                System.out.println("Removing " + p.get(0));
                p.remove(0).rename();
            }
        }

        @Override
        public synchronized Payload next() {
            if (!hasNext()) {
                throw new IllegalStateException(
                    "hasNext() is false so next() should not be called");
            }
            if (subs.size() > 0) { // hasNext guarantees content in the first
                return subs.get(0).next();
            }
            throw new InternalError(
                "There were no next in the first sub, but this was guaranteed "
                + "by hasNext()");
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not allowed");
        }

        @Override
        public synchronized void close(boolean success) {
            close(success, subs);
            close(success, open);
            super.close(success);    
        }
        private synchronized void close(boolean success, List<FileProvider> p) {
            int index = p.size()-1;
            while (index >= 0) {
                p.get(index--).close(success);
            }
        }

        @Override
        public String toString() {
            return "FileContainer('" + root + ", " + (subs == null ? "subs not expanded" : subs.size() + " subs") + ")";
        }
    }
}
