package dk.statsbiblioteket.summa.support.embeddedsolr;

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
		System.setProperty("basedir", ".");
    	System.setProperty("solr.solr.home", "target/test-classes/support/solr_home1");          	
    	System.setProperty("solr.data.dir", "target/test-classes/support/solr_home1");
    	
        CoreContainer.Initializer initializer = new CoreContainer.Initializer();
        CoreContainer coreContainer = initializer.initialize();
        server = new EmbeddedSolrServer(coreContainer, "");        
    }

    
    @Test
    public void testThatDocumentIsFound() throws SolrServerException, IOException {
        SolrInputDocument document = new SolrInputDocument();
    
        document.addField("recordId","x5");
        document.addField("author_main","mikis og henning");
        
       
        server.add(document);
        server.commit();

       SolrQuery query = new SolrQuery("henning");
       query.setQueryType("/edismax"); //Specielt defineret i solrconfig.xml
        
        QueryResponse response = server.query(query);        
        assertEquals("Hvor er Henning?",1, response.getResults().getNumFound());

        
               
        
    }
    
   // @Test
    public void testThatNoResultsAreReturned() throws SolrServerException {
        SolrParams params = new SolrQuery("text that is not found");
        QueryResponse response = server.query(params);
        assertEquals(0L, response.getResults().getNumFound());
    }

    
    
}
