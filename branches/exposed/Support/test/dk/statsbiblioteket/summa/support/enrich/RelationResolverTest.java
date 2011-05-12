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
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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
            RelationResolver.CONF_SEARCH_METAKEY, "SearchTerm",
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

}
