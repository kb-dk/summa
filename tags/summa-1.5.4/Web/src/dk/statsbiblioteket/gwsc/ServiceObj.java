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

import javax.xml.namespace.QName;
import javax.xml.rpc.*;
import javax.xml.rpc.encoding.XMLType;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.security.Security;
import java.util.Hashtable;
import java.util.Vector;

public class ServiceObj {
    private boolean haveParameter = false;
    private boolean haveReturnvalue = false;
    private Hashtable<String, QName> types = null;
    private String servicetype = ""; //(soap|rest)
    private Vector<ParameterObj> parameters = null;
    private ReturnvalueObj returnvalue = null;
    private QName servicename = null;
    private QName operationname = null;
    private String serviceurl = "";
    private String name = "";

    public ServiceObj(String type) {
        this.types = new Hashtable<String, QName>();
        this.types.put("xsd:int",XMLType.XSD_INT);
        this.types.put("xsd:string",XMLType.XSD_STRING);
        this.types.put("xsd:boolean",XMLType.XSD_BOOLEAN);
        this.types.put("xsd:datetime",XMLType.XSD_DATETIME);
        this.types.put("xsd:byte",XMLType.XSD_BYTE);
        this.types.put("xsd:decimal",XMLType.XSD_DECIMAL);
        this.types.put("xsd:double",XMLType.XSD_DOUBLE);
        this.types.put("xsd:float",XMLType.XSD_FLOAT);
        this.types.put("xsd:integer",XMLType.XSD_INTEGER);
        this.types.put("xsd:long",XMLType.XSD_LONG);
        this.types.put("xsd:short",XMLType.XSD_SHORT);
        this.types.put("soapenc:int",XMLType.XSD_INT);
        this.types.put("soapenc:string",XMLType.XSD_STRING);
        this.types.put("soapenc:boolean",XMLType.XSD_BOOLEAN);
        this.types.put("soapenc:datetime",XMLType.XSD_DATETIME);
        this.types.put("soapenc:byte",XMLType.XSD_BYTE);
        this.types.put("soapenc:decimal",XMLType.XSD_DECIMAL);
        this.types.put("soapenc:double",XMLType.XSD_DOUBLE);
        this.types.put("soapenc:float",XMLType.XSD_FLOAT);
        this.types.put("soapenc:integer",XMLType.XSD_INTEGER);
        this.types.put("soapenc:long",XMLType.XSD_LONG);
        this.types.put("soapenc:short",XMLType.XSD_SHORT);
        this.servicetype = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServicetype(String servicetype) {
        this.servicetype = servicetype;
    }

    public void setServicename(QName servicename) {
        this.servicename = servicename;
    }

    public void setOperationname(QName operationname) {
        this.operationname = operationname;
    }

    public void setServiceurl(String serviceurl) {
        this.serviceurl = serviceurl;
    }

    public void setParameters(String name) {
        setParameters(name,"");
    }

    public void setReturnvalue(String name, String type) {
        if (this.servicetype.equals("soap")) {
            this.returnvalue = new ReturnvalueObj(name,types.get(type), ParameterMode.OUT);
        } else {
            this.returnvalue = new ReturnvalueObj(name);
        }
        this.haveReturnvalue = true;
    }

    public void setParameters(String name, String type) {
        if (!this.haveParameter) {
            this.parameters = new Vector<ParameterObj>();
        }
        ParameterObj param = null;
        if (this.servicetype.equals("soap")) {
            param = new ParameterObj(name,types.get(type), ParameterMode.IN);
        } else {
            param = new ParameterObj(name);
        }
        this.parameters.add(param);
        this.haveParameter = true;
    }

    public String getName() {
        return this.name;
    }

    public Vector getParameters() {
        return this.parameters;
    }

    public ReturnvalueObj getReturnvalue() {
        return this.returnvalue;
    }

    public Object execute(Object... arguments) {
        //System.out.println(this.operationname.getLocalPart() + ":Argumenter................" + arguments.length);
        //System.out.println(this.operationname.getLocalPart() + ":Parametre ................" + this.parameters.size());
        Object result = null;
        try {
            MultiCall callobj = null;
            if (this.servicetype.equals("rest")) {
                //System.out.println("REST");
                callobj = new MultiCall(this.servicetype);
                callobj.setTargetEndpointAddress(this.serviceurl);
            } else {
                //System.out.println("SOAP");
                callobj = new MultiCall(this.servicetype,this.servicename);
                callobj.setOperationName(this.operationname);
                callobj.setTargetEndpointAddress(this.serviceurl);
            }
            if (this.haveParameter) {
                for (int i = 0; i < this.parameters.size(); i++) {
                    if (this.parameters.elementAt(i).getQName() != null) {
                        //System.out.println(i + " " + this.parameters.elementAt(i).getName() + " - " + this.parameters.elementAt(i).getQName() +  " - " + this.parameters.elementAt(i).getIn().toString());
                        callobj.addParameter(this.parameters.elementAt(i).getName(),this.parameters.elementAt(i).getQName(),this.parameters.elementAt(i).getIn());
                    } else {
                        //System.out.println(i + "?" + this.parameters.elementAt(i).getName() + " - ?" );
                        callobj.addParameter(this.parameters.elementAt(i).getName());
                    }
                }
            }
            if (this.haveReturnvalue) {
                if (this.returnvalue.getQName() != null) {
                    callobj.setReturnType(this.returnvalue.getQName());
                } else {
                    callobj.setReturnType(this.returnvalue.getQName());
                }
            }
            result = callobj.invoke((Object[])arguments);
        } catch (RemoteException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!" + this.operationname.getLocalPart());
        } catch (ServiceException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!" + this.operationname.getLocalPart());
        }
        return result;
    }


    public void createCallObj(String resturl) {
        createCallObj(resturl,"","","");
    }


    public void createCallObj(String serviceurl,String namespace, String servicename, String operationname) {
        this.serviceurl = serviceurl;
        QName serviceName = new QName(namespace,servicename);
        QName operationName = new QName(namespace, operationname);
        this.servicename = serviceName;
        this.operationname = operationName;
    }

    public class ParameterObj {
        private String name = null;
        private QName qName = null;
        private ParameterMode in = null;

        private ParameterObj(String name, QName qName, ParameterMode in) {
            this.name = name;
            this.qName = qName;
            this.in = in;
        }

        private ParameterObj(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public QName getQName() {
            return qName;
        }

        public ParameterMode getIn() {
            return in;
        }
    }

    public class ReturnvalueObj {
        private String name = null;
        private QName qName = null;
        private ParameterMode out = null;

        private ReturnvalueObj(String name, QName qName, ParameterMode out) {
            this.name = name;
            this.qName = qName;
            this.out = out;
        }

        private ReturnvalueObj(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public QName getQName() {
            return qName;
        }

        public ParameterMode getOut() {
            return out;
        }
    }

    private class MultiCall {
        private Call soapcall = null;
        private String calltype = "";
        private String resturl = "";
        private Vector<String> restparameters = new Vector<String>();

        public MultiCall(String calltype) throws ServiceException {
            this.calltype = calltype;
            init(new QName(""));
        }

        public MultiCall(String calltype, QName servicename) throws ServiceException {
            this.calltype = calltype;
            init(servicename);
        }

        public void init(QName servicename) throws ServiceException {
            if (this.calltype.equals("soap")) {
                //System.out.println("Creating call");
                ServiceFactory factory = ServiceFactory.newInstance();
                Service service = factory.createService(servicename);
                //System.out.println("done Creating call");
                soapcall = service.createCall();
            }
        }

        public void addParameter(String name) {
            if (rest()) {
                this.restparameters.add(name);
            }
        }

        public void addParameter(String name, QName qName, ParameterMode in) {
            if (soap()) {
                this.soapcall.addParameter(name,qName,in);
            }
        }

        public synchronized Object invoke(Object[] objects) throws RemoteException {
            if (soap()) {
                return this.soapcall.invoke(objects);
            } else if (rest()) {
                String query = "";
                int j = 0;
                for (int i=0; i < this.restparameters.size(); i++) {
                    String param = this.restparameters.elementAt(i);
                    query = query + param + "=" + (String)objects[j] + "&";
                    j++;
                }
                query = resturl + "?" + query.substring(0,query.length() - 1);
                try {
                    return postData(query,"");
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            return null;
        }

        public void setOperationName(QName operationName) {
            if (soap()) {
                this.soapcall.setOperationName(operationName);
            }
        }

        public void setTargetEndpointAddress(String serviceurl) {
            if (soap()) {
                this.soapcall.setTargetEndpointAddress(serviceurl);
            }
            if (rest()) {
                resturl = serviceurl;
            }
        }

        public void setReturnType(QName qName) {
            if (soap()) {
                this.soapcall.setReturnType(qName);
            }
        }

        private boolean soap() {
            if (this.calltype.equals("soap")) {
                return true;
            } else {
                return false;
            }
        }

        private boolean rest() {
            if (this.calltype.equals("rest")) {
                return true;
            } else {
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
}




