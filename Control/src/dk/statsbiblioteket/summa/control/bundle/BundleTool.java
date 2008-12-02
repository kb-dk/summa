package dk.statsbiblioteket.summa.control.bundle;

import org.apache.commons.cli.*;

import java.util.List;
import java.io.File;

/**
 * Main class for the Summa bundle builder tool.
 */
public class BundleTool {

    private boolean verbose;
    private boolean dryRun;
    private boolean sloppy;
    private String overrideName;
    private File specFile;

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
        String overrideName = null;
        File specFile = null;

        // Build command line options
        CommandLineParser cliParser = new PosixParser();
        Options options = new Options();
        options.addOption("h", "help", false, "Print help message and exit");
        options.addOption("v", "verbose", false, "Enable verbose ourput");
        options.addOption("n", "name", true, "Override bundle name");
        options.addOption("d", "dry-run", false, "Don't roll the bundle. Only do validation");
        options.addOption("s", "sloppy", false, "Don't do validation of bundle contents");

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
        overrideName = cli.getOptionValue("name");

        try {
            new BundleTool(specFile, verbose, dryRun, sloppy, overrideName).run();
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
        }
    }


    public BundleTool(File specFile,
                      boolean verbose, boolean dryRun,
                      boolean sloppy, String overrideName) {
        this.specFile = specFile;
        this.verbose = verbose;
        this.dryRun = dryRun;
        this.sloppy = sloppy;
        this.overrideName = overrideName;

        if (!specFile.exists()) {
            throw new BundleLoadingException("No such bundle spec '"
                                             + specFile + "'");
        }
    }

    public void println (String line) {
        System.out.println (line);
    }

    public void run () {
        if (verbose) {
            println("Building bundle for '" + specFile  + "'");
        }
    }
}
