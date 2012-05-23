package dk.statsbiblioteket.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class EmbeddedSolrTest{
	
	
    private SolrServer server;
	
    @Before    
    public void setUp() throws Exception {             
    	System.setProperty("solr.solr.home", "src/test/tomcat/solr");      
    	System.setProperty("solr.data.dir", "target/data/embeddedSolrTest");
    	 
        CoreContainer.Initializer initializer = new CoreContainer.Initializer();
        CoreContainer coreContainer = initializer.initialize();
        server = new EmbeddedSolrServer(coreContainer, "");        
    }

    
    @Test
    public void testThatDocumentIsFound() throws SolrServerException, IOException {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("recordId", "1");
        document.addField("text", "thomas");

        server.add(document);
        server.commit();

        SolrParams params = new SolrQuery("thomas");
        QueryResponse response = server.query(params);
        assertEquals(1L, response.getResults().getNumFound());
        assertEquals("1", response.getResults().get(0).get("recordId"));
               
    }
    
   // @Test
    public void testThatNoResultsAreReturned() throws SolrServerException {
        SolrParams params = new SolrQuery("text that is not found");
        QueryResponse response = server.query(params);
        assertEquals(0L, response.getResults().getNumFound());
    }

    
    
}
