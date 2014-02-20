package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.collation.CollationKeyFilterFactory;
import org.apache.lucene.collation.ICUCollationKeyFilterFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Before;
import org.junit.Test;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class DanishCollationAdvancedTest {

    String solrHome = "support/solr_home_default"; //data-dir (index) will be created here.  
	EmbeddedJettyWithSolrServer server;
	SolrServer solrServer;
    private Class<? extends TokenFilterFactory> lookupClass;
	
	@Before
	public void setUp() throws Exception {
		System.setProperty("basedir", "."); //for logback
		server =  new EmbeddedJettyWithSolrServer(Resolver.getFile(solrHome).toString());
		server.run();
		solrServer = new HttpSolrServer(server.getServerUrl());		
	}

	@Test
	public void testDanishCollationTest() throws Exception {

//		dumpCollatorRules();
		String[] files = new String[]{
				"support/solr_test_documents/doc1_sort_advanced.xml"							
				
		};
		SolrServerUnitTestUtil.indexFiles(files);
	

		//Notice the first two should be switched. Try fix this in solr 5.0. 
		String[] correctOrder = new String[]{
		        "§14 af ophavsretsloven",
		        "$4 er prisen",
		        "Analyse og teori",
		        "Analysen af teksten",
		        "Dagen derpå",
		        "(dagen oprandt)",
		        "làm đĩ",
		        "lam di dam di duehhh",
		        "lam fra brugsen",
		        "\"Lam i roen\"",
		        "... og så blev det aften",
		        "Også andre blev udpeget",
		        "är allt förbi?",
		        "Ære & skam",
		        "Ärenden som beröra samerna",
		        "Øg din digitale træfprocent",
		        "Öga för öga",
		        "Åen, der hadede løgn",
		        "Åen der vendte tilbage"
		};
		
		
		 //dumpCollatorRules();
		 
		 
		//ICUCollationKeyFilterFactory"
		SolrQuery query = new SolrQuery("*:*");
 		query.setSortField("sort_title", ORDER.asc);
 		query.setRows(25);
		QueryResponse response = solrServer.query(query);
	    Iterator<SolrDocument> iterator = response.getResults().iterator();

	    int index=0;
	    while(iterator.hasNext()){
	        Object fieldValue = iterator.next().getFieldValue("shortformat");
	        assertEquals(correctOrder[index++],fieldValue.toString());
	        System.out.println(fieldValue.toString());
	        
	    }
	 // Thread.sleep(50000000000L);	    
	   
	    	   	  		  	
	}

	//Only used once to create the default DA locale rules which is then
	//renamed to summa_collation_rules.dat and modified further.
	
    private void dumpCollatorRules() throws IOException, FileNotFoundException {
        RuleBasedCollator baseCollator = (RuleBasedCollator) Collator.getInstance(new Locale("da", "DA"));
	
         

        IOUtils.write(baseCollator.getRules() , new FileOutputStream("/home/teg/customRules.dat"), "UTF-8");
    }

}