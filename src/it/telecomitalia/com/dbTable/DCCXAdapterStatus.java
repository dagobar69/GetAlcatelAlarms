package it.telecomitalia.com.dbTable;

import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

import it.telecomitalia.com.GetAlcatelAlarmsMain;

public class DCCXAdapterStatus {

	public static String nomeTabella="DCCXAdapterStatus";
	public final static String KEY_TABELLA = "ID_SEQ";
	private static Properties kafkaProps = null;
	private HashMap<String,String> allarme;
	private Logger myLog;
	private Properties myProps;
	private static String topicName;
	private static KafkaProducer<String, String> producer = null;
	
	public DCCXAdapterStatus(HashMap<String,String> allarme){
		this.allarme=allarme;
		myLog = Logger.getLogger(getClass().getName());
		myProps = GetAlcatelAlarmsMain.getProperties();
		
		if (kafkaProps == null) {
			setKafkaConfiguration();
		}
	}
	
	public void send() {
		Gson gson = new Gson();
        String messaggio = gson.toJson(allarme);
		
		myLog.debug("send. Messaggio da inviare: " +messaggio);
		String alarmId = allarme.get(KEY_TABELLA);
		if (allarme.containsKey("IDENTIFIER")) {
			String identifier = allarme.get("IDENTIFIER");
			if (messaggio.length()>0) {
				myLog.debug("send. allarme Id: " + alarmId + " AlarmID: " + " Key: "  + identifier + " Msg: " + messaggio );
				
				if (producer==null) {
					producer = new KafkaProducer<String, String>(kafkaProps);
				}
							
				try {
					ProducerRecord<String, String> data = new ProducerRecord<String, String>(topicName, identifier, messaggio);
					try{
						Future<RecordMetadata> recMetadata = producer.send(data);
						
						myLog.debug("send. allarme Id: " + alarmId + " AlarmID: " +  " Key: "  + identifier + 
									" Allarme inviato con offset: " + recMetadata.get().offset());
					}catch(Exception e){
						myLog.error("send. allarme Id: " + alarmId  + " Key: "  + identifier + 
								" Allarme non inviato. " + e.getMessage());
					}
		        	
		    	}catch(Exception e) {
		    		myLog.error("send. allarme Id: " + alarmId  + " Key: "  + identifier  + " exception: " + e.getMessage());
		    	}
			}else {
				myLog.error("Allarme senza IDENTIFIER impossibile inviarlo");
			}
		}else {
			myLog.error("send. Messaggio da inviare vuoto");
		}
	}
	
	public static void closeProducer() {
		if (producer != null) {
			producer.close();
			producer=null;
		}
	}
	
	public static String getQuery(String whereCond) {
		StringBuffer sb = new StringBuffer("select * from ");
		sb.append(nomeTabella);
		if (whereCond != null == whereCond.length()>0) {
			sb.append(" where ").append(whereCond);
		}
		sb.append(" order by id_seq");
		
		return sb.toString();
	}
	
	private void setKafkaConfiguration(){
		myLog.debug("setKafkaConfiguration. Start....");
		kafkaProps = new Properties();
		
        kafkaProps.put("bootstrap.servers", myProps.getProperty("BOOTSTRAP_SERVERS_CONFIG",""));
        kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
       
        topicName = myProps.getProperty("TOPIC_NAME","");
        
		myLog.debug("setKafkaConfiguration. " + kafkaProps.toString());
	}

}
