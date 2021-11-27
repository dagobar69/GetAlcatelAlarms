package it.telecomitalia.com;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import it.telecomitalia.com.dbTable.DCCXAdapterStatus;


public class GetAlcatelAlarmsMain {

	private Logger myLog;
	private static Properties  prop; 
	
	public static void main(String[] args) {
		String logFileName = getAbsolutePathFile("conf/log4j.properties");
		PropertyConfigurator.configure(logFileName);
		Logger logI = Logger.getLogger(GetAlcatelAlarmsMain.class);
		String fileNameProperties = getAbsolutePathFile("conf/getAlarms.properties");
		readProps(fileNameProperties,logI);
		
		if (prop != null) {
			GetAlcatelAlarmsMain gm = new GetAlcatelAlarmsMain();
			gm.go();
		}
	}
	
	public void go() {
		myLog = Logger.getLogger(getClass().getName());
		String dbUser = prop.getProperty("DB_USER");
		String dbPwd = prop.getProperty("DB_PWD");
		String dbUrl = prop.getProperty("DB_URL");
		int intervallo = (new Integer(prop.getProperty("INTERVALLO","10"))).intValue();
		
		String fileRunName = getAbsolutePathFile("GetAlcatelAlarms.pid");
		writeFileRun(fileRunName);
		File fileRunning = new File(fileRunName);
		
		while(fileRunning.exists()) {
			
			Long lastIdSeq=getLastIdSeq();
			
			Connection connection = DBUtility.getConnection(dbUser, dbPwd, dbUrl);
			if (connection != null) {
				
				Hashtable<String,HashMap<String,String>> allarmi = getAlarms(connection
																			 , lastIdSeq);
				
				for(String key: allarmi.keySet()) {
					
					DCCXAdapterStatus dbCXAdapterStatus = new DCCXAdapterStatus(allarmi.get(key));
					dbCXAdapterStatus.send();
					lastIdSeq=Long.decode(allarmi.get(key).get("ID_SEQ"));
				}
				
				try {
					connection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				DCCXAdapterStatus.closeProducer();
				
				setLastIdSeq(lastIdSeq);
			}
			myLog.debug("waiting " + intervallo + " minuti.........");
			try {
				Thread.sleep(intervallo*60000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		myLog.debug("Processo terminato");
	}
	
	private void writeFileRun(String fileRunName) {
		
		try{
			BufferedWriter out = new BufferedWriter(new FileWriter(fileRunName));
			out.write("running........");
			out.flush();
			out.close();
		
			Runtime.getRuntime().exec("chmod 777 " + fileRunName);
		}catch(Exception e){
		}
	}
	
	@SuppressWarnings("unused")
	public Hashtable<String,HashMap<String,String>> getAlarms(Connection conn
																, Long lastIdSeq) {
		

		StringBuilder whereCond=new StringBuilder();

		Hashtable<String,HashMap<String,String>> allarmi = new Hashtable<String,HashMap<String,String>>();

		if (lastIdSeq != null) {
			whereCond.append(" id_seq > "+lastIdSeq+" and ");
		}
		whereCond.append(" rownum <= 1000");
		
		String query = DCCXAdapterStatus.getQuery(whereCond.toString());
		
		allarmi = DBUtility.getRecords(conn,
				query, 
				DCCXAdapterStatus.KEY_TABELLA);
		
		return allarmi;
	}
	
	private Long getLastIdSeq() {
		
		Long lastIdSeq=0L;
		
		String fileNameDateOld = getAbsolutePathFile("DBLastDownload");
		File fileToFind = new File(fileNameDateOld);
		StringBuilder sb = new StringBuilder();
		
		if( fileToFind.exists()) {
			BufferedReader br;
			
			try {
				br = new BufferedReader(new FileReader(fileToFind));
			
				String line;
				
				while((line=br.readLine())!= null){

					int sepPosition=line.indexOf("-");
					
					lastIdSeq=Long.decode(line.substring(0, sepPosition));
				    sb.append(line.substring(sepPosition+1, line.length()));
				}
				myLog.debug("From file: last id_seq "+lastIdSeq.toString()+" last query date "+sb.toString());
			} catch (IOException e) {
				myLog.error("IOException. " + e.getMessage());
				sb.setLength(0);
			}
		}
		
		return lastIdSeq;
	}
	
	private void setLastIdSeq(Long lastIdSeq) {
		String fileNameDateOld = getAbsolutePathFile("DBLastDownload");
		BufferedWriter outFile = null;
		try {
			outFile = new BufferedWriter(new FileWriter(fileNameDateOld));
			SimpleDateFormat formatoData = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String sDate = formatoData.format(new Date());
			StringBuffer sb = new StringBuffer();
			if (lastIdSeq == null) lastIdSeq=(long) 0;
			sb.append(lastIdSeq.toString()).append("-").append(sDate);
			outFile.write(sb.toString());
			outFile.flush();
			outFile.close();
			myLog.debug("setDateOld: DateOld scritta nel file: " + fileNameDateOld);
		}catch (IOException e1) {
			 outFile = null;
			 myLog.error("setDateOld. IOException " + e1.getMessage());
		}
	}
	
	public static Properties getProperties() {
		return prop;
	}
	
	 private static void readProps(String fileNameProperties,Logger logI){
	    	prop = null;
			try {
				InputStream inputStream = new FileInputStream(fileNameProperties);
				if (inputStream != null) {
					prop = new Properties();
					prop.load(inputStream);				
					logI.debug("readIt. " +prop.toString());
				}
			} catch (FileNotFoundException e) {
				logI.error("readIt. Properties file <" + fileNameProperties + "> not found");
				prop = null;
			} catch (IOException e) {
				logI.error("readIt. IOException. " + e.getMessage());
				prop = null;
			}
		}
	    
	    private static String getAbsolutePathFile(String fileName){

			String retFileName;
			String path = (new java.io.File(".") ).getAbsolutePath();
			path = path.substring(0,path.length()-1);

			if (fileName.startsWith(path)){
			    retFileName = fileName;
			}else{
			    retFileName = path + fileName.replaceAll("/", Matcher.quoteReplacement(File.separator));
			}

			return retFileName;
	    }

}
