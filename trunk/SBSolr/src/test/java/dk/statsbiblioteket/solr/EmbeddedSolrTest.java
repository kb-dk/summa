package dk.statsbiblioteket.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.embedded.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.Before;
import org.junit.Test;

public class EmbeddedSolrTest extends AbstractSolrTestCase {
	  
    private SolrServer server;
	
    @Override
    public String getSchemaFile() {
        return "solr/conf/schema.xml";
    }

    @Override
    public String getSolrConfigFile() {
        return "solr/conf/solrconfig.xml";
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        server = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
    }

    
    @Test
    public void testThatDocumentIsFound() throws SolrServerException, IOException {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("recordId", "1");
        document.addField("text", "thomas bjørn");

        server.add(document);
        server.commit();

        SolrParams params = new SolrQuery("thomas");
        QueryResponse response = server.query(params);
        assertEquals(1L, response.getResults().getNumFound());
        assertEquals("1", response.getResults().get(0).get("recordId"));
               
    }
    
    @Test
    public void testThatNoResultsAreReturned() throws SolrServerException {
        SolrParams params = new SolrQuery("text that is not found");
        QueryResponse response = server.query(params);
        assertEquals(0L, response.getResults().getNumFound());
    }

    
    
}
