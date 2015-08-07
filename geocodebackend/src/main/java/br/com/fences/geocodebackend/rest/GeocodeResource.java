package br.com.fences.geocodebackend.rest;

import java.io.InputStream;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import br.com.fences.fencesutils.conversor.InputStreamParaJson;
import br.com.fences.geocodebackend.cadastro.negocio.EnderecoBO;
import br.com.fences.geocodeentidade.geocode.Endereco;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@RequestScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GeocodeResource {
	
	@Inject 
	private EnderecoBO enderecoBO;
	
	private Gson gson = new GsonBuilder().create();
	
    @POST
    @Path("cacheGeocode/consultar")
    public String cadastroCacheGeocodeConsultar(InputStream ipFiltros)
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Endereco endereco = gson.fromJson(json, Endereco.class);
    	endereco = enderecoBO.cacheGeocode(endereco); 
    	json = gson.toJson(endereco);
    	return json;
    }

    @POST
    @Path("cacheGeocode/adicionarCasoNaoExista")
    public void cadastroCacheAdicionarCasoNaoExista(InputStream ipFiltros)
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Endereco endereco = gson.fromJson(json, Endereco.class);
    	enderecoBO.adicionarCasoNaoExista(endereco);
    }
    
}
