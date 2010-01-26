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




