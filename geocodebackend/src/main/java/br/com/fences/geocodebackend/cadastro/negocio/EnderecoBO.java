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
	private EnderecoDAO enderecoDAO;
	 
	public Endereco cacheGeocode(Endereco enderecoFiltro)
	{
		Endereco endereco = null;
		Endereco enderecoComGeocode = enderecoDAO.consultar(enderecoFiltro);
		if (enderecoComGeocode == null)
		{
			String enderecoFormatado = formatarEndereco(enderecoFiltro);
			
			Geocode geocode = consultarGeocode(enderecoFormatado);

			endereco = enderecoFiltro;
			endereco.setLatitude(geocode.getLatitude());
			endereco.setLongitude(geocode.getLongitude());
			endereco.setGeocodeStatus(geocode.getGeocoderStatus());
			
			enderecoDAO.adicionar(endereco);
		}
		else
		{
			if (enderecoComGeocode.getGeocodeStatus().equalsIgnoreCase("OK"))
			{
				endereco = enderecoFiltro;
				endereco.setLatitude(enderecoComGeocode.getLatitude());
				endereco.setLongitude(enderecoComGeocode.getLongitude());
				endereco.setGeocodeStatus(enderecoComGeocode.getGeocodeStatus());
				endereco.setUltimaAtualizacao(enderecoComGeocode.getUltimaAtualizacao());
			}
			else
			{
				String enderecoFormatado = formatarEndereco(enderecoFiltro);
				
				Geocode geocode = consultarGeocode(enderecoFormatado);

				endereco = enderecoFiltro;
				endereco.setLatitude(geocode.getLatitude());
				endereco.setLongitude(geocode.getLongitude());
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
						Float latitude = results.get(0).getGeometry().getLocation().getLat().floatValue();
					    Float longitude = results.get(0).getGeometry().getLocation().getLng().floatValue();
					    geocode = new Geocode(latitude.toString(), longitude.toString(), geocoderResponse.getStatus().value());
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
				}
				if (geocoderStatus.equals(GeocoderStatus.ZERO_RESULTS))
				{
					//throw new RestRuntimeException(2, "Endereço sem retorno do Google.[ZERO_RESULTS]");
				}
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
		private String latitude;
		private String longitude;
		private String geocoderStatus;
		public Geocode(){}
		public Geocode(String latitude, String longitude, String geocoderStatus) {
			super();
			this.latitude = latitude;
			this.longitude = longitude;
			this.geocoderStatus = geocoderStatus;
		}
		public String getLatitude() {
			return latitude;
		}
		public void setLatitude(String latitude) {
			this.latitude = latitude;
		}
		public String getLongitude() {
			return longitude;
		}
		public void setLongitude(String longitude) {
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
