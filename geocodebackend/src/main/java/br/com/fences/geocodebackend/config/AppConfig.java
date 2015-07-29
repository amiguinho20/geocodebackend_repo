package br.com.fences.geocodebackend.config;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppConfig implements Serializable{

	private static final long serialVersionUID = 9159464655935948935L;

	private String logConsole;
	private String logLevel;
	private String logDiretorio;
	private String serverBackendHost;
	private String serverBackendPort;
	private String dbMongoHost;
	private String dbMongoPort;
	private String dbMongoDatabase;
	private String dbMongoUser;
	private String dbMongoPass;
	
	public String getLogConsole() {
		return logConsole;
	}
	public void setLogConsole(String logConsole) {
		this.logConsole = logConsole;
	}
	public String getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}
	public String getLogDiretorio() {
		return logDiretorio;
	}
	public void setLogDiretorio(String logDiretorio) {
		this.logDiretorio = logDiretorio;
	}
	public String getServerBackendHost() {
		return serverBackendHost;
	}
	public void setServerBackendHost(String serverBackendHost) {
		this.serverBackendHost = serverBackendHost;
	}
	public String getServerBackendPort() {
		return serverBackendPort;
	}
	public void setServerBackendPort(String serverBackendPort) {
		this.serverBackendPort = serverBackendPort;
	}
	public String getDbMongoHost() {
		return dbMongoHost;
	}
	public void setDbMongoHost(String dbMongoHost) {
		this.dbMongoHost = dbMongoHost;
	}
	public String getDbMongoPort() {
		return dbMongoPort;
	}
	public void setDbMongoPort(String dbMongoPort) {
		this.dbMongoPort = dbMongoPort;
	}
	public String getDbMongoDatabase() {
		return dbMongoDatabase;
	}
	public void setDbMongoDatabase(String dbMongoDatabase) {
		this.dbMongoDatabase = dbMongoDatabase;
	}
	public String getDbMongoUser() {
		return dbMongoUser;
	}
	public void setDbMongoUser(String dbMongoUser) {
		this.dbMongoUser = dbMongoUser;
	}
	public String getDbMongoPass() {
		return dbMongoPass;
	}
	public void setDbMongoPass(String dbMongoPass) {
		this.dbMongoPass = dbMongoPass;
	}

	
}
