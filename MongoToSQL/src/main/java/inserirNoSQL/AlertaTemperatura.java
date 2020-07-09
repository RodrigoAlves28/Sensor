package inserirNoSQL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;

import org.json.JSONObject;

public class AlertaTemperatura {

	private Statement myStatement;
	private EstadoSistema estado;
	private LinkedList<Medicao> buffer = new LinkedList<Medicao>();
	private LinkedList<Medicao> medicoes = new LinkedList<Medicao>();
	private double medicao;
	private int contador;
	private double tmpLimite;
	public static final double DESVIO_MEDIA = 3;
	public static final int MEDICOES_SIZE = 10;
	public static final int BUFFER_SIZE = 5;
	public static final double DELTA_MEDICOES = 2;
	public static final int MEDICOES_ACIMA_LIMITE = 30;

	
	public AlertaTemperatura(Statement myStatement) {
		this.myStatement = myStatement;
		estado = EstadoSistema.ESTAVEL;
	}



	public void verificarMedicaoTemperatura(JSONObject json) throws SQLException{
		String temperatura = json.optString("tmp");
		String data = json.optString("dat");
		String hora = json.optString("tim");
		try {
			String[] datavector = data.split("/");
			data = datavector[2] + "-" + datavector[1] + "-" + datavector[0];
		} catch (Exception e) {
		}
		String dataHora = "'" + data + " " + hora + "'";
		medicao = Double.parseDouble(temperatura);
		Medicao m = new Medicao(medicao, dataHora);
		if (buffer.size() < BUFFER_SIZE)
			buffer.add(m);
		else {
			double media = calcularMedia(buffer);
			Medicao primeiraMedicao = buffer.remove();
			if (primeiraMedicao.getValor() <= media + DESVIO_MEDIA
					&& primeiraMedicao.getValor() >= media - DESVIO_MEDIA) {
				adicionarMedicoes(primeiraMedicao);
			}
			buffer.add(m);
		}
		ResultSet rs = myStatement.executeQuery("SELECT LimiteTemperatura from sistema");
		rs.absolute(1);
		tmpLimite = rs.getDouble("LimiteTemperatura");
		enviarAlerta();
//		System.out.println("buffer -> "+buffer);
//		System.out.println("medicoes -> "+medicoes);

	}

	public double calcularMedia(LinkedList<Medicao> lista) {
		double soma = 0;
		for (Medicao d : lista)
			soma += d.getValor();
		return soma / lista.size();
	}

	public void adicionarMedicoes(Medicao m) {
		if (medicoes.size() == MEDICOES_SIZE) {
			medicoes.remove();
			medicoes.add(m);
		} else
			medicoes.add(m);
	}

	public void enviarAlertaSubidaTemperatura() throws SQLException {
		if (estado == EstadoSistema.ESTAVEL) {
//			System.out.println("entrei no primeiro if");
			double diferenca_temp = 0;
			if (medicoes.size() == MEDICOES_SIZE) {
//				System.out.println("entrei no segundo if");
				diferenca_temp = medicoes.peekLast().getValor() - medicoes.get(0).getValor();
				if (diferenca_temp >= DELTA_MEDICOES) {
					System.out.println("entrei no 3 if");
					estado = EstadoSistema.SUBIDA;
					myStatement.executeUpdate("insert into alerta" + " values(0" + ","
							+ medicoes.peekLast().getDataHora() + ",'tmp'," + medicoes.peekLast().getValor() + ","
							+ tmpLimite + ",'O sistema detetou que " + "houve uma subida da temperatura',"
							+ estado.getNivel() + ", '" + estado.getTituloTmp() + "')");
//					System.out.println("enviou alerta subida");
				}
			}
		}
	}

	public void enviarAlertaPertoLimite() throws SQLException {
		if (estado == EstadoSistema.ESTAVEL || estado == EstadoSistema.SUBIDA) {
				if (!medicoes.isEmpty() && medicoes.peekLast().getValor() >= tmpLimite - 10) {
					estado = EstadoSistema.PERTO_LIMITE;
					myStatement.executeUpdate(
							"insert into alerta" + " values(0" + "," + medicoes.peekLast().getDataHora() + ",'tmp'," + medicoes.peekLast().getValor() + ","
									+ tmpLimite + ",'O sistema detetou que " + "a temperatura está perto do limite',"
									+ estado.getNivel() + ", '" + estado.getTituloTmp() + "')");
//					System.out.println("enviou alerta perto limite");
				}
		}				
	}

	public void enviarAlertaAcimaLimite() throws SQLException {
		if(estado == EstadoSistema.ACIMA_LIMITE)
			contador++;
		if (estado == EstadoSistema.PERTO_LIMITE || contador==MEDICOES_ACIMA_LIMITE) {
				if (medicoes.peekLast().getValor() >= tmpLimite) {
					estado = EstadoSistema.ACIMA_LIMITE;
					myStatement.executeUpdate(
							"insert into alerta" + " values(0" + "," + medicoes.peekLast().getDataHora() + ",'tmp'," + medicoes.peekLast().getValor() + ","
									+ tmpLimite + ",'O sistema detetou que " + "a temperatura está acima do limite',"
									+ estado.getNivel() + ", '" + estado.getTituloTmp() + "')");
					contador=0;
//					System.out.println("enviou alerta acima limite");
			}
		}
	}
	
	public void enviarAlertaDescidaLimite() throws SQLException {
		if(estado == EstadoSistema.ACIMA_LIMITE)
				if (medicoes.peekLast().getValor() < tmpLimite) {
					estado = EstadoSistema.PERTO_LIMITE;
					myStatement.executeUpdate(
							"insert into alerta" + " values(0" + "," + medicoes.peekLast().getDataHora() + ",'tmp'," + medicoes.peekLast().getValor() + ","
									+ tmpLimite + ",'O sistema detetou que a temperatura está abaixo mas perto do limite',"
									+ estado.getNivel() + ", '" + estado.getTituloTmp() + "')");
					contador=0;
//					System.out.println("enviou alerta descida limite dps de tar acima");
					
			}
		if (estado == EstadoSistema.PERTO_LIMITE) {
				if (medicoes.peekLast().getValor() < tmpLimite-10) {
					estado = EstadoSistema.ESTAVEL;
					myStatement.executeUpdate(
							"insert into alerta" + " values(0" + "," + medicoes.peekLast().getDataHora() + ",'tmp'," + medicoes.peekLast().getValor() + ","
									+ tmpLimite + ",'O sistema detetou que a temperatura voltou ao normal',"
									+ estado.getNivel() + ", '" + estado.getTituloTmp() + "')");
					contador=0;
//					System.out.println("enviou alerta descida perto limite");
			}
		}
	}

	public void enviarAlerta() throws SQLException {
//		System.out.println("entrei na funçao enviar alerta");
		enviarAlertaSubidaTemperatura();
		enviarAlertaPertoLimite();
		enviarAlertaAcimaLimite();
		enviarAlertaDescidaLimite();
	}
}
