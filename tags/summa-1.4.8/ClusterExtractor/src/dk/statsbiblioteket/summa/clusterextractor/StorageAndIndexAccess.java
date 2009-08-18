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
package dk.statsbiblioteket.summa.clusterextractor;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * The StorageAndIndexAccess gives access to storage and index for clustering.
 * This class is temporary and should be replaced with an access offered by
 * storage and index.
 */
public class StorageAndIndexAccess {
    protected static final Log log =
            LogFactory.getLog(StorageAndIndexAccess.class);
    /** Index reader for the work index. */
    private IndexReader ir;
    private String index_location;

    public StorageAndIndexAccess(String index_location) {
        this.index_location = index_location;
        this.openIndexReader();

    }

    /**
     * Open {@link IndexReader} to index given in constructor.
     * @return true if successfull, false otherwise
     */
    private boolean openIndexReader() {
        try {
            this.ir = IndexReader.open(index_location);
        } catch (IOException e) {
            log.error("StorageAndIndexAccess not able to open " +
                    "IndexReader; index_location = " + index_location +
                    "; the cluster builder does not work without " +
                    "access to an index.", e);
            return false;
        }
        return true;
    }


    public Collection getFieldNames() {
        return ir.getFieldNames(IndexReader.FieldOption.INDEXED_WITH_TERMVECTOR);
    }

    public IndexReader getIndexReader() {
        if (ir==null) {
            this.openIndexReader();
        }
        return ir;
    }
}



