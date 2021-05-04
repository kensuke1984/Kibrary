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
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
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

    static final String CODENAME = "Kushiro";

    static final String VERSION = "1.3.8.31b";

    private ANISOtime() {
    }

    //anisotime (shell script with jar)
    private static final String UNIX_SCRIPT_URL = "https://bit.ly/2DbS5NH";
    //anisotime.bat (Windows batch script with jar)
    private static final String WINDOWS_SCRIPT_URL = "https://bit.ly/31CXCX5";
    //user guide pdf
    private static final String USER_GUIDE_URL = "https://bit.ly/3hFNUZH";
    private static final Path USER_GUIDE_PATH = Environment.KIBRARY_SHARE.resolve("user_guide.pdf");


    static final Path AGREEMENT_PATH =
            Environment.KIBRARY_HOME.resolve(".anisotime_agreed");

    private static final Path LAST_ACTIVATION_PATH = Environment.KIBRARY_HOME.resolve(".anisotime_last_activation");


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (0 <= Arrays.binarySearch(args, "-U") || shouldCheckUpdate())
            try {
                downloadManual();
                downloadANISOtime();
                Files.setLastModifiedTime(LAST_ACTIVATION_PATH, FileTime.from(Instant.now()));
            } catch (IOException e) {
                System.err.println("Can't check for updates, could be due to Off-Line.");
            }


        if (!EULA.isAccepted())
            System.exit(71);

        if (args.length != 0 && (!args[0].equals("-U") || 2 < args.length)) try {
            ANISOtimeCLI.main(args);
            return;
        } catch (UnrecognizedOptionException e) {
            System.err.println("The command line has invalid options.");
            ANISOtimeCLI.printHelp();
            return;
        } catch (OutOfMemoryError oe) {
            System.err.println("Sorry, this machine does not have enough memory to run ANISOtime.\n" +
                    "Please try again on a more modern machine with more memory.");
            return;
        }

        if (GraphicsEnvironment.isHeadless())
            throw new HeadlessException("No graphical environment. Please use CLI.");

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
        Path localPath = Environment.KIBRARY_SHARE.resolve(Paths.get("user_guide.pdf"));
        Path path = Utilities.download(new URL(USER_GUIDE_URL));
        if (Files.exists(localPath)) {
            String localSum = Utilities.checksum(localPath, "SHA-256");
            String cloudSum = Utilities.checksum(path, "SHA-256");
            if (localSum.equals(cloudSum)) return;
        }
        Files.createDirectories(localPath.getParent());
        Files.move(path, localPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean shouldCheckUpdate() throws IOException {
        if (!Files.exists(LAST_ACTIVATION_PATH)) {
            Files.createFile(LAST_ACTIVATION_PATH);
            return true;
        }
        FileTime lastModifiedTime = Files.getLastModifiedTime(LAST_ACTIVATION_PATH);
        LocalDate localDate = Instant.ofEpochMilli(lastModifiedTime.toMillis()).atZone(ZoneId.systemDefault()).toLocalDate();
        return !LocalDate.now().equals(localDate);
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
        String updateMsg = "ANISOtime update in progress.  Please relaunch ANISOtime if it doesn't restart automatically.";
        try {
            Object[] choices = {"Close"};
            Object defaultChoice = choices[0];
            JOptionPane.showOptionDialog(null, updateMsg,
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, defaultChoice);
        } catch (HeadlessException e) {
            System.err.println(updateMsg);
        }
        System.exit(55);
    }

    static void showUserGuide() {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(USER_GUIDE_PATH.toFile());
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        JOptionPane
                .showMessageDialog(null, "Can't open the manual file. Look at " + USER_GUIDE_PATH + ".");
    }
}
