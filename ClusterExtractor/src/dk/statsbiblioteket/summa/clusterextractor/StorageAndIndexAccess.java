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




