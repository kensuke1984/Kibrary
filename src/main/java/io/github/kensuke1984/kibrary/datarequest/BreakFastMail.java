package io.github.kensuke1984.kibrary.datarequest;

import java.awt.GraphicsEnvironment;
import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * IRISのデータセンターに送るBREQ_FASTリクエストのメール
 * 
 * @version 0.0.5
 * 
 * @author Kensuke Konishi
 * 
 */
public class BreakFastMail {

	private static final String IRIS_EMAIL = "breq_fast@iris.washington.edu";
	private static final String OHP_EMAIL = "breq-fast@ocean.eri.u-tokyo.ac.jp";

	private String name;
	private String institute;
	private String mail;
	private String email;
	private String phone;
	private String fax;

	private String label = "19841006";
	private String media = "FTP";

	// private String[] alternateMedia = { "EXABYTE2", "DAT" };

	private Channel[] channels;

	public void setLabel(String newLabel) {
		label = newLabel;
	}

	/**
	 * password of the gmail account.
	 */
	private static String password;

	/**
	 * @return リクエストメールに書くべきヘッダー部分
	 */
	public String[] getLines() {
		List<String> lines = new ArrayList<>();
		lines.add(".NAME " + name);
		lines.add(".INST " + institute);
		lines.add(".MAIL " + mail);
		lines.add(".EMAIL " + email);
		lines.add(".PHONE " + phone);
		lines.add(".FAX " + fax);
		lines.add(".LABEL " + label);
		lines.add(".MEDIA " + media);
		lines.add(".END");
		if (channels != null)
			for (Channel channel : channels)
				lines.add(channel.toString());
		// lines[lines.length-1] = ".END";
		return lines.toArray(new String[lines.size()]);
	}

	void sendIris() throws Exception {
		sendIris(getLines());
	}

	private static String getPassword() throws InterruptedException {
		if (password != null)
			return password;
		else if (!GraphicsEnvironment.isHeadless()) {
			PasswordInput pi = PasswordInput.createAndShowGUI();
			password = pi.getPassword();
		} else {
			Console console = System.console();
			password = String.copyValueOf(console.readPassword("Password for waveformrequest2015@gmail.com"));
		}
		return password;
	}

	private static void sendIris(String[] lines) throws Exception {
		Email email = new SimpleEmail();
		email.setHostName("smtp.googlemail.com");
		email.setSmtpPort(465);
		getPassword();
		email.setAuthenticator(new DefaultAuthenticator("waveformrequest2015@gmail.com", password));
		email.setSSLOnConnect(true);
		email.setFrom("waveformrequest2015@gmail.com");
		email.setSubject("Request" + Utilities.getTemporaryString());
		email.setMsg(String.join("\n", lines));
		email.addTo(IRIS_EMAIL);
		email.send();
	}

	/**
	 * Channelのセット
	 * 
	 * @param channels
	 *            channels for the request
	 */
	public void setChannels(Channel[] channels) {
		this.channels = channels;
	}

	public BreakFastMail(String name, String institute, String mail, String email, String phone, String fax,
			String label, String media, Channel[] channels) {
		this.name = name;
		this.institute = institute;
		this.mail = mail;
		this.email = email;
		this.phone = phone;
		this.fax = fax;
		this.label = label;
		this.media = media;
		this.channels = channels;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * 
	 * Option: -iris now only iris is possible <br>
	 * Send mails every 2 min.
	 * @param args
	 *            (option) [mail file]
	 * @throws Exception
	 *             if an error occurs
	 */
	public static void main(String[] args) throws Exception {
		if (args[0].equals("-iris")) {
			for (int i = 1; i < args.length; i++) {
				sendIris(Files.readAllLines(Paths.get(args[i])).toArray(new String[0]));
				System.out.println("Sent "+args[i]);
				Thread.sleep(2 * 60 * 1000);
			}
		} else
			throw new IllegalArgumentException("-iris [mail files]   (Only sending to IRIS is possible now");

	}

}
