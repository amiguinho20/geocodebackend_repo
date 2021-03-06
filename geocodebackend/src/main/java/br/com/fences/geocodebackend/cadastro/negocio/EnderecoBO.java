package br.com.fences.geocodebackend.cadastro.negocio;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import br.com.fences.fencesutils.rest.tratamentoerro.exception.RestRuntimeException;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.geocodebackend.cadastro.dao.EnderecoDAO;
import br.com.fences.geocodeentidade.geocode.Endereco;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import com.google.code.geocoder.model.GeocoderStatus;


@RequestScoped
public class EnderecoBO {
	
	private static final String IDIOMA = "pt";
	private static final Geocoder geocoder = new Geocoder();
	
	@Inject
	private TemporizadorCacheSingleton temporizador;
	
	@Inject
	private EnderecoDAO enderecoDAO;
	
	public void adicionarCasoNaoExista(Endereco endereco)
	{
		Double longitude = endereco.getGeometry().getLongitude();
		Double latitude = endereco.getGeometry().getLatitude();
		Endereco enderecoResultado = enderecoDAO.consultar(longitude,latitude);
		if (enderecoResultado == null)
		{
			endereco.setGeocodeStatus("OK");
			colocarEmMaiusculoComRetiradaDeAcentos(endereco);
			enderecoDAO.adicionar(endereco);
		}
	}
	 
	public Endereco cacheGeocode(Endereco enderecoFiltro)
	{
		Endereco endereco = null;
		colocarEmMaiusculoComRetiradaDeAcentos(enderecoFiltro);
		Endereco enderecoComGeocode = enderecoDAO.consultar(enderecoFiltro);
		if (enderecoComGeocode == null)
		{
			String enderecoFormatado = formatarEndereco(enderecoFiltro);
			
			Geocode geocode = consultarGeocode(enderecoFormatado);

			endereco = enderecoFiltro;
			endereco.getGeometry().setLngLat(geocode.getLongitude(), geocode.getLatitude());
			endereco.setGeocodeStatus(geocode.getGeocoderStatus());
			
			enderecoDAO.adicionar(endereco);
		}
		else
		{
			if (enderecoComGeocode.getGeocodeStatus().equalsIgnoreCase("OK"))
			{
				endereco = enderecoFiltro;
				endereco.setGeometry(enderecoComGeocode.getGeometry());
				endereco.setGeocodeStatus(enderecoComGeocode.getGeocodeStatus());
				endereco.setUltimaAtualizacao(enderecoComGeocode.getUltimaAtualizacao());
			}
			else
			{
				String enderecoFormatado = formatarEndereco(enderecoFiltro);
				
				Geocode geocode = consultarGeocode(enderecoFormatado);

				endereco = enderecoFiltro;
				endereco.getGeometry().setLngLat(geocode.getLongitude(), geocode.getLatitude());
				endereco.setGeocodeStatus(geocode.getGeocoderStatus());
				endereco.setUltimaAtualizacao(enderecoComGeocode.getUltimaAtualizacao());
				
				enderecoDAO.substituir(endereco);
			}
			
		}
		
		return endereco;
	}
	
	
	private Geocode consultarGeocode(String endereco)
	{
		int qtdTentativa = 0;
		boolean sucesso = false;
		GeocoderStatus geocoderStatus = null;
		Geocode geocode = new Geocode();
		try
		{
			if (temporizador.permiteConsultarGeocode())
			{
				while (!sucesso && qtdTentativa < 3)
				{
					qtdTentativa++;
	
					GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(endereco).setLanguage(IDIOMA).getGeocoderRequest();
					GeocodeResponse geocoderResponse = geocoder.geocode(geocoderRequest);
	
					geocoderStatus = geocoderResponse.getStatus();
					geocode.setGeocoderStatus(geocoderStatus.value());
					if (!geocoderStatus.equals(GeocoderStatus.OVER_QUERY_LIMIT))
					{
						sucesso = true;
						
						List<GeocoderResult> results = geocoderResponse.getResults(); 
						if (Verificador.isValorado(results))
						{
							double latitude = results.get(0).getGeometry().getLocation().getLat().doubleValue();
						    double longitude = results.get(0).getGeometry().getLocation().getLng().doubleValue();
						    geocode = new Geocode(latitude, longitude, geocoderResponse.getStatus().value());
						}
						else
						{
							//logger.info("consultarGeocode: geocode erro[" + geocoderStatus + "] para o endereco [" + endereco + "].");
						}
					}
					else
					{
						Thread.sleep(3000); //-- aguardar 3 segundos 
					}
				}
				if (!geocoderStatus.equals(GeocoderStatus.OK))
				{
					geocode.setGeocoderStatus(geocoderStatus.value());
					if (geocoderStatus.equals(GeocoderStatus.OVER_QUERY_LIMIT))
					{
						//throw new RestRuntimeException(1, "Limite de 2500 pesquisas diárias foi atingido. [OVER_QUERY_LIMIT]");
						temporizador.registrarTempoDoUltimoQueryLimit();
					}
					if (geocoderStatus.equals(GeocoderStatus.ZERO_RESULTS))
					{
						//throw new RestRuntimeException(2, "Endereço sem retorno do Google.[ZERO_RESULTS]");
					}
				}
			}
			else
			{
				geocode.setGeocoderStatus(GeocoderStatus.OVER_QUERY_LIMIT.name());
			}
		}
		catch(Exception e)
		{
			throw new RestRuntimeException(3, "Erro no processamento do Geocode : " + e.getMessage());
		}
		return geocode;
	}
	
