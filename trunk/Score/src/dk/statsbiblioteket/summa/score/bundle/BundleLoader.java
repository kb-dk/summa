/* $Id: BundleLoader.java,v 1.10 2007/10/29 14:38:15 mke Exp $
 * $Revision: 1.10 $
 * $Date: 2007/10/29 14:38:15 $
 * $Author: mke $
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
package dk.statsbiblioteket.summa.score.bundle;

import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.summa.score.api.ServicePackageException;
import dk.statsbiblioteket.summa.score.client.Client;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;

/**
 * The {@code BundleLoader} class is used to create a {@link BundleStub} object
 * from a given unpacked {@code .bundle} file.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class BundleLoader implements Configurable {

    /** The file name used to identify a service */
    public static final String SERVICE_BUNDLE_SPEC = "service.xml";

    /** The file name used to identify a client */
    public static final String CLIENT_BUNDLE_SPEC = "client.xml";

    private DocumentBuilder xmlParser;

    private Log log;

    private class MetaBundle {
        private File bundleDir, bundleSpec, mainJar;
        private String mainClass, instanceId, bundleId;
        private List<String> jvmArgs;

        public MetaBundle(File bundleDir, File bundleSpec, File mainJar,
                          String mainClass, String instanceId, String bundleId,
                          List<String> jvmArgs) {
            this.bundleDir = bundleDir;
            this.bundleSpec = bundleSpec;
            this.mainJar = mainJar;
            this.mainClass = mainClass;
            this.instanceId = instanceId;
            this.bundleId = bundleId;
            this.jvmArgs = jvmArgs;
        }

        public File getBundleDir () { return bundleDir; }
        public File getBundleSpec () { return bundleSpec; }
        public File getMainJar () { return mainJar; }
        public String getMainClass () { return mainClass; }
        public String getInstanceId () { return instanceId; }
        public String getBundleId () { return bundleId; }
        public List<String> getJVMArgs () { return jvmArgs; }
    }

    public BundleLoader (Configuration conf) {
        log = LogFactory.getLog(BundleLoader.class);
        try {
            xmlParser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch(ParserConfigurationException e){
            throw new BundleLoadingException("Error creating DocumentBuilder", e);
        }
        log.debug ("Created " + this.getClass().getName());
    }

    /**
     * Parse and prepare a bundle for loading via a {@link BundleStub}
     * @param bundleDir Directory containing an unpacked {@code .bundle} file
     * @return a stub from which the new JVM can be launched
     * @throws ServicePackageException if the package is not a directory
     * @throws FileNotFoundException if the corresponding package is not deployed
     */
    public BundleStub load (File bundleDir) throws IOException {
        MetaBundle meta = checkBundle(bundleDir);
        List<String> libs = new ArrayList<String>();
        File libDir = new File (bundleDir, "lib");

        if (libDir.exists()) {
            for (String lib : libDir.list()) {
                if (lib.endsWith(".jar")) {
                    libs.add("lib" + File.separator + lib);
                }
            }
        }

        return new BundleStub(meta.getBundleDir(),
                              meta.getBundleId(),
                              meta.getInstanceId(),
                              meta.getMainJar(),
                              meta.getMainClass(),
                              libs,
                              meta.getJVMArgs());

    }

    private MetaBundle checkBundle (File bundleDir) {
        if (!bundleDir.isDirectory()) {
            throw new BundleLoadingException(bundleDir + " is not a directory");
        }

        File bundleSpec = new File (bundleDir, SERVICE_BUNDLE_SPEC);

        if (!bundleSpec.exists()) {
            bundleSpec = new File (bundleDir, CLIENT_BUNDLE_SPEC);
        } else {
            if (new File(bundleDir, CLIENT_BUNDLE_SPEC).exists()) {
                throw new BundleLoadingException("Bundle in " + bundleDir
                                           + " contains both "
                                           + SERVICE_BUNDLE_SPEC + " and "
                                           + CLIENT_BUNDLE_SPEC);
            }
        }

        if (!bundleSpec.exists()) {
            throw new BundleLoadingException("Bundle in " + bundleDir
                                           + " contains no "
                                           + SERVICE_BUNDLE_SPEC + " or "
                                           + CLIENT_BUNDLE_SPEC);
        }

        return checkBundleSpec(bundleDir, bundleSpec);


    }

    private MetaBundle checkBundleSpec (File bundleDir, File bundleSpec) {
        Document doc;
        Element docElement;
        NodeList children;
        File mainJar = null;
        String mainClass = null;
        String bundleId = null;
        String instanceId = null;
        String description = null;
        List<String> jvmArgs = new ArrayList<String>();
        boolean checkedFileList = false;

        try {
            doc = xmlParser.parse(bundleSpec);
        } catch (Exception e) {
            throw new BundleLoadingException("Error parsing bundle spec "
                                             + bundleSpec, e);
        }

        docElement = doc.getDocumentElement();
        if (!docElement.getTagName().equals("bundle")) {
            throw new BundleFormatException("Bundle spec " + bundleSpec
                                          + " has root element '"
                                          + docElement.getTagName()
                                          + "', expected 'bundle'");
        }

        children = docElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if ("mainJar".equals(node.getNodeName())) {
                if (mainJar != null) {
                    log.error ("Duplicate definition of mainJar. Ignoring.");
                }
                mainJar = new File (node.getTextContent());
            } else if ("mainClass".equals(node.getNodeName())) {
                if (mainClass != null) {
                    log.error ("Duplicate definition of mainClass. Ignoring.");
                }
                mainClass = node.getTextContent();
            } else if ("description".equals(node.getNodeName())) {
                if (description != null) {
                    log.error ("Duplicate definition of description. Ignoring.");
                }
                description = node.getTextContent();
            } else if ("bundleId".equals(node.getNodeName())) {
                if (bundleId != null) {
                    log.error ("Duplicate definition of bundleId. Ignoring.");
                }
                bundleId = node.getTextContent();
            } else if ("instanceId".equals(node.getNodeName())) {
                if (instanceId != null) {
                    log.error ("Duplicate definition of instanceId. Ignoring.");
                }
                instanceId = node.getTextContent();
            } else if ("fileList".equals(node.getNodeName())) {
                if (checkedFileList) {
                    log.error ("Duplicate definition of fileList. Ignoring.");
                }
                checkFilelist (bundleDir, node);
                checkedFileList = true;
            } else if ("property".equals(node.getNodeName())) {
                String name = node.getAttributes().getNamedItem("name").getNodeValue();
                String value = node.getAttributes().getNamedItem("value").getNodeValue();

                if (name == null) {
                    log.error ("Found property element without name. Ignoring.");
                    continue;
                } else if (value == null) {
                    log.error ("Found property element '" + name
                              + "'without value. Ignoring.");
                    continue;
                }
                jvmArgs.add ("-D"+name+"="+value);
            }
        }

        if (mainJar == null) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing 'mainJar' tag");
        } else if (mainClass == null) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing  'mainClass' tag");
        } else if (description == null) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing 'description' tag");
        } else if (bundleId == null) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing 'bundleId' tag");
        } else if (instanceId == null) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing  'instanceId' tag");
        } else if (!checkedFileList) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing  'fileList' tag");
        }

        return new MetaBundle(bundleDir, bundleSpec, mainJar,
                              mainClass, instanceId, bundleId,
                              jvmArgs);
    }

    private void checkFilelist(File bundleDir, Node node) {
        NodeList files = node.getChildNodes();

        // Check that each file in fileList exists
        for (int i = 0; i < files.getLength(); i++) {

            // Skip garbage text and comments
            if (files.item(i).getNodeType() == Node.TEXT_NODE ||
                files.item(i).getNodeType() == Node.COMMENT_NODE) {
                continue;
            }

            // Assert that we only have file nodes
            if (!"file".equals(files.item(i).getNodeName())) {
                throw new BundleFormatException("Illegal child '"
                + files.item(i).getNodeName() + "' of fileList");
            }

            File testFile = new File (bundleDir,
                                      files.item(i).getTextContent());
            if (!testFile.exists()) {
                throw new BundleFormatException("Listed file '"
                                              + testFile + "' does not exist");
            }
        }

        // TODO: Check the converse - that each file is listed in fileList

        // TODO: Check md5 if the md5 attribute exists on the file element
    }

    private void checkJVMArgs (Node node) {

    }
}
