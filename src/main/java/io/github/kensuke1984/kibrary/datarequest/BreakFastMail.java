package io.github.kensuke1984.kibrary.datarequest;

import io.github.kensuke1984.kibrary.Environment;
import io.github.kensuke1984.kibrary.util.Utilities;
import org.apache.commons.mail.DefaultAuthenticator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * BREQ_FAST request Mail
 *
 * @author Kensuke Konishi
 * @version 0.1.1
 */
public class BreakFastMail {

    private static final String IRIS_EMAIL = "breq_fast@iris.washington.edu";
    private static final String OHP_EMAIL = "breq-fast@ocean.eri.u-tokyo.ac.jp";

    private final String LABEL;
    private final String MEDIA = "FTP";

    // private String[] alternateMedia = { "EXABYTE2", "DAT" };
    private final Channel[] CHANNELS;

    public BreakFastMail(String label, Channel[] channels) {
        LABEL = label;
        CHANNELS = channels;
    }

    private static volatile DefaultAuthenticator authenticator;

    private static synchronized DefaultAuthenticator createAuthenticator() throws InterruptedException {
        return Objects.nonNull(authenticator) ? authenticator :
                new DefaultAuthenticator(Environment.getGmail(), Utilities.getPassword(Environment.getGmail()));
    }

    private static void sendIris(String[] lines) throws Exception {
        if (Objects.isNull(authenticator)) authenticator = createAuthenticator();
        Utilities.sendGmail("Request" + Utilities.getTemporaryString(), IRIS_EMAIL, lines, authenticator);
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

    /**
     * @return header parts in a request.
     */
    public String[] getLines() {
        List<String> lines = new ArrayList<>();
        lines.add(".NAME " + Environment.getUserName());
        lines.add(".INST " + Environment.getInstitute());
        lines.add(".MAIL " + Environment.getMail());
        lines.add(".EMAIL " + Environment.getEmail());
        lines.add(".PHONE " + Environment.getPhone());
        lines.add(".FAX " + Environment.getFax());
        lines.add(".LABEL " + LABEL);
        lines.add(".MEDIA " + MEDIA);
        lines.add(".END");
        if (CHANNELS != null) for (Channel channel : CHANNELS)
            lines.add(channel.toString());
        return lines.toArray(new String[0]);
    }

    void sendIris() throws Exception {
        sendIris(getLines());
    }

    public String getLabel() {
        return LABEL;
    }
}
