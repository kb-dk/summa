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
package dk.statsbiblioteket.summa.common.index;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.text.ParseException;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An alias in Summa is a naming convention for naming fields and groups in
 * different languages.<br>
 * This is used for automated generation of multi-lingual aware
 * queryparsers.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hal")
public final class IndexAlias implements Comparable {
    private static Log log = LogFactory.getLog(IndexAlias.class);

    private String name;
    private String lang;

    /**
     * Generates a new Alias with a given name on a given language.<br>
     * @param name the name of the alias (as used by the queryparser)
     * @param lang the language to bind this name to. This can be null,
     *             in which case all langs in {@link#isMatch} matches.
     */
    public IndexAlias(String name, String lang) {
        log.trace("Creating alias(" + name + ", " + lang + ")");
        this.name = name;
        this.lang = lang;
    }

    /**
     * Extracts aliases from the given node and returns them as a list.
     * @param node the node containing aliases.
     * @return a list of the aliases contained in the node.
     * @throws ParseException if the aliases could not be parsed.
     */
    public static Set<IndexAlias> getAliases(Node node) throws ParseException {
        log.trace("getAliases called");
        NodeList children =  node.getChildNodes();
        Set<IndexAlias> aliases = new HashSet<IndexAlias>(10);
        for (int i = 0 ; i < children.getLength(); i++) {
            Node child = children.item(i);
            //noinspection DuplicateStringLiteralInspection
            if (child.getLocalName() != null
                && child.getLocalName().equals("alias")) {
                Node nameNode = child.getAttributes().getNamedItem("name");
                if (nameNode == null || nameNode.getNodeValue().equals("")) {
                    log.trace("Undefined name in alias. Skipping");
                    continue;
                }
                String name = nameNode.getNodeValue();
                Node langNode = child.getAttributes().getNamedItem("lang");
                String lang = langNode == null
                              || langNode.getNodeValue().equals("")
                              ? null : langNode.getNodeValue();
                log.trace("Found alias(" + name + ", " + lang + ")");
                aliases.add(new IndexAlias(name, lang));
            }
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Found " + aliases.size() + " aliases");
        return aliases;
    }


    /**
     * The name used for the associated Field in the queryparser for the
     * Alias's language.
     * @return the name for this alias.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name for the associated Field in the queryparser for the
     * Alias's language.
     * @param name the name to use for the field with the alias language.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the language code for this alias, codes follows the XML:lang
     * language code specification. (lang-contry).
     * @return the ISO language code.
     */
    public String getLang() {
        return lang;
    }

    //todo: check the format for the lang attribute
    /**
     *
     * @param lang
     */
    public void setLang(String lang)  {

        this.lang = lang;
    }

    /**
     * Indicates if Object o is equal to this Alias.<br>
     * Two Aliases are considered equal if both the name and the lang for
     * the alias are constidered equal according to the String equal method.
     * {@link String#equals(Object o)}.
     * </p><p>
     * Two equal Aliases will always produce the same hashCode.
     * @param o
     * @return
     * @see #hashCode()
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final IndexAlias alias = (IndexAlias) o;

        if (lang != null ? !lang.equals(alias.lang) : alias.lang != null) {
            return false;
        }
        return !(name != null ? !name.equals(alias.name) : alias.name != null);

    }

    //reuses javadoc
    public int hashCode() {
        int result;
        result = name != null ? name.hashCode() : 0;
        result = 29 * result + (lang != null ? lang.hashCode() : 0);
        return result;
    }

    //reuse javadoc
    public int compareTo(Object o) {
        //noinspection ObjectEquality
        if (this == o) {
            return 0;
        }
        IndexAlias that = (IndexAlias) o;
        int compare;
        if (name != null) {
            if (that.name == null) {
                return -1;
            }
            compare = name.compareTo(that.name);
            if (compare != 0) {
                return compare;
            }
        } else if (that.name != null) {
            return 1;
        }

        if (lang != null) {
            if (that.lang == null) {
                return -1;
            }
            return lang.compareTo(that.lang);
        } else if (that.lang != null) {
            return 1;
        }

        return 0;

    }

    public boolean isMatch(String name, String lang) {
        return this.name.equals(name)
               && (this.lang == null || lang == null
                   || this.lang.equals(lang));
    }

    public String toXMLFragment() {
        return "<alias name=\"" + name + "\""
               + (lang == null ? "" : " lang=\"" + lang + "\"")
               + "/>\n";
    }
}




