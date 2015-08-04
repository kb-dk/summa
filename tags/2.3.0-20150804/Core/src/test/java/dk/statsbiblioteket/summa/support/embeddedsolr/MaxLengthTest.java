package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class MaxLengthTest {


    String solrHome = "support/solr_home_default"; //data-dir (index) will be created here.  
    EmbeddedJettyWithSolrServer server;
    SolrServer solrServer;


    @Before
    public void setUp() throws Exception {
        System.setProperty("basedir", "."); //for logback
        server =  new EmbeddedJettyWithSolrServer(Resolver.getFile(solrHome).toString());
        server.run();
        solrServer = new HttpSolrServer(server.getServerUrl());     
    }

    @Test
    public void testFieldAlias() throws Exception {

        //title field of the dokument: "Sjov med fraktaler"
        //'title'field also has alias: 'titel' in solr.config edismax-requesthandler
        String[] files = new String[]{
                "support/solr_test_documents/max_length_doc.txt",      
        };
        SolrServerUnitTestUtil.indexFiles(files);
        //Thread.sleep(1000000000L);    
                    
        //Find the post. Notice 255 characters seems to be Solr maximum token-size when searching
        SolrQuery query = new SolrQuery(
                //First 250 characters, wildcard search 1234567890......*
           "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890*"
                );
        QueryResponse response = solrServer.query(query);
        assertEquals(1L, response.getResults().getNumFound());
      
        
    }

    
}
