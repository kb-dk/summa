package dk.statsbiblioteket.summa.support.embeddedsolr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;

import java.io.File;


/**
* This class startes up an embedded memory-based Solr server from a given solr.home directory.
* 
* All communication  with the server is wrapped with SolrJ.
*/
public class EmbeddedSolrJServer {
	private static Log log = LogFactory.getLog(EmbeddedSolrJServer.class);
		
	private static final String DEFAULT_SOLR_HOME = "support/solr_home_default";
	
	/**
	 * Starts up a Jetty server running the SOLR instance.
	 * The SolrHome will be prefixed with "_merged" and can be found under /target/test-classes/support
	 * 
	 * @param solrHome a folder with a Solr setup. The data folder in the setup will be modified by this test.
	 *                 The homeDir may be specified as a file system path or as a folder accessible by the ClassLoader.
	 *                 Any files missing in this SolrHome directory will be taken from the default SolrHome directory.		
	 * @throws Exception if the embedded Solr server could not be created.
	 */
	public static EmbeddedSolrServer createServer(String solrHome) throws Exception{
	
		solrHome = SolrServerUnitTestUtil.resolve("Solr home", solrHome);	
		System.setProperty("basedir", "."); //for logback		
		System.setProperty("solr.solr.home", solrHome+SolrServerUnitTestUtil.SOLR_HOME_SUFFIX);	
		System.setProperty("solr.data.dir", solrHome+SolrServerUnitTestUtil.SOLR_HOME_SUFFIX);          	
		String solrHomeDefault = SolrServerUnitTestUtil.resolve("Solr Home default",DEFAULT_SOLR_HOME);
		
		
		//Copy solrconfig.xml, schema.xml etc. to the solrhome.
		SolrServerUnitTestUtil.populateSolrHome( new File(solrHome), new File(solrHomeDefault));		
	
		
        CoreContainer.Initializer initializer = new CoreContainer.Initializer();
        CoreContainer coreContainer = initializer.initialize();
        return new EmbeddedSolrServer(coreContainer, "");
	}
	

	
}
