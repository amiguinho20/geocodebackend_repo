package br.com.fences.geocodebackend.cadastro.negocio;

import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.Logger;

import br.com.fences.fencesutils.formatar.FormatarData;

/**
 * Aguarda 24h para permitir nova consulta.
 * 
 */

@Singleton
public class TemporizadorCacheSingleton {
	
	@Inject
	private transient Logger logger;
	
	private final long VINTE_E_QUATRO_HORAS = 24 * 60 * 60 * 1000;
	
	private long tempoDoUltimoQueryLimit;
	
	public void registrarTempoDoUltimoQueryLimit()
	{
		tempoDoUltimoQueryLimit = Calendar.getInstance().getTimeInMillis();
	}
	
	public boolean permiteConsultarGeocode()
	{
		boolean permissao = true;
		if (tempoDoUltimoQueryLimit != 0)
		{
			long tempoAtual = Calendar.getInstance().getTimeInMillis();
			long periodo = tempoAtual - tempoDoUltimoQueryLimit;
			if (periodo < VINTE_E_QUATRO_HORAS)
			{
				String formatTempoDoUltimoQueryLimit = FormatarData.getDiaMesAnoComBarrasEHoraMinutoSegundoComDoisPontos().format(new Date(tempoDoUltimoQueryLimit));
				String formatTempoDaDisponibilidade = FormatarData.getDiaMesAnoComBarrasEHoraMinutoSegundoComDoisPontos().format(new Date(tempoDoUltimoQueryLimit + VINTE_E_QUATRO_HORAS));
				logger.info("Permissao negada desde[" + formatTempoDoUltimoQueryLimit + "] a autorizacao sera concedida em [" + formatTempoDaDisponibilidade + "].");
				permissao = false;
			}
		}
		return permissao;
	}

}
