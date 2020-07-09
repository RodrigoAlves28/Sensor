package inserirNoSQL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import org.json.JSONObject;

public class AlertaHumidade {
	
	private Statement myStatement;
	private EstadoSistema estado;
	private LinkedList<Medicao> buffer = new LinkedList<Medicao>();
	private LinkedList<Medicao> medicoes = new LinkedList<Medicao>();
	private double medicao;
	private int contador;
	private double humLimite;
	public static final double DESVIO_MEDIA = 3;
	public static final int MEDICOES_SIZE = 10;
	public static final int BUFFER_SIZE = 5;
	public static final double DELTA_MEDICOES = 2;
	public static final int MEDICOES_ACIMA_LIMITE = 30;

	
	public AlertaHumidade(Statement myStatement) {
		this.myStatement = myStatement;
		estado = EstadoSistema.ESTAVEL;
	}



	public void verificarMedicaoHumidade(JSONObject json) throws SQLException{
		String humidade = json.optString("hum");
		String data = json.optString("dat");
		String hora = json.optString("tim");
		try {
			String[] datavector = data.split("/");
			data = datavector[2] + "-" + datavector[1] + "-" + datavector[0];
		} catch (Exception e) {
		}
		String dataHora = "'" + data + " " + hora + "'";
		medicao = Double.parseDouble(humidade);
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
		ResultSet rs = myStatement.executeQuery("SELECT LimiteHumidade from sistema");
		rs.absolute(1);
		humLimite = rs.getDouble("LimiteHumidade");
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

	public void enviarAlertaSubidaHumidade() throws SQLException {
		if (estado == EstadoSistema.ESTAVEL) {
//			System.out.println("entrei no primeiro if");
			double diferenca_hum = 0;
			if (medicoes.size() == MEDICOES_SIZE) {
//				System.out.println("entrei no segundo if");
				diferenca_hum = medicoes.peekLast().getValor() - medicoes.get(0).getValor();
				if (diferenca_hum >= DELTA_MEDICOES) {
//					System.out.println("entrei no 3 if");
					estado = EstadoSistema.SUBIDA;
					myStatement.executeUpdate("insert into alerta" + " values(0" + ","
							+ medicoes.peekLast().getDataHora() + ",'hum'," + medicoes.peekLast().getValor() + ","
							+ humLimite + ",'O sistema detetou que " + "houve uma subida da humidade',"
							+ estado.getNivel() + ", '" + estado.getTituloHum() + "')");
//					System.out.println("enviou alerta subida");
				}
			}
		}
	}

	public void enviarAlertaPertoLimite() throws SQLException {
		if (estado == EstadoSistema.ESTAVEL || estado == EstadoSistema.SUBIDA) {
				if (!medicoes.isEmpty() && medicoes.peekLast().getValor() >= humLimite - 10) {
					estado = EstadoSistema.PERTO_LIMITE;
					myStatement.executeUpdate(
							"insert into alerta" + " values(0" + "," + medicoes.peekLast().getDataHora() + ",'hum'," + medicoes.peekLast().getValor() + ","
									+ humLimite + ",'O sistema detetou que " + "a humidade está perto do limite',"
									+ estado.getNivel() + ", '" + estado.getTituloHum() + "')");
//					System.out.println("enviou alerta perto limite");
				}
		}				
	}

	public void enviarAlertaAcimaLimite() throws SQLException {
		if(estado == EstadoSistema.ACIMA_LIMITE)
			contador++;
		if (estado == EstadoSistema.PERTO_LIMITE || contador==MEDICOES_ACIMA_LIMITE) {
				if (medicoes.peekLast().getValor() >= humLimite) {
					estado = EstadoSistema.ACIMA_LIMITE;
					myStatement.executeUpdate(
							"insert into alerta" + " values(0" + "," + medicoes.peekLast().getDataHora() + ",'hum'," + medicoes.peekLast().getValor() + ","
									+ humLimite + ",'O sistema detetou que " + "a humidade está acima do limite',"
									+ estado.getNivel() + ", '" + estado.getTituloHum() + "')");
					contador=0;
//					System.out.println("enviou alerta acima limite");
			}
		}
	}
	
	public void enviarAlertaDescidaLimite() throws SQLException {
		if(estado == EstadoSistema.ACIMA_LIMITE)
				if (medicoes.peekLast().getValor() < humLimite) {
					estado = EstadoSistema.PERTO_LIMITE;
					myStatement.executeUpdate(
							"insert into alerta" + " values(0" + "," + medicoes.peekLast().getDataHora() + ",'hum'," + medicoes.peekLast().getValor() + ","
									+ humLimite + ",'O sistema detetou que a humidade está abaixo mas perto do limite',"
									+ estado.getNivel() + ", '" + estado.getTituloHum() + "')");
					contador=0;
//					System.out.println("enviou alerta descida limite dps de tar acima");
					
			}
		if (estado == EstadoSistema.PERTO_LIMITE) {
				if (medicoes.peekLast().getValor() < humLimite-10) {
					estado = EstadoSistema.ESTAVEL;
					myStatement.executeUpdate(
							"insert into alerta" + " values(0" + "," + medicoes.peekLast().getDataHora() + ",'hum'," + medicoes.peekLast().getValor() + ","
									+ humLimite + ",'O sistema detetou que a humidade voltou ao normal',"
									+ estado.getNivel() + ", '" + estado.getTituloHum() + "')");
					contador=0;
//					System.out.println("enviou alerta descida perto limite");
			}
		}
	}

	public void enviarAlerta() throws SQLException {
//		System.out.println("entrei na funçao enviar alerta");
		enviarAlertaSubidaHumidade();
		enviarAlertaPertoLimite();
		enviarAlertaAcimaLimite();
		enviarAlertaDescidaLimite();
	}
}
