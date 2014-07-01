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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.*;

/**
 * Searches folders for ALTO duplicate files. The hash value of Strings are used for comparison to lower memory
 * requirements, so false positives are possible.
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoDuplicateFinder {
    private static Log log = LogFactory.getLog(AltoDuplicateFinder.class);

    /**
     * The main method for the BundleTool class.
     * @param args The arguments given on commandline.
     */
    @SuppressWarnings({"AccessStaticViaInstance", "UseOfSystemOutOrSystemErr"})
    public static void main (String[] args) throws FileNotFoundException, XMLStreamException {
        List<Requirement> requirements = new ArrayList<>();
        List<File> paths = new ArrayList<>();
        int maxGroups = Integer.MAX_VALUE;
        int maxGroupSize = Integer.MAX_VALUE;
        String altoSuffix = ".alto.xml";
        String root = "/avis-upload";
        String output = "/itweb-data/ninestars/duplicates";
        boolean verbose = false;

        // Build command line options
        CommandLineParser cliParser = new GnuParser();
        Options options = new Options();
        options.addOption("h", "help", false, "Print help message and exit");
//        options.addOption("v", "verbose", false, "Enable verbose output");
        options.addOption(OptionBuilder.withArgName("requirements").isRequired().hasArgs().withDescription(
                "1 or more requirements of the form b@s or b%@s, where b is the minimum number of blocks " +
                "(absolute number or percent) and s is the minimum block size (counted as characters) that" +
                "has to match for two ALTO's to be considered duplicates. Samples: 3@200 and 50%@0.").create("r"));
        options.addOption(OptionBuilder.withArgName("maxgroups").hasArg().withDescription(
                "The maximum number of groups to output. Optional (default is unlimited).").create("g"));
        options.addOption(OptionBuilder.withArgName("maxgroupsize").hasArg().withDescription(
                "The maximum number of ALTOs to output per group. Optional (default is unlimited).").create("s"));
        options.addOption(OptionBuilder.withArgName("altosuffix").hasArg().withDescription(
                "The suffix for ALTO files. Optional (default is .alto.xml).").create("a"));
        options.addOption(OptionBuilder.withArgName("pathprefix").hasArg().withDescription(
                "This prefix will be removed from paths when generating the output structure. " +
                "Optional (default is " + root + ").").create("x"));
        options.addOption(OptionBuilder.withArgName("output").hasArg().withDescription(
                "This output folder for the structure of groups to generate. " +
                "Optional (default is " + output + ").").create("o"));
        options.addOption(OptionBuilder.withArgName("paths").isRequired().hasArgs().withDescription(
                "Paths to search for duplicates. Each path is processed independent of the other paths.").
                create("p"));
        options.addOption(OptionBuilder.withArgName("verbose").withDescription(
                "Verbose output (mainly intended for debug).").create("v"));

        // Parse and validate command line
        try {
            CommandLine cli = cliParser.parse(options, args);
            for (String r: cli.getOptionValues("r")) {
                requirements.add(new Requirement(r));
            }
            maxGroups = Integer.parseInt(cli.getOptionValue("g", Integer.toString(maxGroups)));
            maxGroupSize = Integer.parseInt(cli.getOptionValue("s", Integer.toString(maxGroupSize)));
            altoSuffix = cli.getOptionValue("a", altoSuffix);
            root = cli.getOptionValue("x", root);
            output = cli.getOptionValue("o", output);
            verbose = cli.hasOption("v");

            for (String p: cli.getOptionValues("p")) {
                paths.add(new File(p));
                if (!paths.get(paths.size()-1).exists()) {
                    throw new ParseException("The path '" + p + "' does not exists");
                }
            }
            findDuplicates(requirements, maxGroups, maxGroupSize, altoSuffix, paths, root, output, verbose);
        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            String usage = "AltoDuplicateFinder [options] <paths>";

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);
            System.exit (2);
        }

        // Extract information from command line
