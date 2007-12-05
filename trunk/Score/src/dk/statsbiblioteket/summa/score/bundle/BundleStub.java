/* $Id: BundleStub.java,v 1.9 2007/10/29 14:38:15 mke Exp $
 * $Revision: 1.9 $
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

import dk.statsbiblioteket.summa.score.client.Client;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Stub class for launching a bundle in a separate JVM.
 * @see BundleLoader
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="some methods needs Javadoc")
public class BundleStub {

    /**
     * If this file is present the system property {@code java.security.policy}
     * will be set to point at this file.
     */
    public static final String POLICY_FILE = "config" + File.separator + "policy";

    /**
     * If this file is present the system property
     * {@code com.sun.management.jmxremote.password.file} will be set to point
     * at it.
     */
    public static final String JMX_PASSWORD_FILE =
                                     "config" + File.separator + "jmx.password";

    /**
     * If this file is present the system property
     * {@code com.sun.management.jmxremote.access.file} will be set to point
     * at it.
     */
    public static final String JMX_ACCESS_FILE =
                                       "config" + File.separator + "jmx.access";

    private File bundleDir, mainJar;
    private String bundleId, instanceId, mainClass;
    private List<String> libs;
    private List<String> jvmArgs;
    private Log log;

    /**
     * It is the responsibilty of the {@link Client} building the
     * BundleStub to set the following properties on the stub:
     * <ul>
     *   <li>{@code summa.configuration}</li>
     *   <li>{@code summa.score.client.persistent.dir}</li>
     * </ul>
     */
    BundleStub (File bundleDir, String bundleId, String instanceId,
                File mainJar, String mainClass,
                List<String> libs, List<String> jvmArgs) {
        this.bundleDir = bundleDir;
        this.bundleId = bundleId;
        this.instanceId = instanceId;
        this.mainJar = mainJar;
        this.mainClass = mainClass;
        this.libs = libs;
        this.jvmArgs = jvmArgs;

        log = LogFactory.getLog (BundleStub.class);
    }

    /**
     * Set a system property for the JVM that will be spawned by this stub.
     * @param name the name of the property to set
     * @param value the value of the property to set
     */
    public void addSystemProperty (String name, String value) {
        jvmArgs.add ("-D"+name+"="+value);
    }

    /**
     * Launch the bundle in a separate JVM
     * @throws IOException if there is an error launching the command
     * @return A process handle to the spawned JVM
     */
    public Process start () throws IOException {
        List<String> cmdLine = buildCommandLine();
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(bundleDir);
        pb.command(cmdLine);

        return pb.start();
    }

    public List<String> buildCommandLine () {
        String javaExecutable;
        List<String> cl = new ArrayList<String>();
        boolean hasJmxPassword = false, hasJmxAccess = false, hasPolicy = false;

        javaExecutable = System.getProperty("java.home")
                         + File.separator + "bin" + File.separator + "java";

        cl.add (javaExecutable);

        if (new File(bundleDir,POLICY_FILE).exists()) {
            cl.add ("-Djava.security.policy=" + POLICY_FILE);
            hasPolicy = true;
        }

        if (new File(bundleDir,JMX_PASSWORD_FILE).exists()) {
            cl.add ("-Dcom.sun.management.jmxremote.password.file="
                    + JMX_PASSWORD_FILE);
            hasJmxPassword = true;
        }

        if (new File(bundleDir,JMX_ACCESS_FILE).exists()) {
            cl.add ("-Dcom.sun.management.jmxremote.access.file="
                    + JMX_ACCESS_FILE);
            hasJmxAccess = true;
        }

        if ( ! (hasPolicy == hasJmxAccess == hasJmxPassword)) {
            log.warn ("Missing " + (hasPolicy ? "":POLICY_FILE) + " "
                                 + (hasJmxAccess ? "":JMX_ACCESS_FILE) + " "
                                 + (hasJmxPassword ? "":JMX_PASSWORD_FILE)
                                 + ". JMX is unlikely to work.");
        }

        cl.addAll(jvmArgs);

        // Build class path
        String classPath = ".:config:data:"+ mainJar.toString().trim();
        for (String lib : libs) {
            classPath += ":" + lib;
        }

        cl.add ("-cp");
        cl.add (classPath);

        cl.add (mainClass);

        return cl;
    }

}
