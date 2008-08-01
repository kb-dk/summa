/* $Id: BuilderImpl.java,v 1.4 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:24 $
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
/**
 * Created: te 2007-08-28 14:02:35
 * CVS:     $Id: BuilderImpl.java,v 1.4 2007/10/05 10:20:24 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.build;

import dk.statsbiblioteket.summa.facetbrowser.browse.Browser;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerFactory;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.io.File;
import java.util.List;

/**
 * Default implementation of
 * {@link Browser}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BuilderImpl implements Builder {
    private Configuration configuration;
    private StructureDescription structure;
    private IndexConnector connector;
    private CoreMap coreMap;
    private TagHandler tagHandler;

    public BuilderImpl(Configuration configuration,
                       StructureDescription structure,
                       IndexConnector connector,
                       CoreMap coreMap,
                       TagHandler tagHandler) {
        this.configuration = configuration;
        this.structure = structure;
        this.connector = connector;
        this.tagHandler = tagHandler;
        this.coreMap = coreMap;
    }

    public void save(String directory) throws IOException {
        // TODO: Implement this
    }

    public void updateRecord(Record record) {
        // TODO: Implement this
    }

    public void build(boolean keepTags) {

        // TODO: Implement this
    }

    public void save(File directory) throws IOException {
        // TODO: Implement this
    }

    public void addDocuments(List<Document> documents) {
        // TODO: Implement this
    }

    public void rebuildMap() {
        // TODO: Implement this
    }
}
