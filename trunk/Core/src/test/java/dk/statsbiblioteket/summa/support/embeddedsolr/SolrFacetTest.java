package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class SolrFacetTest {

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
	public void testFacets() throws Exception {

		//Data:
		//3 books by Thomas Egense, 1 located at Matematik, 2 at Statsbiblioteket. 2 from 2011, 1 from 2012
		//2 books by Toke Eskildsen, 2 located at Statsbiblioteket, 1 from 2012, 1 from 2010
		//Facets defined for year,location_normalised,author_normalised
		String[] files = new String[]{
				"support/solr_test_documents/doc1.xml",
				"support/solr_test_documents/doc2.xml",
				"support/solr_test_documents/doc3.xml",
				"support/solr_test_documents/doc4.xml",
				"support/solr_test_documents/doc5.xml",
		};
		SolrServerUnitTestUtil.indexFiles(files);

		SolrQuery query = new SolrQuery("Egense");
		query.setFacet(true);

		QueryResponse response = solrServer.query(query);
		assertEquals(3L, response.getResults().getNumFound());

		FacetField facetYear = response.getFacetField("year");
		assertEquals(2L, facetYear.getValueCount()); //2011 and 2012
		assertEquals("2011 (2)", facetYear.getValues().get(0).toString()); //can also be done with getFacetField and then get value and count
		assertEquals("2012 (1)", facetYear.getValues().get(1).toString());

		FacetField facetLocation = response.getFacetField("location_normalised");
		assertEquals(2L, facetLocation.getValueCount()); //2010 and 2012
		assertEquals("statsbiblioteket (2)", facetLocation.getValues().get(0).toString());
		assertEquals("matematik (1)", facetLocation.getValues().get(1).toString()); //can also be done with getFacetField and then get value and count

		FacetField facetAuthor = response.getFacetField("author_normalised");
		//assertEquals("thomas egense (3)",facetAuthor.getValues().get(0).toString()); Not working before analyzer on field is fixed to lowercasefilter etc.


		query = new SolrQuery("toke");
		query.setFacet(true);
		response = solrServer.query(query);
		assertEquals(2L, response.getResults().getNumFound());

		facetYear = response.getFacetField("year");
		assertEquals(2L, facetYear.getValueCount()); //2010 and 2012
		assertEquals("2010 (1)", facetYear.getValues().get(0).toString()); //can also be done with getFacetField and then get value and count
		assertEquals("2012 (1)", facetYear.getValues().get(1).toString());

		facetLocation = response.getFacetField("location_normalised");
		assertEquals(1L, facetLocation.getValueCount()); //2010 and 2012
		assertEquals("statsbiblioteket (2)", facetLocation.getValues().get(0).toString());

		//  Direct REST call. /edismax is defined to search in all summa-fields with boosts
		//String httpResponse = SolrServerUnitTestUtil.callURL(url+"/edismax/?q=datsun&spellcheck=true");
		//String httpResponse = SolrServerUnitTestUtil.callURL(server.getServerUrl()+"/select/?spellcheck=true&spellcheck.dictionary=summa_spell&spellcheck.extendedResults=true&spellcheck.maxCollationsTries=5&spellcheck.collate=true&spellcheck.collateExtendedResults=true&spellcheck.count=5&q=egense");

	}



}