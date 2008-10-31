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
package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;

import java.io.*;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>A {@link dk.statsbiblioteket.summa.control.api.bundle.BundleRepository} fetching bundles via Java {@link URL}s.</p>
 *
 * <p>Given a bundle id it is mapped to a URL as specified by the
 * {@link #CONF_REPO_ADDRESS} property in the {@link Configuration}.</p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class URLRepository implements BundleRepository {

    private String tmpDir;
    private Log log = LogFactory.getLog(this.getClass());

    /**
     * Set from the {@link #CONF_BASE_URL} property
     */
    protected String baseUrl;

    /**
     * Set from the {@link #CONF_API_BASE_URL}
     */
    protected String apiBaseUrl;

    /**
     * Configuration property defining the base URL from bundles are
     * downloaded. Consumers will download the bundle files from
     * {@code <baseUrl>/<bundleId>.bundle}.
     * <p></p>
     * If this property is unset the value {@link #CONF_REPO_ADDRESS} of
     * will be used. If this property again is unset, the fallback value will be
     * {@link #DEFAULT_REPO_URL}
     */
    public static final String CONF_BASE_URL =
                                         "summa.control.repository.baseurl";

    /**
     * Configuration property defining the base URL from which jar files are
     * served. Consumers will download the jar files from
     * {@code <baseUrl>/<jarFileName>}. This controls the behavior of
     * {@link #expandApiUrl}.
     * <p></p>
     * If this property is unset {@link #baseUrl}{@code /api}
     * will be used as fallback.
     */
    public static final String CONF_API_BASE_URL =
                                         "summa.control.repository.api.baseurl";

    /**
     * Fallback value for the {@link #CONF_BASE_URL} used if the
     * initial fallback property {@link #CONF_REPO_ADDRESS} is also unset.
     */
    public static final String DEFAULT_REPO_URL = "file://"
                                                  + System.getProperty("user.home")
                                                  + "/summa-control"
                                                  + "/repository";

    /**
     * <p>Create a new URLRepository. If the {@link #CONF_DOWNLOAD_DIR} is
     * not set {@code tmp/} relative to the working directory will be used.</p>
     *
     * <p>If {@link #CONF_REPO_ADDRESS} is not set in the configuration
     * <code>file://${user.home}/summa-control/repo</code> is used.</p>
     * @param conf
     */
    public URLRepository (Configuration conf) {
        this.tmpDir = conf.getString(CONF_DOWNLOAD_DIR, "tmp");

        String repoAddress = conf.getString (BundleRepository.CONF_REPO_ADDRESS,
                                             DEFAULT_REPO_URL);
        this.baseUrl = conf.getString (CONF_BASE_URL,
                                       repoAddress);

        /* make sure baseUrl ends with a slash */
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        this.apiBaseUrl = conf.getString (URLRepository.CONF_API_BASE_URL,
                                          baseUrl + "api");

        /* make sure apiBaseUrl ends with a slash */
        if (!apiBaseUrl.endsWith("/")) {
            apiBaseUrl += "/";
        }

        log.debug ("Created " + this.getClass().getName()
                 + " instance with base URL " + baseUrl + " and API URL "
                 + apiBaseUrl);
    }

    public File get(String bundleId) throws IOException {
        log.trace ("Getting '" + bundleId + " from URLRepository");
        URL bundleUrl = new URL (baseUrl + bundleId + ".bundle");
        return download(bundleUrl);

    }

    private File download (URL url) throws IOException {
        log.trace("Preparing to download " + url);
        String filename = url.getPath();
        filename = filename.substring (filename.lastIndexOf("/") + 1);
        File result = new File (tmpDir, filename);

        /* Ensure tmpDir exists */
        new File (tmpDir).mkdirs();

        /* make sure we find an unused filename to store the downloaded pkg in */
        int count = 0;
        while (result.exists()) {
            log.trace ("Target file " + result + " already exists");
            result = new File (tmpDir, filename + "." + count);
            count++;
        }

        log.debug ("Downloading " + url + " to " + result);
        InputStream con = new BufferedInputStream(url.openStream());
        OutputStream out = new BufferedOutputStream(
                                                 new FileOutputStream (result));

        Streams.pipe (con, out);

        log.debug ("Done downloading " + url + " to " + result);

        return result;

    }

    public List<String> list (String regex) throws IOException {
        log.trace ("Got list() request for '" + regex + "'");

        if (!baseUrl.startsWith("file://")) {
            throw new UnsupportedOperationException("Only 'file://'-based "
                                                    + "repositories support "
                                                    + "listing. Baseurl: "
                                                    + baseUrl);
        }

        File baseDir = new File (new URL(baseUrl).getFile());
        Pattern pat = Pattern.compile(regex);
        List<String> result = new ArrayList <String> (10);

        for (String bdl : baseDir.list()) {
            if (!bdl.endsWith(".bundle")) {
                log.trace ("Skipping non-.bundle '" + bdl + "'");
                continue;
            }
            bdl = bdl.replace (".bundle", "");
            if (pat.matcher(bdl).matches()) {
                result.add (bdl);
                log.trace ("Match: " + bdl);
            } else {
                log.trace ("No match: " + bdl);
            }
        }

        return result;
    }

    public String expandApiUrl (String jarFileName) throws IOException {
        // By contract we must return URLs as is
        if (jarFileName.startsWith("http://") ||
            jarFileName.startsWith("https://")||
            jarFileName.startsWith("ftp://")  ||
            jarFileName.startsWith("sftp://")) {
            log.trace ("Returning AIP URL of jar file as is: " + jarFileName);
            return jarFileName;
        }

        /* Only take the basename into account */
        jarFileName = new File (jarFileName).getName();

        String result = apiBaseUrl + jarFileName;
        log.trace ("API URL of " + jarFileName + " is: " + result);
        return result;
    }
}



