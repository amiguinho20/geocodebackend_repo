package br.com.fences.geocodebackend.cadastro.dao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.bson.Document;

import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.geocodebackend.config.AppConfig;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@ApplicationScoped
public class MongoProvider {

	private static final String COLECAO_CADASTRO_ENDERECO = "cadastro_endereco";
	
	private MongoClient conexao;
	private MongoDatabase banco;
	private MongoCollection<Document> colecaoCadastroEndereco;
	
	@Inject
	private AppConfig appConfig;
	
	@PostConstruct
	public void abrirConexao() 
	{
		String dbMongoHost = appConfig.getDbMongoHost();
		String dbMongoPort = appConfig.getDbMongoPort();
		String dbMongoDatabase = appConfig.getDbMongoDatabase();
		String dbMongoUser = appConfig.getDbMongoUser();
		String dbMongoPass = appConfig.getDbMongoPass();
		
		if (Verificador.isValorado(dbMongoUser))
		{
			String uriConexao = String.format("mongodb://%s:%s@%s:%s/%s", dbMongoUser, dbMongoPass, dbMongoHost, dbMongoPort, dbMongoDatabase);
			MongoClientURI uri  = new MongoClientURI(uriConexao); 
			conexao = new MongoClient(uri);
		}
		else
		{
			conexao = new MongoClient(dbMongoHost, Integer.parseInt(dbMongoPort));
		}
		banco = conexao.getDatabase(dbMongoDatabase);
		

		colecaoCadastroEndereco = banco.getCollection(COLECAO_CADASTRO_ENDERECO);
		if (colecaoCadastroEndereco == null)
		{
			banco.createCollection(COLECAO_CADASTRO_ENDERECO);
			colecaoCadastroEndereco = banco.getCollection(COLECAO_CADASTRO_ENDERECO);
		}
		
	
	}
	
	/**
	 * Fechar a conexao com o banco quando o objeto for destruido.
	 */
	@PreDestroy
	public void fecharConecao()
	{
		conexao.close();
	}
	
	@Produces @ColecaoEndereco
	public MongoCollection<Document> getColecaoCadastroEndereco()
	{
		return colecaoCadastroEndereco;
	}
	
}
