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
package dk.statsbiblioteket.gwsc;

import dk.statsbiblioteket.util.xml.DOM;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
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

    /**
     * Constructor, if services container is empty, services are created.
     */
    public WebServices() {
        if (servicehash.isEmpty()) {
            createServices();
        }
    }

    /**
     * Create a service with the given name.
     * 
     * @param name the name of the service to create.
     * @return true if services are created without error. False otherwise.
     */
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
//                Document doc = docBuilder.parse(new File(propurl.getFile().replace("%23", "#")));
                Document doc =
                        docBuilder.parse(getResourceInputStream(propurl));
                Node nn = DOM.selectNode(doc,"properties/service[name=\""
                                             + name + "\"]");
                ServiceObj service = null;
                if (nn != null) {
                    service = getServiceObj(nn);
                }
                if (service != null && service.getName() != null
                    && !service.getName().equals("")
                    && !servicehash.containsKey(
                        service.getName().toLowerCase())) {
                    servicehash.put(service.getName().toLowerCase(),service);
                    success = true;
                }
            } catch (ParserConfigurationException e) {
                log.warn("Exception while parsing configuration for service: '" + name + "': "  + e.getMessage(), e);
            } catch (IOException e) {
                log.warn("IOException while creating service: '" + name + "': " + e.getMessage(), e);
            } catch (SAXException e) {
                log.warn("SAXException while creating services: '" + name + "': "  + e.getMessage(), e);
            } catch (Exception e) {
                log.warn("Exception while creating service: '" + name + "': "  + e.getMessage(), e);
            }
        }
        return success;
    }

    /**
     * Workaround for a bug in DocumentBuilder.parse where Files with '#' cannot
     * be opened.
     * @param resource the URL to the resource to open,. Must be present at the
     *                 local file system.
     * @return an InputStream for the given property.
     * @throws FileNotFoundException if the property could not be resolved to
     *         a file.
     */
    private InputStream getResourceInputStream(URL resource) throws
                                                         FileNotFoundException {
        try {
            return new FileInputStream(new File(resource.toURI()));
        } catch (URISyntaxException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new RuntimeException(String.format(
                    "Unable to convert the URL '%s' to URI", resource), e);
        }
    }


    /**
     * Create services described in service.xml.
     *
     * @return true if any services is created, false is non is created.
     */
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
            Document doc = docBuilder.parse(getResourceInputStream(propurl));
            NodeList nl = DOM.selectNodeList(doc,"properties/service");
            for (int i = 0; i < nl.getLength(); i++) {
                ServiceObj service = getServiceObj(nl.item(i));
                if (service != null && service.getName() != null && !service.getName().equals("")
                        && !servicehash.containsKey(service.getName().toLowerCase())) {
                    servicehash.put(service.getName().toLowerCase(),service);
                }            }
        } catch (ParserConfigurationException e) {
            log.warn("Exception while parsing configuration: "  + e.getMessage(), e);
        } catch (IOException e) {
            log.warn("IOException while creating service: " + e.getMessage(), e);
        } catch (SAXException e) {
            log.warn("SAXException while creating services: "  + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Exception while parsing configuration: "  + e.getMessage(), e);
        }
        // TODO: return true if the first services (or more) is created.
        return !servicehash.isEmpty();
    }

    /**
     *
     * @param nn DOM 'properties/services' node.
     * @return the service Object defined from input node.
     * @throws Exception
     */
    private ServiceObj getServiceObj(Node nn) throws Exception {
        ServiceObj service = null;
        String name = "";
        boolean ok = false;
        if (nn.getAttributes().getNamedItem("type").getNodeValue().equals("soap")) {
            String wsdlurl = DOM.selectNode(nn,"wsdl/text()").getNodeValue();
            if (checkUrl(new URL(wsdlurl))) {
                service = new ServiceObj(nn.getAttributes().getNamedItem("type").getNodeValue());
                name = nn.getAttributes().getNamedItem("name").getNodeValue();
                log.info("Node '" + nn + "' has Iteme 'name' with value: '" + name + "'");
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
                                    } else if (nl2 != null) {
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
                                    } else if (nl2 != null) {
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
            } else {
                log.warn("CheckULR for URL: '" + wsdlurl + "' returned false.");
            }
            if (ok) {
                log.info("Soap-Service: '" + wsdlurl + "' started!");
            } else {
                log.info("Soap-Service: '" + wsdlurl + "' not available!");
            }
        } else if (nn.getAttributes().getNamedItem("type").getNodeValue().equals("rest")) {
            String url = DOM.selectNode(nn,"url/text()").getNodeValue();
            service = new ServiceObj(nn.getAttributes().getNamedItem("type").getNodeValue());
            name = nn.getAttributes().getNamedItem("name").getNodeValue();
            log.info("Node '" + nn + "' has item 'name' with value '" + name + "'.");
            service.setName(name);
            service.createCallObj(DOM.selectNode(nn,"url/text()").getNodeValue());
            NodeList nl1 = DOM.selectNodeList(nn,"parameters/parameter");
            for (int j=0; j < nl1.getLength(); j++) {
                service.setParameters(nl1.item(j).getAttributes().getNamedItem("name").getNodeValue());
            }
            ok = true;
            log.info("Rest-Service: " + url + " started!");
        } else {
            log.warn("Service Object is not of type soap or REST.");
        }
        if (!ok) {
            service = null;
        }
        return service;
    }


    /**
     *
     * @param name service name.
     * @param arguments arguements.
     * @return null if no service with the given name exists, otherwise return the result of executing the service with
     * the given arguements.
     */
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

    /**
     * Return service object pin-pointed by input name. If services isn't started, it is tried started.
     *
     * @param name of service. 
     * @return the services object pin-pointed by the input name.
     */
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

    /**
     * Check if an URL is alive and answers with HTTP status ok.
     *  
     * @param url URL to check.
     * @return true if HTTP response is okay, false otherwise.
     */
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
            log.warn("HTTP connection failed in some way for URL: '" + url + "'.", e);
            return false;
        }
    }


    /**
     * Posting content via HTTP post to a given target (URL).
     * 
     * @param target URL target.
     * @param content content to post.
     * @return response from URL after posting content.
     * @throws Exception
     */
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
        in.close ();
        return response;
    }
}





