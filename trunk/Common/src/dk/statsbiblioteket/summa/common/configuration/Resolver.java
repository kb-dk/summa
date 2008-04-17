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
package dk.statsbiblioteket.summa.common.configuration;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Resolves paths relative to System properties.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class Resolver {
    private static Log log = LogFactory.getLog(Resolver.class);

    /**
     * The location of the persistent dir for a service. This can be resolved
     * by calling System.getProperty(SYSPROP_PERSISTENT_DIR).
     */
    public static final String SYSPROP_PERSISTENT_DIR =
            "summa.score.client.persistent.dir";

    /**
     * Transforms the given file to an absolute path, if it is not absolute
     * already. The absolute location will be relative to the System property
     * "summa.score.client.persistent.dir". If that system property does not
     * exist, the location will be relative to the current dir.
     * @param file a file that should be relative to the persistent dir.
     * @return the file relative to the persistent dir.
     */
    public static File getPersistentFile(File file) {
        if (file == null) {
            log.warn("Got null in getPersistentFile. Returning null");
            return null;
        }
        if (file.getPath().equals(file.getAbsolutePath())) {
            log.trace("getPersistentFile(" + file.getPath() + ") got a file"
                      + " that was already absolute");
            return file;
        }
        String persistentBase;
        try {
            persistentBase = System.getProperty(SYSPROP_PERSISTENT_DIR);
        } catch (NullPointerException e) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("System property '" + SYSPROP_PERSISTENT_DIR
                     + "' not defined");
            return file;
        } catch (SecurityException e) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("The SecurityManager disallows the extraction of system "
                     + "property '" + SYSPROP_PERSISTENT_DIR + "'");
            return file;
        }
        log.trace("Resolved system property '" + SYSPROP_PERSISTENT_DIR
                  + "' to '" + persistentBase + "'");
        try {
            File persistentBaseFile = new File(persistentBase);
            File resolved =
                 new File(persistentBaseFile, file.getPath()).getAbsoluteFile();
            log.debug("Returning resolved File '" + resolved.getPath() + "'");
            return resolved;
        } catch (Exception e) {
            log.error("Could not transmute File '" + file
                      + "' to absolute File", e);
            return file;
        }
    }
}
