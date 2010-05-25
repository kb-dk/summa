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
package dk.statsbiblioteket.summa.common;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This class is auto generated by ant, when releasing a new version of Summa.
 * NOTE: Auto generated DO NOT EDIT
 * @author Henrik Bitsch Kirk <mailto:hbk@statsbiblioteket.dk>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hbk")
public class SummaConstants {
    /**
     * Current Summa version,
     * Note: Auto generated.
     */
    public static final String SUMMAVERSION = "1.5.5";

    /**
     * Last revision for Summa release,
     * Note: Auto generated.
     */
    public static final int SUMMAVERSIONREVISION = 2340;

    /**
     * Get version string, used to present the Summa version and the revision
     * number it comes from.
     *
     * @return a string representation of summa version and revision.
     */
    public static String getVersion() {
        return "Summa version: " + SUMMAVERSION + ", Revision: "
                + SUMMAVERSIONREVISION;
    }
}
