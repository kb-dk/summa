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
package dk.statsbiblioteket.summa.index;

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Lucene-related helper methods for indexing.
 */
// TODO: Consider moving this to Common as Search can also use it
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneIndexUtils {
    private static Log log = LogFactory.getLog(LuceneIndexUtils.class);

    /**
     * The property-key for the substorage containing the setup for the
     * LuceneIndexDescriptor.
     */
    public static final String CONF_DESCRIPTOR = "summa.index.descriptor-setup";

    /**
     * Get the sub-properties {@link #CONF_DESCRIPTOR} and create a descriptor
     * based on that. If the sub-properties does not exist, the default
     * descriptor is created.
     * @param conf a configuration with the setup for descriptor stored as a
     *             sub-configuration with the key {@link #CONF_DESCRIPTOR}.
     * @return a descriptor usable for indexing (and querying).
     * @throws Configurable.ConfigurationException if there was an error with
     *                                             the configuration.
     */
    public static LuceneIndexDescriptor getDescriptor(Configuration conf) throws
                                           Configurable.ConfigurationException {
        Configuration descConf = null;
        try {
            descConf = conf.getSubConfiguration(CONF_DESCRIPTOR);
        } catch (IOException e) {
            //noinspection DuplicateStringLiteralInspection
            log.error("Exception requesting '" + CONF_DESCRIPTOR
                      + "' from properties");
        } catch (UnsupportedOperationException e) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("The configuration does not support sub-storages. "
                      + "Attempting to use the storage directly");
            descConf = conf;
        }
        LuceneIndexDescriptor descriptor;
        if (descConf == null) {
            log.warn("No '" + CONF_DESCRIPTOR + "' specified in properties. "
                     + "Using default LuceneIndexDescriptor");
            descriptor = new LuceneIndexDescriptor();
        } else {
            log.trace("Creating LuceneIndexDescriptor based on properties");
            try {
                descriptor = new LuceneIndexDescriptor(descConf);
            } catch (IOException e) {
                throw new Configurable.ConfigurationException(
                        "Exception creating LuceneIndexDescriptor based "
                        + "on properties", e);
            }
        }
        return descriptor;
    }
}
