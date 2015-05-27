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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.search.api.*;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocumentResponseTest extends TestCase {

    // Invalid test outside of SB
    public void testRemote() throws IOException {
        final String ADDRESS = "//mars:56700/aviser-searcher";
        SummaSearcher searcher = new SearchClient(Configuration.newMemoryBased(
                SearchClient.CONF_SERVER, ADDRESS));
        ResponseCollection responses = searcher.search(new Request(DocumentSearcher.SEARCH_QUERY, "hest"));
        assertFalse("There should be at least one response from " + ADDRESS, responses.isEmpty());

        List<String> groupsAll = getGroups(searcher.search(new Request(
                DocumentSearcher.SEARCH_QUERY, "hest",
                DocumentSearcher.GROUP, true,
                DocumentSearcher.GROUP_FIELD, "editionUUID",
                DocumentSearcher.SEARCH_MAX_RECORDS, 6
        )));

        List<String> groupsFirst3 = getGroups(searcher.search(new Request(
                DocumentSearcher.SEARCH_QUERY, "hest",
                DocumentSearcher.GROUP, true,
                DocumentSearcher.GROUP_FIELD, "editionUUID",
                DocumentSearcher.SEARCH_MAX_RECORDS, 3
        )));
        List<String> groupsSecond3 = getGroups(searcher.search(new Request(
                DocumentSearcher.SEARCH_QUERY, "hest",
                DocumentSearcher.GROUP, true,
                DocumentSearcher.GROUP_FIELD, "editionUUID",
                DocumentSearcher.SEARCH_MAX_RECORDS, 3,
                DocumentSearcher.SEARCH_START_INDEX, 2
        )));
        assertEquals("All-group should contain the right number of groups", 6, groupsAll.size());
        ExtraAsserts.assertEquals("The first 3 groups should match those from the all-group",
                                  groupsAll.subList(0, 3), groupsFirst3);
        ExtraAsserts.assertEquals("The second 3 groups should match those from the all-group",
                                  groupsAll.subList(3, 6), groupsSecond3);

        searcher.close();
    }

    // Invalid test outside of SB
    public void testRemoteMars() throws IOException {
        final String ADDRESS = "//mars:56700/aviser-searcher";
        SummaSearcher searcher = new SearchClient(Configuration.newMemoryBased(
                SearchClient.CONF_SERVER, ADDRESS));
        ResponseCollection responses = searcher.search(new Request(
                DocumentSearcher.SEARCH_QUERY, "*:*",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
                FacetKeys.SEARCH_FACET_FACETS, "timestamp (1500 ALPHA)"));
        assertFalse("There should be at least one response from " + ADDRESS, responses.isEmpty());

        String result = responses.toXML().replace("\n", " ");
        Matcher outer = Pattern.compile("<facetmodel.*./facetmodel").matcher(result);
        assertTrue("There should be a facet response\n" + responses.toXML(), outer.find());
        Matcher tagM = Pattern.compile("<tag[^>]+").matcher(outer.group());
        List<String> tags = new ArrayList<>();
        while (tagM.find()) {
            tags.add(tagM.group());
        }
        assertTrue("There should be at least 205 facet entries, but there were only " + tags.size() + "\n"
                   + responses.toXML(),
                   tags.size() > 205);
        System.out.println("There were " + tags.size() + " facet tags");
        searcher.close();
    }

    private List<String> getGroups(ResponseCollection responses) {
        List<String> groups = new ArrayList<>();
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                for (DocumentResponse.Group group: ((DocumentResponse)response).getGroups()) {
                    groups.add(group.getGroupValue() + "(" + group.getNumFound() + ")");
                }
            }
        }
        return groups;
    }

    public void testGrouping() {
        DocumentResponse response1 = getDocumentResponse1(null, null);
        DocumentResponse response2 = getDocumentResponse2(null, null);

        assertGroupOrder("Response 1, null sorters", new String[][]{{"c", "b"}}, response1);
        assertGroupOrder("Response 2, null sorters", new String[][]{{"a"}, {"e", "d"}}, response2);

        response1.merge(response2);
        assertGroupOrder("Merged, null sorters", new String[][]{{"a", "c"}, {"e", "d"}}, response1);
    }

    public void testGroupSortingMissing() {
        final String SORT_KEY = "basesortfield";
        final String GROUP_SORT = "basesortfield desc";
        DocumentResponse response1 = getDocumentResponse1(SORT_KEY, GROUP_SORT);
        DocumentResponse responseEmpty = getDocumentResponseEmpty(SORT_KEY, GROUP_SORT);
        response1.merge(responseEmpty);
        System.out.println(response1.toXML());
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
                Arrays.asList("myfilter"), "myquery", 0, 10, sortKey, false,
                new String[]{"resultField1", "resultField2"}, 123, 100, "groupField", 2, 2, groupSort);
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
                Arrays.asList("myfilter"), "myquery", 0, 10, sortKey, false, new String[]{"resultField1", "resultField2"},
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

    private DocumentResponse getDocumentResponseEmpty(String sortKey, String groupSort) {
        DocumentResponse response = new DocumentResponse(
                Arrays.asList("myfilter"), "myquery", 0, 10, sortKey, false,
                new String[]{"resultField1", "resultField2"}, 124, 200, "groupField", 2, 2, groupSort);
        { // Group 1a
            DocumentResponse.Group group = response.createAndAddGroup("", 1);
            {
                DocumentResponse.Record record = new DocumentResponse.Record(
                        "record_e", "sourceE", 17.0f, "");
                record.add(new DocumentResponse.Field("fieldE.1", "contentE", false));
//                record.add(new DocumentResponse.Field("basesortfield", "a", false));
                group.add(record);
            }
        }
        response.sort();
        return response;
    }

}
