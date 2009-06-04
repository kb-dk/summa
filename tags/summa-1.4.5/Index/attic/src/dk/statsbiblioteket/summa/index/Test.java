/* $Id: Test.java,v 1.4 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.4 $
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

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.Iterator;

import org.apache.lucene.queryParser.ParseException;

import org.xml.sax.SAXException;

/**
 * @deprecated as part of the old workflow.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal")
public class Test {

    private static final String XSLT_SUFFIX = "_xslt_url";
    private static final String IO_SERVICE = "_io_storage";
    private static final String ID_SUFFIX = "_last_id";
    private static final String TIME_SUFFIX = "_time";
    private static final String COMPLETE_SUFFIX = "_complete_full_index";

    private static final String csaRecord = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<MetadataRecord>\n" +
            "<recordID>phonofile-0000000000000004</recordID>\n" +
            "<track>\n" +
            "<title>IT'S ONLY MONEY</title>\n" +
            "<sort_title>IT'S ONLY MONEY</sort_title>\n" +
            "<genre>Pop</genre>\n" +
            "<duration>2:45</duration>\n" +
            "<on_album>phonofile-0000000000000001</on_album>\n" +
            "<releaseDate>2003-03-02</releaseDate>\n" +
            "<track_number_on_cd>3</track_number_on_cd>\n" +
            "<contribution>\n" +
            "<DisplayArtist>\n" +
            "<contributorID>phonofile-v5748bzn1j9cp</contributorID>\n" +
            "<name>Concrete Blonde</name>\n" +
            "<sort_name>concrete blonde</sort_name>\n" +
            "</DisplayArtist>\n" +
            "</contribution>\n" +
            "<bundling_req>0</bundling_req>\n" +
            "<coverLarge format=\"jpg\" height=\"256\" width=\"256\">00/01/0000000000000001_256x256_large.jpg</coverLarge>\n" +
            "<coverMedium format=\"jpg\" height=\"128\" width=\"128\">00/01/0000000000000001_128x128_medium.jpg</coverMedium>\n" +
            "<coverSmall format=\"jpg\" height=\"48\" width=\"48\">00/01/0000000000000001_48x48_small.jpg</coverSmall>\n" +
            "<soundClip bitrate=\"128\" codec=\"WMA9\" samplerate=\"44100\">00/04/0000000000000004_WMA9_192kbps_44kHz_stereo_CBR_sample.wma</soundClip>\n" +
            "<license>7 days---WMA 192</license>\n" +
            "</track>\n" +
            "</MetadataRecord>";

    static Transformer getTransformer(final String xsltUrl) throws IndexServiceException {

        /*if (xsltUrl.equals(xslt)) {
            if (trans != null)
                return trans;
        }*/
        Transformer trans;
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
       // xslt = xsltUrl;
        return trans;
    }



    private  static org.w3c.dom.Document buildDoc(Reader xml, Transformer t, String id) throws IndexServiceException {

        final DocumentBuilderFactory dfac = DocumentBuilderFactory.newInstance();

        dfac.setNamespaceAware(true);
        dfac.setValidating(false);
        DocumentBuilder xmlBuilder;
        try {
            xmlBuilder = dfac.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IndexServiceException("Unable to make DocumentBuilder", e);
        }

        //log.debug(xml);
        final StreamResult input = new StreamResult();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        input.setOutputStream(out);
        final Source so = new StreamSource(xml);

        final org.w3c.dom.Document doc;
        try {
            t.transform(so, input);
            final byte[] b = out.toByteArray();
            System.out.println("transformed index document: " + new String(b, "utf-8"));
            doc = xmlBuilder.parse(new ByteArrayInputStream(b));

        } catch (SAXException e) {
            e.printStackTrace();
            System.out.print(e);
            return null;

        } catch (TransformerException e) {
            e.printStackTrace();
            System.out.print(e);
            return null;

        } catch (IOException e) {
            e.printStackTrace();
            System.out.print(e);
            return null;
        }
        return doc;
    }


    private static String transform(Reader xml, Transformer t) throws UnsupportedEncodingException, TransformerException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final StreamResult input = new StreamResult(out);
        final Source so = new StreamSource(xml);
        t.transform(so,input);
        return new String(out.toByteArray(), "utf-8");

    }


    public static void main(String args[]) throws IndexServiceException, IOException, ParseException, NotBoundException, TransformerException {


 //       Storage io = (Storage) Naming.lookup("//localhost:8500/lucene_storage");

        Storage io1 = (Storage) Naming.lookup("//localhost:8500/lucene_storage_new");

 //       FileWriter w = new FileWriter("/home/hal/netmusik_debug_old_storage.txt");
        FileWriter w2 = new FileWriter("/home/hal/netmusik_debug_new_storage.txt");


        Transformer t = getTransformer("http://hera/cgi-bin/viewcvs.cgi/*checkout*/netmusik2006/Index/config/netmusik_index.xslt?rev=1.1.2.3");
        Transformer t1 = getTransformer("file:///home/hal/ganymedexslt/netmusik_index_anvendt.xslt");

 /*       Iterator<Record> ir = io.getRecordsFromBase("netmusik");
        while (ir.hasNext()){
            Record r = ir.next();
            String content = new String(r.getUTF8Content(), "utf-8");
            String tr = transform(new StringReader(content),t);
            String tr1 = transform(new StringReader(content), t1);
            if (!tr.equals(tr1)){ w.append("XSLT not identical"); }
            w.append("\nCONTENT::::::::\n\n").append(content).append("\n\nCVSXSLT:::::::::::::\n\n").append(tr).append("\n\nanvendt::::::::::::").append(tr1).append("\n\n\n\n");
        }
        w.flush();w.close();
 */     Iterator<Record> ir = new StorageIterator(io1, io1.getRecordsFromBase("netmusik"));
        while (ir.hasNext()){
            Record r = ir.next();
            String content = new String(r.getContent(), "utf-8");
            String tr = transform(new StringReader(content),t);
            String tr1 = transform(new StringReader(content), t1);
            if (!tr.equals(tr1)){ w2.append("XSLT not identical"); }
            w2.append("\nCONTENT::::::::\n\n").append(content).append("\n\nCVSXSLT:::::::::::::\n\n").append(tr).append("\n\nanvendt::::::::::::").append(tr1).append("\n\n\n\n");
        }
        w2.flush();w2.close();
    }
}



