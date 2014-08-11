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

import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoDuplicateTest extends TestCase {
    private static Log log = LogFactory.getLog(AltoDuplicateTest.class);

    // TODO: We do not have license to publish these files. Generate obfuscated test files from them
/*    public static final File[] sources = new File[]{
            new File("/home/te/smb/U/B400026951974-RT1")
//            new File("/home/te/smb/U/B400026952083-RT1/400026952083-10") // Verified 4 duplicate groups
//            new File("/home/te/smb/U/B400026952121-RT1") // Verified at least 2 duplicate groups
            //new File("/home/te/smb/U/B400026952083-RT1/400026952083-13") // Alleged 4 groups
    };*/
    public static final File[] sources = new File[]{
//            new File("/home/te/smb/U/B400026951974-RT1"), // None
//            new File("/home/te/smb/U/B400026951982-RT1"), // None
//            new File("/home/te/smb/U/B400026951990-RT1"), // None
//            new File("/home/te/smb/U/B400026952016-RT1"), // None
//            new File("/home/te/smb/U/B400026952032-RT1"), // None
//            new File("/home/te/smb/U/B400026952040-RT1"), // 27 groups
//            new File("/home/te/smb/U/B400026952059-RT1"), // 24 groups
/*            new File("/home/te/smb/U/B400026952067-RT1"),
            new File("/home/te/smb/U/B400026952075-RT1"),
            new File("/home/te/smb/U/B400026952083-RT1"),
            new File("/home/te/smb/U/B400026952091-RT1"),
            new File("/home/te/smb/U/B400026952105-RT1"),
            new File("/home/te/smb/U/B400026952113-RT1"),
            new File("/home/te/smb/U/B400026952121-RT1"),
            new File("/home/te/smb/U/B400026952131-RT1"),
            new File("/home/te/smb/U/B400026952148-RT1"),
            new File("/home/te/smb/U/B400026952148-RT2"),
            new File("/home/te/smb/U/B400026952148-RT4"),
            new File("/home/te/smb/U/B400026952156-RT1"),
            new File("/home/te/smb/U/B400026955252-RT1"), // None
            new File("/home/te/smb/U/B400026955260-RT1"), // None
            new File("/home/te/smb/U/B400026955287-RT1"), // None
            new File("/home/te/smb/U/B400026955295-RT1"), // None

            new File("/home/te/smb/U/B400026955244-RT1"), // Jun 7*/
            //new File("/home/te/smb/U/B400026952008-RT1"), // None
            /*new File("/home/te/smb/U/B400027043877-RT1"), // None
            */
            //new File("/home/te/smb/U/B400026952024-RT1")
            /*new File("/home/te/smb/U/B400027043796-RT1"), // None
            new File("/home/te/smb/U/B400027043761-RT1"), // None
            new File("/home/te/smb/U/B400027043818-RT1"), // None
            new File("/home/te/smb/U/B400027043788-RT1"), // None
            new File("/home/te/smb/U/B400027043834-RT1"), // None
            new File("/home/te/smb/U/B400027043915-RT1"), // None
            new File("/home/te/smb/U/B400026952148-RT4"), // Maybe duplicates
            new File("/home/te/smb/U/B400026952156-RT1"), // Maybe duplicates
            new File("/home/te/smb/U/B400027043931-RT1"), // None
            new File("/home/te/smb/U/B400027043893-RT1"), // None
            new File("/home/te/smb/U/B400026951771-RT1"), // None
            new File("/home/te/smb/U/B400027043923-RT1"), // None
            new File("/home/te/smb/U/B400027043885-RT1"), // None
            new File("/home/te/smb/U/B400027043907-RT1"), // None
            new File("/home/te/smb/U/B400026951753-RT1"), // None
            new File("/home/te/smb/U/B400026951761-RT1"), // None
            new File("/home/te/smb/U/B400026951796-RT1"), // None
            new File("/home/te/smb/U/B400026951788-RT1")    // 2 duplicates (false positives)*/
//            new File("/home/te/smb/U/B400026951801-RT1"), // 9 groups (false positives)
//            new File("/home/te/smb/U/B400026951818-RT1") , // None
//            new File("/home/te/smb/U/B400026951826-RT1") // 4 groups (false positives)
    };


            //"/home/te/projects/summa-trunk/Core/src/test/resources/support/alto/as2/sample_2_2/B400022028241-RT2/";
            //"/home/te/smb/U/B400026952040-RT1/";
    public static final String ALTO_SUFFIX = ".alto.xml";

    private static final int FIRST_MIN_CHARS = 200;
    private static final int FIRST_MIN_DUPLICATES = 3;

    private static final int SECOND_MIN_CHARS = 100;
    private static final int SECOND_MIN_BLOCK_MATCHES_PER_PAGE = 3;
    private static final double SECOND_MIN_BLOCK_FRACTION_MATCHES_PER_PAGE = 0.5;
    private static final int SECOND_MIN_DUPLICATES = 2;

    public static final int MAX_OUTPUT_GROUPS = 5;
    public static final int MAX_OUTPUT_GROUP_SIZE = 5;

    public AltoDuplicateTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(AltoDuplicateTest.class);
    }

    public void testStandAlone() throws FileNotFoundException, XMLStreamException {
        AltoDuplicateFinder.main(new String[]{
                "-r", "3@200", "50%@10",
                "-x", "/home/te/smb/U/",
                //"-p", "/home/te/smb/U/B400026952040-RT1" // 20 groups
                "-p", "/home/te/smb/U/B400026952121-RT1" // 20 groups
                //"-p", "/home/te/smb/U/B400026952040-RT1/400026952040-06/1959-07-26-01"
        });
    }

    public void testStandAloneFalsePositive() throws FileNotFoundException, XMLStreamException {
        AltoDuplicateFinder.main(new String[]{
                "-r", "3@200", "50%@1",
                "-x", "/home/te/smb/U/",
                "-v",
//                "-p", "/home/te/smb/U/B400026951826-RT1/400026951826-07/1990-06-12-01/"
                "-p", "/home/te/smb/U/B400026952121-RT1/400026952121-07/"
                //"-p", "/home/te/smb/U/B400026952040-RT1/400026952040-06/1959-07-26-01"
        });
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void testDuplicates() throws FileNotFoundException, XMLStreamException {
        if (!sources[0].exists()) {
            System.out.println("The source '" + sources[0] + "' is not accessible");
            return;
        }
        final String DEST = "/media/te/SB_TE/avis/duplicates/sample/";

        for (File source: sources) {
            long startTime = System.nanoTime();
            System.out.println("# Processing " + source);
            List<List<File>> duplicated = findPotentialDuplicates(new File[]{source}, FIRST_MIN_CHARS);
            System.out.println(String.format("\n# Found %d duplicate text blocks from source %s in %s seconds",
                                             duplicated.size(), source, (System.nanoTime()-startTime)/1000000000L));
            System.out.println(String.format(
                    "# Min %d blocks of %d chars, %d blocks of %d chars and %d%% blocks of any chars",
                    FIRST_MIN_DUPLICATES, FIRST_MIN_CHARS, SECOND_MIN_BLOCK_MATCHES_PER_PAGE, SECOND_MIN_CHARS,
                    (int)(SECOND_MIN_BLOCK_FRACTION_MATCHES_PER_PAGE*100)));
            if (duplicated.isEmpty()) {
                continue;
            }

            String sans = source.toString().replace("/home/te/smb/U/", "");
            String superGroup = DEST + sans;

            int dup = 1;
            for (List<File> entries: duplicated) {
                System.out.println("");
                String finalDest = String.format("%s/%02d", superGroup, dup);
                if (dup > MAX_OUTPUT_GROUPS) {
                    System.out.print("# ");
                }
                System.out.println("mkdir -p " + finalDest);
                int entryCount = 1;
                for (File entry: entries) {
                    if (dup > MAX_OUTPUT_GROUPS || entryCount >= MAX_OUTPUT_GROUP_SIZE) {
                        System.out.print("# ");
                    }
                    System.out.println(
                            "cp -n "+ entry.getAbsolutePath().replace(ALTO_SUFFIX, "") + ".* " + finalDest);
                    entryCount++;
                }
                dup++;
//                System.out.println(Strings.join(entries, "\n"));
            }
            System.out.println("");
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private List<List<File>> findPotentialDuplicates(File[] sources, int minChars)
            throws FileNotFoundException, XMLStreamException {
        Map<Integer, Set<File>> duplicates = new HashMap<>();
        List<File> altoFiles = getAltoFiles(sources);
        System.out.print("# Calculating text block hashes from " + altoFiles.size() + " alto files\n# ");
        for(int i = 0 ; i < 99 ; i++) {
            System.out.print(".");
        }
        System.out.print("|\n# ");
        int altoCount = 0;
        for (File altoFile: altoFiles) {
            if (++altoCount % (altoFiles.size() / 100) == 0) {
                System.out.print(".");
            }
            Alto alto = new Alto(altoFile);
            for (String text: getTextBlocks(alto, minChars)) {
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
        System.out.println("");
        return prune(duplicates);
    }

    private List<String> getTextBlocks(Alto alto, int minChars) {
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

    private List<List<File>> prune(Map<Integer, Set<File>> potentialDuplicates)
            throws FileNotFoundException, XMLStreamException {
        System.out.println(
                "# Pruning " + potentialDuplicates.size() + " groups (min group size " + FIRST_MIN_DUPLICATES + ")");
        List<List<File>> minDupReduced = pruneMinDuplicates(potentialDuplicates);
        System.out.println("# Eliminating false positives from " + minDupReduced.size() + " duplicate groups");
        List<List<File>> minBlocksReduced = pruneMinBlocks(minDupReduced);

        return minBlocksReduced;
    }

    private List<List<File>> pruneMinDuplicates(Map<Integer, Set<File>> potentialDuplicates) {
        Map<String, List<File>> pruned = new HashMap<>();
        for (Map.Entry<Integer, Set<File>> entry: potentialDuplicates.entrySet()) {
            if (entry.getValue().size() >= FIRST_MIN_DUPLICATES) {
                List<File> ordered = new ArrayList<>(entry.getValue());
                Collections.sort(ordered);
                // Auto-remove entries with the same value
                pruned.put(Strings.join(ordered), ordered);
            }
        }
        List<List<File>> reduced = new ArrayList<>();
        for (Map.Entry<String, List<File>> entry: pruned.entrySet()) {
            reduced.add(entry.getValue());
        }
        return reduced;
    }

    private List<List<File>> pruneMinBlocks(List<List<File>> groups) throws FileNotFoundException, XMLStreamException {
        for (int i = groups.size()-1 ; i >= 0 ; i--) {
            if (countConnectedAltos(groups.get(i)) < SECOND_MIN_DUPLICATES) {
                groups.remove(i);
            }
        }
        return groups;
    }

    // Count total number of connected Altos
    private int countConnectedAltos(List<File> files) throws FileNotFoundException, XMLStreamException {
        List<Alto> altos = new ArrayList<>(files.size());
        for (File file: files) {
            altos.add(new Alto(file));
        }

        int overallMatchCount = 0;
        for (Alto primary: altos) {
            int matchCount = 0;
            for (Alto secondary: altos) {
                if (primary != secondary && connectedMatches(primary, secondary)) {
                    matchCount++;
                }
            }
            if (matchCount >= SECOND_MIN_DUPLICATES) {
                overallMatchCount++;
            }
        }
        return overallMatchCount;
    }

    // Count matching minBlocks between two Altos
    private boolean connectedMatches(Alto primary, Alto secondary) {
        int largeMatches = 0;
        int allMatches = 0;
        List<String> pBlocks = getTextBlocks(primary, 1);
        for (String pBlock: pBlocks) {
            for (String sBlock: getTextBlocks(secondary, 1)) {
                if (pBlock.equals(sBlock)) {
                    if (pBlock.length() >= SECOND_MIN_CHARS) {
                        largeMatches++;
                    }
                    allMatches++;
                }
            }

        }

        return largeMatches >= SECOND_MIN_BLOCK_MATCHES_PER_PAGE
               && allMatches >= pBlocks.size() * SECOND_MIN_BLOCK_FRACTION_MATCHES_PER_PAGE;
    }

    public void testGetAltos() throws XMLStreamException, FileNotFoundException {
        if (!sources[0].exists()) {
            return;
        }
        List<File> altoFiles = getAltoFiles();
        assertFalse("There should be at least 1 ALTO file", altoFiles.isEmpty());
        log.info("There is " + altoFiles.size() + " ALTO files in " + sources.length + " sources");
    }

    private List<File> getAltoFiles() {
        return getAltoFiles(sources);
    }
    private List<File> getAltoFiles(File[] sources) {
        List<File> altoFiles = new ArrayList<>();
        for (File source: sources) {
            addFiles(altoFiles, source);
        }
        return altoFiles;
    }

    @SuppressWarnings("ConstantConditions")
    private void addFiles(List<File> altoFiles, File folder) {
        Collections.addAll(altoFiles, folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(ALTO_SUFFIX);
            }
        }));
        for (File sub: folder.listFiles()) {
            if (sub.isDirectory()) {
                addFiles(altoFiles, sub);
            }
        }
    //    System.out.println("Alto count: " + altoFiles.size() + " from " + folder);
    }
}
