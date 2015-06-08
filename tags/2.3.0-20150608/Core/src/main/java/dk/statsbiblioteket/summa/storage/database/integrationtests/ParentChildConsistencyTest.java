package dk.statsbiblioteket.summa.storage.database.integrationtests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//This unit test is not part of automatic test suite. This test is used to
//check parent-child id consistency for the database.
//Some records refer to  none-existing parent/child id's.
public class ParentChildConsistencyTest {

	private static final Logger log = LoggerFactory.getLogger(ParentChildConsistencyTest.class);
	private static final String DB_FILE =  "/media/500GB/prod_database_kopi/storage/summa_h2storage";//.h2.db part is implicit	
	private static SimpleH2Storage storage = null;

	/*
	 * Delete database file if it exists. Create database with tables
	 */

	@BeforeClass
	public static void beforeClass() throws Exception {		
		storage = new SimpleH2Storage(DB_FILE);

	}

	@AfterClass
	public static void afterClass() throws Exception {
		 SimpleH2Storage.shutdown();
	}

/* In-comment to run consistency test
	@Test
	public void checkParentChildConsistency() throws Exception {
    //to generate nice output from console use: cut -d ] -f 4 records.txt  | less				
     storage.checkParentChildConsistencyTest();
	}
*/
	
	
}
