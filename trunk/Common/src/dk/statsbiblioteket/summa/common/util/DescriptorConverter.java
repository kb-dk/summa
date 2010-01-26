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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.namespace.NamespaceContext;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Converts an old-style SearchDescriptor to proper Summa Index Descriptor XML.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DescriptorConverter {
    private static Log log = LogFactory.getLog(DescriptorConverter.class);

    private boolean dumpResolver = true;
    private boolean dumpAnalyzer = true;
    private boolean dumpHasSuggest = true;
    private boolean resetBoosts = false;

    private Set<String> elements = new HashSet<String>(100);
    private Set<String> aliasClash = new HashSet<String>(100);

    public String convert(File oldDescriptor)
            throws IOException, SAXException, ParserConfigurationException {
        return convert(Files.loadString(oldDescriptor));
    }

    public String convert(String oldDescriptor)
            throws IOException, SAXException, ParserConfigurationException {
        elements.clear();
        aliasClash.clear();
        Document doc = getDomDescription(oldDescriptor);
        StringWriter out = new StringWriter(oldDescriptor.length());
        out.append(ParseUtil.XML_HEADER).append("\n");
        out.append("<IndexDescriptor version=\"1.0\" "
                   + "xmlns=\"http://statsbiblioteket.dk/summa/2008/"
                   + "IndexDescriptor\">\n");
        out.append("<!-- This index descriptor was automatically generated\n"
                   + " and should be verified before use, as some elements\n"
                   + " are not present in the old search descriptor format "
                   + "-->\n");
        processGroups(doc, out);
        processFields(doc, out);
        addExtras(doc, out);
        if (aliasClash.size() != 0) {
            out.append("  <!-- The following group- and field-names clashes\n"
                       + "       with aliases: ");
            for (String name: aliasClash) {
                out.append(name).append(" ");
            }
            out.append("-->\n");
        }
        out.append("</IndexDescriptor>\n");
        return out.toString();
    }

    private synchronized Document getDomDescription(String xml)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);
        fac.setValidating(false);
        DocumentBuilder b = fac.newDocumentBuilder();
        return b.parse(new InputSource(new StringReader(xml)));
    }

    private XPath getXPath() {
        NamespaceContext nsCon = new DefaultNamespaceContext();
        XPathFactory xpfac = XPathFactory.newInstance();
        XPath xPath = xpfac.newXPath();
        xPath.setNamespaceContext(nsCon);
        return xPath;
    }
    private XPath xPath = getXPath();
    private List<Node> getNamedChildren(Node node, String childNames) {
        NodeList childNodes = node.getChildNodes();
        List<Node> children = new ArrayList<Node>(childNodes.getLength());
        for (int i = 0 ; i < childNodes.getLength() ; i++) {
            Node childNode = childNodes.item(i);
            if (childNames.equals(childNode.getLocalName())) {
                children.add(childNode);
            }
        }
        return children;
    }

    private Node getNamedChild(Node node, String childName) {
        List<Node> children = getNamedChildren(node, childName);
        if (children.size() > 1) {
            throw new IllegalStateException(String.format(
                    "Got %d children answering to the name '%s'. Expected 1",
                    children.size(), childName));
        }
        if (children.size() == 0) {
            throw new IllegalStateException(String.format(
                    "Got 0 children answering to the name '%s'. Expected 1",
                    childName));
        }
        return children.get(0);
    }

    private String getAttributeContent(Node node, String attributeName) {
        return node.getAttributes().getNamedItem(attributeName).
                getTextContent();
    }

    private void processGroups(Document doc, StringWriter out) {
        out.append("  <groups>\n");
        for (Node group: getNamedChildren(getNamedChild(getNamedChild(
                doc, "IndexDescriptor"), "groups"), "group")) {
            processGroup(group, out);
        }
        out.append("  </groups>\n");
    }
    private void processGroup(Node group, StringWriter out) {
        String name = getAttributeContent(group, "name");
        out.append(String.format("    <group name=\"%s\">\n", name));
        processAliases(group, out);
        elements.add(name);
        for (Node field: getNamedChildren(getNamedChild(
                group, "fields"), "field")) {
            out.append(String.format(
                    "      <field ref=\"%s\"/>\n",
                    getAttributeContent(field, "name")));
        }
        out.append("    </group>\n");
    }

    private void processAliases(Node node, StringWriter out) {
        for (Node alias: getNamedChildren(node, "alias")) {
            String name = alias.getTextContent();
            out.append(String.format(
                    "      <alias name=\"%s\" lang=\"%s\"/>\n",
                    name, getAttributeContent(alias, "xml:lang")));
            if (elements.contains(name)) {
                aliasClash.add(name);
            }
        }
    }

    private void processFields(Document doc, StringWriter out) {
        out.append("  <fields>\n");
        for (Node group: getNamedChildren(getNamedChild(getNamedChild(
                doc, "IndexDescriptor"), "groups"), "group")) {
            for (Node field : getNamedChildren(getNamedChild(
                            group, "fields"), "field")) {
                processField(field, out);
            }
        }
        for (Node field : getNamedChildren(getNamedChild(getNamedChild(
                doc, "IndexDescriptor"), "singleFields"), "field")) {
            processField(field, out);
        }
        out.append("  </fields>\n");
    }

    private void processField(Node field, StringWriter out) {
        String name = getAttributeContent(field, "name");
        out.append(String.format(
                "    <field name=\"%s\" parent=\"%s\"",
                name,
                getNamedChild(field, "type").getTextContent()));
        if (!resetBoosts) {
            out.append(String.format(" boost=\"%s\"", getNamedChild(
                    field, "boost").getTextContent()));
        }
        if ("true".equals(getAttributeContent(field, "isRepeated"))) {
            out.append(" multiValued=\"true\"");
        }
        if ("true".equals(getAttributeContent(field, "isInFreetext"))) {
            out.append(" inFreeText=\"true\"");
        }
        out.append(">\n");
        processAliases(field, out);
        elements.add(name);
        if (dumpResolver) {
            out.append(String.format(
                    "      <!-- resolver: %s -->\n",
                    getNamedChild(field, "resolver").getTextContent()));
        }
        if (dumpAnalyzer) {
            out.append(String.format(
                    "      <!-- analyzer: %s -->\n",
                    getNamedChild(field, "analyzer").getTextContent()));
        }
        if (dumpHasSuggest &&
            "true".equals(getAttributeContent(field, "hasSuggest"))) {
            out.append("      <!-- hasSuggest: true -->\n");
        }
        out.append("    </field>\n");
    }

    private void addExtras(Document doc, StringWriter out) {
        out.append("  <defaultLanguage>da</defaultLanguage>\n");
        out.append("  <defaultSearchFields>\n");
        out.append("    <field ref=\"freetext\"/>\n");
        out.append("  </defaultSearchFields>\n");
        out.append("  <QueryParser defaultOperator=\"AND\"/>\n");
    }

    /* Mutators */

    public void setDumpResolver(boolean dumpResolver) {
        this.dumpResolver = dumpResolver;
    }

    public void setDumpAnalyzer(boolean dumpAnalyzer) {
        this.dumpAnalyzer = dumpAnalyzer;
    }

    public void setDumpHasSuggest(boolean dumpHasSuggest) {
        this.dumpHasSuggest = dumpHasSuggest;
    }

    public void setResetBoosts(boolean resetBoosts) {
        this.resetBoosts = resetBoosts;
    }
}

