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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.stream.ArchiveReader;
import dk.statsbiblioteket.summa.support.alto.as2.AS2AltoAnalyzerTest;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoStatFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(AltoStatFilterTest.class);

    public static final String SAMPLE = "/home/te/projects/hvideprogrammer/samples_with_paths.zip";
    public static final String FULL = "/home/te/projects/hvideprogrammer/data/";

    // SB-specific
    public void testHPSampleStats() throws Exception {
        testCaStats(SAMPLE);
    }
    // SB-specific
    public void testHPFullStats() throws Exception {
        testCaStats(FULL);
    }

    public void testCaStats(String source) throws Exception {
        if (!new File(source).exists()) {
            return;
        }
        ObjectFilter feeder = getFeeder(source);
        Configuration conf = Configuration.newMemoryBased(
                AltoStatFilter.CONF_REGEXPS, new String[] {
                "(?:^| )([cC][aA][.] [^ ]+)",
                "(?:^| )([cC][iI][rR][cCkK][aA][.] [^ ]+)"},
                AltoStatFilter.CONF_REPLACEMENTS, new String[] {
                "$1",
                "$1"},
                AltoStatFilter.CONF_LOWERCASE, true,
                AltoStatFilter.CONF_LINEBASED, false
        );
        dumpStats(feeder, conf);
    }

    public void testStartMatch() {
        final String[] INPUTS = new String[]{
                "ca. midnat",
                "den er ca. midnat",
        };

        Pattern pattern = Pattern.compile("(^| )ca[.] [^ ]+");
        for (String input: INPUTS) {
            assertTrue("The pattern " + pattern.pattern() + " should match '" + input + "'",
                       pattern.matcher(input).find());
        }
    }

    public void testBasicStats() throws Exception {
        ObjectFilter feeder = new PayloadFeederHelper(AS2AltoAnalyzerTest.s1795_1, AS2AltoAnalyzerTest.s1846_1);
        Configuration conf = Configuration.newMemoryBased(
                AltoStatFilter.CONF_REGEXPS, "a[a-z]+"
        );
        dumpStats(feeder, conf);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void dumpStats(ObjectFilter feeder, Configuration conf) throws IOException {
        AltoStatFilter stats = new AltoStatFilter(conf);
        stats.setSource(feeder);

        //noinspection StatementWithEmptyBody
        while (stats.pump());

        System.out.println(stats.getStats());
    }

    public ObjectFilter getFeeder(String source) {
        return new ArchiveReader(Configuration.newMemoryBased(
                ArchiveReader.CONF_COMPLETED_POSTFIX, "",
                ArchiveReader.CONF_FILE_PATTERN, ".*.xml",
                ArchiveReader.CONF_RECURSIVE, true,
                ArchiveReader.CONF_ROOT_FOLDER, source
        ));
    }
}
