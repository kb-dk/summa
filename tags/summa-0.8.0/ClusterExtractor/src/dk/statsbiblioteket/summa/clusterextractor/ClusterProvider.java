/* $Id: ClusterProvider.java,v 1.1 2007/12/03 08:51:20 bam Exp $
 * $Revision: 1.1 $
 * $Date: 2007/12/03 08:51:20 $
 * $Author: bam $
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
package dk.statsbiblioteket.summa.clusterextractor;

import org.apache.lucene.document.Document;
import dk.statsbiblioteket.summa.common.lucene.index.FieldType;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * ClusterProvider provides clusters for given records.
 *
 * The enrich method takes a Lucene Document parameter. If any appropriate
 * clusters can be found, the document is enriched and returned (if no
 * clusters are found, the document is simply returned without alterations).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public interface ClusterProvider extends Configurable {
    /** The String key to the dendogram path property. */
    public static final String DENDROGRAM_PATH_KEY = "clusterprovider.dendrogrampath";

    /** Cluster field name. */
    public static final String CLUSTER_FIELD_NAME = "cluster";
    /** Cluster field type. */
    public static final FieldType CLUSTER_FIELD_TYPE = FieldType.keyWord;
    /** Cluster field alias; danish. */
    public static final String CLUSTER_FIELD_ALIAS_DANISH = "klynge";

    /**
     * Enrich given document with clusters.
     * If this document can be determined to belong to any clusters,
     * enrich the document with these cluster.
     * @param document the document to be enriched
     * @return the enriched document
     */
    public Document enrich(Document document);

    /**
     * Reload data structure(s) from disc.
     */
    public void reload();

    //TODO: register merger with provider or?
}