//        verbose = cli.hasOption("verbose");
//        overrideName = cli.getOptionValue("name");
//        outputDir = cli.getOptionValue("output", System.getProperty("user.dir"));

    }

    private static class Requirement {
        public final double minBlocks;
        public final int minTextSize;
        public final boolean percent;

        public Requirement(String setup) {
            String[] tokens = setup.split("@");
            if (tokens.length != 2) {
                throw new IllegalArgumentException(
                        "Unrecognized requirement '" + setup + "'. Expected b@s or b%@s, for example 3@200 or 50%@10");
            }
            if (tokens[0].endsWith("%")) {
                percent = true;
                minBlocks = Double.parseDouble(tokens[0].replace("%", ""));
            } else {
                percent = false;
                minBlocks = Double.parseDouble(tokens[0]);
            }
            minTextSize = Integer.parseInt(tokens[1]);
        }

        @Override
        public String toString() {
            return "Requirement(minBlocks=" + (percent ? minBlocks + "%" : (int) minBlocks)
                   + ", minTextSize=" + minTextSize + ")";
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void findDuplicates(
            List<Requirement> requirements, int maxGroups, int maxGroupSize, String altoSuffix, List<File> paths,
            String root, String output, boolean verbose) throws FileNotFoundException, XMLStreamException {
        for (File path: paths) {
            findDuplicates(requirements, maxGroups, maxGroupSize, altoSuffix, path, root, output, verbose);
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void findDuplicates(
            List<Requirement> requirements, int maxGroups, int maxGroupSize, String altoSuffix, File source,
            String root, String output, boolean verbose) throws FileNotFoundException, XMLStreamException {
        long startTime = System.nanoTime();
        System.out.println("# Locating ALTO files in " + source);
        List<Set<File>> groups = new ArrayList<>();
        groups.add(new HashSet<>(getAltoFiles(new File[]{source}, altoSuffix)));
        for (int r = 0 ; r < requirements.size() ; r++) {
            int memberCount = 0;
            for (Set<File> group: groups) {
                memberCount += group.size();
            }
            System.out.println(String.format(
                    "#   Duplicate isolation %d/%d for %d ALTOs in %d groups with rule %s",
                    r+1, requirements.size(), memberCount, groups.size(), requirements.get(r)));
            List<Set<File>> reduced = new ArrayList<>();
            for (Set<File> group: groups) {
                reduced.addAll(findDuplicates(group, requirements.get(r), verbose));
            }
            groups = reduced;
        }

        if (groups.isEmpty()) {
            System.out.println(String.format(
                    "#   No groups satisfying all given requirements. Analysis time %d seconds",
                    (System.nanoTime()-startTime)/1000000000L));
            return;
        }

        final int outGroups = Math.min(maxGroups, groups.size());
        System.out.println(String.format(
                "#   Listing %d/%d groups of ALTOs with duplicate text blocks from source %s. " +
                "Analysis time %s seconds",
                outGroups, groups.size(), source, (System.nanoTime()-startTime)/1000000000L));
        if (groups.isEmpty()) {
            return;
        }

        String sans = source.toString().replace(root, "");
        String superGroup = new File(output, sans).toString(); // Handles /-fiddling

        int dup = 1;
        for (Set<File> entries: groups) {
            System.out.println("");
            String finalDest = String.format("%s/%02d", superGroup, dup);
            if (dup > maxGroups) {
                System.out.print("# ");
            }
            System.out.println(String.format(
                    "#   Group %d/%d with %d/%d ALTOs",
                    dup, groups.size(), Math.min(entries.size(), maxGroupSize), entries.size()));
            if (dup > maxGroups) {
                System.out.print("#   ");
            }
            System.out.println("mkdir -p " + finalDest);
            int entryCount = 1;
            for (File entry: entries) {
                String prefix = entry.getName().replace(altoSuffix, "");
                for (File f: entry.getParentFile().listFiles()) {
                    if (f.isFile() && f.getName().startsWith(prefix)) {
                        if (dup > maxGroups || entryCount >= maxGroupSize) {
                            System.out.print("#   ");
                        }
                        System.out.println(
                                "ln -s "+ f.getAbsolutePath() + " " + new File(finalDest, f.getName()));
                    }
                }
                entryCount++;
            }
            dup++;
//                System.out.println(Strings.join(entries, "\n"));
        }
        System.out.println("");
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static List<Set<File>> findDuplicates(Set<File> altoFiles, Requirement requirement, boolean verbose)
            throws FileNotFoundException, XMLStreamException {
        Map<Integer, Set<File>> duplicates = new HashMap<>(); // Text-hash to alto
        System.out.print("#   Calculating text block hashes from " + altoFiles.size() + " alto files\n#   ");
        for(int i = 0 ; i < Math.min(altoFiles.size(), 99) ; i++) {
            System.out.print(".");
        }
        System.out.print("|\n#   ");
        int altoCount = 0;
        int dot = altoFiles.size() > 100 ? altoFiles.size() / 100 : altoFiles.isEmpty() ? 1 : altoFiles.size();
        for (File altoFile: altoFiles) {
            if (++altoCount % dot == 0) {
                System.out.print(".");
            }
            Alto alto = new Alto(altoFile);
            List<String> texts = getTextBlocks(alto, requirement.minTextSize);
            for (String text: texts) {
                Integer hashValue = text.hashCode();
                Set<File> files;
                files = duplicates.get(hashValue);
                if (files == null) {
                    files = new HashSet<>();
                    duplicates.put(hashValue, files);
                }
                files.add(altoFile);
            }
        }
//        System.out.println("\n# Removing 1-member groups from " + duplicates.size() + " potential duplicates");
        System.out.println("\n#   Pruning potential group of " + altoFiles.size() + " ALTOs with " + duplicates.size()
                           + " blocks with " + requirement);
        Map<Integer, Set<File>> moreThanOnes = extractMoreThanOne(duplicates);
        if (verbose) {
            System.out.println("#   Reduced to " + altoFiles.size() + " ALTOs with " + moreThanOnes.size() + " blocks");
        }
        return groupOnMinBlocks(moreThanOnes, duplicates, requirement, verbose);
    }

    private static List<Set<File>> groupOnMinBlocks(
            Map<Integer, Set<File>> moreThanOnes, Map<Integer, Set<File>> all,
            Requirement requirement, boolean verbose) {
        // Reverse the map to go from files to text-hashes
        Map<File, Set<Integer>> reverse = reverseMap(moreThanOnes);
        Map<File, Set<Integer>> reverseAll = reverseMap(all);

        // n^2 search for groups of minBlock matches. This also leaves all sub-groups
        List<Set<File>> groups = new ArrayList<>();
        List<File> files = new ArrayList<>(reverse.keySet());
        for (int g1 = 0 ; g1 < files.size() ; g1++) {
            Set<File> keep = new HashSet<>();
            for (int g2 = g1+1 ; g2 < files.size() ; g2++) {
                Set<Integer> s1 = new HashSet<>(reverse.get(files.get(g1)));
                s1.retainAll(reverse.get(files.get(g2)));
                int maxBlockCount = Math.max(reverseAll.get(files.get(g1)).size(),
                                             reverseAll.get(files.get(g2)).size());
                int minBlocks = Math.max(1, requirement.percent ?
                        (int)(maxBlockCount * requirement.minBlocks / 100) : (int)requirement.minBlocks);
                if (s1.size() >= minBlocks) {
                    keep.add(files.get(g1));
                    keep.add(files.get(g2));
                    if (verbose) {
                        System.out.println(String.format(
                                "#   Matched %d blocks (min %d, max %d) of min %d chars for %s and %s",
                                s1.size(), minBlocks, maxBlockCount, requirement.minTextSize,
                                files.get(g1).getName(), files.get(g2).getName()));
                        Set<String> matchingBlocks = new HashSet<>(s1.size());
                        try {
                            for (Alto alto: new Alto[]{new Alto(files.get(g1)), new Alto(files.get(g1))}) {
                                for (String text: getTextBlocks(alto, requirement.minTextSize)) {
                                    if (s1.contains(text.hashCode())) {
                                        matchingBlocks.add(text);
                                    }
                                }
                            }
                            for (String text: matchingBlocks) {
                                System.out.println("#     " + text.replace("\n", " "));
                            }
                        } catch (Exception e) {
                            System.out.println("#     Exception dumping matching text blocks");
                            e.printStackTrace();
                        }
                    }
                } else if (verbose) {
                    System.out.println(String.format(
                            "#   Non-match with %d blocks (min %d, max %d) of min %d chars for %s and %s",
                            s1.size(), minBlocks, maxBlockCount, requirement.minTextSize,
                            files.get(g1).getName(), files.get(g2).getName()));
                }

            }
            if (!keep.isEmpty()) {
                groups.add(keep);
            }
        }

        // Remove all true sub-groups
        for (int g1 = 0 ; g1 < groups.size() ; g1++) {
            for (int g2 = groups.size()-1 ; g2 > g1 ; g2--) {
                if (groups.get(g1).containsAll(groups.get(g2))) {
                    groups.remove(g2);
                }
            }
        }
        return groups;
    }

    private static Map<File, Set<Integer>> reverseMap(Map<Integer, Set<File>> fileToHash) {
        Map<File, Set<Integer>> reverse;
        reverse = new HashMap<>();
        for (Map.Entry<Integer, Set<File>> entry: fileToHash.entrySet()) {
            Set<Integer> hashes;
            for (File f: entry.getValue()) {
                hashes = reverse.get(f);
                if (hashes == null) {
                    hashes = new HashSet<>();
                    reverse.put(f, hashes);
                }
                hashes.add(entry.getKey());
            }
        }
        return reverse;
    }

    private static Map<Integer, Set<File>> extractMoreThanOne(Map<Integer, Set<File>> duplicates) {
        Map<Integer, Set<File>> pruned = new HashMap<>();
        for (Map.Entry<Integer, Set<File>> entry: duplicates.entrySet()) {
            if (entry.getValue().size() > 1) {
                pruned.put(entry.getKey(), entry.getValue());
            }
        }
        return pruned;
    }

    private static List<String> getTextBlocks(Alto alto, int minChars) {
        List<String> texts = new ArrayList<>();
        for (Alto.Page page: alto.getLayout()) {
            for (Alto.TextBlock textBlock: page.getPrintSpace()) {
                String text = textBlock.getAllText();
                if (text.length() >= minChars) {
                    texts.add(text);
                }
            }
        }
        return texts;
    }

    private static List<File> getAltoFiles(File[] sources, String altoSuffix) {
        List<File> altoFiles = new ArrayList<>();
        for (File source: sources) {
            addFiles(altoFiles, source, altoSuffix);
        }
        return altoFiles;
    }

    @SuppressWarnings("ConstantConditions")
    private static void addFiles(List<File> altoFiles, File folder, final String altoSuffix) {
        Collections.addAll(altoFiles, folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(altoSuffix);
            }
        }));
        for (File sub: folder.listFiles()) {
            if (sub.isDirectory()) {
                addFiles(altoFiles, sub, altoSuffix);
            }
        }
    //    System.out.println("Alto count: " + altoFiles.size() + " from " + folder);
    }
}
