package inserirNoSQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.json.JSONObject;

public class InserirSQL {

	private static String USERNAME = "migMongoSql";
	private static String PASSWORD = "migrador";
	private Connection myConnection;
	private Statement myStatement;
	private AlertaTemperatura alertaTemperatura;
	private AlertaHumidade alertaHumidade;

	public InserirSQL() {
		try {
			myConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/museu2", USERNAME, PASSWORD);
			myStatement = this.myConnection.createStatement();
			alertaTemperatura = new AlertaTemperatura(myStatement);
			alertaHumidade = new AlertaHumidade(myStatement);
		} catch (Exception e) {
			System.out.println("Erro InserirSQL");
			e.printStackTrace();
		}
	}

	public void escreverNoSQL(String json) {
		try {
			enviarMedicoesParaSQL(new JSONObject(json));
			alertaTemperatura.verificarMedicaoTemperatura(new JSONObject(json));
			alertaHumidade.verificarMedicaoHumidade(new JSONObject(json));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void enviarMedicoesParaSQL(JSONObject json) {
		String humidade = json.optString("hum");
		String temperatura = json.optString("tmp");
		String luz = json.optString("cell");
		String movimento = json.optString("mov");
		String data = json.optString("dat");
		String hora = json.optString("tim");
		int hour = Integer.parseInt(hora.split(":")[0]+1);
		String[]horaArray = hora.split(":");
		String horaNew = String.valueOf(hour) + ":" + horaArray[1] + ":" + horaArray[2];
		try {
			String[] datavector = data.split("/");
			data = datavector[2] + "-" + datavector[1] + "-" + datavector[0];
		} catch (Exception e) {
			e.printStackTrace();
		}

		String dataHora = "'" + data + " " + horaNew + "'";
		if (!humidade.equals(""))
			inserirMedicaoNoSQL(humidade, "'" + "hum" + "'", dataHora);
		if (!temperatura.equals(""))
			inserirMedicaoNoSQL(temperatura, "'" + "tmp" + "'", dataHora);
		if (!movimento.equals(""))
			inserirMedicaoNoSQL(movimento, "'" + "mov" + "'", dataHora);
		if (!luz.equals(""))
			inserirMedicaoNoSQL(luz, "'" + "luz" + "'", dataHora);
	}

	public void inserirMedicaoNoSQL(String valor, String tipo, String dataHora) {
		try {
			myStatement.executeUpdate("insert into medicoessensores values(" + "0" + "," + Double.parseDouble(valor)
					+ "," + tipo + "," + dataHora + ")");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}