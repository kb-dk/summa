/* $Id: IndexDefaults.java,v 1.2 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:19 $
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
package dk.statsbiblioteket.summa.common.lucene.index;

import dk.statsbiblioteket.util.qa.QAInfo;


/**
 * The IndexDefaults is a simple helper class defining a set of index charateristics used default for indexing i Summa.<br><br>
 * The default values are:<br>
 * <code>suggest=false</code> - this field is not base for suggestions.<br>
 * <code>index=true</code>  - the field is indexed.<br>
 * <code>freeText=true</code>  - the text is by default also appended to a freetext field.<br>
 * <code>group = false</code> - no automated grouping defined.<br>
 * <code>boost = 1.0F</code>   - default boost.<br>
 * <code>fieldType = FieldType.text</code> - indexed, tokenized, FreeTextAnalyzer, termVectors with offset and position.<br>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IndexDefaults {


    private String type;
    private String resolver;

    private boolean suggest;
    private boolean index;
    private boolean freeText;
    private float boost;
    private boolean group;
    private FieldType fieldType;

    public IndexDefaults(){
        suggest = false;
        index = true;
        freeText = true;
        group = false;
        boost = 1.0F;
        fieldType = FieldType.text;
        type = "all";
        resolver = "";
    }

    /**
     * This method is replaced by the getFieldType method.
     * @return - a String not used anymore
     * @deprecated
     */
    @Deprecated public String getType() {
        return type;
    }


    /**
     * This method is replaced by the setFieldType method.
     *
     * @see dk.statsbiblioteket.summa.common.lucene.index.IndexDefaults#setFieldType(FieldType)
     * @param type
     * @deprecated
     */
    @Deprecated public void setType(String type) {
        this.type = type;
    }

    /**
     * Suggestions is a Summa meta index functionality.
     * @return  - true if base for suggestion
     */
    public boolean isSuggest() {
        return suggest;
    }

    /**
     * Set the suggestion state for the field
     * @param suggest
     */
    public void setSuggest(boolean suggest) {
        this.suggest = suggest;
    }

    /**
     * If index is true some data will go into the Summa index,
     * the rules for indexing is desided based on the FieldType.
     * @return - true if index
     */
    public boolean isIndex() {
        return index;
    }

    /**
     * Set the index status.
     * @param index
     */
    public void setIndex(boolean index) {
        this.index = index;
    }

    /**
     * If true the data in the field will also be appended to to Summa mandatory field 'freeText'.
     * @return the state of the freetext attribute.
     */
    public boolean isFreeText() {
        return freeText;
    }

    /**
     * If set to true, data will be appended to the freetext field.
     * @param freeText
     */
    public void setFreeText(boolean freeText) {
        this.freeText = freeText;
    }

    /**
     * The boost factor affects the ranking calculation. If boost is increased data is considered more important
     * when calculating the rank.
     * @return  the numeric value for the current boostFactor.
     */
    public float getBoost() {
        return boost;
    }

    /**
     * Sets the boostFactor used when indexing the field
     * @param boost normal boost is 1.0F higher boosts means higher rank in searches.
     */
    public void setBoost(float boost) {
        this.boost = boost;
    }

    /**
     * The FieldType is a collection of index chararteristics.
     * @see dk.statsbiblioteket.summa.common.lucene.index.FieldType
     * @return the current FieldType
     */
    public FieldType getFieldType() {
        return fieldType;
    }

    /**
     * Sets a new FieldType to be used for indexing.
     * @param fieldType use this FieldType for indexing.
     */
    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    /**
     * If true, the meta-layer grouping is on.
     * @return group status
     */
    public boolean isGroup() {
        return group;
    }

    /**
     * if autogroup should be on set this to true.
     * @param group
     */
    public void setGroup(boolean group) {
        this.group = group;
    }

    /**
     * Gets the resolver for the document.
     * @return the name of the resolver
     */
    public String getResolver() {
        return resolver;
    }

    /**
     * Sets the resolver.
     * @param resolver
     */
    public void setResolver(String resolver) {
        this.resolver = resolver;
    }
}
