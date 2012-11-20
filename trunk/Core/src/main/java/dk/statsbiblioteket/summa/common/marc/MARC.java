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
package dk.statsbiblioteket.summa.common.marc;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * MARC21Slim constants etc.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MARC {

    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    public static final String ATTRIBUTE_ID = "id";

    public static final String TAG_RECORD = "record";
    public static final String TAG_RECORD_ATTRIBUTE_TYPE = "type";

    public static final String TAG_LEADER = "leader";

    public static final String TAG_DATAFIELD = "datafield";
    public static final String TAG_DATAFIELD_ATTRIBUTE_TAG = "tag";
    public static final String TAG_DATAFIELD_ATTRIBUTE_IND1 = "ind1";
    public static final String TAG_DATAFIELD_ATTRIBUTE_IND2 = "ind2";

    public static final String TAG_SUBFIELD = "subfield";
    public static final String TAG_SUBFIELD_ATTRIBUTE_CODE = "code";

    public static final String TAG_CONTROLFIELD = "controlfield";
    public static final String TAG_CONTROLFIELD_ATTRIBUTE_TAG = "tag";
}
