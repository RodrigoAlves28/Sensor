package escutarMongo;

import org.bson.Document;
import com.mongodb.CursorType;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import baseDadosMongo.BaseDadosMongo;
import inserirNoSQL.InserirSQL;

public class EscutarMongo {

	private BaseDadosMongo mongodb;
	private MongoClient mongoClient;
	private InserirSQL inserirSQL;

	public EscutarMongo(BaseDadosMongo mongodb, InserirSQL inserirSQL) {
		this.mongodb = mongodb;
		this.inserirSQL = inserirSQL;
		conectarMongo();
		escutar();
	}

	public void conectarMongo() {
		try {
			String mongo_host = this.mongodb.getMongo_host();
			mongoClient = new MongoClient(new MongoClientURI(mongo_host));
			this.mongodb.estabelecerLigacao(mongoClient);
		} catch (Exception e) {
			System.out.println("Erro: Conectar Base Dados Mongo");
		}
	}

	public void escutar() {
		try {
			MongoCollection<Document> mongocol = this.mongodb.getCollection();
			MongoCursor<Document> cursor = mongocol.find().cursorType(CursorType.TailableAwait).iterator();
			ignorarDocumentosJaExistentesNoMongo(cursor, mongocol);
			while (cursor.hasNext()) {
				this.inserirSQL.escreverNoSQL(cursor.next().toJson());
			}
		} catch (Exception e) {
			System.out.println("Erro: Escutar Mongo");
		}
	}

	private void ignorarDocumentosJaExistentesNoMongo(MongoCursor<Document> cursor, MongoCollection<Document> mongocol) throws Exception {
		long numeroDocumentosExistentes = mongocol.countDocuments();
		for (long i = 0; i < numeroDocumentosExistentes; i++) {
			cursor.next();
		}
	}
}