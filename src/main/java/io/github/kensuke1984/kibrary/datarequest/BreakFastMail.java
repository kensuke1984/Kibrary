package io.github.kensuke1984.kibrary.datarequest;

import java.awt.GraphicsEnvironment;
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
 * @author Kensuke Konishi
 * @version 0.0.5.1
 */
public class BreakFastMail {

    private static final String IRIS_EMAIL = "breq_fast@iris.washington.edu";
//    private static final String OHP_EMAIL = "breq-fast@ocean.eri.u-tokyo.ac.jp";
//    private static final String OHP_EMAIL = "breq-fast-vietnet@ohpdmc.eri.u-tokyo.ac.jp";
    private static final String OHP_EMAIL = "breq-fast-tsar@ohpdmc.eri.u-tokyo.ac.jp";
    
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
        if (channels != null) for (Channel channel : channels)
            lines.add(channel.toString());
        return lines.toArray(new String[0]);
    }

    void sendIris() throws Exception {
        sendIris(getLines());
    }
    
    void sendOHP() throws Exception {
        sendOHP(getLines());
    }

    private static String getPassword() throws InterruptedException {
        if (password != null) return password;
        else if (!GraphicsEnvironment.isHeadless()) {
            password = PasswordInput.createAndShowGUI().getPassword();
        } else {
            password = String.copyValueOf(System.console().readPassword("Password for datarequest.yuki@gmail.com"));
        }
        return password;
    }

    private static void sendIris(String[] lines) throws Exception {
        Email email = new SimpleEmail();
        email.setHostName("smtp.googlemail.com");
        email.setSmtpPort(465);
        getPassword();
        email.setAuthenticator(new DefaultAuthenticator("datarequest.yuki@gmail.com", password));
        email.setSSLOnConnect(true);
        email.setFrom("datarequest.yuki@gmail.com");
        email.setSubject("Request" + Utilities.getTemporaryString());
        email.setMsg(String.join("\n", lines));
        email.addTo(IRIS_EMAIL);
        email.send();
    }
    
    private static void sendOHP(String[] lines) throws Exception {
        Email email = new SimpleEmail();
        email.setHostName("smtp.googlemail.com");
        email.setSmtpPort(465);
        getPassword();
        email.setAuthenticator(new DefaultAuthenticator("datarequest.yuki@gmail.com", password));
        email.setSSLOnConnect(true);
        email.setFrom("datarequest.yuki@gmail.com");
        email.setSubject("Request" + Utilities.getTemporaryString());
        email.setMsg(String.join("\n", lines));
        email.addTo(OHP_EMAIL);
        email.send();
    }

    /**
     * Channelのセット
     *
     * @param channels channels for the request
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
     * Option: -iris now only iris is possible <br>
     * Send mails every 2 min.
     *
     * @param args (option) [mail file]
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        if (args[0].equals("-iris")) {
            for (int i = 1; i < args.length; i++) {
                sendIris(Files.readAllLines(Paths.get(args[i])).toArray(new String[0]));
                System.out.println("Sent " + args[i]);
                Thread.sleep(2 * 60 * 1000);
            }
        } else throw new IllegalArgumentException("-iris [mail files]   (Only sending to IRIS is possible now");

    }

}
