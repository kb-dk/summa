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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Layout;
import org.apache.log4j.RollingFileAppender;

import java.io.IOException;

/**
 * Custom rolling file appender implementation for Log4J that rolls
 * the log files each time the JVM starts
 */
@QAInfo(author = "mkamstrup",
        state = QAInfo.State.QA_OK,
        level = QAInfo.Level.NORMAL)
public class CleanSlateRollingFileAppender extends RollingFileAppender {
    /**
     * Creates a clean slate rolling file appender, with not layout and no
     * filename.
     */
    public CleanSlateRollingFileAppender () {
        super();
    }

    /**
     * Creates a clean slate rolling file appender, with a specific layout and
     * to a specific filename.
     * @param layout The layout.
     * @param filename They filename.
     * @throws IOException If error opening file specified.
     */
    public CleanSlateRollingFileAppender(Layout layout, String filename)
                                                            throws IOException {
        super(layout, filename);
    }

    /**
     * Creates a clean slate rolling file appender, with a specific layout and
     * to a specific filename. This appender appends to existing files.
     * @param layout The Layout.
     * @param filename The filename.
     * @param append True iff the appender should append to existing files.
     * @throws IOException If error opening file specified.
     */
    public CleanSlateRollingFileAppender(
            Layout layout, String filename, boolean append) throws IOException {
        super(layout, filename, append);
    }

    /**
     * This calls {@link super#activateOptions()} and den do a
     * {@link super#rollOver}.
     */
    @Override
    public void activateOptions() {
        super.activateOptions();
        rollOver();
    }
}