/* $Id: Config.java,v 1.3 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.3 $
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
package dk.statsbiblioteket.summa.dice;

import dk.statsbiblioteket.summa.dice.util.NormalSocketFactory;

import javax.rmi.ssl.SslRMIServerSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.Serializable;
import java.io.IOException;
import java.io.File;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.HashMap;
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 6/09/2006
 * Time: 09:43:49
 * Simple class containing information about a WEC setup.
 * Return values for all getters are validated in the sense that
 * Classes and Strings are guaranteed non-null, and integers (ports) are
 * guaranteed >= 0.
 *
 */
public class Config implements Serializable, Constants {

    private HashMap<String,Object> map;
    private Date startDate;

    public Config() {
        map = new HashMap<String,Object> ();
        startDate = new Date();
    }

    public String getStartDate () {
        return new SimpleDateFormat("yyyyMMdd'-'HHmm").format(new Date());
    }

    public void set (String key, Object value) {
        map.put(key, value);
    }

    public Object get (String property) {
        return map.get (property);
    }

    public String getString (String property) {
        Object val = map.get (property);
        if (val == null) {
            return null;
        }
        return val.toString();
    }

    public int getInt (String property) {
        Object val = map.get (property);
        if (val == null) {
            return NO_SUCH_INT;
        }
        return Integer.parseInt(val.toString());
    }

