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

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.io.File;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface FacetCore {
    /**
     * The property key for the facets that should be used in the FacetBrowser.
     * The facets are optionally specified with the maximum number of tags
     * that should be displayed. Example:
     * FacetBrowser.FACETS=Author (5), Title, subject (20)  
     */
    public static final String FACETS = "FacetBrowser.FACETS";

    /**
     * Load previously generated data for facet browsing. The data location is
     * taken from the properties.
     * @throws IOException if the data could not be read from the file system.
     */
    public void load() throws IOException;

    /**
     * Load previously generated data for facet browsing.
     * @param directory the location of the data.
     * @throws IOException if the data could not be read from the file system.
     */
    public void load(File directory) throws IOException;

    /**
     * Checks to see if the internal facet representation is synchronized to
     * the Lucene index and the configuration. The check is not guaranteed to
     * deliver the correct result, only a strong indication.
     * @return true if the internal representation is in sync with the index.
     */
    public boolean isSynchronized();
}
