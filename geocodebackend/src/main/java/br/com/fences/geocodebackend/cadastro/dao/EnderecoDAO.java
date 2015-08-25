package br.com.fences.geocodebackend.cadastro.dao;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.New;
import javax.inject.Inject;
import javax.inject.Named;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import br.com.fences.fencesutils.conversor.AcentuacaoParaRegex;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.formatar.FormatarData;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.geocodeentidade.geocode.Endereco;

@Named
@ApplicationScoped
public class EnderecoDAO {     

	
	@Inject
	private Converter<Endereco> converter;
	
	@Inject @ColecaoEndereco
	private MongoCollection<Document> colecao;
	
	
	/**
	 * Consulta pelo id (identificador unico), o "_id"
	 * @param id
	 */
	public Endereco consultar(final String id)
	{
	    Document documento = colecao.find(eq("_id", new ObjectId(id))).first();
	    Endereco endereco = converter.paraObjeto(documento, Endereco.class);
	    return endereco;
	}
	
	public Endereco consultar(double longitude, double latitude)
	{
		//BasicDBObject pesquisa = new BasicDBObject();
		
		//and("geometry", and(eq("coordinates.0", longitude), eq("coordinates.1", latitude)));

		BasicDBObject pesquisa = new BasicDBObject();
		pesquisa.append("geometry.coordinates.0", longitude);
		pesquisa.append("geometry.coordinates.1", latitude);
		
		Document documento = colecao.find(pesquisa).first();
		Endereco endereco = null;
		if (documento != null)
		{
			endereco = converter.paraObjeto(documento, Endereco.class);
		}
		return endereco;
	}
	
	public Endereco consultar(Endereco enderecoFiltro)
	{
		BasicDBObject pesquisa = montarPesquisaEnderecoExato(enderecoFiltro);
		Document documento = colecao.find(pesquisa).first();
		Endereco enderecoPesquisado = null;
		if (documento == null)
		{
			pesquisa = montarPesquisaEnderecoRegex(enderecoFiltro);
			documento = colecao.find(pesquisa).first();
		}
		if (documento != null)
		{
			enderecoPesquisado = converter.paraObjeto(documento, Endereco.class);
		}
		return enderecoPesquisado;
	}
	
	/**
	 * Contagem de pesquisa 
	 * @param pesquisa
	 * @return count
	 */
	public int contar(final Map<String, String> filtros)
	{
		BasicDBObject dbFiltros = montarPesquisa(filtros);
	    long countL = colecao.count(dbFiltros);
	    int countI = (int) countL;
	    return countI;
	}	
	
	/**
	 * Pesquisa com <b>PAGINACAO</b>
	 * @param pesquisa
	 * @param primeiroRegistro
	 * @param registrosPorPagina
	 * @return List<EnderecoAvulso> paginado
	 */
	public List<Endereco> pesquisarLazy(final  Map<String, String> filtros, final int primeiroRegistro, final int registrosPorPagina)
	{
		List<Endereco> enderecos = new ArrayList<>();
		
		BasicDBObject dbFiltros = montarPesquisa(filtros);
	    MongoCursor<Document> cursor = colecao.find(dbFiltros).skip(primeiroRegistro).limit(registrosPorPagina).iterator();

		
	    try {
	        while (cursor.hasNext()) {
	        	Document documento = cursor.next();
	        	Endereco endereco = converter.paraObjeto(documento, Endereco.class);
	        	enderecos.add(endereco);
	        }
	    } finally {
	        cursor.close();
	    }
	    
	    return enderecos;
	}
	
	
	/**
	 * Substitui (replace) o enderecoAvulso pelo id
	 * @param ocorrencia
	 */
	public void substituir(Endereco endereco)
	{
		try
		{
			endereco.setUltimaAtualizacao(dataHoraCorrente());
			Document documento = converter.paraDocumento(endereco);
			colecao.replaceOne(eq("_id", documento.get("_id")), documento);
		}
		catch (Exception e)
		{
			String msg = "Erro na alteracao. log[" + endereco.getLogradouro() + "].";
			System.err.println(msg);
			e.printStackTrace();
			throw new RuntimeException(msg);
		}
	} 
	
	public void adicionar(Endereco endereco)
	{
		try
		{
			endereco.setUltimaAtualizacao(dataHoraCorrente());
			Document documento = converter.paraDocumento(endereco);
			colecao.insertOne(documento);
		}
		catch (Exception e)
		{
			String msg = "Erro na inclusao unica. log[" + endereco.getLogradouro() + "].";
			System.err.println(msg);
			e.printStackTrace();
			throw new RuntimeException(msg);
		}
	}
	
