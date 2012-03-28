package dk.statsbiblioteket.summa.common;

import junit.framework.TestCase;

public class SummaConstantsTest extends TestCase {

	  public void testLoadRevisionProperty (){
		  		 
		String version=SummaConstants.getVersion();
		assertEquals("${pom.version}", version);  // it is ${pom.version} before Maven filtering.
		
	  }
	
	
	
}
