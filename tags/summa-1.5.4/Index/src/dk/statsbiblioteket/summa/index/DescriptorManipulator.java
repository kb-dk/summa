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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

/**
 * A simple manipulator that keeps track of the IndexDescriptor and store
 * the XML in the index-folder upon commit and consolidate.
 * </p><p>
 * In order for the manipulator to work, the setup for the IndexDescriptor must
 * be included in the configuration.
 * @see {@link
 *      dk.statsbiblioteket.summa.common.index.IndexDescriptor#CONF_DESCRIPTOR}.
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DescriptorManipulator implements IndexManipulator {
    private static Log log = LogFactory.getLog(DescriptorManipulator.class);

    private File indexRoot = null;
    private StoringIndexDescriptor descriptor = null;

    public DescriptorManipulator(Configuration conf) {
        if (!conf.valueExists(IndexDescriptor.CONF_DESCRIPTOR)) {
            log.warn(String.format(
                    "The IndexDescriptor-setup with key '%s' was not defined. "
                    + "No descriptor-XML will be stored with the index-data",
                    IndexDescriptor.CONF_DESCRIPTOR));
            return;
        }
        Configuration descriptorConf;
        try {
            descriptorConf =
                    conf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR);
        } catch (IOException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new ConfigurationException(String.format(
                    "Unable to extract sub configuration %s",
                    IndexDescriptor.CONF_DESCRIPTOR));
        }
        try {
            descriptor = new StoringIndexDescriptor(descriptorConf);
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Exception initializing IndexDescriptor", e);
        }
        log.debug("DescriptorManipulator ready for use");
    }

    public void open(File indexRoot) throws IOException {
        log.debug(String.format("open(%s) called. Setting this as destination "
                                + "for the IndexDescriptor", indexRoot));
        this.indexRoot = indexRoot;
    }

    public void clear() throws IOException {
        log.trace("clear() does nothing");
    }

    public boolean update(Payload payload) throws IOException {
        log.trace("update(...) called. No action taken");
        return false;
    }

    public void commit() throws IOException {
        if (indexRoot == null) {
            log.error("No indexRoot specified. This indicates that open(...) "
                      + "has not been called. Unable to store IndexDescriptor");
            return;
        }
        if (descriptor == null) {
            log.warn(String.format(
                    "No descriptor available. Unable to store IndexDescriptor "
                    + "to '%s'", indexRoot));
            return;
        }
        if (descriptor.getXml() == null) {
            log.warn(String.format(
                    "No descriptor XML available. Unable to store "
                    + "IndexDescriptor to '%s'", indexRoot));
            return;
        }
        File descFile =
                new File(indexRoot, IndexDescriptor.DESCRIPTOR_FILENAME);
        if (descFile.exists()) {
            log.debug("Removing existing IndexDescriptor at '"
                      + descFile + "'");
            if (!descFile.delete()) {
                log.warn(String.format(
                        "Unable to remove '%s'. This might mean that it is not "
                        + "possible to update the IndexDescriptor", descFile));
            }
        }
        Files.saveString(descriptor.getXml(), descFile);
        if (log.isTraceEnabled()) {
            log.debug(String.format("Stored IndexDescriptor to '%s':\n%s",
                                    descFile, descriptor.getXml()));
        } else {
            log.debug(String.format("Stored IndexDescriptor of size %d to '%s'",
                                    descriptor.getXml().length(), descFile));
        }
    }

    public void consolidate() throws IOException {
        commit();
    }

    public void close() throws IOException {
        indexRoot = null;
        if (descriptor != null) {
            descriptor.close();
            descriptor = null;
        }
    }

    @Override
    public void orderChangedSinceLastCommit() {
        // Don't care
    }

    @Override
    public boolean isOrderChangedSinceLastCommit() {
        return false;
    }
}

