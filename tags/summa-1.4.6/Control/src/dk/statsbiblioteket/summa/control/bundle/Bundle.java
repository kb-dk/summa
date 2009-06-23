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

import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.summa.control.api.Service;
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
    public static final String CONF_BUNDLE_CONFIGURATION =
                                           "summa.control.bundle.configuration";

    /**
     * Configuration used for the {@link BundleStub}
     */
    public static final String CONF_STUB_CONFIGURATION =
                                      "summa.control.bundle.stub.configuration";

    /**
     * The id, passed as a system property, to a {@link Client}
     */
    public static final String CONF_CLIENT_ID = "summa.control.client.id";

    /**
     * The id, passed as a system property, to a {@link Service}
     */
    public static final String CONF_SERVICE_ID = "summa.control.service.id";

    /**
     * The full path to the sourceRoot of the bundle installation, passed as a
     * system property.
     */
    public static final String CONF_BUNDLE_DIR = "summa.control.bundle.dir";

    /**
     * Property defining whether the service or client defined by the bundle
     * should be automatically started.
     * <p/>
     * Clients will be started by the Control server while Services will
     * be started by the hosting Client server.
     * <p/>
     * The default value is {@link #DEFAULT_AUTO_START}
     */
    public static final String CONF_AUTO_START =
                                               "summa.control.bundle.autostart";

    /**
     * Default value for the {@link #CONF_AUTO_START} property
     */
    public static final boolean DEFAULT_AUTO_START = false;

    /**
     * Extension used for bundle files.
     */
    public static final String BUNDLE_EXT = ".bundle";

    /**
     * Enumeration of bundle types
     */
    public enum Type {
        /** Specifies that a bundle is a Client bundle */
        CLIENT,

        /** Specifies that a bundle is a Service bundle */
        SERVICE
    }

}



