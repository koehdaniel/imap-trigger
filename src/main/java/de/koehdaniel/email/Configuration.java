package de.koehdaniel.email;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Configuration {

	private String name="DefaultConfiguration",host,user,password,url,errorUrl;
	private int waitTimeSec=0;
	public int maxConnections = -1;
	
	public Configuration(File file, int verbindung){
		List<String> lines = new ArrayList<String>();
		try{
			lines = Files.readAllLines(file.toPath());
		}
		catch(Exception ex){
			Main.writeException(this,ex);
		}

		int con = -1;
		for(String line : lines){
			if(line.startsWith("//")){
				//überspringen
			}
			else if(line.startsWith("#")){
				con++;
				if(con == verbindung)
					setName(line.substring(1));
			}
			else if(con == verbindung){
				String[] l = line.split("=",2);
				
				switch(l[0]){
				case "imapHost":setHost(l[1]);break;
				case "imapUser":setUser(l[1]);break;
				case "imapPswd":setPassword(Main.passwd_decrypt(l[1]));break;
				case "imapNewMailURL":setUrl(l[1]);break;
				case "imapNewMailErrorURL":setErrorUrl(l[1]);break;
				case "imapWaitTimeSec":setWaitTimeSec(Integer.parseInt(l[1]));break;
				default:Main.writeLine(this, "Unbekannte Variable:" + l[0]);
				}
			}
		}
		
		maxConnections = con;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getErrorUrl() {
		return errorUrl;
	}
	
	public void setErrorUrl(String errorUrl) {
		this.errorUrl = errorUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getWaitTimeSec() {
		return waitTimeSec;
	}
	
	public void setWaitTimeSec(int waitTimeSec) {
		this.waitTimeSec = waitTimeSec;
	}
	
}
