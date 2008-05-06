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
package dk.statsbiblioteket.summa.common.index;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * IndexFields are used on several levels. From abstract to concrete, their
 * roles are as follows:
 * </p><p>
 * - Type role: The field defines how to handle content.<br />
 * - Named field role: The field has a concrete name.<br />
 * - Instantiated field: The field has content and is ready for indexing.
 * </p><p>
 * The class is generic. In order to use it properly, the A (Analyzer) and
 * F (Filter) must be specified.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexField<A, F> {
    private static Log log = LogFactory.getLog(IndexField.class);

    private String name = null;

    /**
     * If true, the field should be indexed.
     * </p><p>
     * This is used at index-time.
     */
    private boolean doIndex = true;

    /**
     * If true, the field should be stored in the index.
     * </p><p>
     * This is used at index-time.
     */
    private boolean doStore = true;

    /**
     * If true, the field should be compressed.
     * </p><p>
     * This is used at index-time.
     */
    private boolean doCompress = false;

    /**
     * The parent used for creating this index field. This is null if the field
     * was build from scratch.
     * </p><p>
     * This is used at index-time.
     */
    private IndexField parent = null;

    /**
     * If true, multiple value-addition calls to this field will be result in
     * more instances of the field in the index. If false, multiple additions
     * will append to the field.
     * </p><p>
     * This is used at index-time.
     */
    private boolean multiValued = true;
    /**
     * The boost for the index. Boosts defined on all other levels than
     * instantiated field should be used at query-time, making them easy to
     * modify. When an instantiated field is created by building on another
     * field, the boost should thus be reset to 1.0, so as not to boost the
     * field both at index-time and query-time.
     * </p><p>
     * It is still possible to set the boost for instantiated fields, but the
     * boost will thus be multiplied with the query-time boost.
     * </p><p>
     * This is used at index-time and at query-time.
     */
    private float boost = 1.0f;
    /**
     * A ISO 639-1 code specifying which locale that should be used for sorting.
     * Note that sorting with locale is a lot heavier than sorting without.
     * </p><p>
     * This is used at search-time.
     * @see [http://www.loc.gov/standards/iso639-2/php/code_list.php]
     */
    private String sortLocale = null;

    /**
     * The content of this field should be duplicated in the freetext-field
     * upon indexing.
     * </p><p>
     * This is used at index-time.
     */
    private boolean inFreetext = true;

    /**
     * If true, this index-field must be present upon indexing.<br />
     * This only makes sense for non-instantiated index fields.
     * </p><p>
     * This is used at index-time.
     */
    private boolean required = false;

    /**
     * Aliases for this index-field. If alias-expansion is used at query-time,
     * aliases are resolved to field-names in the index.
     * </p><p>
     * This is used at query-time.
     */
    private List<IndexAlias> aliases = new ArrayList<IndexAlias>(10);

    /**
     * The analyzer to use for indexing. The type of the analyzer is defined by
     * the user of this class and will normally be a Lucene-analyzer. This will
     * normally be identical to the queryAnalyzer.
     * </p><p>
     * This is used at index-time.
     */
    private A indexAnalyzer;

    /**
     * The analyzer to use for query expansion. The type of the analyzer is
     * defined by the user of this class and will normally be a Lucene-analyzer.
     * This analyzer will normally be identical to the queryAnalyzer.
     * </p><p>
     * This is used at query-time.
     */
    private A queryAnalyzer;

    /**
     * The filters to use when indexing. The type of filter is defined by the
     * user of this class.
     * </p><p>
     * This is used at index-time.
     */
    private List<F> indexFilters = new ArrayList<F>(10);

    /**
     * The filters to use for query expansion. The type of filter is defined by
     * the user of this class.
     * </p><p>
     * This is used at query-time.
     */
    private List<F> queryFilters = new ArrayList<F>(10);

    public IndexField(IndexField<A, F> parentField) {
        log.debug("Creating field based on " + parentField);
        assignFrom(parentField);
        this.parent = parentField;
    }

    /**
     * Assigns the values from the given field to this field. Used for
     * construction and cloning. Assignment will override all values.
     * Lists are shallow copies, to it is safe to modify the lists themselves
     * after assignment.
     * @param parent the field to get values from.
     */
    private void assignFrom(IndexField<A, F> parent) {
        log.trace("Assigning from " + parent);
        name = parent.getName();
        doIndex = parent.isDoIndex();
        doStore = parent.isDoStore();
        doCompress = parent.isDoCompress();
        this.parent = parent.getParent();
        multiValued = parent.isMultiValued();
        boost = parent.getBoost();
        sortLocale = parent.getSortLocale();
        inFreetext = parent.isInFreetext();
        required = parent.isRequired();
        aliases = new ArrayList<IndexAlias>(parent.getAliases());
        indexAnalyzer = parent.getIndexAnalyzer();
        queryAnalyzer = parent.getQueryAnalyzer();
        indexFilters = new ArrayList<F>(parent.getIndexFilters());
        queryFilters = new ArrayList<F>(parent.getQueryFilters());
    }

    public String toString() {
        return "Field(" + name + ")";
    }

    /* Getters */

    public String getName() {
        return name;
    }

    public boolean isDoIndex() {
        return doIndex;
    }

    public boolean isDoStore() {
        return doStore;
    }

    public boolean isDoCompress() {
        return doCompress;
    }

    public IndexField getParent() {
        return parent;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public float getBoost() {
        return boost;
    }

    public String getSortLocale() {
        return sortLocale;
    }

    public boolean isInFreetext() {
        return inFreetext;
    }

    public boolean isRequired() {
        return required;
    }

    public List<IndexAlias> getAliases() {
        return aliases;
    }

    public A getIndexAnalyzer() {
        return indexAnalyzer;
    }

    public A getQueryAnalyzer() {
        return queryAnalyzer;
    }

    public List<F> getIndexFilters() {
        return indexFilters;
    }

    public List<F> getQueryFilters() {
        return queryFilters;
    }
}
