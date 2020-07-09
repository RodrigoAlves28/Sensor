package baseDadosMongo;

import java.io.FileInputStream;
import java.util.Properties;
import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;

import main.Main;

public class BaseDadosMongo {

	private MongoCollection<Document> collection;
	private String mongo_host = new String();
	private String mongo_database = new String();
	private String mongo_collection = new String();
	private MongoDatabase db;

	public BaseDadosMongo() {
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(Main.CONFIG_INI_PATH + "CloudToMongo.ini"));
			mongo_host = p.getProperty("mongo_host");
			mongo_database = p.getProperty("mongo_database");
			mongo_collection = p.getProperty("data_collection");
		} catch (Exception e) {
			System.out.println("Erro: ficheiro.ini\n");
		}
	}

	public void estabelecerLigacao(MongoClient client) {
		this.db = client.getDatabase(mongo_database);
		try {
			CreateCollectionOptions options = new CreateCollectionOptions();
			options.capped(true);
			options.sizeInBytes(2000000000l);
			this.db.createCollection(mongo_collection, options);
			this.collection = db.getCollection(mongo_collection);
		} catch (Exception e) {
			this.collection = db.getCollection(mongo_collection);
		}
	}

	public String getMongo_host() {
		return mongo_host;
	}
	
	public MongoCollection<Document> getCollection() {
		return collection;
	}
}