/* $Id: SearchWS.java,v 1.2 2007/10/04 13:28:21 mv Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
 * $Author: mv $
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
package dk.statsbiblioteket.gwsc;

import dk.statsbiblioteket.util.xml.*;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.security.Security;
import java.util.Enumeration;
import java.util.Hashtable;

public class WebServices {
    private static String services = "services.xml";
    private static WebServices ourInstance = null;
    private Hashtable<String, ServiceObj> servicehash = new Hashtable<String, ServiceObj>();
    private static Logger log = Logger.getLogger(WebServices.class);

    public synchronized static WebServices getInstance() {
        if (ourInstance == null) {
            ourInstance = new WebServices();
        }
        return ourInstance;
    }

    public WebServices() {
        if (servicehash.isEmpty()) {
            createServices();
        }
    }

    private boolean createService(String name) {
        boolean success = false;
        if (!servicehash.containsKey(name.toLowerCase())) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            URL propurl = loader.getResource(services);
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = null;
            try {
                docBuilder = docBuilderFactory.newDocumentBuilder();
                docBuilder.isValidating();
                docBuilder.isNamespaceAware();
                Document doc = docBuilder.parse(new File(propurl.getFile()));
                Node nn = DOM.selectNode(doc,"properties/service[name=\""+ name + "\"]");
                ServiceObj service = null;
                if (nn != null) {
                    service = getServiceObj(nn);
                }
                if (service != null && service.getName() != null && !service.getName().equals("") && !servicehash.containsKey(service.getName().toLowerCase())) {
                    servicehash.put(service.getName().toLowerCase(),service);
                    success = true;
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (SAXException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return success;
    }

    private boolean createServices() {
        //parsing service.xml + wsdls creating serviceobjects
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL propurl = loader.getResource(services);
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
            docBuilder.isValidating();
            docBuilder.isNamespaceAware();
            Document doc = docBuilder.parse(new File(propurl.getFile()));
            NodeList nl = DOM.selectNodeList(doc,"properties/service");
            for (int i = 0; i < nl.getLength(); i++) {
                ServiceObj service = getServiceObj(nl.item(i));
                if (service != null && service.getName() != null && !service.getName().equals("") && !servicehash.containsKey(service.getName().toLowerCase())) {
                    servicehash.put(service.getName().toLowerCase(),service);
                }            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        if (!servicehash.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    private ServiceObj getServiceObj(Node nn) throws Exception {
        ServiceObj service = null;
        String name = "";
        boolean ok = false;
        if (nn.getAttributes().getNamedItem("type").getNodeValue().equals("soap")) {
            String wsdlurl = DOM.selectNode(nn,"wsdl/text()").getNodeValue();
            if (checkUrl(new URL(wsdlurl))) {
                service = new ServiceObj(nn.getAttributes().getNamedItem("type").getNodeValue());
                name = nn.getAttributes().getNamedItem("name").getNodeValue();
                System.out.println(name);
                String servicename = DOM.selectNode(nn,"servicename/text()").getNodeValue();
                String operationname = DOM.selectNode(nn,"operationname/text()").getNodeValue();
                String wsdl = postData(DOM.selectNode(nn,"wsdl/text()").getNodeValue(),"");
                Document wsdldoc = DOM.stringToDOM(wsdl);
                nn = DOM.selectNode(wsdldoc,"//*/address[@location]");
                if (nn != null) {
                    String serviceurl = nn.getAttributes().getNamedItem("location").getNodeValue();
                    nn = DOM.selectNode(wsdldoc,"definitions");
                    if (nn != null) {
                        String namespace = nn.getAttributes().getNamedItem("targetNamespace").getNodeValue();
                        NodeList nl1 = DOM.selectNodeList(wsdldoc,"definitions/service/port");
                        //System.out.println(nl1.item(0).getAttributes().getNamedItem("name").getNodeValue() + " - " + servicename);
                        if (nl1.item(0).getAttributes().getNamedItem("name").getNodeValue().equals(servicename)) {
                            service.setName(name);
                            service.createCallObj(serviceurl,namespace,servicename,operationname);
                            nl1 = DOM.selectNodeList(wsdldoc,"definitions/portType/operation");
                            for (int j=0; j < nl1.getLength(); j++) {
                                if (nl1.item(j).getAttributes().getNamedItem("name").getNodeValue().trim().equals(operationname)) {
                                    Node input = DOM.selectNode(nl1.item(j),"./input");
                                    Node output = DOM.selectNode(nl1.item(j),"./output");
                                    String inputstr = input.getAttributes().getNamedItem("name").getNodeValue();
                                    String outputstr = output.getAttributes().getNamedItem("name").getNodeValue();
                                    NodeList nl2 = DOM.selectNodeList(wsdldoc,"definitions/message[@name='" + inputstr + "']/part");
                                    if (nl2 != null && nl2.getLength() == 1 && nl2.item(0).getAttributes().getNamedItem("name").getNodeValue().equals("parameters")) {
                                        NodeList nl3 = DOM.selectNodeList(wsdldoc,"definitions/types/schema/element[@name='" + nl1.item(j).getAttributes().getNamedItem("name").getNodeValue() + "']" + "/complexType/sequence/element");
                                        for (int k = 0; k < nl3.getLength(); k++) {
                                            service.setParameters(nl3.item(k).getAttributes().getNamedItem("name").getNodeValue(),nl3.item(k).getAttributes().getNamedItem("type").getNodeValue());
                                        }
                                    } else {
                                        for (int k = 0; k < nl2.getLength(); k++) {
                                            service.setParameters(nl2.item(k).getAttributes().getNamedItem("name").getNodeValue(), nl2.item(k).getAttributes().getNamedItem("type").getNodeValue());
                                        }
                                    }
                                    nl2 = DOM.selectNodeList(wsdldoc,"definitions/message[@name='" + outputstr + "']/part");
                                    if (nl2 != null && nl2.getLength() == 1 && nl2.item(0).getAttributes().getNamedItem("name").getNodeValue().equals("parameters")) {
                                        NodeList nl3 = DOM.selectNodeList(wsdldoc,"definitions/types/schema/element[@name='" + outputstr + "']" + "/complexType/sequence/element");
                                        for (int k = 0; k < nl3.getLength(); k++) {
                                            service.setReturnvalue(nl3.item(k).getAttributes().getNamedItem("name").getNodeValue(),nl3.item(k).getAttributes().getNamedItem("type").getNodeValue());
                                        }
                                    } else {
                                        for (int k = 0; k < nl2.getLength(); k++) {
                                            service.setReturnvalue(nl2.item(k).getAttributes().getNamedItem("name").getNodeValue(), nl2.item(k).getAttributes().getNamedItem("type").getNodeValue());
                                        }
                                    }
                                    ok = true;
                                }
                            }
                        }
                    }
                }
            } else{
                //System.out.println("??????????????");
            }
            if (ok) {
                log.info("Soap-Service: " + wsdlurl + " started!");
            } else {
                log.info("Soap-Service: " + wsdlurl + " not available!");
            }
        } else if (nn.getAttributes().getNamedItem("type").getNodeValue().equals("rest")) {
            String url = DOM.selectNode(nn,"url/text()").getNodeValue();
            service = new ServiceObj(nn.getAttributes().getNamedItem("type").getNodeValue());
            name = nn.getAttributes().getNamedItem("name").getNodeValue();
            System.out.println(name);
            service.setName(name);
            service.createCallObj(DOM.selectNode(nn,"url/text()").getNodeValue());
            NodeList nl1 = DOM.selectNodeList(nn,"parameters/parameter");
            for (int j=0; j < nl1.getLength(); j++) {
                service.setParameters(nl1.item(j).getAttributes().getNamedItem("name").getNodeValue());
            }
            ok = true;
            log.info("Rest-Service: " + url + " started!");
        } else {
            System.out.println("Problemer: ??????????????????");
        }
        if (!ok) {
            service = null;
        }
        return service;
    }

    public Object execute(String name, Object... arguments) {
        Object obj = null;
        ServiceObj so = getService(name);
        if (so != null) {
            log.debug("getService " + name + " success");
            obj = so.execute(arguments);
            if (obj == null) {
                log.debug("\tservice returned null");
                for (Object argument : arguments) {
                    log.debug("\t\t" + argument);
                }
            }
        }
        else {
            log.debug("getService " + name + " failed");
        }
        return obj;
    }

    public ServiceObj getService(String name) {
        if (this.servicehash.containsKey(name)) {
            return this.servicehash.get(name);
        } else {
            this.createService(name);
            if (this.servicehash.containsKey(name)) {
                return this.servicehash.get(name);
            } else {
               return null;
            }
        }
    }

    public String[] getAllServices() {
        String[] retstr = new String[this.servicehash.size()];
        Enumeration<String> ee = this.servicehash.keys();
        int i = 0;
        while (ee.hasMoreElements()) {
            String key = ee.nextElement();
            retstr[i] = key;
            i++;
        }
        return retstr;
    }

    private boolean checkUrl(URL url){
        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String postData(String target, String content) throws Exception {
        System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        String response = "";
        URL url = new URL(target);
        URLConnection conn = url.openConnection();
        conn.setReadTimeout(10000);
        conn.setDoInput (true);
        if (!content.equals("")) {
            conn.setDoOutput (true);
            conn.setUseCaches (false);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            DataOutputStream out = new DataOutputStream (conn.getOutputStream ());
            out.writeBytes(content);
            out.flush ();
            out.close ();
        }
        String contentType = conn.getHeaderField("Content-Type");
        String[] dummy = contentType.split("charset=");
        BufferedReader in = null;
        if (dummy != null && dummy.length == 2) {
            in = new BufferedReader (new InputStreamReader(conn.getInputStream (),dummy[1]));
        } else {
            in = new BufferedReader (new InputStreamReader(conn.getInputStream ()));
        }
        String temp;
        while ((temp = in.readLine()) != null){
            response += temp + "\n";
        }
        temp = null;
        in.close ();
        return response;
    }
}




