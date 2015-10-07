package dk.statsbiblioteket.summa.storage.database.integrationtests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;

//Simple H2Storage that takes the H2-file in the constructor
public class  SimpleH2Storage {

	private static final Logger log = LoggerFactory.getLogger(SimpleH2Storage.class);
	private static SimpleH2Storage instance = null;

	private static Connection singleDBConnection = null;

	private static String getRecordById = "SELECT ID FROM SUMMA_RECORDS WHERE ID = ?";

	public SimpleH2Storage(String dbFilePath) throws SQLException {
		log.info("Intialized H2Storage, dbFile=" + dbFilePath);
		synchronized (H2Storage.class) {
			initializeDBConnection(dbFilePath);
		}
	}


	public static SimpleH2Storage getInstance() {
		if (instance == null) {
			throw new IllegalArgumentException("SimpleH2Storage has not been initialized yet");
		}

		return instance;
	}

	public void checkParentChildConsistencyTest() throws Exception{
		//Cache so each ID is only checked once.

		PreparedStatement stmt = null;
		ResultSet rs  = null;


		stmt = singleDBConnection.prepareStatement("SELECT * FROM SUMMA_RELATIONS");
		log.info("loaded SUMMA_RECORDS table");
		try{
			rs = stmt.executeQuery();
			int index = 1;
			while (rs.next()){
				index++;


				String parentId = rs.getString(1);
				String childId = rs.getString(2);

				if (parentId.equals(childId)){
					log.error(" parentId=childId: "+parentId);
				}

				boolean parentFound= recordIdFound(parentId);
				if (!parentFound){
					log.error("ParentId not found: "+ parentId  +" (childId :"+childId+")");
				}

				boolean childFound= recordIdFound(childId);
				if (!childFound){
					log.error("ChildId not found: "+ childId  +" (parentId :"+parentId+")");
				}

				if (!parentFound && ! childFound){
					log.error("Both ChildId and parentId not found: childId:"+childId+ " parentId:"+parentId);
				}			

			}
			log.info("all "+index +" rows checked for SUMMA_RELATIONS table");

		}
		finally{
			closeStatement(stmt);
		}

	}

	private boolean recordIdFound(String recordId) throws Exception{
		PreparedStatement stmt = null;
		ResultSet rs  = null;
		try{
			stmt = singleDBConnection.prepareStatement(getRecordById);
			stmt.setString(1, recordId);
			rs = stmt.executeQuery();
			while (rs.next()){
				return true;
			}

			return false;
		}
		finally{
			closeStatement(stmt);
		}

	}

	private synchronized void initializeDBConnection(String dbFilePath) throws SQLException {
		log.info("initializeDBConnection. DB path:" + dbFilePath);
		if (singleDBConnection != null) {
			log.error("DB allready initialized and locked:" + dbFilePath);
			throw new RuntimeException("DB allready initialized and locked:" + dbFilePath);
		}

		try {
			Class.forName("org.h2.Driver"); // load the driver
		} catch (ClassNotFoundException e) {
			throw new SQLException(e);
		}
		String DB_URL = "jdbc:h2:" + dbFilePath;
		singleDBConnection = DriverManager.getConnection(DB_URL, "", "");
		singleDBConnection.setAutoCommit(false);
		instance = this;

	}
	private void closeStatement(PreparedStatement stmt) {
		try {
			if (stmt != null) {
				stmt.close();
			}
		} catch (SQLException e) {
			log.error("Failed to close statement"); 
		}
	}
	public static void shutdown() {
		log.info("Shutdown SimpleH2Storage");
		try {
			if (singleDBConnection != null) {
				PreparedStatement shutdown = singleDBConnection.prepareStatement("SHUTDOWN");
				shutdown.execute();
				if (singleDBConnection != null) {
					singleDBConnection.close();
				}		
			}
		} catch (Exception e) {
			// ignore errors during shutdown, we cant do anything about it anyway
			log.error("shutdown failed", e);
		}
	}
}
