package it.telecomitalia.com;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.log4j.Logger;

public class DBUtility {

	public static Connection getConnection(String dbUser,String dbPwd,String dbURL){
		Connection connection = null;
		
		Logger myLog = Logger.getLogger(DBUtility.class);
		if (dbUser == null || dbPwd == null || dbURL == null ){
			myLog.error("connect. Parametri di accesso al db non corretti");
		}else{
			myLog.debug("connect. dbUser: " + dbUser + " dbPwd: *****" + " dbUrl: " + dbURL);
			try {
				//DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
				Class.forName("oracle.jdbc.driver.OracleDriver");
				connection = DriverManager.getConnection(dbURL,dbUser,dbPwd);
				myLog.debug("connect. DB connesso");
			} catch (SQLException e) {
				myLog.debug("connect. SQLException nella connection al db: " + e.getMessage());
				e.printStackTrace();
				connection = null;
			} catch (Exception e) {
				connection = null;
				e.printStackTrace();
				myLog.error("connect. Eccezione generica nella connection al db: " + e.getMessage());
			}
		}

		return connection;
	}
	
	public static Hashtable<String,HashMap<String,String>> getRecords(Connection connection
			                                                          , String query
			                                                          , String keyTable) {
		Logger logger = Logger.getLogger(DBUtility.class);
		Statement stmt=null;
		ResultSet rs=null;
		Hashtable<String,HashMap<String,String>> hTable = new Hashtable<String,HashMap<String,String>>();
		int numRecords = 0;
		
		try {
			stmt = connection.createStatement();

			logger.debug("getRecords. Eseguo query: " + query);

			rs = stmt.executeQuery(query);
			while (rs.next()) {
				HashMap<String,String> newRecord = new HashMap<String,String>();
				ResultSetMetaData rsmd = rs.getMetaData();
				String value;
				for (int idx=1; idx <= rsmd.getColumnCount(); idx++){
					value = (rs.getString(idx)==null?"":rs.getString(idx));
					if (value != null){
						newRecord.put(rsmd.getColumnName(idx).toUpperCase(), value);
					}
				}
				numRecords++;
				hTable.put(newRecord.get(keyTable), newRecord);
				
			}
			
			logger.debug("getRecords. Records presenti in tabella " + numRecords);
			rs.close();
			stmt.close();

		} catch (SQLException e) {
			logger.error("getRecords. SQLException : ", e);
			numRecords = -1;
			
			hTable = null;
		} catch (SecurityException e) {
			hTable = null;
			logger.error("getRecords. SecurityException : ", e);
		} catch (IllegalArgumentException e) {
			hTable = null;
			logger.error("getRecords. IllegalArgumentException : ", e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ignore) {
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (Exception ignore) {
				}
			}
		}
		
		return hTable;
	}
}