	public void adicionar(List<Endereco> enderecos)
	{
		try
		{
			List<Document> documentos = new ArrayList<>();
			for (Endereco endereco : enderecos)
			{
				endereco.setUltimaAtualizacao(dataHoraCorrente());
				Document documento = converter.paraDocumento(endereco);
				documentos.add(documento);
			}
			colecao.insertMany(documentos);
		}
		catch (Exception e)
		{
			String msg = "Erro na inclusão em lote. log[" + e.getMessage() + "].";
			System.err.println(msg);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public void remover(Endereco endereco)
	{
		try
		{
			Document documento = converter.paraDocumento(endereco);
			colecao.deleteOne(eq("_id", documento.get("_id")));
		}
		catch (Exception e)
		{
			String msg = "Erro na alteracao. log[" + endereco.getLogradouro() + "].";
			System.err.println(msg);
			e.printStackTrace();
			throw new RuntimeException(msg);
		}
	}
	
	private String dataHoraCorrente()
	{
		String ultimaAtualizacao = FormatarData.getAnoMesDiaHoraMinutoSegundoConcatenados().format(new Date());
		return ultimaAtualizacao; 
	}
	
	private BasicDBObject montarPesquisaEnderecoExato(Endereco endereco)
	{
		BasicDBObject pesquisa = new BasicDBObject();
		if (Verificador.isValorado(endereco.getLogradouro()))
		{
			pesquisa.append("logradouro", endereco.getLogradouro());
		}
		if (Verificador.isValorado(endereco.getNumero()))
		{
			int dezena = 10;
			int intervaloSuperior = 0;
			int intervaloInferior = 0;
			int numero = Integer.parseInt(endereco.getNumero());
			if (numero == 0)
			{
				intervaloSuperior = dezena;
			}
			else
			{
				if (numero % dezena == 0)
				{
					intervaloInferior = numero;
				}
				else
				{
					int resto = numero % dezena;
					intervaloInferior = numero - resto;
				}
				intervaloSuperior = intervaloInferior + dezena;
			}
			
			List<String> numeros = new ArrayList<>();
			for (int num = intervaloInferior ; num < intervaloSuperior ; num++)
			{
				numeros.add(Integer.toString(num));
			}
			pesquisa.put("numero", new BasicDBObject("$in", numeros));
			
			//-- normaliza
			endereco.setNumero(Integer.toString(numero));
		}
		if (Verificador.isValorado(endereco.getBairro()))
		{
			pesquisa.append("bairro", endereco.getBairro());
		}
		if (Verificador.isValorado(endereco.getCidade()))
		{
			pesquisa.append("cidade", endereco.getCidade());
		}
		if (Verificador.isValorado(endereco.getUf()))
		{
			pesquisa.append("uf", endereco.getUf());
		}
		return pesquisa;
	}

	
	private BasicDBObject montarPesquisaEnderecoRegex(Endereco endereco)
	{
		BasicDBObject pesquisa = new BasicDBObject();
		if (Verificador.isValorado(endereco.getLogradouro()))
		{
			String convertido = AcentuacaoParaRegex.converter(endereco.getLogradouro());
			pesquisa.append("logradouro", new BasicDBObject("$regex", convertido).append("$options", "i"));
		}
		if (Verificador.isValorado(endereco.getNumero()))
		{
			int dezena = 10;
			int intervaloSuperior = 0;
			int intervaloInferior = 0;
			int numero = Integer.parseInt(endereco.getNumero());
			if (numero == 0)
			{
				intervaloSuperior = dezena;
			}
			else
			{
				if (numero % dezena == 0)
				{
					intervaloInferior = numero;
				}
				else
				{
					int resto = numero % dezena;
					intervaloInferior = numero - resto;
				}
				intervaloSuperior = intervaloInferior + dezena;
			}
			
//			BasicDBObject intervalo = new BasicDBObject();
//			intervalo.put("$gte", intervaloInferior); //inclusive
//			intervalo.put("$lt", intervaloSuperior);  //exclusive
			List<String> numeros = new ArrayList<>();
			for (int num = intervaloInferior ; num < intervaloSuperior ; num++)
			{
				numeros.add(Integer.toString(num));
			}
			pesquisa.put("numero", new BasicDBObject("$in", numeros));
			
			//-- normaliza
			endereco.setNumero(Integer.toString(numero));
		}
		if (Verificador.isValorado(endereco.getBairro()))
		{
			String convertido = AcentuacaoParaRegex.converter(endereco.getBairro());
			pesquisa.append("bairro", new BasicDBObject("$regex", convertido).append("$options", "i"));
		}
		if (Verificador.isValorado(endereco.getCidade()))
		{
			String convertido = AcentuacaoParaRegex.converter(endereco.getCidade());
			pesquisa.append("cidade", new BasicDBObject("$regex", convertido).append("$options", "i"));
		}
		if (Verificador.isValorado(endereco.getUf()))
		{
			String convertido = AcentuacaoParaRegex.converter(endereco.getUf());
			pesquisa.append("uf", new BasicDBObject("$regex", convertido).append("$options", "i"));
		}
		return pesquisa;
	}
	
	private BasicDBObject montarPesquisa(Map<String, String> filtros)
	{
		BasicDBObject pesquisa = new BasicDBObject();
		if (Verificador.isValorado(filtros))
		{
			for (Map.Entry<String, String> filtro : filtros.entrySet())
			{
				if (filtro.getValue() != null)
				{
					String valor = filtro.getValue().toString().trim();
					if (Verificador.isValorado(valor))
					{
						String convertido = AcentuacaoParaRegex.converter(valor);
						pesquisa.append(filtro.getKey(), new BasicDBObject("$regex", convertido).append("$options", "i"));
					}
				}
			}
		}
		return pesquisa;
	}
	
}
