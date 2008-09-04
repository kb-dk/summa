/* $Id: FacetCore.java,v 1.5 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.facetbrowser.core;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;

/**
 * The core is responsible for loading existing Facet-structures and for
 * handling Facet setup in the form of configuration.
 * </p><p>
 * @see {link Structure} for Configuration parameters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface FacetCore extends Configurable {
    /**
     * The folder containing all persistent facet-information.
     */
    String FACET_FOLDER = "facet";

    /**
     * Open a Facet structure at the given location. If there is no existing
     * structure, .
     * @param directory the location of the data.
     * @throws IOException if the data could not be read from the file system.
     */
    public void open(File directory) throws IOException;

    /**
     * Closes any connections to underlying persistent data and clears
     * structures in memory.
     * </p><p>
     * Note: This does not synchronize content in memory to storage.
     * @throws IOException if opened persistent files could not be closed.
     */
    public void close() throws IOException;
}
