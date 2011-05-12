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

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RelationResolverTest extends TestCase {
    public RelationResolverTest(String name) {
        super(name);
    }

    private File STORAGE = new File("RelationResolverTest.storage");

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
                DocumentResponse response = new DocumentResponse(
                    null, searchValue, 0, 10, "LUCENE", false,
                    new String[]{DocumentKeys.RECORD_ID}, 1, 1);
                response.addRecord(new DocumentResponse.Record(
                    searchValue, "na", 1.0f, ""));
                return response;
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
    /*
    A near full-chain test of an enrichment workflow
     */
    public void testShapeEnrichIngest() throws IOException {
        Record parentR1 = new Record("internal_id1", "foo", new byte[0]);
        Record parentR2 = new Record("internal_id2", "foo", new byte[0]);
        Record parentR3 = new Record("internal_id3", "foo", new byte[0]);
        Record teiR = new Record("TEIRecord1", "bar", TEI.getBytes("utf-8"));

        Configuration shaperConf = Configuration.newMemoryBased();
        List<Configuration> metaConfs = shaperConf.createSubConfigurations(
            RecordShaperFilter.CONF_META, 1);
        Configuration metaConfContent = metaConfs.get(0); 
        metaConfContent.set(RecordShaperFilter.CONF_META_SOURCE,
                     RecordShaperFilter.PART.content);
        metaConfContent.set(RecordShaperFilter.CONF_META_KEY,
                     RecordShaperFilter.META_PREFIX + "isbn");
        metaConfContent.set(RecordShaperFilter.CONF_META_REGEXP,
                     "(?s)<publicationStmt.*?>.+?<idno.+?type=\"ISBN\">(.+?)</idno>");
        metaConfContent.set(RecordShaperFilter.CONF_META_TEMPLATE, "$1");
        Configuration metaConfOrigin = metaConfs.get(0); 
        metaConfOrigin.set(RecordShaperFilter.CONF_META_SOURCE,
                     RecordShaperFilter.META_PREFIX + Payload.ORIGIN);
        metaConfOrigin.set(RecordShaperFilter.CONF_META_KEY,
                     RecordShaperFilter.META_PREFIX + "isbn_from_origin");
        // TODO: Write the proper regexp here
        metaConfOrigin.set(RecordShaperFilter.CONF_META_REGEXP,
                     "([0-9]((?s)<publicationStmt.*?>.+?<idno.+?type=\"ISBN\">(.+?)</idno>");
        metaConfOrigin.set(RecordShaperFilter.CONF_META_TEMPLATE, "$1");

        Configuration resolverConf = Configuration.newMemoryBased(
            RelationResolver.CONF_ASSIGN_PARENTS, true,
            RelationResolver.CONF_SEARCH_FIELD, "id",
            RelationResolver.CONF_SEARCH_MAXHITS, 10,
            RelationResolver.CONF_SEARCH_METAKEYS,
            new ArrayList<String>(Arrays.asList("isbn", "isbn_from_origin")),
            ConnectionConsumer.CONF_RPC_TARGET, "NotUsed"
        );

        Configuration storageConf = Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, STORAGE.getAbsolutePath());

        Payload teiP = new Payload(teiR);
        teiP.getData().put(
            Payload.ORIGIN,
            "somefolder/somezip!/01234 567 890 - Svamperiget 8.xml");

        Storage storage = StorageFactory.createStorage(storageConf);
        storage.flush(parentR1);
        storage.flush(parentR2);
        storage.flush(parentR3);
        assertNotNull("The parent Records should be stored",
                      storage.getRecord(parentR1.getId(), null));

        ObjectFilter feeder = new PayloadFeederHelper(Arrays.asList(teiP));
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
                if (searchValue.equals("01234 567 890")) {
                    response = new DocumentResponse(
                        null, searchValue, 0, 2, "LUCENE", false,
                        new String[]{DocumentKeys.RECORD_ID}, 1, 1);
                    response.addRecord(new DocumentResponse.Record(
                        "internal_id1", "foo", 1.0f, ""));
                } else if (searchValue.equals("12-3456-789-0")) {
                    response = new DocumentResponse(
                        null, searchValue, 0, 2, "LUCENE", false,
                        new String[]{DocumentKeys.RECORD_ID}, 1, 2);
                    response.addRecord(new DocumentResponse.Record(
                        "internal_id1", "foo", 1.0f, ""));
                    response.addRecord(new DocumentResponse.Record(
                        "internal_id2", "foo", 1.0f, ""));
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

        //noinspection StatementWithEmptyBody
        while (writer.pump());
        writer.close(true);

        Record enriched = storage.getRecord(
            teiR.getId(), new QueryOptions(null, null, 10, 10));
        assertNotNull("The enriching Record should be stored", enriched);
        assertEquals("The enriching Record should have the right parent count",
                     3, enriched.getParents().size());
        storage.close();
    }

}
