package de.koehdaniel.email;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Properties;

import javax.mail.FolderClosedException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import com.sun.mail.imap.IMAPFolder;

public class Main {
	
	static PrintStream logFile = System.out;

	public static void main(String[] args) throws InterruptedException {
		File config = null;
		File log = null;
		try{
			config = new File(args[0]);
			log = new File(args[1]);
		}
		catch(Exception ex){
			//
		}
		
		if(log != null && !log.exists()){
			try{
				writeLine("Log-Datei wird versucht anzulegen");
				log.createNewFile();
			}
			catch(Exception ex){
				writeLine("Fehler beim erzeugen der Log-Datei");
				writeException(ex);
			}
		}
		if(log != null && log.exists()){
			try{
				writeLine("Ausgabe wird umgeleitet in: " + log.getCanonicalPath());
				setLogFile(log);
			}
			catch(Exception ex){
				writeLine("Fehler beim setzen der Log-Datei!");
				writeException(ex);
			}
		}
		
		if(config == null || !config.exists()){
			writeLine("Fehler, Config-Datei exisitert nicht!");
			if(config != null){
				try{
					writeLine(config.getCanonicalPath());
				}
				catch(Exception ex){
					writeLine(""+config);
				}
			}
			System.exit(-1);
		}
		
		int maxCon = 1;
		for(int i=0;i<maxCon;i++){
			Configuration c = new Configuration(config, i);
			maxCon = c.maxConnections+1;
			
			writeLine(c, "Start");
			Main main = new Main();
			
			int waitTime = c.getWaitTimeSec();
			c.setWaitTimeSec(0); //Damit am Anfang nicht unnötig gewartet wird
			main.openNewMailUrl(c);
			c.setWaitTimeSec(waitTime);
			
			main.starteVerarbeitungInThread(c);
		}
	}

	private void starteVerarbeitungInThread(Configuration c) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					process(c);
				} catch (MessagingException e) {
					writeLine(c, e.getMessage());
					writeException(c, e);
				}
				finally{
					writeLine(c,"Unerwartetes Ende, warte 60 Sekunden...");
					try{
						Thread.sleep(60000);
					}
					catch(Exception ex){
						writeLine(c, "Fehler bei Sleep");
						writeException(c,ex);
					}
					writeLine(c, "run wird neugestartet...");
					starteVerarbeitungInThread(c);
				}
			}
		}).start();
	}

	private static String get_current_time() {
		return new Date().toString();
	}
	
	private static void setLogFile(File logFile) throws FileNotFoundException{
		Main.logFile = new PrintStream(logFile);
	}
	
	public static void writeLine(Configuration c, String text){
		writeLine(text + " [" + c.getName() + "]");
	}
	
	@Deprecated
	public static void writeLine(String text){
		logFile.println(get_current_time() + " >> " + text);
	}
	
	public static void writeException(Configuration c, Exception ex){
		writeLine(c, ex.getMessage());
		writeException(ex);
	}
	
	@Deprecated
	public static void writeException(Exception ex){
		ex.printStackTrace(logFile);
	}
	
	public static String passwd_decrypt(String passwd){
		return passwd;
	}
	
	public static String passwd_encrypt(String klartext){
		return klartext;
	}

	private void process(Configuration c) throws MessagingException {
		// Variablen setzen
		final Properties props = System.getProperties();
		final Session session = Session.getDefaultInstance(props, null);
		final Store imapStore = session.getStore("imap");
		imapStore.connect(c.getHost(), c.getUser(), c.getPassword());

		// Erfolgreich Verbunden
		writeLine(c, "connected");

		// Den Ordner "Posteingang" auswählen
		final IMAPFolder folder = (IMAPFolder) imapStore.getFolder("Inbox");
		folder.open(IMAPFolder.READ_ONLY);

		// Listener-Registrieren
		folder.addMessageCountListener(new MessageCountListener() {

			public void messagesAdded(MessageCountEvent e) {
				openNewMailUrl(c);
			}


			public void messagesRemoved(MessageCountEvent e) {
				// writeLine("Message Removed Event fired");
			}
		});

		// Verbindung am Leben halten bzw. ggf. Neustarten
		while (!Thread.interrupted()) {
			try {
				folder.idle();
			} catch (FolderClosedException e) {
				writeLine(c, "The remote server closed the IMAP folder, we're going to try reconnecting.");
				process(c);
			} catch (MessagingException e) {
				writeLine(c, "Now closing imap mailbox, due to unhandlable exception: ");
				writeException(c,e);
				break;
			}
		}
	}
	
	private void openNewMailUrl(Configuration c) {
		if(c.getWaitTimeSec() > 0){
			try{
				writeLine(c, c.getWaitTimeSec() + " Sekunden warten");
				Thread.sleep(c.getWaitTimeSec()*1000);
			}
			catch(InterruptedException ex){
				//
			}
		}
		
		try {
			URL url = new URL(c.getUrl());
			URLConnection con = url.openConnection();
			writeLine(c, "URL erfolgreich aufgerufen");
			con.getInputStream();
		} catch (IOException ex) {
			writeLine(c, "Fehler beim URL aufruf");
			writeException(c, ex);
			try{
				if(c.getErrorUrl() != null && !c.getErrorUrl().isEmpty()){
					writeLine(c, "ErrorUrl wird aufgerufen");
					URL url = new URL(c.getErrorUrl());
					URLConnection con = url.openConnection();
					con.getInputStream();
				}
			}
			catch(Exception exc){
				writeLine(c, "Fehler beim Aufruf der Error-URL");
				writeException(c,exc);
			}
		}
	}
}
