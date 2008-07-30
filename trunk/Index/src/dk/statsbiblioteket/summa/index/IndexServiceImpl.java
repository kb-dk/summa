/* $Id: IndexServiceImpl.java,v 1.6 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.Directory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

import dk.statsbiblioteket.summa.common.lucene.index.*;
import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaRepeatAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaKeywordAnalyzer;
import dk.statsbiblioteket.summa.common.index.IndexAlias;
import dk.statsbiblioteket.summa.index.lucene.DocumentCreator;
import dk.statsbiblioteket.util.qa.QAInfo;


/**
 * Implementation of an {@link IndexService}.
 * The implementation uses the RMI Activatable framework, ensuring a minimum of resource (memory) footprint when not in use,
 * and in the same time ensures the availability of the service. The implementation is therefore pseudo stateless.
 * The pseudo stateless nature of the service means that the implementation dynamically builds it's state according to what is needed according to index the documents in hand.
 * As a consequence the IndexService implementation is not guarantied have full knowledge of the index it is working on.
 * Any information about the index therefore should be fetched from the SearchDescriptor {@see SearchDescriptor}
 * This implementation uses the Apache commons-logging framework for logging and needs log4j, or an alternative logging backend supported by commons-logging,
 * to be configured with a Category for the package name.
 * @deprecated use {@link DocumentCreator} instead.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IndexServiceImpl implements IndexService {


    private static XPathExpression singleFieldXPathExpression;
    private static XPathExpression groupFieldXPathExpression;
    private static XPath groupFields;


    /**
     * the path where the index is stored.
     */
    private String indexPath = null;

    /**
     * builder for building DOM objects of the transformed source documents.
     */
    private DocumentBuilder xmlBuilder;

    /**
     * The namespace URL for the IndexService document format.
     */
    public static final String NAMESPACE = "http://statsbiblioteket.dk/2004/Index";

    /**
     * The namespace URL for default XML.
     */
    //  public static final String XMLNAMESPACE = "http://www.w3.org/XML/1998/namespace";

    /**
     * HashMap of possible analysers. ( loaded from configuration file )
     */
    public HashMap Analyzers;


    private ArrayList<String> fields;

    /**
     * The name of the properties file where language/analyzer mapping is defined. ( The file should be in the class path )
     */
    public static final String PROPERTIES = "index.properties";

    public static int maxIndexManipulationCount = 10000;


    /**
     * The Analyzer used to analyze documents.
     */
    private PerFieldAnalyzerWrapper indexAnalyzer;

    /**
     * Writer to write the index.
     */
    public IndexWriter writer;

    SearchDescriptor descriptor = null;

    private boolean useRam;

    private String xslt = null;
    private Transformer trans = null;

    /**
     * The Log to write log messages to.
     */
    private static final Log log = LogFactory.getLog(IndexServiceImpl.class);

    private static final boolean isTrace = log.isTraceEnabled();
    private static final boolean isDebug = log.isDebugEnabled();
    private static final boolean isInfo = log.isInfoEnabled();

    public IndexServiceImpl() throws IndexServiceException {
        this(false);
    }


    public IndexServiceImpl (boolean useRAM) throws IndexServiceException{

        this.useRam = useRAM;

        final DocumentBuilderFactory dfac = DocumentBuilderFactory.newInstance();

        dfac.setNamespaceAware(true);
        dfac.setValidating(false);

        try {
            xmlBuilder = dfac.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IndexServiceException("Unable to make DocumentBuilder", e);
        }


        fields = new ArrayList<String>();
        indexAnalyzer = new PerFieldAnalyzerWrapper(FieldType.freetext.getAnalyzer());

        NamespaceContext nsCon = new DefaultNamespaceContext();
        ((DefaultNamespaceContext) nsCon).setNameSpace("http://statsbiblioteket.dk/2004/Index", "in");


        XPathFactory xpfac = XPathFactory.newInstance();
        XPath singleFields = xpfac.newXPath();
        XPath group = xpfac.newXPath();
        groupFields = xpfac.newXPath();

        groupFields.setNamespaceContext(nsCon);
        singleFields.setNamespaceContext(nsCon);
        group.setNamespaceContext(nsCon);
        try {
            singleFieldXPathExpression = singleFields.compile("/in:document/in:fields/in:field");
            groupFieldXPathExpression = group.compile("/in:document/in:fields/in:group");

        } catch (XPathExpressionException e) {
            log.error(e);
        }
        if (isInfo){log.info("IndexService activated");}
    }


    /**
     * Gets a proper configured transformer.
     *
     * @param xsltUrl
     * @return a transformer instance, ready to use.
     * @throws IndexServiceException thrown if for some reason a Transformer could not be instantiated ( properly problems with the xsltUrl )
     */
    Transformer getTransformer(final String xsltUrl) throws IndexServiceException {

        if (xsltUrl.equals(xslt)) {
            if (trans != null)
                return trans;
        }

        log.info ("Compiling XSLT: " + xsltUrl);
        final TransformerFactory tfactory = TransformerFactory.newInstance();
        InputStream in = null;
        try {
            URL url = new URL(xsltUrl);
            in = url.openStream();
            trans = tfactory.newTransformer(new StreamSource(in, url.toString()));
        } catch (MalformedURLException e) {
            throw new IndexServiceException("The URL to the XSLT is not a valid URL: " + xsltUrl, e);
        } catch (IOException e) {
            throw new IndexServiceException("Unable to open the XSLT resource, check the destination: " + xsltUrl, e);
        } catch (TransformerException e) {
            throw new IndexServiceException("Unable to instantiate Transformer, a system configuration error?", e);
        } finally {
            try {
                assert in != null;
                in.close();
            } catch (IOException e) {
                // do nothing
            }
        }
        xslt = xsltUrl;
        return trans;
    }

    private Field newField(String name, String val, FieldType t, boolean isRepeat) {
        if (!fields.contains(name)) {
            if (isRepeat && !(t.getAnalyzer() instanceof SummaKeywordAnalyzer)){
                indexAnalyzer.addAnalyzer(name, new SummaRepeatAnalyzer(t.getAnalyzer()));
                if (isInfo){log.info("adding name:" + name + "using: " + t.getAnalyzer().getClass().getName() + " (analyzer wrapped in SummaRepeatAnalyzer)" + val);}
            } else {
                if (isInfo){log.info("adding name:" + name + "using: " + t.getAnalyzer().getClass().getName() + "::" + val);}
                indexAnalyzer.addAnalyzer(name, t.getAnalyzer());
            }
            fields.add(name);
        }
        Field f = new Field(name, val, t.getStore(), t.getIndex(), t.getVector());
        if (isTrace){log.trace("made new lucene field:" + f);}
        return f;
    }


    private Document buildDoc(String xml, Transformer t, String id) throws FaultyDataException, IOException  {
        if (isTrace){log.trace(xml);}
        final StreamResult input = new StreamResult();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        input.setOutputStream(out);
        final Source so = new StreamSource(new StringReader(xml));

        final Document doc;
        try {
            t.transform(so, input);
            if (isDebug){log.debug("transformed document");}
            final byte[] b = out.toByteArray();
            if (isTrace){log.trace("transformed index document: " + new String(b, "utf-8"));}
            doc = xmlBuilder.parse(new ByteArrayInputStream(b));

        } catch (SAXException e) {
            //log.error("Error in parsing the transformation result of the record: " + id, e);
            throw new FaultyDataException("Error in parsing the transformation result of the record: " + id, e);

        } catch (TransformerException e) {
            //log.error("Unable to transform the record:" + id, e);
            throw new FaultyDataException("Unable to transform the record:" + id, e);
        }

        return doc;
    }

    private synchronized void indexXML(String xml, String id, Transformer t) throws IndexServiceException, FaultyDataException, IOException {
        if (isDebug){log.debug("indexing: " + id  +", xml: " + xml);}

        if (descriptor == null) {
            descriptor = new SearchDescriptor(indexPath);
        }

        if (isDebug) {log.debug("got searchdescriptor");}

        long time = System.currentTimeMillis();

        final org.apache.lucene.document.Document docIndex = new org.apache.lucene.document.Document();

        docIndex.add(new Field("recordID", id, Field.Store.YES, Field.Index.NO_NORMS));

        if (isDebug){log.debug("added recorid to record");}

        final Document doc = buildDoc(xml, t, id);

        final NamedNodeMap att = doc.getDocumentElement().getAttributes();
        IndexDefaults defaults = IndexUtils.getDefaults(att, NAMESPACE);

        docIndex.add(new Field("resolver", defaults.getResolver(), Field.Store.YES, Field.Index.NO_NORMS));

        try {
            NodeList singleFields = (NodeList) singleFieldXPathExpression.evaluate(doc, XPathConstants.NODESET);
            makeIndexFields(singleFields, defaults, id, docIndex);
        } catch (XPathExpressionException e) {
            log.error(e);
        }


        try {
            NodeList group = (NodeList) groupFieldXPathExpression.evaluate(doc, XPathConstants.NODESET);
            makeGroups(group, defaults, id , docIndex, doc);
        } catch(XPathExpressionException e){
            log.error(e);
        }
        if (isDebug){log.debug("Added to index: " + id + " indexing in: " + (System.currentTimeMillis() - time) + "ms");}
        
        addToIndex (docIndex);


    }


    private IndexWriter getWriter() throws IOException {
        if (writer == null){
            if (useRam){
                writer = new IndexWriter(new RAMDirectory(), indexAnalyzer, true);
            } else {
                writer = new IndexWriter(indexPath, indexAnalyzer, false);
                writer.setMergeFactor(4);
                writer.setMaxBufferedDocs(1500);
                writer.setMaxFieldLength(Integer.MAX_VALUE);
                if (isDebug) {
                    log.warn("LOGGING ON DEBUG LEVEL TRIGGERS LUCENE LOGGING TO log/lucene.log. " +
                                    "This may cause performance regressions");
                    writer.setInfoStream(new PrintStream (
                                            new BufferedOutputStream(
                                                new FileOutputStream ("log/lucene.log")
                                            )
                                        ));
                }
            }
        }
        return writer;
    }

    private void addToIndex (org.apache.lucene.document.Document luceneDocument)
                                                throws IndexServiceException {
        try {
            long time = System.currentTimeMillis();
            getWriter();
            /*if (writer == null) {
                try {
                    writer = new IndexWriter(indexPath, indexAnalyzer, false);
                    writer.setMergeFactor(4);
                    writer.setMaxBufferedDocs(1500);
                    writer.setMaxFieldLength(Integer.MAX_VALUE);
                } catch (IOException e) {
                    throw new IndexServiceException(e.getMessage());
                }
            }*/
            if (isTrace){log.trace(luceneDocument);}
            writer.addDocument(luceneDocument);
            if (isDebug) {log.debug ("Wrote document to index in: " + (System.currentTimeMillis() - time));}

        } catch (IOException e) {
            log.warn(e);
            throw new IndexServiceException(e.getMessage());
        } catch (RuntimeException e) {
            log.warn(e);
        }


    }

    private void makeGroups (NodeList group,
                             IndexDefaults defaults,
                             String id,
                             org.apache.lucene.document.Document luceneDocument,
                             Document doc)
            throws IndexServiceException {

        int groupCount = group.getLength();

        for (int i = 0; i < groupCount; i++) {
            String gruppeNavn = null;
            String groupName;
            IndexDefaults gdef = new IndexDefaults();
            gdef.setBoost(defaults.getBoost());
            gdef.setFieldType(defaults.getFieldType());
            gdef.setFreeText(defaults.isFreeText());
            gdef.setGroup(defaults.isGroup());
            gdef.setIndex(defaults.isIndex());
            gdef.setSuggest(defaults.isSuggest());
            gdef.setType(defaults.getType());
            gdef.setResolver(defaults.getResolver());

            Node groupNode = group.item(i);
            NamedNodeMap attributes = groupNode.getAttributes();
            Node suggest = attributes.getNamedItemNS(NAMESPACE, "suggest");
            Node navn = attributes.getNamedItemNS(NAMESPACE, "navn");
            Node name = attributes.getNamedItemNS(NAMESPACE, "name");

            if (suggest != null) {
                gdef.setSuggest(Boolean.parseBoolean(suggest.getNodeValue()));
            }
            if (navn != null) {
                gruppeNavn = navn.getNodeValue();
            }

            if (name != null) {
                groupName = name.getNodeValue();
            } else {
                throw new IndexServiceException("Found a group without a name");
            }

            //log.debug("got name for the group:" + groupName);

            if (gruppeNavn != null) {
                //log.debug("generating group with name:" + groupName + " and alias: " + gruppeNavn);
                IndexAlias alias = new IndexAlias(gruppeNavn, "da");
                ArrayList<IndexAlias>  li = new ArrayList<IndexAlias>();
                li.add(alias);
                descriptor.createGroup(groupName, li);
                //log.debug("done generating group");
            } else {
                //log.debug("generating group with name:" + groupName);

                descriptor.createGroup(groupName, new ArrayList<IndexAlias>());
                //log.debug("done generating group");
            }

            try {
                NodeList nodes = (NodeList) groupFields.evaluate("/in:document/in:fields/in:group[@in:name = '" + groupName + "']/in:field", doc, XPathConstants.NODESET);
                makeIndexFields(nodes,gdef, id, luceneDocument,groupName);
            } catch (XPathExpressionException e) {
                log.error(e);
            }


        }

    }


    private void makeIndexFields(NodeList list, IndexDefaults defaults,
                                 String id, org.apache.lucene.document.Document docIndex) throws IndexServiceException {
        makeIndexFields(list, defaults, id, docIndex, null);
    }

    /**
     * Adds each item in <code>list</code> as a field in <code>docIndex</code>.
     */
    private void makeIndexFields(NodeList list, IndexDefaults defaults,
                                 String id, org.apache.lucene.document.Document docIndex, String groupName) throws IndexServiceException {

        int len = list.getLength();
        String value;

        for (int i = 0; i < len; i++) {
            Node field = list.item(i);
            OldIndexField f = IndexUtils.makeField(field, defaults, groupName, NAMESPACE);
            value = IndexUtils.getElementNodeValue(field);

            if (value != null && !"".equals(value.trim())) {

                Field newField = newField(f.getName(), value, f.getType(), f.isRepeat());
                newField.setBoost(f.getBoost());
                docIndex.add(newField);

                if (f.getGroup() != null && !"".equals(f.getGroup())){
                    descriptor.addFieldToGroup(f, f.getGroup());
                } else {
                    descriptor.addUnGroupedField(f);
                }
                if (f.isFreetext()){
                    docIndex.add(newField("freetext", " "+value, FieldType.freetext, true));
                }
            }
        }
    }


    /**
     * Optimizes a index for better search performance.
     *
     * @throws IndexServiceException
     */
    public synchronized void optimizeAll() throws IndexServiceException {
        if (writer != null) {

            try {
                if (isInfo){log.info("optimizing index: " + writer);}
                writer.optimize();
            } catch (IOException e) {
                throw new IndexServiceException("Unable to optimize:" + writer.getInfoStream(), e);
            }
            if(isInfo){log.info("Closing IndexWriter: " + writer);}
            closeWriter();
            writer = null;
            try {
                if (descriptor == null) {
                    descriptor = new SearchDescriptor(indexPath);
                }
                descriptor.writeDescription(indexPath);
            } catch (IOException e) {
                throw new IndexServiceException(e.getMessage());
            } catch (TransformerException e) {
                throw new IndexServiceException(e.getMessage());
            }
        }
    }

    private synchronized void closeWriter() throws IndexServiceException{
         if (writer != null) {
            try {
                if(isInfo){log.info("Closing IndexWriter: " + writer);}
                Directory[] d = null;
                if (useRam){
                   d = new Directory[]{writer.getDirectory()};
                }
                writer.close();
                if (useRam && d != null){
                    IndexWriter w = new IndexWriter(indexPath, indexAnalyzer, false);
                    w.setMergeFactor(4);
                    w.setMaxBufferedDocs(1500);
                    w.setMaxFieldLength(Integer.MAX_VALUE);
                    w.addIndexes(d);
                    w.close();
                }
            } catch (IOException e) {
                throw new IndexServiceException("Unable to close:" + writer.getInfoStream(), e);
            }
        }
    }

    public synchronized void closeAll() throws IndexServiceException {
        closeWriter();
        //noinspection AssignmentToNull
        writer = null;
        try {
            if (descriptor == null) {
                descriptor = new SearchDescriptor(indexPath);
            }
            descriptor.writeDescription(indexPath);
        } catch (IOException e) {
            throw new IndexServiceException(e.getMessage());
        } catch (TransformerException e) {
            throw new IndexServiceException(e.getMessage());
        }
    }

    public synchronized String getLastIndexedID() {
        final IndexReader read;
        try {
            read = IndexReader.open(indexPath);
            org.apache.lucene.document.Document d = read.document(read.maxDoc());
            return d.get("recordID");
        } catch (IOException e) {
            log.error(e);
        }
        return null;
    }

    public void resumeIndex(final String path_to_index) throws RemoteException, IndexServiceException {
        if(isInfo){log.info("Resuming index on " + path_to_index);}
        setIndexPath(path_to_index);
        if (descriptor == null) {
            descriptor = new SearchDescriptor(indexPath);
        }
        descriptor.loadDescription(indexPath);
    }


    public void startIndex(final String path_to_index) throws RemoteException, IndexServiceException {
        if(isInfo){log.info("Starting new index on: " + path_to_index);}
        setIndexPath(path_to_index);
        descriptor = new SearchDescriptor(path_to_index); // Reset SearchDescriptor
        try {
            new IndexWriter(path_to_index, indexAnalyzer, true).close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Gets the path where the index is stored.
     *
     * @return String representation of the local path where
     * @throws RemoteException
     */
    public String getIndexPath() throws RemoteException {
        return indexPath;
    }


    public String getPathToIndex() {
        return indexPath;
    }


    /**
     * Sets the local path where index is stored.
     *
     * @param _indexPath
     * @throws RemoteException
     */
    private void setIndexPath(final String _indexPath) throws RemoteException {
        indexPath = _indexPath;
    }

    /**
     * Get a Map of the loaded Analyzers.
     *
     * @return Map with lang as key and Analyzers {@see org.apache.lucene.analysis.Analyzer}
     * @throws RemoteException
     */
    public HashMap getIndexAnalyzers() throws RemoteException {
        return Analyzers;
    }


    /**
     * Gets the current search descriptor, which is a XML document form where a generic search engine can be build.
     *
     * @return The String contains a full XML document - used for building the search engine
     * @throws RemoteException
     */
    public String getSearchDescriptor() throws RemoteException {
        try {
            if (descriptor == null) {
                descriptor = new SearchDescriptor(indexPath);
            }
            return descriptor.getDescription();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
//        } catch (TransformerException e) {
//            log.error (e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param urlToXLST
     * @throws RemoteException
     */
    public void setXSLT(String urlToXLST) throws RemoteException {
        trans = null;
        xslt = urlToXLST;
    }

    /**
     * @param xml
     * @param id
     * @throws RemoteException
     * @throws IndexServiceException
     */
    public void addXMLRecord(String xml, String id, String urlToXSLT) throws RemoteException, IndexServiceException, IOException {
        if (isDebug){log.debug("xslt:" + urlToXSLT);}
        indexXML(xml, id, getTransformer(urlToXSLT));
        //if (isInfo){log.info("Added new record: " + id);}
    }

    /**
     * @param id
     * @throws RemoteException
     * @throws IndexServiceException
     */
    public void removeRecord(String id) throws RemoteException, IndexServiceException {
        final Term t = new Term("recordID", id);
        try {
            final IndexReader read = IndexReader.open(indexPath);
            int deleted = read.deleteDocuments(t);
            if (isInfo){log.info("removeRecord: " + id + " - " + deleted + " instances removed.");}
            read.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

    }

    /**
     * @param xml
     * @param id
     * @throws RemoteException
     * @throws IndexServiceException
     */
    public void updateXMLRecord(String xml, String id, String urlToXSLT) throws RemoteException, IndexServiceException, IOException {
        if (isInfo){log.info("Updating record: " + id);}
        removeRecord(id);
        addXMLRecord(xml, id, urlToXSLT);

    }

    public static int getMaxIndexManipulationCount() {
        return maxIndexManipulationCount;
    }

    public static void setMaxIndexManipulationCount(int max) {
        maxIndexManipulationCount = max;
    }

    public String getXslt() {
        return xslt;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        log.debug("Finalizing IndexServiceImpl: " + this);
        try {
            if (descriptor == null) {
                if (indexPath == null) {
                    log.error ("Failed to write SearchDescriptor in finalizer. indexPath is null.");
                } else {
                    // Write an empty descriptor (potentially overwriting an old one)
                    descriptor = new SearchDescriptor(indexPath);
                    descriptor.writeDescription(indexPath);
                }
            } else {
                descriptor.writeDescription(indexPath);
            }
        } catch (IOException e) {
            log.error("", e);
        } catch (TransformerException e) {
            log.error("", e);
        }
    }

    public SearchDescriptor getDescriptor() {
        return descriptor;
    }


}