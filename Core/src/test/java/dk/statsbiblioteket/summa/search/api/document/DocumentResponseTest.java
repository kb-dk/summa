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
package dk.statsbiblioteket.summa.search.api.document;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocumentResponseTest extends TestCase {
    private static Log log = LogFactory.getLog(DocumentResponseTest.class);

    public void testGrouping() {
        DocumentResponse response1 = new DocumentResponse(
                "myfilter", "myquery", 0, 10, null, false, new String[]{"resultField1", "resultField2"}, 123,
                100, "groupField", 2, 2);
        { // Group 1a
            DocumentResponse.Group group = new DocumentResponse.Group("group1a", 2);
            {
                DocumentResponse.Record record1 = new DocumentResponse.Record("record1.1", "source1", 2.0f, "2.0");
                record1.add(new DocumentResponse.Field("field1.1", "content1", false));
                group.add(record1);
            }
            {
                DocumentResponse.Record record1 = new DocumentResponse.Record("record1.2", "source1", 3.0f, "3.0");
                record1.add(new DocumentResponse.Field("field1.2", "content2", false));
                group.add(record1);
            }
            response1.addGroup(group);
        }

        DocumentResponse response2 = new DocumentResponse(
                "myfilter", "myquery", 0, 10, null, false, new String[]{"resultField1", "resultField2"}, 124,
                200, "groupField", 2, 2);
        { // Group 1a
            DocumentResponse.Group group = new DocumentResponse.Group("group1a", 1);
            {
                DocumentResponse.Record record = new DocumentResponse.Record("record2.1", "source2", 5.0f, "5.0");
                record.add(new DocumentResponse.Field("field2.1", "content3", false));
                group.add(record);
            }
            response2.addGroup(group);
        }
        { // Group 1b
            DocumentResponse.Group group = new DocumentResponse.Group("group1b", 2);
            {
                DocumentResponse.Record record = new DocumentResponse.Record("record2b.1", "source2", 11.0f, "11.0");
                record.add(new DocumentResponse.Field("field2b.1", "content5", false));
                group.add(record);
            }
            {
                DocumentResponse.Record record1 = new DocumentResponse.Record("record2b.2", "source2", 13.0f, "13.0");
                record1.add(new DocumentResponse.Field("field2b.2", "content6", false));
                group.add(record1);
            }
            response2.addGroup(group);
        }

        System.out.println("*********************** 1");
        System.out.println(response1.toXML());
        System.out.println("*********************** 2");
        System.out.println(response2.toXML());
        System.out.println("*********************** 1+2");
        response1.merge(response2);
        System.out.println(response1.toXML());

    }
}
