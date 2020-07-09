package lerSensores;

import org.bson.Document;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;

import main.Main;

import java.util.*;
import java.io.*;

public class LerSensores implements MqttCallback {
	MqttClient mqttclient;
	static MongoClient mongoClient;
	static MongoDatabase db;
	static MongoCollection<Document> mongocol;
	static MongoCollection<Document> dumpcol;
	static String cloud_server = new String();
	static String cloud_topic = new String();
	static String mongo_host = new String();
	static String mongo_database = new String();
	static String data_collection = new String();
	static String dump_collection = new String();

	public static void main(String[] args) {
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(Main.CONFIG_INI_PATH + "CloudToMongo.ini"));
			cloud_server = p.getProperty("cloud_server");
			cloud_topic = p.getProperty("cloud_topic");
			mongo_host = p.getProperty("mongo_host");
			mongo_database = p.getProperty("mongo_database");
			data_collection = p.getProperty("data_collection");
			dump_collection = p.getProperty("dump_collection");
			new LerSensores().connecCloud();
			new LerSensores().connectMongo();
		} catch (Exception e) {
			System.out.println("Erro: LerSensores Main");
		}
	}

	public void connecCloud() {
		int i;
		try {
			i = new Random().nextInt(100000);
			mqttclient = new MqttClient(cloud_server, "CloudToMongo_" + String.valueOf(i) + "_" + cloud_topic);
			mqttclient.connect();
			mqttclient.setCallback(this);
			mqttclient.subscribe(cloud_topic);
		} catch (MqttException e) {
			System.out.println("Erro: Coneccao Cloud");
		}
	}

	public void connectMongo() {
		try {
			mongoClient = new MongoClient(new MongoClientURI(mongo_host));
			db = mongoClient.getDatabase(mongo_database);
			getColeccoes();
		} catch (Exception e) {
			System.out.println("Erro: Coneccao Mongo");
		}
	}

	private void getColeccoes() {
		try {
			dumpcol = db.getCollection(dump_collection);
			CreateCollectionOptions options = new CreateCollectionOptions();
			options.capped(true);
			options.sizeInBytes(2000000000l);
			db.createCollection(data_collection, options);
			mongocol = db.getCollection(data_collection);
		} catch (Exception e) {
			mongocol = db.getCollection(data_collection);
			dumpcol = db.getCollection(dump_collection);
		}
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String payload = new String(message.getPayload());
		payload = payload.replaceAll("[^A-Za-z0-9,./:{}\"]", "");
		System.out.println(payload);

		// limpar os erros dos sensores do professor
//		payload = limparErros(payload);

		try {
			JSONObject jsonmsg = new JSONObject(payload);
			Document doc = Document.parse(jsonmsg.toString());
			Filtrar filtro = new Filtrar(mongocol,dumpcol,jsonmsg);
			filtro.executar();
//			
		} catch (Exception e) {}
	}
	

	


	public String limparErros(String payload) {
		String old = '"' + "mov" + '"' + ':' + '"' + "0" + '"';
		payload = payload.replaceFirst(old, ",");
		old = '"' + "" + '"';
		String replace = '"' + "," + '"';
		payload = payload.replaceFirst(old, replace);
		return payload;
	}

	public void connectionLost(Throwable cause) {
	}
	
	

	public void deliveryComplete(IMqttDeliveryToken token) {
	}
}