    public Class getClass (String property) {
        Object val = map.get (property);
        if (val instanceof String) {
            try {
                val = Class.forName((String)val);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return (Class) val;
    }

    public void setEmployerClass (Class employer) {
        map.put(CONF_EMPLOYER_CLASS, employer);
    }

    public void setConsumerClass (Class consumer) {
        map.put (CONF_CONSUMER_CLASS, consumer);
    }

    public void setWorkerClass (Class worker) {
        map.put (CONF_WORKER_CLASS, worker);
    }

    public void setSocketType (String type) {
        map.put (CONF_SOCKET_TYPE, type);
    }

    public void setEmployerHostname (String hostname) {
        map.put (CONF_EMPLOYER_HOSTNAME, hostname);
    }

    public void setConsumerHostname (String hostname) {
        map.put (CONF_CONSUMER_HOSTNAME, hostname);
    }

    public void setEmployerServiceName (String serviceName) {
        map.put (CONF_EMPLOYER_SERVICE, serviceName);
    }

    public void setConsumerServiceName (String serviceName) {
        map.put (CONF_CONSUMER_SERVICE, serviceName);
    }

    public void setEmployerPort (int port) {
        map.put(CONF_EMPLOYER_PORT, port);
    }

    public void setConsumerPort (int port) {
        map.put (CONF_CONSUMER_PORT, port);
    }

    public void setRegistryPort (int port) {
        map.put(CONF_REGISTRY_PORT, port);
    }

    public void setEmployerQueueSize (int size) {
        map.put (CONF_EMPLOYER_QUEUE_SIZE, size);
    }

    public void setConsumerQueueSize (int size) {
        map.put (CONF_CONSUMER_QUEUE_SIZE, size);
    }

    public void setWorkerQueueSize (int size) {
        map.put (CONF_WORKER_QUEUE_SIZE, size);
    }

    public Class getEmployerClass () { return getClass(CONF_EMPLOYER_CLASS); }

    public Class getConsumerClass () { return getClass(CONF_CONSUMER_CLASS); }

    public Class getWorkerClass () { return getClass(CONF_WORKER_CLASS); }

    public String getSocketType () { return getString(CONF_SOCKET_TYPE); }

    public String getEmployerHostname () { return getString(CONF_EMPLOYER_HOSTNAME); }

    public String getConsumerHostname () { return getString(CONF_CONSUMER_HOSTNAME); }

    public String getEmployerServiceName () { return getString(CONF_EMPLOYER_SERVICE); }

    public String getConsumerServiceName () { return getString(CONF_CONSUMER_SERVICE); }

    public int getEmployerPort () { return getInt(CONF_EMPLOYER_PORT); }

    public int getConsumerPort () { return getInt(CONF_CONSUMER_PORT); }

    public int getRegistryPort () { return getInt(CONF_REGISTRY_PORT); }

    public int getEmployerQueueSize () { return getInt(CONF_EMPLOYER_QUEUE_SIZE); }

    public int getConsumerQueueSize () { return getInt(CONF_CONSUMER_QUEUE_SIZE); }

    public int getWorkerQueueSize () { return getInt(CONF_WORKER_QUEUE_SIZE); }

    public void setDefaults () {
        setSocketType("normal");

        // Set hostname to that of the current vm
        String hostname;
        try {
            java.net.InetAddress localMachine =
                    java.net.InetAddress.getLocalHost();
            hostname = localMachine.getHostName();
        } catch (java.net.UnknownHostException uhe) {
            throw new RuntimeException ("Unable to obtain hostname.", uhe);
        }

        setEmployerHostname(hostname);
        setConsumerHostname(hostname);

        setEmployerServiceName("dice_employer");
        setConsumerServiceName("dice_consumer");

        setEmployerPort(20001);
        setConsumerPort(20002);
        setRegistryPort(20000);

        setEmployerQueueSize(100);
        setConsumerQueueSize(200);
        setWorkerQueueSize(1);

        set (CONF_EMPLOYER_DATA_PATH, System.getProperty("user.home") + File.separator + "tmp" + File.separator + "dice_data_" + getStartDate() + File.separator + "employer");
        set (CONF_CONSUMER_DATA_PATH, System.getProperty("user.home") + File.separator + "tmp" + File.separator + "dice_data_" + getStartDate() + File.separator + "employer");
        set (CONF_WORKER_DATA_PATH, System.getProperty("user.home") + File.separator + "tmp" + File.separator + "dice_data_" + getStartDate() + File.separator + "employer");

        set (CONF_EMPLOYER_CACHE_PATH, getString(CONF_EMPLOYER_DATA_PATH) + File.separator + "cache");
        set (CONF_CONSUMER_CACHE_PATH, getString(CONF_EMPLOYER_DATA_PATH) + File.separator + "cache");
        set (CONF_WORKER_CACHE_PATH, getString(CONF_EMPLOYER_DATA_PATH) + File.separator + "cache");
    }

    public RMIClientSocketFactory getClientSocketFactory () {
        // Call socketType getter to ensure we validate it
        String type = getSocketType();
        if (type.equals("gzip")) {
            throw new UnsupportedOperationException ("gzip transport not available yet");
            //return  new GZIPSocketFactory();
        } else if(type.equals("ssl")) {
            return new SslRMIClientSocketFactory ();
        } else if (type.equals ("normal")){
            return new NormalSocketFactory();
        } else {
            throw new RuntimeException("Unknown socketType: " + getSocketType());
        }
    }

    public RMIServerSocketFactory getServerSocketFactory () {
        // Call socketType getter to ensure we validate it
        String type = getSocketType();
        if (type.equals("gzip")) {
            throw new UnsupportedOperationException ("gzip transport not available yet");
            //return  new GZIPSocketFactory();
        } else if(type.equals("ssl")) {
            return new SslRMIServerSocketFactory ();
        } else if (type.equals ("normal")){
            return new NormalSocketFactory();
        } else {
            throw new RuntimeException("Unknown socketType: " + getSocketType());
        }
    }

    public String[] dump () {
        String[] result = new String[map.size()];
        int i = 0;
        for (String key : map.keySet()) {
            result[i] = key + "=" + map.get (key);
            i++;
        }
        return result;
    }

    public String dumpString () {
        String result = "";
        for (String s : dump()) {
            result += s + "\n";
        }
        return result;
    }

    public String toString () {
        return dumpString();
    }

    /**
     * Load default config from the {@link Constants#DEFAULT_RESOURCE}
     * if it is found in the classpath.
     * @throws IOException if there was a problem loading the file
     */
    public void loadDefaults () throws IOException {
        loadFromXML(DEFAULT_RESOURCE);
    }

    /**
     * Load properties from the given file into this config.
     * @param filename name of file in the classpath
     * @throws IOException if there was a problem loading the file
     */
    public void loadFromXML (String filename) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Properties p = new Properties();
        p.loadFromXML(loader.getResourceAsStream(filename));

        for (Object prop : p.keySet()) {
            set (prop.toString(), p.get(prop));
        }

    }
}



