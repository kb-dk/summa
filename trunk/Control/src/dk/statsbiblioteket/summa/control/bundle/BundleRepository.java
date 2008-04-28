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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

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
    public static final String DOWNLOAD_DIR_PROPERTY =
                                          "summa.control.repository.download.dir";

    /**
     * Configuration property defining the address the repository is
     * reachable on. If the repository is a {@link URLRepository} this should
     * be an URI, if it is a repository exposed over RMI it should be the
     * RMI address etc.
     */
    public static final String REPO_ADDRESS_PROPERTY =
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
    public abstract File get (String bundleId) throws IOException;

}
