package dk.statsbiblioteket.summa.support.embeddedsolr;

import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmbeddedSolrTest{
	
    private EmbeddedSolrServer server;
	
    
    @Test
    public void testThatDocumentIsFound() throws Exception {
    
    	EmbeddedSolrServer server = EmbeddedSolrJServer.createServer("target/test-classes/support/solr_home1");
    	SolrInputDocument document = new SolrInputDocument();
    
        document.addField(IndexUtils.RECORD_FIELD,"x5");
        document.addField("recordBase","aleph");
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
