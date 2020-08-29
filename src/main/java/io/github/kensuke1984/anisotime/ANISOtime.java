package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.Environment;
import io.github.kensuke1984.kibrary.util.Utilities;
import org.apache.commons.cli.UnrecognizedOptionException;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ANISOtime launcher.
 *
 * @author Kensuke Konishi, Anselme Borgeaud
 * @version {@value #VERSION} {@value #CODENAME}
 */
final class ANISOtime {

    public static final String EMAIL_ADDRESS = "ut-globalseis@googlegroups.com";

    static final String CODENAME = "Tokoname";

    static final String VERSION = "1.3.8.22b";

    private ANISOtime() {
    }

    //anisotime (shell script)
    private static final String UNIX_SCRIPT_URL = "https://bit.ly/2YM8qjH";
    //anisotime.bat
    private static final String WINDOWS_SCRIPT_URL = "https://bit.ly/2EAgVra";
    //user guide pdf
    private static final String USER_GUIDE_URL = "https://bit.ly/32A4rIi";

    static final Path AGREEMENT_PATH =
            Environment.KIBRARY_HOME.resolve(".anisotime_agreed");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        try {
            downloadManual();
            downloadANISOtime();
        } catch (IOException e) {
            System.err.println("Can't check for updates, could be due to Off-Line.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!EULA.isAccepted())
            System.exit(71);
        if (args.length != 0) try {
            ANISOtimeCLI.main(args);
            return;
        } catch (UnrecognizedOptionException e) {
            System.err.println("The command line arguments are invalid.");
            ANISOtimeCLI.printHelp();
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } catch (OutOfMemoryError oe) {
            System.err.println("Sorry, this machine does not have enough memory to run ANISOtime.\n" +
                    "Please try again on a more modern machine with more memory.");
            return;
        }
        else if (GraphicsEnvironment.isHeadless()) {
            System.err.println("No graphical environment.. please use CLI.");
            ANISOtimeCLI.printHelp();
            return;
        }

        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(ANISOtime.class.getName()).log(Level.SEVERE, null, ex);
        }
        SwingUtilities.invokeLater(() -> new ANISOtimeGUI().setVisible(true));
    }

    private static void downloadManual() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        Path localPath = Environment.KIBRARY_HOME.resolve(Paths.get("share/user_guide.pdf"));
        Path path = Utilities.download(new URL(USER_GUIDE_URL));
        if (Files.exists(localPath)) {
            String localSum = Utilities.checksum(localPath, "SHA-256");
            String cloudSum = Utilities.checksum(path, "SHA-256");
            if (localSum.equals(cloudSum)) return;
        }
        Files.move(path, localPath, StandardCopyOption.REPLACE_EXISTING);
    }


    private static void downloadANISOtime() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        Path localPath = Paths.get(ANISOtime.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(localPath) || localPath.getFileName().toString().contains("kibrary")) return;
        String localSum = Utilities.checksum(localPath, "SHA-256");
        Path path = Utilities.download(
                new URL(System.getProperty("os.name").contains("Windows") ? WINDOWS_SCRIPT_URL : UNIX_SCRIPT_URL));
        String cloudSum = Utilities.checksum(path, "SHA-256");
        if (localSum.equals(cloudSum)) return;
        Files.move(path, localPath.resolveSibling("latest_anisotime"), StandardCopyOption.REPLACE_EXISTING);
        try {
            Object[] choices = {"Close"};
            Object defaultChoice = choices[0];
            JOptionPane.showOptionDialog(null, "ANISOtime update in progress. Program will relaunch automatically.",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, defaultChoice);
        } catch (HeadlessException e) {
            System.err.println("ANISOtime update in progress. Program will relaunch automatically.");
        }
        System.exit(55);
    }

}
