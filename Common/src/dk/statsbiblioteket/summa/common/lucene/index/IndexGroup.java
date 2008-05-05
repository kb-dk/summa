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
package dk.statsbiblioteket.summa.common.lucene.index;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A representation of IndexFields and sub-groups.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexGroup {
    private static Log log = LogFactory.getLog(IndexDescriptor.class);

    private String name;
    private Set<IndexField.Alias> aliases =
            new HashSet<IndexField.Alias>(5);
    private TreeSet<IndexField> fields = new TreeSet<IndexField>();
    private TreeSet<IndexGroup> groups = new TreeSet<IndexGroup>();

    public IndexGroup(String name) {
        log.trace("Creating group '" + name + "'");
        this.name = name;
    }

    /**
     * True if the given groupName matches the name of the group or
     * any of its aliases.
     * @param groupName the name to match.
     * @param lang      the language for alias-searching.
     * @return true if the name matches this group.
     */
    public boolean isMatch(String groupName, String lang) {
        if (name.equals(groupName)) {
            return true;
        }
        for (IndexField.Alias alias: aliases) {
            if (alias.isMatch(groupName, lang)) {
                return true;
            }
        }
        return false;
    }

    /* Mutators */

    public void addGroup(IndexGroup group) {
        log.trace("addGroup " + group + " called on group " + this);
        // TODO: Check for circular references
/*        if (isCircularReference(group)) {
            throw new IllegalArgumentException("The group '" + group
                                               + "' would introduce a"
                                               + " circular reference");
        }*/
        groups.add(group);
    }

    /**
     * Adds the alias to the list of aliases for this Group. Adding the same
     * alias multiple times results in only one extra stored alias.
     * @param alias the alias to add.
     */
    public void addAlias(IndexField.Alias alias) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding Alias '" + alias + "' to group '" + name + "'");
        aliases.add(alias);
    }

    /**
     * Searches the group for a Field that matches the given name and lang.
     * The order of priority is direct match first, the alias-match.
     * @param name      the name to search for.
     * @param lang      the language to use for alias-match. If lang == null,
     *                  lang is ignored when matching aliases.
     * @param recursive if true, a recursive descend into subgroups is used.
     * @return the first match for the given name and lang or null if no match
     *         was found.
     */
    public IndexField getField(String name, String lang, boolean recursive) {
        for (IndexField field: fields) {
            if (field.isMatch(name, lang)) {
                return field;
            }
        }
        if (recursive) {
            for (IndexGroup subGroup: groups) {
                IndexField field = subGroup.getField(name, lang, recursive);
                if (field != null) {
                    return field;
                }
            }
        }
        return null;
    }

    /**
     * Searches the group for a subGroup that matches the given name and lang.
     * The order of priority is direct match first, the alias-match.
     * @param name      the name to search for.
     * @param lang      the language to use for alias-match. If lang == null,
     *                  lang is ignored when matching aliases.
     * @param recursive if true, a recursive descend into subgroups is used.
     * @return the first match for the given name and lang.
     */
    public IndexGroup getGroup(String name, String lang, boolean recursive) {
        IndexGroup group = getGroup(name, lang, recursive, true);
        if (group == null) {
            group = getGroup(name, lang, recursive, true);
        }
        return group;
    }

    /**
     * Searches the group for a subGroup that matches the given name and lang.
     * @param name      the name to search for.
     * @param lang      the language to use for alias-match. If lang == null,
     *                  lang is ignored when matching aliases.
     * @param recursive if true, a recursive descend into subgroups is used.
     * @param directMatch if true, only the name is matched, not aliases.
     * @return the first match for the given name and lang.
     */
    protected IndexGroup getGroup(String name, String lang,
                                  boolean recursive, boolean directMatch) {
        for (IndexGroup group: groups) {
            if (directMatch) {
                if (group.getName().equals(name)) {
                    return group;
                }
            } else {
                if (group.isMatch(name, lang)) {
                    return group;
                }
            }
            if (recursive) {
                IndexGroup subGroup =
                        group.getGroup(name, lang, recursive, directMatch);
                if (subGroup != null) {
                    return subGroup;
                }
            }
        }
        return null;
    }

    /**
     * Performs a recursive descend and returns a set with all contained
     * fields. The Set is a shallow copy.
     * @return all Fields in this group with sub-groups.
     */
    public Set<IndexField> getFields() {
        log.trace("getFields called on group '" + name + "'");
        if (groups.size() == 0) {
            //noinspection unchecked
            return (Set<IndexField>)fields.clone();
        }
        Set<IndexField> fields = new HashSet<IndexField>(20);
        fields.addAll(this.fields);
        for (IndexGroup subgroup: groups) {
            fields.addAll(subgroup.getFields());
        }
        return fields;
    }

    /**
     * Adds the Field to the group. Adding the same Field multiple times
     * results in only one stored reference.
     * </p><p>
     * Important: Adding a Field to a group implies that the Field-object is
     *            also available in the fields-list in the Descriptor.
     *            It is the responsibility of the caller to ensure this
     *            invariant. It is recommended to use accessor-methods from
     *            IndexDescriptor instead of calling the addField-method
     *            on Group directly.
     * @param field the field to add to the group.
     */
    public void addField(IndexField field) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding Field '" + field + "' to group '" + name + "'");
        fields.add(field);
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "Group(name '" + name + "', " + fields.size() + " fields, "
               + groups.size() + " subgroups)";
    }
}
