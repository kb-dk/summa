/* $Id: URLRepository.java,v 1.4 2007/10/29 14:38:15 mke Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/29 14:38:15 $
 * $Author: mke $
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
package dk.statsbiblioteket.summa.score.bundle;

import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.*;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>A {@link BundleRepository} fetching bundles via Java {@link URL}s.</p>
 *
 * <p>Given a bundle id it is mapped to a URL as specified by the
 * {@link #BASE_URL} property in the {@link Configuration}.</p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class URLRepository implements BundleRepository {

    /** <p>Property defining where the repository is located.
     * Request to {@link #get(String bundleId)} will try and download
     * the URL</p>
     * <code>
     *    ${summa.score.repository.url}/${bundleId}.bundle 
     * </code>
     *  */
    public static final String BASE_URL = "summa.score.repository.url";

    private String tmpDir;
    private Log log;
    private String baseUrl;

    public URLRepository (Configuration conf) {
        this.tmpDir = conf.getString(DOWNLOAD_DIR);

        if (tmpDir == null || "".equals(tmpDir)) {
            throw new ConfigurationException("Insufficient configuration. "
                                           + "Property " + DOWNLOAD_DIR
                                           + " not set");
        }

        this.baseUrl = conf.getString (BASE_URL);

        if (baseUrl == null || "".equals(baseUrl)) {
            throw new ConfigurationException("Insufficient configuration. "
                                           + "Property " + BASE_URL
                                           + " not set");
        }

        /* make sure baseurl ends with a slash */
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }


        this.log = LogFactory.getLog(this.getClass());

        log.debug ("Created " + this.getClass().getName()
                 + " instance with base url " + baseUrl);
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

        Streams.pipeStream(con, out);

        log.debug ("Done downloading " + url + " to " + result);

        return result;

    }
}
