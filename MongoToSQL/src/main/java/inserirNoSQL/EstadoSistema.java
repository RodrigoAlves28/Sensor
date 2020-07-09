package inserirNoSQL;

public enum EstadoSistema {
	
	ESTAVEL(0,"Temperatura normal","Humidade normal"),SUBIDA(1,"Subida de temperatura","Subida de humidade"),
	PERTO_LIMITE(2,"Temperatura próxima do limite","Humidade próxima do limite"),ACIMA_LIMITE(3,"Temperatura acima do limite","Humidade acima do limite");
	
	private int nivel_alerta;
	private String titulo_alerta_tmp;
	private String titulo_alerta_hum;
	
	EstadoSistema(int nivel_alerta, String titulo_alerta_tmp, String titulo_alerta_hum){
		this.nivel_alerta=nivel_alerta;
		this.titulo_alerta_tmp=titulo_alerta_tmp;
		this.titulo_alerta_hum=titulo_alerta_hum;
		
	}
	
	public int getNivel() {
		return nivel_alerta;
	}
	
	public String getTituloTmp() {
		return titulo_alerta_tmp;
	}
	
	public String getTituloHum() {
		return titulo_alerta_hum;
	}

}