	public String formatarEndereco(Endereco endereco)
	{
		String formatado = concatenarEndereco(endereco.getLogradouro(),
				endereco.getNumero(),
				endereco.getBairro(), endereco.getCidade(),
				endereco.getUf());
		formatado = retirarAcentos(formatado);
		return formatado;
	}
	
	public String retirarAcentos(String arg)
	{
	    String normalizador = Normalizer.normalize(arg, Normalizer.Form.NFD);
	    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	    String retorno = pattern.matcher(normalizador).replaceAll("");
	    return retorno;
	}
	
	
	public void colocarEmMaiusculoComRetiradaDeAcentos(Endereco endereco)
	{
		if (Verificador.isValorado(endereco.getLogradouro()))
		{
			endereco.setLogradouro(retirarAcentos(endereco.getLogradouro().toUpperCase()));
		}
		if (Verificador.isValorado(endereco.getNumero()))
		{
			endereco.setNumero(retirarAcentos(endereco.getNumero().toUpperCase()));
		}
		if (Verificador.isValorado(endereco.getComplemento()))
		{
			endereco.setComplemento(retirarAcentos(endereco.getComplemento().toUpperCase()));
		}
		if (Verificador.isValorado(endereco.getBairro()))
		{
			endereco.setBairro(retirarAcentos(endereco.getBairro().toUpperCase()));
		}
		if (Verificador.isValorado(endereco.getCidade()))
		{
			endereco.setCidade(retirarAcentos(endereco.getCidade().toUpperCase()));
		}
		if (Verificador.isValorado(endereco.getUf()))
		{
			endereco.setUf(retirarAcentos(endereco.getUf().toUpperCase()));
		}
	}
	
	public String concatenarEndereco(String... campos) 
	{
		String resultado = "";
		for (String campo : campos) 
		{
			if (Verificador.isValorado(campo) && !campo.trim().equals("0"))
			{
				campo = campo.replaceAll(",", ""); //-- retirar virgulas adicionais
				if (!resultado.isEmpty())
				{
					resultado += ", ";
				} 
				resultado += campo.trim(); 					
			}
		}
		return resultado;
	}
	
	private class Geocode{
		private Double latitude;
		private Double longitude;
		private String geocoderStatus;
		public Geocode(){}
		public Geocode(Double latitude, Double longitude, String geocoderStatus) {
			super();
			this.latitude = latitude;
			this.longitude = longitude;
			this.geocoderStatus = geocoderStatus;
		}
		public Double getLatitude() {
			return latitude;
		}
		public void setLatitude(Double latitude) {
			this.latitude = latitude;
		}
		public Double getLongitude() {
			return longitude;
		}
		public void setLongitude(Double longitude) {
			this.longitude = longitude;
		}
		public String getGeocoderStatus() {
			return geocoderStatus;
		}
		public void setGeocoderStatus(String geocoderStatus) {
			this.geocoderStatus = geocoderStatus;
		}
		
	}

}
