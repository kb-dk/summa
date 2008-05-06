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
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A representation of IndexFields. Groups are used for Query-expansion and
 * cannot have sub-groups. Sub-groups might be introduced at a later time,
 * but so far there has been no requests for them.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexGroup {
    private static Log log = LogFactory.getLog(IndexDescriptor.class);

    private final String name; // Immutable to allow for caching of lookups
    private Set<IndexAlias> aliases =
            new HashSet<IndexAlias>(5);
    private TreeSet<OldIndexField> fields = new TreeSet<OldIndexField>();

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
        for (IndexAlias alias: aliases) {
            if (alias.isMatch(groupName, lang)) {
                return true;
            }
        }
        return false;
    }

    /* Mutators */

    /**
     * Adds the alias to the list of aliases for this Group. Adding the same
     * alias multiple times results in only one extra stored alias.
     * @param alias the alias to add.
     */
    public void addAlias(IndexAlias alias) {
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
     * @return the first match for the given name and lang or null if no match
     *         was found.
     */
    // TODO: Speed-optimize this
    public OldIndexField getField(String name, String lang) {
        for (OldIndexField field: fields) {
            if (field.isMatch(name, lang)) {
                return field;
            }
        }
        return null;
    }

    /**
     * @return a shallow copy of all Fields in this group.
     */
    public Set<OldIndexField> getFields() {
        log.trace("getFields called on group '" + name + "'");
        //noinspection unchecked
        return (Set<OldIndexField>)fields.clone();
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
    public void addField(OldIndexField field) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding Field '" + field + "' to group '" + name + "'");
        fields.add(field);
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "Group(name '" + name + "', " + fields.size()
               + " fields) subgroups)";
    }

    /**
     * Generate an XML fragment suitable for insertion in an IndexDescriptor
     * XML representation. The fragment references fields by name only.
     * @return an XML fragment representing the group.
     */
    public String toXMLFragment() {
        StringWriter sw = new StringWriter(500);
        sw.append("<group name=\"").append(name).append("\">\n");
        for (IndexAlias alias: aliases) {
            sw.append(alias.toXMLFragment());
        }
        for (OldIndexField field: fields) {
            sw.append("<field name=\"").append(field.getName());
            sw.append("\"/>\n");
        }
        sw.append("</group>\n");
        return sw.toString();
    }
}
