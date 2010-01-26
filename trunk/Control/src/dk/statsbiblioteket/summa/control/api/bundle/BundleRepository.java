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
package dk.statsbiblioteket.summa.control.api.bundle;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Class needs Javadoc")
public interface BundleRepository extends Configurable, Serializable {

    /**
     * <p>The {@link Configuration} property pointing at the place to store
     * downloaded bundles in files in.</p>
     *
     * <p>If unset {@code ${user.home}/tmp} should be used.</p>
     */
    public static final String CONF_DOWNLOAD_DIR =
                                          "summa.control.repository.download.dir";

    public static final String DEFAULT_DOWNLOAD_DIR =
                                                System.getProperty("user.home")
                                              + File.separator
                                              + "tmp";

    /**
     * Configuration property defining the address the repository is
     * reachable on. If the repository is a {@link dk.statsbiblioteket.summa.control.bundle.URLRepository} this should
     * be an URI, if it is a repository exposed over RMI it should be the
     * RMI address etc.
     * <p></p>
     * Implementation should assume sensible default values if this property
     * is unset. 
     */
    public static final String CONF_REPO_ADDRESS =
                                           "summa.control.repository.address";

    /**
     * Retrieve a bundle returning a {@link File} reference to it.
     * The bundle file will be stored in
     * {@code summa.control.repository.download.dir} as provided by the
     * configuration.
     * @param bundleId bundle id of the bundle to retrieve
     * @return a reference to the temporary {@code .bundle} file
     * @throws IOException if there is an error retrieving the bundle
     */
    public File get (String bundleId) throws IOException;

    /**
     * Retrieve a list of available bundles, matching a regular expression.
     * <p></p>
     * This method is optional to implement. Implementations not supporting
     * this method must throw {@link UnsupportedOperationException}
     *
     * @param regex a regular expression the bundle names must match. Use '.*'
     *              to list all bundles.
     * @return a list of bundle ids
     * @throws IOException on communication errros
     * @throws UnsupportedOperationException if the repository does not support
     *                                       listing
     */
    public List<String> list (String regex) throws IOException;

    /**
     * Return a string encoded URL for where the given jar file can be
     * downloaded.
     * <p></p>
     * This is useful for building the system property
     * {@code java.rmi.server.codebase} or similar where consumers need a place
     * to look up custom classes used in the reply from a server process.
     * <p></p>
     * If {@code jarFileName} already start with an URL scheme, ie.
     * {@code http://}, {@code ftp://}, or what ever, the repository should
     * return {@code jarFileName} unmodified.
     * <p></p>
     * The repository will only use the basename of the provided file
     * to construct the URL. Applications are free to pass in full paths
     * to the jar files, like
     * <pre>
     *    lib/myLib-0.1.jar
     * </pre>
     * Which might resolve to something like
     * <pre>
     *    http://meanmachine:8080/summa-control/api/myLib-0.1.jar
     * </pre>
     * @param jarFileName path to .jar file to look up an API URL for
     * @return A valid URL pointing at a place where the jar file can be
     *         downloaded or {@code null} if the jar file is not found in the
     *         API segment of the repository. If {@code jarFileName} is already
     *         an URL then it will be returned unmodified
     */
    public String expandApiUrl (String jarFileName) throws IOException;
}




