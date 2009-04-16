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

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Arrays;
import java.io.StringWriter;
import java.text.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;

/**
 * A representation of a group of fields. Groups are used for Query-expansion.
 * </p><p>
 * As groups represent IndexFields, A (Analyzer) and F (Filter) should be
 * specified upon usage, in order to make the group index-specific.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexGroup<F extends IndexField> {
    private static Log log = LogFactory.getLog(IndexGroup.class);

    private final String name; // Immutable to allow for caching of lookups
    private Set<IndexAlias> aliases = new HashSet<IndexAlias>(5);
    private Set<F> fields = new HashSet<F>();

    /**
     * Create a new empty group.
     * @param name the name of the group.
     */
    public IndexGroup(String name) {
        log.trace("Creating group '" + name + "'");
        this.name = name;
    }

    /**
     * Create a new Field based on the given node.
     * @param node          the description of the group.
     * @param fieldProvider where to locate the fields specified in node.
     * @throws ParseException if the group could not be created.
     */
    public IndexGroup(Node node, FieldProvider<F> fieldProvider) throws
                                                                ParseException {
        log.trace("Creating group based on node");
        name = parse(node, fieldProvider);
        log.trace("Created group based on node with name '" + name + "'");
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
     * @return the aliases for this group.
     */
    public Set<IndexAlias> getAliases() {
        return aliases;
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
    public F getField(String name, String lang) {
        for (F field: fields) {
            if (field.isMatch(name, lang)) {
                return field;
            }
        }
        return null;
    }

    /**
     * @return a shallow copy of all Fields in this group.
     */
    public Set<F> getFields() {
        log.trace("getFields called on group '" + name + "'");
        //noinspection unchecked
        return new HashSet<F>(fields);
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
    public void addField(F field) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding Field '" + field + "' to group '" + name + "'");
        fields.add(field);
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
        for (F field: fields) {
            sw.append("<field ref=\"").append(field.getName());
            sw.append("\"/>\n");
        }
        sw.append("</group>\n");
        return sw.toString();
    }

    /**
     * Create a group based on the given Document Node. The Node should conform
     * to the output from {@link #toXMLFragment()}.
     * </p><p>
     * @param node          a representation of a Field.
     * @param fieldProvider if any fields are specified, the fieldProvider is
     *                      queried for the parent.
     * @throws ParseException if the node could not be parsed.
     *                        The index of the exception will normally be -1.
     * @return the name of the newly group.
     */
    private String parse(Node node, FieldProvider<F> fieldProvider) throws
                                                                ParseException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("parse called");
        Node groupNameNode = node.getAttributes().getNamedItem("name");
        if (groupNameNode == null) {
            throw new ParseException("No name specified for group", -1);
        }
        String name = groupNameNode.getNodeValue();
        log.trace("parse: Located group name '" + name + "'");
        aliases = IndexAlias.getAliases(node);

        NodeList children =  node.getChildNodes();
        for (int i = 0 ; i < children.getLength(); i++) {
            Node child = children.item(i);
            //noinspection DuplicateStringLiteralInspection
            if (child.getLocalName() != null
                && child.getLocalName().equals("field")) {
                Node fieldNameNode = child.getAttributes().getNamedItem("ref");
                if (fieldNameNode == null
                    || fieldNameNode.getNodeValue().equals("")) {
                    //noinspection DuplicateStringLiteralInspection
                    log.warn(String.format(
                            "Undefined field name in group '%s'. Skipping",
                            name));
                    continue;
                }
                String fieldRef = fieldNameNode.getNodeValue();
                log.trace("Found field ref '" + fieldRef + " in group '"
                          + name + "'");
                F field = fieldProvider.getField(fieldRef);
                if (field == null) {
                    throw new ParseException(String.format(
                            "The field '%s' in group '%s' did not exist",
                            fieldRef, name), -1);
                }
                addField(field);
            }
        }
        log.debug("Resolved " + fields.size() + " fields for group " + name
                  + ": " + Strings.join(fields, ", "));
        return name;
    }

    /* Fundamental methods */

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof IndexGroup)) {
            return false;
        }
        IndexGroup<F> other;
        try {
            // How do we check for generic types?
            //noinspection unchecked
            other = (IndexGroup<F>)o;
        } catch (ClassCastException e) {
            return false;
        }
        return name.equals(other.getName())
               && aliases.size() == other.getAliases().size()
               && aliases.containsAll(other.getAliases())
               && fields.size() == other.getFields().size()
               && fields.containsAll(other.getFields());
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "Group(name '" + name + "', " + fields.size()
               + " fields) subgroups)";
    }

}



