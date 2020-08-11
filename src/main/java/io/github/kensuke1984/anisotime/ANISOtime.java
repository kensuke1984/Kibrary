package io.github.kensuke1984.anisotime;

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

    static final String CODENAME = "Taoyuan";

    static final String VERSION = "1.3.8.9b";

    private ANISOtime() {
    }

    //anisotime (shell script)
    private static final String UNIX_SCRIPT_URL = "https://bit.ly/2Xdq5QI";
    //anisotime.bat
    private static final String WINDOWS_SCRIPT_URL = "https://bit.ly/2QUnqJr";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            downloadANISOtime();
        } catch (IOException e) {
            System.err.println("Can't check for updates, could be due to Off-Line.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private static void downloadANISOtime() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        Path localPath = Paths.get(ANISOtime.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(localPath) || localPath.toString().contains("kibrary")) return;
        String localSum = Utilities.checksum(localPath, "SHA-256");
        Path path = Utilities.download(
                new URL(System.getProperty("os.name").contains("Windows") ? WINDOWS_SCRIPT_URL : UNIX_SCRIPT_URL));
        String cloudSum = Utilities.checksum(path, "SHA-256");
        if (localSum.equals(cloudSum)) return;
        Files.move(path, localPath.resolveSibling("latest_anisotime"), StandardCopyOption.REPLACE_EXISTING);
        try {
            JOptionPane.showMessageDialog(null, "Software update is found. ANISOtime restarts.");
        } catch (HeadlessException e) {
            System.err.println("Software update is found. ANISOtime restarts.");
        }
        System.exit(55);
    }

}
