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

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocumentResponseTest extends TestCase {
    private static Log log = LogFactory.getLog(DocumentResponseTest.class);

    public void testGrouping() {
        DocumentResponse response1 = getDocumentResponse1(null, null);
        DocumentResponse response2 = getDocumentResponse2(null, null);

        assertGroupOrder("Response 1, null sorters", new String[][]{{"c", "b"}}, response1);
        assertGroupOrder("Response 2, null sorters", new String[][]{{"a"}, {"e", "d"}}, response2);

        response1.merge(response2);
        assertGroupOrder("Merged, null sorters", new String[][]{{"a", "c"}, {"e", "d"}}, response1);
    }

    public void testGroupSortingReverse() {
        final String SORT_KEY = "basesortfield";
        final String GROUP_SORT = "basesortfield desc";
        DocumentResponse response1 = getDocumentResponse1(SORT_KEY, GROUP_SORT);
        DocumentResponse response2 = getDocumentResponse2(SORT_KEY, GROUP_SORT);
        response1.merge(response2);

        assertGroupOrder("basesortfield asc, basesortfield desc",
                         new String[][]{{"c", "b"}, {"e", "d"}}, response1);
    }

    public void testGroupSortingEqual() {
        final String SORT_KEY = "basesortfield";
        final String GROUP_SORT = "basesortfield asc";
        DocumentResponse response1 = getDocumentResponse1(SORT_KEY, GROUP_SORT);
        DocumentResponse response2 = getDocumentResponse2(SORT_KEY, GROUP_SORT);
        response1.merge(response2);

        assertGroupOrder("basesortfield asc, basesortfield asc",
                         new String[][]{{"a", "b"}, {"d", "e"}}, response1);
    }

    private void assertGroupOrder(String message, String[][] expectedOrder, DocumentResponse response) {
        for (int g = 0 ; g < response.getGroups().size() ; g++) {
            String[] expectedGroup = expectedOrder[g];
            for (int r = 0 ; r < response.getGroups().get(g).size() ; r++) {
                DocumentResponse.Record record = response.getGroups().get(g).get(r);
                assertEquals(message + ". Group " + g + ", record " + r + " should have the expected ID\n"
                             + response.toXML(),
                             "record_" + expectedGroup[r], record.getId());
            }
        }
    }

    private DocumentResponse getDocumentResponse1(String sortKey, String groupSort) {
        DocumentResponse response = new DocumentResponse(
                "myfilter", "myquery", 0, 10, sortKey, false, new String[]{"resultField1", "resultField2"},
                123, 100, "groupField", 2, 2, groupSort);
        { // Group 1a
            DocumentResponse.Group group = response.createAndAddGroup("group1a", 2);
            {
                DocumentResponse.Record record = new DocumentResponse.Record(
                        "record_b", "source1", 2.0f, sortKey == null ? "2.0" : "b");
                record.add(new DocumentResponse.Field("field1.1", "content1", false));
                record.add(new DocumentResponse.Field("basesortfield", "b", false));
                group.add(record);
            }
            {
                DocumentResponse.Record record = new DocumentResponse.Record(
                        "record_c", "source1", 3.0f, sortKey == null ? "3.0" : "c");
                record.add(new DocumentResponse.Field("field1.2", "content2", false));
                record.add(new DocumentResponse.Field("basesortfield", "c", false));
                group.add(record);
            }
        }
        response.sort();
        return response;
    }

    private DocumentResponse getDocumentResponse2(String sortKey, String groupSort) {
        DocumentResponse response = new DocumentResponse(
                "myfilter", "myquery", 0, 10, sortKey, false, new String[]{"resultField1", "resultField2"},
                124, 200, "groupField", 2, 2, groupSort
        );
        { // Group 1a
            DocumentResponse.Group group = response.createAndAddGroup("group1a", 1);
            {
                DocumentResponse.Record record = new DocumentResponse.Record(
                        "record_a", "source2", 5.0f, sortKey == null ? "5.0" : "a");
                record.add(new DocumentResponse.Field("field2.1", "content3", false));
                record.add(new DocumentResponse.Field("basesortfield", "a", false));
                group.add(record);
            }
        }
        { // Group 1b
            DocumentResponse.Group group = response.createAndAddGroup("group1b", 2);
            {
                DocumentResponse.Record record = new DocumentResponse.Record(
                        "record_d", "source2", 11.0f, sortKey == null ? "11.0" : "d");
                record.add(new DocumentResponse.Field("field2b.1", "content5", false));
                record.add(new DocumentResponse.Field("basesortfield", "d", false));
                group.add(record);
            }
            {
                DocumentResponse.Record record = new DocumentResponse.Record(
                        "record_e", "source2", 13.0f, sortKey == null ? "13.0" : "e");
                record.add(new DocumentResponse.Field("field2b.2", "content6", false));
                record.add(new DocumentResponse.Field("basesortfield", "e", false));
                group.add(record);
            }
        }
        response.sort();
        return response;
    }
}
