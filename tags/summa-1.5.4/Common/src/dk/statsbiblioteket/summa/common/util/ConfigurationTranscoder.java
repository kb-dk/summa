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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.JStorage;

/**
 * Helper class to read the default system configuration, as obtained by
 * {@link Configuration#getSystemConfiguration()}, and dump it as a JStorage.
 * <p/>
 * This tool should be expanded to include arbitrary configuration formats,
 * but there is not canonical way to serialize configurations as of writing.
 */
public class ConfigurationTranscoder {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please provide exactly one configuration"
                               + " to transcode");
            System.exit(-1);
        }
        
        Configuration from = Configuration.load(args[0]);
        Configuration to = new Configuration(new JStorage());
        to.importConfiguration(from);
        System.out.println(to.getStorage().toString());
    }
}

