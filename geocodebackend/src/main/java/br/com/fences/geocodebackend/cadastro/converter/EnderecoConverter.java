package br.com.fences.geocodebackend.cadastro.converter;

import javax.enterprise.context.ApplicationScoped;

import org.bson.Document;

import br.com.fences.fencesutils.conversor.mongodb.Converter;
import br.com.fences.geocodeentidade.geocode.Endereco;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@ApplicationScoped
public class EnderecoConverter extends Converter<Endereco>{

	private Gson gson = new GsonBuilder().create();
	
	@Override
	public Document paraDocumento(Endereco endereco) 
	{
    	String json = gson.toJson(endereco);
    	String jsonMongoDB = transformarIdParaJsonDb(json);
    	Document documento = Document.parse(jsonMongoDB);
		return documento;
	}

	@Override
	public Endereco paraObjeto(Document doc) 
	{
		String jsonMongoDB = doc.toJson();
    	String json = transformarIdParaJsonObj(jsonMongoDB);
    	Endereco endereco = gson.fromJson(json, Endereco.class);
    	return endereco;
	}	
}
