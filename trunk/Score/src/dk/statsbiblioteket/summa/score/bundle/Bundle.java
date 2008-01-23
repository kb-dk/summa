/* $Id: Bundle.java,v 1.3 2007/10/11 12:56:25 te Exp $
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

import dk.statsbiblioteket.summa.score.client.Client;
import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Package interface for client- and service bundles.
 * Contains shared constants.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface Bundle {

    /**
     * Configuration used to instantiate the {@link Client} or {@link Service}
     */
    public static final String BUNDLE_CONFIGURATION = "summa.score.bundle.configuration";

    /**
     * Configuration used for the {@link BundleStub}
     */
    public static final String STUB_CONFIGURATION = "summa.score.bundle.stub.configuration";

    /**
     * The id, passed as a system property, to a {@link Client}
     */
    public static final String CLIENT_ID = "summa.score.client.id";

    /**
     * The id, passed as a system property, to a {@link Service}
     */
    public static final String SERVICE_ID = "summa.score.service.id";

    /**
     * The full path to the root of the bundle installation, passed as a
     * system property.
     */
    public static final String BUNDLE_DIR = "summa.score.bundle.dir";

    /**
     * Extension used for bundle files.
     */
    public static final String BUNDLE_EXT = ".bundle";

}
