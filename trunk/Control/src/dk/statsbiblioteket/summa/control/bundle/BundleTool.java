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
package dk.statsbiblioteket.summa.control.bundle;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import dk.statsbiblioteket.summa.common.util.Environment;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Main class for the Summa bundle builder tool.
 */
@QAInfo(author = "mke, hbk",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED
)
public class BundleTool {
    private boolean verbose;
    private boolean dryRun;
    private boolean sloppy;
    private boolean expandProps;
    private String overrideAutostart;
    private String overrideName;
    private File specFile;
    private File outputDir;
    private File[] fileDirs;

    /**
    * Print a help text for the bundle.
    * @param options The possible options for this command.
    */
    private static void printHelp (Options options) {
        String usage = "java -jar summa-bundle.jar [options] <bundle-spec>";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(usage, options);
    }

    /**
     * The main method for the BundleTool class.
     * @param args The arguments given on commandline.
     */
    public static void main (String[] args) {
        CommandLine cli = null;
        boolean verbose;
        boolean dryRun;
        boolean sloppy;
        boolean expandProps;
        String overrideAutostart;
        String overrideName;
        String outputDir;
        String[] fileDirs;
        File specFile = null;

        // Build command line options
        CommandLineParser cliParser = new GnuParser();
        Options options = new Options();
        options.addOption("h", "help", false, "Print help message and exit");
        options.addOption("v", "verbose", false, "Enable verbose output");
        options.addOption("n", "name", true, "Override bundle name");
        options.addOption("d", "dry-run", false, "Don't roll the bundle. Only do validation");
        options.addOption("s", "sloppy", false, "Don't do validation of bundle contents");
        options.addOption("o", "output", true, "Directory to place the resulting bundle in");
        options.addOption("x", "expand-properties", false, "Expand ${} enclosed, Ant-style, system properties in the spec file");
        options.addOption("a", "auto-start", true, "Override whether or not to enable auto start of the bundle");
        options.addOption("f", "files", true,
                          "Path to collect files for the bundle from. "
                          + "You may specify multiple paths by separated by"
                          + "':'");


        // Parse and validate command line
        try {
            cli = cliParser.parse(options, args);
            String[] specFiles = cli.getArgs();
            if (specFiles.length == 0) {
                throw new ParseException("Not enough arguments, "
                                         + "you must specify a bundle spec");
            } else if (specFiles.length > 1) {
                throw new ParseException("Too many arguments. Only one bundle "
                                         + "spec may be specified");
            }

            specFile = new File (specFiles[0]);

        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            printHelp(options);
            System.exit (2);
        }

        // Extract information from command line
        verbose = cli.hasOption("verbose");
        dryRun = cli.hasOption("dry-run");
        sloppy = cli.hasOption("sloppy");
        expandProps = cli.hasOption("expand-properties");
        overrideName = cli.getOptionValue("name");
        overrideAutostart = cli.getOptionValue("auto-start");
        outputDir = cli.getOptionValue("output", System.getProperty("user.dir"));

        fileDirs = cli.getOptionValue("files") == null ?
                                  null : cli.getOptionValue("files").split(":");
        if (fileDirs == null) {
            fileDirs = new String[] {specFile.getParent()};
        }

        File[] _fileDirs = new File[fileDirs.length];
        for (int i = 0; i < fileDirs.length; i++) {
            _fileDirs[i] = new File(fileDirs[i]);
        }

        try {
            new BundleTool(specFile, new File(outputDir), _fileDirs,
                           verbose, dryRun, sloppy, expandProps,
                           overrideAutostart, overrideName).run();
        } catch (BundleLoadingException e) {
            System.err.println("Error loading bundle: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(3);
        } catch (BundleFormatException e) {
            System.err.println("Bundle format error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(4);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(5);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(-1);
        }
    }

    /**
     * Constructor for the BundleTool class. This is used to specify values for the BundleTool, eg. wether or not this
     * should be verbose, see params for all params.
     *
     * @param specFile Specification file for the bundle. Should be a valid specification file.
     * @param outputDir Directory where to write bundle data.
     * @param fileDirs File directories to look into for bundle.
     * @param verbose True if this run should be verbose.
     * @param dryRun True if this is a dry run.
     * @param sloppy True if this run should be handle sloppy.
     * @param expandProps True if properties should be expanded.
     * @param overrideAutostart String overriding the auto start commands.
     * @param overrideName True if we should override the bundle name.
     */
    public BundleTool(File specFile, File outputDir, File[] fileDirs,
                      boolean verbose, boolean dryRun,
                      boolean sloppy, boolean expandProps,
                      String overrideAutostart, String overrideName) {
        this.specFile = specFile;
        this.outputDir = outputDir;
        this.fileDirs = fileDirs;
        this.verbose = verbose;
        this.dryRun = dryRun;
        this.sloppy = sloppy;
        this.expandProps = expandProps;
        this.overrideName = overrideName;
        this.overrideAutostart = overrideAutostart;

        if (!specFile.exists()) {
            throw new BundleLoadingException("No such bundle spec '"
                                             + specFile + "'");
        }

        if (!specFile.isFile()) {
            throw new BundleLoadingException("Bundle spec '" + specFile + "' "
                                             + "is not a regular file");
        }

        if (!("client.xml".equals(specFile.getName()) ||
            "service.xml".equals(specFile.getName()))) {
            throw new BundleLoadingException("Invalid bundle spec '"
                                             + specFile + "'. The bundle spec "
                                             + "file must be either a "
                                             + "'service.xml' or 'client.xml'."
                                             + " Found '" + specFile.getName()
                                             + "'");
        }

        if (!outputDir.exists()) {
            throw new RuntimeException("Output directory '" + outputDir
                                       + "' does not exist");
        }

        if (!outputDir.isDirectory()) {
            throw new RuntimeException("Output destination '" + outputDir
                                       + "' is not a directory");
        }

        for (File dir : fileDirs) {
            if (!dir.exists()) {
                throw new RuntimeException("File source directory '"
                                           + fileDirs.toString()
                                           + "' does not exist");
            }
            if (!dir.isDirectory()) {
                throw new RuntimeException("File source '" + fileDirs.toString()
                                           + "' is not a directory");
            }
        }



    }

    /**
     * Shortcut for printing a line to System.out.
     * @param line The line to print.
     */
    public void println (String line) {
        System.out.println (line);
    }

    /**
     * Run the bundle, bundle must have a valid file list.
     * @throws IOException if error occurs while running bundle.
     */
    public void run () throws IOException {
      String bundleName;
        if (verbose) {
            println("Building bundle for '" + specFile  + "'");
            println("Output dir: " + outputDir);
            println("Enable validation: " + !sloppy);
            println("Dry run: " + dryRun);
            println("File input dirs: " + Strings.join(fileDirs, ", "));
        }

        BundleSpecBuilder builder = BundleSpecBuilder.open(specFile);

        // Make sure we have a valid fileList.
        // This must be done before validation
        try {
            if (builder.getFiles().isEmpty()) {
                if (verbose) {
                    println("No fileList specified. Scanning input directories "
                            + "to build file list:");
                }
                for (File dir: fileDirs) {
                    if (verbose) {
                        println("Scanning: " + dir);
                    }
                    builder.buildFileList(dir);
                }
            }
        } catch (IOException e) {
            throw new BundleLoadingException("Failed to build file list for "
                                             + "spec '" + specFile + "': "
                                             + e.getMessage(), e);
        }

        // Expand system properties
        if (expandProps) {
            expandProperties(builder);
        }

        // Validate
        if (!sloppy) {
            validate(builder);
            if (verbose) {
                println("Validation ok");
            }
        } else if (verbose) {
            println("Skipping validation");
        }

        // Autostart handling
        if (overrideAutostart != null) {
            boolean autostart = Boolean.parseBoolean(overrideAutostart);

            if (verbose) {
                println("Overriding autoStart to: " + autostart);
            }

            builder.setAutoStart(autostart);
        } else if (verbose) {
            println("Keeping original autoStart settings");
        }

        // Get/Set bundle name
        bundleName = builder.getBundleId();
        if (overrideName != null) {
            bundleName = overrideName;
            builder.setBundleId(bundleName);
        }
        if (verbose) {
            println("Bundle name: " + bundleName);
        }

        // Always print the resume
        println("\n" + builder.getDisplayString(true));

        println("Writing bundle to: "
                + new File(outputDir, bundleName + Bundle.BUNDLE_EXT));

        // Roll bundle zip-file
        if (!dryRun) {
            builder.buildBundle(fileDirs, outputDir);

            if (!new File(outputDir, bundleName + Bundle.BUNDLE_EXT).exists()) {
                throw new RuntimeException("Unknown error writing bundle to '"
                                           + outputDir + "'");
            }

        } else {
            println("Dry run. Skipped creation of bundle, nothing written to disk");
        }

    }

    /**
     * Expand the properties given.
     * 
     * @param builder The builder from which to expand properties.
     */
    private void expandProperties(BundleSpecBuilder builder) {
        if (verbose) {
            println("Expanding system properties");
        }

        builder.setBundleId(
                Environment.escapeSystemProperties(builder.getBundleId()));
        builder.setDescription(
                Environment.escapeSystemProperties(builder.getDescription()));
        builder.setMainClass(
                Environment.escapeSystemProperties(builder.getMainClass()));
        builder.setMainJar(
                Environment.escapeSystemProperties(builder.getMainJar()));

        // Update the API list
        List<String> api = Environment.escapeSystemProperties(builder.getApi());
        builder.getApi().clear();
        builder.getApi().addAll(api);

        // Update the File list
        List<String> files =
                         Environment.escapeSystemProperties(builder.getFiles());
        builder.getFiles().clear();
        builder.getFiles().addAll(files);

        // Update extra args to the JVM
        List<String> jvmArgs =
                       Environment.escapeSystemProperties(builder.getJvmArgs());
        builder.getJvmArgs().clear();
        builder.getJvmArgs().addAll(jvmArgs);

        // Update property values        
        for (Map.Entry<String, Serializable> entry: builder.getProperties()) {
            entry.setValue(
               Environment.escapeSystemProperties(entry.getValue().toString()));
        }
    }

    /**
     * Validate the builder.
     *
     * @param builder The bulider to validate.
     */
    private void validate (BundleSpecBuilder builder) {
        builder.checkFileList(fileDirs);
        builder.checkPublicApi();

        if (builder.getInstanceId() != null) {
            throw new BundleFormatException("Bundle has instance id '"
                                            + builder.getInstanceId()
                                            + "' assigned. Only the bundleId "
                                            + "should be present");
        }

        if (builder.getMainClass() == null) {
            throw new BundleFormatException("Bundle spec has no mainClass"
                                            + " element");
        }

        if (builder.getMainJar() == null) {
            throw new BundleFormatException("Bundle spec has no mainJar"
                                            + " element");
        }

        if (builder.getBundleId() == null) {
            throw new BundleFormatException("Bundle spec has no bundleId"
                                            + " element");
        }

        if (verbose) {
            println("Bundle validated. Found bundle id: "
                    + builder.getBundleId());
        }

    }
}

