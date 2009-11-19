/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.summa.control.api.ServicePackageException;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>The {@code BundleLoader} class is used to create a {@link BundleStub} object
 * from a given unpacked {@code .bundle} file.</p>
 *
 * <p>It facilitates a {@link BundleSpecBuilder} to read the bundle spec
 * and then does additional validation on top. There core thing that sets
 * it apart from a BundleSpecBuilder is that it needs a reference directory
 * used to look up the files listed in the spec.</p>
 *
 * @see BundleStub
 * @see BundleSpecBuilder
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class BundleLoader implements Configurable {

    /** The file name used to identify a service */
    public static final String SERVICE_BUNDLE_SPEC = "service.xml";

    /** The file name used to identify a client */
    public static final String CLIENT_BUNDLE_SPEC = "client.xml";

    private Log log = LogFactory.getLog(BundleLoader.class);;

    public BundleLoader (Configuration conf) {
        // Do nothing
    }

    /**
     * Parse and prepare a bundle for loading via a {@link BundleStub}
     * @param bundleDir Directory containing an unpacked {@code .bundle} file
     * @return a stub from which the new JVM can be launched
     * @throws ServicePackageException if the package is not a directory
     * @throws FileNotFoundException if the corresponding package is not deployed
     */
    public BundleStub load (File bundleDir) throws IOException {
        log.trace("load(" + bundleDir + ") called");
        List<String> libs = new ArrayList<String>(20);
        List<String> jvmArgs = new ArrayList<String>(10);
        BundleSpecBuilder builder = checkBundle(bundleDir);

        /* Find all .jar files in lib/ */
        File libDir = new File (bundleDir, "lib");
        if (libDir.exists()) {
            for (String lib : libDir.list()) {
                if (lib.endsWith(".jar")) {
                    String jar = "lib" + File.separator + lib;
                    libs.add(jar);
                    log.trace("load added jar '" + jar + "'");
                }
            }
        }

        /* Construct JVM args. Property values need to be surrounded by
         * quotes to ensure property values may contain spaces */
        for (Map.Entry<String, Serializable> entry : builder.getProperties()) {
            String arg = (String) entry.getValue();
            //arg = arg.replace (" ", "\\ ");
            arg = "-D"+entry.getKey()+"="+arg; 
            jvmArgs.add(arg);
            log.trace("Added JVM arg " + arg);
        }

        /* Add explicit JVM args */
        for (String arg : builder.getJvmArgs()) {
            jvmArgs.add(arg);
        }

        log.trace("Returning BundleStub for instance '"
                  + builder.getInstanceId() + "' of '"
                  + builder.getBundleId() + "'");
        return new BundleStub(bundleDir,
                              builder.getBundleId(),
                              builder.getInstanceId(),
                              new File(builder.getMainJar()),
                              builder.getMainClass(),
                              libs,
                              jvmArgs);

    }

    private BundleSpecBuilder checkBundle (File bundleDir) {
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

    private BundleSpecBuilder checkBundleSpec (File bundleDir, File bundleSpec) {
        BundleSpecBuilder builder;

        try {
            builder = BundleSpecBuilder.open (bundleSpec);
        } catch (IOException e) {
            throw new BundleLoadingException("Failed to read bundle spec "
                                             + bundleSpec, e);
        }

        if (builder.getMainJar() == null) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing 'mainJar' tag");
        } else if (builder.getMainClass() == null) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing  'mainClass' tag");
        } else if (builder.getDescription() == null) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing 'description' tag");
        } else if (builder.getBundleId() == null) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing 'bundleId' tag");
        } else if (builder.getInstanceId() == null) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing  'instanceId' tag");
        } else if (builder.getFiles().size() == 0) {
            throw new BundleFormatException("In bundle spec '" + bundleSpec
                                          + "', missing  or empty 'fileList'"
                                          + " tag");
        }

        File mainJar = new File(bundleDir, builder.getMainJar());
        if (!mainJar.exists()) {
            throw new BundleLoadingException("Main jar does not exist: '"
                                             + mainJar + "'");
        }

        builder.checkFileList(bundleDir);

        return builder;
    }

}



