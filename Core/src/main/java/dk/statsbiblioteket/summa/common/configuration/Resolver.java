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
package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

/**
 * Resolves paths relative to System properties.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class Resolver {
    private static Log log = LogFactory.getLog(Resolver.class);

    /**
     * The location of the persistent dir for a service. This can be resolved
     * by calling System.getProperty(SYSPROP_PERSISTENT_DIR).
     */
    public static final String SYSPROP_PERSISTENT_DIR = "summa.control.client.persistent.dir";

    /**
     * Transforms the given file to an absolute path, if it is not absolute
     * already. The absolute location will be relative to the System property
     * "summa.control.client.persistent.dir". If that system property does not
     * exist, the location will be relative to the current dir.
     *
     * @param file a file that should be relative to the persistent dir.
     * @return the file relative to the persistent dir.
     */
    public static File getPersistentFile(File file) {
        if (file == null) {
            log.warn("Got null in getPersistentFile. Returning null");
            return null;
        }
        if (file.getPath().equals(file.getAbsolutePath())) {
            log.trace("getPersistentFile(" + file.getPath() + ") got a file that was already absolute");
            return file;
        }
        String persistentBase;
        try {
            persistentBase = System.getProperty(SYSPROP_PERSISTENT_DIR);
        } catch (NullPointerException e) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("System property '" + SYSPROP_PERSISTENT_DIR + "' not defined");
            return file;
        } catch (SecurityException e) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("The SecurityManager disallows the extraction of system property '" + SYSPROP_PERSISTENT_DIR
                     + "'");
            return file;
        }
        log.trace("Resolved system property '" + SYSPROP_PERSISTENT_DIR + "' to '" + persistentBase + "'");
        if (persistentBase == null || "".equals(persistentBase)) {
            log.debug("No persistentBase specified for system property " + SYSPROP_PERSISTENT_DIR
                      + ". Returning path '" + file.getAbsolutePath() + "' directly");
            return file.getAbsoluteFile();
        }
        try {
            File persistentBaseFile = new File(persistentBase);
            File resolved = new File(persistentBaseFile, file.getPath()).getAbsoluteFile();
            log.debug("Returning resolved File '" + resolved.getPath() + "'");
            return resolved;
        } catch (Exception e) {
            log.info(String.format(
                    Locale.ROOT, "Could not resolve File '%s' to persistentBaseFile with persistent base '%s'. "
                                 + "Using file directly as '%s'",
                    file, persistentBase, file.getAbsolutePath()));
            return file;
        }
    }


    /**
     * Catch-all URL resolver. Handles:<br />
     * HTTP-URLs: http://example.org/resource.xml<br />
     * File-URLs: file:///tmp/resource.txt<br />
     * Relative files: subdir/resource.xml<br />
     * Absolute paths: /tmp/resource.txt<br />
     * Class-loader resolvable: resource.xml<br />
     * The resource resolving is done in the order above.
     *
     * @param resource the resource to locate.
     * @return the URL for the resource, if possible, else null.
     */
    public static URL getURL(String resource) {
        if (resource == null) {
            log.debug("getURL: got null");
            return null;
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("getURL(" + resource + ") called");
        try {
            return new URL(resource);
        } catch (MalformedURLException e) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Malformed URL '" + resource + "'. Attempting file resolving");
        }
        File file = new File(resource);
        if (file.exists() && file.canRead()) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                log.debug("Could not convert the file '" + file + "' to URL. Attempting class-loader", e);
            }
        } else {
            log.trace("File(" + resource + ") cannot be accesses. Attempting class-loader resolving");
        }
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

    /**
     * Subset of the {@link #getURL} that only resolves Files.
     *
     * @param resource the resource to locate.
     * @return the File for the resource if possible, else null.
     */
    public static File getFile(String resource) {
        URL url = getURL(resource);
        try {
            return url == null ? null : new File(url.toURI());
        } catch (URISyntaxException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new RuntimeException(String.format(Locale.ROOT, "Unable to convert URL '%s' to URI", url), e);
        }
    }

    /**
     * Catch-all resource getter. Handles:<br />
     * HTTP-URLs: http://example.org/resource.xml<br />
     * File-URLs: file:///tmp/resource.txt<br />
     * Relative files: subdir/resource.xml<br />
     * Absolute paths: /tmp/resource.txt<br />
     * Class-loader resolvable: resource.xml<br />
     * The resource resolving is done in the order above.
     *
     * @param resource the resource to get, in a format as specified above.
     * @return the content of the resource, read as UTF8.
     * @throws IOException if the resource could not be retrieved.
     */
    public static String getUTF8Content(String resource) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("getUTF8Resource(" + resource + ") called");
        try {
            URL url = new URL(resource);
            return getUTF8Content(url);
        } catch (MalformedURLException e) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Malformed URL '" + resource + "'. Attempting file loading");
        } catch (IOException e) {
            log.debug("Could not get '" + resource + "' as an URL. Attempting file loading", e);
        }
        try {
            File file = new File(resource);
            if (file.exists() && file.canRead()) {
                return Files.loadString(file);
            } else {
                //noinspection DuplicateStringLiteralInspection
                log.trace("File(" + resource + ") can not be accesses. Attempting class-loader");
            }
        } catch (IOException e) {
            log.debug("Error accessing File(" + resource + "). Attempting class-loader", e);
        }
        return Streams.getUTF8Resource(resource);
    }

    /**
     * Retrieve an UTF-8 test file from the given location.
     *
     * @param location where to fetch the resource.
     * @return the UTF-8 string at the location.
     * @throws IOException if the string could not be requested.
     */
    public static String getUTF8Content(URL location) throws IOException {
        log.trace("getUTF8Content(URL(" + location + ")) called");
        if (location == null) {
            throw new IOException("Cannot get content from null");
        }
        //noinspection OverlyBroadCatchBlock
        try {
            InputStream in = location.openStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(1000);
            Streams.pipe(in, bytes);
            return bytes.toString("utf-8");
        } catch (IOException e) {
            throw new IOException("Could not get content of '" + location + "'", e);
        }
    }

    /**
     * Converts a URL to a File.
     * </p><p>
     * Note: Do not just make a {@code new File(url.getFile())} as it does not
     * work for much else that paths consisting of a-zA-Z0-9 due to escaping.
     *
     * @param url the URL to convert to File.
     * @return the URL as a File.
     */
    public static File urlToFile(URL url) {
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format(Locale.ROOT, "Unable to convert the URL '%s' to URI", url), e);
        }
    }
}
