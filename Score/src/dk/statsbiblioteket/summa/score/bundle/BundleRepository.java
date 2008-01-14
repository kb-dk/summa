/* $Id: BundleRepository.java,v 1.3 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/11 12:56:25 $
 * $Author: te $
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
public interface BundleRepository extends Configurable {

    /**
     * <p>The {@link Configuration} property pointing at the place to store
     * downloaded bundles in files in.</p>
     *
     * <p>If unset {@code ${user.home}/tmp} should be used.</p>
     */
    public static final String DOWNLOAD_DIR =
                                          "summa.score.repository.download.dir";    

    /**
     * Retrieve a bundle returning a {@link File} reference to it.
     * The bundle file will be stored in
     * {@code summa.score.repository.tmp.dir} as provided by the configuration.
     * @param bundleId bundle id of the bundle to retrieve
     * @return a reference to the temporary {@code .bundle} file
     * @throws IOException if there is an error retrieving the bundle
     */
    public abstract File get (String bundleId) throws IOException;

}
