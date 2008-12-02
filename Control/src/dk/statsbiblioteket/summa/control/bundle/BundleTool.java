package dk.statsbiblioteket.summa.control.bundle;

import org.apache.commons.cli.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.IOException;

/**
 * Main class for the Summa bundle builder tool.
 */
public class BundleTool {

    private boolean verbose;
    private boolean dryRun;
    private boolean sloppy;
    private boolean expandProps;
    private String overrideName;
    private String bundleName;
    private File specFile;
    private File outputDir;

    private static void printHelp (Options options) {
        String usage = "java -jar summa-bundle.jar [options] <bundle-spec>";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(usage, options);
    }

    public static void main (String[] args) {
        CommandLine cli = null;
        boolean verbose = false;
        boolean dryRun = false;
        boolean sloppy = false;
        boolean expandProps = false;
        String overrideName = null;
        String outputDir = null;
        File specFile = null;

        // Build command line options
        CommandLineParser cliParser = new PosixParser();
        Options options = new Options();
        options.addOption("h", "help", false, "Print help message and exit");
        options.addOption("v", "verbose", false, "Enable verbose ourput");
        options.addOption("n", "name", true, "Override bundle name");
        options.addOption("d", "dry-run", false, "Don't roll the bundle. Only do validation");
        options.addOption("s", "sloppy", false, "Don't do validation of bundle contents");
        options.addOption("o", "output", true, "Directory to place the resulting bundle in");
        options.addOption("x", "expand-properties", false, "Expand @-enclosed system properties in the spec file");

        // Parse and validate command line
        try {
            cli = cliParser.parse(options, args);
            String[] specFiles = cli.getArgs();
            if (specFiles.length == 0) {
                throw new ParseException("Not enough arguments,"
                                         + "you must specify a bundle spec");
            } else if (specFiles.length > 1) {
                throw new ParseException("Too many arguments. Only one bundle "
                                         + "spec may be specified");
            }

            specFile = new File (specFiles[0]);

        } catch (ParseException e) {
            printHelp(options);
            System.exit (2);
        }

        // Extract information from command line
        verbose = cli.hasOption("verbose");
        dryRun = cli.hasOption("dry-run");
        sloppy = cli.hasOption("sloppy");
        expandProps = cli.hasOption("expand-properties");
        overrideName = cli.getOptionValue("name");
        outputDir = cli.getOptionValue("name", System.getProperty("user.dir"));

        try {
            new BundleTool(specFile, new File(outputDir),
                           verbose, dryRun, sloppy, expandProps,
                           overrideName).run();
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


    public BundleTool(File specFile, File outputDir,
                      boolean verbose, boolean dryRun,
                      boolean sloppy, boolean expandProps,
                      String overrideName) {
        this.specFile = specFile;
        this.outputDir = outputDir;
        this.verbose = verbose;
        this.dryRun = dryRun;
        this.sloppy = sloppy;
        this.expandProps = expandProps;
        this.overrideName = overrideName;

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

    }

    public void println (String line) {
        System.out.println (line);
    }

    public void run () throws IOException {
        if (verbose) {
            println("Building bundle for '" + specFile  + "'");
            println("Output dir: " + outputDir);
            println("Enable validation: " + !sloppy);
            println("Dry run: " + dryRun);
            println("Bundle dir: " + specFile.getParent());
        }

        BundleSpecBuilder builder = BundleSpecBuilder.open(specFile);

        // Make sure we have a valid fileList.
        // This must be done before validation
        try {
            if (builder.getFiles().isEmpty()) {
                builder.buildFileList(specFile.getParentFile());
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
        println(builder.getDisplayString(true));

        // Roll bundle zip-file
        if (!dryRun) {
            if (verbose) {
                println("Writing bundle '" + bundleName + "' to '"
                        + outputDir + "'");
            }
            builder.buildBundle(specFile.getParentFile(), outputDir);

            if (!new File(outputDir, bundleName + Bundle.BUNDLE_EXT).exists()) {
                throw new RuntimeException("Unknown error writing bundle to '"
                                           + outputDir + "'");
            }

        } else if (verbose) {
            println("Skipped creation of bundle");
        }

    }

    private void expandProperties(BundleSpecBuilder builder) {
        if (verbose) {
            println("Expanding system properties");
        }

        // This is ridiculously inefficient, but it gets the job done...
        for (Map.Entry entry: System.getProperties().entrySet()){
            String pattern = "@" + entry.getKey().toString() + "@";
            String newVal = entry.getValue().toString();

            builder.setBundleId(builder.getBundleId().replace(pattern, newVal));
            builder.setDescription(builder.getDescription().replace(pattern,
                                                                    newVal));
            builder.setMainClass(builder.getMainClass().replace(pattern,
                                                                newVal));
            builder.setMainJar(builder.getMainJar().replace(pattern, newVal));

            // Update the API list
            ArrayList<String> api = new ArrayList<String>();
            for (String apiFile : builder.getApi()) {
                api.add(apiFile.replace(pattern, newVal));
            }
            builder.getApi().clear();
            builder.getApi().addAll(api);

            // Update the File list
            ArrayList<String> files = new ArrayList<String>();
            for (String file : builder.getFiles()) {
                files.add(file.replace(pattern, newVal));
            }
            builder.getFiles().clear();
            builder.getFiles().addAll(api);
        }
    }

    private void validate (BundleSpecBuilder builder) {
        builder.checkFileList(specFile.getParentFile());
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
