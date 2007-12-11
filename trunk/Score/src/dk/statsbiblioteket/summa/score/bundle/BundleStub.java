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
import dk.statsbiblioteket.summa.score.api.Service;
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
        author = "mke")
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
     *
     * @param bundleDir Root directory of the bundle
     * @param bundleId Bundle specific id
     * @param instanceId id to use for the {@link Service or {@link Client}
     *                   spawned by {@link #start}
     * @param mainJar The jar containing the main file of the bundle
     * @param mainClass The qualified class name of the main class
     * @param libs List of jar files, relative to {@code bundleDir}, to include
     *             in the class path when launching the bundle.
     * @param jvmArgs Extra arguments to pass to the JVM when spawning the
     *                JVM of the {@link Service or {@link Client} 
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
     * Set a system property for the command line that will be created
     * by {@link #buildCommandLine()}. This will be used when running
     * {@link #start()}.
     * @param name the name of the property to set
     * @param value the value of the property to set
     */
    public void addSystemProperty (String name, String value) {
        jvmArgs.add ("-D"+name+"="+value);
    }

    /**
     * <p>Launch the bundle in a separate JVM. The working directory of the
     * child JVM will be that specified in the {@code bundleDir} passed
     * to the constructor.</p>
     * <p>The command used to launch the new JVM is constructed by
     * the {@link #buildCommandLine()} method of this class.</p>
     *
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

    /**
     * <p>Create a command line suitable for passing to
     * {@link ProcessBuilder#command}.</p>
     * <p>The command line will automatically pick up any jmx or RMI policy
     * files found in the {@code bundleDir} passed to the constructor.
     * Specifically it will update the following system properties
     * given that the relevant files are found</p>
     * <ul>
     * <li>{@code java.security.policy} if
     *     {@code <bundleDir>/config/policy} is found</li>
     * <li>{@code com.sun.management.jmxremote.password.file} if
     *     {@code <bundleDir>/config/jmx.password} is found</li>
     * <li>{@code com.sun.management.jmxremote.access.file} if
     *     {@code <bundleDir>/config/jmx.access} is found</li>
     * </ul>
     * <p>The classpath will contain {@code .:config:data} plus the
     * {@code mainJar} and {@code libs} passed to the constructor.</p>
     *
     * <p>Any properties specified by {@link #addSystemProperty(String, String)}
     * will also be added to the command line.</p>
     *
     * @return A list of command line arguments, the first one being the
     *         {@code java} executable of the current JVMs {@code java.home}.
     */
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
