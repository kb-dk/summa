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
package dk.statsbiblioteket.summa.support.enrich;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.filter.object.RecordShaperFilter;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.filter.RecordWriter;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RelationResolverTest extends TestCase {
    public RelationResolverTest(String name) {
        super(name);
    }

    private File STORAGE = new File("RelationResolverTest/storage");
    private File NONMATCHED =
        new File("RelationResolverTest/relations/nonmatched");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (STORAGE.exists()) {
            Files.delete(STORAGE);
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (STORAGE.exists()) {
            Files.delete(STORAGE);
        }
        if (NONMATCHED.exists()) {
            Files.delete(NONMATCHED);
        }
    }

    public static Test suite() {
        return new TestSuite(RelationResolverTest.class);
    }

    public void testBasicEnrichment() throws IOException {
        Record parentR = new Record("parent1", "foo", new byte[0]);
        Record enricherR = new Record("enricher1", "foo", new byte[0]);
        enricherR.getMeta().put("SearchTerm", "parent1");

        Configuration resolverConf = Configuration.newMemoryBased(
            RelationResolver.CONF_ASSIGN_PARENTS, true,
            RelationResolver.CONF_SEARCH_FIELD, "id",
            RelationResolver.CONF_SEARCH_MAXHITS, 1,
            RelationResolver.CONF_SEARCH_METAKEYS,
            new ArrayList<String>(Arrays.asList("SearchTerm")),
            RelationResolver.CONF_NONMATCHED_FOLDER,
            NONMATCHED.getAbsolutePath(),
            ConnectionConsumer.CONF_RPC_TARGET, "NotUsed"
        );

        Storage storage = StorageFactory.createStorage(
            Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, STORAGE.getAbsolutePath()
        ));
        storage.flush(parentR);
        assertNotNull("The parent Record should be stored",
                      storage.getRecord(parentR.getId(), null));

        Filter feeder = new PayloadFeederHelper(Arrays.asList(
            new Payload(enricherR)));
        ObjectFilter resolver = new RelationResolver(resolverConf) {
            @Override
            protected SummaSearcher createSearchClient(Configuration conf) {
                return null;
            }

            @Override
            protected DocumentResponse getHits(
                Payload payload, String searchValue) throws PayloadException {
                return  createResponse(searchValue, searchValue);
            }
        };
        resolver.setSource(feeder);

        assertTrue("The RelationResolver should have a Payload",
                   resolver.hasNext());
        Payload enriched = resolver.next();
        assertNotNull("The enriched " + enriched.getRecord()
                      + " should have parents",
                      enriched.getRecord().getParentIds());
        assertEquals("The enriched Payload should have the right parent",
                     "parent1", enriched.getRecord().getParentIds().get(0));

        storage.flush(enriched.getRecord());

        Record finalParent = storage.getRecord(
            "parent1", new QueryOptions(null, null, 10, 0));
        assertNotNull("The final parent should have a child",
                      finalParent.getChildren());
        assertEquals("The final parent should have the right child",
                     "enricher1", finalParent.getChildren().get(0).getId());
        storage.close();
    }

    private static final String TEI =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\n"
        + "  <teiHeader>\n"
        + "    <fileDesc>\n"
        + "      <titleStmt>\n"
        + "          <title>My fine title</title>\n"
        + "          <editor>Redigeret af Foo Bar Holger Dolker</editor>\n"
        + "      </titleStmt>\n"
        + "      <publicationStmt>\n"
        + "          <idno type=\"ISBN\">12-3456-789-0</idno>\n"
        + "          <publisher>Fake Forlag</publisher>\n"
        + "          <date when=\"2011\">2011</date>\n"
        + "      </publicationStmt>\n"
        + "      <sourceDesc>\n"
        + "        <p></p>\n"
        + "      </sourceDesc>\n"
        + "    </fileDesc>\n"
        + "  </teiHeader>\n"
        + "<text>\n"
        + "    <front>\n"
        + "            <titlePage>\n"
        + "                <docTitle>\n"
        + "                    <titlePart type=\"main\">Min fine titel</titlePart>\n"
        + "                </docTitle>\n"
        + "                <docImprint>\n"
        + "                    <pubPlace>Fake Forlag</pubPlace>\n"
        + "                    <docDate>201</docDate>\n"
        + "                </docImprint>\n"
        + "            </titlePage>\n"
        + "    </front>\n"
        + "<body>\n"
        + "<div>\n"
        + "  <pb n=\"4\"/>\n"
        + "  <p>Lorem Ipsum and all that</p>\n"
        + "</div>\n";

    private static final String TEI13 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "      <publicationStmt>\n"
        + "          <idno type=\"ISBN\">12-3456-789-0-123</idno>\n"
        + "</div>\n";

    @SuppressWarnings({"FieldCanBeLocal"})
    private final String CONTENT10 = "(?s).*<publicationStmt.*?>.+?<idno.+?type=\"ISBN\">[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*?</idno>.*";
    @SuppressWarnings({"FieldCanBeLocal"})
    private final String CONTENT13 = "(?s).*<publicationStmt.*?>.+?<idno.+?type=\"ISBN\">[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*?([0-9])[^0-9]*([0-9])[^0-9]*([0-9])[^0-9]*?</idno>.*";
    @SuppressWarnings({"FieldCanBeLocal"})
    private final String ORIGIN10 = ".*[^0-9/]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*[^/]*$";
    @SuppressWarnings({"FieldCanBeLocal"})
    private final String ORIGIN13 = ".*[^0-9/]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*([0-9])[^0-9/a-zA-Z]*[^/]*$";
    private final String GROUP10 = "$1$2$3$4$5$6$7$8$9$10";
    private final String GROUP13 = "$1$2$3$4$5$6$7$8$9$10$11$12$13";

    public void testRegexprs() {
        testRegexprs(CONTENT10, GROUP10, TEI, "1234567890");
        testRegexprs(CONTENT13, GROUP13, TEI13, "1234567890123");
        testRegexprs(ORIGIN10, GROUP10,
                     "somefolder/somezip7!/1234 567 890 - Svamperiget 876.xml",
                     "1234567890");
        testRegexprs(ORIGIN13, GROUP13,
                     "somefolder/somezip7!/1234 567 890-123 - Svamperiget 876.xml",
                     "1234567890123");
    }
    public void testRegexprs(
        String regexp, String template, String input, String expected) {
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(input);
        assertTrue("The pattern '" + regexp + "' should match input '"
                   + input + "'",
                   matcher.matches());
        StringBuffer buffer = new StringBuffer(50);
        int matchPos = matcher.start();
        matcher.appendReplacement(buffer, template);
        String actual = buffer.toString().substring(matchPos);
        assertEquals("The result from pattern '" + regexp + "' and input '"
                     + input + "' should be as expected",
                     expected, actual);
    }

    /*
    A near full-chain test of an enrichment workflow
     */
    public void testShapeEnrichIngest() throws IOException {
        Record parentR1 = new Record("internal_id1", "foo", new byte[0]);
        Record parentR2 = new Record("internal_id2", "foo", new byte[0]);
        Record parentR3 = new Record("internal_id3", "foo", new byte[0]);
        Record teiR1 = new Record("TEIRecord1", "bar", TEI.getBytes("utf-8"));
        Record teiR2 = new Record("TEIRecord2", "bar", TEI13.getBytes("utf-8"));

        Configuration shaperConf = Configuration.newMemoryBased();
        shaperConf.set(RecordShaperFilter.CONF_DISCARD_ON_ERRORS, true);
        shaperConf.set(RecordShaperFilter.CONF_META_REQUIREMENT, "one");
        shaperConf.set(RecordShaperFilter.CONF_COPY_META, true);
        List<Configuration> metaConfs = shaperConf.createSubConfigurations(
            RecordShaperFilter.CONF_META, 5);

        Configuration conf10Content = metaConfs.get(0);
        conf10Content.set(RecordShaperFilter.CONF_META_SOURCE,
                          RecordUtil.PART_CONTENT);
        conf10Content.set(RecordShaperFilter.CONF_META_KEY,
                          RecordUtil.PART_META_PREFIX + "isbn10");
        conf10Content.set(RecordShaperFilter.CONF_META_REGEXP, CONTENT10);
        conf10Content.set(RecordShaperFilter.CONF_META_TEMPLATE, GROUP10);

        Configuration conf13Content = metaConfs.get(1);
        conf13Content.set(RecordShaperFilter.CONF_META_SOURCE,
                          RecordUtil.PART_CONTENT);
        conf13Content.set(RecordShaperFilter.CONF_META_KEY,
                          RecordUtil.PART_META_PREFIX + "isbn13");
        conf13Content.set(RecordShaperFilter.CONF_META_REGEXP, CONTENT13);
        conf13Content.set(RecordShaperFilter.CONF_META_TEMPLATE, GROUP13);

        Configuration conf10Origin = metaConfs.get(2);
        conf10Origin.set(RecordShaperFilter.CONF_META_SOURCE,
                         RecordUtil.PART_META_PREFIX + Payload.ORIGIN);
        conf10Origin.set(RecordShaperFilter.CONF_META_KEY,
                         RecordUtil.PART_META_PREFIX + "isbn10origin");
        conf10Origin.set(RecordShaperFilter.CONF_META_REGEXP, ORIGIN10);
        conf10Origin.set(RecordShaperFilter.CONF_META_TEMPLATE, GROUP10);

        Configuration conf13Origin = metaConfs.get(3);
        conf13Origin.set(RecordShaperFilter.CONF_META_SOURCE,
                         RecordUtil.PART_META_PREFIX + Payload.ORIGIN);
        conf13Origin.set(RecordShaperFilter.CONF_META_KEY,
                         RecordUtil.PART_META_PREFIX + "isbn10origin");
        conf13Origin.set(RecordShaperFilter.CONF_META_REGEXP, ORIGIN13);
        conf13Origin.set(RecordShaperFilter.CONF_META_TEMPLATE, GROUP13);

        Configuration confUnmatching = metaConfs.get(4);
        confUnmatching.set(RecordShaperFilter.CONF_META_SOURCE,
                         RecordUtil.PART_META_PREFIX + Payload.ORIGIN);
        confUnmatching.set(RecordShaperFilter.CONF_META_KEY,
                         RecordUtil.PART_META_PREFIX + "nonmatching");
        confUnmatching.set(RecordShaperFilter.CONF_META_REGEXP, "nomatch");
        confUnmatching.set(RecordShaperFilter.CONF_META_TEMPLATE, "$0");

        Configuration resolverConf = Configuration.newMemoryBased(
            RelationResolver.CONF_ASSIGN_PARENTS, true,
            RelationResolver.CONF_SEARCH_FIELD, "id",
            RelationResolver.CONF_SEARCH_MAXHITS, 10,
            RelationResolver.CONF_NONMATCHED_FOLDER,
            NONMATCHED.getAbsolutePath(),
            RelationResolver.CONF_SEARCH_METAKEYS,
            new ArrayList<String>(Arrays.asList(
                "isbn10", "isbn13", "isbn10origin", "isbn13")),
            ConnectionConsumer.CONF_RPC_TARGET, "NotUsed"
        );

        Configuration storageConf = Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, STORAGE.getAbsolutePath());


        Storage storage = StorageFactory.createStorage(storageConf);
        storage.flush(parentR1);
        storage.flush(parentR2);
        storage.flush(parentR3);
        assertNotNull("The parent Records should be stored",
                      storage.getRecord(parentR1.getId(), null));

        Payload teiP1 = new Payload(teiR1);
        teiP1.getData().put(
            Payload.ORIGIN,
            "somefolder/somezip7!/09876 543 21 - Svamperiget 8.xml");
        Payload teiP2 = new Payload(teiR2);
        teiP2.getData().put(
            Payload.ORIGIN,
            "somefolder/somezip7!/09876 543 21 - 098 - Svamperiget 8.xml");

        ObjectFilter feeder = new PayloadFeederHelper(Arrays.asList(
            teiP1, teiP2));
        ObjectFilter shaper = new RecordShaperFilter(shaperConf);
        shaper.setSource(feeder);
        ObjectFilter resolver = new RelationResolver(resolverConf) {
            @Override
            protected SummaSearcher createSearchClient(Configuration conf) {
                return null;
            }

            @Override
            protected DocumentResponse getHits(
                Payload payload, String searchValue) throws PayloadException {
                DocumentResponse response;
                if (searchValue.equals("0987654321")) {
                    response = createResponse(
                        searchValue, "internal_id1");
                } else if (searchValue.equals("1234567890")) {
                    response = createResponse(
                        searchValue, "internal_id2", "internal_id3");
                } else if (searchValue.equals("0987654321098")) {
                    response = createResponse(
                        searchValue, "internal_id1");
                } else if (searchValue.equals("1234567890123")) {
                    response = createResponse(
                        searchValue, "internal_id2");
                } else {
                    response = new DocumentResponse(
                        null, searchValue, 0, 2, "LUCENE", false,
                        new String[]{DocumentKeys.RECORD_ID}, 1, 0);
                }
                return response;
            }
        };
        resolver.setSource(shaper);
        ObjectFilter writer = new RecordWriter(storage, 100, 10000);
        writer.setSource(resolver);

        int counter = 1;
        while (writer.pump()) {
            counter++;
        }
        assertEquals(
            "The correct number of Records should be pulled through the chain",
            2, counter);
        writer.close(true);

        Record enriched1 = storage.getRecord(
            teiR1.getId(), new QueryOptions(null, null, 10, 10));
        assertNotNull("The enriching Record1 should be stored", enriched1);
        assertNotNull("The enriching Record1 should have parents",
                      enriched1.getParents());
        assertEquals("The enriching Record1 should have the right parent count",
                     3, enriched1.getParents().size());

        Record enriched2 = storage.getRecord(
            teiR2.getId(), new QueryOptions(null, null, 10, 10));
        assertNotNull("The enriching Record2 should be stored", enriched2);
        assertNotNull("The enriching Record2 should have parents",
                     enriched2.getParents());
        assertEquals("The enriching Record2 should have the right parent count",
                     2, enriched2.getParents().size());
        storage.close();
    }

    private DocumentResponse createResponse(String searchValue, String... ids) {
        DocumentResponse response = new DocumentResponse(
            null, searchValue, 0, 2, "LUCENE", false,
            new String[]{DocumentKeys.RECORD_ID}, 1, ids.length);
        int counter = 0;
        for (String id: ids) {
            DocumentResponse.Record record = new DocumentResponse.Record(
                Integer.toString(counter++), "foo", 1.0f, "");
            record.addField(new DocumentResponse.Field(
                DocumentKeys.RECORD_ID, id, false));
            response.addRecord(record);
        }
        return response;
    }

    /*
    This is not a proper unit-test. For all practical purposes it will only work
    for Toke Eskildsen at Statsbiblioteket after a specific setup.
    // TODO: Create a proper test with a test-searcher
     */
    public void testSearcherConnection() throws IOException {
        final String EXPECTED = "sb_2257916";
        Record enricherR = new Record("enricher1", "foo", new byte[0]);
        enricherR.getMeta().put("isbn10", "8759308656");

        Configuration resolverConf = Configuration.newMemoryBased(
            RelationResolver.CONF_ASSIGN_PARENTS, true,
            RelationResolver.CONF_SEARCH_FIELD, "isbn",
            RelationResolver.CONF_SEARCH_MAXHITS, 1,
            RelationResolver.CONF_NONMATCHED_FOLDER,
            NONMATCHED.getAbsolutePath(),
            RelationResolver.CONF_SEARCH_METAKEYS,
            new ArrayList<String>(Arrays.asList("isbn10")),
            ConnectionConsumer.CONF_RPC_TARGET,
            //"//prod-search01:55000/sb-searcher"
            "//localhost:55000/sb-searcher"
        );

        Filter feeder = new PayloadFeederHelper(Arrays.asList(
            new Payload(enricherR)));
        ObjectFilter resolver = new RelationResolver(resolverConf);
        resolver.setSource(feeder);

        assertTrue("The RelationResolver should provide at least one Payload",
                   resolver.hasNext());
        Payload enriched = resolver.next();
        assertNotNull("The enriched " + enriched.getRecord()
                      + " should have parents",
                      enriched.getRecord().getParentIds());
        assertEquals("The enriched Payload should have the right parent",
                     EXPECTED, enriched.getRecord().getParentIds().get(0));
    }

}
