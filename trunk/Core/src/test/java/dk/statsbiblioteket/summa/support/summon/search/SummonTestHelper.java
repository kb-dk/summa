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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.SimplePair;
import dk.statsbiblioteket.summa.search.PagingSearchNode;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonTestHelper {
    private static Log log = LogFactory.getLog(SummonTestHelper.class);

    private static final File SECRET = new File(System.getProperty("user.home") + "/summon-credentials.dat");

    public static SummonSearchNode createSummonSearchNode() {
        return createSummonSearchNode(SummonResponseBuilder.DEFAULT_SHORT_DATE);
    }

    public static SearchNode createPagingSummonSearchNode() {
        SummonSearchNode summonNode = createSummonSearchNode(SummonResponseBuilder.DEFAULT_SHORT_DATE);
        Configuration conf = Configuration.newMemoryBased(
                PagingSearchNode.CONF_SEQUENTIAL, false,
                PagingSearchNode.CONF_MAXPAGESIZE, 50
        );
        return new PagingSearchNode(conf, summonNode);
    }

    public static SummonSearchNode createSummonSearchNode(boolean useShortDate) {
        SummonSearchNode summon;
        try {
            Configuration conf = getDefaultSummonConfiguration();
            conf.set(SummonResponseBuilder.CONF_SHORT_DATE, useShortDate);

            summon = new SummonSearchNode(conf);
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect to summon", e);
        }
        log.info("Created SummonSearchNode with credentials from " + SECRET);
        return summon;
    }

    public static Configuration getDefaultSummonConfiguration() {
        SimplePair<String, String> credentials = getCredentials();
        return Configuration.newMemoryBased(
                SummonSearchNode.CONF_SUMMON_ACCESSID, credentials.getKey(),
                SummonSearchNode.CONF_SUMMON_ACCESSKEY, credentials.getValue()
        );
    }

    public static SimplePair<String, String> getCredentials() {
        String id;
        String key;
        if (!SECRET.exists()) {
            throw new IllegalStateException(
                "The file '" + SECRET.getAbsolutePath() + "' must exist and "
                + "contain two lines, the first being access ID, the second"
                + "being access key for the Summon API");
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(SECRET), "utf-8"));
            id = br.readLine();
            key = br.readLine();
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to resolve summon credentials from " + SECRET);
        }
        log.debug("Loaded credentials from " + SECRET);
        return new SimplePair<>(id, key);
    }
}
