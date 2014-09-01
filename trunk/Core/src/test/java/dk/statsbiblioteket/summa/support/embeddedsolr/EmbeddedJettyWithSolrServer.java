package dk.statsbiblioteket.summa.support.embeddedsolr;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;



/**
* This class starts up a Jetty server with a specified solr.war and SOLR_HOME directory (schema.xml/solrconfig.xml are in this directory etc.)
* Url-context and port can be specified as well. 
* 
* The server runs as a daemon thread and can also be started and stopped if needed.
* All files from the specified SOLR_HOME will be used and those missing will be taken from the SOLR_HOME_DEFAULT directory.
* This way it is easier to make many different unit-tests with only changes in schema/solrconfig as you only need to have these
* files in your solr-home directory for the test.
*
*/
public class EmbeddedJettyWithSolrServer extends Thread {
	private static Log log = LogFactory.getLog(EmbeddedJettyWithSolrServer.class);

	private static final String DEFAULT_SOLR_WAR = "support/solr.war";
	public static final String DEFAULT_CONTEXT = "/solr";
	public static final int DEFAULT_PORT = 8983;
	
	private Server server;
	private String serverUrl;

	/**
	 * Starts up a Jetty server running the SOLR instance.
	 * The SolrHome will be prefixed with "_merged" and can be found under /target/test-classes/support
	 * 
	 * @param solrHome a folder with a Solr setup. The data folder in the setup will be modified by this test.
	 *                 The homeDir may be specified as a file system path or as a folder accessible by the ClassLoader.
	 *                 Any files missing in this SolrHome directory will be taken from the default SolrHome directory.
	 *                 
	 * @param solrWar  the path to the solr WAR file. Either as a file path or a ClassLoader path.
	 * @param context  the URL-path exposed by the Solr REST interface (normally {@code /solr}).
	 * @param port     the port for the Jetty server.
	 * @throws Exception if the embedded Solr server could not be created.
	 */
	public EmbeddedJettyWithSolrServer(String solrHome, String solrWar , String context, int port) throws Exception {
		this.setDaemon(true);

		solrWar = SolrServerUnitTestUtil.resolve("WAR", solrWar);
		solrHome = SolrServerUnitTestUtil.resolve("Solr home", solrHome);


		// throw new Exception("The Solr war at " + solrWar  + " should exist.
		// Run 'mvn clean install -DskipTests' in the core-module");

		//Copy solrconfig.xml, schema.xml etc. to the solrhome.
		SolrServerUnitTestUtil.populateSolrHome( new File(solrHome));

		System.setProperty("solr.solr.home", solrHome+	SolrServerUnitTestUtil.SOLR_HOME_SUFFIX);

		server = new Server(port);
		WebAppContext webapp = new WebAppContext(solrWar,context);

		
		// Hvis solr fejler under opstart med "mockedint classcast exception" er det fordi denne jar mangler på jetty's
		// classpath.
		// Det er uklart hvorfor dette bliver aktiveret når man kører unittest
        // Ser ikke ud til at være nødvendig mere
		// webapp.setExtraClasspath(SolrServerUnitTestUtil.resolve("Lucene Test Framework", "support/lucene-test-framework-4.7.jar"));
		
		server.setHandler(webapp);
		serverUrl = "http://localhost:" + port + context;
	}

	/**
	 * Starts up a Jetty server running the SOLR instance with default solrWar ({code support/solrwar}).
	 * @param solrHome a folder with a Solr setup. The data folder in the setup will be modified by this test.
	 *                 The homeDir may be specified as a file system path or as a folder accessible by the ClassLoader.
	 * @param context  the URL-path exposed by the Solr REST interface (normally {@code /solr}).
	 * @param port     the port for the Jetty server.
	 * @throws Exception if the embedded Solr server could not be created.
	 */
	public EmbeddedJettyWithSolrServer (String solrHome, String context, int port) throws Exception {
		this(solrHome, DEFAULT_SOLR_WAR, context, port);
	}

	/**
	 * Starts up a Jetty server running the SOLR instance with default solrWar {code support/solr.war}, default context
	 * {code /solr} and default port {@code 8983}.
	 * @param solrHome a folder with a Solr setup. The data folder in the setup will be modified by this test.
	 *                 The homeDir may be specified as a file system path or as a folder accessible by the ClassLoader.
	 * @throws Exception if the embedded Solr server could not be created.
	 */
	public EmbeddedJettyWithSolrServer (String solrHome) throws Exception {
		this(solrHome, DEFAULT_SOLR_WAR, DEFAULT_CONTEXT, DEFAULT_PORT);
	}

	@SuppressWarnings("CallToPrintStackTrace")
	@Override
	public void run()  {
		try{
			server.start();
		} catch(Exception e){
//			System.err.println("Exception running Solr server:");
			e.printStackTrace();
			throw new RuntimeException("Exception running Solr server with address " + serverUrl, e);
		}
	}

	public void stopSolr() throws Exception {
		server.stop();
	}

	public String getServerUrl() {
		return serverUrl;
	}
	
	public boolean isStarted() {
		return server.isStarted();
	}
	
}
