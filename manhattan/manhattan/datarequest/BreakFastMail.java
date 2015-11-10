package manhattan.datarequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

/**
 * IRISのデータセンターに送るBREQ_FASTリクエストのメール
 * 
 * @version 0.0.2
 * @since 2014/6/5
 * 
 * @version 0.0.3
 * @since 2015/1/21 Taiwan affiliation
 * 
 * @version 0.0.4
 * @since 2015/2/10
 * alternativeMedia is removed.
 * 
 * @version 0.0.5
 * @since 2015/2/12
 * {@link java.util.Calendar} &rarr; {@link java.time.LocalDateTime}
 * 
 * 
 * @author kensuke
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

	private void initialize() {
		name = "Kensuke Konishi";
		institute = "Institute of Earth Sciences, Academia Sinica";
		mail = "128, Sec. 2, Academia Road, Nangang, Taipei 11529, Taiwan";
		email = "kensuke@earth.sinica.edu.tw";
		phone = "+886-2-2783-9910 ext. 618";
		fax = "+886-2-2783-9871";

	}

	private BreakFastMail() {
		initialize();
	}

	private Channel[] channels;

	public void setLabel(String newLabel) {
		label = newLabel;
	}

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

	void sendIris() throws Exception{
		Email email = new SimpleEmail();
		email.setHostName("smtp.googlemail.com");
		email.setSmtpPort(465);
		email.setAuthenticator(new DefaultAuthenticator(
				"waveformrequest2015@gmail.com", "!dsmwave"));
		email.setSSLOnConnect(true);
		email.setFrom("waveformrequest2015@gmail.com");
		email.setSubject("Request "+label);
		email.setMsg(String.join("\n", getLines()));
		email.addTo(IRIS_EMAIL);
		email.send();

		return;
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

	public static void main(String[] args) {

		BreakFastMail b = new BreakFastMail();
		b.channels = new Channel[] {
				new Channel("AA", "II", LocalDateTime.now(),
						 LocalDateTime.now()),
				new Channel("?", "II", LocalDateTime.now(),
						 LocalDateTime.now()) };
		for (String line : b.getLines())
			System.out.println(line);

	}

	public BreakFastMail(String name, String institute, String mail,
			String email, String phone, String fax, String label, String media,
			 Channel[] channels) {
		super();
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
	
	public String getLabel(){
		return label;
	}

}
