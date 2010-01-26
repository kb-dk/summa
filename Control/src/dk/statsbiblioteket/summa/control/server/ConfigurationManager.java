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
package dk.statsbiblioteket.summa.control.server;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.RemoteStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class ConfigurationManager implements Runnable, Configurable {

    /**
     * Property defining which class to use for {@link ConfigurationStorage}.
     * Default is
     * {@link dk.statsbiblioteket.summa.common.configuration.storage.XStorage}.
     */
    public static final String CONF_STORAGE =
                                    "summa.control.configurationmanager.storage";

    private Log log = LogFactory.getLog (ConfigurationManager.class);
    private ConfigurationStorage storage;
    private RemoteStorage remote;

    public ConfigurationManager (Configuration conf) {
        log.debug("Creating ConfigurationManager");
        setupStorage(conf);
        exportRemoteStorage(conf);
    }

    private void setupStorage (Configuration conf) {
        Class<? extends ConfigurationStorage> storageClass =
                                       conf.getClass(CONF_STORAGE,
                                                     ConfigurationStorage.class,
                                                     XStorage.class);
        log.debug ("Using configuration storage class: "
                   + storageClass.getName());
        storage = Configuration.create(storageClass, conf);
    }

    private void exportRemoteStorage (Configuration conf) {
        log.debug("Setting up remote interface");
        remote = Configuration.create(RemoteStorage.class, conf);
        log.trace("Overriding default storage for RemoteStorage");
        remote.setStorage(storage);
    }

    public ConfigurationStorage getExportedStorage () {
        return remote;
    }

    /**
     * <p>Get a string representing the public address on which
     * {@link dk.statsbiblioteket.summa.control.client.Client}s can connect to
     * the configuration managed by this ConfigurationManager.</p>
     *
     * <p>The returned address should be so that
     * {@link Configuration#getSystemConfiguration(String)} will work for
     * the clients.</p>
     *
     * <h2>FIXME: Limited to RMI atm</h2>
     * <p>Currently the only the remote RMI configuration interface
     * is supported. Thus this method will always return an RMI address,
     * but this might be changed in the future.</p>
     * @return address as descibed above
     */
    public String getPublicAddress () {
        return remote.getServiceUrl();
    }

    public void run() {

    }

    public static void main (String[] args) {
        Configuration conf = Configuration.getSystemConfiguration();
        ConfigurationManager cm = new ConfigurationManager(conf);

        cm.run();
    }
}




