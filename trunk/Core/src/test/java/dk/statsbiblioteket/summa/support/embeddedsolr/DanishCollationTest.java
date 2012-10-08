package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class DanishCollationTest {

	String solrHome = "support/solr_home1"; //data-dir (index) will be created here.  
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
	public void testDanishCollationTest() throws Exception {

		
		String[] files = new String[]{
				"support/solr_test_documents/doc1_sort.xml",
				"support/solr_test_documents/doc2_sort.xml",
				"support/solr_test_documents/doc3_sort.xml",
				"support/solr_test_documents/doc4_sort.xml",
				"support/solr_test_documents/doc5_sort.xml",
				"support/solr_test_documents/doc6_sort.xml",
				
				
		};
		SolrServerUnitTestUtil.indexFiles(files);
	
		//testing danish collator on field sort_title
		//The texts below are in the shortformat field, which is stored. (sort_title is not stored)
		String doc1Text="Aber er sjove";
		String doc2Text="Ål smager godt";
		String doc3Text="Æbler er sunde";
		String doc4Text="Østers smager ikke godt";
		String doc5Text="Zebraer smager også godt";
		String doc6Text="A efterfulgt af whitespace skal stå før abe";
				
		
		SolrQuery query = new SolrQuery("*:*");
 		query.setSortField("sort_title", ORDER.asc);
		QueryResponse response = solrServer.query(query);
	  	assertEquals(6L, response.getResults().getNumFound());
	    Iterator<SolrDocument> iterator = response.getResults().iterator();
	    	    
	    //Correct order: A alene.., Aber, Zebraer , Æbler, Østers , Ål
	    assertEquals(doc6Text, iterator.next().getFieldValue("shortformat"));
	    assertEquals(doc1Text, iterator.next().getFieldValue("shortformat"));
	    
	    assertEquals(doc5Text, iterator.next().getFieldValue("shortformat"));
	    assertEquals(doc3Text, iterator.next().getFieldValue("shortformat"));
	    assertEquals(doc4Text, iterator.next().getFieldValue("shortformat"));
	    assertEquals(doc2Text, iterator.next().getFieldValue("shortformat"));
	    	   	  		  	
	}

}