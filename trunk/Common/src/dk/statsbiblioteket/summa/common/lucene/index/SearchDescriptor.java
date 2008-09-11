/* $Id: SearchDescriptor.java,v 1.7 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.7 $
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


import java.util.*;
import java.io.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.XMLConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.namespace.NamespaceContext;

import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.index.IndexAlias;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The SearchDescriptor keeps track of any field defined in the index.
 * Information about name, analyzers, language, type etc. are kept here.
 * This information is needed when instantiating the search engine.
 * @deprecated use {@link IndexDescriptor} instead.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class SearchDescriptor {
    /**
     * The folder containing the file searchDesc.xml. This file is needed
     * for the query parser and analyzer.
     */
    public static final String SEARCHDESCRIPTORLOCATION =
            "facetbrowser.searchdescriptorlocation";

    /**
     * List of all fields.
     */
     TreeSet<OldIndexField> singleFields = new TreeSet<OldIndexField>();

                                 
     HashMap<String, Group> groups = new HashMap<String, Group>();

    /**
     * The DOM document describing the index.
     */
  //  private static Document description = null;

    /**
     * Self reference.
     */
    private SearchDescriptor _instance;

    /**
     * The name of the file that the description is written to.
     */
    private static final String SEARCH_DESCRIPTOR_FILE_NAME = "searchDesc.xml";


    private String indexPath = null;

    /**
     *
     */
 //  private static HashMap<String, String[]> SearchMap;

    /**
     * Category used for logging.
     */
    private Log log = LogFactory.getLog(this.getClass().getPackage().getName());

    /**
     * A Group is a collection of fields that should be searchable as one field.
     */
    public static class Group {

        private String name;
        private Set<IndexAlias> aliases;
        private TreeSet<OldIndexField> fields;

        /**
         * The name of the group, needs to be the name for the group in the default query language.
         * @param name
         */
        public Group(String name) {
            this.name = name;
            this.aliases = new HashSet<IndexAlias>();
            this.fields = new TreeSet<OldIndexField>();
        }

        /**
         * Adds an alias fro this group, making the group seachable in the query language defined in the alias.
         * @param alias
         */
        public void addAlias(IndexAlias alias){
            this.aliases.add(alias);
        }

        /**
         * Adds a Field to the group.
         * @param field
         */
        public void addField(OldIndexField field){
            this.fields.add(field);
        }

        /**
         * Get the name of the group.
         * @return the name as the group is known under in the default language.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets all aliases on this group.
         * @return
         */
        public Collection<IndexAlias> getAliases() {
            return aliases;
        }

        /**
         * Get all fields in the index by now in an orderd list {@link java.util.TreeSet}
         * @return
         */
        public TreeSet<OldIndexField> getFields() {
            return fields;
        }

        /**
         * Adds a collection of aliases.
         * @param aliases
         */
        public void addAliases(Collection<IndexAlias> aliases) {
            this.aliases.addAll(aliases);
        }

        /**
         * Set the fields of the group.
         * warning: all previous added field will be removed.
         * @param fields
         */
        public void setFields(TreeSet<OldIndexField> fields) {
            this.fields = fields;
        }


        /**
         * Get a xml fragment descriping the group in a StringBuffer.
         * @return
         */
        public StringBuffer toXMLFragment(){
            StringBuffer buffer = new StringBuffer();
            buffer.append("<group name=\"").append(this.getName()).append("\">");

            for (IndexAlias a : this.aliases){
                buffer.append("<alias xml:lang=\"").append(a.getLang()).append("\">").append(a.getName()).append("</alias>");
            }
            buffer.append("<fields>");
            for (OldIndexField f : this.fields){
                buffer.append(f.toXMLFragment());
            }
            buffer.append("</fields>");
            buffer.append("</group>");
            return buffer;
        }

    }

    /**
     * Creates a new SearchDescriptor, the xml description will be saved to the
     * indexPath.
     * Note: This does not attempt to load the SearchDescriptor XML.
     *       Use {@link #loadDescription(String)} for loading.
     * @param indexPath the location of the SearchDescriptor XML.
     */
    public SearchDescriptor(String indexPath) {
        log.info("Generating the SearchDescriptor object");
        this.indexPath = indexPath;
    }

    /**
     * Creates a new SearchDescriptor, where the index path are taken from
     * the configuration.
     * This loads the description automatically.
     * @param configuration contains the location of the XML-document
     *                      with the content for the SearchDescriptor.
     */
    public SearchDescriptor(Configuration configuration) {
        indexPath = configuration.getString(SEARCHDESCRIPTORLOCATION);
        loadDescription(indexPath);
    }


    /**
     * This will add a IndexField to the group.
     *
     * The Summa search module uses the Searchdescriptor to read feild:group
     * relations. A query on a group is automatomatically expanded to query
     * all field by the
     * {@link dk.statsbiblioteket.summa.common.search.SummaQueryParser}.
     *
     * @param field the name of the field.
     * @param group the name of the group.
     */
    public synchronized void addFieldToGroup(OldIndexField field , String group){
        groups.get(group).addField(field);
    }


    /**
     * Add a stand alone field, with no reference to any group.
     * @param field the name of the field.
     */
    public synchronized void addUnGroupedField(OldIndexField field){
        singleFields.add(field);
    }

    /**
     * Create a group with a set of aliases.
     * @param groupName the name of the group.
     * @param aliases  the list of group aliases.
     */
    public synchronized void createGroup(final String groupName, ArrayList<IndexAlias> aliases){
        if (!groups.containsKey(groupName)){
            Group g = new Group(groupName);

            g.addAliases(aliases);
            groups.put(groupName,g);
        } else {
            Group group = groups.get(groupName);
            group.addAliases(aliases);
        }
    }



    /**
     * Gets a DOM document representation of the current description.
     *
     * @return DOM based description.
     */
    private synchronized Document getDomDescription() {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);
        fac.setValidating(false);
        try {
            DocumentBuilder b = fac.newDocumentBuilder();
            return b.parse(new InputSource(new StringReader(this.toXML().toString())));
        } catch (SAXException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        } catch (ParserConfigurationException e) {
            log.error(e);
        }
        return null;
    }

    /**
     * Writes the current description to a XML file.
     *
     * @param path , the path where the XML description should be written to.
     *
     * @throws java.io.IOException          - properly unable to write to the path
     * @throws javax.xml.transform.TransformerException -
     */
    public synchronized void writeDescription(final String path) throws IOException, TransformerException {

        try {
            log.info("Writing search description to file: " + path + "/" + SEARCH_DESCRIPTOR_FILE_NAME);
            identityTransform(new FileWriter(new File(path + "/" + SEARCH_DESCRIPTOR_FILE_NAME)));
        } catch (IOException e) {
            log.error("Could not write description file: " + e.getClass());
            throw e;
        }
    }

    /**
     * Get the description as a XML documents in a String.
     *
     * @return the current description.
     *
     * @throws java.io.IOException          -
     * @throws javax.xml.transform.TransformerException - // TODO : It doesn't make any sense to throw this
     */
    public synchronized String getDescription() throws IOException {
        final StringWriter w = new StringWriter();
        identityTransform(w);
        return w.toString();
    }


    private OldIndexField generateField(Node field){
          OldIndexField f = new OldIndexField(new IndexDefaults());

                NamedNodeMap att = field.getAttributes();
                Node n;
                f.setName((n =att.getNamedItem("name")) != null ? n.getNodeValue() : null);
                f.setSuggest((n = att.getNamedItem("hasSuggest")) != null && Boolean.parseBoolean(n.getNodeValue()));
                f.setFreetext((n = att.getNamedItem("isInFreetext")) != null && Boolean.parseBoolean(n.getNodeValue()));
                f.setRepeat((n = att.getNamedItem("isRepeated")) != null && Boolean.parseBoolean(n.getNodeValue()));
                f.setLanguage((n=   att.getNamedItemNS(XMLConstants.XML_NS_URI, "lang")) != null ? n.getNodeValue() : null);

                NodeList childs = field.getChildNodes();

                int children = childs.getLength();
                for (int j = 0 ; j<children ; j++){
                    Node c = childs.item(j);
                    //we have no use of the analyzer here, use type instead
                    if (c.getNodeType() == Node.ELEMENT_NODE){
                        String childname = c.getLocalName();
                        if (childname.equals("resolver")){
                          f.setResolver(IndexUtils.getElementNodeValue(c));
                        } else if (childname.equals("repeatSuffix")){
                          f.setRepeatExt(IndexUtils.getElementNodeValue(c));
                        } else if (childname.equals("alias")){
                            NamedNodeMap childAtt = c.getAttributes();
                            f.addAlias(new IndexAlias(IndexUtils.getElementNodeValue(c), (n=childAtt.getNamedItemNS(XMLConstants.XML_NS_URI, "lang")) != null ? n.getNodeValue() : null));
                        } else if (childname.equals("type")){
                            f.setType(FieldType.getType(IndexUtils.getElementNodeValue(c)));
                        } else if (childname.equals("boost")){
                            try{
                            f.setBoost(Float.parseFloat(IndexUtils.getElementNodeValue(c)));
                            } catch (NumberFormatException e){
                                log.warn("Meaningless boostfactor");
                            }
                        }
                    }
                }
              return f;
    }


    /**
     * Loads an exsisting searchDescriptior from file.
     *
     * @param path  the path where the index and searchdescriptor is stored.
     */
    public synchronized void loadDescription(final String path) {
        final File file = new File(path, SEARCH_DESCRIPTOR_FILE_NAME);

        NamespaceContext nsCon = new DefaultNamespaceContext();

        XPathFactory xpfac = XPathFactory.newInstance();
        XPath singleField = xpfac.newXPath();
        XPath group = xpfac.newXPath();
        singleField.setNamespaceContext(nsCon);
        group.setNamespaceContext(nsCon);


        try {
            NodeList singleNodes = (NodeList) singleField.evaluate("/IndexDescriptor/singleFields/field",new InputSource(new FileInputStream(file)), XPathConstants.NODESET);
            int len = singleNodes.getLength();
            for (int i = 0; i< len ; i++){
               OldIndexField f = generateField(singleNodes.item(i));
               singleFields.add(f);
            }

            NodeList _groups = (NodeList)group.evaluate("/IndexDescriptor/groups/group", new InputSource(new FileInputStream(file)), XPathConstants.NODESET);
            int lenG = _groups.getLength();
            for (int j=0; j<lenG; j++){
                Node grp = _groups.item(j);
                Node n;
                NamedNodeMap att = grp.getAttributes();
                String name = (n= att.getNamedItem("name")) != null ? n.getNodeValue() : null;
                Group g = new Group(name);
                NodeList ali = (NodeList)singleField.evaluate("alias", grp, XPathConstants.NODESET);
                int aliLen = ali.getLength();
                for (int k=0; k<aliLen; k++ ){
                    Node alias = ali.item(k);
                    NamedNodeMap attAli = alias.getAttributes();
                    g.addAlias(new IndexAlias(IndexUtils.getElementNodeValue(alias), (n = attAli.getNamedItemNS(XMLConstants.XML_NS_URI, "lang")) != null ? n.getNodeValue(): null));
                }
                groups.put(name,g);
                NodeList fields = (NodeList) group.evaluate("fields/field", grp, XPathConstants.NODESET);
                int fieldLen = fields.getLength();
                for (int l = 0;l < fieldLen; l++){
                    addFieldToGroup(generateField(fields.item(l)),name);
                }


            }
            log.info("loaded description from: " + file.getPath());
        } catch (XPathExpressionException e) {
            log.error(e);
        } catch (FileNotFoundException e) {
            log.error(e);
        }
    }

    /**
     * Apply identity XSLT on the search descriptor. This is mainly used for
     * producing nicely indented output xml. 
     *
     * @param w fills this writer with the description.
     *
     * @throws java.io.IOException if there is an error flushing or closing the writer
     */
    private synchronized void identityTransform(final Writer w) throws IOException {
        try {
            final Source so = new DOMSource(getDomDescription());
            final Result re = new StreamResult(w);
            final TransformerFactory fac = TransformerFactory.newInstance();
            final Transformer t = fac.newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            t.setOutputProperty(OutputKeys.VERSION, "1.0");

            t.transform(so, re);

        } catch (TransformerException e) {
            log.error ("Failed to apply identity transformation to search descriptor");
        } finally {
            w.flush();
            w.close();
        }
    }

    private StringBuffer toXML(){
        StringBuffer buffer = new StringBuffer();
        buffer.append("<IndexDescriptor indexPath=\"").append(this.indexPath).append("\"><singleFields>");

        for (OldIndexField f : singleFields){
            buffer.append(f.toXMLFragment());
        }

        buffer.append("</singleFields><groups>");


        for (Group g: groups.values()){
           buffer.append(g.toXMLFragment());
        }

        buffer.append("</groups></IndexDescriptor>");
        return buffer;
    }

    /**
     * Get the list of fields with no group relations defined.
     * @return  an ordered list of fields.
     */
    public TreeSet<OldIndexField> getSingleFields() {
        return singleFields;
    }

    /**
     * 
     * @return
     */
    public HashMap<String, Group> getGroups() {
        return groups;
    }


    public void setSingleFields(TreeSet<OldIndexField> singleFields) {
        this.singleFields = singleFields;
    }

    public void setGroups(HashMap<String, Group> groups) {
        this.groups = groups;
    }
}



