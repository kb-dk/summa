/* $Id: OldIndexField.java,v 1.5 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.5 $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.index.IndexAlias;

/**
 * The IndexField is a stub class wrapping all attributes for a Field in the
 * Summa index.
 * </p><p>
 * Attribute List:<br>
 * name -     The name of the field, this attribute is defined by Lucene.<br>
 * group -    The name of the group this field belongs to.<br>
 * language - The language code for this field, used in multi-lingual
 *            indexes.<br>
 * type -     The type of the field, wrapping Lucene characteristics
 *            {link:FieldType}<br>
 * repeatExt  - this attribute is deprecated, do not use.<br>
 * boost -    the boostFactor for the field.<br>
 * suggest -  hint to indicate that Summa should use the field as base for
 *            suggestions.<br>
 * freetext - Determines if the content part of the field is duplicated to the
 *            freetext index.<br>
 * repeat -   this is deprecated and part of the type.<br>
 * aliases -  a list a language Aliases.<br>
 * resolver - the resolver for the document.<br><br>
 * @deprecated in favor of IndexField. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class OldIndexField implements Comparable {

    private String name;
    private String group;
    private String language;
    private FieldType type;
    private String repeatExt;
    private float boost;
    private boolean suggest;
    private boolean freetext;
    private boolean repeat;
    private ArrayList<IndexAlias> aliases;
    private String resolver;

    private static Log log = LogFactory.getLog(OldIndexField.class);

    /**
     * In old implementations repeating fields was subfixed.
     * Use the SummaRepeatAnalyzer instead.
     * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaRepeatAnalyzer
     * @return the subfix
     * @deprecated
     */
    @Deprecated public String getRepeatExt() {
        return repeatExt;
    }

    /**
     * In old implementations repeating fields was subfixed.
     * Use the SummaRepeatAnalyzer instead.
     * @param repeatExt
     * @deprecated
     */
    @Deprecated public void setRepeatExt(String repeatExt) {
        this.repeatExt = repeatExt;
    }

    //todo: find usage of this and eliminate - should not go into all fields?
    /**
     * returns the resolver of the field.
     *
     * @return a hint of how to resolve this record.
     */
    public String getResolver() {
        return resolver;
    }

    /**
     * Sets the resolver value.
     * @param resolver
     */
    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    /**
     * Generates a new IndexField using the defaults in {@link dk.statsbiblioteket.summa.common.lucene.index.IndexDefaults}.
     *
     * @param def The IndexDefaults used with this field.
     */
    public OldIndexField(IndexDefaults def) {

        this.name = null;
        this.group = null;
        this.language = null;
        this.repeatExt = "";
        this.type = def.getFieldType();
        this.boost = def.getBoost();
        this.suggest = def.isSuggest();
        this.freetext = def.isFreeText();
        this.repeat = def.isGroup();
        this.aliases = new ArrayList<IndexAlias>();
        this.resolver = def.getResolver();

    }


    /**
     * The name of a IndexField is the same as the name of the Field in the lucene index.
     *
     * @return  the fields name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the field.
     *
     * @param name the new name of the field
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * A Field can belong to a group.
     * If this Field belongs to a group, the name of the group will be returned.
     * If this Field does not belong to any group, <code>null</code> will be returned.
     * @return the name of the group.
     */
    public String getGroup() {
        return group;
    }

    /**
     * Setting the group on a Field will make the field searchable to all other fields belonging to the same group.
     *
     * @param group the group that this Field should belong to.
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Gets the type of this field {@link dk.statsbiblioteket.summa.common.lucene.index.FieldType}.
     * @return the type of the field.
     */
    public FieldType getType() {
        return type;
    }

    /**
     * Sets the type of this Field {@link dk.statsbiblioteket.summa.common.lucene.index.FieldType}.
     * @param type
     */
    public void setType(FieldType type) {
        this.type = type;
    }

    /**
     * Gets the boost value for this field.
     * Boost affects how a hit in this field will score.
     * @return  the boost factor on the field.
     */
    public float getBoost() {
        return boost;
    }

    /**
     * Set the boost on this field.
     * @param boost the boost factor value
     */
    public void setBoost(float boost) {
        this.boost = boost;
    }

    /**
     * checks if the suggestion flag is set on this Field.
     *
     * @return true if the suggestion flag is set.
     */
    public boolean isSuggest() {
        return suggest;
    }

    /**
     * Sets the suggestion flag.
     * 
     * @param suggest
     */
    public void setSuggest(boolean suggest) {
        this.suggest = suggest;
    }

    /**
     * checks if this field will be added to the freetext.
     *
     * @return true if the freetext flag is set.
     */
    public boolean isFreetext() {
        return freetext;
    }

    /**
     * Set the freetext status.
     * If true the value of the field will also be added to a special freetext field in the index.
     *
     * @param freetext
     */
    public void setFreetext(boolean freetext) {
        this.freetext = freetext;
    }

    /**
     * The added Alias, provide language specific query syntax on this field as specified
     *  in the alias {@link IndexAlias}.
     *
     * @param a the alias to add
     */
    public void addAlias(IndexAlias a) {
        aliases.add(a);
    }

    /**
     * Gets the list of aliases.
     * Each alias should represent a unique language code.
     *
     * @return a list of {@link IndexAlias}.
     */
    public ArrayList<IndexAlias> getAliases() {
        return aliases;
    }

    /**
     * Returns the default language of this field.
     * Implementors needs to ensure that all fields in an index has the same default language.
     *
     * @return the default language.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the default language.
     * @param language the language code of the default language.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * checks if the field is repeatable.
     * A repeatable field will be indexed with abirtery large gaps between sucessive adds the to document.
     * This makes the text almost independent to each other on sucessive adds.
     * A non-repeatable field will instead concatenate sucessive adds as if all text had been in one String.
     * @return true if the field is repeatable.
     */
    public boolean isRepeat() {
        return repeat;
    }

    /**
     * Sets the repeatable field.
     * @param repeat
     */
    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    /**
     * Returns a fragment of xml in a Stringbuffer used to build a total descriptive xml doc for the index.
     *
     * @return an xml fragment representing this field in a buffer.
     */
    public StringBuffer toXMLFragment() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("<field ");
        buffer.append(" name=\"").append(this.getName()).append("\"");
        buffer.append(" hasSuggest=\"").append(this.isSuggest()).append("\"");
        buffer.append(" isInFreetext=\"").append(this.isFreetext()).append("\"");
        buffer.append(" isRepeated=\"").append(this.isRepeat()).append("\"");
        if (this.language != null) {
            buffer.append(" xml:lang=\"").append(this.getLanguage()).append("\"");
        }
        buffer.append(">");
        buffer.append("<resolver>").append(this.getResolver()).append("</resolver>");

        if (this.isRepeat()) {
            buffer.append("<repeatSuffix>").append(this.getRepeatExt()).append("</repeatSuffix>");
        }

        for (IndexAlias a : aliases) {
            buffer.append("<alias xml:lang=\"").append(a.getLang()).append("\">").append(a.getName()).append("</alias>");
        }
        buffer.append("<analyzer>").append(this.getType().getAnalyzer().getClass().getName()).append("</analyzer>");
        buffer.append("<type>").append(this.getType().getType()).append("</type>");
        buffer.append("<boost>").append(this.getBoost()).append("</boost>");
        buffer.append("</field>");

        return buffer;

    }

    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final OldIndexField that = (OldIndexField) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (repeatExt != null ? !repeatExt.equals(that.repeatExt) : that.repeatExt != null) return false;
        return !(resolver != null ? !resolver.equals(that.resolver) : that.resolver != null);
    }

    /**
     * Compares this object with the specified object for order.<br><br>
     * Returns a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.<br><br>
     *
     * The implementation follows all recommendation from the Comparable interface
     *
     * This implementation will throw a java.lang.ClassCastException if Object o !instanceOf IndexField.
     *
     * @see Comparable
     */
    public int compareTo(Object o) {

        if (this == o) return 0;
        OldIndexField that = (OldIndexField) o;
        int compare;

        if (this.resolver != null) {
            if (that.resolver == null) {
                return -1;
            }
            compare = this.resolver.compareTo(that.resolver);
            if (compare != 0) {
                return compare;
            }
        } else if (that.resolver != null) {
            return 1;
        }

        if (this.name != null) {
            if (that.name == null) {
                return -1;
            }
            compare = this.name.compareTo(that.name);
            if (compare != 0) {
                return compare;
            }
        } else if (that.name != null) {
            return 1;
        }

        if (this.repeatExt != null) {
            if (that.repeatExt == null) {
                return -1;
            }
            return this.repeatExt.compareTo(that.repeatExt);
        } else if (that.repeatExt != null) {
            return 1;
        }

        return 0;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (repeatExt != null ? repeatExt.hashCode() : 0);
        result = 29 * result + (resolver != null ? resolver.hashCode() : 0);
        return result;
    }

    public boolean isMatch(String name, String lang) {
        if (this.name.equals(name) && (language == null || lang == null
                                       || language.equals(lang))) {
            return true;
        }
        for (IndexAlias alias: aliases) {
            if (alias.isMatch(name, lang)) {
                return true;
            }
        }
        return false;
    }
}



