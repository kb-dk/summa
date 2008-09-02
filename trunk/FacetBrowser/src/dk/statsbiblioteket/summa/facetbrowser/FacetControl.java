/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.facetbrowser.browse.Browser;
import dk.statsbiblioteket.summa.facetbrowser.build.Builder;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetCore;

/**
 * Faceting is presentations of Tags grouped together in Facets. A Facet might
 * be "City" and a Tag might be "Copenhagen". The Tags are derived from
 * the result of a search in an index. A search for "H. C. Andersen" might
 * give a result like {@code
 * Author
   - H.C. Andersen (230)
   - Hans Christian Andersen (123)
 * Title
   - Den grimme ælling (12)
   - Kejserens nye klæder (3)
 * City
   - Odense (102)
   - Copenhagen (10)
 }
 * The exact presentation is up to the presentation layer.
 * </p><p>
 * Updating of the Facet structure will normally be part of indexing, in order
 * to keep the structure in sync with a search index such as Lucene. In case
 * of major clean-up or other position-changing events, a full rebuild of
 * the structure can be triggered.
 * </p><p>
 * FacetControl provides both updating and querying of Facets and Tags.
 * The design goal of Facets in Summa is to provide full faceting, using
 * best-effort calculations. This means that all documents from a search
 * will be used to calculate the Facet-Tag presentation, but that the
 * result is not guaranteed to be 100% correct in terms of tag-counts.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface FacetControl extends Browser, Builder {

}
